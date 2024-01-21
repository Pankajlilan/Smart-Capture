package com.pankaj.smartcapture

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CenterItemDecoration : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    val itemPosition = parent.getChildAdapterPosition(view)
    val layoutManager = parent.layoutManager as LinearLayoutManager
    
    when (itemPosition) {
      0 -> {
        // Center the first item and consider spacing
        val offset = calculateCenterOffset(parent, view)
        outRect.set(offset, 0, 0, 0)
      }
      
      layoutManager.itemCount - 1 -> {
        // Center the last item and consider spacing
        val offset = calculateCenterOffset(parent, view)
        outRect.set(0, 0, offset, 0)
      }
      
      else -> {
        outRect.set(0, 0, 0, 0)
      }
    }
  }
  
  private fun calculateCenterOffset(parent: RecyclerView, view: View): Int {
    return (parent.width - view.width) / 2
  }
}
