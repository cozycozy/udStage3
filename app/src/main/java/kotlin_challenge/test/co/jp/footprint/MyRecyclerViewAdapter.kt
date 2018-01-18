package kotlin_challenge.test.co.jp.footprint

import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import io.realm.RealmResults
import kotlin_challenge.test.co.jp.footprint.model.PhotoInfo

/**
 * Created by k_mitake on 2018/01/18.
 */

class MyRecyclerViewAdapter(val result: RealmResults<PhotoInfo>)
    : RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {

        val selectedPhotUri = result[position].stringContentUri

        //todo 画像表示
        //holder!!.imageSelectedLocationPhoto.setImageURI(Uri.parse(selectedPhotUri))
        Glide.with(MyApplication.mContext).load(selectedPhotUri).into(holder!!.imageSelectedLocationPhoto)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.gallary_photo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return result.size
    }

    inner class ViewHolder(val v : View) : RecyclerView.ViewHolder(v) {

        val imageSelectedLocationPhoto : ImageView

        init {
            imageSelectedLocationPhoto = v.findViewById(R.id.imageSeletedLocationPhoto)
        }

    }

}