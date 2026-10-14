package com.example.backgroundservices

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else{
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private lateinit var txtstatus : TextView
    private lateinit var btnStart: Button

    private lateinit var statustextview : TextView
    private lateinit var btnStartLatihan : Button
    
    private fun hasAllPermissions(): Boolean{
        return  permissionsToRequest.all { 
            ContextCompat.checkSelfPermission(this,it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun startTrackerService(){
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this,serviceIntent)
        txtstatus.text = "Background service sedang berjalan..."
        statustextview.text = "Latihan sedang berjalan..."
    }
    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP = "nit2x.paba.backgroundservice.ACTION_SERVICE_STOPPED_JUMP"
        const val REQUEST_CODE_PERMISSIONS = 101
        const val REQUEST_CODE_WORKOUT_PERMISSIONS = 102
        const val EXTRA_JUMP_COUNT = "extra_jump_count"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                val serviceIntent = Intent(this, TrackerService::class.java)
                startForegroundService(serviceIntent)
                finish()
            }
            else{
                txtstatus.text = "Izin ditolak, service tidak akan dijalankan."
            }
        }

        else if (requestCode == REQUEST_CODE_WORKOUT_PERMISSIONS) {
            val allWorkoutPermissionsGranted =
                grantResults.isNotEmpty() &&
                        grantResults.all {
                            it == PackageManager.PERMISSION_GRANTED
                        }

            if (allWorkoutPermissionsGranted) {
                startTrackerService()
            } else {
                statustextview.text =
                    "Izin ditolak, latihan tidak dapat dimulai."
            }
        }
    }

    private fun handleIntent(intent: Intent?){
        val incomingIntent = intent ?: return
        if (incomingIntent.action == ACTION_SERVICE_STOPPED_JUMP) {
            val count = incomingIntent.getIntExtra(EXTRA_JUMP_COUNT, 0)
            txtstatus.text = "Background service sudah berhenti."
            statustextview.text = "Latihan selesai!\nJumlah lompatan: $count"

            incomingIntent.action = null
            setIntent(incomingIntent)

            openWorkoutCalendarEvent(count)
        }
    }

    private fun openWorkoutCalendarEvent(count: Int) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 30 * 60 * 1000L

        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI

            putExtra(
                CalendarContract.Events.TITLE,
                "Latihan Hari Ini"
            )

            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Latihan selesai dengan $count lompatan terdeteksi."
            )

            putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                startTime
            )

            putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                endTime
            )
        }

        try {
            startActivity(calendarIntent)
        } catch (e: ActivityNotFoundException) {
            statustextview.text =
                "Latihan selesai! Jumlah lompatan: $count\n" +
                        "Aplikasi Kalender tidak ditemukan."
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtstatus = findViewById<TextView>(R.id.txtstatus)
        btnStart = findViewById<Button>(R.id.btnStart)

        statustextview = findViewById<TextView>(R.id.statustextview)
        btnStartLatihan = findViewById<Button>(R.id.btnStartLatihan)

        btnStart.setOnClickListener {
            if (hasAllPermissions()){
                startTrackerService()
            } else{
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnStartLatihan.setOnClickListener {
            if (hasAllPermissions()){
                startTrackerService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest,
                    REQUEST_CODE_WORKOUT_PERMISSIONS
                )
            }
        }

        handleIntent(intent)
    }
}