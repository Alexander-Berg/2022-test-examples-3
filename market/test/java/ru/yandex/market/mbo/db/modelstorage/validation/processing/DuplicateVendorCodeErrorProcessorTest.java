package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;

/**
 * @author danfertev
 * @since 16.03.2018
 */
public class DuplicateVendorCodeErrorProcessorTest extends BaseDuplicateParameterValueErrorProcessorTest {
    @Before
    public void setup() {
        init(new DuplicateVendorCodeErrorProcessor());
    }
}
