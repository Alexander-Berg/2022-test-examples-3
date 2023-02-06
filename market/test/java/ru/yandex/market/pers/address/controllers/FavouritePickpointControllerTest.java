package ru.yandex.market.pers.address.controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.address.config.TestClient;
import ru.yandex.market.pers.address.controllers.model.FavouritePickpointRequest;
import ru.yandex.market.pers.address.controllers.model.FavouritePickpointResponse;
import ru.yandex.market.pers.address.dao.FavouritePickpointDao;
import ru.yandex.market.pers.address.dao.SettingsDao;
import ru.yandex.market.pers.address.model.FavouritePickpoint;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.util.BaseWebTest;
import ru.yandex.market.pers.address.util.Experiments;
import ru.yandex.market.pers.address.util.Utils;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FavouritePickpointControllerTest extends BaseWebTest {
    private static final String USER_ID = "0";
    private static final String USER_TYPE = "uid";
    private static final Identity<?> IDENTITY = Utils.createIdentity(USER_ID, USER_TYPE);
    public static final String PICK_OLD_ID = "272828540";
    public static final String PICK_NEW_ID = "10001666713";
    public static final String PICK_NEW_ID2 = "10001666714";
    public static final String PICK_NEW_ID3 = "10001666715";

    @Autowired
    private FavouritePickpointDao dao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestClient testClient;

    @Autowired
    private SettingsDao settingsDao;

    @BeforeEach
    public void init() {
        jdbcTemplate.update("TRUNCATE TABLE " + FavouritePickpointDao.TABLE_NAME + " CASCADE ");
    }

    @Test
    public void shouldSaveNewPickpoint() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        ImmutableSet<FavouritePickpoint> pickpoints = dao.getAll(IDENTITY, 5);
        FavouritePickpoint pickpoint = pickpoints.asList().get(0);

        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(request.getPickId(), pickpoint.getPickId());
        Assertions.assertEquals(request.getRegionId(), pickpoint.getRegionId());
        Assertions.assertEquals(IDENTITY, pickpoint.getIdentity());
    }

    @Test
    public void shouldUpdatePickpointUseTime() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        FavouritePickpointRequest newRequest = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(newRequest, USER_TYPE, USER_ID);

        ImmutableSet<FavouritePickpoint> pickpoints = dao.getAll(IDENTITY, 5);
        FavouritePickpoint pickpoint = pickpoints.asList().get(0);

        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(request.getPickId(), pickpoint.getPickId());
        Assertions.assertEquals(request.getRegionId(), pickpoint.getRegionId());
        Assertions.assertEquals(IDENTITY, pickpoint.getIdentity());
        Assertions.assertTrue(pickpoint.getLastOrderTime().isAfter(pickpoint.getCreationTime()));
    }

    @Test
    public void shouldNotPassValidationOnNewPickpoint() throws Exception {
        FavouritePickpointRequest request = buildRequest(null, null);
        testClient.savePickpoint(request, USER_TYPE, USER_ID, status().is4xxClientError());
    }

    @Test
    public void shouldReturnSortedListOfPickpoints() throws Exception {
        ImmutableSet<FavouritePickpoint> pickpoints = dao.getAll(IDENTITY, 10);
        Assertions.assertEquals(0, pickpoints.size());

        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        request = buildRequest(PICK_NEW_ID2, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        request = buildRequest(PICK_NEW_ID3, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpointsFromJson =
                testClient.getPickpoints(USER_TYPE, USER_ID, 100);

        Assertions.assertEquals(3, pickpointsFromJson.size());
        Assertions.assertEquals(PICK_NEW_ID3, pickpointsFromJson.get(0).getPickId());
        Assertions.assertEquals(PICK_NEW_ID2, pickpointsFromJson.get(1).getPickId());
        Assertions.assertEquals(PICK_NEW_ID, pickpointsFromJson.get(2).getPickId());
    }


    @Test
    public void shouldReturnSpecifiedNumberOfPickpoints() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        request = buildRequest(PICK_NEW_ID2, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        request = buildRequest(PICK_NEW_ID3, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpointsFromJson = testClient.getPickpoints(USER_TYPE, USER_ID, 2);

        Assertions.assertEquals(2, pickpointsFromJson.size());
    }

    @Test
    public void shouldReturnEmptyListWithWrongId() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        request = buildRequest(PICK_NEW_ID2, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpointsFromJson = testClient.getPickpoints(USER_TYPE, "999", 100);

        Assertions.assertEquals(0, pickpointsFromJson.size());
    }

    @Test
    public void shouldRemovePickpoint() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        TimeUnit.MILLISECONDS.sleep(5);

        request = buildRequest(PICK_NEW_ID2, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        ImmutableSet<FavouritePickpoint> pickpoints = dao.getAll(IDENTITY, 5);
        Assertions.assertEquals(2, pickpoints.size());

        testClient.deletePickpoint(USER_TYPE, USER_ID, PICK_NEW_ID);

        pickpoints = dao.getAll(IDENTITY, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(PICK_NEW_ID2, pickpoints.asList().get(0).getPickId());
    }

    @Test
    public void shouldNotRemovePickpointOnWrongId() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        ImmutableSet<FavouritePickpoint> pickpoints = dao.getAll(IDENTITY, 5);
        Assertions.assertEquals(1, pickpoints.size());

        testClient.deletePickpoint(USER_TYPE, USER_ID, PICK_NEW_ID2);

        pickpoints = dao.getAll(IDENTITY, 5);
        Assertions.assertEquals(1, pickpoints.size());
    }

    @Test
    public void shouldReplaceOldIdWithMapping() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_OLD_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpoints = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(PICK_NEW_ID, pickpoints.iterator().next().getPickId());
    }

    @Test
    public void shouldNotFetchOldIdWithoutMapping() throws Exception {
        String oldIdWithoutMapping = "1";
        FavouritePickpointRequest request = buildRequest(oldIdWithoutMapping, 11);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        FavouritePickpointRequest request2 = buildRequest(PICK_NEW_ID, 12);
        testClient.savePickpoint(request2, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpoints = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(PICK_NEW_ID, pickpoints.iterator().next().getPickId());
    }

    @Test
    public void shouldReturnNewId() throws Exception {
        String newIdWithoutMapping = "123456789101112";
        FavouritePickpointRequest request = buildRequest(newIdWithoutMapping, 11);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpoints = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(newIdWithoutMapping, pickpoints.iterator().next().getPickId());
    }

    @Test
    public void shouldRemoveDuplicates() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 11);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        FavouritePickpointRequest request2 = buildRequest(PICK_NEW_ID, 12);
        testClient.savePickpoint(request2, USER_TYPE, USER_ID);

        FavouritePickpointRequest request3 = buildRequest(PICK_NEW_ID, 13);
        testClient.savePickpoint(request3, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpoints = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(PICK_NEW_ID, pickpoints.iterator().next().getPickId());
    }

    @Test
    public void shouldTurnOffDuplicatesBySetting() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_NEW_ID, 11);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        FavouritePickpointRequest request2 = buildRequest(PICK_OLD_ID, 12);
        testClient.savePickpoint(request2, USER_TYPE, USER_ID);

        FavouritePickpointRequest request3 = buildRequest(PICK_OLD_ID, 13);
        testClient.savePickpoint(request3, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpoints = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(PICK_NEW_ID, pickpoints.iterator().next().getPickId());

        settingsDao.put("pickpoint_remove_duplicates_and_map_id_on", "false");
        List<FavouritePickpointResponse> pickpointsNotRemoveDuplicates =
                testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(3, pickpointsNotRemoveDuplicates.size());
    }

    @Test
    public void shouldTurnOffMappingBySetting() throws Exception {
        FavouritePickpointRequest request = buildRequest(PICK_OLD_ID, 42);
        testClient.savePickpoint(request, USER_TYPE, USER_ID);

        List<FavouritePickpointResponse> pickpoints = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpoints.size());
        Assertions.assertEquals(PICK_NEW_ID, pickpoints.iterator().next().getPickId());

        settingsDao.put("pickpoint_remove_duplicates_and_map_id_on", "false");
        List<FavouritePickpointResponse> pickpointsWithoutMapping = testClient.getPickpoints(USER_TYPE, USER_ID, 5);
        Assertions.assertEquals(1, pickpointsWithoutMapping.size());
        Assertions.assertEquals(PICK_OLD_ID, pickpointsWithoutMapping.iterator().next().getPickId());
    }


    private static FavouritePickpointRequest buildRequest(String pickId, Integer regionId) {
        return FavouritePickpointRequest.builder()
                .setPickId(pickId)
                .setRegionId(regionId)
                .build();
    }
}
