/*
 * Copyright 2019-2020 Chair of Geoinformatics, Technical University of Munich
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

package io.rtron.model.roadspaces.topology.junction

import io.rtron.model.roadspaces.ModelIdentifier
import io.rtron.model.roadspaces.ModelIdentifierInterface


/**
 * Junction identifier interface required for class delegation.
 */
interface JunctionIdentifierInterface {
    val junctionId: String
}


/**
 * Identifier of a [Junction].
 *
 * @param junctionId id of the junction
 * @param modelIdentifier identifier of the model
 */
data class JunctionIdentifier(
        override val junctionId: String,
        val modelIdentifier: ModelIdentifier
): JunctionIdentifierInterface, ModelIdentifierInterface by modelIdentifier {

    // Conversions
    override fun toString(): String {
        return "JunctionIdentifier(junctionId=$junctionId)"
    }
}
