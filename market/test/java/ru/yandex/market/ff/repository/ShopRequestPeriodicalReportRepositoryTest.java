package ru.yandex.market.ff.repository;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.model.entity.ShopRequestPeriodicalReport;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

class ShopRequestPeriodicalReportRepositoryTest extends IntegrationTest {

    @Autowired
    private ShopRequestPeriodicalReportRepository repository;

    @Test
    @DatabaseSetup(value = "classpath:repository/shop-request-periodical-report/before_save.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request-periodical-report/after_save.xml",
            assertionMode = NON_STRICT)
    public void save() throws Exception {
        ShopRequestPeriodicalReport rep = new ShopRequestPeriodicalReport();

        rep.setSupplierType(SupplierType.FIRST_PARTY);
        rep.setWarehouseId(172L);
        rep.setExtension(FileExtension.XLSX);
        rep.setFileUrl("url://");
        rep.setFileName("имяМоегоФайла");
        rep.setReportDate(LocalDate.of(1999, 9, 9));

        repository.save(rep);
    }

    @Test
    @Transactional
    @DatabaseSetup(value = "classpath:repository/shop-request-periodical-report/before_delete.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request-periodical-report/after_delete.xml",
            assertionMode = NON_STRICT)
    void deleteBy() {
        repository.deleteBy(LocalDate.of(1999, 9, 9), 172L, SupplierType.FIRST_PARTY);
    }
}
