package com.ridrio.geo.hash

import com.ridrio.geo.GeoDataLocation
import com.ridrio.geo.utils.Base32Utils
import com.ridrio.geo.utils.Constants
import com.ridrio.geo.utils.GeoUtils
import com.ridrio.geo.utils.QueryUtils


class Query(private val startValue: String, private val endValue: String) {
  companion object {
    private fun queryForHash(hash: Hash, bits: Int): Query {
      var hashStr = hash.getGeoHashString()
      val precision: Int = Math.ceil((bits / Base32Utils.BITS_PER_BASE32_CHAR).toDouble()).toInt()
      if (hashStr.length < precision) {
        return Query(hashStr, "$hashStr~")
      }
      hashStr = hashStr.substring(0, precision)
      val base = hashStr.substring(0, hashStr.length - 1)
      val lastValue = Base32Utils.base32CharToValue(hashStr[hashStr.length - 1])
      val significantBits = bits - base.length * Base32Utils.BITS_PER_BASE32_CHAR
      val unusedBits = Base32Utils.BITS_PER_BASE32_CHAR - significantBits
      // delete unused bits
      val startValue = lastValue shr unusedBits shl unusedBits
      val endValue = startValue + (1 shl unusedBits)
      val startHash = base + Base32Utils.valueToBase32Char(startValue)
      val endHash = if (endValue > 31) "$base~" else base + Base32Utils.valueToBase32Char(endValue)
      return Query(startHash, endHash)
    }

    fun queriesAtLocation(location: GeoDataLocation, radius: Double): HashSet<Query> {
      val queryBits = Math.max(1, QueryUtils.bitsForBoundingBox(location, radius))
      val geoHashPrecision = Math
        .ceil(queryBits.toDouble() / Base32Utils.BITS_PER_BASE32_CHAR).toInt()

      val latitude = location.latitude
      val longitude = location.longitude
      val latitudeDegrees = radius / Constants.METERS_PER_DEGREE_LATITUDE
      val latitudeNorth = Math.min(90.0, latitude + latitudeDegrees)
      val latitudeSouth = Math.max(-90.0, latitude - latitudeDegrees)
      val longitudeDeltaNorth = GeoUtils.distanceToLongitudeDegrees(radius, latitudeNorth)
      val longitudeDeltaSouth = GeoUtils.distanceToLongitudeDegrees(radius, latitudeSouth)
      val longitudeDelta = Math.max(longitudeDeltaNorth, longitudeDeltaSouth)

      val queries = HashSet<Query>()

      val hash = Hash(latitude, longitude, geoHashPrecision)
      val geoHashW = Hash(latitude, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision)
      val geoHashE = Hash(latitude, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision)

      val geoHashN = Hash(latitudeNorth, longitude, geoHashPrecision)
      val geoHashNW = Hash(latitudeNorth, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision)
      val geoHashNE = Hash(latitudeNorth, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision)

      val geoHashS = Hash(latitudeSouth, longitude, geoHashPrecision)
      val geoHashSW = Hash(latitudeSouth, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision)
      val geoHashSE = Hash(latitudeSouth, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision)

      queries.add(Query.queryForHash(hash, queryBits))
      queries.add(Query.queryForHash(geoHashE, queryBits))
      queries.add(Query.queryForHash(geoHashW, queryBits))
      queries.add(Query.queryForHash(geoHashN, queryBits))
      queries.add(Query.queryForHash(geoHashNE, queryBits))
      queries.add(Query.queryForHash(geoHashNW, queryBits))
      queries.add(Query.queryForHash(geoHashS, queryBits))
      queries.add(Query.queryForHash(geoHashSE, queryBits))
      queries.add(Query.queryForHash(geoHashSW, queryBits))

      // Join queries
      var didJoin: Boolean
      do {
        var query1: Query? = null
        var query2: Query? = null
        for (query in queries) {
          for (other in queries) {
            if (query !== other && query.canJoinWith(other)) {
              query1 = query
              query2 = other
              break
            }
          }
        }
        didJoin = if (query1 != null && query2 != null) {
          queries.remove(query1)
          queries.remove(query2)
          queries.add(query1.joinWith(query2))
          true
        } else {
          false
        }
      } while (didJoin)

      return queries
    }
  }

  private fun isPrefix(other: Query): Boolean {
    return other.endValue >= this.startValue &&
      other.startValue < this.startValue &&
      other.endValue < this.endValue
  }

  private fun isSuperQuery(other: Query): Boolean {
    val startCompare = other.startValue.compareTo(this.startValue)
    return if (startCompare <= 0) {
      other.endValue >= this.endValue
    } else {
      false
    }
  }

  fun canJoinWith(other: Query): Boolean {
    return this.isPrefix(other) || other.isPrefix(this) || this.isSuperQuery(other) || other.isSuperQuery(this)
  }

  fun joinWith(other: Query): Query {
    return when {
      other.isPrefix(this) -> Query(this.startValue, other.endValue)
      this.isPrefix(other) -> Query(other.startValue, this.endValue)
      this.isSuperQuery(other) -> other
      other.isSuperQuery(this) -> this
      else -> throw IllegalArgumentException("Can't join these 2 queries: " + this + ", " + other)
    }
  }

  fun containsGeoHash(hash: Hash): Boolean {
    val hashStr = hash.getGeoHashString()
    return this.startValue <= hashStr && this.endValue > hashStr
  }

  fun getStartValue(): String {
    return this.startValue
  }

  fun getEndValue(): String {
    return this.endValue
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    val that = other as Query?

    if (endValue != that!!.endValue) return false
    return startValue == that.startValue

  }

  override fun hashCode(): Int {
    var result = startValue.hashCode()
    result = 31 * result + endValue.hashCode()
    return result
  }

  override fun toString(): String {
    return "Query{" +
      "startValue='" + startValue + '\''.toString() +
      ", endValue='" + endValue + '\''.toString() +
      '}'.toString()
  }
}