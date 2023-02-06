import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getCanonicalCityUrl from '../getCanonicalCityUrl';

const cityId = 53;
const path = `/city/${cityId}`;

const tld = Tld.ru;
const language = Lang.ru;

describe('getCanonicalCityUrl', () => {
    it('Должна вернуться ссылка', () => {
        expect(getCanonicalCityUrl(cityId, tld, language)).toBe(path);
    });
});
