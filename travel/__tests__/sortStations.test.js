jest.disableAutomock();

import {
    sort,
    compareStations,
    isMainBusStation,
    compareBusStations,
} from '../sortStations';

const planeStations = [
    {t_type: 'plane', title: 'Шереметьево'},
    {t_type: 'plane', title: 'Жуковский'},
    {t_type: 'plane', title: 'Домодедово'},
    {t_type: 'plane', title: 'Внуково'},
];

const sortedPlaneStations = [
    {t_type: 'plane', title: 'Внуково'},
    {t_type: 'plane', title: 'Домодедово'},
    {t_type: 'plane', title: 'Жуковский'},
    {t_type: 'plane', title: 'Шереметьево'},
];

const busStations = [
    {t_type: 'bus', title: 'Площадь ж.д. вокзала', station_type: {id: 1}},
    {t_type: 'bus', title: 'Автовокзал Северный', station_type: {id: 2}},
    {
        t_type: 'bus',
        title: 'Автокасса Аэропорта Кольцово',
        station_type: {id: 10},
    },
];

const sortedBusStations = [
    {
        t_type: 'bus',
        title: 'Автокасса Аэропорта Кольцово',
        station_type: {id: 10},
    },
    {t_type: 'bus', title: 'Площадь ж.д. вокзала', station_type: {id: 1}},
    {t_type: 'bus', title: 'Автовокзал Северный', station_type: {id: 2}},
];

describe('sort', () => {
    it('Вернет правильный порядок станций', () => {
        expect(sort(planeStations)).toEqual(sortedPlaneStations);
    });

    it('Вернет правильный порядок автобусных станций', () => {
        expect(sort(busStations)).toEqual(sortedBusStations);
    });
});

describe('compareStations', () => {
    const stationA = planeStations[0];
    const stationB = planeStations[1];

    it('Вернет 1, если название Станции А должно идти после Станции B', () => {
        expect(compareStations(stationA, stationB)).toEqual(1);
    });

    it('Вернет -1, если название Станции А должно предшествовать Станции B', () => {
        expect(compareStations(stationB, stationA)).toEqual(-1);
    });
});

describe('isMainBusStation', () => {
    it('Вернет true, если id станции равен 1 или 10', () => {
        const stationA = {station_type: {id: 1}};
        const stationB = {station_type: {id: 10}};

        expect(isMainBusStation(stationA)).toBe(true);
        expect(isMainBusStation(stationB)).toBe(true);
    });

    it('Вернет false, если id станции не равен ни 1 ни 10', () => {
        const station = {station_type: {id: 0}};

        expect(isMainBusStation(station)).toBe(false);
    });
});

describe('compareBusStations', () => {
    it('Вернет -1, если Станция А - isMainBusStation, а Станция Б - нет', () => {
        const stationC = {station_type: {id: 1}};
        const stationD = {station_type: {id: 0}};

        expect(compareBusStations(stationC, stationD)).toBe(-1);
    });

    it('Вернет 1, если Станция А - не isMainBusStation, а Станция Б - isMainBusStation', () => {
        const stationC = {station_type: {id: 2}};
        const stationD = {station_type: {id: 1}};

        expect(compareBusStations(stationC, stationD)).toBe(1);
    });

    let stationA = {station_type: {id: 1}, title: 'Площадь ж.д. вокзала'};
    let stationB = {station_type: {id: 10}, title: 'Автовокзал Северный'};

    it(`Вернет 1, если обе станции - isMainBusStation и станция А
        должна идти после станции B`, () => {
        expect(compareBusStations(stationA, stationB)).toBe(1);
    });

    it(`Вернет -1, если обе станции - isMainBusStation и станция А
        должна предшествовать станции B`, () => {
        expect(compareBusStations(stationB, stationA)).toBe(-1);
    });

    stationA = {station_type: {id: 0}, title: 'Площадь ж.д. вокзала'};
    stationB = {station_type: {id: 2}, title: 'Автовокзал Северный'};

    it(`Вернет 1, если обе станции - не isMainBusStation и станция А
        должна идти после станции B`, () => {
        expect(compareBusStations(stationA, stationB)).toBe(1);
    });

    it(`Вернет -1, если обе станции - не isMainBusStation и станция А
        должна предшествовать станции B`, () => {
        expect(compareBusStations(stationB, stationA)).toBe(-1);
    });
});
