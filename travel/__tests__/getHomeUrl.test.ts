import getHomeUrl from '../getHomeUrl';

import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

const tld = Tld.ru;
const language = Lang.ru;

describe('getHomeUrl', () => {
    it('Вернет "/"', () => {
        expect(getHomeUrl(tld, language)).toBe('/');
    });
});
