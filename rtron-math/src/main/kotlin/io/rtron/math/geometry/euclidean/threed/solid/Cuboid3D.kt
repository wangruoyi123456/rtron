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

package io.rtron.math.geometry.euclidean.threed.solid

import com.github.kittinunf.result.Result
import io.rtron.math.geometry.euclidean.threed.point.Vector3D
import io.rtron.math.geometry.euclidean.threed.surface.Polygon3D
import io.rtron.math.transform.AffineSequence3D


/**
 * Represents a cuboid in 3D with the dimension ([length], [width], [height]). The origin of the local coordinate system
 * is located at the center of the ground face.
 *
 * @param length length of cuboid in the direction of the x axis
 * @param width width of cuboid in the direction of the y axis
 * @param height height of the cuboid in the direction of the z axis
 */
data class Cuboid3D(
        val length: Double,
        val width: Double,
        val height: Double,
        override val affineSequence: AffineSequence3D = AffineSequence3D.EMPTY
) : AbstractSolid3D() {

    // Properties and Initializers
    init {
        require(length.isFinite() && length > 0.0)
        { "Length must be finite and greater than zero." }
        require(width.isFinite() && width > 0.0)
        { "Width must be finite and greater than zero." }
        require(height.isFinite() && height > 0.0)
        { "Height must be finite and greater than zero." }
    }

    private val halfLength = length / 2.0
    private val halfWidth = width / 2.0

    // see the wikipedia article on [quadrants](https://en.wikipedia.org/wiki/Quadrant_(plane_geometry))
    private val vertexBaseQuadrantI = Vector3D(halfLength, halfWidth, 0.0)
    private val vertexBaseQuadrantII = Vector3D(-halfLength, halfWidth, 0.0)
    private val vertexBaseQuadrantIII = Vector3D(-halfLength, -halfWidth, 0.0)
    private val vertexBaseQuadrantIV = Vector3D(halfLength, -halfWidth, 0.0)

    private val vertexElevatedQuadrantI = Vector3D(halfLength, halfWidth, height)
    private val vertexElevatedQuadrantII = Vector3D(-halfLength, halfWidth, height)
    private val vertexElevatedQuadrantIII = Vector3D(-halfLength, -halfWidth, height)
    private val vertexElevatedQuadrantIV = Vector3D(halfLength, -halfWidth, height)

    private val basePolygon = Polygon3D.of(vertexBaseQuadrantI,
            vertexBaseQuadrantIV, vertexBaseQuadrantIII, vertexBaseQuadrantII)

    private val elevatedPolygon = Polygon3D.of(vertexElevatedQuadrantI,
            vertexElevatedQuadrantII, vertexElevatedQuadrantIII, vertexElevatedQuadrantIV)

    private val frontPolygon = Polygon3D.of(vertexBaseQuadrantI,
            vertexElevatedQuadrantI, vertexElevatedQuadrantIV, vertexBaseQuadrantIV)

    private val leftPolygon = Polygon3D.of(vertexBaseQuadrantI,
            vertexBaseQuadrantII, vertexElevatedQuadrantII, vertexElevatedQuadrantI)

    private val backPolygon = Polygon3D.of(vertexBaseQuadrantII,
            vertexBaseQuadrantIII, vertexElevatedQuadrantIII, vertexElevatedQuadrantII)

    private val rightPolygon = Polygon3D.of(vertexElevatedQuadrantIV,
            vertexElevatedQuadrantIII, vertexBaseQuadrantIII, vertexBaseQuadrantIV)

    // Methods
    override fun calculatePolygonsLocalCS(): Result<List<Polygon3D>, Exception> {
        val polygons = listOf(basePolygon, elevatedPolygon, frontPolygon, leftPolygon, backPolygon, rightPolygon)
        return Result.success(polygons)
    }

    // Conversions
    override fun toString(): String {
        return "Cuboid3D(referencePose=$affineSequence, width=$length, height=$width, depth=$height)"
    }

    companion object {
        val UNIT = Cuboid3D(1.0, 1.0, 1.0)
    }
}