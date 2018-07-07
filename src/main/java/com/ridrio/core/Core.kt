package com.ridrio.core

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*


class Core(model: Object? = null) {

  private val TAG = javaClass.simpleName

  private var nObject = model

  private val firestore = FirebaseFirestore.getInstance()

  fun getInstanceDB(): FirebaseFirestore {
    return firestore
  }

  fun setObject(model: Object) {
    nObject = model
  }

  fun getObjectClass(): Class<out Object> {
    return nObject!!::class.java
  }

  fun addDocument(pathNested: Array<String>, document: Object,
                  listener: EneDocumentActionListener? = null, keyDocument: String = "") {

    document.metadata.user = if (FirebaseAuth.getInstance().uid != null) FirebaseAuth.getInstance().uid!! else ""
    var key = keyDocument
    val docRef = getReferenceNested(pathNested) as CollectionReference

    val task: Task<Void>
    if (keyDocument.isEmpty()) {
      val keyDocRef = docRef.document()
      task = keyDocRef.set(document)
      key = keyDocRef.id
    } else {
      task = docRef.document(keyDocument).set(document)
    }
    if (listener != null) {
      task
        .addOnSuccessListener {
          listener.onComplete(true, key)
        }
        .addOnFailureListener { exception ->
          listener.onComplete(false, exception.message!!)
        }
    }
  }

  fun getCollection(pathNested: Array<String>, listener: CollectionListener): ListenerRegistration? {
    if (nObject == null) {
      throw Exception("Object is null")
    }
    val docRef = getReferenceNested(pathNested) as CollectionReference

    val isEventListening = listener is EneEventCollectionListener
    var registration: ListenerRegistration? = null
    if (isEventListening) {
      registration = docRef.addSnapshotListener(EventListener<QuerySnapshot> { value, err ->
        if (err != null) {
          listener.onComplete(listOf())
          return@EventListener
        }
        responseCollection(value!!, listener)
      })
    } else {
      docRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
          responseCollection(task.result, listener)
        } else {
          Log.d(TAG, task.exception!!.toString())
        }
      }
    }
    return registration
  }

  fun getDocument(pathNested: Array<String>, listener: DocumentListener): ListenerRegistration? {
    if (nObject == null) {
      throw Exception("Object is null")
    }
    val docRef = getReferenceNested(pathNested) as DocumentReference

    val isEventListening = listener is EneEventCollectionListener
    var registration: ListenerRegistration? = null
    if (isEventListening) {
      registration = docRef.addSnapshotListener(EventListener<DocumentSnapshot> { value, err ->
        if (err != null) {
          listener.onComplete(null)
          Log.w(TAG, "Listen Failed", err)
          return@EventListener
        }
        responseDocument(value!!, listener)
      })
    } else {
      docRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
          responseDocument(task.result, listener)
        } else {
          Log.d(TAG, task.exception!!.toString())
        }
      }
    }

    return registration
  }

  fun getDocumentsWhere(pathNested: Array<String>, wheres: Array<Array<Any>>,
                        listener: CollectionListener): ListenerRegistration? {
    if (nObject == null) {
      throw Exception("Object is null")
    }
    var queryRef: Query = getReferenceNested(pathNested) as CollectionReference
    wheres.forEach { where ->
      queryRef = queryWhere(queryRef, where[0].toString(), where[1].toString(), where[2])
    }

    val isEventListening = listener is EneEventCollectionListener
    var registration: ListenerRegistration? = null
    if (isEventListening) {
      registration = queryRef.addSnapshotListener(EventListener<QuerySnapshot> { value, err ->
        if (err != null) {
          listener.onComplete(listOf())
          Log.w(TAG, "Listen Failed", err)
          return@EventListener
        }
        responseCollection(value!!, listener)
      })
    } else {
      queryRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
          responseCollection(task.result, listener)
        } else {
          Log.w(TAG, "Complete Failed", task.exception!!)
        }
      }
    }
    return registration
  }

  fun getDocumentsWhere(pathNested: Array<String>, wheres: Array<Array<Any>>,
                        eventListener: EventListener<QuerySnapshot>): ListenerRegistration {
    if (nObject == null) {
      throw Exception("Object is null")
    }
    var queryRef: Query = getReferenceNested(pathNested) as CollectionReference
    wheres.forEach { where ->
      queryRef = queryWhere(queryRef, where[0].toString(), where[1].toString(), where[2])
    }
    return queryRef.addSnapshotListener(eventListener)
  }

  fun delDocument(pathNested: Array<String>, listener: EneDocumentActionListener? = null) {
    val docRef = getReferenceNested(pathNested) as DocumentReference
    val task = docRef.delete()
    if (listener != null) {
      task
        .addOnSuccessListener {
          listener.onComplete(true)
        }
        .addOnFailureListener { exception ->
          listener.onComplete(false, exception.message!!)
        }
    }
  }

  fun getCollectionReference(pathNested: Array<String>): CollectionReference {
    return getReferenceNested(pathNested, retLastCollectionReference = true) as CollectionReference
  }

  fun getDocumentReference(pathNested: Array<String>): DocumentReference {
    return getReferenceNested(pathNested, retLastDocumentReference = true) as DocumentReference
  }

  private fun getReferenceNested(nested: Array<String>,
                                 retLastCollectionReference: Boolean = false,
                                 retLastDocumentReference: Boolean = false): Any {
    var collectionReference = firestore.collection(nested[0])

    if (nested.size > 1) {
      var documentReference = collectionReference.document(nested[1])

      var lastCollectionReference = collectionReference
      var lastDocumentReference = documentReference

      nested.forEachIndexed { index, path ->
        if (index > 1) {
          if ((index % 2) == 0) {
            lastDocumentReference = documentReference
            collectionReference = documentReference.collection(path)
          } else {
            lastCollectionReference = collectionReference
            documentReference = collectionReference.document(path)
          }
        }
      }

      if (retLastCollectionReference) {
        return lastCollectionReference
      } else if (retLastDocumentReference) {
        return lastDocumentReference
      }

      return if ((nested.size % 2) == 0) {
        documentReference
      } else {
        collectionReference
      }
    }
    return collectionReference
  }

  private fun queryWhere(query: Query, field: String, operator: String, value: Any): Query {
    return when (operator) {
      ">" -> {
        query.whereGreaterThan(field, value)
      }
      ">=" -> {
        query.whereGreaterThanOrEqualTo(field, value)
      }
      "<" -> {
        query.whereLessThan(field, value)
      }
      "<=" -> {
        query.whereLessThanOrEqualTo(field, value)
      }
      else -> {
        query.whereEqualTo(field, value)
      }
    }
  }

  fun responseCollection(snapshot: QuerySnapshot, listener: CollectionListener) {
    if (!snapshot.isEmpty) {
      val models = mutableListOf<Object>()
      snapshot.forEach {
        val model = it.toObject(getObjectClass())
        model.metadata.id = it.id
        models.add(model)
      }
      listener.onComplete(models)
    } else {
      listener.onComplete(listOf())
    }
  }

  fun responseDocument(documentSnapshot: DocumentSnapshot, listener: DocumentListener) {
    if (documentSnapshot.exists()) {
      val model = documentSnapshot.toObject(getObjectClass())
      model!!.metadata.id = documentSnapshot.id
      listener.onComplete(model)
    } else {
      listener.onComplete(null)
    }
  }

  // Interfaces
  interface DocumentListener {
    fun onComplete(document: Object?)
  }

  interface CollectionListener {
    fun onComplete(documents: List<Object>)
  }

  interface EneDocumentActionListener {
    fun onComplete(success: Boolean, data: String = "")
  }

  interface EneCollectionListener : CollectionListener
  interface EneDocumentListener : DocumentListener

  interface EneEventCollectionListener : CollectionListener
  interface EneEventDocumentListener : DocumentListener
}