import Slug from '../../../interfaces/Slug';

import {getHotelRegionUrl} from '../getHotelRegionUrl';

const context = {
    slug: 'moscow' as Slug,
};

describe('getHotelRegionUrl', () => {
    describe('testing', () => {
        it('testing', () => {
            expect(getHotelRegionUrl(context, false)).toEqual(
                'https://travel-test.yandex.ru/hotels/moscow/',
            );
        });

        it('production', () => {
            expect(getHotelRegionUrl(context, true)).toEqual(
                'https://travel.yandex.ru/hotels/moscow/',
            );
        });
    });
});
