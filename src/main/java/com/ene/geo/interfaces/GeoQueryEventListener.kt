package com.ene.geo.interfaces

import com.ene.core.QueryEventListener
import com.ene.geo.GeoData

interface GeoQueryEventListener : QueryEventListener {

  fun onKeyEntered(key: String, geoData: GeoData)

  fun onKeyMoved(key: String, geoData: GeoData)

  fun onKeyExited(key: String)

}