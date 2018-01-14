package kotlin_challenge.test.co.jp.footprint.common

class Constants {
}


enum class ModeInEdit {
    SHOOT, EDIT
}

enum class IntentKey {
    EDIT_MODE, CONTENT_URI
}

//パーミッションの許可を求めるためのリクエストコード
val RQ_CODE_PERMISSION = 200

//保存先のフォルダ名
val PHOTO_FOLDER_NAME = "FOOTPRINT"

//カメラ起動時のリクエストコード
val RQ_CODE_CAMERA : Int = 100

val ZOOM_LEVEL : Int = 15