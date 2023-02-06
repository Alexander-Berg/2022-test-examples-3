import {UTM_CAMPAIGN_DEFAULT, INFORMER_UTM_SOURCE} from '../constants';

import applyInformerUtm from '../applyInformerUtm';

const url = 'https://some.yandex.ru/';
const utmMedium = 'something';
const utmSource = 'yasearch';

describe('applyInformerUtm', () => {
    it('Не переданы utmMedium и utmSource', () => {
        expect(applyInformerUtm(url)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_source=${INFORMER_UTM_SOURCE}`,
        );
    });

    it('Передан utmMedium', () => {
        expect(applyInformerUtm(url, utmMedium)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_medium=${utmMedium}&utm_source=${INFORMER_UTM_SOURCE}`,
        );
    });

    it('Передан utmSource', () => {
        expect(applyInformerUtm(url, undefined, utmSource)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_source=${utmSource}`,
        );
    });

    it('Передан utmMedium и utmSource', () => {
        expect(applyInformerUtm(url, utmMedium, utmSource)).toBe(
            `${url}?utm_campaign=${UTM_CAMPAIGN_DEFAULT}&utm_medium=${utmMedium}&utm_source=${utmSource}`,
        );
    });
});
