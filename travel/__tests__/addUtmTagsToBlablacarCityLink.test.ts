import NationalVersion from '../../../interfaces/NationalVersion';

import addUtmTagsToBlablacarCityLink from '../addUtmTagsToBlablacarCityLink';

const bbkUrl = 'https://www.blablacar.com/search';
const nationalVersions = Object.values(NationalVersion);
const expectedLinkUa = `${bbkUrl}?comuto_cmkt=UA_YANDEXRASP_PSGR_MAIN_none&utm_campaign=UA_YANDEXRASP_PSGR_MAIN_none&utm_medium=Link&utm_source=YANDEXRASP`;
const expectedLinkOthers = `${bbkUrl}?comuto_cmkt=RU_YANDEXRASP_PSGR_MAIN_none&utm_campaign=RU_YANDEXRASP_PSGR_MAIN_none&utm_medium=Link&utm_source=YANDEXRASP`;

function getExpectedUrl(nationalVersion: NationalVersion): string | null {
    switch (nationalVersion) {
        case NationalVersion.by:
        case NationalVersion.uz:
        case NationalVersion.kz:
        case NationalVersion.ru:
            return expectedLinkOthers;
        case NationalVersion.ua:
            return expectedLinkUa;
    }

    return null;
}

describe('addUtmTagsToBlablacarCityLink', () => {
    nationalVersions.forEach(nationalVersion => {
        const expectedUrl = getExpectedUrl(nationalVersion);

        it(`Для "${nationalVersion}" ожидаемый урл должен быть явно определен`, () => {
            expect(expectedUrl).not.toBe(null);
        });

        it(`Для "${nationalVersion}" должен вернуть "${expectedUrl}"`, () => {
            expect(addUtmTagsToBlablacarCityLink(bbkUrl, nationalVersion)).toBe(
                expectedUrl,
            );
        });
    });
});
