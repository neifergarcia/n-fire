package com.ridrio.login

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException


public class EneLogin(listener: EneLoginListener) {

  private val loginListener: EneLoginListener = listener

  init {
    authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      val currentUser = firebaseAuth.currentUser
      loginListener.onAuthStateChange(currentUser != null)
    }
  }

  fun loginWithEmailAndPassword(email: String, password: String) {
    firebaseAuth.signInWithEmailAndPassword(email, password)
      .addOnCompleteListener { task ->
        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful)
        if (!task.isSuccessful) {
          try {
            throw task.exception!!
          } catch (e: FirebaseAuthInvalidUserException) {
            loginListener.onUserNotExist()
          } catch (e: FirebaseAuthInvalidCredentialsException) {
            loginListener.onFailed()
          } catch (e: Exception) {
            e.printStackTrace()
            loginListener.onUserNotExist()
          }
          logout()
        } else {
          loginListener.onSuccess(task.result.user.uid)
        }
      }
  }

  companion object {
    private val TAG = "EneLogin"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    fun addAuthStateListener() {
      firebaseAuth.addAuthStateListener(authStateListener)
    }

    fun removeAuthStateListener() {
      firebaseAuth.removeAuthStateListener(authStateListener)
    }

    fun uid(): String {
      if (firebaseAuth.currentUser != null) {
        return firebaseAuth.currentUser!!.uid
      }
      return ""
    }

    fun logout() {
      firebaseAuth.signOut()
      Log.d(TAG, "Logout ...")
    }
  }

  interface EneLoginListener {
    fun onFailed()
    fun onUserNotExist()
    fun onSuccess(uid: String)
    fun onAuthStateChange(success: Boolean)
  }
}