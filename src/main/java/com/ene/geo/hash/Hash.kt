package com.ene.geo.hash

import com.ene.geo.GeoDataLocation
import com.ene.geo.utils.Base32Utils
import com.ene.geo.utils.GeoUtils


class Hash {

  companion object {
    // The maximal precision of a hash
    const val MAX_PRECISION = 22
    // The maximal number of bits precision for a hash
    const val MAX_PRECISION_BITS = MAX_PRECISION * Base32Utils.BITS_PER_BASE32_CHAR
    // The default precision of a hash
    private const val DEFAULT_PRECISION = 10
  }

  private var geoHash: String

  constructor(geoDataLocation: GeoDataLocation, precision: Int = 10) {
    geoHash = init(geoDataLocation, precision)
  }

  constructor(latitude: Double, longitude: Double, precision: Int) {
    geoHash = init(GeoDataLocation(latitude, longitude), precision)
  }

  private fun init(geoDataLocation: GeoDataLocation, precision: Int = 10): String {
    if (precision < 1) {
      throw IllegalArgumentException("Precision of GeoHash must be larger than zero!")
    }
    if (precision > MAX_PRECISION) {
      throw IllegalArgumentException("Precision of a GeoHash must be less than " + (MAX_PRECISION + 1) + "!")
    }
    if (!GeoUtils.coordinatesValid(geoDataLocation.latitude, geoDataLocation.longitude)) {
      throw IllegalArgumentException(
        "Not valid location coordinates: [${geoDataLocation.latitude}, ${geoDataLocation.longitude}]"
      )
    }
    val longitudeRange = doubleArrayOf(-180.0, 180.0)
    val latitudeRange = doubleArrayOf(-90.0, 90.0)

    val buffer = CharArray(precision)

    for (i in 0 until precision) {
      var hashValue = 0
      for (j in 0 until Base32Utils.BITS_PER_BASE32_CHAR) {
        val even = (i * Base32Utils.BITS_PER_BASE32_CHAR + j) % 2 == 0
        val value = if (even) geoDataLocation.longitude else geoDataLocation.latitude
        val range = if (even) longitudeRange else latitudeRange
        val mid = (range[0] + range[1]) / 2
        if (value > mid) {
          hashValue = (hashValue shl 1) + 1
          range[0] = mid
        } else {
          hashValue = hashValue shl 1
          range[1] = mid
        }
      }
      buffer[i] = Base32Utils.valueToBase32Char(hashValue)
    }
    return String(buffer)
  }

  fun getGeoHashString(): String {
    return geoHash
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    val that = other as Hash?

    return geoHash == that!!.geoHash
  }

  override fun toString(): String {
    return "GeoHash {geoHash: $geoHash}"
  }

  override fun hashCode(): Int {
    return geoHash.hashCode()
  }
}