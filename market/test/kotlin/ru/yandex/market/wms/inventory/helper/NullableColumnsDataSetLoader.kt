package ru.yandex.market.wms.inventory.helper

import com.github.springtestdbunit.dataset.FlatXmlDataSetLoader
import liquibase.util.file.FilenameUtils
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.ReplacementDataSet
import org.dbunit.dataset.csv.CsvDataSet
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.springframework.core.io.Resource

class NullableColumnsDataSetLoader : FlatXmlDataSetLoader() {
    @Throws(Exception::class)
    public override fun createDataSet(resource: Resource): IDataSet {
        val dataSet = createDataSetFromResource(resource)
        return createReplacementDataSet(dataSet)
    }

    @Throws(Exception::class)
    private fun createDataSetFromResource(resource: Resource): IDataSet {
        val extension = FilenameUtils.getExtension(resource.filename)
        if (extension == "xml") {
            val builder = FlatXmlDataSetBuilder()
            builder.isColumnSensing = true
            resource.inputStream.use { inputStream -> return createReplacementDataSet(builder.build(inputStream)) }
        } else {
            return CsvDataSet(resource.file)
        }
    }

    private fun createReplacementDataSet(dataSet: IDataSet): ReplacementDataSet {
        val replacementDataSet = ReplacementDataSet(dataSet)
        replacementDataSet.addReplacementObject("[null]", null)
        return replacementDataSet
    }
}
