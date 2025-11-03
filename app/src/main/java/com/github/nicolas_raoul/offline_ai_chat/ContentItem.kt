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

import android.net.Uri

sealed class ContentItem {
    data class TextItem(val text: String) : ContentItem() {
        companion object {
            fun fromRequest(text: String): TextItem {
                return TextItem(text)
            }
        }
    }

    data class ImageItem(val imageUri: Uri) : ContentItem()

    data class TextAndImagesItem(val text: String, val imageUris: List<Uri>) : ContentItem() {
        companion object {
            fun fromRequest(text: String, imageUris: List<Uri>): TextAndImagesItem {
                return TextAndImagesItem(text, imageUris)
            }
        }
    }

    data class TextWithPromptPrefixItem(val promptPrefix: String, val dynamicSuffix: String) :
        ContentItem() {
        companion object {
            fun fromRequest(promptPrefix: String, dynamicSuffix: String): TextWithPromptPrefixItem {
                return TextWithPromptPrefixItem(promptPrefix, dynamicSuffix)
            }
        }
    }
}
