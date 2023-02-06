package ru.yandex.direct.core.entity.client.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientMeasurerSettings;
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSystem;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientMeasurerSettingsRepositoryTest {

    @Autowired
    public Steps steps;

    @Autowired
    private ClientMeasurerSettingsRepository clientMeasurerSettingsRepository;

    private int shard;
    private List<Long> clientIds;

    @Before
    public void before() {
        var client1 = steps.clientSteps().createDefaultClient();
        var client2 = steps.clientSteps().createDefaultClient();
        clientIds = List.of(client1.getClientId().asLong(), client2.getClientId().asLong());
        shard = client1.getShard();
    }

    @Test
    public void insertOrUpdate_AddTwoRecords_ReturnTwoRecords() {
        try {
            var clientMeasurerSettingsList =
                    clientIds.stream().map(clientId -> new ClientMeasurerSettings()
                            .withClientId(clientId)
                            .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                            .withSettings("{\"json\": \"json\"}")).collect(toList());

            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            var result = clientMeasurerSettingsRepository.getByMeasurerSystem(shard, ClientMeasurerSystem.MEDIASCOPE);

            assertThat(result.size(), equalTo(clientIds.size()));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(shard, clientIds.get(0),
                    ClientMeasurerSystem.MEDIASCOPE);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(shard, clientIds.get(1),
                    ClientMeasurerSystem.MEDIASCOPE);
        }
    }

    @Test
    public void insertOrUpdate_UpdateOneRecord_ReturnNewValue() {
        try {
            var clientMeasurerSettingsList =
                    List.of(new ClientMeasurerSettings()
                            .withClientId(clientIds.get(0))
                            .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                            .withSettings("{\"json\": \"json\"}"));

            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            var record = clientMeasurerSettingsRepository.getByMeasurerSystem(shard, ClientMeasurerSystem.MEDIASCOPE);
            assertThat(record.get(0).getSettings(), equalTo("{\"json\": \"json\"}"));

            clientMeasurerSettingsList.get(0).setSettings("{}");
            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            var updatedRecord = clientMeasurerSettingsRepository.getByMeasurerSystem(shard,
                    ClientMeasurerSystem.MEDIASCOPE);
            assertThat(updatedRecord.get(0).getSettings(), equalTo("{}"));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(shard, clientIds.get(0),
                    ClientMeasurerSystem.MEDIASCOPE);
        }
    }

    @Test
    public void deleteByClientIdAndSystem_DeleteOneRecord_Ok() {
        var clientMeasurerSettingsList =
                List.of(new ClientMeasurerSettings()
                        .withClientId(clientIds.get(0))
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings("{\"json\": \"json\"}"));

        clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
        clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                shard, clientIds.get(0), ClientMeasurerSystem.MEDIASCOPE);
        var result = clientMeasurerSettingsRepository.getByMeasurerSystem(shard, ClientMeasurerSystem.MEDIASCOPE);

        assertThat(result.size(), equalTo(0));
    }

    @Test
    public void deleteByClientIdAndSystem_DeleteOneRecord_ReturnTwoRecords() {
        try {
            var clientMeasurerSettingsList =
                    List.of(new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.NO)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(1))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"));

            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.NO);

            var result = clientMeasurerSettingsRepository.getByMeasurerSystem(shard, ClientMeasurerSystem.MEDIASCOPE);
            assertThat(result.size(), equalTo(2));
            var emptyResult = clientMeasurerSettingsRepository.getByMeasurerSystem(shard, ClientMeasurerSystem.NO);
            assertThat(emptyResult.size(), equalTo(0));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.MEDIASCOPE);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(1), ClientMeasurerSystem.MEDIASCOPE);
        }
    }

    @Test
    public void getByClientId_AddThreeRecords_ReturnTwoRecords() {
        try {
            var clientMeasurerSettingsList =
                    List.of(new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.NO)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(1))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"));

            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            var result = clientMeasurerSettingsRepository.getByClientId(shard, clientIds.get(0));

            assertThat(result.size(), equalTo(2));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.MEDIASCOPE);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.NO);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(1), ClientMeasurerSystem.MEDIASCOPE);
        }
    }

    @Test
    public void getByMeasurerSystem_AddThreeRecords_ReturnOneRecord() {
        try {
            var clientMeasurerSettingsList =
                    List.of(new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.NO)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(1))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"));

            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            var result = clientMeasurerSettingsRepository.getByMeasurerSystem(shard, ClientMeasurerSystem.NO);

            assertThat(result.size(), equalTo(1));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.MEDIASCOPE);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.NO);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(1), ClientMeasurerSystem.MEDIASCOPE);
        }
    }

    @Test
    public void update_UpdateOneRecord_ReturnNewValue() {
        try {
            var clientMeasurerSettings =
                    new ClientMeasurerSettings()
                            .withClientId(clientIds.get(0))
                            .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                            .withSettings("{\"json\": \"json\"}");

            clientMeasurerSettingsRepository.insertOrUpdate(shard, List.of(clientMeasurerSettings));
            clientMeasurerSettings = clientMeasurerSettingsRepository.getByClientId(shard, clientIds.get(0)).get(0);

            String newSettings = "{\"json\": \"json1\"}";
            clientMeasurerSettings.setSettings(newSettings);

            clientMeasurerSettingsRepository.update(shard, clientMeasurerSettings);
            var updatedRecord = clientMeasurerSettingsRepository.getByMeasurerSystem(shard,
                    ClientMeasurerSystem.MEDIASCOPE);
            assertThat(updatedRecord.get(0).getSettings(), equalTo(newSettings));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(shard, clientIds.get(0),
                    ClientMeasurerSystem.MEDIASCOPE);
        }
    }

    @Test
    public void update_UpdateOneRecord_ReturnOldValue() {
        try {
            String oldSettings = "{\"json\": \"json\"}";
            var clientMeasurerSettings =
                    new ClientMeasurerSettings()
                            .withClientId(clientIds.get(0))
                            .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                            .withSettings(oldSettings);

            clientMeasurerSettingsRepository.insertOrUpdate(shard, List.of(clientMeasurerSettings));
            clientMeasurerSettings = clientMeasurerSettingsRepository.getByClientId(shard, clientIds.get(0)).get(0);

            String newSettings = "{\"json\": \"json1\"}";
            clientMeasurerSettings.setSettings(newSettings);
            clientMeasurerSettings.setLastChange(LocalDateTime.now().minusHours(1));

            clientMeasurerSettingsRepository.update(shard, clientMeasurerSettings);
            var updatedRecord = clientMeasurerSettingsRepository.getByMeasurerSystem(shard,
                    ClientMeasurerSystem.MEDIASCOPE);
            assertThat(updatedRecord.get(0).getSettings(), equalTo(oldSettings));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(shard, clientIds.get(0),
                    ClientMeasurerSystem.MEDIASCOPE);
        }
    }


    @Test
    public void getByClientIdsAndSystem_GetTwoRecords_ReturnTwoRecords() {
        try {
            var clientMeasurerSettingsList =
                    List.of(new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(0))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.NO)
                                    .withSettings("{\"json\": \"json\"}"),
                            new ClientMeasurerSettings()
                                    .withClientId(clientIds.get(1))
                                    .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                                    .withSettings("{\"json\": \"json\"}"));

            clientMeasurerSettingsRepository.insertOrUpdate(shard, clientMeasurerSettingsList);
            var result = clientMeasurerSettingsRepository.getByClientIdsAndSystem(
                    shard, clientIds, ClientMeasurerSystem.MEDIASCOPE);

            assertThat(result.size(), equalTo(2));
            assertThat(result.get(clientIds.get(0)).getClientMeasurerSystem(), equalTo(ClientMeasurerSystem.MEDIASCOPE));
            assertThat(result.get(clientIds.get(1)).getClientMeasurerSystem(), equalTo(ClientMeasurerSystem.MEDIASCOPE));
        } finally {
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.MEDIASCOPE);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(0), ClientMeasurerSystem.NO);
            clientMeasurerSettingsRepository.deleteByClientIdAndSystem(
                    shard, clientIds.get(1), ClientMeasurerSystem.MEDIASCOPE);
        }
    }
}
