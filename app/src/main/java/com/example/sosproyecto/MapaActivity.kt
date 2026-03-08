package com.example.sosproyecto

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private var selectMode = false
    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val heatmapCircles = mutableListOf<Circle>()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            enableMyLocation()
            moveToLastKnownLocation()
            googleMap?.let { renderHeatmap(it) }
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val toolbar = findViewById<Toolbar>(R.id.toolbarMapa)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        selectMode = intent.getBooleanExtra("select_mode", false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        googleMap?.setOnCameraIdleListener {
            googleMap?.let { renderHeatmap(it) }
        }

        googleMap?.setOnCircleClickListener { circle ->
            val score = circle.tag as? Int ?: 0
            Toast.makeText(this, "Puntaje de seguridad: $score%", Toast.LENGTH_LONG).show()
        }

        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
            moveToLastKnownLocation()
            googleMap?.let { renderHeatmap(it) }
        } else {
            requestPermissionLauncher.launch(LOCATION_PERMISSION)
        }

        if (selectMode) {
            googleMap?.setOnMapClickListener { latLng ->
                googleMap?.clear()
                googleMap?.addMarker(MarkerOptions().position(latLng))
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val d = FloatArray(1)
                            Location.distanceBetween(location.latitude, location.longitude, latLng.latitude, latLng.longitude, d)
                            val dist = d[0]
                            if (dist <=300f) {
                                val data = Intent().apply {
                                    putExtra("lat", latLng.latitude)
                                    putExtra("lng", latLng.longitude)
                                }
                                setResult(Activity.RESULT_OK, data)
                                finish()
                            } else {
                                Toast.makeText(this, "Estás demasiado lejos (%.0fm). Acércate a menos de 300m para reportar.".format(dist), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this, "No se pudo obtener tu ubicación actual", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (se: SecurityException) {
                    Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val default = LatLng(0.0, 0.0)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(default, 2f))
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap?.isMyLocationEnabled = true
            } catch (_: Throwable) {
            }
        }
    }

    private fun moveToLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && googleMap != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        } catch (e: SecurityException) {
        }
    }

    private fun renderHeatmap(map: GoogleMap) {
        heatmapCircles.forEach { it.remove() }
        heatmapCircles.clear()

        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val reportesJson = prefs.getString("reportes", null) ?: return
        val arr = JSONArray(reportesJson)

        val confirmsJson = prefs.getString("confirmaciones", null) ?: "[]"
        val confirmsArr = JSONArray(confirmsJson)
        val confirmCount = mutableMapOf<String, Int>()
        for (i in 0 until confirmsArr.length()) {
            val o = confirmsArr.getJSONObject(i)
            val rid = o.getString("idReporte")
            confirmCount[rid] = (confirmCount[rid] ?: 0) + 1
        }

        val grid = mutableMapOf<Pair<Int, Int>, Double>()
        val cellSizeDeg = 0.001
        var maxWeight = 0.0

        val dateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val now = Date()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val lat = o.getDouble("latitud")
            val lng = o.getDouble("longitud")
            val rid = o.getString("idReporte")
            val fechaStr = o.getString("fechaReporte")
            var ageDays = 0.0
            try {
                val fecha = dateFmt.parse(fechaStr)
                val diff = now.time - (fecha?.time ?: now.time)
                ageDays = diff.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
            } catch (_: Throwable) {}

            val confirms = confirmCount[rid] ?: 0
            val weightBase = 1.0 + confirms * 0.5
            val timeFactor = kotlin.math.max(0.1, 1.0 - (ageDays / 30.0))
            val weight = weightBase * timeFactor

            val key = Pair((lat / cellSizeDeg).toInt(), (lng / cellSizeDeg).toInt())
            val newW = (grid[key] ?: 0.0) + weight
            grid[key] = newW
            if (newW > maxWeight) maxWeight = newW
        }

        if (maxWeight <= 0.0) return

        for ((key, w) in grid) {
            val centerLat = (key.first + 0.5) * cellSizeDeg
            val centerLng = (key.second + 0.5) * cellSizeDeg
            val center = LatLng(centerLat, centerLng)

            val norm = w / maxWeight // 0..1
            val color = when {
                norm >= 0.66 -> 0x88FF0000.toInt()
                norm >= 0.33 -> 0x88FFA500.toInt()
                else -> 0x88FFFF00.toInt()
            }
            val securityScore = (100 - (norm * 100)).toInt().coerceIn(0, 100)

            val circle = map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(100.0)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(color)
                    .clickable(true)
            )
            circle.tag = securityScore
            heatmapCircles.add(circle)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_mapa, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_usuarios -> startActivity(Intent(this, UsuariosActivity::class.java))
            R.id.menu_reportes -> startActivity(Intent(this, ReportesActivity::class.java))
            R.id.menu_tipos -> startActivity(Intent(this, TiposIncidenteActivity::class.java))
            R.id.menu_evidencias -> startActivity(Intent(this, EvidenciasActivity::class.java))
            R.id.menu_confirmaciones -> startActivity(Intent(this, ConfirmacionesActivity::class.java))
            R.id.menu_comentarios -> startActivity(Intent(this, ComentariosActivity::class.java))
            R.id.menu_emergencias -> startActivity(Intent(this, EmergenciasActivity::class.java))
            R.id.menu_contactos -> startActivity(Intent(this, ContactosEmergenciaActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
