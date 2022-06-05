package com.tuk.myapp

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tuk.myapp.databinding.ActivityMapBinding
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.activity_map.fab_main
import kotlinx.android.synthetic.main.popup_marketinfo.view.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MapActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private var currentSelectMarker: Marker? = null

    private lateinit var searchResult: SearchResultEntity

    private lateinit var locationManager: LocationManager // 안드로이드 에서 위치정보 불러올 때 관리해주는 유틸 클래스

    private lateinit var myLocationListener: MyLocationListener // 나의 위치를 불러올 리스너

    companion object {
        const val SEARCH_RESULT_EXTRA_KEY: String = "SEARCH_RESULT_EXTRA_KEY"
        const val CAMERA_ZOOM_LEVEL = 17f
        const val PERMISSION_REQUEST_CODE = 2021
    }

    //----------------------------------------------------------------

    var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var st: FirebaseStorage = FirebaseStorage.getInstance() // 파이어베이스 스토리지 객체 초기화

    private var isFabOpen = false // Fab 버튼 default는 닫혀있음

    private var isMarkerMake = false // Fab 버튼 default는 닫혀있음
    //데이터 저장 리스트
    var markerList = ArrayList<MyModel>()


    //인텐트
    lateinit var createIntent: Intent

    var current_position : LatLng? = null


    //----------------------------------------------------------------


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)



        job = Job()

        if (::searchResult.isInitialized.not()) {
            intent?.let {
                searchResult = it.getParcelableExtra<SearchResultEntity>(SEARCH_RESULT_EXTRA_KEY)
                    ?: throw Exception("데이터가 존재하지 않습니다.")
                setupGoogleMap()
                set_location.setText(searchResult.fullAddress+" ∨")
            }

        }

        bindViews()


        //----------------------------------------------------------------
        createIntent = Intent(applicationContext, CreateActivity::class.java)

        var current_marker : Marker? = null

        //마커(가게 정보리스트)저장 함수
        saveMarkerData()

        reloadMarker()

        //플로팅 버튼
        binding.fabMain.setOnClickListener {
            toggleFab()
        }

        // 플로팅 버튼 클릭 이벤트 - 마커 생성
        binding.markerCreate.setOnClickListener {
            if((isMarkerMake == false)&& (current_position != null)){
                current_marker = map.addMarker(
                    MarkerOptions()
                        .position(current_position!!)
                        .title(markerList[1].marketMenu)
                        .draggable(true)
                        .flat(true)
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
        binding.markerDelete.setOnClickListener {
            if(isMarkerMake == true) {
                current_marker?.remove()
                isMarkerMake = !isMarkerMake
            }
            else{
                Toast.makeText(this, "위치가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.markerRegister.setOnClickListener{
            if(isMarkerMake == true){
                startActivity(createIntent)
            }
            else {
                Toast.makeText(this, "위치가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.markerShow.setOnClickListener{
            reloadMarker()
            Toast.makeText(this,"맛집 불러오기 성공 !",Toast.LENGTH_SHORT).show()
        }

        //검색 화면으로 넘어가는 버튼
        binding.btnToResearch.setOnClickListener{
            var intentToResearch = Intent(applicationContext, MainActivity::class.java)
            startActivity(intentToResearch)
        }


        //----------------------------------------------------------------




    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun bindViews() = with(binding) {
        // 현재 위치 버튼 리스너
        currentLocationButton.setOnClickListener {
            binding.progressCircular.isVisible = true
            getMyLocation()

        }
    }


    private fun setupGoogleMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(binding.mapFragment.id) as SupportMapFragment
        mapFragment.getMapAsync(this) // callback 구현 (onMapReady)

        // 마커 데이터 보여주기

    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        currentSelectMarker = setupMarker(searchResult)

        currentSelectMarker?.showInfoWindow()

        map.setOnMapClickListener(this@MapActivity)
        map.setOnMarkerClickListener(this@MapActivity)
    }

    private fun setupMarker(searchResult: SearchResultEntity): Marker? {

        // 구글맵 전용 위도/경도 객체
        val positionLatLng = LatLng(
            searchResult.locationLatLng.latitude.toDouble(),
            searchResult.locationLatLng.longitude.toDouble()
        )

        // 구글맵 마커 객체 설정
        val markerOptions = MarkerOptions().apply {
            position(positionLatLng)
            title(searchResult.name)
            //  snippet(searchResult.fullAddress)
        }

        // 카메라 줌 설정
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(positionLatLng, CAMERA_ZOOM_LEVEL))

        return map.addMarker(markerOptions)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getMyLocation() {
        // 위치 매니저 초기화
        if (::locationManager.isInitialized.not()) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        // GPS 이용 가능한지
        val isGpsEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // 권한 얻기
        if (isGpsEnable) {
            when {
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) -> {
                    showPermissionContextPop()
                }

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED -> {
                    makeRequestAsync()
                }

                else -> {
                    setMyLocationListener()
                }
            }
        }
    }

    private fun showPermissionContextPop() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("내 위치를 불러오기위해 권한이 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                makeRequestAsync()
            }
            .create()
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun setMyLocationListener() {
        val minTime = 3000L // 현재 위치를 불러오는데 기다릴 최소 시간
        val minDistance = 100f // 최소 거리 허용

        // 로케이션 리스너 초기화
        if (::myLocationListener.isInitialized.not()) {
            myLocationListener = MyLocationListener()
        }

        // 현재 위치 업데이트 요청
        with(locationManager) {
            requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,
                minDistance,
                myLocationListener
            )
            requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTime,
                minDistance,
                myLocationListener
            )
        }
    }

    private fun onCurrentLocationChanged(locationLatLngEntity: LocationLatLngEntity) {
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    locationLatLngEntity.latitude.toDouble(),
                    locationLatLngEntity.longitude.toDouble()
                ), CAMERA_ZOOM_LEVEL
            )
        )

        loadReverseGeoInformation(locationLatLngEntity)
        removeLocationListener() // 위치 불러온 경우 더이상 리스너가 필요 없으므로 제거
    }

    private fun loadReverseGeoInformation(locationLatLngEntity: LocationLatLngEntity) {
        // 코루틴 사용
        launch(coroutineContext) {
            try {
                binding.progressCircular.isVisible = true

                // IO 스레드에서 위치 정보를 받아옴
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.apiService.getReverseGeoCode(
                        lat = locationLatLngEntity.latitude.toDouble(),
                        lon = locationLatLngEntity.longitude.toDouble()
                    )
                    if (response.isSuccessful) {

                        val body = response.body()

                        // 응답 성공한 경우 UI 스레드에서 처리
                        withContext(Dispatchers.Main) {
                            Log.e("list", body.toString())
                            body?.let {
                                currentSelectMarker = setupMarker(
                                    SearchResultEntity(
                                        fullAddress = it.addressInfo.fullAddress ?: "주소 정보 없음",
                                        name = "내 위치",
                                        locationLatLng = locationLatLngEntity
                                    )
                                )
                                // 마커 보여주기
                                currentSelectMarker?.showInfoWindow()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MapActivity, "검색하는 과정에서 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressCircular.isVisible = false
            }
        }
    }

    private fun removeLocationListener() {
        if (::locationManager.isInitialized && ::myLocationListener.isInitialized) {
            locationManager.removeUpdates(myLocationListener) // myLocationListener 를 업데이트 대상에서 지워줌
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setMyLocationListener()
                } else {
                    Toast.makeText(this, "권한을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
                    binding.progressCircular.isVisible = false
                }
            }
        }
    }

    private fun makeRequestAsync() {
        // 퍼미션 요청 작업. 아래 작업은 비동기로 이루어짐
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }


    //----------------------------------------------------------------
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
            this@MapActivity.markerList.add(myModel)
        }
    }


    private fun toggleFab() {
        if (isFabOpen) {
            ObjectAnimator.ofFloat(binding.markerCreate, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(binding.markerDelete, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(binding.markerRegister, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(binding.markerShow, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(fab_main, View.ROTATION, 45f, 0f).apply { start() }
        } else { // 플로팅 액션 버튼 열기 - 닫혀있는 플로팅 버튼 꺼내는 애니메이션
            ObjectAnimator.ofFloat(binding.markerCreate, "translationY", -640f).apply { start() }
            ObjectAnimator.ofFloat(binding.markerDelete, "translationY", -480f).apply { start() }
            ObjectAnimator.ofFloat(binding.markerRegister, "translationY", -320f).apply { start() }
            ObjectAnimator.ofFloat(binding.markerShow, "translationY", -160f).apply { start() }
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
                        map.addMarker(
                            MarkerOptions()
                                .position(dbMarkerPosition!!)
                                .title(document.get("storeName").toString())
                                .draggable(true)
                                .flat(true)
                        )
                    }
                }
            }
    }

    //----------------------------------------------------------------

    inner class MyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            // 현재 위치 콜백
            val locationLatLngEntity = LocationLatLngEntity(
                location.latitude.toFloat(),
                location.longitude.toFloat()
            )

            onCurrentLocationChanged(locationLatLngEntity)
        }

    }

}