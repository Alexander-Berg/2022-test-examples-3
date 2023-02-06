'use strict';

const got = require('got');
const nock = require('nock');
const { express, middleware: { cookieParser } } = require('@yandex-int/duffman');
const route = require('./index.js');

jest.mock('@yandex-int/yandex-geobase', () => ({
    default: { v6: () => ({ getRegionByIp: () => 213 }) }
}));
jest.unmock('@yandex-int/duffman');

const core = prepareCore();

let server;
let request;

beforeEach((done) => {
    prepareNocks(core);
    server = express()
        .use(cookieParser)
        .use('/message-part/:name?', route)
        .listen(done);

    const { port } = server.address();

    request = (name, authCookie, query) =>
        got(`http://localhost:${port}/message-part/${name}`, {
            agent: false, followRedirect: false,
            headers: { cookie: authCookie },
            query
        });

    nock.enableNetConnect(`localhost:${port}`);
});

afterEach((done) => {
    server.close(done);
    nock.cleanAll();
});

test('happy path', async () => {
    const { body } = await request('TEST.JPG', 'Session_id=FAKE', {
        _uid: '42',
        name: 'TEST.JPG',
        hid: '1.1',
        ids: '12345',

        archive: 'zip',
        exif_rotate: 'y',
        max_size: 'max_size',
        no_disposition: 'y',
        resize: 'resize',
        thumb: 'y',
        thumb_size: 'thumb_size'
    });

    expect(body).toBe(
        'Found. Redirecting to https://retriever-qa.mail.yandex.net/message_part_real/TEST.JPG?' +
        'archive=zip&' +
        'exif_rotate=y&' +
        'max_size=max_size&' +
        'no_disposition=y&' +
        'resize=resize&' +
        'thumb=y&' +
        'thumb_size=thumb_size&' +
        'name=TEST.JPG&' +
        'sid=SID_1.1'
    );
});

test('weird name', async () => {
    const { body } = await request('', 'Session_id=FAKE', {
        _uid: '42',
        name: '',
        hid: '1.1',
        ids: '12345'
    });

    expect(body).toBe(
        'Found. Redirecting to https://retriever-qa.mail.yandex.net/message_part_real/?' +
        'name=&' +
        'sid=SID_1.1'
    );
});

test('no mid', async () => {
    const { body } = await request('TEST.JPG', 'Session_id=FAKE', {
        name: 'TEST.JPG',
        hid: '1.1'
    });

    expect(body).toBe('');
});

test('no hid', async () => {
    const { body } = await request('TEST.JPG', 'Session_id=FAKE', {
        name: 'TEST.JPG',
        hid: '1.1'
    });

    expect(body).toBe('');
});

test('no auth', async () => {
    const { body } = await request('TEST.JPG', '', {
        name: 'TEST.JPG',
        hid: '1.1',
        ids: '12345'
    });

    expect(body).toBe('');
});

function prepareCore() {
    const ApiCore = require('../helpers/api-core/index.js');
    const httpMock = require('node-mocks-http');
    const req = httpMock.createRequest({});
    const res = httpMock.createResponse();

    const setAuth = (core) => {
        core.auth.set({
            mdb: 'mdb1',
            suid: '34',
            timezone: 'Europe/Moscow',
            tz_offset: -180,
            uid: '12',
            users: []
        });
    };

    const core = new ApiCore(req, res);
    setAuth(core);
    return core;
}

function prepareNocks(core) {
    const auth = core.auth.get();

    nock(core.config.services.meta)
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .get('/v2/attach_sid')
        .reply(200, {
            1.1: 'SID_1.1',
            all: 'SID_ALL',
            1.2: 'SID_1.2'
        });

    nock(core.config.services.akita)
        .filteringPath((path) => path.replace(/\?.*/, ''))
        .get('/auth')
        .reply(200, {
            account_information: {
                account: {
                    userId: auth.uid,
                    serviceUserId: auth.suid,
                    mailDataBase: auth.mdb,
                    timeZone: {
                        timezone: auth.timezone,
                        offset: auth.tz_offset
                    },
                    karma: {},
                    userTicket: 'tvm-user-ticket'
                },
                addresses: {
                    defaultAddress: '',
                    internalAddresses: []
                }
            }
        });
}
