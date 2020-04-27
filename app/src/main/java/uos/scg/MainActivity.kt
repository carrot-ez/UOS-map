package uos.scg

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.os.Build
import android.util.Base64
import android.util.Base64.NO_WRAP
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kakao.util.maps.helper.Utility.getPackageInfo
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import net.daum.mf.map.api.*
import net.daum.mf.map.api.MapPOIItem
import androidx.drawerlayout.widget.DrawerLayout
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible


class MainActivity : AppCompatActivity(), MapView.POIItemEventListener, MapView.MapViewEventListener, MapView.CurrentLocationEventListener{
    /* class 변수 선언 */
    val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 6245
    val REQUEST_CODE = 1001
    lateinit var mapView : MapView
    var isTracking = false

    /* Drawer Layout을 위한 변수 */
    lateinit var uosDrawerLayout : DrawerLayout
    lateinit var uosDrawerView : View
    val buildingList = ArrayList<BuildingInfo>()
    var searchingList = ArrayList<String>()
    /* class 변수 선언 */

    class CustomCalloutBalloonAdapter(context: Context) : CalloutBalloonAdapter {
        var mCalloutBalloon : View? = null

        init {
            mCalloutBalloon = LayoutInflater.from(context).inflate(R.layout.custom_callout_balloon, null)
        }


        override fun getCalloutBalloon(poiItem: MapPOIItem) : View? {
            (mCalloutBalloon?.findViewById(R.id.building_img) as ImageView).setImageResource(poiItem.userObject as Int)
            (mCalloutBalloon?.findViewById(R.id.building_name) as TextView).text = poiItem.itemName
            (mCalloutBalloon?.findViewById(R.id.building_num) as TextView).text = poiItem.tag.toString()
            return mCalloutBalloon
        }

        override fun getPressedCalloutBalloon(p0: MapPOIItem?): View? {
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* 테스트용 activity */
//        val intent = Intent(this, TestActivity::class.java)
//        startActivity(intent)

        // permission check
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
        )

        /* late init */
        uosDrawerLayout = findViewById<DrawerLayout>(R.id.uos_drawer_layout)
        uosDrawerView = findViewById<View>(R.id.uos_drawer)

        mapView = MapView(this)
        mapView.setPOIItemEventListener(this) // POI Event를 처리하기 위한 리스너 등록

        val mapViewContainer = findViewById(R.id.map_view) as ViewGroup
        mapViewContainer.addView(mapView)
        /* late init */

        /* set camera uos center point */
        setCenterView()

        // set zoom level
        mapView.setZoomLevel(2, true)


        /* 현재위치를 표시하고, 트래킹 모드를 켜고 끄는 버튼 */
        val cPositionButton = findViewById<ImageButton>(R.id.navigationBtn)
        cPositionButton.setOnClickListener{
            if(isTracking) {
                mapView.currentLocationTrackingMode =
                    MapView.CurrentLocationTrackingMode.TrackingModeOff
                mapView.setShowCurrentLocationMarker(false)
                isTracking = false
            }
            else {
                mapView.currentLocationTrackingMode =
                    MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading
                mapView.setShowCurrentLocationMarker(true)
                isTracking = true
            }
        }

        showAllBuildings()

        /* init function */
        initListView()
        initBuildingList()

        /* ListView로 전환하는 버튼 */
        val listViewButton = findViewById<ImageButton>(R.id.menuBtn)
        listViewButton.setOnClickListener{
            uosDrawerLayout.openDrawer(uosDrawerView)

            /* @Depressed 이전 버전에서 사용했던 기능 */
            /* list view로 intent
            val intent = Intent(this, ListViewActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
            */
        }

        /* myUos로 이동하는 버튼 */
        val myUosButton = findViewById<ImageButton>(R.id.my_uos_button)
        myUosButton.setOnClickListener {
            val myUosPackageName = "com.project.s.school"
            intentOtherUosApp(myUosPackageName)
        }

        /* 중앙 도서관 어플로 이동하는 버튼 */
        val uosLibraryButton = findViewById<ImageButton>(R.id.uos_library_button)
        uosLibraryButton.setOnClickListener {
            val uosLibraryPackageName = "mmm.slpck.uos"
            intentOtherUosApp(uosLibraryPackageName)
        }

        /* search list view 설정 */
        val searchListView = findViewById<ListView>(R.id.search_item)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, searchingList)
        searchListView.adapter = arrayAdapter
        searchListView.isVisible = false

        /* Search된 ListVIew의 item을 클릭했을때 반응하는 EventListener */
        searchListView.setOnItemClickListener { parent, view, position, id ->
            val itemName = parent.getItemAtPosition(position)
            for(item in buildingList) {
                if(item.buildingName.equals(itemName)) {
                    showAllBuildings()
                    searchListView.isVisible = false
                    mapView.selectPOIItem(mapView.findPOIItemByTag(item.buildingNum), true)
                    break
                }
            }
        }

        /* search view 설정 */
        val searchView = findViewById<SearchView>(R.id.search_map)

        /* search view event listener */
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            /* 새로운 단어가 입력되었을때 호출 */
            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText!!.length == 0) {
                    searchListView.isVisible = false
                }
                else {
                    searchListView.isVisible = true
                    search(newText, searchingList)
                    arrayAdapter.notifyDataSetChanged()
                }
                return true
            }

            /* 검색하기를 눌렀을때 호출 */
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchListView.isVisible = true
                search(query, searchingList)
                arrayAdapter.notifyDataSetChanged()
                return true
            }
        })
    }
    /* building list와 query를 비교하여 일치하는 것만 list에 담는 메소드 */
    fun search(query : String?, list : ArrayList<String>){
        list.clear()
        for(item in buildingList) {
            if(item.buildingName.toLowerCase().contains(query!!.toLowerCase())) {
                list.add(item.buildingName)
            }
        }
    }

    fun initBuildingList() {
        buildingList.add(BuildingInfo(1, "전농관"))
        buildingList.add(BuildingInfo(2, "제1공학관"))
        buildingList.add(BuildingInfo(3, "건설공학관"))
        buildingList.add(BuildingInfo(4, "창공관"))
        buildingList.add(BuildingInfo(5, "인문학관"))
        buildingList.add(BuildingInfo(6, "배봉관"))
        buildingList.add(BuildingInfo(7, "대학본부"))
        buildingList.add(BuildingInfo(8, "자연과학관"))
        buildingList.add(BuildingInfo(10, "경농관"))
        buildingList.add(BuildingInfo(11, "제2공학관"))
        buildingList.add(BuildingInfo(12, "학생회관"))
        buildingList.add(BuildingInfo(13, "학군단"))
        buildingList.add(BuildingInfo(14, "과학기술관"))
        buildingList.add(BuildingInfo(15, "21세기관"))
        buildingList.add(BuildingInfo(16, "조형관"))
        buildingList.add(BuildingInfo(18, "자작마루"))
        buildingList.add(BuildingInfo(19, "정보기술관"))
        buildingList.add(BuildingInfo(20, "법학관"))
        buildingList.add(BuildingInfo(21, "중앙도서관"))
        buildingList.add(BuildingInfo(22, "생활관"))
        buildingList.add(BuildingInfo(23, "건축구조실험동"))
        buildingList.add(BuildingInfo(24, "토목구조실험동"))
        buildingList.add(BuildingInfo(25, "미디어관"))
        buildingList.add(BuildingInfo(27, "대강당"))
        buildingList.add(BuildingInfo(28, "운동장"))
        buildingList.add(BuildingInfo(29, "박물관"))
        buildingList.add(BuildingInfo(32, "웰니스센터"))
        buildingList.add(BuildingInfo(33, "미래관"))
        buildingList.add(BuildingInfo(34, "국제학사"))
        buildingList.add(BuildingInfo(35, "음악관"))
        buildingList.add(BuildingInfo(36, "어린이집"))
        buildingList.add(BuildingInfo(37, "100주년 기념관"))
        buildingList.add(BuildingInfo(41, "실외 테니스장"))
        buildingList.add(BuildingInfo(81, "자동화온실"))
        buildingList.add(BuildingInfo(-1, "하늘못"))
    }

    /* packageName이 존재하는지 확인하는 메소드 */
    fun getPackageList(packageName : String) : Boolean {
        var isExist = false
        val pkgMgr = packageManager
        val mApps: List<ResolveInfo>
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0)

        try {
            for (i in mApps.indices) {
                if (mApps[i].activityInfo.packageName.startsWith(packageName)) {
                    isExist = true
                    break
                }
            }
        } catch (e: Exception) {
            isExist = false
        }
        return isExist
    }

    /* packageName을 가지는 App을 실행하는 메소드 */
    fun intentOtherUosApp(packageName : String) {
        if(getPackageList(packageName)) {
            val myUosIntent = packageManager.getLaunchIntentForPackage(packageName)
            myUosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(myUosIntent)
        }
        else {
            val url = "market://details?id=" + packageName
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(i)
        }
    }

    /* 카메라를 uos중앙으로 위치 */
    fun setCenterView() {
        /* uos map center point */
        val uos_x = 37.584142
        val uos_y = 127.058666

        // set center point
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(uos_x, uos_y), true)
        mapView.setZoomLevel(2, true)
    }

    /* 모든 건물 마커를 화면에 출력 */
    fun showAllBuildings() {
        /* Marker와 callout balloon이 충돌하지 않게 하기 위한 함수 */
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(CustomCalloutBalloonAdapter(this))

        val building_lat1 = 37.583597
        val building_long1 = 127.056548

        val locate_building1 = MapPoint.mapPointWithGeoCoord(building_lat1, building_long1)

        val building_marker1 = MapPOIItem()
        building_marker1.itemName = "전농관"
        building_marker1.tag = 1
        building_marker1.mapPoint = locate_building1

        building_marker1.markerType = MapPOIItem.MarkerType.BluePin
        building_marker1.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker1.userObject = R.drawable.building_01

        mapView.addPOIItem(building_marker1)

        val building_lat2 = 37.584818
        val building_long2 = 127.05847

        val locate_building2 = MapPoint.mapPointWithGeoCoord(building_lat2, building_long2)

        val building_marker2 = MapPOIItem()
        building_marker2.itemName = "제 1공학관"
        building_marker2.tag = 2
        building_marker2.mapPoint = locate_building2

        building_marker2.markerType = MapPOIItem.MarkerType.BluePin
        building_marker2.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker2.userObject = R.drawable.building_02

        mapView.addPOIItem(building_marker2)

        val building_lat3 = 37.583838
        val building_long3 = 127.05791

        val locate_building3 = MapPoint.mapPointWithGeoCoord(building_lat3, building_long3)

        val building_marker3 = MapPOIItem()
        building_marker3.itemName = "건설공학관"
        building_marker3.tag = 3
        building_marker3.mapPoint = locate_building3

        building_marker3.markerType = MapPOIItem.MarkerType.BluePin
        building_marker3.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker3.userObject = R.drawable.building_03

        mapView.addPOIItem(building_marker3)

        val building_lat4 = 37.584597
        val building_long4 = 127.060698

        val locate_building4 = MapPoint.mapPointWithGeoCoord(building_lat4, building_long4)

        val building_marker4 = MapPOIItem()
        building_marker4.itemName = "창공관"
        building_marker4.tag = 4
        building_marker4.mapPoint = locate_building4

        building_marker4.markerType = MapPOIItem.MarkerType.BluePin
        building_marker4.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker4.userObject = R.drawable.building_04

        mapView.addPOIItem(building_marker4)

        val building_lat5 = 37.583766
        val building_long5 = 127.061073

        val locate_building5 = MapPoint.mapPointWithGeoCoord(building_lat5, building_long5)

        val building_marker5 = MapPOIItem()
        building_marker5.itemName = "인문학관"
        building_marker5.tag = 5
        building_marker5.mapPoint = locate_building5

        building_marker5.markerType = MapPOIItem.MarkerType.BluePin
        building_marker5.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker5.userObject = R.drawable.building_05

        mapView.addPOIItem(building_marker5)

        val building_lat6 = 37.584664
        val building_long6 = 127.059661

        val locate_building6 = MapPoint.mapPointWithGeoCoord(building_lat6, building_long6)

        val building_marker6 = MapPOIItem()
        building_marker6.itemName = "배봉관"
        building_marker6.tag = 6
        building_marker6.mapPoint = locate_building6

        building_marker6.markerType = MapPOIItem.MarkerType.BluePin
        building_marker6.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker6.userObject = R.drawable.building_06

        mapView.addPOIItem(building_marker6)

        val building_lat7 = 37.584768
        val building_long7 = 127.0577

        val locate_building7 = MapPoint.mapPointWithGeoCoord(building_lat7, building_long7)

        val building_marker7 = MapPOIItem()
        building_marker7.itemName = "대학본부"
        building_marker7.tag = 7
        building_marker7.mapPoint = locate_building7

        building_marker7.markerType = MapPOIItem.MarkerType.BluePin
        building_marker7.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker7.userObject = R.drawable.building_07

        mapView.addPOIItem(building_marker7)

        val building_lat8 = 37.582519
        val building_long8 = 127.059173

        val locate_building8 = MapPoint.mapPointWithGeoCoord(building_lat8, building_long8)

        val building_marker8 = MapPOIItem()
        building_marker8.itemName = "자연과학관"
        building_marker8.tag = 8
        building_marker8.mapPoint = locate_building8

        building_marker8.markerType = MapPOIItem.MarkerType.BluePin
        building_marker8.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker8.userObject = R.drawable.building_08

        mapView.addPOIItem(building_marker8)

        val building_lat10 = 37.582901
        val building_long10 = 127.056628

        val locate_building10 = MapPoint.mapPointWithGeoCoord(building_lat10, building_long10)

        val building_marker10 = MapPOIItem()
        building_marker10.itemName = "경농관"
        building_marker10.tag = 10
        building_marker10.mapPoint = locate_building10

        building_marker10.markerType = MapPOIItem.MarkerType.BluePin
        building_marker10.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker10.userObject = R.drawable.building_10

        mapView.addPOIItem(building_marker10)

        val building_lat11 = 37.584651
        val building_long11 = 127.05902

        val locate_building11 = MapPoint.mapPointWithGeoCoord(building_lat11, building_long11)

        val building_marker11 = MapPOIItem()
        building_marker11.itemName = "제 2공학관"
        building_marker11.tag = 11
        building_marker11.mapPoint = locate_building11

        building_marker11.markerType = MapPOIItem.MarkerType.BluePin
        building_marker11.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker11.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker11)

        val building_lat12 = 37.583727
        val building_long12 = 127.060117

        val locate_building12 = MapPoint.mapPointWithGeoCoord(building_lat12, building_long12)

        val building_marker12 = MapPOIItem()
        building_marker12.itemName = "학생회관"
        building_marker12.tag = 12
        building_marker12.mapPoint = locate_building12

        building_marker12.markerType = MapPOIItem.MarkerType.BluePin
        building_marker12.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker12.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker12)

        val building_lat13 = 37.584938
        val building_long13 = 127.060734

        val locate_building13 = MapPoint.mapPointWithGeoCoord(building_lat13, building_long13)

        val building_marker13 = MapPOIItem()
        building_marker13.itemName = "학군단"
        building_marker13.tag = 13
        building_marker13.mapPoint = locate_building13

        building_marker13.markerType = MapPOIItem.MarkerType.BluePin
        building_marker13.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker13.userObject = R.drawable.building_13

        mapView.addPOIItem(building_marker13)

        val building_lat14 = 37.535334
        val building_long14 = 127.057526

        val locate_building14 = MapPoint.mapPointWithGeoCoord(building_lat14, building_long14)

        val building_marker14 = MapPOIItem()
        building_marker14.itemName = "과학기술관"
        building_marker14.tag = 14
        building_marker14.mapPoint = locate_building14

        building_marker14.markerType = MapPOIItem.MarkerType.BluePin
        building_marker14.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker14.userObject = R.drawable.building_14

        mapView.addPOIItem(building_marker14)

        val building_lat15 = 37.583114
        val building_long15 = 127.058641

        val locate_building15 = MapPoint.mapPointWithGeoCoord(building_lat15, building_long15)

        val building_marker15 = MapPOIItem()
        building_marker15.itemName = "21세기관"
        building_marker15.tag = 15
        building_marker15.mapPoint = locate_building15

        building_marker15.markerType = MapPOIItem.MarkerType.BluePin
        building_marker15.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker15.userObject = R.drawable.building_15

        mapView.addPOIItem(building_marker15)

        val building_lat16 = 37.5842
        val building_long16 = 127.056267

        val locate_building16 = MapPoint.mapPointWithGeoCoord(building_lat16, building_long16)

        val building_marker16 = MapPOIItem()
        building_marker16.itemName = "조형관"
        building_marker16.tag = 16
        building_marker16.mapPoint = locate_building16

        building_marker16.markerType = MapPOIItem.MarkerType.BluePin
        building_marker16.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker16.userObject = R.drawable.building_16

        mapView.addPOIItem(building_marker16)

        val building_lat18 = 37.582838
        val building_long18 = 127.057668

        val locate_building18 = MapPoint.mapPointWithGeoCoord(building_lat18, building_long18)

        val building_marker18 = MapPOIItem()
        building_marker18.itemName = "자작마루"
        building_marker18.tag = 18
        building_marker18.mapPoint = locate_building18

        building_marker18.markerType = MapPOIItem.MarkerType.BluePin
        building_marker18.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker18.userObject = R.drawable.building_18

        mapView.addPOIItem(building_marker18)

        val building_lat19 = 37.582855
        val building_long19 = 127.06083

        val locate_building19 = MapPoint.mapPointWithGeoCoord(building_lat19, building_long19)

        val building_marker19 = MapPOIItem()
        building_marker19.itemName = "정보기술관"
        building_marker19.tag = 19
        building_marker19.mapPoint = locate_building19

        building_marker19.markerType = MapPOIItem.MarkerType.BluePin
        building_marker19.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker19.userObject = R.drawable.building_19

        mapView.addPOIItem(building_marker19)

        val building_lat20 = 37.582032
        val building_long20 = 127.05675

        val locate_building20 = MapPoint.mapPointWithGeoCoord(building_lat20, building_long20)

        val building_marker20 = MapPOIItem()
        building_marker20.itemName = "법학관"
        building_marker20.tag = 20
        building_marker20.mapPoint = locate_building20

        building_marker20.markerType = MapPOIItem.MarkerType.BluePin
        building_marker20.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker20.userObject = R.drawable.building_20

        mapView.addPOIItem(building_marker20)

        val building_lat21 = 37.584909
        val building_long21 = 127.062091

        val locate_building21 = MapPoint.mapPointWithGeoCoord(building_lat21, building_long21)

        val building_marker21 = MapPOIItem()
        building_marker21.itemName = "중앙도서관"
        building_marker21.tag = 21
        building_marker21.mapPoint = locate_building21

        building_marker21.markerType = MapPOIItem.MarkerType.BluePin
        building_marker21.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker21.userObject = R.drawable.building_21

        mapView.addPOIItem(building_marker21)

        val building_lat22 = 37.584894
        val building_long22 = 127.063434

        val locate_building22 = MapPoint.mapPointWithGeoCoord(building_lat22, building_long22)

        val building_marker22 = MapPOIItem()
        building_marker22.itemName = "생활관"
        building_marker22.tag = 22
        building_marker22.mapPoint = locate_building22

        building_marker22.markerType = MapPOIItem.MarkerType.BluePin
        building_marker22.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker22.userObject = R.drawable.building_22

        mapView.addPOIItem(building_marker22)

        val building_lat23 = 37.582276
        val building_long23 = 127.057817

        val locate_building23 = MapPoint.mapPointWithGeoCoord(building_lat23, building_long23)

        val building_marker23 = MapPOIItem()
        building_marker23.itemName = "건축구조실험동"
        building_marker23.tag = 23
        building_marker23.mapPoint = locate_building23

        building_marker23.markerType = MapPOIItem.MarkerType.BluePin
        building_marker23.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker23.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker23)

        val building_lat24 = 37.582111
        val building_long24 = 127.057508

        val locate_building24 = MapPoint.mapPointWithGeoCoord(building_lat24, building_long24)

        val building_marker24 = MapPOIItem()
        building_marker24.itemName = "토목구조실험동"
        building_marker24.tag = 24
        building_marker24.mapPoint = locate_building24

        building_marker24.markerType = MapPOIItem.MarkerType.BluePin
        building_marker24.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker24.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker24)

        val building_lat25 = 37.582562
        val building_long25 = 127.060095

        val locate_building25 = MapPoint.mapPointWithGeoCoord(building_lat25, building_long25)

        val building_marker25 = MapPOIItem()
        building_marker25.itemName = "미디어관"
        building_marker25.tag = 25
        building_marker25.mapPoint = locate_building25

        building_marker25.markerType = MapPOIItem.MarkerType.BluePin
        building_marker25.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker25.userObject = R.drawable.building_25

        mapView.addPOIItem(building_marker25)

        val building_lat27 = 37.583031
        val building_long27 = 127.059756

        val locate_building27 = MapPoint.mapPointWithGeoCoord(building_lat27, building_long27)

        val building_marker27 = MapPOIItem()
        building_marker27.itemName = "대강당"
        building_marker27.tag = 27
        building_marker27.mapPoint = locate_building27

        building_marker27.markerType = MapPOIItem.MarkerType.BluePin
        building_marker27.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker27.userObject = R.drawable.building_27

        mapView.addPOIItem(building_marker27)

        val building_lat28 = 37.585351
        val building_long28 = 127.056673

        val locate_building28 = MapPoint.mapPointWithGeoCoord(building_lat28, building_long28)

        val building_marker28 = MapPOIItem()
        building_marker28.itemName = "운동장"
        building_marker28.tag = 28
        building_marker28.mapPoint = locate_building28

        building_marker28.markerType = MapPOIItem.MarkerType.BluePin
        building_marker28.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker28.userObject = R.drawable.building_28

        mapView.addPOIItem(building_marker28)

        val building_lat29 = 37.583158
        val building_long29 = 127.056936

        val locate_building29 = MapPoint.mapPointWithGeoCoord(building_lat29, building_long29)

        val building_marker29 = MapPOIItem()
        building_marker29.itemName = "박물관"
        building_marker29.tag = 29
        building_marker29.mapPoint = locate_building29

        building_marker29.markerType = MapPOIItem.MarkerType.BluePin
        building_marker29.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker29.userObject = R.drawable.building_29

        mapView.addPOIItem(building_marker29)

        val building_lat32 = 37.582426
        val building_long32 = 127.056503

        val locate_building32 = MapPoint.mapPointWithGeoCoord(building_lat32, building_long32)

        val building_marker32 = MapPOIItem()
        building_marker32.itemName = "웰니스센터"
        building_marker32.tag = 32
        building_marker32.mapPoint = locate_building32

        building_marker32.markerType = MapPOIItem.MarkerType.BluePin
        building_marker32.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker32.userObject = R.drawable.building_32

        mapView.addPOIItem(building_marker32)

        val building_lat33 = 37.584622
        val building_long33 = 127.056978

        val locate_building33 = MapPoint.mapPointWithGeoCoord(building_lat33, building_long33)

        val building_marker33 = MapPOIItem()
        building_marker33.itemName = "미래관"
        building_marker33.tag = 33
        building_marker33.mapPoint = locate_building33

        building_marker33.markerType = MapPOIItem.MarkerType.BluePin
        building_marker33.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker33.userObject = R.drawable.building_33

        mapView.addPOIItem(building_marker33)

        val building_lat34 = 37.584376
        val building_long34 = 127.06317

        val locate_building34 = MapPoint.mapPointWithGeoCoord(building_lat34, building_long34)

        val building_marker34 = MapPOIItem()
        building_marker34.itemName = "국제학사"
        building_marker34.tag = 34
        building_marker34.mapPoint = locate_building34

        building_marker34.markerType = MapPOIItem.MarkerType.BluePin
        building_marker34.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker34.userObject = R.drawable.building_34

        mapView.addPOIItem(building_marker34)

        val building_lat35 = 37.58336
        val building_long35 = 127.06269

        val locate_building35 = MapPoint.mapPointWithGeoCoord(building_lat35, building_long35)

        val building_marker35 = MapPOIItem()
        building_marker35.itemName = "음악관"
        building_marker35.tag = 35
        building_marker35.mapPoint = locate_building35

        building_marker35.markerType = MapPOIItem.MarkerType.BluePin
        building_marker35.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker35.userObject = R.drawable.building_35

        mapView.addPOIItem(building_marker35)

        val building_lat36 = 37.586318
        val building_long36 = 127.057065

        val locate_building36 = MapPoint.mapPointWithGeoCoord(building_lat36, building_long36)

        val building_marker36 = MapPOIItem()
        building_marker36.itemName = "어린이집"
        building_marker36.tag = 36
        building_marker36.mapPoint = locate_building36

        building_marker36.markerType = MapPOIItem.MarkerType.BluePin
        building_marker36.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker36.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker36)

        val building_lat37 = 37.584241
        val building_long37 = 127.055702

        val locate_building37 = MapPoint.mapPointWithGeoCoord(building_lat37, building_long37)

        val building_marker37 = MapPOIItem()
        building_marker37.itemName = "100주년 기념관"
        building_marker37.tag = 37
        building_marker37.mapPoint = locate_building37

        building_marker37.markerType = MapPOIItem.MarkerType.BluePin
        building_marker37.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker37.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker37)

        val building_lat41 = 37.584988
        val building_long41 = 127.061239

        val locate_building41 = MapPoint.mapPointWithGeoCoord(building_lat41, building_long41)

        val building_marker41 = MapPOIItem()
        building_marker41.itemName = "실외테니스장"
        building_marker41.tag = 41
        building_marker41.mapPoint = locate_building41

        building_marker41.markerType = MapPOIItem.MarkerType.BluePin
        building_marker41.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker41.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker41)

        val building_lat81 = 37.582499
        val building_long81 = 127.060792

        val locate_building81 = MapPoint.mapPointWithGeoCoord(building_lat81, building_long81)

        val building_marker81 = MapPOIItem()
        building_marker81.itemName = "자동화온실"
        building_marker81.tag = 81
        building_marker81.mapPoint = locate_building81

        building_marker81.markerType = MapPOIItem.MarkerType.BluePin
        building_marker81.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_marker81.userObject = R.drawable.default_image

        mapView.addPOIItem(building_marker81)

        val building_latC = 37.584127
        val building_longC = 127.061589

        val locate_buildingC = MapPoint.mapPointWithGeoCoord(building_latC, building_longC)

        val building_markerC = MapPOIItem()
        building_markerC.itemName = "하늘못"
        building_markerC.tag = -1
        building_markerC.mapPoint = locate_buildingC

        building_markerC.markerType = MapPOIItem.MarkerType.BluePin
        building_markerC.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        building_markerC.userObject = R.drawable.default_image

        mapView.addPOIItem(building_markerC)

        setCenterView()
    }

    /* 주차 가능 구역 마커 출력 */
    fun showParkingArea() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val parking_lat1 = 37.585268
        val parking_long1 = 127.056157

        val locate_parking1 = MapPoint.mapPointWithGeoCoord(parking_lat1, parking_long1)

        val parking_marker1 = MapPOIItem()
        parking_marker1.itemName = "운동장밑"
        parking_marker1.tag = 0
        parking_marker1.mapPoint = locate_parking1

        parking_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker1.customImageResourceId = R.drawable.parking

        parking_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker1.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker1)

        val parking_lat2 = 37.585229
        val parking_long2 = 127.05722

        val locate_parking2 = MapPoint.mapPointWithGeoCoord(parking_lat2, parking_long2)

        val parking_marker2 = MapPOIItem()
        parking_marker2.itemName = "과기관옆"
        parking_marker2.tag = 0
        parking_marker2.mapPoint = locate_parking2

        parking_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker2.customImageResourceId = R.drawable.parking

        parking_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker2.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker2)

        val parking_lat3 = 37.584507
        val parking_long3 = 127.057838

        val locate_parking3 = MapPoint.mapPointWithGeoCoord(parking_lat3, parking_long3)

        val parking_marker3 = MapPOIItem()
        parking_marker3.itemName = "본부앞"
        parking_marker3.tag = 0
        parking_marker3.mapPoint = locate_parking3

        parking_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker3.customImageResourceId = R.drawable.parking

        parking_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker3.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker3)

        val parking_lat4 = 37.585004
        val parking_long4 = 127.058374

        val locate_parking4 = MapPoint.mapPointWithGeoCoord(parking_lat4, parking_long4)

        val parking_marker4 = MapPOIItem()
        parking_marker4.itemName = "제1공뒤"
        parking_marker4.tag = 0
        parking_marker4.mapPoint = locate_parking4

        parking_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker4.customImageResourceId = R.drawable.parking

        parking_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker4.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker4)

        val parking_lat5 = 37.58433
        val parking_long5 = 127.059906

        val locate_parking5 = MapPoint.mapPointWithGeoCoord(parking_lat5, parking_long5)

        val parking_marker5 = MapPOIItem()
        parking_marker5.itemName = "배봉관앞"
        parking_marker5.tag = 0
        parking_marker5.mapPoint = locate_parking5

        parking_marker5.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker5.customImageResourceId = R.drawable.parking

        parking_marker5.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker5.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker5)

        val parking_lat6 = 37.584702
        val parking_long6 = 127.060359

        val locate_parking6 = MapPoint.mapPointWithGeoCoord(parking_lat6, parking_long6)

        val parking_marker6 = MapPOIItem()
        parking_marker6.itemName = "창공관뒤"
        parking_marker6.tag = 0
        parking_marker6.mapPoint = locate_parking6

        parking_marker6.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker6.customImageResourceId = R.drawable.parking

        parking_marker6.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker6.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker6)

        val parking_lat7 = 37.584648
        val parking_long7 = 127.061845

        val locate_parking7 = MapPoint.mapPointWithGeoCoord(parking_lat7, parking_long7)

        val parking_marker7 = MapPOIItem()
        parking_marker7.itemName = "중도앞"
        parking_marker7.tag = 0
        parking_marker7.mapPoint = locate_parking7

        parking_marker7.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker7.customImageResourceId = R.drawable.parking

        parking_marker7.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker7.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker7)

        val parking_lat8 = 37.583122
        val parking_long8 = 127.060221

        val locate_parking8 = MapPoint.mapPointWithGeoCoord(parking_lat8, parking_long8)

        val parking_marker8 = MapPOIItem()
        parking_marker8.itemName = "정기관앞"
        parking_marker8.tag = 0
        parking_marker8.mapPoint = locate_parking8

        parking_marker8.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker8.customImageResourceId = R.drawable.parking

        parking_marker8.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker8.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker8)

        val parking_lat9 = 37.582642
        val parking_long9 = 127.058467

        val locate_parking9 = MapPoint.mapPointWithGeoCoord(parking_lat9, parking_long9)

        val parking_marker9 = MapPOIItem()
        parking_marker9.itemName = "자과관옆(정문방향)"
        parking_marker9.tag = 0
        parking_marker9.mapPoint = locate_parking9

        parking_marker9.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker9.customImageResourceId = R.drawable.parking

        parking_marker9.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker9.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker9)

        val parking_lat10 = 37.582745
        val parking_long10 = 127.059796

        val locate_parking10 = MapPoint.mapPointWithGeoCoord(parking_lat10, parking_long10)

        val parking_marker10 = MapPOIItem()
        parking_marker10.itemName = "자과관옆(후문방향)"
        parking_marker10.tag = 0
        parking_marker10.mapPoint = locate_parking10

        parking_marker10.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker10.customImageResourceId = R.drawable.parking

        parking_marker10.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker10.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker10)

        val parking_lat11 = 37.583272
        val parking_long11 = 127.057829

        val locate_parking11 = MapPoint.mapPointWithGeoCoord(parking_lat11, parking_long11)

        val parking_marker11 = MapPOIItem()
        parking_marker11.itemName = "건공옆"
        parking_marker11.tag = 0
        parking_marker11.mapPoint = locate_parking11

        parking_marker11.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker11.customImageResourceId = R.drawable.parking

        parking_marker11.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker11.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker11)

        val parking_lat12 = 37.58243
        val parking_long12 = 127.056193

        val locate_parking12 = MapPoint.mapPointWithGeoCoord(parking_lat12, parking_long12)

        val parking_marker12 = MapPOIItem()
        parking_marker12.itemName = "법학관밑"
        parking_marker12.tag = 0
        parking_marker12.mapPoint = locate_parking12

        parking_marker12.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker12.customImageResourceId = R.drawable.parking

        parking_marker12.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker12.customSelectedImageResourceId = R.drawable.click_parking


        mapView.addPOIItem(parking_marker12)

        val parking_lat13 = 37.583395
        val parking_long13 = 127.056645

        val locate_parking13 = MapPoint.mapPointWithGeoCoord(parking_lat13, parking_long13)

        val parking_marker13 = MapPOIItem()
        parking_marker13.itemName = "전농관뒤"
        parking_marker13.tag = 0
        parking_marker13.mapPoint = locate_parking13

        parking_marker13.markerType = MapPOIItem.MarkerType.CustomImage
        parking_marker13.customImageResourceId = R.drawable.parking

        parking_marker13.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        parking_marker13.customSelectedImageResourceId = R.drawable.click_parking

        mapView.addPOIItem(parking_marker13)

        setCenterView()
    }

    /* 카페 마커 출력 */
    fun showAllCafes() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val cafe_lat1 = 37.583597
        val cafe_long1 = 127.056548

        val locate_cafe1 = MapPoint.mapPointWithGeoCoord(cafe_lat1, cafe_long1)

        val cafe_marker1 = MapPOIItem()
        cafe_marker1.itemName = "전농관"
        cafe_marker1.tag = 0
        cafe_marker1.mapPoint = locate_cafe1

        cafe_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker1.customImageResourceId = R.drawable.cafe

        cafe_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker1.customSelectedImageResourceId = R.drawable.click_cafe


        mapView.addPOIItem(cafe_marker1)

        val cafe_lat2 = 37.582519
        val cafe_long2 = 127.059173

        val locate_cafe2 = MapPoint.mapPointWithGeoCoord(cafe_lat2, cafe_long2)

        val cafe_marker2 = MapPOIItem()
        cafe_marker2.itemName = "자연과학관"
        cafe_marker2.tag = 0
        cafe_marker2.mapPoint = locate_cafe2

        cafe_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker2.customImageResourceId = R.drawable.cafe

        cafe_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker2.customSelectedImageResourceId = R.drawable.click_cafe


        mapView.addPOIItem(cafe_marker2)

        val cafe_lat3 = 37.583727
        val cafe_long3 = 127.060117

        val locate_cafe3 = MapPoint.mapPointWithGeoCoord(cafe_lat3, cafe_long3)

        val cafe_marker3 = MapPOIItem()
        cafe_marker3.itemName = "학생회관"
        cafe_marker3.tag = 0
        cafe_marker3.mapPoint = locate_cafe3

        cafe_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker3.customImageResourceId = R.drawable.cafe

        cafe_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker3.customSelectedImageResourceId = R.drawable.click_cafe


        mapView.addPOIItem(cafe_marker3)

        val cafe_lat4 = 37.582032
        val cafe_long4 = 127.05675

        val locate_cafe4 = MapPoint.mapPointWithGeoCoord(cafe_lat4, cafe_long4)

        val cafe_marker4 = MapPOIItem()
        cafe_marker4.itemName = "법학관"
        cafe_marker4.tag = 0
        cafe_marker4.mapPoint = locate_cafe4

        cafe_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker4.customImageResourceId = R.drawable.cafe

        cafe_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker4.customSelectedImageResourceId = R.drawable.click_cafe


        mapView.addPOIItem(cafe_marker4)

        val cafe_lat5 = 37.584622
        val cafe_long5 = 127.056978

        val locate_cafe5 = MapPoint.mapPointWithGeoCoord(cafe_lat5, cafe_long5)

        val cafe_marker5 = MapPOIItem()
        cafe_marker5.itemName = "미래관"
        cafe_marker5.tag = 0
        cafe_marker5.mapPoint = locate_cafe5

        cafe_marker5.markerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker5.customImageResourceId = R.drawable.cafe

        cafe_marker5.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker5.customSelectedImageResourceId = R.drawable.click_cafe


        mapView.addPOIItem(cafe_marker5)

        val cafe_lat6 = 37.584241
        val cafe_long6 = 127.055702

        val locate_cafe6 = MapPoint.mapPointWithGeoCoord(cafe_lat6, cafe_long6)

        val cafe_marker6 = MapPOIItem()
        cafe_marker6.itemName = "100주년 기념관"
        cafe_marker6.tag = 0
        cafe_marker6.mapPoint = locate_cafe6

        cafe_marker6.markerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker6.customImageResourceId = R.drawable.cafe

        cafe_marker6.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        cafe_marker6.customSelectedImageResourceId = R.drawable.click_cafe


        mapView.addPOIItem(cafe_marker6)

//        val cafe_lat7 = nan
//        val cafe_long7 = nan
//
//        val locate_cafe7 = MapPoint.mapPointWithGeoCoord(cafe_lat7, cafe_long7)
//
//        val cafe_marker7 = MapPOIItem()
//        cafe_marker7.itemName = "정문"
//        cafe_marker7.tag = 0
//        cafe_marker7.mapPoint = locate_cafe7
//
//        cafe_marker7.markerType = MapPOIItem.MarkerType.CustomImage
//        cafe_marker7.customImageResourceId = R.drawable.cafe
//
//        cafe_marker7.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
//        cafe_marker7.customSelectedImageResourceId = R.drawable.click_cafe
//
//
//        mapView.addPOIItem(cafe_marker7)
//
//        val cafe_lat8 = nan
//        val cafe_long8 = nan
//
//        val locate_cafe8 = MapPoint.mapPointWithGeoCoord(cafe_lat8, cafe_long8)
//
//        val cafe_marker8 = MapPOIItem()
//        cafe_marker8.itemName = "후문"
//        cafe_marker8.tag = 0
//        cafe_marker8.mapPoint = locate_cafe8
//
//        cafe_marker8.markerType = MapPOIItem.MarkerType.CustomImage
//        cafe_marker8.customImageResourceId = R.drawable.cafe
//
//        cafe_marker8.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
//        cafe_marker8.customSelectedImageResourceId = R.drawable.click_cafe

//        mapView.addPOIItem(cafe_marker8)

        setCenterView()
    }

    /* 편의점 마커 출력  Show all Convenience Store */
    fun showAllConvStores() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val convenience_lat1 = 37.583727
        val convenience_long1 = 127.060117

        val locate_convenience1 = MapPoint.mapPointWithGeoCoord(convenience_lat1, convenience_long1)

        val convenience_marker1 = MapPOIItem()
        convenience_marker1.itemName = "학생회관"
        convenience_marker1.tag = 0
        convenience_marker1.mapPoint = locate_convenience1

        convenience_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker1.customImageResourceId = R.drawable.convenience

        convenience_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker1.customSelectedImageResourceId = R.drawable.click_convenience


        mapView.addPOIItem(convenience_marker1)

        val convenience_lat2 = 37.584909
        val convenience_long2 = 127.062091

        val locate_convenience2 = MapPoint.mapPointWithGeoCoord(convenience_lat2, convenience_long2)

        val convenience_marker2 = MapPOIItem()
        convenience_marker2.itemName = "중앙도서관"
        convenience_marker2.tag = 0
        convenience_marker2.mapPoint = locate_convenience2

        convenience_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker2.customImageResourceId = R.drawable.convenience

        convenience_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker2.customSelectedImageResourceId = R.drawable.click_convenience


        mapView.addPOIItem(convenience_marker2)

        val convenience_lat3 = 37.584894
        val convenience_long3 = 127.063434

        val locate_convenience3 = MapPoint.mapPointWithGeoCoord(convenience_lat3, convenience_long3)

        val convenience_marker3 = MapPOIItem()
        convenience_marker3.itemName = "생활관"
        convenience_marker3.tag = 0
        convenience_marker3.mapPoint = locate_convenience3

        convenience_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker3.customImageResourceId = R.drawable.convenience

        convenience_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker3.customSelectedImageResourceId = R.drawable.click_convenience


        mapView.addPOIItem(convenience_marker3)

        val convenience_lat4 = 37.584622
        val convenience_long4 = 127.056978

        val locate_convenience4 = MapPoint.mapPointWithGeoCoord(convenience_lat4, convenience_long4)

        val convenience_marker4 = MapPOIItem()
        convenience_marker4.itemName = "미래관"
        convenience_marker4.tag = 0
        convenience_marker4.mapPoint = locate_convenience4

        convenience_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker4.customImageResourceId = R.drawable.convenience

        convenience_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        convenience_marker4.customSelectedImageResourceId = R.drawable.click_convenience


        mapView.addPOIItem(convenience_marker4)

//        val convenience_lat5 = nan
//        val convenience_long5 = nan
//
//        val locate_convenience5 = MapPoint.mapPointWithGeoCoord(convenience_lat5, convenience_long5)
//
//        val convenience_marker5 = MapPOIItem()
//        convenience_marker5.itemName = "정문"
//        convenience_marker5.tag = 0
//        convenience_marker5.mapPoint = locate_convenience5
//
//        convenience_marker5.markerType = MapPOIItem.MarkerType.CustomImage
//        convenience_marker5.customImageResourceId = R.drawable.convenience
//
//        convenience_marker5.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
//        convenience_marker5.customSelectedImageResourceId = R.drawable.click_convenience
//
//
//        mapView.addPOIItem(convenience_marker5)

//        val convenience_lat6 = nan
//        val convenience_long6 = nan
//
//        val locate_convenience6 = MapPoint.mapPointWithGeoCoord(convenience_lat6, convenience_long6)
//
//        val convenience_marker6 = MapPOIItem()
//        convenience_marker6.itemName = "후문"
//        convenience_marker6.tag = 0
//        convenience_marker6.mapPoint = locate_convenience6
//
//        convenience_marker6.markerType = MapPOIItem.MarkerType.CustomImage
//        convenience_marker6.customImageResourceId = R.drawable.convenience
//
//        convenience_marker6.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
//        convenience_marker6.customSelectedImageResourceId = R.drawable.click_convenience
//
//
//        mapView.addPOIItem(convenience_marker6)

        setCenterView()
    }

    /* 프린터 위치 마커 */
    fun showAllPrinter() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val printer_lat1 = 37.583727
        val printer_long1 = 127.060117

        val locate_printer1 = MapPoint.mapPointWithGeoCoord(printer_lat1, printer_long1)

        val printer_marker1 = MapPOIItem()
        printer_marker1.itemName = "학생회관"
        printer_marker1.tag = 0
        printer_marker1.mapPoint = locate_printer1

        printer_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        printer_marker1.customImageResourceId = R.drawable.printer

        printer_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        printer_marker1.customSelectedImageResourceId = R.drawable.click_printer


        mapView.addPOIItem(printer_marker1)

        val printer_lat2 = 37.583114
        val printer_long2 = 127.058641

        val locate_printer2 = MapPoint.mapPointWithGeoCoord(printer_lat2, printer_long2)

        val printer_marker2 = MapPOIItem()
        printer_marker2.itemName = "21세기관"
        printer_marker2.tag = 0
        printer_marker2.mapPoint = locate_printer2

        printer_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        printer_marker2.customImageResourceId = R.drawable.printer

        printer_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        printer_marker2.customSelectedImageResourceId = R.drawable.click_printer


        mapView.addPOIItem(printer_marker2)

        val printer_lat3 = 37.582032
        val printer_long3 = 127.05675

        val locate_printer3 = MapPoint.mapPointWithGeoCoord(printer_lat3, printer_long3)

        val printer_marker3 = MapPOIItem()
        printer_marker3.itemName = "법학관"
        printer_marker3.tag = 0
        printer_marker3.mapPoint = locate_printer3

        printer_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        printer_marker3.customImageResourceId = R.drawable.printer

        printer_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        printer_marker3.customSelectedImageResourceId = R.drawable.click_printer


        mapView.addPOIItem(printer_marker3)

        val printer_lat4 = 37.584909
        val printer_long4 = 127.062091

        val locate_printer4 = MapPoint.mapPointWithGeoCoord(printer_lat4, printer_long4)

        val printer_marker4 = MapPOIItem()
        printer_marker4.itemName = "중앙도서관"
        printer_marker4.tag = 0
        printer_marker4.mapPoint = locate_printer4

        printer_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        printer_marker4.customImageResourceId = R.drawable.printer

        printer_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        printer_marker4.customSelectedImageResourceId = R.drawable.click_printer


        mapView.addPOIItem(printer_marker4)

        val printer_lat5 = 37.584622
        val printer_long5 = 127.056978

        val locate_printer5 = MapPoint.mapPointWithGeoCoord(printer_lat5, printer_long5)

        val printer_marker5 = MapPOIItem()
        printer_marker5.itemName = "미래관"
        printer_marker5.tag = 0
        printer_marker5.mapPoint = locate_printer5

        printer_marker5.markerType = MapPOIItem.MarkerType.CustomImage
        printer_marker5.customImageResourceId = R.drawable.printer

        printer_marker5.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        printer_marker5.customSelectedImageResourceId = R.drawable.click_printer

        mapView.addPOIItem(printer_marker5)

        setCenterView()
    }

    /* 흡연구역 마커 */
    fun showSmokeArea() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val smoke_lat1 = 37.585004
        val smoke_long1 = 127.058374

        val locate_smoke1 = MapPoint.mapPointWithGeoCoord(smoke_lat1, smoke_long1)

        val smoke_marker1 = MapPOIItem()
        smoke_marker1.itemName = "제1공학관 뒤편 및 우측 쪽문 계단"
        smoke_marker1.tag = 0
        smoke_marker1.mapPoint = locate_smoke1

        smoke_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker1.customImageResourceId = R.drawable.smoke

        smoke_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker1.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker1)

        val smoke_lat2 = 37.583907
        val smoke_long2 = 127.057794

        val locate_smoke2 = MapPoint.mapPointWithGeoCoord(smoke_lat2, smoke_long2)

        val smoke_marker2 = MapPOIItem()
        smoke_marker2.itemName = "건설공학관 뒤편 나눔 쉼터"
        smoke_marker2.tag = 0
        smoke_marker2.mapPoint = locate_smoke2

        smoke_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker2.customImageResourceId = R.drawable.smoke

        smoke_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker2.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker2)

        val smoke_lat3 = 37.583668
        val smoke_long3 = 127.06091

        val locate_smoke3 = MapPoint.mapPointWithGeoCoord(smoke_lat3, smoke_long3)

        val smoke_marker3 = MapPOIItem()
        smoke_marker3.itemName = "인문학관 정문 앞 공터"
        smoke_marker3.tag = 0
        smoke_marker3.mapPoint = locate_smoke3

        smoke_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker3.customImageResourceId = R.drawable.smoke

        smoke_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker3.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker3)

        val smoke_lat4 = 37.58463
        val smoke_long4 = 127.059219

        val locate_smoke4 = MapPoint.mapPointWithGeoCoord(smoke_lat4, smoke_long4)

        val smoke_marker4 = MapPOIItem()
        smoke_marker4.itemName = "제2공학관과 배봉관 사이"
        smoke_marker4.tag = 0
        smoke_marker4.mapPoint = locate_smoke4

        smoke_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker4.customImageResourceId = R.drawable.smoke

        smoke_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker4.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker4)

        val smoke_lat5 = 37.583969
        val smoke_long5 = 127.05988

        val locate_smoke5 = MapPoint.mapPointWithGeoCoord(smoke_lat5, smoke_long5)

        val smoke_marker5 = MapPOIItem()
        smoke_marker5.itemName = "학생회관정문 좌측 자전거 보관대"
        smoke_marker5.tag = 0
        smoke_marker5.mapPoint = locate_smoke5

        smoke_marker5.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker5.customImageResourceId = R.drawable.smoke

        smoke_marker5.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker5.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker5)

        val smoke_lat6 = 37.583727
        val smoke_long6 = 127.060117

        val locate_smoke6 = MapPoint.mapPointWithGeoCoord(smoke_lat6, smoke_long6)

        val smoke_marker6 = MapPOIItem()
        smoke_marker6.itemName = "학생회관3층 옥상"
        smoke_marker6.tag = 0
        smoke_marker6.mapPoint = locate_smoke6

        smoke_marker6.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker6.customImageResourceId = R.drawable.smoke

        smoke_marker6.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker6.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker6)

        val smoke_lat7 = 37.585148
        val smoke_long7 = 127.057688

        val locate_smoke7 = MapPoint.mapPointWithGeoCoord(smoke_lat7, smoke_long7)

        val smoke_marker7 = MapPOIItem()
        smoke_marker7.itemName = "과학기술관정문 우측 공간"
        smoke_marker7.tag = 0
        smoke_marker7.mapPoint = locate_smoke7

        smoke_marker7.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker7.customImageResourceId = R.drawable.smoke

        smoke_marker7.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker7.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker7)

        val smoke_lat8 = 37.583226
        val smoke_long8 = 127.058459

        val locate_smoke8 = MapPoint.mapPointWithGeoCoord(smoke_lat8, smoke_long8)

        val smoke_marker8 = MapPOIItem()
        smoke_marker8.itemName = "21세기관건물 앞 공터"
        smoke_marker8.tag = 0
        smoke_marker8.mapPoint = locate_smoke8

        smoke_marker8.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker8.customImageResourceId = R.drawable.smoke

        smoke_marker8.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker8.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker8)

        val smoke_lat9 = 37.584233
        val smoke_long9 = 127.056375

        val locate_smoke9 = MapPoint.mapPointWithGeoCoord(smoke_lat9, smoke_long9)

        val smoke_marker9 = MapPOIItem()
        smoke_marker9.itemName = "조형관정문 우측 공터"
        smoke_marker9.tag = 0
        smoke_marker9.mapPoint = locate_smoke9

        smoke_marker9.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker9.customImageResourceId = R.drawable.smoke

        smoke_marker9.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker9.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker9)

        val smoke_lat10 = 37.582969
        val smoke_long10 = 127.061134

        val locate_smoke10 = MapPoint.mapPointWithGeoCoord(smoke_lat10, smoke_long10)

        val smoke_marker10 = MapPOIItem()
        smoke_marker10.itemName = "정보기술관후문 쉼터"
        smoke_marker10.tag = 0
        smoke_marker10.mapPoint = locate_smoke10

        smoke_marker10.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker10.customImageResourceId = R.drawable.smoke

        smoke_marker10.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker10.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker10)

        val smoke_lat11 = 37.584831
        val smoke_long11 = 127.061805

        val locate_smoke11 = MapPoint.mapPointWithGeoCoord(smoke_lat11, smoke_long11)

        val smoke_marker11 = MapPOIItem()
        smoke_marker11.itemName = "중앙도서관정문 좌측 등나무"
        smoke_marker11.tag = 0
        smoke_marker11.mapPoint = locate_smoke11

        smoke_marker11.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker11.customImageResourceId = R.drawable.smoke

        smoke_marker11.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker11.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker11)

        val smoke_lat12 = 37.582323
        val smoke_long12 = 127.056799

        val locate_smoke12 = MapPoint.mapPointWithGeoCoord(smoke_lat12, smoke_long12)

        val smoke_marker12 = MapPOIItem()
        smoke_marker12.itemName = "법학관나무계단 4층"
        smoke_marker12.tag = 0
        smoke_marker12.mapPoint = locate_smoke12

        smoke_marker12.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker12.customImageResourceId = R.drawable.smoke

        smoke_marker12.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker12.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker12)

        val smoke_lat13 = 37.582654
        val smoke_long13 = 127.057161

        val locate_smoke13 = MapPoint.mapPointWithGeoCoord(smoke_lat13, smoke_long13)

        val smoke_marker13 = MapPOIItem()
        smoke_marker13.itemName = "법학관구름다리 밑"
        smoke_marker13.tag = 0
        smoke_marker13.mapPoint = locate_smoke13

        smoke_marker13.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker13.customImageResourceId = R.drawable.smoke

        smoke_marker13.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker13.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker13)

        val smoke_lat16 = 37.58353
        val smoke_long16 = 127.056724

        val locate_smoke16 = MapPoint.mapPointWithGeoCoord(smoke_lat16, smoke_long16)

        val smoke_marker16 = MapPOIItem()
        smoke_marker16.itemName = "전농관주차장 쪽 출입문 부근"
        smoke_marker16.tag = 0
        smoke_marker16.mapPoint = locate_smoke16

        smoke_marker16.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker16.customImageResourceId = R.drawable.smoke

        smoke_marker16.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker16.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker16)

        val smoke_lat18 = 37.584894
        val smoke_long18 = 127.063434

        val locate_smoke18 = MapPoint.mapPointWithGeoCoord(smoke_lat18, smoke_long18)

        val smoke_marker18 = MapPOIItem()
        smoke_marker18.itemName = "생활관1층 실내 흡연구역"
        smoke_marker18.tag = 0
        smoke_marker18.mapPoint = locate_smoke18

        smoke_marker18.markerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker18.customImageResourceId = R.drawable.smoke

        smoke_marker18.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        smoke_marker18.customSelectedImageResourceId = R.drawable.click_smoke


        mapView.addPOIItem(smoke_marker18)

        setCenterView()
    }

    /* 도서관 마커 */
    fun showAllLibraries() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val library_lat1 = 37.582032
        val library_long1 = 127.05675

        val locate_library1 = MapPoint.mapPointWithGeoCoord(library_lat1, library_long1)

        val library_marker1 = MapPOIItem()
        library_marker1.itemName = "법학관"
        library_marker1.tag = 0
        library_marker1.mapPoint = locate_library1

        library_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        library_marker1.customImageResourceId = R.drawable.library

        library_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        library_marker1.customSelectedImageResourceId = R.drawable.click_library


        mapView.addPOIItem(library_marker1)

        val library_lat2 = 37.584909
        val library_long2 = 127.062091

        val locate_library2 = MapPoint.mapPointWithGeoCoord(library_lat2, library_long2)

        val library_marker2 = MapPOIItem()
        library_marker2.itemName = "중앙도서관"
        library_marker2.tag = 0
        library_marker2.mapPoint = locate_library2

        library_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        library_marker2.customImageResourceId = R.drawable.library

        library_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        library_marker2.customSelectedImageResourceId = R.drawable.click_library


        mapView.addPOIItem(library_marker2)

        val library_lat3 = 37.584622
        val library_long3 = 127.056978

        val locate_library3 = MapPoint.mapPointWithGeoCoord(library_lat3, library_long3)

        val library_marker3 = MapPOIItem()
        library_marker3.itemName = "미래관"
        library_marker3.tag = 0
        library_marker3.mapPoint = locate_library3

        library_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        library_marker3.customImageResourceId = R.drawable.library

        library_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        library_marker3.customSelectedImageResourceId = R.drawable.click_library


        mapView.addPOIItem(library_marker3)

        val library_lat4 = 37.584241
        val library_long4 = 127.055702

        val locate_library4 = MapPoint.mapPointWithGeoCoord(library_lat4, library_long4)

        val library_marker4 = MapPOIItem()
        library_marker4.itemName = "100주년 기념관"
        library_marker4.tag = 0
        library_marker4.mapPoint = locate_library4

        library_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        library_marker4.customImageResourceId = R.drawable.library

        library_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        library_marker4.customSelectedImageResourceId = R.drawable.click_library

        mapView.addPOIItem(library_marker4)

        setCenterView()
    }

    /* atm 마커 */
    fun showAllATM() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val atm_lat1 = 37.584768
        val atm_long1 = 127.0577

        val locate_atm1 = MapPoint.mapPointWithGeoCoord(atm_lat1, atm_long1)

        val atm_marker1 = MapPOIItem()
        atm_marker1.itemName = "대학본부"
        atm_marker1.tag = 0
        atm_marker1.mapPoint = locate_atm1

        atm_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker1.customImageResourceId = R.drawable.atm

        atm_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker1.customSelectedImageResourceId = R.drawable.click_atm


        val atm_lat2 = 37.582519
        val atm_long2 = 127.059173

        val locate_atm2 = MapPoint.mapPointWithGeoCoord(atm_lat2, atm_long2)

        val atm_marker2 = MapPOIItem()
        atm_marker2.itemName = "자연과학관"
        atm_marker2.tag = 0
        atm_marker2.mapPoint = locate_atm2

        atm_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker2.customImageResourceId = R.drawable.atm

        atm_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker2.customSelectedImageResourceId = R.drawable.click_atm


        val atm_lat3 = 37.583727
        val atm_long3 = 127.060117

        val locate_atm3 = MapPoint.mapPointWithGeoCoord(atm_lat3, atm_long3)

        val atm_marker3 = MapPOIItem()
        atm_marker3.itemName = "학생회관"
        atm_marker3.tag = 0
        atm_marker3.mapPoint = locate_atm3

        atm_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker3.customImageResourceId = R.drawable.atm

        atm_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker3.customSelectedImageResourceId = R.drawable.click_atm


        val atm_lat4 = 37.583114
        val atm_long4 = 127.058641

        val locate_atm4 = MapPoint.mapPointWithGeoCoord(atm_lat4, atm_long4)

        val atm_marker4 = MapPOIItem()
        atm_marker4.itemName = "21세기관"
        atm_marker4.tag = 0
        atm_marker4.mapPoint = locate_atm4

        atm_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker4.customImageResourceId = R.drawable.atm

        atm_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker4.customSelectedImageResourceId = R.drawable.click_atm


        val atm_lat5 = 37.584909
        val atm_long5 = 127.062091

        val locate_atm5 = MapPoint.mapPointWithGeoCoord(atm_lat5, atm_long5)

        val atm_marker5 = MapPOIItem()
        atm_marker5.itemName = "중앙도서관"
        atm_marker5.tag = 0
        atm_marker5.mapPoint = locate_atm5

        atm_marker5.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker5.customImageResourceId = R.drawable.atm

        atm_marker5.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker5.customSelectedImageResourceId = R.drawable.click_atm


        val atm_lat6 = 37.584622
        val atm_long6 = 127.056978

        val locate_atm6 = MapPoint.mapPointWithGeoCoord(atm_lat6, atm_long6)

        val atm_marker6 = MapPOIItem()
        atm_marker6.itemName = "미래관"
        atm_marker6.tag = 0
        atm_marker6.mapPoint = locate_atm6

        atm_marker6.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker6.customImageResourceId = R.drawable.atm

        atm_marker6.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker6.customSelectedImageResourceId = R.drawable.click_atm
        /*
        val atm_lat7 = nan
        val atm_long7 = nan

        val locate_atm7 = MapPoint.mapPointWithGeoCoord(atm_lat7, atm_long7)

        val atm_marker7 = MapPOIItem()
        atm_marker7.itemName = "후문"
        atm_marker7.tag = 0
        atm_marker7.mapPoint = locate_atm7

        atm_marker7.markerType = MapPOIItem.MarkerType.CustomImage
        atm_marker7.customImageResourceId = R.drawable.atm

        atm_marker7.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        atm_marker7.customSelectedImageResourceId = R.drawable.click_atm
        */
        mapView.addPOIItem(atm_marker1)
        mapView.addPOIItem(atm_marker2)
        mapView.addPOIItem(atm_marker3)
        mapView.addPOIItem(atm_marker4)
        mapView.addPOIItem(atm_marker5)
        mapView.addPOIItem(atm_marker6)
        //mapView.addPOIItem(atm_marker7)

        setCenterView()
    }

    /* 식당 */
    fun showAllRestaurant() {
        mapView.removeAllPOIItems()
        mapView.setCalloutBalloonAdapter(null)

        val restaurant_lat1 = 37.584768
        val restaurant_long1 = 127.0577

        val locate_restaurant1 = MapPoint.mapPointWithGeoCoord(restaurant_lat1, restaurant_long1)

        val restaurant_marker1 = MapPOIItem()
        restaurant_marker1.itemName = "대학본부 식당"
        restaurant_marker1.tag = 0
        restaurant_marker1.mapPoint = locate_restaurant1

        restaurant_marker1.markerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker1.customImageResourceId = R.drawable.restaurant

        restaurant_marker1.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker1.customSelectedImageResourceId = R.drawable.click_restaurant

        mapView.addPOIItem(restaurant_marker1)


        val restaurant_lat2 = 37.582519
        val restaurant_long2 = 127.059173

        val locate_restaurant2 = MapPoint.mapPointWithGeoCoord(restaurant_lat2, restaurant_long2)

        val restaurant_marker2 = MapPOIItem()
        restaurant_marker2.itemName = "자연과학관 식당"
        restaurant_marker2.tag = 0
        restaurant_marker2.mapPoint = locate_restaurant2

        restaurant_marker2.markerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker2.customImageResourceId = R.drawable.restaurant

        restaurant_marker2.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker2.customSelectedImageResourceId = R.drawable.click_restaurant

        mapView.addPOIItem(restaurant_marker2)


        val restaurant_lat3 = 37.583727
        val restaurant_long3 = 127.060117

        val locate_restaurant3 = MapPoint.mapPointWithGeoCoord(restaurant_lat3, restaurant_long3)

        val restaurant_marker3 = MapPOIItem()
        restaurant_marker3.itemName = "학생 식당, 양식당"
        restaurant_marker3.tag = 0
        restaurant_marker3.mapPoint = locate_restaurant3

        restaurant_marker3.markerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker3.customImageResourceId = R.drawable.restaurant

        restaurant_marker3.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker3.customSelectedImageResourceId = R.drawable.click_restaurant

        mapView.addPOIItem(restaurant_marker3)


        val restaurant_lat4 = 37.584894
        val restaurant_long4 = 127.063434

        val locate_restaurant4 = MapPoint.mapPointWithGeoCoord(restaurant_lat4, restaurant_long4)

        val restaurant_marker4 = MapPOIItem()
        restaurant_marker4.itemName = "생활관 식당"
        restaurant_marker4.tag = 0
        restaurant_marker4.mapPoint = locate_restaurant4

        restaurant_marker4.markerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker4.customImageResourceId = R.drawable.restaurant

        restaurant_marker4.selectedMarkerType = MapPOIItem.MarkerType.CustomImage
        restaurant_marker4.customSelectedImageResourceId = R.drawable.click_restaurant

        mapView.addPOIItem(restaurant_marker4)

        setCenterView()
    }

    /* list view 초기화 함수 */
    fun initListView() {
        var mListView = findViewById<ListView>(R.id.userlist)

        /* ListView의 item을 저장하는 배열 */
        val item_list = ArrayList<String>()
        item_list.add("건물 위치")
        item_list.add("도서관")
        item_list.add("카페")
        item_list.add("편의점")
        item_list.add("프린터")
        item_list.add("흡연 구역")
        item_list.add("주차 가능 구역")
        item_list.add("ATM")
        item_list.add("식당")

        /* List view와 array 사이 처리를 위한 ArrayAdapter */
        val arrayAdapter: ArrayAdapter<*>

        arrayAdapter = ArrayAdapter(this,
            android.R.layout.simple_list_item_1, item_list)

        mListView.adapter = arrayAdapter

//        /* ListVIew의 item을 클릭했을때 반응하는 EventListener */
        mListView.setOnItemClickListener { parent, view, position, id ->
            when(position) {
                // 건물
                0 -> {
                    showAllBuildings()
                    uosDrawerLayout.closeDrawers()
                }
                // 도서관
                1 -> {
                    showAllLibraries()
                    uosDrawerLayout.closeDrawers()
                }
                // 카페
                2 -> {
                    showAllCafes()
                    uosDrawerLayout.closeDrawers()
                }
                // 편의점
                3 -> {
                    showAllConvStores()
                    uosDrawerLayout.closeDrawers()
                }
                // 프린터
                4 -> {
                    showAllPrinter()
                    uosDrawerLayout.closeDrawers()
                }
                // 흡연 구역
                5 -> {
                    showSmokeArea()
                    uosDrawerLayout.closeDrawers()
                }
                // 주차 가능 구역
                6 -> {
                    showParkingArea()
                    uosDrawerLayout.closeDrawers()
                }
                // ATM
                7 -> {
                    showAllATM()
                    uosDrawerLayout.closeDrawers()
                }
                // 식당
                8 -> {
                    showAllRestaurant()
                    uosDrawerLayout.closeDrawers()
                }
            }
        }
    }

    /* @Depressed 이전 버전에서 사용하던 메소드 */
    /* 새로운 액티비티에서 돌아올 때 자동 호출되는 메소드 */
    override fun onActivityResult(requestCode: Int, resultCode : Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        /* ListViewActivity code */
        if(requestCode == REQUEST_CODE) {
            when(resultCode) {
                // 도서관
                0 -> {
                    mapView.removeAllPOIItems()
                    showAllLibraries()
                }
                // 카페
                1 -> {
                    mapView.removeAllPOIItems()
                    showAllCafes()
                }
                // 편의점
                2 -> {
                    mapView.removeAllPOIItems()
                    showAllConvStores()
                }
                // 프린터
                3 -> {
                    mapView.removeAllPOIItems()
                    showAllPrinter()
                }
                // 흡연 구역
                4 -> {
                    mapView.removeAllPOIItems()
                    showSmokeArea()
                }
                // 주차 가능 구역
                5 -> {
                    mapView.removeAllPOIItems()
                    showParkingArea()
                }
                // 건물
                6 -> {
                    mapView.removeAllPOIItems()
                    showAllBuildings()
                }
            }
        }
    }

    // 권한 요청 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // if request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                }
                return

            }
            else -> {
            }
        }
    }

    // key hash 구하는 함수
    fun KeyHash(context: Context): String? {
        try {
            if(Build.VERSION.SDK_INT >= 28) {
                val info = getPackageInfo(context, PackageManager.GET_SIGNING_CERTIFICATES)
                val signatures = info.signingInfo.apkContentsSigners
                val md = MessageDigest.getInstance("SHA")
                for (signature in signatures) {
                    md.update(signature.toByteArray())
                    return String(Base64.encode(md.digest(), NO_WRAP))
                }
            }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onCalloutBalloonOfPOIItemTouched(
        var1: MapView,
        var2: MapPOIItem,
        var3: MapPOIItem.CalloutBalloonButtonType
    ) {
//        Toast.makeText(this, "Clicked" + var2.itemName + "Callout Balloon", Toast.LENGTH_SHORT).show()
    }

    override fun onDraggablePOIItemMoved(var1: MapView, var2: MapPOIItem, var3: MapPoint) { }

    /* Marker가 클릭 되었을 때, 마커를 화면 중심으로, zoom level을 1로 설정 */
    override fun onPOIItemSelected(var1: MapView, var2: MapPOIItem) {
        var1.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(var2.mapPoint.mapPointGeoCoord.latitude, var2.mapPoint.mapPointGeoCoord.longitude), true)
        var1.setZoomLevel(1, true)
    }

    override fun onMapViewInitialized(var1: MapView) { }

    override fun onMapViewCenterPointMoved(var1: MapView, var2: MapPoint) { }

    override fun onMapViewZoomLevelChanged(var1: MapView, var2: Int) { }

    override fun onMapViewSingleTapped(var1: MapView, var2: MapPoint){ }

    override fun onMapViewDoubleTapped(var1: MapView, var2: MapPoint) { }

    override fun onMapViewLongPressed(var1: MapView, var2: MapPoint) { }

    override fun onMapViewDragStarted(var1: MapView, var2: MapPoint) { }

    override fun onMapViewDragEnded(var1: MapView, var2: MapPoint) { }

    override fun onMapViewMoveFinished(var1: MapView, var2: MapPoint) { }

    override fun onCalloutBalloonOfPOIItemTouched(var1: MapView, var2: MapPOIItem) { }

    /* Current Location Event Listenner */
    override fun onCurrentLocationUpdate(var1: MapView, var2: MapPoint, var3: Float) { }

    override fun onCurrentLocationDeviceHeadingUpdate(var1: MapView, var2: Float){ }

    override fun onCurrentLocationUpdateFailed(var1: MapView) { }

    override fun onCurrentLocationUpdateCancelled(var1: MapView) { }
}

