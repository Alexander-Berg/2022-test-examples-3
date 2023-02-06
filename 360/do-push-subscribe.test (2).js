'use strict';

const doPushSubscribe = require('./do-push-subscribe.js');
let core;
const mockXiva = jest.fn();

jest.mock('../../models/meta/_helpers/get-folder-by-symbolic-name.js', () => jest.fn());
const getFolderBySymbolName = require('../../models/meta/_helpers/get-folder-by-symbolic-name.js');

jest.mock('../_helpers/uuid.js', () => ({
    getUUID: () => '12345678-1123-1234-1234-123456789112',
    rawUuid: () => '12345678112312341234123456789112'
}));

jest.mock('./push-keys.js', () => {
    return () => ({
        token: 'pushToken',
        client: 'touch-push-test'
    });
});

beforeEach(() => {
    mockXiva.mockResolvedValue('raw');
    getFolderBySymbolName.mockResolvedValue({ folderId: '1' });
    core = {
        auth: {
            get: () => ({ uid: '124345' })
        },
        req: {
            cookies: {
                yandexuid: '67908657'
            }
        },
        service: jest.fn(() => mockXiva)
    };
});

describe('do-push-subscribe (touch)', () => {
    it('function receiving folder id by symbol called', async () => {
        await doPushSubscribe({
            folders: [ '132', '133', '135' ],
            token: 'pushToken'
        }, core);

        expect(getFolderBySymbolName).toBeCalled();
    });

    it('standart request for subscribing (no tabs)', async () => {
        await doPushSubscribe({
            folders: [ '132', '133', '135' ],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('standart request for subscribing (no folders)', async () => {
        await doPushSubscribe({
            tabs: [ 'social' ],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('not array folder and tabs array', async () => {
        await doPushSubscribe({
            folders: '132',
            tabs: [ 'social' ],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('success request for subscribing (with tabs)', async () => {
        await doPushSubscribe({
            folders: [ '132', '133', '135' ],
            tabs: [ 'relevant', 'news' ],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('empty values for folders and tabs: pass default values', async () => {
        await doPushSubscribe({
            folders: [],
            tabs: [],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('empty values for folders and non-empty for tabs: pass default values', async () => {
        await doPushSubscribe({
            folders: [],
            tabs: [ 'social' ],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('empty values for tabs and non-empty for folders: pass default values', async () => {
        await doPushSubscribe({
            folders: [ '113' ],
            tabs: [],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('if folders is array then don\'t convert', async () => {
        await doPushSubscribe({
            folders: [ 113, 122 ],
            tabs: '[]',
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('if tabs is array then don\'t convert', async () => {
        await doPushSubscribe({
            folders: [ 113, 122 ],
            tabs: [ 'news', 'social' ],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('if tabs is empty array then don\'t convert', async () => {
        await doPushSubscribe({
            folders: [ 113, 122 ],
            tabs: [],
            token: 'pushToken'
        }, core);

        expect(mockXiva.mock.calls[0]).toMatchSnapshot();
    });

    it('xiva respond correctly', async () => {
        const result = await doPushSubscribe({
            folders: [ '132', '133', '135' ]
        }, core);

        expect(result).toStrictEqual({ status: 'ok' });
    });

    it('xiva not respond correctly', async () => {
        mockXiva.mockRejectedValue('raw');

        const result = await doPushSubscribe({
            folders: [ '132', '133', '135' ]
        }, core);

        expect(result).toEqual({ status: 'ok' });

    });
});
