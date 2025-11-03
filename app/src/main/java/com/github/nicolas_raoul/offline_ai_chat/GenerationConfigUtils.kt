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

import android.content.Context

object GenerationConfigUtils {
    fun getUseDefaultConfig(context: Context): Boolean {
        return true
    }

    fun setUseDefaultConfig(context: Context, useDefaultConfig: Boolean) {
        // No-op
    }

    fun getTemperature(context: Context): Float {
        return 0.5f
    }

    fun getTopK(context: Context): Int {
        return 50
    }

    fun getSeed(context: Context): Int {
        return 1
    }

    fun getCandidateCount(context: Context): Int {
        return 1
    }

    fun getMaxOutputTokens(context: Context): Int {
        return 512
    }
}
