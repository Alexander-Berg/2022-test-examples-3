import getCanonicalHomeUrl from '../getCanonicalHomeUrl';

import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

const tld = Tld.ru;
const language = Lang.ru;

describe('getCanonicalHomeUrl', () => {
    it('Вернет "/"', () => {
        expect(getCanonicalHomeUrl(tld, language)).toBe('/');
    });
});
