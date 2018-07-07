package com.ene.core

import android.os.Handler
import android.os.Looper

class EventRaiser {

  private val mainThreadHandler = Handler(Looper.getMainLooper())

  fun raiseEvent(runnable: Runnable) {
    mainThreadHandler.post(runnable)
  }
}