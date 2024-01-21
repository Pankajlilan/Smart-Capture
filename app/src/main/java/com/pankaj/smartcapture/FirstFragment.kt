package com.pankaj.smartcapture

import android.app.Activity.RESULT_OK
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.Text.TextBlock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pankaj.smartcapture.databinding.BottomSheetLayoutBinding
import com.pankaj.smartcapture.databinding.FragmentFirstBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class FirstFragment : Fragment(), ImageAdapter.OnItemClickListener {
  
  private lateinit var layoutManager: LinearLayoutManager
  private lateinit var images: MutableList<ImageModel>
  private var _binding: FragmentFirstBinding? = null
  
  private val binding get() = _binding!!
  private lateinit var imageAdapter: ImageAdapter
  private lateinit var selectedImageModel: ImageModel
  
  private var labels: List<ImageLabel> = listOf()
  private var descriptionTextBlocks: List<TextBlock> = listOf()
  
  private var deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
    registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
      if (result.resultCode === RESULT_OK) {
        selectImage(images[refreshThePosition(layoutManager)])
        binding.bannerImageView.setImageURI(selectedImageModel.contentUri)
        Toast.makeText(context, "Image deleted.", Toast.LENGTH_SHORT).show()
      }
    }
  
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentFirstBinding.inflate(inflater, container, false)
    return binding.root
  }
  
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    binding.recyclerView.layoutManager = layoutManager
    binding.recyclerView.addItemDecoration(CenterItemDecoration())
    imageAdapter = ImageAdapter()
    imageAdapter.setOnItemClickListener(this)
    
    val pagerSnapHelper = LinearSnapHelper()
    pagerSnapHelper.attachToRecyclerView(binding.recyclerView)
    binding.recyclerView.onFlingListener = pagerSnapHelper
    binding.recyclerView.adapter = imageAdapter
    
    binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      var isScrolling = false
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        when (newState) {
          RecyclerView.SCROLL_STATE_IDLE -> {
            // Scroll has finished
            if (isScrolling) {
              val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
              val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
              val centerPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2
              
              Log.i("POSITION: ", "FIRST ITEM: $firstVisibleItemPosition")
              Log.i("POSITION: ", "CENTER ITEM: $centerPosition")
              Log.i("POSITION: ", "LAST ITEM: $lastVisibleItemPosition")
              
              if (images.isNotEmpty()) {
                if (firstVisibleItemPosition == 0 && centerPosition == 1 && lastVisibleItemPosition == 2) {
                  // RecyclerView has reached the first item
                  binding.bannerImageView.setImageURI(images[firstVisibleItemPosition].contentUri)
                  selectImage(images[firstVisibleItemPosition])
                  return
                }
                if (lastVisibleItemPosition == images.size && centerPosition == images.size - 1 && firstVisibleItemPosition == images.size - 2) {
                  // RecyclerView has reached the last item
                  Toast.makeText(requireContext(), "Reached Last Item", Toast.LENGTH_SHORT).show()
                  selectImage(images[lastVisibleItemPosition])
                  return
                }
                selectImage(images[centerPosition])
                binding.bannerImageView.setImageURI(images[centerPosition].contentUri)
              }
              isScrolling = false
            }
          }
          
          else -> {
            // Scroll is in progress
            isScrolling = true
          }
        }
      }
    })
    binding.navView.setOnItemSelectedListener {
      when (it.itemId) {
        R.id.navigation_share -> {
          shareUri(selectedImageModel.contentUri)
          true
        }
        
        R.id.navigation_info -> {
          showCustomBottomSheetDialog()
          true
        }
        
        R.id.navigation_delete -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteImageAPI30(requireContext(), selectedImageModel)
          } else {
            deleteImageAPI29(requireContext(), selectedImageModel.contentUri)
          }
          true
        }
        
        else -> false
      }
    }
  }
  
  private fun showCustomBottomSheetDialog() {
    LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_layout, null)
    
    val bottomSheetDialog = BottomSheetDialog(requireContext())
    val inflater = LayoutInflater.from(requireContext())
    val bsBinding = BottomSheetLayoutBinding.inflate(inflater)
    bottomSheetDialog.setContentView(bsBinding.root)
    
    bsBinding.btnAddChip.setOnClickListener {
      val newLabel = bsBinding.collectionsEditText.text.toString()
      if (newLabel.isNotEmpty()) {
        addButton(bsBinding.chipGroup, newLabel)
        bsBinding.collectionsEditText.text.clear()
      }
      
    }
    
    var descriptionText = ""
    if (descriptionTextBlocks.isNotEmpty()) {
      bsBinding.descriptionTv.visibility = View.VISIBLE
      bsBinding.descriptionHeading.visibility = View.VISIBLE
      for (description in descriptionTextBlocks) {
        descriptionText += description.text + ",\n"
      }
      bsBinding.descriptionTv.text = descriptionText
    } else {
      bsBinding.descriptionTv.visibility = View.GONE
      bsBinding.descriptionHeading.visibility = View.GONE
    }
    
    for (label in labels) {
      addButton(bsBinding.chipGroup, label.text)
    }
    
    bsBinding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
      val chip = group.findViewById<Chip>(checkedId)
      if (chip != null) {
        // Chip is selected, handle deletion or other actions
        (chip.parent as? ChipGroup)?.removeView(chip)
      }
    }
    bottomSheetDialog.show()
  }
  
  private fun addButton(chipGroup: ChipGroup, label: String) {
    val chip = Chip(requireContext())
    chip.text = label
    chip.isCloseIconVisible = true
    chip.setOnCloseIconClickListener {
      chipGroup.removeView(chip)
    }
    chipGroup.addView(chip)
  }
  
  private fun deleteImageAPI29(context: Context, uri: Uri?) {
    val resolver = context.contentResolver
    try {
      resolver.delete(uri!!, null, null)
    } catch (securityException: SecurityException) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val recoverableSecurityException = securityException as RecoverableSecurityException
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(
          recoverableSecurityException.userAction
            .actionIntent.intentSender
        ).build()
        deleteResultLauncher.launch(senderRequest) //Use of ActivityResultLauncher
      }
    }
  }
  
  @RequiresApi(Build.VERSION_CODES.R)
  fun deleteImageAPI30(
    context: Context,
    arrayList: ImageModel
  ) {
    val contentResolver = context.contentResolver
    val arrayList2: ArrayList<Uri?> = ArrayList()
    arrayList2.add(arrayList.contentUri) // You need to use the Uri you got using ContentUris.withAppendedId() method
    Collections.addAll(arrayList2)
    val intentSender = MediaStore.createDeleteRequest(contentResolver, arrayList2).intentSender
    val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
      .setFillInIntent(null)
      .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
      .build()
    deleteResultLauncher.launch(senderRequest)
  }
  
  private fun refreshThePosition(layoutManager: LinearLayoutManager): Int {
    // Get the current position of the last visible item
    val currentVisiblePosition = layoutManager.findLastVisibleItemPosition()
    if (currentVisiblePosition != RecyclerView.NO_POSITION && currentVisiblePosition < images.size) {
      // Remove the item at the current position
      images.removeAt(currentVisiblePosition - 2)
      
      // Notify the adapter about the item removal
      imageAdapter.notifyItemRemoved(currentVisiblePosition - 2)
      
      // Optionally, update the current position if needed
      // For example, if you want to handle the case where the last item is deleted
      val updatedPosition = if (currentVisiblePosition >= images.size) {
        // If the last item is deleted, move to the previous item
        images.size - 1
      } else {
        currentVisiblePosition - 2
      }
      
      return updatedPosition
    }
    return 0
  }
  
  override fun onResume() {
    loadImagesFromGallery()
    super.onResume()
  }
  
  private fun detectImage() {
    // When using Latin script library
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(
      MediaStore.Images.Media.getBitmap(
        requireContext().contentResolver,
        selectedImageModel.contentUri
      ), 0
    )
    descriptionTextBlocks = listOf()
    labels = listOf()
    
    lifecycleScope.launch(Dispatchers.IO) {
      recognizer.process(image)
        .addOnSuccessListener { visionText ->
          descriptionTextBlocks = visionText.textBlocks
        }
        .addOnFailureListener {
          Toast.makeText(requireContext(), "Did not Detected Text", Toast.LENGTH_SHORT).show()
        }
      
      labeler.process(image)
        .addOnSuccessListener { visionText ->
          labels = visionText
        }
        .addOnFailureListener {
          Toast.makeText(requireContext(), "Did not Detected Image", Toast.LENGTH_SHORT).show()
        }
      
    }
  }
  
  private fun loadImagesFromGallery() {
    lifecycleScope.launch(Dispatchers.Main) {
      images = loadImagesFromGalleryAsync(requireContext())
      imageAdapter.setImages(images)
      
      if (!::selectedImageModel.isInitialized && images.isNotEmpty()) {
        selectImage(images[0])
        binding.bannerImageView.setImageURI(selectedImageModel.contentUri)
      } else if (layoutManager.findFirstVisibleItemPosition() == 0) {
        selectImage(images[0])
        binding.bannerImageView.setImageURI(selectedImageModel.contentUri)
      }
    }
  }
  
  private fun shareUri(uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.setType("image/*")
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    requireContext().startActivity(intent)
  }
  
  private suspend fun loadImagesFromGalleryAsync(context: Context): MutableList<ImageModel> {
    return withContext(Dispatchers.IO) {
      val images = mutableListOf<ImageModel>()
      
      val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME
      )
      
      val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
      val selection = "${MediaStore.Images.Media.DATA} like '%Screenshots%'"
      
      val query = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder
      )
      
      query?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        
        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumn)
          val displayName = cursor.getString(displayNameColumn)
          val contentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
          )
          images.add(ImageModel(id, displayName, contentUri))
        }
      }
      images
    }
  }
  
  private fun selectImage(image: ImageModel) {
    selectedImageModel = image
    detectImage()
  }
  
  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
  
  override fun onItemClick(image: ImageModel, position: Int) {
    when (position) {
      1 -> scrollToPositionAndCenter(position)
      else -> scrollToPositionAndCenter(position - 2)
    }
  }
  
  private fun scrollToPositionAndCenter(position: Int) {
    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
      override fun getHorizontalSnapPreference(): Int {
        return SNAP_TO_START
      }
    }
    smoothScroller.targetPosition = position
    layoutManager.startSmoothScroll(smoothScroller)
  }
}
