package com.tuk.myapp

import android.util.Log

class MyModel(var positionInfo: String? = null, var marketName: String? = null, var marketInfo: String? = null , var marketMenu : String? = null, var marketTime: String? = null) {

    val TAG: String = "로그"

    // 기본 생성자
    init {
        Log.d(TAG, "MyModel - init() called")
    }

}
