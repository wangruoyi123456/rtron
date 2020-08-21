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

package io.rtron.model.opendrive.road.lanes

import io.rtron.model.opendrive.common.DataQuality
import io.rtron.model.opendrive.common.Include
import io.rtron.model.opendrive.common.UserData


data class RoadLanes(

        var laneOffset: List<RoadLanesLaneOffset> = listOf(),
        var laneSection: List<RoadLanesLaneSection> = listOf(),

        var userData: List<UserData> = listOf(),
        var include: List<Include> = listOf(),
        var dataQuality: DataQuality = DataQuality()
) {

    // Methods
    fun containsLaneOffset() = laneOffset.isNotEmpty()

    /**
     * Returns the list of lane offsets with the first entry starting at s=0.0.
     * Sometimes OpenDRIVE datasets contain an entry at s=0.0, sometimes the first entry starts at e.g. 3.0.
     */
    fun laneOffsetsWithStartingEntry(): List<RoadLanesLaneOffset> = when {
        laneOffset.isEmpty() -> laneOffset
        laneOffset.first().s > 0.0 -> listOf(RoadLanesLaneOffset.ZERO) + laneOffset
        else -> laneOffset
    }
}
