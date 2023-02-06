import NationalVersion from '../../interfaces/NationalVersion';

import shouldRequestBlablacarCityLink from '../shouldRequestBlablacarCityLink';

function getShouldRequestByNationalVersion(
    nationalVersion: NationalVersion,
): boolean | null {
    switch (nationalVersion) {
        case NationalVersion.kz:
        case NationalVersion.ru:
        case NationalVersion.ua:
        case NationalVersion.uz:
            return true;
        case NationalVersion.by:
            return false;
    }

    return null;
}

describe('shouldRequestBlablacarCityLink', () => {
    Object.values(NationalVersion).forEach(nationalVersion => {
        const shouldRequest =
            getShouldRequestByNationalVersion(nationalVersion);

        it(`Необходимость запроса ссылки для "${nationalVersion}" на блаблакар должна быть явно определена`, () => {
            expect(shouldRequest).not.toBe(null);
        });

        it(`Для ${nationalVersion} ${
            shouldRequest ? '' : 'не '
        } должна запрашиваться ссылка на блаблакар`, () => {
            expect(shouldRequestBlablacarCityLink(nationalVersion)).toBe(
                shouldRequest,
            );
        });
    });
});
