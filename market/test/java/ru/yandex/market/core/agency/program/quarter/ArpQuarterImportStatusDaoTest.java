package ru.yandex.market.core.agency.program.quarter;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.program.ProgramRewardType;

public class ArpQuarterImportStatusDaoTest extends FunctionalTest {
    @Autowired
    ArpQuarterImportStatusDao dao;

    @Test
    void smokeTest() {
        dao.updateStatus(ArpQuarterImportStatus.builder()
                .setClosed(true)
                .setLevel(ArpCalculatingLevel.DATASOURCE)
                .setQuarter(Quarter.rewardQuarter(Instant.now()))
                .setProgram(ProgramRewardType.CUT_PRICE)
                .setStateDate(Instant.now())
                .build()
        );
    }
}
