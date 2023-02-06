package ru.yandex.market.logistics.yard.base;

import java.io.IOException;
import java.io.InputStream;

import com.github.springtestdbunit.dataset.FlatXmlDataSetLoader;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.springframework.core.io.Resource;

public class NullableColumnsDataSetLoader extends FlatXmlDataSetLoader {

    @Override
    protected IDataSet createDataSet(Resource resource) throws DataSetException, IOException {
        FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
        builder.setColumnSensing(true);
        try (InputStream inputStream = resource.getInputStream()) {
            return createReplacementDataSet(builder.build(inputStream));
        }
    }

    private ReplacementDataSet createReplacementDataSet(FlatXmlDataSet dataSet) {
        ReplacementDataSet replacementDataSet = new ReplacementDataSet(dataSet);

        replacementDataSet.addReplacementObject("[null]", null);

        return replacementDataSet;
    }
}
