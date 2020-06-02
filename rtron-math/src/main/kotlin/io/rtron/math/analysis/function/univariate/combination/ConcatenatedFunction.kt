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

package io.rtron.math.analysis.function.univariate.combination

import com.github.kittinunf.result.Result
import io.rtron.math.analysis.function.univariate.UnivariateFunction
import io.rtron.math.analysis.function.univariate.pure.LinearFunction
import io.rtron.math.analysis.function.univariate.pure.PolynomialFunction
import io.rtron.math.container.ConcatenationContainer
import io.rtron.math.range.Range
import io.rtron.std.ContextMessage
import io.rtron.std.handleFailure
import io.rtron.std.hasSameSizeAs
import io.rtron.std.isSorted


/**
 * Represents the sequential concatenation of the provided member functions.
 *
 * @param memberFunctions functions to be concatenated
 */
class ConcatenatedFunction(
        memberFunctions: List<UnivariateFunction>
) : UnivariateFunction() {

    // Properties and Initializers
    private val container = ConcatenationContainer(memberFunctions)
    override val domain: Range<Double> get() = container.domain

    // Methods
    override fun valueUnbounded(x: Double): Result<Double, Exception> {

        val localMember = container.strictSelectMember(x)
                .handleFailure { return it }
        return localMember.member.valueUnbounded(localMember.localParameter)
    }

    override fun slopeUnbounded(x: Double): Result<Double, Exception> {
        val localMember = container.strictSelectMember(x)
                .handleFailure { return it }
        return localMember.member.slopeUnbounded(localMember.localParameter)
    }

    override fun valueInFuzzy(x: Double, tolerance: Double): Result<Double, Exception> {
        val localMember = container.fuzzySelectMember(x, tolerance)
                .handleFailure { return it }
        return localMember.member.valueUnbounded(localMember.localParameter)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ConcatenatedFunction

        if (container != other.container) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + container.hashCode()
        return result
    }


    companion object {

        /**
         * Creates a concatenated function with a list of polynomial function.
         * For example:
         * f(x) = 0 for [0, 5)
         * f(x) = x - 5 for [5, ∞)
         * The [starts] would be listOf(0, 5), the [intercepts] would be listOf(0, -5);
         * the [slopes] would be listOf(0, 1).
         *
         * @param starts absolute start value of the function member
         * @param intercepts local intercept of the linear function
         * @param slopes slope of the linear function (if list is empty, a slope of zero is assumed)
         */
        fun ofLinearFunctions(starts: List<Double>, intercepts: List<Double>, slopes: List<Double> = emptyList()):
                UnivariateFunction {

            require(starts.hasSameSizeAs(intercepts)) { "Equally sized starts and intercepts required." }
            require(starts.size >= slopes.size) { "Too many slope values provided." }
            require(starts.isSorted()) { "Start values must be sorted in ascending order." }

            if (starts.isEmpty()) return LinearFunction.X_AXIS

            val adjustedSlopes = slopes + List(starts.size - slopes.size) { 0.0 }
            check(starts.hasSameSizeAs(intercepts)) { "Equally sized starts and adjustedSlopes required." }

            val lengths = starts.zipWithNext().map { it.second - it.first }
            val domains = lengths.map { Range.closedOpen(0.0, it) } + Range.atLeast(0.0)
            val linearFunctions = starts.mapIndexed { index, _ ->
                LinearFunction(adjustedSlopes[index], intercepts[index], domains[index])
            }

            return ConcatenatedFunction(linearFunctions)
        }

        /**
         * Creates a concatenated function with a list of polynomial function.
         * For example:
         * f(x) = 2 + 3*x + 4*x^2 + x^3 for [0, 5)
         * f(x) = 1 + 2*x + 3*x^2 + 4* x^3  for [5, ∞)
         * The [starts] would be listOf(0, 5) and the [coefficients] would be
         * listOf(arrayOf(2, 3, 4, 1), arrayOf(1, 2, 3, 4).
         *
         * @param starts absolute start value of the function member
         * @param coefficients coefficients of the polynomial function members
         */
        fun ofPolynomialFunctions(starts: List<Double>, coefficients: List<DoubleArray>):
                ContextMessage<UnivariateFunction> {

            require(starts.hasSameSizeAs(coefficients)) { "Equally sized starts and coefficients required." }
            require(starts.isSorted()) { "Polynomials must be sorted in ascending order." }

            if (starts.isEmpty()) return ContextMessage(LinearFunction.X_AXIS)

            val lengths = starts.zipWithNext().map { it.second - it.first } + Double.POSITIVE_INFINITY
            val polynomials = coefficients.zip(lengths).filter { it.second != 0.0 }.map { PolynomialFunction.of(it.first, it.second) }

            val message = if (polynomials.hasSameSizeAs(starts)) "" else "Removed element(s) with length zero when building a concatenated polynomial."

            return ContextMessage(ConcatenatedFunction(polynomials), message)
        }

    }
}