package ru.yandex.market.hrms.core.persistence;

import java.time.ZoneId;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.model.domain.DomainType;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.domain.domain.repo.DomainRepo;

public class PostgresStringArrayTypeTest extends AbstractCoreTest {

    @Autowired
    private DomainRepo domainRepo;

    @Test
    public void arrayShouldBeReadCorrectly() {
        var domain = Domain.builder()
                .name("Софьино")
                .timezone(ZoneId.of("Europe/Moscow"))
                .additionalDepartmentNames(new String[]{ "раз", "два", "три" })
                .type(DomainType.FFC)
                .build();

        domainRepo.save(domain);
        var updatedDomain = domainRepo.findById(domain.getId());
        MatcherAssert.assertThat(updatedDomain.isPresent(), Matchers.is(true));
        MatcherAssert.assertThat(updatedDomain.get().getAdditionalDepartmentNames(),
                Matchers.is(new String[]{ "раз", "два", "три" }));

    }
}
