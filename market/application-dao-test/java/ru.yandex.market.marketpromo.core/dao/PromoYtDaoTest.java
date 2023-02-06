package ru.yandex.market.marketpromo.core.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.utils.YtTestHelper;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.Promo;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author kl1san
 */
public class PromoYtDaoTest extends ServiceTestBase {

    @Autowired
    private PromoYtDao promoYtDao;
    @Autowired
    private YtTestHelper ytTestHelper;

    @Value("classpath:/proto/promo_description_proto.bin")
    private Resource promoDescriptionProto;

    @Test
    public void shouldMapToModel() throws IOException {
        String promoKey = "#1783";
        //TODO переделать - в тесте должно быть видно данные, не должно быть бинарников
        byte[] promoBinaryProto = Files.readAllBytes(promoDescriptionProto.getFile().toPath());
        ytTestHelper.mockPromoDescriptionResponse(Map.of(promoKey, promoBinaryProto));
        List<Promo> promos = new ArrayList<>();
        promoYtDao.loadOldRecords(promos::addAll);
        MatcherAssert.assertThat(promos, hasItem(allOf(
                hasProperty("name", is("Скидки на летние шины")),
                hasProperty("promoId", is("#1783")),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("startDate", notNullValue()),
                hasProperty("endDate", notNullValue()),
                hasProperty("publishDate", notNullValue()),
                hasProperty("categories", notNullValue()),
                hasProperty("categoriesWithDiscounts", notNullValue()),
                hasProperty("hasErrors", notNullValue()),
                hasProperty("mechanicsProperties",
                        hasProperty("minimalDiscountPercentSize", CoreMatchers.notNullValue())
                )
        )));
    }
}
