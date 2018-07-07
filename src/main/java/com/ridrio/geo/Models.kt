package com.ridrio.geo

import com.ridrio.core.Object

data class GeoDataLocation(val latitude: Double = 0.0, val longitude: Double = 0.0)

data class GeoData(
  val location: GeoDataLocation = GeoDataLocation(), var hash: String = ""
) : Object()