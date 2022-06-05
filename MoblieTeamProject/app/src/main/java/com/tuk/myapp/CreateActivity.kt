package com.tuk.myapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_create.*

class CreateActivity : AppCompatActivity() {

    //파이어베이스 이용 등록
    var REQUEST_GET_IMAGE = 105
    lateinit var currentImage : ImageView // 현재 처리중 이미지
    var imgUri : Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)




        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1) // 권한 허용 요청

        var storeAdr: TextView = findViewById<TextView>(R.id.storeAdr) // 가게 주소

        var storeNameCount: TextView = findViewById<TextView>(R.id.storeNameCount) // 가게 이름 길이 카운트
        var storeName: EditText = findViewById<EditText>(R.id.storeName) // 가게 이름

        var storeInfoCount: TextView = findViewById<TextView>(R.id.storeInfoCount) // 가게 설명 카운트
        var storeInfo: EditText = findViewById<EditText>(R.id.storeInfo) // 가게 설명

        var storeMenuCount: TextView = findViewById<TextView>(R.id.storeMenuCount) // 가게 메뉴 카운트
        var storeMenu: EditText = findViewById<EditText>(R.id.storeMenu) // 가게 메뉴

        var storeImage : ImageView = findViewById<ImageView>(R.id.storeImage) // 가게 이미지 사진
        var storeImageAddButton : ImageButton = findViewById<ImageButton>(R.id.storeImageAdd) // 가게 이미지 추가 버튼

        var radioGroup1: RadioGroup = findViewById<RadioGroup>(R.id.radioGroup1) // 가게 유형 라디오 그룹
        var storeType: String = "건물"

        var radioGroup2: RadioGroup = findViewById<RadioGroup>(R.id.radioGroup2) // 결제 유형 라디오 그룹
        var storePayType: String = "카드"

        var radioGroup3: RadioGroup = findViewById<RadioGroup>(R.id.radioGroup3) // 가게 휴무일 라디오 그룹
        var storeClosed: String = "일요일"

        var addCancel: Button = findViewById<Button>(R.id.addCancel) // 취소 버튼
        var addCheck: Button = findViewById<Button>(R.id.addCheck) // 등록 버튼

        var db: FirebaseFirestore = FirebaseFirestore.getInstance() // 파이어베이스 파이어스토어 객체 초기화
        var st: FirebaseStorage = FirebaseStorage.getInstance() // 파이어베이스 스토리지 객체 초기화



        data class Store(
            // 가게 정보를 담기위한 클래스
            var StoreAddress: String? = null,
            var StoreName: String? = null,
            var StoreMenu: String? = null,
            var StoreInfo: String? = null,
            var StoreType: String? = null,
            var StorePayType: String? = null,
            var StoreLatValue: String? = null,
            var StoreLonValue: String? = null,
            var StoreClosedDay: String? = null,
        )


        val latlngThing = intent.getStringExtra("lat").toString()
        val latValue  = intent.getStringExtra("lat").toString()
        val lonValue   = intent.getStringExtra("lon").toString()

        ////////////////////////////// 함수 //////////////////////////////

        fun writeData(store: Store? = null) { // 파이어스토어 데이터 쓰기 함수
            if (store != null) {
                db.collection("Stores").document(latlngThing).set(store)
                Toast.makeText(this, "등록완료", Toast.LENGTH_SHORT).show()
            }
        }

        fun writeImage(store: Store) { // 스토리지 이미지 업로드 함수
            var imgFileName = store.StoreName.toString() + "_img"
            var stRef: StorageReference =
                st.reference.child(store.StoreName.toString()).child(imgFileName) // 스토리지 참조
            stRef.putFile(imgUri!!)
        }

        ////////////////////////////// 리스너 //////////////////////////////


        storeName.addTextChangedListener() { // 가게 이름 글자 수 카운트 리스너
            val input: String = storeName.getText().toString()
            storeNameCount.text = input.length.toString() + " / 20"
        }

        storeInfo.addTextChangedListener() { // 가게 설명 글자 수 카운트 리스너
            val input: String = storeInfo.getText().toString()
            storeInfoCount.text = input.length.toString() + " / 50"
        }

        storeMenu.addTextChangedListener() { // 가게 이름 글자 수 카운트 리스너
            val input: String = storeMenu.getText().toString()
            storeMenuCount.text = input.length.toString() + " / 30"
        }



        storeImageAddButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_GET_IMAGE)
            currentImage = storeImage
        }

        radioGroup1.setOnCheckedChangeListener { Group, checkedId -> // 가게 유형 라디오그룹 리스너
            when (checkedId) {
                R.id.storeType1 -> storeType = "건물"
                R.id.storeType2 -> storeType = "포장마차"
            }
        }

        radioGroup2.setOnCheckedChangeListener { Group, checkedId -> // 결제 유형 라디오그룹 리스너
            when (checkedId) {
                R.id.storePayType1 -> storeType = "카드"
                R.id.storePayType1 -> storeType = "현금"
                R.id.storePayType3 -> storeType = "카드, 현금"
            }
        }

        radioGroup3.setOnCheckedChangeListener { Group, checkedId -> // 메뉴 라디오그룹 리스너
            when (checkedId) {
                R.id.storeClosedSun -> storeClosed = "일요일"
                R.id.storeClosedMon -> storeClosed = "월요일"
                R.id.storeClosedTue -> storeClosed = "화요일"
                R.id.storeClosedWed -> storeClosed = "수요일"
                R.id.storeClosedThu -> storeClosed = "목요일"
                R.id.storeClosedFri -> storeClosed = "금요일"
                R.id.storeClosedSat -> storeClosed = "토요일"
            }
        }

        addCheck.setOnClickListener() { // 등록 버튼 리스너
            var store = Store()
            if (storeName.text.toString().length == 0) { // 가게 이름이 없을 시
                Toast.makeText(this, "가게 이름을 입력해 주세요", Toast.LENGTH_SHORT).show()
            }

            else if (storeMenu.text.toString().length == 0) { // 가게 메뉴가 없을 시
                Toast.makeText(this, "가게 메뉴를 입력해 주세요", Toast.LENGTH_SHORT).show()
            }

            else if (storeInfo.text.toString().length == 0) { // 가게 설명이 없을 시
                Toast.makeText(this, "가게 설명을 입력해 주세요", Toast.LENGTH_SHORT).show()
            }

            else if (imgUri == null){
                Toast.makeText(this, "이미지를 등록해 주세요", Toast.LENGTH_SHORT).show()
            }
            else {
                store.StoreAddress = storeAdr.text.toString()
                store.StoreName = storeName.text.toString()
                store.StoreMenu = storeMenu.text.toString()
                store.StoreInfo = storeInfo.text.toString()
                store.StoreType = storeType
                store.StorePayType = storePayType
                store.StoreLatValue = latValue
                store.StoreLonValue = lonValue
                store.StoreClosedDay = storeClosed
                writeData(store)
                writeImage(store)
            }
        }
//

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                REQUEST_GET_IMAGE -> {
                    try{
                        var currentImageUri : Uri? = data?.data
                        currentImage.setImageURI(currentImageUri)
                        imgUri = currentImageUri
                    }catch (e:Exception){
                        Toast.makeText(this,"이미지를 가져올 수 없음",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}


