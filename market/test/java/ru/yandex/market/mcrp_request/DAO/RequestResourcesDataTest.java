package ru.yandex.market.mcrp_request.DAO;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mcrp_request.config.McrpTestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {McrpTestConfig.class})
public class RequestResourcesDataTest {
    @Test
    public void parseResurcesTest() throws IOException {
        String mdbJson = IOUtils.toString(Objects.requireNonNull(RequestResourcesDataTest.class.getResourceAsStream(
                "MDBResources.json")));
        String mdbJsonMigrated = IOUtils.toString(Objects.requireNonNull(RequestResourcesDataTest.class.getResourceAsStream(
                "MDBResourcesMigrated.json")));

        Map<String, RequestResources> resourcesData = RequestResourcesData.fromJsonString(mdbJson, false, "today", MDBResourcesOld.class);

        Map<String, RequestResources> resourcesDataMigrated = RequestResourcesData.fromJsonString(mdbJsonMigrated, true, "", MDBResources.class);

        Assert.assertEquals(resourcesData, resourcesDataMigrated);

    }
}
