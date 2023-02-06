import ISegmentTariffClass from '../../../interfaces/segment/ISegmentTariffClass';

import ybusUrl from '../ybusUrl';

describe('ybusUrl', () => {
    const baseHost = 'https://yandex.ru/bus/ride/c2/c213/2017-09-07/';
    const utmMedium = 'search_segment_title';
    const classLink = {
        parsedUrl: {
            pathname: baseHost,
            query: {
                utm_source: 'rasp',
            },
        },
    } as unknown as ISegmentTariffClass;

    it('Ссылка на покупку Автобусов', () => {
        expect(ybusUrl(classLink, utmMedium)).toBe(
            `${baseHost}?utm_medium=${utmMedium}&utm_source=rasp`,
        );
    });
});
