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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.nicolas_raoul.offline_ai_chat.ContentAdapter
import com.github.nicolas_raoul.offline_ai_chat.R
import com.google.mlkit.genai.common.GenerativeAIException
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import java.util.concurrent.Future
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

  private var requestEditText: EditText? = null
  private var sendButton: Button? = null
  private var contentRecyclerView: RecyclerView? = null
  private var model: GenerativeModel? = null
  
  /**
   * Tracks whether a generation process is currently active.
   */
  private var inGenerating = false
  private var generateContentFuture: Future<Unit>? = null

  private val contentAdapter = ContentAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Show app icon in title bar
    supportActionBar?.setDisplayShowHomeEnabled(true)
    supportActionBar?.setLogo(R.mipmap.ic_launcher)
    supportActionBar?.setDisplayUseLogoEnabled(true)

    requestEditText = findViewById(R.id.request_edit_text)
    sendButton = findViewById(R.id.send_button)
    sendButton!!.setOnClickListener {
      if (inGenerating) {
        generateContentFuture?.cancel(true)
        endGeneratingUi()
      } else {
        val request = requestEditText?.text.toString()
        if (TextUtils.isEmpty(request)) {
          Toast.makeText(this, R.string.prompt_is_empty, Toast.LENGTH_SHORT).show()
          return@setOnClickListener
        }

        contentAdapter.addContent(ContentAdapter.VIEW_TYPE_REQUEST, request)
        startGeneratingUi()
        generateContent(request)
      }
      inGenerating = !inGenerating
    }

    contentRecyclerView = findViewById<RecyclerView>(R.id.content_recycler_view)
    contentRecyclerView!!.layoutManager = LinearLayoutManager(this)
    contentRecyclerView!!.adapter = contentAdapter

    initGenerativeModel()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.main_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_about -> {
        val message = getString(R.string.about_content)
        val spannableString = SpannableString(message)
        val url = "https://github.com/nicolas-raoul/offline-ai-chat"
        val startIndex = message.indexOf(url)
        val endIndex = startIndex + url.length

        val clickableSpan = object : ClickableSpan() {
          override fun onClick(widget: View) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
          }
        }

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val dialog = android.app.AlertDialog.Builder(this)
          .setTitle(R.string.app_name)
          .setMessage(spannableString)
          .setPositiveButton(android.R.string.ok, null)
          .show()

        // Make the text view clickable
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    model?.close()
  }

  private fun initGenerativeModel() {
    model = Generation.getClient()
  }

  private fun generateContent(request: String) {
    generateContentFuture =
      lifecycleScope.future {
        try {
          var hasFirstStreamingResult = false
          var result = ""
          val genRequest =
              generateContentRequest(TextPart(request)) {
                temperature = 0.2f
                topK = 16
                maxOutputTokens = 256
              }
          model!!
            .generateContentStream(genRequest)
            .onCompletion { endGeneratingUi() }
            .collect { response ->
              run {
                result += response.candidates.first().text
                if (hasFirstStreamingResult) {
                  contentAdapter.updateStreamingResponse(result)
                } else {
                  contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE, result)
                  hasFirstStreamingResult = true
                }
              }
            }
        } catch (e: GenerativeAIException) {
          android.util.Log.e("Offline AI chat MainActivity", "AICore failed: ${e.message}")
          endGeneratingUi()
        }
        inGenerating = false
      }
  }

  private fun startGeneratingUi() {
    sendButton?.setText(R.string.button_cancel)
    requestEditText?.setText(R.string.empty)
  }

  private fun endGeneratingUi() {
    sendButton?.setText(R.string.button_generate)
    contentRecyclerView?.smoothScrollToPosition(contentAdapter.itemCount - 1)
  }
}
