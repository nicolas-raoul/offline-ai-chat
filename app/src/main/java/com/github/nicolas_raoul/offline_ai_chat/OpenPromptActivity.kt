/*
* Copyright 2025 Google LLC. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.github.nicolas_raoul.offline_ai_chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.prompt.Candidate
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import java.io.IOException
import kotlinx.coroutines.launch

/**
* An activity that demonstrates a chat-like interface for the Open Prompt API, allowing requests
* with both text and images, and including generation configuration.
*/
class OpenPromptActivity : BaseActivity() {
    private val ACTION_CLEAR_CACHES = 1000

    private var generativeModel: GenerativeModel? = null
    private lateinit var requestEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var selectImageButton: ImageButton
    private lateinit var imagePreview: ImageView
    private lateinit var configButton: Button
    private lateinit var prefixEditText: EditText

    private var selectedImageUri: Uri? = null

    private var curTemperature: Float? = null
    private var curTopK: Int? = null
    private var curSeed: Int? = null
    private var curMaxOutputTokens: Int? = null
    private var curCandidateCount: Int? = null
    private var useDefaultConfig = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                imagePreview.setImageURI(uri)
                imagePreview.visibility = View.VISIBLE
                Toast.makeText(this, "1 image selected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_openprompt)
        requestEditText = findViewById(R.id.request_edit_text)
        sendButton = findViewById(R.id.send_button)
        selectImageButton = findViewById(R.id.select_image_prompt_button)
        imagePreview = findViewById(R.id.image_thumbnail_preview_input)
        configButton = findViewById(R.id.config_button)
        prefixEditText = findViewById(R.id.prefix_edit_text)

        selectImageButton.setOnClickListener { pickImageLauncher.launch("image/*") }

        // Remove the selected image when the user clicks on the image preview.
        imagePreview.setOnClickListener {
            selectedImageUri = null
            imagePreview.visibility = View.GONE
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
        }

        configButton.setOnClickListener { GenerationConfigDialog().show(supportFragmentManager, null) }

        sendButton.setOnClickListener {
            val requestText = requestEditText.text.toString().trim()
            if (TextUtils.isEmpty(requestText)) {
                Toast.makeText(this, R.string.input_message_is_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefixText = prefixEditText.text.toString().trim()
            if (!TextUtils.isEmpty(prefixText) && selectedImageUri != null) {
                Toast.makeText(this, R.string.warning_prefix_used_with_image, Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            val requestItem: ContentItem =
                if (selectedImageUri != null) {
                    ContentItem.TextAndImagesItem.fromRequest(
                        requestText,
                        arrayListOf(selectedImageUri!!)
                    )
                } else if (!TextUtils.isEmpty(prefixText)) {
                    ContentItem.TextWithPromptPrefixItem.fromRequest(prefixText, requestText)
                } else {
                    ContentItem.TextItem.fromRequest(requestText)
                }
            // onSend(requestItem)

            requestEditText.setText("")
            imagePreview.visibility = View.GONE
            selectedImageUri = null
        }

        initGenerator()
    }

    private fun createGenerateContentRequest(request: ContentItem): GenerateContentRequest {
        var requestText = ""
        var promptPrefixText = ""
        var imageBitmap: Bitmap? = null

        when (request) {
            is ContentItem.TextItem -> {
                requestText = request.text
            }
            is ContentItem.TextAndImagesItem -> {
                requestText = request.text
                for (uri in request.imageUris) {
                    try {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)?.let { bitmap -> imageBitmap = bitmap }
                        }
                    } catch (e: IOException) {
                        Log.e("OpenPromptActivity", "Error decoding image URI: $uri", e)
                    }
                }
            }
            is ContentItem.ImageItem -> {
                try {
                    contentResolver.openInputStream(request.imageUri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.let { bitmap -> imageBitmap = bitmap }
                    }
                } catch (e: IOException) {
                    Log.e("OpenPromptActivity", "Error decoding image URI: ${request.imageUri}", e)
                }
            }
            is ContentItem.TextWithPromptPrefixItem -> {
                requestText = request.dynamicSuffix
                promptPrefixText = request.promptPrefix
            }
        }

        return if (imageBitmap != null) {
            generateContentRequest(ImagePart(imageBitmap!!), TextPart(requestText)) {
                temperature = curTemperature
                topK = curTopK
                seed = curSeed
                maxOutputTokens = curMaxOutputTokens
                candidateCount = curCandidateCount
            }
        } else {
            generateContentRequest(TextPart(requestText)) {
                promptPrefix = PromptPrefix(promptPrefixText)
                temperature = curTemperature
                topK = curTopK
                seed = curSeed
                maxOutputTokens = curMaxOutputTokens
                candidateCount = curCandidateCount
            }
        }
    }

    private fun resultToStrings(result: GenerateContentResponse): List<String> =
        result.candidates.map { candidate ->
            val text = candidate.text
            if (candidate.finishReason == Candidate.FinishReason.MAX_TOKENS) {
                "$text\n(FinishReason: MAX_TOKENS)"
            } else {
                text
            }
        }

    private fun initGenerator() {
        generativeModel?.close()
        generativeModel = com.google.mlkit.genai.prompt.Generation.getClient()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!super.onCreateOptionsMenu(menu)) {
            return false
        }
        menu.add(Menu.NONE, ACTION_CLEAR_CACHES, Menu.NONE, "Clear all prefix caches")
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_about)?.apply {
            isVisible = true
            isChecked = useDefaultConfig
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            ACTION_CLEAR_CACHES -> {
                lifecycleScope.launch {
                    generativeModel?.clearCaches()
                    Toast.makeText(this@OpenPromptActivity, "Caches cleared", Toast.LENGTH_SHORT)
                        .show()
                }
                return true
            }
            R.id.action_about -> {
                val newState = !item.isChecked
                item.isChecked = newState
                GenerationConfigUtils.setUseDefaultConfig(applicationContext, newState)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
