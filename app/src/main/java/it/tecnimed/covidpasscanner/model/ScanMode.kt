/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by kaizen-7 on 23/12/21, 10:32
 */

package it.tecnimed.covidpasscanner.model

enum class ScanMode(val value: String) {
    STANDARD("3G"),
    STRENGTHENED("2G"),
    BOOSTER("BOOSTED"),
    SCHOOL("SCHOOL"),
    WORK("WORK"),
    ENTRY_ITALY("ENTRY_ITALY"),
    DOUBLE_SCAN("DOUBLE_SCAN");

    companion object {
        fun from(s: String?): ScanMode = values().find { it.value == s } ?: STANDARD
    }
}
