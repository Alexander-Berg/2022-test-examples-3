import getHotelUrl from '../getHotelUrl';

describe('getHotelUrl', () => {
    it('travel', () => {
        expect(getHotelUrl({isProduction: true})).toBe(
            'https://travel.yandex.ru/hotels',
        );

        expect(
            getHotelUrl({
                isProduction: true,
                path: '',
            }),
        ).toBe('https://travel.yandex.ru/hotels');
    });

    it('path + query', () => {
        expect(
            getHotelUrl({
                isProduction: false,
                path: '/search',
                query: {geoId: 213, checkinDate: '2023-04-13'},
            }),
        ).toBe(
            'https://travel-test.yandex.ru/hotels/search?checkinDate=2023-04-13&geoId=213',
        );
    });
});
