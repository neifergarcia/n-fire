package com.ene.geo

import android.util.Log
import com.ene.core.*
import com.ene.geo.hash.Hash
import com.ene.geo.hash.Query
import com.ene.geo.interfaces.GeoQueryEventListener
import com.ene.geo.utils.GeoUtils
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot


class Query(private val eneGeo: Geo, private var center: GeoDataLocation, radius: Double) :
  ChildEventListener {
  private val TAG = javaClass.simpleName
  // convert from kilometers to meters
  private var radius = radius * 1000

  private val eventRaiser = EventRaiser()

  private val eventListeners = hashSetOf<QueryEventListener>()
  private val outstandingQueries = hashSetOf<Query>()

  private data class GeoDataWithHash(val geoData: GeoData,
                                     val geoHash: Hash, val inGeoQuery: Boolean)

  private val eneQueries = hashMapOf<Query, ListenerRegistration>()
  private val listGeoData = hashMapOf<String, GeoDataWithHash>()
  private var queries: HashSet<Query>? = null

  private val eventListener = EventListener<QuerySnapshot> { value, _ ->
    synchronized(this@Query) {
      for (dc in value!!.documentChanges) {
        val document = dc.document
        val model = document.toObject(eneGeo.getDatabase().getObjectClass())
        model.metadata.id = document.id
        when (dc.type) {
          DocumentChange.Type.ADDED -> {
            Log.d(TAG, "Added: ${document.data}")
            onAdded(document.id, model)
          }
          DocumentChange.Type.MODIFIED -> {
            Log.d(TAG, "Modified: ${document.data}")
            onModified(document.id, model)
          }
          DocumentChange.Type.REMOVED -> {
            Log.d(TAG, "Removed: ${document.data}")
            onRemoved(document.id, model)
          }
        }
      }
    }
  }

  init {
    if (radius <= (0).toDouble()) {
      throw Throwable("Error radius can't be zero")
    }
  }

  fun reset() {
    outstandingQueries.clear()

    for (entry in eneQueries.entries) {
      entry.value.remove()
    }

    eneQueries.clear()

    queries = null
    listGeoData.clear()
  }

  private fun raiseEvent(runnable: Runnable) {
    eventRaiser.raiseEvent(runnable)
  }

  private fun geoDataLocationInQuery(location: GeoDataLocation): Boolean {
    return GeoUtils.distance(location, center) <= radius
  }

  private fun updateGeoData(key: String, geoData: GeoData) {
    val oldGD = listGeoData[key]
    val added = (oldGD == null)
    val modified = (!added && (oldGD!!.geoData.location != geoData.location))
    val wasInGeoQuery = (!added && oldGD!!.inGeoQuery)

    val inGeoQuery = geoDataLocationInQuery(geoData.location)

    if ((added || !wasInGeoQuery) && inGeoQuery) {
      for (listener in eventListeners) {
        raiseEvent(Runnable { (listener as GeoQueryEventListener).onKeyEntered(key, geoData) })
      }
    } else if (!added && modified && inGeoQuery) {
      for (listener in eventListeners) {
        raiseEvent(Runnable { (listener as GeoQueryEventListener).onKeyMoved(key, geoData) })
      }
    } else if (wasInGeoQuery && !inGeoQuery) {
      for (listener in eventListeners) {
        raiseEvent(Runnable { (listener as GeoQueryEventListener).onKeyExited(key) })
      }
    }
    listGeoData[key] = GeoDataWithHash(geoData, Hash(geoData.location),
      geoDataLocationInQuery(geoData.location))
  }

  private fun geoHashQueriesContainGeoHash(geoHash: Hash): Boolean {
    if (this.queries == null) {
      return false
    }
    for (query in this.queries!!) {
      if (query.containsGeoHash(geoHash)) {
        return true
      }
    }
    return false
  }

  fun hasListeners(): Boolean {
    return eventListeners.isNotEmpty()
  }

  fun canReady(): Boolean {
    return outstandingQueries.isEmpty()
  }

  fun checkAndReady() {
    if (canReady()) {
      for (listener in eventListeners) {
        eventRaiser.raiseEvent(Runnable { listener.onQueryReady() })
      }
    }
  }

  fun addQueryEventListener(listener: QueryEventListener) {
    if (eventListeners.contains(listener)) {
      throw IllegalArgumentException("Added the same listener twice to a EneQuery!")
    }
    eventListeners.add(listener)

    if (queries == null) {
      initQueries()
    } else {
      for (entry in listGeoData) {
        val key = entry.key
        val value = entry.value
        if (value.inGeoQuery) {
          raiseEvent(Runnable { (listener as GeoQueryEventListener).onKeyEntered(key, value.geoData) })
        }
      }
      if (canReady()) {
        raiseEvent(Runnable { listener.onQueryReady() })
      }
    }
  }

  private fun initQueries() {
    val oldQueries: HashSet<Query> = if (queries == null) hashSetOf() else queries!!
    val newQueries: HashSet<Query> = Query.queriesAtLocation(center, radius)
    queries = newQueries
    for (query in oldQueries) {
      if (!newQueries.contains(query)) {
        eneQueries[query]!!.remove()
        eneQueries.remove(query)
        outstandingQueries.remove(query)
      }
    }
    for (query in newQueries) {
      if (!oldQueries.contains(query)) {
        outstandingQueries.add(query)
        eneQueries[query] = eneGeo.documentsWheres(arrayOf(
          arrayOf("hash", ">=", query.getStartValue() as Any),
          arrayOf("hash", "<=", query.getEndValue() as Any)
        ), eventListener)
      }
    }

    for (entry in listGeoData) {
      if (geoHashQueriesContainGeoHash(entry.value.geoHash)) {
        updateGeoData(entry.key, entry.value.geoData)
      } else {
        listGeoData.remove(entry.key)
      }
    }

    checkAndReady()
  }

  fun setGeoDataLocation(center: GeoDataLocation, radius: Double = (0).toDouble()) {
    this.center = center
    if (radius > 0) {
      this.radius = radius * 1000
    }

    if (hasListeners()) {
      initQueries()
    }
  }

  fun getGeoDataLocation(): GeoDataLocation {
    return center
  }

  fun setRadius(radius: Double) {
    if (radius > 0) {
      this.radius = radius * 1000
      if (hasListeners()) {
        initQueries()
      }
    }
  }

  fun getRadius(): Double {
    return radius / 1000
  }

  fun removeQueryEventListener(listener: QueryEventListener) {
    if (!eventListeners.contains(listener)) {
      throw IllegalArgumentException("Trying to remove listener that was removed or not added!")
    }
    eventListeners.remove(listener)
    if (!this.hasListeners()) {
      reset()
    }
  }

  fun removeAllQueryEventListener() {
    eventListeners.clear()
    reset()
  }

  override fun onAdded(key: String, data: Object) {
    val geoDataLocation = Geo.getDataLocation(data as GeoData)
    if (geoDataLocation != null) {
      updateGeoData(key, data)
    }
  }

  override fun onModified(key: String, data: Object) {
    val geoDataLocation = Geo.getDataLocation(data as GeoData)
    if (geoDataLocation != null) {
      updateGeoData(key, data)
    }
  }

  override fun onRemoved(key: String, data: Object) {
    val id = (data as GeoData).metadata.id
    val geoDataHash = listGeoData[id]
    if (geoDataHash != null) {
      eneGeo.fetchGeoData(id, object : Core.EneDocumentListener {
        override fun onComplete(document: Object?) {
          synchronized(this@Query) {
            val geoDataLocation = if (document != null) Geo.getDataLocation(document as GeoData) else null
            val hash = if (geoDataLocation != null) Hash(geoDataLocation) else null
            if (hash == null || !geoHashQueriesContainGeoHash(hash)) {
              val geoData = listGeoData[id]
              if (geoData != null) {
                listGeoData.remove(id)
                if (geoData.inGeoQuery) {
                  for (listener in eventListeners) {
                    raiseEvent(Runnable { (listener as GeoQueryEventListener).onKeyExited(id) })
                  }
                }
              }
            }
          }
        }
      })
    }
  }
}