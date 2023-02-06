import {EAffiliateQueryParams} from 'constants/affiliate/queryParams';
import {distributionUtmParams} from 'projects/partners/constants/partnersUtms';

import {
    getPartnersLink,
    IDistributionQueryParams,
} from 'projects/partners/utilities/urls/getPartnersLink';

const PARTNERS_PARAMS: IDistributionQueryParams = {
    [EAffiliateQueryParams.AFFILIATE_CLID]: 'neo',
    [EAffiliateQueryParams.AFFILIATE_VID]: '4',
};

describe('getPartnersLink', () => {
    it('Выбросит исключение если ссылка невалидная', () => {
        expect(() =>
            getPartnersLink('https://weather.yandex.ru', PARTNERS_PARAMS),
        ).toThrow();
    });

    it('Обработает ссылку без указания протокола', () => {
        expect(
            getPartnersLink(
                'travel.yandex.ru/hotels?affiliate_clid=trinity&validParam=1&utm_source=rasp&serpReqId=456',
                PARTNERS_PARAMS,
            ),
        ).toBe(
            'https://travel.yandex.ru/hotels?validParam=1&affiliate_clid=neo&affiliate_vid=4',
        );
    });

    it('Вернёт ссылку с партнёрскими параметрами', () => {
        expect(
            getPartnersLink(
                'https://travel.yandex.ru/hotels?affiliate_clid=trinity&validParam=1&utm_source=rasp&serpReqId=456',
                PARTNERS_PARAMS,
            ),
        ).toBe(
            'https://travel.yandex.ru/hotels?validParam=1&affiliate_clid=neo&affiliate_vid=4',
        );
    });

    it('Вернёт ссылку с партнёрскими параметрами и utm метками', () => {
        expect(
            getPartnersLink(
                'https://travel.yandex.ru/hotels?affiliate_clid=trinity&validParam=1&utm_source=rasp&serpReqId=456',
                PARTNERS_PARAMS,
                distributionUtmParams,
            ),
        ).toBe(
            'https://travel.yandex.ru/hotels?validParam=1&affiliate_clid=neo&affiliate_vid=4&utm_source=distribution&utm_medium=cpa',
        );
    });
});
