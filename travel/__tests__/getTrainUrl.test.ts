import getTrainUrl from '../getTrainUrl';

describe('getTrainUrl', () => {
    it('travel', () => {
        expect(
            getTrainUrl({
                isProduction: true,
                path: '',
            }),
        ).toBe('https://travel.yandex.ru/trains');
    });

    it('path + query', () => {
        expect(
            getTrainUrl({
                isProduction: false,
                path: '/moscow--saint-petersburg/2019-05-30',
                query: {utm_source: 'rasp'},
            }),
        ).toBe(
            'https://travel-test.yandex.ru/trains/moscow--saint-petersburg/2019-05-30?utm_source=rasp',
        );
    });
});
