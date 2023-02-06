import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getInfoUrl from '../getInfoUrl';

const tld = Tld.ru;
const language = Lang.ru;

describe('getInfoUrl', () => {
    it('Должна вернуться ссылка', () => {
        expect(getInfoUrl('slug', tld, language)).toBe('/info/slug');
        expect(getInfoUrl(1, tld, language)).toBe('/info/1');
        expect(getInfoUrl(1, tld, language, '/station/')).toBe(
            '/info/station/1',
        );
        expect(getInfoUrl(1, tld, language, 'station/')).toBe(
            '/info/station/1',
        );
    });
});
