package ru.yandex.market.logistics.calendaring.base

import com.github.springtestdbunit.dataset.FlatXmlDataSetLoader
import org.dbunit.dataset.DataSetException
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.ReplacementDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.springframework.core.io.Resource
import java.io.IOException

class NullableColumnsDataSetLoader : FlatXmlDataSetLoader() {

    @Throws(DataSetException::class, IOException::class)
    override fun createDataSet(resource: Resource): IDataSet {
        val builder = FlatXmlDataSetBuilder()
        builder.isColumnSensing = true
        resource.inputStream.use { inputStream -> return createReplacementDataSet(builder.build(inputStream)) }
    }

    private fun createReplacementDataSet(dataSet: FlatXmlDataSet): ReplacementDataSet {
        val replacementDataSet = ReplacementDataSet(dataSet)
        replacementDataSet.addReplacementObject("[null]", null)
        return replacementDataSet
    }
}
