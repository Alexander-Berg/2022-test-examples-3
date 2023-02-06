package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;

public class PdbDemandStatusesLoaderTest extends FunctionalTest {

    @Autowired
    private PdbDemandStatusesLoader pdbDemandStatusesLoader;

    @Test
    @DbUnitDataSet(dataSource = "pdbDataSource", before = "PdbDemandStatusesLoaderTest.loadSuccess.before.pdb.csv")
    @DbUnitDataSet(before = "PdbDemandStatusesLoaderTest.loadSuccess.before.csv",
            after = "PdbDemandStatusesLoaderTest.loadSuccess.after.csv")
    public void loadSuccessTest() {
        pdbDemandStatusesLoader.load();
    }

    @Test
    @DbUnitDataSet(dataSource = "pdbDataSource", before = "PdbDemandStatusesLoaderTest.loadNoStatus.before.pdb.csv")
    @DbUnitDataSet(before = "PdbDemandStatusesLoaderTest.loadNoStatus.before.csv",
            after = "PdbDemandStatusesLoaderTest.loadNoStatus.after.csv")
    public void loadNoStatusTest() {
        Assertions.assertThrows(NullPointerException.class, () -> pdbDemandStatusesLoader.load());
    }

    @Test
    @DbUnitDataSet(dataSource = "pdbDataSource",
        before = "PdbDemandStatusesLoaderTest.loadSuccessMonoXdoc.before.pdb.csv")
    @DbUnitDataSet(before = "PdbDemandStatusesLoaderTest.loadSuccessMonoXdoc.before.csv",
        after = "PdbDemandStatusesLoaderTest.loadSuccessMonoXdoc.after.csv")
    public void loadSuccessMonoXdocTest() {
        setTestTime(LocalDateTime.of(2022, 4, 27, 0, 0, 0));
        pdbDemandStatusesLoader.load();
    }

    @Test
    @DbUnitDataSet(dataSource = "pdbDataSource", before = "PdbDemandStatusesLoaderTest.loadSuccessMonoxdocByCreatedDatetime.before.pdb.csv")
    @DbUnitDataSet(before = "PdbDemandStatusesLoaderTest.loadSuccessMonoXdocByCreatedTimestamp.before.csv",
            after = "PdbDemandStatusesLoaderTest.loadSuccessMonoXdocByCreatedTimestamp.after.csv")
    public void loadSuccessMonoXdocByCreatedDatetimeTest() {
        setTestTime(LocalDateTime.of(2022, 4, 28, 0, 0, 0));
        pdbDemandStatusesLoader.load();
    }
}
