package com.pankaj.smartcapture

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.pankaj.smartcapture.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
  
  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding
  
  private val activityResultLauncher =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    )
    { permissions ->
      // Handle Permission granted/rejected
      permissions.entries.forEach {
        Log.d("MainActivity", "${it.key} = ${it.value}")
      }
    }
  
  @RequiresApi(Build.VERSION_CODES.M)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    
    setSupportActionBar(binding.toolbar)
    
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)
    
    requestForStoragePermissions()
    if (ContextCompat.checkSelfPermission(
        applicationContext, READ_EXTERNAL_STORAGE
      ) ==
      PackageManager.PERMISSION_GRANTED
    ) {
    } else if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
      // In an educational UI, explain to the user why your app requires this
      // permission for a specific feature to behave as expected. In this UI,
      // include a "cancel" or "no thanks" button that allows the user to
      // continue using your app without granting the permission.
      
    } else {
      // You can directly ask for the permission.
      // The registered ActivityResultCallback gets the result of this request.
      activityResultLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE))
    }
    
    CoroutineScope(Dispatchers.IO).launch {
      delay(1000)
      activityResultLauncher.launch(
        arrayOf(
          READ_EXTERNAL_STORAGE,
          WRITE_EXTERNAL_STORAGE,
          READ_MEDIA_IMAGES
        )
      )
    }
  }
  
  private fun requestForStoragePermissions() {
    activityResultLauncher.launch(
      arrayOf(arrayOf(READ_EXTERNAL_STORAGE).toString())
    )
  }
  
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.navigation_share -> true
      else -> super.onOptionsItemSelected(item)
    }
  }
  
  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp(appBarConfiguration)
        || super.onSupportNavigateUp()
  }
}