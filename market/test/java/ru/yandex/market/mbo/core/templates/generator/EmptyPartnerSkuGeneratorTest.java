package ru.yandex.market.mbo.core.templates.generator;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.MboAssertions;

/**
 * @author danfertev
 * @since 20.06.2019
 */
public class EmptyPartnerSkuGeneratorTest {
    private static final long UID = 93939L;
    private static final long CATEGORY_ID = 1L;

    private AutoUser autoUser;
    private ModelGenerator modelGenerator;

    @Before
    public void setUp() {
        autoUser = new AutoUser(UID);
        modelGenerator = new EmptyPartnerSkuGenerator(autoUser);
    }

    @Test
    public void modelContainOnlyNameAndVendorValue() {
        CommonModel model = modelGenerator.generateModel(CATEGORY_ID);

        Assertions.assertThat(model.getParameterValues()).hasSize(2);
        MboAssertions.assertThat(model).getParameterValues(XslNames.NAME).exists();
        MboAssertions.assertThat(model).getParameterValues(XslNames.VENDOR).exists();
    }
}
