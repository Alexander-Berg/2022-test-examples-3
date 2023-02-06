package ru.yandex.market.logistics.cte.base

import com.github.springtestdbunit.dataset.AbstractDataSetLoader
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.csv.CsvURLDataSet
import org.springframework.core.io.Resource

class CsvDataSetLoader : AbstractDataSetLoader() {
    override fun createDataSet(resource: Resource?): IDataSet {
        return CsvURLDataSet(resource?.url)
    }
}
