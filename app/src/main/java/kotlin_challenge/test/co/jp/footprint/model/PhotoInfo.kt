package kotlin_challenge.test.co.jp.footprint.model

import io.realm.RealmObject

open class PhotoInfo : RealmObject() {

    //写真のContentURI
    var stringContentUri : String = ""

    // 撮影日時
    var dateTime: String = ""

    // 緯度
    var latitude : Double = 0.0

    // 軽度
    var longitude : Double = 0.0

    // 地点
    var location : String = ""

    // コメント
    var comment : String = ""
}