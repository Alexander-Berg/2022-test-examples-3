import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getMapsUrl from '../getMapsUrl';

describe('getMapsUrl', () => {
    it('Ожидается корректный урл', () => {
        expect(getMapsUrl(56, 36, Tld.ru, Lang.ru)).toBe(
            'https://yandex.ru/maps?text=36%2C56',
        );
    });
});
