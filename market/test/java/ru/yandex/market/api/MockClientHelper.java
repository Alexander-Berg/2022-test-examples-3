package ru.yandex.market.api;

import java.util.Optional;

import org.mockito.Mockito;

import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;

/**
 * @authror dimkarp93
 */
public class MockClientHelper {
    private ClientHelper clientHelper;

    public MockClientHelper(ClientHelper clientHelper) {
        this.clientHelper = clientHelper;
        Mockito.when(clientHelper.findType(Mockito.isNull(Client.class))).thenReturn(Optional.empty());
        Mockito.when(clientHelper.findType(Mockito.isNull(String.class))).thenReturn(Optional.empty());
    }


    public void is(ClientHelper.Type type, boolean result) {
        Mockito
            .when(
                clientHelper.is(
                    Mockito.eq(type),
                    Mockito.any(Client.class)
                )
            )
            .thenReturn(result);
         Mockito
            .when(
                clientHelper.is(
                    Mockito.eq(type),
                    Mockito.anyString()
                )
            )
            .thenReturn(result);

         Mockito
             .when(
                     clientHelper.findType(Mockito.isNotNull(Client.class))
             )
             .thenReturn(Optional.ofNullable(type));
        Mockito
            .when(
                    clientHelper.findType(Mockito.isNotNull(String.class))
            )
            .thenReturn(Optional.ofNullable(type));

    }

    public void rearr(String rearr) {
        Mockito
                .when(
                        clientHelper.getRearrs(
                                Mockito.any(ClientHelper.Type.class)
                        )
                )
                .thenReturn(rearr);
    }
}
