import {purifyPartnerLink} from 'projects/partners/utilities/urls/purify';

const BASE_URL = 'https://travel.yandex.ru/hotels';

describe('purifyPartnerLink', () => {
    it('Вернёт ссылку без изменений если ссылка не содержала параметры', () => {
        expect(purifyPartnerLink(BASE_URL)).toBe(BASE_URL);
    });

    it('Вернёт ссылку без изменений если в ней не было запрещённых параметров', () => {
        expect(purifyPartnerLink(`${BASE_URL}?validParam=1`)).toBe(
            `${BASE_URL}?validParam=1`,
        );
    });

    it('Вернёт ссылку без utm параметров', () => {
        expect(
            purifyPartnerLink(`${BASE_URL}?utm_source=rasp&utm_campaign=main`),
        ).toBe(BASE_URL);
    });

    it('Вернёт ссылку без meta параметров', () => {
        expect(
            purifyPartnerLink(
                `${BASE_URL}?gclid=123&serpReqId=456&validParam=1`,
            ),
        ).toBe(`${BASE_URL}?validParam=1`);
    });

    it('Вернёт ссылку без партнёрских параметров', () => {
        expect(
            purifyPartnerLink(
                `${BASE_URL}?affiliate_clid=noname&validParam=2&affiliate_vid=33&admitad_uid=test&travelpayouts_uid=test`,
            ),
        ).toBe(`${BASE_URL}?validParam=2`);
    });
});
