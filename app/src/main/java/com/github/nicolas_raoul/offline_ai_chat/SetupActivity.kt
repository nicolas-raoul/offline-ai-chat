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
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.GenAiFeatures
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var downloadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        statusTextView = findViewById(R.id.status_text_view)
        downloadButton = findViewById(R.id.download_button)

        downloadButton.setOnClickListener {
            downloadModel()
        }

        checkModelStatus()
    }

    private fun checkModelStatus() {
        lifecycleScope.launch {
            val isAvailable = GenAiFeatures.getGenerativeModelManager().isModelAvailable()
            if (isAvailable) {
                launchMainActivity()
            } else {
                statusTextView.text = "Model not downloaded. Please download the model to continue."
                downloadButton.isEnabled = true
            }
        }
    }

    private fun downloadModel() {
        downloadButton.isEnabled = false
        statusTextView.text = "Downloading model..."

        lifecycleScope.launch {
            GenAiFeatures.getGenerativeModelManager().downloadModel(
                object : com.google.mlkit.genai.common.DownloadCallback {
                    override fun onDownloadCompleted() {
                        statusTextView.text = "Model downloaded successfully."
                        launchMainActivity()
                    }

                    override fun onDownloadFailed(e: GenAiException) {
                        statusTextView.text = "Model download failed: ${e.message}"
                        downloadButton.isEnabled = true
                    }

                    override fun onDownloadProgress(totalBytesDownloaded: Long) {
                        statusTextView.text = "Downloading model... $totalBytesDownloaded bytes"
                    }

                    override fun onDownloadStarted(bytesToDownload: Long) {
                        statusTextView.text = "Download started... $bytesToDownload bytes"
                    }
                }
            )
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
