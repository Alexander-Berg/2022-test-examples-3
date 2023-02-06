package ru.yandex.direct.grid.processing.service.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;



@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientMutationServiceTurningOffConversionModifiersPopupDoesntAffectOtherFlagsTest {
    @Autowired
    private Steps steps;

    @Autowired
    private ClientMutationService clientMutationService;

    @Autowired
    private ClientService clientService;

    @Test
    public void turningOffConversionModifiersPopupWithAllFlagsEnabled() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        Client client = clientInfo.getClient();

        AppliedChanges<Client> appliedChanges =
                buildChanges(client);

        clientService.update(appliedChanges);

        Client expectedClient = clientService.getClient(clientId)
                .withIsConversionMultipliersPopupDisabled(true);

        clientMutationService.turnOfShowingConversionModifiersPopup(client);


        Client actualClient = clientService.getClient(clientId);

        assertThat(actualClient)
                .isEqualTo(expectedClient);
    }

    private AppliedChanges<Client> buildChanges(Client client) {
        return ModelChanges.build(client, Client.NO_TEXT_AUTOCORRECTION, true)
                .process(false, Client.IS_CONVERSION_MULTIPLIERS_POPUP_DISABLED)
                .process(true, Client.NO_DISPLAY_HREF)
                .process(true, Client.NOT_AGREED_ON_CREATIVES_AUTOGENERATION)
                .process(true, Client.CAN_COPY_CTR)
                .process(true, Client.NOT_CONVERT_TO_CURRENCY)
                .process(true, Client.AUTO_VIDEO)
                .process(true, Client.SUSPEND_VIDEO)
                .process(true, Client.FEATURE_ACCESS_AUTO_VIDEO)
                .process(true, Client.SHARED_ACCOUNT_DISABLED)
                .process(true, Client.FEATURE_CONTEXT_RELEVANCE_MATCH_ALLOWED)
                .process(true, Client.FEATURE_CONTEXT_RELEVANCE_MATCH_INTERFACE_ONLY)
                .process(true, Client.CANT_UNBLOCK)
                .process(true, Client.CAN_PAY_BEFORE_MODERATION)
                .process(true, Client.AS_SOON_AS_POSSIBLE)
                .process(true, Client.AUTO_OVERDRAFT_NOTIFIED)
                .process(true, Client.IS_PRO_STRATEGY_VIEW_ENABLED)
                .process(true, Client.IS_TOUCH)
                .applyTo(client);
    }
}
