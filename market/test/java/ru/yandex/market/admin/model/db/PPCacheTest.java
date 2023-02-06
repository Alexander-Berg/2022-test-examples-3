package ru.yandex.market.admin.model.db;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.pp.PPBean;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

class PPCacheTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Проверка, что данные поднимаются из базы в ожидаемом количестве и формате.
     */
    @Test
    @DbUnitDataSet(before = "PpCacheTest.before.csv")
    void test_reload() {
        PPCache ppCache = new PPCache();
        ppCache.setJdbcTemplate(jdbcTemplate);
        ppCache.reload();

        List<PPBean> actual = ppCache.getPPValues();

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(List.of(
                        new PPBean("611,612,613,614,615,616", "Все | 611,612,613,614,615,616"),
                        new PPBean("613,614", "group_631 | 613,614"),
                        new PPBean("615", "group_632 | 615"),
                        new PPBean("611", "group_633 | 611"),
                        new PPBean("611", "pp_611 | 611"),
                        new PPBean("612", "pp_612 | 612"),
                        new PPBean("613", "pp_613 | 613"),
                        new PPBean("614", "pp_614 | 614"),
                        new PPBean("615", "pp_615 | 615"),
                        new PPBean("616", "pp_616 | 616")
                ));
    }
}
