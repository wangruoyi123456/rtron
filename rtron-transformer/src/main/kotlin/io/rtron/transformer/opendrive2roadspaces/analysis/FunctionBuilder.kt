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

package io.rtron.transformer.opendrive2roadspaces.analysis

import io.rtron.io.logging.Logger
import io.rtron.math.analysis.function.univariate.UnivariateFunction
import io.rtron.math.analysis.function.univariate.combination.ConcatenatedFunction
import io.rtron.math.analysis.function.univariate.combination.SectionedUnivariateFunction
import io.rtron.math.analysis.function.univariate.combination.StackedFunction
import io.rtron.math.analysis.function.univariate.pure.LinearFunction
import io.rtron.math.geometry.euclidean.threed.curve.Curve3D
import io.rtron.model.opendrive.road.lanes.RoadLanes
import io.rtron.model.opendrive.road.lanes.RoadLanesLaneSectionLRLaneWidth
import io.rtron.model.opendrive.road.lateralprofile.RoadLateralProfileShape
import io.rtron.model.opendrive.road.lateralprofile.RoadLateralProfileSuperelevation
import io.rtron.model.opendrive.road.objects.RoadObjectsObjectRepeat
import io.rtron.model.roadspaces.roadspace.RoadspaceIdentifier
import io.rtron.model.roadspaces.roadspace.road.LaneIdentifier
import io.rtron.std.filterToStrictSortingBy
import io.rtron.transformer.opendrive2roadspaces.parameter.Opendrive2RoadspacesParameters


/**
 * Builder for functions of the OpenDRIVE data model.
 */
class FunctionBuilder(
        private val reportLogger: Logger,
        private val parameters: Opendrive2RoadspacesParameters
) {

    // Methods

    /**
     * Builds a function that describes the torsion of the road reference line.
     *
     * @param srcSuperelevation entries containing coefficients for polynomial functions
     */
    fun buildCurveTorsion(id: RoadspaceIdentifier, srcSuperelevation: List<RoadLateralProfileSuperelevation>):
            UnivariateFunction {
        if (srcSuperelevation.isEmpty()) return LinearFunction.X_AXIS

        val superelevationEntriesAdjusted = srcSuperelevation
                .filterToStrictSortingBy { it.s }
        if (superelevationEntriesAdjusted.size < srcSuperelevation.size)
            this.reportLogger.info("Removing superelevation entries which are not placed in strict order" +
                    " according to s.", id.toString())

        return ConcatenatedFunction.ofPolynomialFunctions(
                superelevationEntriesAdjusted.map { it.s },
                superelevationEntriesAdjusted.map { it.coefficients },
                prependConstant = true,
                prependConstantValue = 0.0)
    }

    /**
     * Builds a function that describes one lateral entry of a road's shape.
     *
     * @param srcRoadLateralProfileShape the cross-sectional profile of a road at a certain curve position
     */
    fun buildLateralShape(id: RoadspaceIdentifier, srcRoadLateralProfileShape: List<RoadLateralProfileShape>):
            UnivariateFunction {
        require(srcRoadLateralProfileShape.isNotEmpty())
        { "Lateral profile shape must contain elements in order to build a univariate function." }
        require(srcRoadLateralProfileShape.all { it.s == srcRoadLateralProfileShape.first().s })
        { "All lateral profile shape elements must have the same curve position." }

        val lateralProfileEntriesAdjusted = srcRoadLateralProfileShape
                .filterToStrictSortingBy { it.t }
        if (lateralProfileEntriesAdjusted.size < srcRoadLateralProfileShape.size)
            this.reportLogger.info("Removing lateral profile entries which are not placed in strict order " +
                    "according to t.", id.toString())

        return ConcatenatedFunction.ofPolynomialFunctions(
                lateralProfileEntriesAdjusted.map { it.t },
                lateralProfileEntriesAdjusted.map { it.coefficients },
                prependConstant = true)
    }

    /**
     * Builds a function that described the lateral lane offset to the road reference line.
     */
    fun buildLaneOffset(id: RoadspaceIdentifier, srcLanes: RoadLanes): UnivariateFunction {
        if (srcLanes.laneOffset.isEmpty()) return LinearFunction.X_AXIS

        val laneOffsetEntriesAdjusted = srcLanes.laneOffset.filterToStrictSortingBy { it.s }
        if (laneOffsetEntriesAdjusted.size < srcLanes.laneOffset.size)
            this.reportLogger.info("Removing lane offset entries which are not placed in strict order " +
                    "according to s.", id.toString())

        return ConcatenatedFunction.ofPolynomialFunctions(
                laneOffsetEntriesAdjusted.map { it.s },
                laneOffsetEntriesAdjusted.map { it.coefficients },
                prependConstant = true,
                prependConstantValue = 0.0)
    }


    /**
     * Builds a function that describes the lane width.
     *
     * @param srcLaneWidthEntries entries containing coefficients for polynomial functions
     * @param id identifier of the lane, required for logging output
     * @return function describing the width of a lane
     */
    fun buildLaneWidth(id: LaneIdentifier, srcLaneWidthEntries: List<RoadLanesLaneSectionLRLaneWidth>):
            UnivariateFunction {
        if (srcLaneWidthEntries.isEmpty()) {
            this.reportLogger.info("The lane does not contain any width entries. " +
                    "Continuing with a zero width.", id.toString())
            return LinearFunction.X_AXIS
        }

        if (srcLaneWidthEntries.first().sOffset > 0.0)
            this.reportLogger.info("The width should be defined for the full length of the lane section and" +
                    " thus must also be defined for s=0.0. Not defined positions are interpreted with a width of 0.",
                    id.toString())

        val widthEntriesAdjusted = srcLaneWidthEntries
                .filterToStrictSortingBy { it.sOffset }
        if (widthEntriesAdjusted.size < srcLaneWidthEntries.size)
            this.reportLogger.info("Removing width entries which are not in strict order according to sOffset.",
                    id.toString())

        return ConcatenatedFunction.ofPolynomialFunctions(
                widthEntriesAdjusted.map { it.sOffset },
                widthEntriesAdjusted.map { it.coefficients },
                prependConstant = true,
                prependConstantValue = 0.0)
    }

    /**
     * Returns the absolute height function of a [RoadObjectsObjectRepeat] object. Therefore a linear function is build
     * for the zOffsets and is added to the height function of the [roadReferenceLine].
     *
     * @param srcRepeat object for which the height function shall be constructed
     * @param roadReferenceLine road's height
     * @return function of the object's absolute height
     */
    fun buildStackedHeightFunctionFromRepeat(srcRepeat: RoadObjectsObjectRepeat, roadReferenceLine: Curve3D):
            StackedFunction {

        val heightFunctionSection = SectionedUnivariateFunction(
                roadReferenceLine.heightFunction, srcRepeat.getRoadReferenceLineParameterSection())
        return StackedFunction.ofSum(heightFunctionSection, srcRepeat.getHeightOffsetFunction())
    }

}
