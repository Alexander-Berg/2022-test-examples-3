import {
    OsFamily,
} from '@yandex-int/frontend-apphost-context';
import { EPage } from '~/libs/pages';
import { IDeviceSource, IBlackboxSource, IRequestSource } from '~/report-renderer/apphost';
import { getFApiRequest } from '~/report-renderer/actions/fapi_pre/request';

const DEVICE_TEMPLATE: IDeviceSource = {
    browser: {
        base: 'Chromium',
        baseVersion: '89.0.4389.86',
        engine: 'WebKit',
        engineVersion: '537.36',
        name: 'YandexBrowser',
        version: '21.3.0.740',
    },
    device: {
        id: '',
        model: '',
        name: '',
        vendor: '',
    },
    is_browser: true,
    is_mobile: false,
    is_robot: false,
    is_tablet: false,
    is_touch: false,
    is_tv: false,
    os: {
        family: OsFamily.MacOS,
        name: '',
        version: '10.15.7',
    }
};

// @ts-ignore
const blackbox: IBlackboxSource = {
    status: {
        id: 0,
        value: 'OK'
    },
    user_ticket: 'AAA BBB CCC',
};

const request: IRequestSource = {
    ip: '::1',
    hostname: 'example.com',
    scheme: 'https',
    proto: 'https',
    path: '/',
    uri: '/',
    reqid: '',
    method: 'GET',
    tld: 'com',
    cookies_parsed: {},
    ua: '',
    headers: {},
    time_epoch: 0,
    params: {},
    is_internal: 0,
};

describe('getFApiRequest()', () => {
    it('Оффер', () => {
        const url = 'https://m.market.yandex.ru/offer/SrlQOMl1NgDMAa9fK4raWw?businessId=10671581';
        const page = EPage.offer;
        const device = { ...DEVICE_TEMPLATE };
        device.os.family = OsFamily.Android;

        const expectedPayload = JSON.stringify({
            params: [{
                offerIds: ['SrlQOMl1NgDMAa9fK4raWw'],
                billingZone: 'default',
            }]
        });

        expect(getFApiRequest({ device, blackbox, url, page, request })).toStrictEqual({
            method: 1,
            scheme: 2,
            path: '/api/v1/?name=resolveOffersById',
            headers: [
                { Name: 'Content-Type', Value: 'application/json' },
                { Name: 'cache-control', Value: 'no-cache' },
                { Name: 'X-fapi-internal', Value: '1' },
                { Name: 'api-platform', Value: 'ANDROID' },
                { Name: 'X-Forwarded-For', Value: '::1' },
                { Name: 'X-Real-IP', Value: '::1' },
                { Name: 'X-Forwarded-Host', Value: 'example.com' },
                { Name: 'X-Forwarded-Proto', Value: 'https' },
                { Name: 'X-Ya-User-Ticket', Value: 'AAA BBB CCC' },
            ],
            content: Buffer.from(expectedPayload, 'utf-8'),
        });
    });
});
