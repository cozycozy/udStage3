package kotlin_challenge.test.co.jp.footprint

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.davemorrissey.labs.subscaleview.ImageSource
import io.realm.Realm
import kotlin_challenge.test.co.jp.footprint.common.*
import kotlin_challenge.test.co.jp.footprint.model.PhotoInfo
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.content_edit.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : AppCompatActivity(), LocationListener {


    lateinit var mode : ModeInEdit
    var contentURI : Uri? = null

    var PERMISSION = arrayOf(permission.CAMERA,
            permission.WRITE_EXTERNAL_STORAGE,
            permission.ACCESS_FINE_LOCATION)

    var isCameraEnabled : Boolean = false
    var isExternalStrageEnabled : Boolean = false
    var isLocationEnabled : Boolean = false

    var selectedPhotoInfo = PhotoInfo()

    var locationManager : LocationManager? = null
    var isGeoLocation : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        setSupportActionBar(toolbar)

        toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            setNavigationOnClickListener {
                finish()
            }
        }

        mode = intent.extras.getSerializable(IntentKey.EDIT_MODE.name) as ModeInEdit

        //パーミッションの都度確認
        if (mode == ModeInEdit.SHOOT) {
            if (Build.VERSION.SDK_INT >= 23 ) permissionCheck() else launchCamera()
        } else {
            // TODO
        }

        buttonGoMap.setOnClickListener{

            if (mode == ModeInEdit.SHOOT && !isGeoLocation) {
                Toast.makeText(this@EditActivity, getString(R.string.location_not_set), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            displayMap(selectedPhotoInfo.latitude,selectedPhotoInfo.longitude)

        }

        buttonResister.setOnClickListener{
            ResisterPhotoInfo()
        }
    }

    private fun ResisterPhotoInfo() {

        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        var photoInfoRecord = PhotoInfo()

        when(mode) {

            ModeInEdit.SHOOT -> {
                photoInfoRecord = realm.createObject(photoInfoRecord::class.java)
            }

            ModeInEdit.EDIT -> {

            }

        }
        photoInfoRecord.apply {
            stringContentUri = selectedPhotoInfo.stringContentUri
            dateTime = selectedPhotoInfo.dateTime
            latitude = selectedPhotoInfo.latitude
            longitude = selectedPhotoInfo.longitude
            location = latitude.toString() + longitude.toString()
            comment = inputComment.text.toString()
        }
        realm.commitTransaction()
        inputComment.setText("")
        Toast.makeText(this@EditActivity, getString(R.string.photo_info_written),Toast.LENGTH_SHORT).show()
        finish()

    }

    //AIzaSyBKNwqK-XYTMJVU7Kri79reDt5yItsGPi4

    // GoogleMapの起動
    private fun displayMap(latitude: Double, longitude: Double) {

        val geoString = "geo:" + latitude + "," + longitude + "?z=" + ZOOM_LEVEL
        val gmmIntentUri = Uri.parse(geoString)
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)

    }

    private fun launchCamera() {

        val contentFileName = SimpleDateFormat("yyyyMMdd_HHmmss_z").format(Date())

        contentURI = generateContentUriFromFileName(contentFileName)

        //カメラの起動(暗黙的インテント)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, contentURI)
        }

        //APIレベル21未満の場合の個別対応
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            val context = applicationContext
            val resolvedIntentActivities = context.packageManager
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolvedIntentInfo in resolvedIntentActivities){
                val packageName = resolvedIntentInfo.activityInfo.packageName
                context.grantUriPermission(packageName,contentURI,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }

        startActivityForResult(intent, RQ_CODE_CAMERA)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK ) {
            Toast.makeText(this@EditActivity, getString(R.string.shoot_failed), Toast.LENGTH_SHORT).show()
            return
        }

        if (requestCode != RQ_CODE_CAMERA) {
            Toast.makeText(this@EditActivity,getString(R.string.shoot_failed), Toast.LENGTH_SHORT).show()
            return
        }

        if (contentURI == null ) {
            Toast.makeText(this@EditActivity, getString(R.string.shoot_failed), Toast.LENGTH_SHORT).show()
            return
        }

        imageView.setImage(ImageSource.uri(contentURI))

        //DBへの登録情報の設定
        selectedPhotoInfo.stringContentUri = contentURI.toString()
        selectedPhotoInfo.dateTime = SimpleDateFormat("yyyyMMd_HHmmss_z").format(Date())

        //APIレベル21未満の場合の個別対応(設定した権限を取り除く)
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            applicationContext.revokeUriPermission(contentURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        getLocation()

    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {

        //ロケーションマネージャの取得
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // GPS設定がオンになっていない場合、GPS設定画面を起動
        if(!isGPSEnabled){
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,this)
        locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0f,this)

    }

    override fun onLocationChanged(location: Location) {

        // 緯度、軽度の設定
        selectedPhotoInfo.latitude = location.latitude
        selectedPhotoInfo.longitude = location.longitude
        Toast.makeText(this@EditActivity, getString(R.string.location_get), Toast.LENGTH_SHORT).show()

        // ロケーション取得を停止する
        locationManager?.removeUpdates(this)
        isGeoLocation = true
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

    private fun generateContentUriFromFileName(contentFileName: String?): Uri? {

        //フォルダの作成
        val contentFolder  = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), PHOTO_FOLDER_NAME)

        contentFolder.mkdirs()

        //ファイルのパス
        val contentFilePath = contentFolder.path + "/" + contentFileName + ".jpg"

        //ファイルオブジェクトの作成
        val contentFile = File(contentFilePath)

        // 共有用のURIを作成
        return FileProvider.getUriForFile(
                this@EditActivity,
                applicationContext.packageName + ".fileprovider",
                contentFile
        )

    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        outState?.putParcelable(IntentKey.CONTENT_URI.name, contentURI)

    }

    private fun permissionCheck() {

        //権限状況を取得する
        val permissionCheckCamera: Int = ContextCompat.checkSelfPermission(this@EditActivity,PERMISSION[0])
        val permissionCheckWriteStrage: Int = ContextCompat.checkSelfPermission(this@EditActivity,PERMISSION[1])
        val permissionCheckLocation: Int = ContextCompat.checkSelfPermission(this@EditActivity,PERMISSION[2])

        //権限があるのかの確認
        if (permissionCheckCamera == PackageManager.PERMISSION_GRANTED) isCameraEnabled = true
        if (permissionCheckCamera == PackageManager.PERMISSION_GRANTED) isExternalStrageEnabled = true
        if (permissionCheckCamera == PackageManager.PERMISSION_GRANTED) isLocationEnabled = true

        if (isCameraEnabled && isExternalStrageEnabled && isLocationEnabled) launchCamera() else permissionRequest()

    }

    private fun permissionRequest() {

        val isNeedCameraPermission = ActivityCompat.shouldShowRequestPermissionRationale(this@EditActivity,PERMISSION[0])
        val isNeedFilePermission = ActivityCompat.shouldShowRequestPermissionRationale(this@EditActivity,PERMISSION[1])
        val isNeedLocationPermission = ActivityCompat.shouldShowRequestPermissionRationale(this@EditActivity,PERMISSION[2])

        val isNeededPermission
                = if(isNeedCameraPermission || isNeedFilePermission || isNeedLocationPermission) true else false


        // 許可をリクエストする必要があるパーミッションを入れるリスト
        val requestPermissionList = ArrayList<String>()

        //許可状況を確認して、必要な許可をリストに追加する
        if(!isCameraEnabled) requestPermissionList.add(PERMISSION[0])
        if(!isExternalStrageEnabled) requestPermissionList.add(PERMISSION[1])
        if (!isLocationEnabled) requestPermissionList.add(PERMISSION[2])


        if (!isNeededPermission) {
            ActivityCompat.requestPermissions(
                    this@EditActivity,
                    requestPermissionList.toArray(arrayOfNulls(requestPermissionList.size)),
                    RQ_CODE_PERMISSION
            )
            return
        }

        // 確認ダイアログの表示
        val dialog = AlertDialog.Builder(this@EditActivity).apply {
            setTitle(getString(R.string.permission_request_title))
            setMessage(getString(R.string.permission_request_message))
            setPositiveButton(getString(R.string.admit)){ dialogInterface, i ->
                ActivityCompat.requestPermissions(
                        this@EditActivity,
                        requestPermissionList.toArray(arrayOfNulls(requestPermissionList.size)),
                        RQ_CODE_PERMISSION
                )

            }
            setNegativeButton(getString(R.string.reject)){ dialogInterface, i ->
                Toast.makeText(this@EditActivity, "Cannot go any further.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
            show()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        // 指定したリターンコードが返ってきているかどうか？
        if (requestCode != RQ_CODE_PERMISSION) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        // 結果が何もない場合
        if ( grantResults.size <= 0 ) return

        for (i in 0.. permissions.size -1 ) {
            when (permissions[i]){

                PERMISSION[0] -> {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this@EditActivity,getString(R.string.cannot_go_any_further),
                                Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                    isCameraEnabled = true
                }
                PERMISSION[1] -> {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this@EditActivity,getString(R.string.cannot_go_any_further),
                                Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                    isExternalStrageEnabled = true
                }
                PERMISSION[2] -> {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this@EditActivity,getString(R.string.cannot_go_any_further),
                                Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                    isLocationEnabled = true
                }
            }
        }

        if (isCameraEnabled && isExternalStrageEnabled && isLocationEnabled) launchCamera() else finish()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)

        menu.apply {
            findItem(R.id.action_settings).isVisible = true
            findItem(R.id.action_share).isVisible = false
            findItem(R.id.action_comment).isVisible = false
            findItem(R.id.action_delete).isVisible = true
            findItem(R.id.action_edit).isVisible = false
            findItem(R.id.action_camera).isVisible = if (mode == ModeInEdit.SHOOT) true else false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId){
            R.id.action_delete -> {
                when(mode){
                    ModeInEdit.SHOOT -> {
                        contentResolver.delete(Uri.parse(selectedPhotoInfo.stringContentUri),null,null)
                        Toast.makeText(this@EditActivity,getString(R.string.photo_info_deleted), Toast.LENGTH_SHORT).show()
                        finish()
                        return  true
                    }
                    ModeInEdit.EDIT -> {

                    }
                }
            }
            R.id.action_camera -> {
                inputComment.setText("")
                if (Build.VERSION.SDK_INT >= 23 ) permissionCheck() else launchCamera()
            }
            else -> return super.onOptionsItemSelected(item)

        }

        return true
    }


}
