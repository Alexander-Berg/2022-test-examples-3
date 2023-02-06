import {UTM_CAMPAIGN_DEFAULT, UTM_SOURCE} from '../constants';

import IStateSeoQueryParams from '../../../interfaces/state/IStateSeoQueryParams';

import applyUtm from '../applyUtm';

const url = 'https://some.yandex.ru/';
const outerUrl = 'https://service.com';
const reqId = '123';
const utmMedium = 'something';
const from = 'koldun';
const utmSource = 'yasearch';
const clientId = '999';

describe('applyUtm', () => {
    it('Не передан utmMedium и clientId, нет параметров с которыми пришли на Расписания', () => {
        expect(applyUtm(url)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_source=${UTM_SOURCE}`,
        );
    });

    it('Передан clientId', () => {
        expect(applyUtm(url, undefined, clientId)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_content=${clientId}&utm_source=${UTM_SOURCE}`,
        );
    });

    it('Передан reqId', () => {
        expect(applyUtm(url, {reqId} as IStateSeoQueryParams)).toBe(
            `${url}?req_id=${reqId}&utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_source=${UTM_SOURCE}&wizardReqId=${reqId}`,
        );
    });

    it('Передан параметр с которыми пришли на расписания', () => {
        expect(applyUtm(url, {utmSource} as IStateSeoQueryParams)).toBe(
            `${url}?utm_campaign=${utmSource}&utm_source=${UTM_SOURCE}`,
        );
        expect(applyUtm(url, {from} as IStateSeoQueryParams)).toBe(
            `${url}?utm_campaign=${from}&utm_source=${UTM_SOURCE}`,
        );
    });

    it('Передан utmMedium', () => {
        expect(applyUtm(url, undefined, undefined, utmMedium)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_medium=${utmMedium}&utm_source=${UTM_SOURCE}`,
        );
    });

    it('Ссылка ведёт не на другой яндексовый сервис', () => {
        expect(applyUtm(outerUrl, {utmSource} as IStateSeoQueryParams)).toBe(
            outerUrl,
        );
    });

    it('Явно передали другой utmSource', () => {
        expect(applyUtm(url, undefined, undefined, undefined, utmSource)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_source=${utmSource}`,
        );
    });
});
