package kotlin_challenge.test.co.jp.footprint

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.GridLayout
import io.realm.Realm
import io.realm.RealmResults
import kotlin_challenge.test.co.jp.footprint.common.IntentKey
import kotlin_challenge.test.co.jp.footprint.common.ModeInEdit
import kotlin_challenge.test.co.jp.footprint.model.PhotoInfo
import kotlinx.android.synthetic.main.activity_gallary.*
import kotlinx.android.synthetic.main.content_gallary.*

class GallaryActivity : AppCompatActivity() {

    lateinit var realm: Realm
    lateinit var results: RealmResults<PhotoInfo>

    private fun getSelectedLocation(): String {

        val selectedLatitude = intent.extras.getDouble(IntentKey.LATITUDE.name)
        val selectedLongitude = intent.extras.getDouble(IntentKey.LONGITUDE.name)

        return selectedLatitude.toString() + selectedLongitude.toString()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_camera -> {

                val intent = Intent(this@GallaryActivity, EditActivity::class.java).apply {
                    putExtra(IntentKey.EDIT_MODE.name, ModeInEdit.SHOOT)
                }
                startActivity(intent)

            }

            else -> {
                super.onOptionsItemSelected(item)
            }

        }

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)

        menu.apply {
            findItem(R.id.action_settings).isVisible = true
            findItem(R.id.action_share).isVisible = false
            findItem(R.id.action_comment).isVisible = false
            findItem(R.id.action_delete).isVisible = false
            findItem(R.id.action_edit).isVisible = false
            findItem(R.id.action_camera).isVisible = true
        }
        return true

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallary)
        setSupportActionBar(toolbar)

        toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            setNavigationOnClickListener {
                finish()
            }
        }

    }

    override fun onResume() {
        super.onResume()

        val selectedLocation: String = getSelectedLocation()

        realm = Realm.getDefaultInstance()
        results = realm.where(PhotoInfo::class.java)
                .equalTo(PhotoInfo::location.name, selectedLocation)
                .findAllSorted(PhotoInfo::dateTime.name)

        setGallary(results)

    }

    private fun setGallary(results: RealmResults<PhotoInfo>?) {

        // 画面の縦横を判断
        val screenOrientation = resources.configuration.orientation

        myRecyclerView.layoutManager = if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
            GridLayoutManager(this, 2)
        } else {
            GridLayoutManager(this, 4)
        }

        // RecyclerViewの設定
        val adapter = MyRecyclerViewAdapter(results!!)
        myRecyclerView.adapter = adapter

    }
}


