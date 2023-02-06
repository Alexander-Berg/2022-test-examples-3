package ru.yandex.direct.core.entity.outdoor.repository;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.outdoor.model.PlacementsOutdoorData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PlacementsOutdoorDataRepositoryTest {

    private static final Long PAGE_ID_1 = 1L;
    private static final Long PAGE_ID_2 = 2L;

    private static final String NAME_1 = "russ outdoor";
    private static final String NAME_2 = "gallery";

    @Autowired
    private PlacementsOutdoorDataRepository repoUnderTest;

    @Before
    public void clear() {
        repoUnderTest.delete(asList(PAGE_ID_1, PAGE_ID_2));
    }

    @Test
    public void addOrUpdate_Add_Success() {
        PlacementsOutdoorData placementsOutdoorData =
                new PlacementsOutdoorData().withPageId(PAGE_ID_1).withInternalName(NAME_1);
        repoUnderTest.addOrUpdate(singletonList(placementsOutdoorData));

        List<PlacementsOutdoorData> allData = repoUnderTest.getAll(LimitOffset.maxLimited());
        assertThat(allData).hasSize(1);
        assertThat(allData.get(0)).isEqualTo(placementsOutdoorData);
    }

    @Test
    public void addOrUpdate_AddAndUpdate_Success() {
        PlacementsOutdoorData data = new PlacementsOutdoorData().withPageId(PAGE_ID_1).withInternalName(NAME_1);
        repoUnderTest.addOrUpdate(singletonList(data));

        data.withInternalName(NAME_2);
        repoUnderTest.addOrUpdate(singletonList(data));

        List<PlacementsOutdoorData> allData = repoUnderTest.getAll(LimitOffset.maxLimited());
        assertThat(allData).hasSize(1);
        assertThat(allData.get(0)).isEqualTo(data);
    }

    @Test
    public void getAll_Success() {
        PlacementsOutdoorData data1 = new PlacementsOutdoorData().withPageId(PAGE_ID_1).withInternalName(NAME_1);
        PlacementsOutdoorData data2 = new PlacementsOutdoorData().withPageId(PAGE_ID_2).withInternalName(NAME_2);
        repoUnderTest.addOrUpdate(asList(data1, data2));
        List<PlacementsOutdoorData> limitedData = repoUnderTest.getAll(new LimitOffset(1, 0));
        Assert.assertThat(limitedData, hasSize(1));

        List<PlacementsOutdoorData> allData = repoUnderTest.getAll(LimitOffset.maxLimited());
        Assert.assertThat(allData, hasSize(2));
    }
}
