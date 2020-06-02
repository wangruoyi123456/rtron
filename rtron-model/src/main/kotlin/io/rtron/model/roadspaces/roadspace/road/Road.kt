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

package io.rtron.model.roadspaces.roadspace.road

import com.github.kittinunf.result.Result
import io.rtron.math.analysis.function.univariate.UnivariateFunction
import io.rtron.math.analysis.function.univariate.combination.SectionedUnivariateFunction
import io.rtron.math.analysis.function.univariate.combination.StackedFunction
import io.rtron.math.geometry.curved.threed.surface.AbstractCurveRelativeSurface3D
import io.rtron.math.geometry.curved.threed.surface.SectionedCurveRelativeParametricSurface3D
import io.rtron.math.geometry.euclidean.threed.curve.AbstractCurve3D
import io.rtron.math.geometry.euclidean.threed.curve.CurveOnParametricSurface3D
import io.rtron.math.geometry.euclidean.threed.point.fuzzyEquals
import io.rtron.math.geometry.euclidean.threed.surface.AbstractSurface3D
import io.rtron.math.geometry.euclidean.threed.surface.CompositeSurface3D
import io.rtron.math.geometry.euclidean.threed.surface.LinearRing3D
import io.rtron.math.range.Range
import io.rtron.model.roadspaces.roadspace.RoadspaceIdentifier
import io.rtron.model.roadspaces.roadspace.attribute.AttributeList
import io.rtron.std.getValueResult
import io.rtron.std.handleFailure
import io.rtron.std.isSortedBy


/**
 * Representation of an actual road (without road objects) containing the surface and information on the lane topology
 * as well as attributes.
 *
 * @param id identifier of this road (each roadspace has exactly one road)
 * @param surface surface geometry of the road with torsion applied
 * @param surfaceWithoutTorsion surface geometry of the road without torsion applied
 * @param laneOffset lateral lane offset to road reference line
 * @param laneSections lane sections of this road
 */
class Road(
        val id: RoadspaceIdentifier,
        val surface: AbstractCurveRelativeSurface3D,
        val surfaceWithoutTorsion: AbstractCurveRelativeSurface3D,
        val laneOffset: UnivariateFunction,
        private val laneSections: List<LaneSection>
) {

    // Properties and Initializers
    init {
        require(surface.domain == surfaceWithoutTorsion.domain)
        { "Domains of provided surfaces must have the same domain." }
        require(curvePositionDomain.hasLowerBound() && curvePositionDomain.hasUpperBound())
        { "Domain of curve position must have upper and lower bounds." }
        require(laneSections.isNotEmpty()) { "Road must contain laneSections." }
        require(laneSections.mapIndexed { index, laneSection -> index to laneSection }
                .all { it.first == it.second.id.laneSectionId })
        { "LaneSection elements must be positioned according to their laneSection id on the list." }

        val expectedLaneIds = laneSections.indices.toList()
        require(laneSections.indices.toList().containsAll(expectedLaneIds))
        { "There must be no gaps within the given laneSectionIds." }
        require(laneSections.isSortedBy { it.id.laneSectionId }) { "LaneSections have to be sorted." }

        assert(getLaneSectionCurvePositionDomains().isNotEmpty())
        { "The domains of the lane sections must not be empty." }
    }

    /** domain of the curve positions of this road */
    private val curvePositionDomain get() = surface.domain

    /** road surface sectioned into the domains of the lane sections */
    private val sectionedSurfaces: List<AbstractCurveRelativeSurface3D> =
            getLaneSectionCurvePositionDomains().map { SectionedCurveRelativeParametricSurface3D(surface, it) }

    /** road surface without torsion sectioned into the domains of the lane sections */
    private val sectionedSurfacesWithoutTorsion: List<AbstractCurveRelativeSurface3D> =
            getLaneSectionCurvePositionDomains().map { SectionedCurveRelativeParametricSurface3D(surfaceWithoutTorsion, it) }

    /** lateral lane offset function sectioned into the domains of the lane sections */
    private val sectionedLaneOffset: List<UnivariateFunction> =
            getLaneSectionCurvePositionDomains().map { SectionedUnivariateFunction(laneOffset, it) }


    // Methods

    /**
     * Returns the identifiers of all lanes as a flattened list.
     */
    fun getAllLaneIdentifiers(): List<LaneIdentifier> = laneSections.flatMap { it.lanes.values }.map { it.id }

    /**
     * Returns the lane reference line which is a laterally translated road reference line.
     */
    fun getLaneReferenceLine(): AbstractCurve3D = CurveOnParametricSurface3D(surface, laneOffset)

    /**
     * Returns the lane section with the [laneSectionIdentifier]; if it does not exist, an [Result.Failure] is returned.
     */
    fun getLaneSection(laneSectionIdentifier: LaneSectionIdentifier) =
            laneSections.getValueResult(laneSectionIdentifier.laneSectionId)

    private fun getLane(laneIdentifier: LaneIdentifier): Result<Lane, IllegalArgumentException> =
            getLaneSection(laneIdentifier.laneSectionIdentifier)
                    .handleFailure { return it }
                    .getLane(laneIdentifier.laneId)

    /**
     * Returns all lane surfaces contained in this road.
     *
     * @param step discretization step size
     * @return a triple of the lane identifier, the lane surface geometry and the lane's attribute list
     */
    fun getAllLanes(step: Double): List<Triple<LaneIdentifier, AbstractSurface3D, AttributeList>> =
            getAllLaneIdentifiers().map { id ->
                val laneSurface = getLaneSurface(id, step).handleFailure { throw it.error }
                val attributes = getAttributeList(id).handleFailure { throw it.error }
                Triple(id, laneSurface, attributes)
            }

    /**
     * Returns the left boundary of all lanes contained in this road.
     *
     * @return a triple of the lane identifier, the curve geometry and the lane's id attribute list
     */
    fun getAllLeftLaneBoundaries(): List<Triple<LaneIdentifier, AbstractCurve3D, AttributeList>> =
            getAllLaneIdentifiers().map { id ->
                val curve = getLeftLaneBoundaries(id).handleFailure { throw it.error }
                val attributes = getLane(id).handleFailure { throw it.error }.idAttributes
                Triple(id, curve, attributes)
            }

    /**
     * Returns the right boundary of all lanes contained in this road.
     *
     * @return a triple of the lane identifier, the curve geometry and the lane's id attribute list
     */
    fun getAllRightLaneBoundaries(): List<Triple<LaneIdentifier, AbstractCurve3D, AttributeList>> =
            getAllLaneIdentifiers().map { id ->
                val curve = getRightLaneBoundary(id).handleFailure { throw it.error }
                val attributes = getLane(id).handleFailure { throw it.error }.idAttributes
                Triple(id, curve, attributes)
            }

    /**
     * Returns a lane curve for all lanes contained in this road.
     *
     * @param factor if the factor is 0.0, the inner lane boundary is returned; if the factor is 1.0, the outer
     * lane boundary is returned; if the factor is 0.5, the center line of the lane is returned
     * @return a triple of the lane identifier, the curve geometry and the lane's id attribute list
     */
    fun getAllCurvesOnLanes(factor: Double): List<Triple<LaneIdentifier, AbstractCurve3D, AttributeList>> =
            getAllLaneIdentifiers().map { id ->
                val curve = getCurveOnLane(id, factor).handleFailure { throw it.error }
                val attributes = getLane(id).handleFailure { throw it.error }.idAttributes
                Triple(id, curve, attributes)
            }

    /**
     * Returns all filler surfaces of this road.
     *
     * @param step discretization step size
     */
    fun getAllFillerSurfaces(step: Double): List<AbstractSurface3D> =
            laneSections
                    .map { getAllFillerSurfaces(it.id, step) }
                    .handleFailure { throw it.error }
                    .flatten()

    /**
     * Returns all filler surfaces of a lane section.
     *
     * @param id identifier of requested lane section
     * @param step discretization step size
     */
    fun getAllFillerSurfaces(id: LaneSectionIdentifier, step: Double): Result<List<AbstractSurface3D>, Exception> =
            getLaneSection(id)
                    .handleFailure { return it }
                    .laneList
                    .map { it.id }
                    .dropLast(1)
                    .flatMap { curId -> getLeftLateralFillerSurface(curId, step).handleFailure { throw it.error } }
                    .let { Result.success(it) }

    /**
     * Returns the left boundary of an individual lane with [laneIdentifier].
     */
    fun getLeftLaneBoundaries(laneIdentifier: LaneIdentifier): Result<AbstractCurve3D, Exception> =
            if (laneIdentifier.laneId > 0) getCurveOnLane(laneIdentifier, 1.0)
            else getCurveOnLane(laneIdentifier, 0.0)

    /**
     * Returns the right boundary of an individual lane with [laneIdentifier].
     */
    fun getRightLaneBoundary(laneIdentifier: LaneIdentifier): Result<AbstractCurve3D, Exception> =
            if (laneIdentifier.laneId > 0) getCurveOnLane(laneIdentifier, 0.0)
            else getCurveOnLane(laneIdentifier, 1.0)

    /**
     * Returns a curve that lies on the road surface and is parallel to the lane boundaries
     *
     * @param laneIdentifier identifier for requested lane
     * @param factor if the factor is 0.0, the inner lane boundary is returned; if the factor is 1.0, the outer
     * lane boundary is returned; if the factor is 0.5, the center line of the lane is returned
     */
    private fun getCurveOnLane(laneIdentifier: LaneIdentifier, factor: Double): Result<AbstractCurve3D, Exception> {
        // select the requested lane
        val selectedLaneSection = getLaneSection(laneIdentifier.laneSectionIdentifier)
                .handleFailure { return it }
        val selectedLane = selectedLaneSection.getLane(laneIdentifier.laneId)
                .handleFailure { return it }

        // select the correct surface and section it
        val sectionedSurface =
                if (selectedLane.level) sectionedSurfacesWithoutTorsion[laneIdentifier.laneSectionId]
                else sectionedSurfaces[laneIdentifier.laneSectionId]

        // calculate the total lateral offset function to the road's reference line
        val sectionedLaneReferenceOffset = sectionedLaneOffset[laneIdentifier.laneSectionId]
        val lateralLaneOffset = selectedLaneSection
                .getLateralLaneOffset(laneIdentifier.laneId, factor)
                .handleFailure { return it }
        val lateralOffset = StackedFunction.ofSum(sectionedLaneReferenceOffset, lateralLaneOffset)

        // calculate the additional height offset for the specific factor
        val heightLaneOffset = selectedLaneSection
                .getLaneHeightOffset(laneIdentifier, factor)
                .handleFailure { return it }

        // combine it to a curve on the sectioned road surface
        val curveOnLane = CurveOnParametricSurface3D(sectionedSurface, lateralOffset, heightLaneOffset)
        return Result.success(curveOnLane)
    }

    /**
     * Returns the surface of an individual lane with [laneIdentifier] and a certain discretization [step] size.
     */
    fun getLaneSurface(laneIdentifier: LaneIdentifier, step: Double): Result<AbstractSurface3D, Exception> {
        val leftBoundary = getLeftLaneBoundaries(laneIdentifier)
                .handleFailure { return it }
                .calculatePointListGlobalCS(step)
                .handleFailure { throw it.error }
        val rightBoundary = getRightLaneBoundary(laneIdentifier)
                .handleFailure { throw it.error }
                .calculatePointListGlobalCS(step)
                .handleFailure { throw it.error }
        val linearRings = LinearRing3D.ofWithDuplicatesRemoval(leftBoundary, rightBoundary)
                .handleFailure { return it }
        val surface = CompositeSurface3D(linearRings)
        return Result.success(surface)
    }

    /**
     * Returns the filler surface which closes holes occurring at the lateral transition of two lane elements.
     * These lateral transitions might contain vertical holes which are caused by e.g. lane height offsets.
     *
     * @param laneIdentifier lane identifier for which the lateral filler surfaces to the left shall be created
     * @param step discretization step size
     */
    private fun getLeftLateralFillerSurface(laneIdentifier: LaneIdentifier, step: Double):
            Result<List<AbstractSurface3D>, Exception> {

        val leftLaneBoundary = getLeftLaneBoundaries(laneIdentifier)
                .handleFailure { return it }
                .calculatePointListGlobalCS(step)
                .handleFailure { return it }
        val rightLaneBoundary = getRightLaneBoundary(laneIdentifier.getAdjacentLeftLaneIdentifier())
                .handleFailure { return it }
                .calculatePointListGlobalCS(step)
                .handleFailure { return it }

        if (leftLaneBoundary.fuzzyEquals(rightLaneBoundary))
            return Result.success(emptyList())

        return LinearRing3D.ofWithDuplicatesRemoval(rightLaneBoundary, leftLaneBoundary)
                .handleFailure { return it }
                .let { CompositeSurface3D(it) }
                .let { Result.success(listOf(it)) }
    }

    /**
     * Returns the curve position domains of each lane section.
     */
    private fun getLaneSectionCurvePositionDomains(): List<Range<Double>> {
        val laneSectionDomains = laneSections
                .map { it.curvePositionStart.curvePosition }
                .zipWithNext()
                .map { Range.closed(it.first, it.second) }

        val lastLaneSectionDomain = Range.closedX(laneSections.last().curvePositionStart.curvePosition,
                curvePositionDomain.upperEndpointOrNull()!!, curvePositionDomain.upperBoundType())

        return laneSectionDomains + lastLaneSectionDomain
    }

    private fun getAttributeList(laneIdentifier: LaneIdentifier): Result<AttributeList, IllegalArgumentException> =
            getLane(laneIdentifier)
                    .handleFailure { return it }
                    .attributes
                    .let { Result.success(it) }

}