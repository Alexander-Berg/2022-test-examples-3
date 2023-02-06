package ru.yandex.market.marketpromo.core.service.task;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.test.ServiceTaskTestBase;
import ru.yandex.market.marketpromo.core.test.utils.YtTestHelper;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.Promo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class PromoMigrationTaskTest extends ServiceTaskTestBase {

    public static final String PROMO_ID = "#1783";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private YtTestHelper ytTestHelper;
    @Autowired
    private PromoMigrationTask promosMigrationTask;

    @Value("classpath:/proto/promo_description_proto.bin")
    private Resource promoDescriptionProto;

    @Test
    void shouldImportPromos() throws IOException {
        byte[] promoBinaryProto = Files.readAllBytes(promoDescriptionProto.getFile().toPath());
        ytTestHelper.mockPromoDescriptionResponse(Map.of(PROMO_ID, promoBinaryProto));

        promosMigrationTask.process();

        Optional<Promo> promoOptional = promoDao.findExistedByPromoId(PROMO_ID);

        assertThat(promoOptional.isPresent(), is(true));
        assertThat(promoOptional.get(), allOf(
                hasProperty("name", notNullValue()),
                hasProperty("name", is("Скидки на летние шины")),
                hasProperty("promoId", is("#1783")),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("startDate", notNullValue()),
                hasProperty("endDate", notNullValue()),
                hasProperty("publishDate", notNullValue()),
                hasProperty("categories", notNullValue()),
                hasProperty("categoriesWithDiscounts", hasSize(5)),
                hasProperty("mechanicsProperties",
                        hasProperty("minimalDiscountPercentSize", notNullValue())),
                hasProperty("mechanicsProperties"
                ))

        );
    }
}
