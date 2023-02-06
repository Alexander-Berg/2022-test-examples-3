/* eslint-disable max-len */

'use strict';

const Client = require('./../../lib/client');

async function sendIphoneRequest(client, isAccepted = true) {
    Client.PRODUCT_REQUESTS.SAVEFROM.IPHONE.query.settings.optOutAccepted = !isAccepted ? isAccepted : undefined;
    return client.request(
        Client.ROUTES.PRODUCTS,
        Client.PRODUCT_REQUESTS.SAVEFROM.IPHONE,
        Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
        Client.API_MARKET_MOCKS.SHOPS.EMPTY,
        Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
        Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
        Client.API_MARKET_MOCKS.MODEL.IPHONE,
        Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
        Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND
    );
}

async function changeOptOutStatus(client, isAccepted) {
    return client.request(
        Client.ROUTES.SETTINGS,
        isAccepted ? Client.SETTINGS_REQUESTS.OPT_OUT_ACCEPTED : Client.SETTINGS_REQUESTS.OPT_OUT_REJECTED
    );
}

// TODO: rewrite
describe('opt-out', () => {
    test('should not have a command to show an opt-in when a user already accepted it', async () => {
        const client = new Client();
        let result = await sendIphoneRequest(client);
        let partnerSettings = result.getCookieValue('svt-partner');
        let userSettings = result.getCookieValue('svt-user');

        expect(result.response.settings.needShowOptIn).toBeTruthy();
        expect(partnerSettings.optOutAccepted).toBeFalsy();

        // следующий запрос пойдет с настройками, которые вернул предыдущий запрос, т.е. у юзера уже будут тапки SF
        client.applySettings(partnerSettings, userSettings);

        result = await changeOptOutStatus(client, true);
        partnerSettings = result.getCookieValue('svt-partner');
        userSettings = result.getCookieValue('svt-user');

        // следующий запрос пойдет с настройками, которые вернул предыдущий запрос, т.е. у юзера будет принят опт-аут
        client.applySettings(partnerSettings, userSettings);

        expect(partnerSettings.optOutAccepted).toBeTruthy();

        result = await sendIphoneRequest(client);

        expect(result.response.settings.needShowOptIn).toBeFalsy();
    });

    test('should have "opt-in-rejected" rule if a user declined an opt-out after accepting', async () => {
        const client = new Client();
        let result = await sendIphoneRequest(client, false);
        let partnerSettings = result.getCookieValue('svt-partner');
        let userSettings = result.getCookieValue('svt-user');

        client.applySettings(partnerSettings, userSettings);

        result = await changeOptOutStatus(client, true);
        partnerSettings = result.getCookieValue('svt-partner');
        userSettings = result.getCookieValue('svt-user');

        client.applySettings(partnerSettings, userSettings);

        result = await changeOptOutStatus(client, false);
        partnerSettings = result.getCookieValue('svt-partner');
        userSettings = result.getCookieValue('svt-user');

        client.applySettings(partnerSettings, userSettings);

        expect(partnerSettings.optOutAccepted).toBeFalsy();

        result = await sendIphoneRequest(client, false);

        expect(result.response.rules[0]).toBe('offer-rejected');
    });

    test('should have "opt-in-rejected" rule if a user declined an opt-out and svt-partner was removed', async () => {
        const client = new Client();
        let result = await sendIphoneRequest(client);
        const partnerSettings = result.getCookieValue('svt-partner');
        let userSettings = result.getCookieValue('svt-user');

        client.applySettings(partnerSettings, userSettings);
        client.applySettings(partnerSettings, userSettings);
        result = await changeOptOutStatus(client, false);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);
        result = await sendIphoneRequest(client);

        expect(result.response.rules[0]).toBe('offer-rejected');
    });

    test('should not have a command to show an opt-in when a user already accepted it and svt-partner was removed', async () => {
        const client = new Client();
        let result = await sendIphoneRequest(client);
        const partnerSettings = result.getCookieValue('svt-partner');
        let userSettings = result.getCookieValue('svt-user');

        expect(result.response.settings.needShowOptIn).toBeTruthy();
        expect(partnerSettings.optOutAccepted).toBeFalsy();

        client.applySettings(partnerSettings, userSettings);
        result = await changeOptOutStatus(client, true);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);
        result = await sendIphoneRequest(client);

        expect(result.response.settings.needShowOptIn).toBeFalsy();
    });
});
