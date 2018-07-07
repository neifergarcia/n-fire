package com.ridrio.geo

import com.ridrio.core.Core
import com.ridrio.geo.hash.Hash
import com.ridrio.geo.utils.GeoUtils
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot


class Geo(private val reference: Array<String>) {
  companion object {
    fun getDataLocation(geoData: GeoData): GeoDataLocation? {
      return if (GeoUtils.coordinatesValid(geoData.location)) geoData.location else null
    }
  }
  private val nCore = Core(GeoData())

  fun addGeoData(key: String, geoData: GeoData, listener: Core.EneDocumentActionListener? = null) {
    val geoHash = Hash(geoData.location)
    geoData.hash = geoHash.getGeoHashString()
    nCore.addDocument(reference, geoData, listener, key)
  }

  fun fetchGeoData(key: String, listener: Core.EneDocumentListener) {
    nCore.getDocument(reference.plus(key), listener)
  }

  fun delGeoData(key: String, listener: Core.EneDocumentActionListener? = null) {
    nCore.delDocument(reference.plus(key), listener)
  }

  fun getDatabaseReference(): CollectionReference {
    return nCore.getCollectionReference(this.reference)
  }

  fun getDatabase(): Core {
    return nCore
  }

  fun documentsWheres(wheres: Array<Array<Any>>,
                      eventListener: EventListener<QuerySnapshot>): ListenerRegistration {
    return nCore.getDocumentsWhere(reference, wheres, eventListener)
  }

  fun queryAtLocation(geoCenter: GeoData, radius: Double): Query {
    return Query(this@Geo, geoCenter.location, radius)
  }
}