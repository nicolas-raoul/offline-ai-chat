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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.prompt.Generation
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
            val generativeModel = Generation.getClient()
            val isAvailable = generativeModel.checkStatus() == 0
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
            val generativeModel = Generation.getClient()
            try {
                generativeModel.download().collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadStarted ->
                            statusTextView.text = "Download started... ${status.bytesToDownload} bytes"
                        is DownloadStatus.DownloadProgress ->
                            statusTextView.text = "Downloading model... ${status.totalBytesDownloaded} bytes"
                        is DownloadStatus.DownloadFailed -> {
                            statusTextView.text = "Model download failed: ${status.e.message}"
                            downloadButton.isEnabled = true
                        }
                        is DownloadStatus.DownloadCompleted -> {
                            statusTextView.text = "Model downloaded successfully."
                            launchMainActivity()
                        }
                    }
                }
            } catch (e: Exception) {
                statusTextView.text = "Model download failed: ${e.message}"
                downloadButton.isEnabled = true
            }
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
