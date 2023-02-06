package ru.yandex.market.replenishment.autoorder.service.quotas;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.dto.QuotaDto;
import ru.yandex.market.replenishment.autoorder.dto.QuotaRequestParams;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Quota;
import ru.yandex.market.replenishment.autoorder.repository.postgres.QuotaRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.junit.Assert.assertEquals;

@Slf4j
@WithMockLogin
@ActiveProfiles("testing")
public class QuotasServiceImplTest extends ControllerTest {

    @Autowired
    private QuotasService quotasService;
    @Autowired
    private QuotaRepository quotaRepository;

    @Test
    @DbUnitDataSet(before = "QuotasServiceImplTest.before.csv")
    public void findByTest() {
        quotaRepository.truncate();
        this.buildTestQuotaList().forEach(quotaRepository::save);

        QuotaRequestParams quotaRequestParams = buildRequestParams();
        List<QuotaDto> actual = quotasService.findBy(quotaRequestParams);

        assertEquals(actual.size(), 4);
        System.out.println();
    }

    private QuotaRequestParams buildRequestParams() {
        return QuotaRequestParams.builder()
            .from(LocalDate.now())
            .build();
    }

    private List<Quota> buildTestQuotaList() {
        LocalDate now = LocalDate.now();
        return Stream.of(
            this.buildQuota(now.minusDays(3)),
            this.buildQuota(now.minusDays(2)),
            this.buildQuota(now.minusDays(1)),
            this.buildQuota(now),
            this.buildQuota(now.plusDays(1)),
            this.buildQuota(now.plusDays(2)),
            this.buildQuota(now.plusDays(3))

        ).collect(Collectors.toList());
    }

    private Quota buildQuota(LocalDate date) {
        Quota quota = new Quota();
        quota.setQuantity(1000L);
        quota.setDepartmentId(1L);
        quota.setDepartmentName("DIY");
        quota.setWarehouseId(172L);
        quota.setWarehouseName("Ростов");
        quota.setDate(date);
        return quota;
    }
}
