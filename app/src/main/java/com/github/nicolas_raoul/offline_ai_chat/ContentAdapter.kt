/*
 * Copyright 2024 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nicolas_raoul.offline_ai_chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

/** A [RecyclerView.Adapter] for displaying the request and response views. */
class ContentAdapter : RecyclerView.Adapter<ContentAdapter.ViewHolder>() {
  private val contentList: MutableList<Pair<Int, String>> = ArrayList()

  fun addContent(viewType: Int, content: String?) {
    contentList.add(Pair(viewType, content))
    notifyDataSetChanged()
  }

  fun updateStreamingResponse(response: String) {
    contentList[contentList.size - 1] = Pair(VIEW_TYPE_RESPONSE, response)
    notifyDataSetChanged()
  }

  override fun getItemViewType(position: Int): Int {
    return contentList[position].first
  }

  override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
    val layoutId =
      when (viewType) {
        VIEW_TYPE_REQUEST -> R.layout.row_item_request
        VIEW_TYPE_RESPONSE -> R.layout.row_item_response
        VIEW_TYPE_RESPONSE_ERROR -> R.layout.row_item_response
        else -> throw IllegalArgumentException("Invalid view type $viewType")
      }

    val layoutInflater = LayoutInflater.from(viewGroup.context)
    val view = layoutInflater.inflate(layoutId, viewGroup, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val item = contentList[position]
    viewHolder.bind(item)

    // Set long-press listener for copying text
    viewHolder.contentTextView.setOnLongClickListener {
      val context = viewHolder.itemView.context
      val textToCopy = viewHolder.contentTextView.text.toString()
      val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clipData = ClipData.newPlainText("chat_content", textToCopy)
      clipboardManager.setPrimaryClip(clipData)

      Snackbar.make(viewHolder.itemView, "Copied to clipboard.", Snackbar.LENGTH_SHORT).show()
      true // Consume the long click
    }
  }

  override fun getItemCount(): Int {
    return contentList.size
  }

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val contentTextView: TextView // Made public for access in onBindViewHolder
    private val defaultTextColors: ColorStateList

    init {
      contentTextView = view.findViewById(R.id.content_text_view)
      defaultTextColors = contentTextView.textColors
    }

    fun bind(content: Pair<Int, String>) {
      contentTextView.text = content.second
      if (content.first == VIEW_TYPE_RESPONSE_ERROR) {
        contentTextView.setTextColor(Color.RED)
      } else {
        contentTextView.setTextColor(defaultTextColors)
      }
    }
  }

  companion object {
    const val VIEW_TYPE_REQUEST = 0
    const val VIEW_TYPE_RESPONSE = 1
    const val VIEW_TYPE_RESPONSE_ERROR = 2
  }
}
