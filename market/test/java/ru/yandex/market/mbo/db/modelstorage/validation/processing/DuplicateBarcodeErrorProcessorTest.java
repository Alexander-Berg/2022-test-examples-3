package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;

/**
 * @author danfertev
 * @since 15.03.2018
 */
public class DuplicateBarcodeErrorProcessorTest extends BaseDuplicateParameterValueErrorProcessorTest {
    @Before
    public void setup() {
        init(new DuplicateBarcodeErrorProcessor());
    }
}
