package ru.yandex.market.mbi.yt.replicator;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.transfermanager.TransferClient;
import ru.yandex.market.core.transfermanager.TransferCreateRequest;
import ru.yandex.market.core.transfermanager.TransferStatusResponse;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YtReplicatorTest extends FunctionalTest {
    private final Cypress cypress = mock(Cypress.class);

    private YtReplicator ytReplicator;
    private YtTemplate turboYtTemplate;

    @Autowired
    private TransferClient transferClient;

    @BeforeEach
    void setUp() {
        Yt yt = mock(Yt.class);
        turboYtTemplate = new YtTemplate(new YtCluster[]{
                new YtCluster("test1", yt),
                new YtCluster("test2", yt)
        });
        ytReplicator = new YtReplicator(turboYtTemplate, transferClient, 100, 5);
    }

    @Test
    @DisplayName("Тест репликации данных")
    void testReplication() {
        YtCluster cluster1 = turboYtTemplate.getClusters()[0];
        YtCluster cluster2 = turboYtTemplate.getClusters()[1];
        reset(cluster1.getYt());
        reset(cluster2.getYt());
        when(cluster2.getYt().cypress()).thenReturn(cypress);

        YPath testTable = YPath.cypressRoot().child("home/replicatorTestTable");
        YPath testTableLink = YPath.cypressRoot().child("home/latest");

        TransferCreateRequest request = new TransferCreateRequest(
                cluster1.getSimpleName(), testTable.toString(),
                cluster2.getSimpleName(), testTable.toString()
        );

        // репликация
        when(transferClient.createTask(refEq(request))).thenReturn("1");
        when(transferClient.getTaskStatus(eq("1")))
                .thenReturn(statusResponse("pending"))
                .thenReturn(statusResponse("running"))
                .thenReturn(statusResponse("completed"));

        ytReplicator.replicate(cluster1, testTable);

        verify(transferClient).createTask(refEq(request));
        verify(transferClient, times(3)).getTaskStatus(eq("1"));
        // создание линка
        verify(cypress).remove(eq(Optional.empty()), eq(false), eq(testTableLink), eq(false), eq(true));
        verify(cypress).link(eq(Optional.empty()), eq(false), eq(testTable), eq(testTableLink), eq(false),
                eq(false));
    }

    private static TransferStatusResponse statusResponse(String state) {
        TransferStatusResponse response = new TransferStatusResponse();
        response.setState(state);
        return response;
    }
}
