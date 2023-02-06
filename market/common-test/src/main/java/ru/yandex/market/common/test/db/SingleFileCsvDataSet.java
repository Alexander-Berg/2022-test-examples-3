package ru.yandex.market.common.test.db;

import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.DataSetException;

import java.io.InputStream;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class SingleFileCsvDataSet extends CachedDataSet {

    public SingleFileCsvDataSet(InputStream is) throws DataSetException {
        super(new SingleFileCsvProducer(is));
    }
}
