package com.ridrio.geo.utils

class Constants {
  companion object {
    // Length of a degree latitude at the equator
    const val METERS_PER_DEGREE_LATITUDE = 110574.0

    // The equatorial circumference of the earth in meters
    const val EARTH_MERIDIONAL_CIRCUMFERENCE = 40007860.0

    // The equatorial radius of the earth in meters
    const val EARTH_EQ_RADIUS = 6378137.0

    // The meridional radius of the earth in meters
    const val EARTH_POLAR_RADIUS = 6357852.3

    /* The following value assumes a polar radius of
     * r_p = 6356752.3
     * and an equatorial radius of
     * r_e = 6378137
     * The value is calculated as e2 == (r_e^2 - r_p^2)/(r_e^2)
     * Use exact value to avoid rounding errors
     */
    const val EARTH_E2 = 0.00669447819799

    // Cutoff for floating point calculations
    const val EPSILON = 1e-12
  }
}