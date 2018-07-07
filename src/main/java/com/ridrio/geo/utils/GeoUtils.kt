package com.ridrio.geo.utils

import com.ridrio.geo.GeoData
import com.ridrio.geo.GeoDataLocation

class GeoUtils {
  companion object {
    fun coordinatesValid(latitude: Double, longitude: Double): Boolean {
      return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
    }

    fun coordinatesValid(geoDataLocation: GeoDataLocation): Boolean {
      return coordinatesValid(geoDataLocation.latitude, geoDataLocation.longitude)
    }

    fun distance(location1: GeoDataLocation, location2: GeoDataLocation): Double {
      return distance(location1.latitude, location1.longitude, location2.latitude, location2.longitude)
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
      // Earth's mean radius in meters
      val radius = (Constants.EARTH_EQ_RADIUS + Constants.EARTH_POLAR_RADIUS) / 2
      val latDelta = Math.toRadians(lat1 - lat2)
      val lonDelta = Math.toRadians(long1 - long2)

      val a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2)
      return radius * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun distanceToLatitudeDegrees(distance: Double): Double {
      return distance / Constants.METERS_PER_DEGREE_LATITUDE
    }

    fun distanceToLongitudeDegrees(distance: Double, latitude: Double): Double {
      val radians = Math.toRadians(latitude)
      val numerator = Math.cos(radians) * Constants.EARTH_EQ_RADIUS * Math.PI / 180
      val denominator = 1 / Math.sqrt(1 - Constants.EARTH_E2 * Math.sin(radians) * Math.sin(radians))
      val deltaDegrees = numerator * denominator
      return if (deltaDegrees < Constants.EPSILON) {
        if (distance > 0) 360.toDouble() else distance
      } else {
        Math.min(360.0, distance / deltaDegrees)
      }
    }

    fun wrapLongitude(longitude: Double): Double {
      if (longitude >= -180 && longitude <= 180) {
        return longitude
      }
      val adjusted = longitude + 180
      return if (adjusted > 0) {
        adjusted % 360.0 - 180
      } else {
        180 - -adjusted % 360
      }
    }

    fun equals(geoData1: GeoData, geoData2: GeoData): Boolean {
      if (geoData1 == geoData2) return true
      return geoData1.location.latitude.compareTo(geoData2.location.latitude) == 0 &&
        geoData1.location.longitude.compareTo(geoData2.location.longitude) == 0
    }

    fun hashCode(geoData: GeoData): Int {
      var result: Int
      var temp = java.lang.Double.doubleToLongBits(geoData.location.latitude)
      result = (temp xor temp.ushr(32)).toInt()
      temp = java.lang.Double.doubleToLongBits(geoData.location.longitude)
      result = 31 * result + (temp xor temp.ushr(32)).toInt()
      return result
    }

    fun toString(geoData: GeoData): String {
      return "GeoUtils(${geoData.location.latitude}, ${geoData.location.longitude})"
    }
  }
}