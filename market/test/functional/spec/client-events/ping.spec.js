'use strict';

const _ = require('underscore');

const Client = require('./../../lib/client');

describe('ping statistics', () => {
    describe('server should log ping event', () => {
        test('SaveFrom', async () => {
            const client = new Client(Client.SETTINGS.PARTNERS.SAVE_FROM, Client.SETTINGS.USERS.SAVE_FROM);
            const requestResult = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_SAVEFROM);
            const pingEvents = requestResult.logs.clientEvent.filter(({ event }) => event === 'ping');

            expect(pingEvents).toHaveLength(1);
        });

        test('YaBro', async () => {
            const client = new Client();
            const requestResult = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_YABRO);
            const pingEvents = requestResult.logs.clientEvent.filter(({ event }) => event === 'ping');

            expect(pingEvents).toHaveLength(1);
        });
    });

    describe('is_master parameter', () => {
        test('user without any settings should log ping event with is_master=1', async () => {
            const client = new Client();
            const requestResult = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_YABRO);
            const pingEvent = requestResult.logs.clientEvent.find(({ event }) => event === 'ping');

            expect(pingEvent.is_master).toBe(1);
        });

        test('user with SaveFrom extension should log pings from SaveFrom with is_master=1', async () => {
            const client = new Client(Client.SETTINGS.SAVE_FROM);
            const requestResult = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_SAVEFROM);
            const pingEvent = requestResult.logs.clientEvent.find(({ event }) => event === 'ping');

            expect(pingEvent.is_master).toBe(1);
        });

        test('user with SaveFrom extension should log pings from YaBro with is_master=0', async () => {
            const client = new Client(Client.SETTINGS.PARTNERS.SAVE_FROM);
            const requestResult = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_YABRO);
            const pingEvent = requestResult.logs.clientEvent.find(({ event }) => event === 'ping');

            expect(pingEvent.is_master).toBe(0);
        });
    });

    describe('cookies', () => {
        test('request from SF should not create the svt-partner cookie', async () => {
            const client = new Client();
            const result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_SAVEFROM);

            expect(result.getCookieValue('svt-partner')).toBeUndefined();
            expect(result.getCookieValue('svt-user')).toBeTruthy();
        });

        test('request from YaBro should create the svt-partner cookie', async () => {
            const client = new Client();
            const result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_YABRO);
            const requestPartnerSettings = Client.CLIENT_EVENTS.PING_YABRO.body.settings;
            const partnerSettings = result.getCookieValue('svt-partner');

            expect(partnerSettings).toBeTruthy();
            expect(_.pick(partnerSettings, 'clid', 'affId', 'optOutAccepted')).toEqual({
                clid: requestPartnerSettings.clid,
                affId: requestPartnerSettings.affId,
                optOutAccepted: null
            });
            expect(result.getCookieValue('svt-user')).toBeTruthy();
        });

        test('request from YaBro should not modify clid from cookies', async () => {
            const client = new Client(Client.SETTINGS.PARTNERS.SAVE_FROM);
            const result = await client.request(Client.ROUTES.CLIENT_EVENT, Client.CLIENT_EVENTS.PING_YABRO);

            expect(result.getCookieValue('svt-partner')).toBeUndefined();
        });
    });
});
