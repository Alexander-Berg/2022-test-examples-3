const sinon = require('sinon');

const settings = require('../../../data/settings');
const Client = require('../../../lib/client');

async function sendIphoneRequestWithButton(client) {
    return await client.request(
        Client.ROUTES.PRODUCTS,
        Client.PRODUCT_REQUESTS.SOVETNIK.IPHONE_BTN_SS,
        Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
        Client.API_MARKET_MOCKS.SHOPS.EMPTY,
        Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
        Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
        Client.API_MARKET_MOCKS.MODEL.IPHONE,
        Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
        Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
    );
}

async function sendIphoneRequest(client) {
    return await client.request(
        Client.ROUTES.PRODUCTS,
        Client.PRODUCT_REQUESTS.SOVETNIK.IPHONE,
        Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
        Client.API_MARKET_MOCKS.SHOPS.EMPTY,
        Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
        Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
        Client.API_MARKET_MOCKS.MODEL.IPHONE,
        Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
        Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
    );
}

// TODO: rewrite
describe.skip('logic for three times pricebar closed', () => {
    afterEach(() => {
        Date.now.restore && Date.now.restore();
    });

    test('should return rule', async () => {
        const shift = 10 * 60 * 1000;
        const date = new Date(2017, 8, 10);

        sinon.stub(Date, 'now').returns(date.getTime());

        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.WITH_PRICEBAR_CLOSINGS_COUNT);
        let result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);

        let userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift * 2);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        result = await sendIphoneRequest(client);

        expect(result.response.rules).toEqual(['too-many-bar-closings']);
        expect(result.response.model).toBeFalsy();
    });

    test('should return rule and response', async () => {
        const shift = 10 * 60 * 1000;
        const date = new Date(2017, 8, 10);

        sinon.stub(Date, 'now').returns(date.getTime());

        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.WITH_PRICEBAR_CLOSINGS_COUNT);
        let result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);

        let userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift * 2);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        result = await sendIphoneRequestWithButton(client);

        expect(result.response.rules).toEqual(['too-many-bar-closings']);
        // expect(result.logs.product.view_type).toEqual('button');
        expect(result.response.model).toBeTruthy();
    });

    test('should not forbid requests is diff was more then 1 hour', async () => {
        const shift = 30 * 60 * 1000;
        const date = new Date(2017, 8, 10);

        sinon.stub(Date, 'now').returns(date.getTime());

        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.WITH_PRICEBAR_CLOSINGS_COUNT);
        let result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);

        let userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift * 2);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        result = await sendIphoneRequest(client);
        expect(result.response.offers).toBeTruthy();
        expect(result.getCookieValue('svt-user').lastHourBarClosingsCount).toBe(2);
    });

    test('should not return rule after 3 hours', async () => {
        const shift = 10 * 60 * 1000;
        const date = new Date(2017, 8, 10);

        sinon.stub(Date, 'now').returns(date.getTime());

        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.WITH_PRICEBAR_CLOSINGS_COUNT);
        let result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);

        let userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift * 2);
        result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PRICEBAR_CLOSE);
        userSettings = result.getCookieValue('svt-user');
        client.applySettings({}, userSettings);

        Date.now.returns(date.getTime() + shift * 30);

        result = await sendIphoneRequest(client);

        expect(result.response.offers).toBeTruthy();
    });
});
