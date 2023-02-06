'use strict';

require('./_helpers/push-subscription-id.js');

const doPushSubscribe = require('./do-push-subscribe.js');

let core;
let subscription;
const mockXiva = jest.fn();

jest.mock('../../models/meta/_helpers/get-folder-by-symbolic-name.js', () => jest.fn());
const getFolderBySymbolName = require('../../models/meta/_helpers/get-folder-by-symbolic-name.js');

beforeEach(() => {
    mockXiva.mockResolvedValue('raw');
    getFolderBySymbolName.mockResolvedValue({ folderId: '1' });
    core = {
        auth: {
            get: jest.fn(() => ({ sids: '' }))
        },
        service: jest.fn(() => mockXiva)
    };

    subscription = '{"endpoint":"","expirationTime":null,"keys":{"p26dh":"BB","auth":"BRQ"}}';
});

describe('do-push-subscribe', () => {
    it('function receiving folder id by symbol called', async () => {
        await doPushSubscribe({
            folders: '["132","133","135"]',
            subscription
        }, core);

        expect(getFolderBySymbolName).toBeCalled();
    });

    it('success request for subscribing (no tabs)', async () => {
        await doPushSubscribe({
            folders: '["132","133","135"]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('success request for subscribing (with tabs)', async () => {
        await doPushSubscribe({
            folders: '["132","133","135"]',
            tabs: '["relevant", "news"]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('empty values for folders and tabs: pass default values', async () => {
        await doPushSubscribe({
            folders: '[]',
            tabs: '[]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('empty values for folders and non-empty for tabs: pass default values', async () => {
        await doPushSubscribe({
            folders: '[]',
            tabs: '["social"]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('empty values for tabs and non-empty for folders: pass default values', async () => {
        await doPushSubscribe({
            folders: '["113"]',
            tabs: '[]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('if has calendar sid, then subscribe push from calendar', async () => {
        core.auth.get = jest.fn(() => ({ sids: '31' }));

        await doPushSubscribe({
            folders: '["113"]',
            tabs: '[]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
        expect(mockXiva.mock.calls[1]).toMatchSnapshot();
    });

    it('request non empty tabs and folders', async () => {
        await doPushSubscribe({
            folders: '[ "123" ]',
            tabs: '[ "news" ]',
            subscription
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('xiva not responded correctly', async () => {
        mockXiva.mockRejectedValue('502');

        try {
            await doPushSubscribe({
                folders: '[]',
                tabs: '["social"]',
                subscription
            }, core);
        } catch (error) {
            expect(error).toEqual('502');
        }
    });

    it('xiva responded correctly', async () => {
        const result = await doPushSubscribe({
            folders: '[]',
            tabs: '["social"]',
            subscription
        }, core);

        expect(result).toEqual([ 'raw' ]);
    });
});
