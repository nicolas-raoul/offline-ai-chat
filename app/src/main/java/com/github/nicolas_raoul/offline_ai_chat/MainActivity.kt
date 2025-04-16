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

import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.github.nicolas_raoul.offline_ai_chat.ContentAdapter
import com.github.nicolas_raoul.offline_ai_chat.R
import com.google.ai.edge.aicore.generationConfig
import java.util.concurrent.Future
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.future.future

/** Demonstrates the AICore SDK usage from Kotlin. */
class MainActivity : AppCompatActivity() {

  private var requestEditText: EditText? = null
  private var sendButton: Button? = null
  private var contentRecyclerView: RecyclerView? = null
  private var model: GenerativeModel? = null
  private var inGenerating = false
  private var generateContentFuture: Future<Unit>? = null

  private val contentAdapter = ContentAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

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

  override fun onDestroy() {
    super.onDestroy()
    model?.close()
  }

  private fun initGenerativeModel() {
    model =
      GenerativeModel(
        generationConfig {
          context = applicationContext
          temperature = 0.2f
          topK = 16
          maxOutputTokens = 256
        }
      )
  }

  private fun generateContent(request: String) {
    generateContentFuture =
      lifecycleScope.future {
        try {
          var hasFirstStreamingResult = false
          var result = ""
          model!!
            .generateContentStream(request)
            .onCompletion { endGeneratingUi() }
            .collect { response ->
              run {
                result += response.text
                if (hasFirstStreamingResult) {
                  contentAdapter.updateStreamingResponse(result)
                } else {
                  contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE, result)
                  hasFirstStreamingResult = true
                }
              }
            }
        } catch (e: GenerativeAIException) {
          contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE_ERROR, e.message!!)
          endGeneratingUi()
        }
      }
  }

  private fun startGeneratingUi() {
    sendButton?.setText(R.string.button_cancel)
    requestEditText?.setText(R.string.empty)
  }

  private fun endGeneratingUi() {
    sendButton?.setText(R.string.button_send)
    contentRecyclerView?.smoothScrollToPosition(contentAdapter.itemCount - 1)
  }
}
