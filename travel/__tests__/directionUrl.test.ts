import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import directionUrl from '../directionUrl';

describe('directionUrl', () => {
    it('Должна вернуть правильный url', () => {
        const result = '/city/10/direction?direction=test_dir';

        expect(directionUrl('test_dir', 10, false, Tld.ru, Lang.ru)).toBe(
            result,
        );
    });

    it('Должна вернуть правильный мобильный url', () => {
        const result = '/direction?direction=test_dir';

        expect(directionUrl('test_dir', 10, true, Tld.ru, Lang.ru)).toBe(
            result,
        );
    });
});
