/* eslint-env node, jest */
import nock from 'nock';
import { nockLog, NockLog } from '@yandex-int/nock-log';

import { config } from '../lib/config';

export function deepFreeze<T extends object>(object: T): T {
    Object.freeze(object);

    Object.getOwnPropertyNames(object).forEach(prop => {
        if (object.hasOwnProperty(prop)) {
            // Нельзя правильно типизировать из-за того, что есть '{}'
            // @ts-ignore
            const value = object[prop];

            if (value !== null && (typeof value === 'object' || typeof value === 'function')) {
                deepFreeze(value);
            }
        }
    });

    return object;
}

export function mockNetwork() {
    const mock = {
        nock: nock as NockLog['nock'],
        getLog: (() => []) as NockLog['getLog'],

        mockUaTraits: (params: MockUaTraitsParams) => mockUaTraits(mock.nock, params),
        mockTvm: (params?: MockTvmParams) => mockTvm(mock.nock, params),
        mockBlackbox: (params: MockBlackboxParams) => mockBlackbox(mock.nock, params),
        mockGeobase: () => mockGeobase(mock.nock),
        mockGeocoder: () => mockGeocoder(mock.nock),
        mockLaas: () => mockLaas(mock.nock),
        mockPersAddress: () => mockPersAddress(mock.nock),
    };

    beforeEach(() => {
        nock.disableNetConnect();
        nock.enableNetConnect('127.0.0.1');

        const requestsLog = nockLog();

        mock.nock = requestsLog.nock.bind(requestsLog);
        mock.getLog = requestsLog.getLog.bind(requestsLog);
    });

    afterEach(() => {
        nock.cleanAll();
        nock.enableNetConnect();
    });

    afterAll(() => {
        // https://github.com/nock/nock/issues/1817
        nock.restore();
    });

    return mock;
}

type MockUaTraitsParams = {
    data: object;
    statusCode?: number;
};
function mockUaTraits(nock: NockLog['nock'], { data, statusCode = 200 }: MockUaTraitsParams) {
    return (
        nock('http://uatraits-test.qloud.yandex.ru')
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/json',
            })
            .post('/v0/detect')
            .reply(statusCode, data)
    );
}

type MockTvmParams =
    | {
          blackboxErr?: boolean;
          geocoderErr?: boolean;
          persAddressErr?: boolean;
      }
    | undefined;
function mockTvm(
    nock: NockLog['nock'],
    { blackboxErr = false, geocoderErr = false, persAddressErr = false }: MockTvmParams = {}
) {
    return (
        nock(config.tvm.serverUrl)
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/json',
            })
            .get(/.*/)
            .reply(200, {
                blackbox: { ticket: blackboxErr ? '' : 'blackbox-service-ticket' },
                geocoder: { ticket: geocoderErr ? '' : 'geocoder-service-ticket' },
                persAddress: { ticket: persAddressErr ? '' : 'persAddress-service-ticket' },
            })
    );
}

export type BlackboxUser = Partial<{
    uid: string;
    login: string;
    avatarId: string;
    displayName: string;
}>;
type MockBlackboxParams = {
    isValid: boolean;
    user?: BlackboxUser;
    fatal?: boolean;
};
export const defaultBlackboxUid = '123';
function mockBlackbox(nock: NockLog['nock'], { isValid, user, fatal = false }: MockBlackboxParams) {
    const userMock: BlackboxUser = {
        uid: defaultBlackboxUid,
        login: 'test-user',
        avatarId: '1/2-3',
        displayName: 'Test User',
        ...user,
    };

    return (
        nock(config.blackbox.api)
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/json',
            })
            .get('/blackbox')
            .query(true)
            .reply(fatal ? 500 : 200, {
                status: { value: isValid ? 'VALID' : 'INVALID' },
                error: 'OK',
                uid: isValid ? { value: userMock.uid, lite: false, hosted: false } : undefined,
                login: userMock.login,
                regname: userMock.login,
                display_name: { name: userMock.displayName, avatar: { default: userMock.avatarId, empty: false } },
                attributes: { '1008': userMock.login },
                user_ticket: 'tvm-user-ticket',
            })
    );
}

function mockGeobase(nock: NockLog['nock']) {
    return (
        nock(config.geobase.origin)
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/json',
            })
    );
}

function mockGeocoder(nock: NockLog['nock']) {
    return (
        nock(config.geocoder.origin)
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/x-protobuf',
            })
            .get(config.geocoder.path)
    );
}

function mockLaas(nock: NockLog['nock']) {
    return (
        nock(config.laas.origin)
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/json',
            })
            .get('/region')
    );
}

function mockPersAddress(nock: NockLog['nock']) {
    return (
        nock(config.persAddress.origin)
            // Для ретраев
            .persist()
            .defaultReplyHeaders({
                'Content-Type': 'application/json',
            })
    );
}
