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

package io.rtron.transformer.roadspace2citygml.parameter

import io.rtron.math.geometry.euclidean.threed.solid.Cylinder3D
import io.rtron.math.geometry.euclidean.threed.solid.ParametricSweep3D
import io.rtron.std.Property
import io.rtron.transformer.AbstractTransformerParameters
import io.rtron.transformer.TransformerConfiguration

typealias Roadspaces2CitygmlConfiguration = TransformerConfiguration<Roadspaces2CitygmlParameters>


/**
 * Transformation parameters for the RoadSpaces model to the CityGML model transformer.
 */
class Roadspaces2CitygmlParameters(
        private val gmlIdPrefixProperty: Property<String> = Property("UUID_", isDefault = true),
        private val identifierAttributesPrefixProperty: Property<String> = Property("identifier_", isDefault = true),
        private val flattenGenericAttributeSetsProperty: Property<Boolean> = Property(value = true, isDefault = true),
        private val discretizationStepSizeProperty: Property<Double> = Property(0.7, true),
        private val sweepDiscretizationStepSizeProperty: Property<Double> = Property(ParametricSweep3D.DEFAULT_STEP_SIZE, true),
        private val circleSlicesProperty: Property<Int> = Property(Cylinder3D.DEFAULT_NUMBER_SLICES, true)
) : AbstractTransformerParameters() {

    // Properties and Initializers

    /**
     * prefix for generated gml ids
     */
    val gmlIdPrefix by gmlIdPrefixProperty

    /**
     * prefix for identifier attribute names
     */
    val identifierAttributesPrefix by identifierAttributesPrefixProperty

    /**
     * distance between each discretization step for curves and surfaces
     */
    val discretizationStepSize by discretizationStepSizeProperty

    /**
     * distance between each discretization step for solid geometries of [ParametricSweep3D]
     */
    val sweepDiscretizationStepSize by sweepDiscretizationStepSizeProperty

    /**
     * number of discretization points for a circle or cylinder
     */
    val circleSlices by circleSlicesProperty

    /**
     * true, if nested attribute lists shall be flattened out
     */
    val flattenGenericAttributeSets by flattenGenericAttributeSetsProperty

    // Methods

    /**
     * Merges the [other] parameters into this. See [Property.leftMerge] for the prioritization rules.
     */
    infix fun leftMerge(other: Roadspaces2CitygmlParameters) = Roadspaces2CitygmlParameters(
            this.gmlIdPrefixProperty leftMerge other.gmlIdPrefixProperty,
            this.identifierAttributesPrefixProperty leftMerge other.identifierAttributesPrefixProperty,
            this.flattenGenericAttributeSetsProperty leftMerge other.flattenGenericAttributeSetsProperty,
            this.discretizationStepSizeProperty leftMerge other.discretizationStepSizeProperty,
            this.sweepDiscretizationStepSizeProperty leftMerge other.sweepDiscretizationStepSizeProperty,
            this.circleSlicesProperty leftMerge other.circleSlicesProperty
    )

    // Conversions
    override fun toString(): String =
            "Roadspaces2CitygmlParameters(gmlIdPrefix=$gmlIdPrefix, identifierAttributesPrefix=$identifierAttributesPrefix, " +
                    "flattenGenericAttributeSets=$flattenGenericAttributeSets, discretizationStepSize=$discretizationStepSize, " +
                    "sweepDiscretizationStepSize=$sweepDiscretizationStepSize, circleSlices=$circleSlices)"
}
