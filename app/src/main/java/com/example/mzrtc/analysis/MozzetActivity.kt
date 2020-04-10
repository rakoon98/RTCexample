//package com.example.mzrtc.analysis
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.mzrtc.R
//import com.example.mzrtc.testsampletry.view.vns.VSActivity
//import com.example.mzrtc.utils.setLogDebug
//import kotlinx.android.synthetic.main.activity_main.*
//
//class MozzetActivity: AppCompatActivity() {
//
//
//    companion object {
//        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
//        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
//    }
//
//    val mozzetManager = MozzetManager()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//
//        // renderer 초기화
//        mozzetManager.onCameraPermissionGranted()
//
//        peerConnectionManager.rendererInit(local_view, remote_view)
////        peerConnectionManager.remoteRenderer = remote_view
////        peerConnectionManager.localRenderer = local_view
//
//        checkCameraPermission()
//    }
//
//    // 카메라 권한
//    private fun checkCameraPermission(){
//        setLogDebug("checkCameraPermission")
//        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED ){
//            requestCameraPermission()
//        } else {
//            vsViewModel?.onCameraPermissionGranted()
//        }
//    }
//
//
//    /** Camera Permission Request Granted & Denied  **/
//    private fun requestCameraPermission(dialogShown: Boolean = false) {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) && !dialogShown) {
//            showPermissionRationaleDialog()
//        } else {
//            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_REQUEST_CODE)
//        }
//    }
//
//    private fun showPermissionRationaleDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Camera Permission Required")
//            .setMessage("This app need the camera to function")
//            .setPositiveButton("Grant") { dialog, _ ->
//                dialog.dismiss()
//                requestCameraPermission(true)
//            }
//            .setNegativeButton("Deny") { dialog, _ ->
//                dialog.dismiss()
//                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG)
//            }
//            .show()
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//            if(requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it==PackageManager.PERMISSION_GRANTED }){
//            onCameraPermissionGranted()
//        } else {
//            Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG)
//        }
//    }
//
//
//
//
//
//}