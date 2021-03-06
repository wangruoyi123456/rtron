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

package io.rtron.readerwriter.citygml

import com.github.kittinunf.result.Result
import org.citygml4j.CityGMLContext
import org.citygml4j.model.module.citygml.CityGMLVersion
import io.rtron.io.files.Path
import io.rtron.model.AbstractModel
import io.rtron.model.citygml.CitygmlModel
import io.rtron.readerwriter.AbstractReaderWriter

class CitygmlReaderWriter(
        override val configuration: CitygmlReaderWriterConfiguration
) : AbstractReaderWriter(configuration) {

    // Properties and Initializers
    private val _citygmlContext = CityGMLContext.getInstance()
    private val _builder = _citygmlContext.createCityGMLBuilder()!!

    // Methods
    override fun isSupported(fileExtension: String) = fileExtension in supportedFileExtensions
    override fun isSupported(model: AbstractModel) = model is CitygmlModel

    override fun read(filePath: Path): AbstractModel {
        TODO("not implemented")
    }

    override fun write(model: AbstractModel, directoryPath: Path): Result<List<Path>, Exception> {
        require(model is CitygmlModel) { "$this received not a CitygmlModel." }

        val out = _builder.createCityGMLOutputFactory(CityGMLVersion.DEFAULT)
        val path = directoryPath.resolve(Path("${directoryPath.fileName}.gml"))
        val writer = out.createCityGMLWriter(path.toFileJ(), "UTF-8")!!

        writer.setPrefixes(CityGMLVersion.DEFAULT)
        writer.setSchemaLocations(CityGMLVersion.DEFAULT)
        writer.indentString = "  "
        writer.write(model.cityModel)
        writer.close()

        return Result.success(listOf(path))
    }

    companion object {
        val supportedFileExtensions = listOf("gml")
    }
}
