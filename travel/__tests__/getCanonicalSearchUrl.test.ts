import {FilterTransportType} from '../../transportType';
import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getCanonicalSearchUrl from '../getCanonicalSearchUrl';

const fromSlug = 'yekaterinburg';
const toSlug = 'moscow';

describe('getCanonicalSearchUrl', () => {
    it('Получает корректные ссылки', () => {
        expect(
            getCanonicalSearchUrl({
                transportType: FilterTransportType.all,
                fromSlug,
                toSlug,
                tld: Tld.ru,
                language: Lang.ru,
            }),
        ).toBe('/all-transport/yekaterinburg--moscow');

        expect(
            getCanonicalSearchUrl({
                transportType: FilterTransportType.train,
                fromSlug,
                toSlug,
                tld: Tld.ru,
                language: Lang.ru,
            }),
        ).toBe('/train/yekaterinburg--moscow');

        expect(
            getCanonicalSearchUrl({
                transportType: FilterTransportType.suburban,
                fromSlug,
                toSlug,
                tld: Tld.ru,
                language: Lang.ru,
            }),
        ).toBe('/suburban/yekaterinburg--moscow/today');
    });
});
