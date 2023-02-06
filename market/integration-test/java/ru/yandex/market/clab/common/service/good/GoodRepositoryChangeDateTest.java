package ru.yandex.market.clab.common.service.good;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.01.2019
 */
@RunWith(Parameterized.class)
public class GoodRepositoryChangeDateTest extends BasePgaasIntegrationTest {
    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private ControlledClock clock;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public ActionSource actionSource;

    @Parameterized.Parameters(name = "actionSource={0}")
    public static List<ActionSource> actionSources() {
        return Arrays.asList(ActionSource.values());
    }

    @Test
    public void savesChangeLastDate() {
        Good good = goodRepository.save(createGood(), actionSource);
        LocalDateTime createdDate = good.getLastChangeDate();

        clock.tickHour();
        good.setState(GoodState.LOST);
        Good updated = goodRepository.save(good, actionSource);

        verifyLastChangeDate(updated, actionSource, createdDate);
    }

    @Test
    public void updatesLastChangeDate() {
        Good good = goodRepository.save(createGood(), actionSource);
        LocalDateTime createdDate = good.getLastChangeDate();

        clock.tickHour();
        good.setState(GoodState.LOST);
        List<Good> updated = goodRepository.save(Collections.singleton(good), actionSource);

        assertThat(updated).isNotEmpty();
        verifyLastChangeDate(updated.get(0), actionSource, createdDate);
    }

    private void verifyLastChangeDate(Good good, ActionSource actionSource, LocalDateTime date) {
       if (actionSource.isUpdateLastChangeDate()) {
           assertThat(good.getLastChangeDate()).isEqualTo(good.getModifiedDate());
           assertThat(good.getLastChangeDate()).isAfter(date);
       } else {
           assertThat(good.getLastChangeDate()).isEqualTo(date);
       }
    }

    private Good createGood() {
        return new Good()
            .setSupplierId(1L)
            .setSupplierSkuId("test-supplier-id");
    }
}
