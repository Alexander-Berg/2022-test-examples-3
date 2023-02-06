import moment from 'moment';

import getPlaneThreadUrl from '../getPlaneThreadUrl';

describe('getPlaneThreadUrl', () => {
    it('', () => {
        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                departureFrom: '2019-04-01 05:15:00',
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp&when=2019-04-01',
        );

        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                departure: '2019-04-01',
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp&when=2019-04-01',
        );

        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp',
        );

        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                query: {utm_campaign: 'test'},
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_campaign=test&utm_source=rasp',
        );
    });

    it('Учет таймзоны в свойствах departure и departureFrom', () => {
        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                departure: '2021-03-25T23:50:00+00:00',
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp&when=2021-03-25',
        );

        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                departureFrom: '2021-03-25T23:50:00+00:00',
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp&when=2021-03-25',
        );
    });

    it('Работа свойства departureMoment', () => {
        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                departureMoment: moment.parseZone('2021-03-25T23:50:00+00:00'),
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp&when=2021-03-25',
        );

        // Если есть departureMoment, то departure и departureFrom игнорируются
        expect(
            getPlaneThreadUrl({
                numberPlane: 'SU 1409',
                departure: '2020-07-01',
                departureFrom: '2020-07-01T23:50:00+00:00',
                departureMoment: moment.parseZone('2021-03-25T23:50:00+00:00'),
                isProduction: true,
            }),
        ).toBe(
            'https://travel.yandex.ru/avia/flights/SU-1409/?utm_source=rasp&when=2021-03-25',
        );
    });
});
