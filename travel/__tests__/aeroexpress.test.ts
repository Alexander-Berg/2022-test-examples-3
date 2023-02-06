import Tld from '../../interfaces/Tld';
import Lang from '../../interfaces/Lang';

import {TransportType} from '../transportType';
import IStation from '../../interfaces/state/stationsGroup/IStation';

import {
    filterAirports,
    getAeroexpressListUrl,
    getUrlForAirport,
    shouldShowAeroexpress,
    trueAeroexpress,
} from '../aeroexpress';

describe('getUrlForAirport', () => {
    const allDays = 'на все дни';
    const encodedAllDays = '%D0%BD%D0%B0+%D0%B2%D1%81%D0%B5+%D0%B4%D0%BD%D0%B8';
    const airport = {
        aeroexpress: {
            settlement_id: 22,
        },
        id: 123,
    } as IStation;

    it('Вернет url с get-параметрами для аэроэкспресса', () => {
        const url = `/search/suburban/?aeroex=y&fromId=c22&toId=s123&when=${encodedAllDays}`;

        expect(getUrlForAirport(airport, true, allDays, Tld.ru, Lang.ru)).toBe(
            url,
        );
    });

    it('Вернет url с get-параметрами для электрички', () => {
        const url = `/search/suburban/?fromId=c22&toId=s123&when=${encodedAllDays}`;

        expect(getUrlForAirport(airport, false, allDays, Tld.ru, Lang.ru)).toBe(
            url,
        );
    });
});

describe('trueAeroexpress', () => {
    it('Вернет true для id Внуково', () => {
        const airport = {id: 9600215} as IStation;

        expect(trueAeroexpress([airport])).toBe(true);
    });

    it('Вернет false для id не входящего в список TRUE_AEROEXPRESS_AIRPORT_IDS', () => {
        const airport = {id: 960} as IStation;

        expect(trueAeroexpress([airport])).toBe(false);
    });
});

describe('filterAirports', () => {
    it('Вернет массив аэропортов у которых есть аэроэкспресс', () => {
        const airports = [
            {aeroexpress: {id: 1}},
            {aeroexpress: {id: 2}},
            {aeroexpress: null},
            {aeroexpress: {}},
            {},
            {aeroexpress: undefined},
        ] as IStation[];
        const airportsWithExpress = [
            {aeroexpress: {id: 1}},
            {aeroexpress: {id: 2}},
            {aeroexpress: {}},
        ];

        expect(filterAirports(airports)).toEqual(airportsWithExpress);
    });
});

describe('shouldShowAeroexpress', () => {
    it('Вернет false если тип - не самолеты', () => {
        expect(
            shouldShowAeroexpress(
                [
                    {aeroexpress: '2'},
                    {aeroexpress: '1'},
                ] as unknown as IStation[],
                TransportType.bus,
            ),
        ).toBe(false);
    });

    it('Вернет false если у переданных сегментов нет аэроэкспрессов', () => {
        expect(
            shouldShowAeroexpress(
                [{name: '1'}, {name: '2'}] as unknown as IStation[],
                TransportType.plane,
            ),
        ).toBe(false);
    });

    it('Вернет true если у переданных сегментов есть хотя бы один аэроэкспресс', () => {
        expect(
            shouldShowAeroexpress(
                [{name: '1'}, {aeroexpress: '2'}] as unknown as IStation[],
                TransportType.plane,
            ),
        ).toBe(true);
    });
});

describe('getAeroexpressListUrl', () => {
    it('Вернет url для списка аэроэкспрессов города', () => {
        const cityId = 1;
        const result = `/stations/plane/?aeroex=1&city=${cityId}`;

        expect(getAeroexpressListUrl(cityId, Tld.ru, Lang.ru)).toBe(result);
    });
});
