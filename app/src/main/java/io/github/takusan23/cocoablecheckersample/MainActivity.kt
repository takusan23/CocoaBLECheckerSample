package io.github.takusan23.cocoablecheckersample

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import io.github.takusan23.cocoablecheckersample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /** 権限コールバック */
    private val permissionCallBack = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // お許しをもらった
            start()
        }
    }

    /** ViewBinding */
    private val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        when {
            !isSupportedBLE() -> {
                // BLE非対応
                finish()
                Toast.makeText(this, "BLE未対応端末では利用できません", Toast.LENGTH_SHORT).show()
            }
            !isEnableBluetooth() -> {
                // BluetoothがOFF
                finish()
                Toast.makeText(this, "Bluetoothを有効にしてください", Toast.LENGTH_SHORT).show()
            }
            !isGrantedAccessFineLocationPermission() -> {
                // パーミッションがない。リクエストする
                requestAccessFineLocationPermission()
            }
            else -> {
                // 検出開始
                viewBinding.activityMainStartButton.setOnClickListener {
                    start()
                }
            }
        }
    }

    /** BLE端末の検出を始める。10秒後に終了する */
    private fun start() {
        // 結果を入れる配列
        val resultList = arrayListOf<ScanResult>()
        // BLE端末を検出したら呼ばれるコールバック
        val bleCallBack = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                // 配列に追加
                if (result?.scanRecord?.serviceUuids?.get(0)?.uuid?.toString() == "0000fd6f-0000-1000-8000-00805f9b34fb") {
                    resultList.add(result)
                }
            }
        }
        // スキャン開始
        bluetoothAdapter.bluetoothLeScanner.startScan(bleCallBack)
        // くるくる
        viewBinding.activityMainProgressBar.isVisible = true
        // 10秒後に終了
        Handler(Looper.getMainLooper()).postDelayed(10 * 1000) {
            // 止める
            bluetoothAdapter.bluetoothLeScanner.stopScan(bleCallBack)
            // 重複を消す
            val finalList = resultList.distinctBy { scanResult -> scanResult.device?.address }
            // 結果
            viewBinding.activityMainProgressBar.isVisible = false
            // 電波強度
            val singalText = finalList.joinToString(separator = "\n") { scanResult -> "${scanResult.rssi} dBm" }
            // TextViewに表示
            viewBinding.activityMainCountTextView.text = """
COCOAインストール台数
およそ ${finalList.size} 台
--- 電波強度 ---
$singalText
            """.trimIndent()
        }
    }

    /** android.permission.ACCESS_FINE_LOCATION 権限があるかどうか */
    private fun isGrantedAccessFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /** android.permission.ACCESS_FINE_LOCATION 権限を貰いに行く */
    private fun requestAccessFineLocationPermission() {
        permissionCallBack.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** Bluetoothが有効ならtrue */
    private fun isEnableBluetooth(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    /** BLE対応時はtrueを返す */
    private fun isSupportedBLE(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

}