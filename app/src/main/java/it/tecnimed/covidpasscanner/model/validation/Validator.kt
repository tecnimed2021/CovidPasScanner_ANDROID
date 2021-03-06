/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2022 T-Systems International GmbH and all other contributors
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
 *  Created by nicolamcornelio on 1/19/22, 9:31 AM
 */

package it.tecnimed.covidpasscanner.model.validation

import it.tecnimed.covidpasscanner.model.CertificateModel
import it.tecnimed.covidpasscanner.model.CertificateStatus
import it.tecnimed.covidpasscanner.model.ScanMode
import it.tecnimed.covidpasscanner.model.TestModel

class Validator {

    companion object {
        private fun checkPreconditions(certificateModel: CertificateModel): CertificateStatus? {
            if (certificateModel.certificateIdentifier.isEmpty()) return CertificateStatus.NOT_EU_DCC
            if (!certificateModel.isValid) {
                return if (certificateModel.isCborDecoded) CertificateStatus.NOT_VALID else
                    CertificateStatus.NOT_EU_DCC
            }
            if (certificateModel.isBlackListed) return CertificateStatus.NOT_VALID
            if (certificateModel.isRevoked) return CertificateStatus.REVOKED
            if (certificateModel.scanMode == ScanMode.DOUBLE_SCAN && !certificateModel.hasTests()) return CertificateStatus.NOT_VALID
            return null
        }

        fun validate(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
            val certificateStatus = checkPreconditions(certificateModel)
            certificateStatus?.let {
                return certificateStatus
            }

            val validationStrategy = ValidationStrategyFactory.getValidationStrategy(certificateModel)
            return validationStrategy?.checkCertificate(certificateModel, ruleSet) ?: CertificateStatus.NOT_VALID
        }
    }
}