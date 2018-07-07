package com.ridrio.core


interface ChildEventListener {
  fun onAdded(key: String, data: Object)

  fun onModified(key: String, data: Object)

  fun onRemoved(key: String, data: Object)

}