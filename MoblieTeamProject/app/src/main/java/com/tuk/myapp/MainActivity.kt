package com.tuk.myapp

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Transformations.map
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap

    lateinit var btn_market_create : FloatingActionButton
    lateinit var btn_market_delete : FloatingActionButton
    lateinit var fab_main : FloatingActionButton
    private var isFabOpen = false // Fab 버튼 default는 닫혀있음

    //데이터 저장 리스트
    var markerList = ArrayList<MyModel>()




    var current_position : LatLng? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
        mapFragment.getMapAsync(this)

        var current_marker : Marker? = null


        btn_market_create = findViewById(R.id.marker_create)
        btn_market_delete = findViewById(R.id.marker_delete)
        fab_main = findViewById(R.id.fab_main)

        //마커(가게 정보리스트)저장 함수
        saveMarkerData()



        //플로팅 버튼
        fab_main.setOnClickListener {
            toggleFab()
        }

        // 플로팅 버튼 클릭 이벤트 - 캡처
        btn_market_create.setOnClickListener {
            current_marker = mMap.addMarker(
                MarkerOptions()
                    .position(current_position!!)
                    .title(markerList[1].marketMenu)
                    .draggable(true)
                    .flat(true)
            )!!

        }

        // 플로팅 버튼 클릭 이벤트 - 공유
        btn_market_delete.setOnClickListener {
            current_marker?.remove()
        }




//        add_marker_btn.setOnClickListener {
//
//                current_marker = mMap.addMarker(
//                    MarkerOptions()
//                        .position(current_position!!)
//                        .title(markerList[1].marketMenu)
//                        .draggable(true)
//                        .flat(true)
//                )!!
//
////            if(current_marker != null){
////                current_marker?.remove()
////            }
//        }
//
//        add_marker_btn2.setOnClickListener{
//            current_marker?.remove()
//        }
////        mMap.setOnMapClickListener {
////
////        }

    }

    override fun onMapReady(googleMap: GoogleMap) {

        //여기 안에 myModel리스트 안에 있는 데이터를 가져와서 마커 하나 하나 생성 해줘야함
//        for(i in 0..MarkerList.maxIndex-1){
//            mMap.addMarker(
//                MarkerOptions()
//                    .position(MarkerList[i].position)
//                    .title(MarkerList[i].title)
//                    .draggable(true)
//                    .flat(true)
//            )
//        }



        mMap = googleMap
        val marker = LatLng(35.241615, 128.695587)
        mMap.addMarker(
            MarkerOptions()
                .position(marker)
                .title("우리집")
                .draggable(true)
                .flat(true)
        )


        mMap.setOnMapClickListener(this)
        mMap.setOnMarkerClickListener(this)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(marker))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker, 16F))

    }
    override fun onMapClick(point: LatLng) {
        Toast.makeText(this, "위치는?$point", Toast.LENGTH_SHORT).show()
        current_position = point


    //마커 리스트 받아와서 생성
//        MarkerList
//        for(i in 0..10){
//            if(MarkerList[i].position == postion){
//                currnetMarker = i
//            }
//        }
//
//        MarkerList[currentMarker].제목
//        MarkerList[currentMarker].가게이름
//        MarkerList[currentMarker].영업시간 등등



    }

    override fun onMarkerClick(p0: Marker): Boolean {

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewPopup = inflater.inflate(R.layout.popup_marketinfo, null)

        var title_tv = viewPopup.findViewById<TextView>(R.id.Title_tv1)

        title_tv.setText(p0.title)
        //⭐️여기서 이 위치값을 키로 해서 데이터베이스에 저장된 맛집 정보(가게이름, 가게소개, 메뉴, 영업시간 등등) 을 데이터 모델로 받아온다 .

        val alertDialog = AlertDialog.Builder(this)
            .create()

        alertDialog.setView(viewPopup)
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        alertDialog.show()
        alertDialog.window!!.setLayout(500,500)

        return true
    }

    //데이터 저장 메소드, myModel 을 변경해줘야함
    private fun saveMarkerData(){
        //이거 반복해야함 최대 인덱스는 파이어베이스내에 저장된 데이터의 수를 가져와서 나중에 넣으면 될 듯 함
        for (i in 0..10) {
            val myModel = MyModel(
                positionInfo = "$i 번째 위치",
                marketName = "$i 번째 이름",
                marketInfo = "$i 번째 정보",
                marketMenu = "$i 번째 메뉴",
                marketTime = "$i 번째 시간"
            )
            this@MainActivity.markerList.add(myModel)
        }
    }



    private fun toggleFab() {
        Toast.makeText(this, "메인 버튼 클릭!", Toast.LENGTH_SHORT).show()
        // 플로팅 액션 버튼 닫기 - 열려있는 플로팅 버튼 집어넣는 애니메이션
        if (isFabOpen) {
            ObjectAnimator.ofFloat(btn_market_create, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_delete, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(fab_main, View.ROTATION, 45f, 0f).apply { start() }
        } else { // 플로팅 액션 버튼 열기 - 닫혀있는 플로팅 버튼 꺼내는 애니메이션
            ObjectAnimator.ofFloat(btn_market_create, "translationY", -360f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_delete, "translationY", -180f).apply { start() }
            ObjectAnimator.ofFloat(fab_main, View.ROTATION, 0f, 45f).apply { start() }
        }

        isFabOpen = !isFabOpen

    }

//    private fun moveCamera(map: GoogleMap, marker: Marker) {
//        map.animateCamera(
//            CameraUpdateFactory.newLatLngZoom(
//                LatLng(
//                    marker.position.latitude,
//                    marker.position.longitude
//                ), 16f
//            )
//        )
//        marker.showInfoWindow()
//    }


}