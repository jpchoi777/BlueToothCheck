package com.bluetooth.chart.activity

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetooth.chart.R
import com.bluetooth.chart.adapter.DeviceAdapter
import com.bluetooth.chart.databinding.ActivityMainBinding
import com.bluetooth.chart.module.BlueToothParingUtil
import com.bluetooth.chart.module.data.BlueToothInfoModel
import com.bluetooth.chart.module.interface_callback.PermissionResultCallback

class MainActivity : AppCompatActivity() {
    final lateinit var binding: ActivityMainBinding

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceList = mutableListOf<BlueToothInfoModel>()
    private lateinit var deviceAdapter: DeviceAdapter
    private var blueToothInfoModel : BlueToothInfoModel? = null

    private  var TAG = "TAG_Ble"
    // 블루투스 장치 검색 브로드캐스트 리시버
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 10)

                return
            }

            if (BluetoothDevice.ACTION_FOUND == action) {
                val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
               /* val bondState = intent.getStringExtra(BluetoothDevice.EXTRA_BOND_STATE)
                val bluetoothName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                val bluetoothUUID = intent.getStringExtra(BluetoothDevice.EXTRA_UUID)

*/

                var device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                var uuids: Array<ParcelUuid>? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    uuids =  intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID, BluetoothDevice::class.java) as Array<ParcelUuid>?
                } else {
                    uuids =  intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)as Array<ParcelUuid>?
                }


                device?.let {
                    val deviceName = it.name ?: "Unknown Device"
                    val deviceAddress = it.address
                    val deviceInfo = "$deviceName - $deviceAddress"
                    val deviceMajorClass = it.bluetoothClass?.majorDeviceClass ?: "Unknown Major Class"
                    val deviceClass = it.bluetoothClass?.deviceClass ?: "Unknown Class"
                    val bluetoothPairingVariant = intent.getStringExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT)
                    val bondState = when (it.bondState) {
                        BluetoothDevice.BOND_BONDED -> "Paired"
                        BluetoothDevice.BOND_BONDING -> "Pairing"
                        BluetoothDevice.BOND_NONE -> "Not Paired"
                        else -> "Unknown"
                    }

                    var bluetoothUUID = ""

                    val uuids = device.uuids
                    if(uuids == null){
                        // UUID 목록이 없으면 SDP 요청
                        device.fetchUuidsWithSdp()
                    }
                    uuids?.forEach {
                        bluetoothUUID = it.uuid.toString()
                        // UUID 정보를 표시할 수 있습니다
                    }


                    var data = BlueToothInfoModel()
                    data.rssi = rssi.toString()
                    data.name = deviceName
                    data.orignName = deviceName ?: ""
                    data.bondState = bondState ?:""
                    data.deviceClass = deviceClass.toString()
                    data.pairingvariant = bluetoothPairingVariant ?:""
                    data.uuid = bluetoothUUID
                    data.address = deviceAddress
                    data.deviceMajorClass = deviceMajorClass.toString()
                    deviceList.add(data)
                    Log.d(TAG, "onReceive: $bondState ")
                    Log.d(TAG, "onReceive: $rssi ")
                    Log.d(TAG, "onReceive: $bluetoothUUID [$uuids] ")
                    Log.d(TAG, "onReceive: ${it.uuids} ")
                    Log.d(TAG, "onReceive: $bluetoothPairingVariant ")

                    Log.d(TAG, "onReceive: ${deviceList.toList()} ")

                    deviceAdapter.notifyDataSetChanged()


                }
            }
        }
    }

    // 페어링 상태 변화 감지 (브로드캐스트 리시버)
    private val pairingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)

                when (bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        // 페어링 완료 후 연결 시작
                        Toast.makeText(context, "Device paired", Toast.LENGTH_SHORT).show()
                        device?.let {
                            BlueToothParingUtil.connectToDevice(this@MainActivity,device)
                        }
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Toast.makeText(context, "Pairing in progress", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Toast.makeText(context, "Pairing failed or removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 블루투스 어댑터 초기화
        // 블루투스 어댑터 초기화
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        // 블루투스가 켜져 있는지 확인
        binding.btnBlueToothServiceConnect.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        if (!bluetoothAdapter.isEnabled) {
            binding.btnBlueToothServiceConnect.visibility = View.VISIBLE
            val activityLauncher  = openActivityResultLauncher()
            binding.btnBlueToothServiceConnect.setOnClickListener {
                var enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)


                CheckPermissionManager.checkAndRequestPermissions(this@MainActivity,object : PermissionResultCallback{
                    override fun onPermissionsGranted() {
                        activityLauncher.launch(enableBtIntent)
                    }

                    override fun onPermissionsDenied() {
                    }

                })

            }
        }else{
            binding.recyclerView.visibility = View.VISIBLE
        }


        deviceAdapter = DeviceAdapter(deviceList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = deviceAdapter
        deviceAdapter.adapterListener = object : DeviceAdapter.AdapterListener{
            override fun onItemClick(item : BlueToothInfoModel, position: Int) {
                blueToothInfoModel  = item
                for (i in 0 until deviceList.size) {
                    deviceList[i].isOpen = false

                }
                deviceList[position].isOpen = true
                deviceAdapter.notifyDataSetChanged()
            }

            override fun onItemParing(item: BlueToothInfoModel, position: Int) {
                blueToothInfoModel  = item

                if (blueToothInfoModel == null){
                    Toast.makeText(this@MainActivity,"불루투스 가 선택되지 않았습니다.",Toast.LENGTH_SHORT).show()
                    return
                }
                val intent = Intent(this@MainActivity,BlueToothDetailChartActivity::class.java)
                intent.putExtra("blueToothData", blueToothInfoModel)
                startActivity(intent)

                /*val device = bluetoothAdapter.getRemoteDevice(item.address)
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 11)
                    return
                }
                if(device.bondState != BluetoothDevice.BOND_BONDED){
                    BlueToothParingUtil.pairDevice(device)
                }else{
                    BlueToothParingUtil.connectToDevice(this@MainActivity,device)

                }*/
            }

        }

        // 블루투스 장치 검색
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        binding.btnInformation.setOnClickListener {
            if (blueToothInfoModel == null){
                Toast.makeText(this@MainActivity,"불루투스 가 선택되지 않았습니다.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity,BlueToothInformationActivity::class.java)
            intent.putExtra("blueToothData", blueToothInfoModel)
            startActivity(intent)
        }

        binding.btnScanner.setOnClickListener {
            deviceList.clear()

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN), 10)
                return@setOnClickListener
            }
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()


        }
        deviceList.clear()
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN), 10)
            return
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }
    private fun openActivityResultLauncher(): ActivityResultLauncher<Intent> {
        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "수신 성공", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "수신 실패", Toast.LENGTH_SHORT).show()
            }
        }
        return resultLauncher
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        CheckPermissionManager.handlePermissionsResult(requestCode, permissions,
            grantResults, this@MainActivity, object : PermissionResultCallback{
                override fun onPermissionsGranted() {
                    // 장치 검색 시작
                    if (bluetoothAdapter.isEnabled) {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_SCAN
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {

                            return
                        }
                        if (bluetoothAdapter.isDiscovering) {
                            bluetoothAdapter.cancelDiscovery()
                        }
                        bluetoothAdapter.startDiscovery()
                    }
                }

                override fun onPermissionsDenied() {
                }

            })


    }

    override fun onStart() {
        super.onStart()
        // 페어링 상태 변경 리시버 등록
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(pairingReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        // 리시버 해제
        unregisterReceiver(pairingReceiver)
    }
}