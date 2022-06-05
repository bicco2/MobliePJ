package com.tuk.myapp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize // 인텐트로 넘길라고
data class SearchResultEntity(
    val fullAddress: String,
    val name: String,
    val locationLatLng: LocationLatLngEntity
) : Parcelable