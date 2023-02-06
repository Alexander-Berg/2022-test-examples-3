import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getCityUrl from '../getCityUrl';

const cityId = 53;
const path = `/city/${cityId}`;

const tld = Tld.ru;
const language = Lang.ru;

describe('buildCityUrl', () => {
    it('Должна вернуться ссылка', () => {
        expect(getCityUrl(cityId, tld, language)).toBe(path);
    });
});
