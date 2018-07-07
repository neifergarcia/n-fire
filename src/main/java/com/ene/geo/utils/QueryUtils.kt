package com.ene.geo.utils

import com.ene.geo.GeoDataLocation
import com.ene.geo.hash.Hash


class QueryUtils {
  companion object {
    fun bitsLatitude(resolution: Double): Double {
      return Math.min(Math.log(Constants.EARTH_MERIDIONAL_CIRCUMFERENCE / 2 / resolution) / Math.log(2.0),
        Hash.MAX_PRECISION_BITS.toDouble())
    }

    fun bitsLongitude(resolution: Double, latitude: Double): Double {
      val degrees = GeoUtils.distanceToLongitudeDegrees(resolution, latitude)
      return if (Math.abs(degrees) > 0) Math.max(1.0, Math.log(360 / degrees) / Math.log(2.0))
      else (1).toDouble()
    }

    fun bitsForBoundingBox(location: GeoDataLocation, size: Double): Int {
      val latitudeDegreesDelta = GeoUtils.distanceToLatitudeDegrees(size)
      val latitudeNorth = Math.min((90).toDouble(), location.latitude + latitudeDegreesDelta)
      val latitudeSouth = Math.max((-90).toDouble(), location.latitude - latitudeDegreesDelta)
      val bitsLatitude = Math.floor(QueryUtils.bitsLatitude(size)).toInt() * 2
      val bitsLongitudeNorth = Math.floor(QueryUtils.bitsLongitude(size, latitudeNorth)).toInt() * 2 - 1
      val bitsLongitudeSouth = Math.floor(QueryUtils.bitsLongitude(size, latitudeSouth)).toInt() * 2 - 1
      return Math.min(bitsLatitude, Math.min(bitsLongitudeNorth, bitsLongitudeSouth))
    }
  }
}