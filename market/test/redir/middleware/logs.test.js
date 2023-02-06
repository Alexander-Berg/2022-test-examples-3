const luster = require('luster');

const logMiddleware = require('./logs');
const { createRequest, createResponse, createNext, createAffId } = require('./../utils/create');

beforeEach(() => {
    jest.clearAllMocks();
});

describe('logMiddleware', () => {
    test('default behaviour', () => {
        const req = createRequest({
            headers: {},
            query: {},
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(luster.logWinston.redir.info).toHaveBeenCalled();
        expect(luster.logWinston.redir.info).toHaveBeenCalledTimes(1);
    });

    test('iznanka (affId = 1651)', () => {
        const req = createRequest({
            headers: {},
            query: {
                aff_id: createAffId(1651),
            },
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test('referrer exists', () => {
        const req = createRequest({
            headers: {
                referrer: 'FAKE_REFERRER',
            },
            query: {},
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test('user-agent exists', () => {
        const req = createRequest({
            headers: {
                'user-agent':
                    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:73.0) Gecko/20100101 Firefox/73.0',
            },
            query: {},
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test('unknown user-agent', () => {
        const req = createRequest({
            headers: {
                'user-agent': 'FAKE_USER_AGENT',
            },
            query: {},
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`'yandex_login' exists`, () => {
        const req = createRequest({
            headers: {},
            query: {},
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
            cookies: {
                yandex_login: 'FAKE_YANDEX_LOGIN',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`'yandexuid' exists`, () => {
        const req = createRequest({
            headers: {},
            query: {},
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
            cookies: {
                yandexuid: 'FAKE_YANDEX_UID',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`'ab' object exists`, () => {
        const req = createRequest({
            headers: {},
            query: {
                ab: JSON.stringify({
                    fakeTest1: true,
                    fakeTest2: false,
                    fakeTest3: true,
                }),
            },
            connection: {},
            socket: {
                remoteAddress: 'FAKE_REMOTE_ADDRESS',
            },
        });
        const res = createResponse();
        const next = createNext();

        logMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });
});
