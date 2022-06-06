package com.tuk.myapp

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.popup_marketinfo.view.*


class MainActivity2 : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap

    lateinit var btn_market_create : FloatingActionButton
    lateinit var btn_market_delete : FloatingActionButton
    lateinit var btn_market_register : FloatingActionButton
    lateinit var btn_market_show : FloatingActionButton
    lateinit var fab_main : FloatingActionButton
    lateinit var fv_button: FloatingActionButton
    lateinit var btnToResearch : Button

    private lateinit var listView: ListView
    private lateinit var editTextName: EditText
    private lateinit var editTextNumber: EditText
    private lateinit var buttonAdd: Button
    private var dataArrayList: ArrayList<String>? = null
    private var adapter: ArrayAdapter<String>? = null
    private lateinit var prefe1: SharedPreferences


    var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var st: FirebaseStorage = FirebaseStorage.getInstance() // 파이어베이스 스토리지 객체 초기화


    private var isFabOpen = false // Fab 버튼 default는 닫혀있음

    private var isMarkerMake = false // Fab 버튼 default는 닫혀있음

    private var isPositionMake = false // Fab 버튼 default는 닫혀있음

    //데이터 저장 리스트
    var markerList = ArrayList<MyModel>()


    //인텐트
    lateinit var createIntent: Intent
    lateinit var fvIntent: Intent


    var current_position : LatLng? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        createIntent = Intent(applicationContext, CreateActivity::class.java)


        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
        mapFragment.getMapAsync(this)

        var current_marker : Marker? = null


        btn_market_create = findViewById(R.id.marker_create)
        btn_market_delete = findViewById(R.id.marker_delete)
        btn_market_register = findViewById(R.id.marker_register)
        btn_market_show = findViewById(R.id.marker_show)

        fab_main = findViewById(R.id.fab_main)
        fv_button=findViewById(R.id.fab_fv)
        btnToResearch = findViewById(R.id.btnToResearch)

//        editTextName = findViewById(R.id.edtTxt_addName_actvtMain)

        //마커(가게 정보리스트)저장 함수
        saveMarkerData()

        reloadMarker()

        //플로팅 버튼
        fab_main.setOnClickListener {
            toggleFab()
        }
        fv_button.setOnClickListener {
            fvIntent= Intent(this,MainActivity3::class.java)
            startActivity(fvIntent)

        }


        // 플로팅 버튼 클릭 이벤트 - 마커 생성
        btn_market_create.setOnClickListener {
            if((isMarkerMake == false)&& (current_position != null)){
                current_marker = mMap.addMarker(
                    MarkerOptions()
                        .position(current_position!!)
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1))
                )!!
                isMarkerMake = !isMarkerMake
                //인텐트 데이터 넘기기(위치 정보)
                createIntent.putExtra("lat", current_position!!.latitude.toString())
                createIntent.putExtra("lon", current_position!!.longitude.toString())
            }
            else {
                Toast.makeText(this, "이미 위치가 설정되어있거나 위치 클릭을 하지 않았습니다.", Toast.LENGTH_SHORT).show()
            }

        }

        // 플로팅 버튼 클릭 이벤트 - 마커 삭제
        btn_market_delete.setOnClickListener {
            if(isMarkerMake == true) {
                current_marker?.remove()
                isMarkerMake = !isMarkerMake
            }
            else{
                Toast.makeText(this, "위치가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btn_market_register.setOnClickListener{
            if(isMarkerMake == true){
                startActivity(createIntent)
            }
            else {
                Toast.makeText(this, "위치가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btn_market_show.setOnClickListener{
            reloadMarker()
            Toast.makeText(this,"맛집 불러오기 성공 !",Toast.LENGTH_SHORT).show()
        }

        //검색 화면으로 넘어가는 버튼
        btnToResearch.setOnClickListener{
            var intentToResearch = Intent(applicationContext, MainActivity::class.java)
            startActivity(intentToResearch)
        }



    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        val marker = LatLng(35.241615, 128.695587)
        mMap.addMarker(
            MarkerOptions()
                .position(marker)
                .flat(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1))
        )

        mMap.setOnMapClickListener(this)
        mMap.setOnMarkerClickListener(this)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(marker))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker, 16F))

    }
    override fun onMapClick(point: LatLng) {
        Toast.makeText(this, "위치는?$point", Toast.LENGTH_SHORT).show()
        current_position = point
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewPopup = inflater.inflate(R.layout.popup_marketinfo, null)


            db.collection("Stores").document(p0.position.latitude.toString()).get()
                .addOnSuccessListener { document ->
                    if (document.data != null) {
                        viewPopup.store_information.setText(document.get("storeInfo") as String?)
                        viewPopup.store_title_name.setText(document.get("storeName") as String?)
                        viewPopup.store_payType.setText(document.get("storePayType") as String?)
                        viewPopup.store_type.setText(document.get("storeType") as String?)
                        viewPopup.day_text.setText(document.get("storeClosedDay") as String?)
                        viewPopup.menu_name.setText(document.get("storeMenu") as String?)


                        // storeName 에  ex. test111 를 갖고와야함 즉, 가게 이름 이 storeName임
                        var imgFileName = document.get("storeName").toString() + "_img"
                        var stRef = st.reference.child(document.get("storeName").toString()).child(imgFileName) // 스토리지 참조
                        stRef.downloadUrl.addOnCompleteListener(){
                            Glide.with(this /* context */)
                                .load(it.result)
                                .into(viewPopup.store_picture)
                        }




                        Toast.makeText(this,"조회완료",Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(this,"가게를 찾을 수 없음",Toast.LENGTH_SHORT).show()
                    }
                }




        val alertDialog = AlertDialog.Builder(this).create()

        alertDialog.setView(viewPopup)
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        alertDialog.show()
        alertDialog.window!!.setLayout(800,1400)

        viewPopup.favorite_check.setOnClickListener {
            fvIntent= Intent(this,MainActivity3::class.java)
            fvIntent.putExtra("title",viewPopup.store_title_name.text.toString())
            fvIntent.putExtra("content",viewPopup.store_information.text.toString())
            startActivity(fvIntent)
//            Toast.makeText(this, "저장되었습니다.",Toast.LENGTH_SHORT).show()
        }

        viewPopup.close_button.setOnClickListener {
            alertDialog.dismiss()
        }

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
            this@MainActivity2.markerList.add(myModel)
        }
    }



    private fun toggleFab() {
        // 플로팅 액션 버튼 닫기 - 열려있는 플로팅 버튼 집어넣는 애니메이션
        if (isFabOpen) {
            ObjectAnimator.ofFloat(btn_market_create, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_delete, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_register, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_show, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(fab_main, View.ROTATION, 45f, 0f).apply { start() }
        } else { // 플로팅 액션 버튼 열기 - 닫혀있는 플로팅 버튼 꺼내는 애니메이션
            ObjectAnimator.ofFloat(btn_market_create, "translationY", -640f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_delete, "translationY", -480f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_register, "translationY", -320f).apply { start() }
            ObjectAnimator.ofFloat(btn_market_show, "translationY", -160f).apply { start() }
            ObjectAnimator.ofFloat(fab_main, View.ROTATION, 0f, 45f).apply { start() }
        }

        isFabOpen = !isFabOpen

    }

    private fun reloadMarker(){
        db.collection("Stores").get()
            .addOnSuccessListener { documents ->
                for(document in documents){
                    if (document.data != null) {
                        val dbMarkerPosition : LatLng = LatLng(document.get("storeLatValue").toString().toDouble(), document.get("storeLonValue").toString().toDouble())
                        mMap.addMarker(
                            MarkerOptions()
                                .position(dbMarkerPosition!!)
                                .title(document.get("storeName").toString())
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1))
                        )
                    }
                }
            }
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