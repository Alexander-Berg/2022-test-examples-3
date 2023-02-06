package ru.yandex.market.supportwizard.storage;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.base.ProgramType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

public class PartnerMoneyRepositoryTest extends BaseFunctionalTest {

    @Autowired
    private PartnerMoneyRepository partnerMoneyRepository;

    @Test
    void testProgramTypeInDb() {
        partnerMoneyRepository.save(new PartnerMoneyEntity(
                1,
                1,
                1,
                1,
                "name",
                PartnerType.SHOP,
                true,
                LocalDate.now(),
                List.of(ProgramType.ADV.name())
        ));
        var money = partnerMoneyRepository.findAll();
        Assertions.assertEquals(1, money.size());
        Assertions.assertArrayEquals(new String[]{ProgramType.ADV.name()}, money.get(0).getProgramTypes());
    }
}
