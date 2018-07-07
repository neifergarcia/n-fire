package com.ridrio.geo.interfaces

import com.ridrio.core.QueryEventListener
import com.ridrio.geo.GeoData

interface GeoQueryEventListener : QueryEventListener {

  fun onKeyEntered(key: String, geoData: GeoData)

  fun onKeyMoved(key: String, geoData: GeoData)

  fun onKeyExited(key: String)

}