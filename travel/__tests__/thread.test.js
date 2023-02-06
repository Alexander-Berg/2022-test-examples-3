import {getStationFrom, getStationTo, getThreadBreadCrumbs} from '../thread';
import getMainPage from '../../url/crumble/getMainPage';

import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import {FilterTransportType} from '../../../../common/lib/transportType';

const tld = Tld.ru;
const language = Lang.ru;
const flags = {};

describe('getStationFrom', () => {
    it(
        'Вернет undefined в случае, если в в качестве станций передан не массив или ' +
            'станций меньше двух',
        () => {
            expect(getStationFrom({stations: {}})).toBeUndefined();
            expect(getStationFrom({stations: 1})).toBeUndefined();
            expect(getStationFrom({stations: []})).toBeUndefined();
            expect(getStationFrom({stations: [{}]})).toBeUndefined();
        },
    );

    it(
        'Вернет undefined в случае, если станция отправления является последней ' +
            'станцией в нитке',
        () => {
            const stations = [{}, {isStationFrom: true}];

            expect(getStationFrom({stations})).toBeUndefined();
        },
    );

    it('Флаг isStationFrom не определен ни у одной станции. Вернет первую станцию', () => {
        const stations = [{}, {}, {}];

        expect(getStationFrom({stations})).toBe(stations[0]);
    });

    it(
        'Вернет первую станцию у которой указан isStationFrom, даже если флаг ' +
            'указан у многих станций',
        () => {
            const stations = [
                {},
                {isStationFrom: true},
                {isStationFrom: true},
                {isStationFrom: true},
            ];

            expect(getStationFrom({stations})).toBe(stations[1]);
        },
    );

    it('Вернет станцию у которой указан isStationFrom', () => {
        const stations = [{}, {}, {isStationFrom: true}, {}, {}];

        expect(getStationFrom({stations})).toBe(stations[2]);
    });
});

describe('getStationTo', () => {
    it(
        'Вернет undefined в случае, если в в качестве станций передан не массив или ' +
            'станций меньше двух',
        () => {
            expect(getStationTo({stations: {}})).toBeUndefined();
            expect(getStationTo({stations: 1})).toBeUndefined();
            expect(getStationTo({stations: []})).toBeUndefined();
            expect(getStationTo({stations: [{}]})).toBeUndefined();
            expect(
                getStationTo({stations: {}, stationFrom: {}}),
            ).toBeUndefined();
            expect(
                getStationTo({stations: 1, stationFrom: {}}),
            ).toBeUndefined();
            expect(
                getStationTo({stations: [], stationFrom: {}}),
            ).toBeUndefined();
            expect(
                getStationTo({stations: [{}], stationFrom: {}}),
            ).toBeUndefined();
        },
    );

    it('Вернет undefined в случае невозможности определить станцию отправления', () => {
        expect(getStationTo({stations: [{}]})).toBeUndefined();
    });

    it(
        'Флаг isStationTo не определен ни у одной станции - вернет последнюю станцию ' +
            'в нитке',
        () => {
            const stations = [{}, {}, {}];

            expect(getStationTo({stations})).toBe(
                stations[stations.length - 1],
            );
        },
    );

    it(
        'Станция прибытия не может быть первой станцией в нитке. Должна вернуться ' +
            'последняя станция',
        () => {
            const stations = [{isStationTo: true}, {}, {}];

            expect(getStationTo({stations})).toBe(
                stations[stations.length - 1],
            );
        },
    );

    it('Вернет станцию у которой указан флаг isStationTo', () => {
        const stations = [{}, {}, {isStationTo: true}, {}];

        expect(getStationTo({stations})).toBe(stations[2]);
    });

    it(
        'Вернет первую станцию у которой указан isStationTo после станции отправления, ' +
            'даже если флаг указан у многих станций',
        () => {
            const stationFrom = {isStationTo: true};
            const stations = [
                {},
                {isStationTo: true},
                stationFrom,
                {isStationTo: true},
                {isStationTo: true},
                {},
            ];

            expect(getStationTo({stations, stationFrom})).toBe(stations[3]);
        },
    );

    it('Если после станции отправления больше нет станций, то вернет undefined', () => {
        const stationFrom = {};
        const stations = [{}, {}, stationFrom];

        expect(getStationTo({stations, stationFrom})).toBeUndefined();
    });
});

describe('getBreadCrumbs', () => {
    it('Вернет массив с хлебными крошками', () => {
        const title = 'Test title';
        const transportType = FilterTransportType.suburban;
        const stationFrom = {
            slug: 'moscow',
            title: 'Москва',
            isStationFrom: true,
        };
        const stations = [{}, stationFrom, {}, {slug: 'suhum', title: 'Сухум'}];

        let breadCrumbs = getThreadBreadCrumbs({
            title,
            stations,
            stationFrom,
            transportType,
            tld,
            language,
            flags,
        });

        expect(breadCrumbs.length).toBe(4);
        expect(breadCrumbs[0].url).toBe(getMainPage('/').url);
        expect(breadCrumbs[0].name).toBe('Главная');
        expect(breadCrumbs[1].url).toBe('/suburban');
        expect(breadCrumbs[1].name).toBe('Расписание электричек');
        expect(breadCrumbs[2].url).toBe('/suburban/moscow--suhum/today');
        expect(breadCrumbs[2].name).toBe('Электрички Москва – Сухум');
        expect(breadCrumbs[3].name).toBe(title);

        // тоже самое, но без явной передачи станции отправления
        breadCrumbs = getThreadBreadCrumbs({
            title,
            stations,
            transportType,
            tld,
            language,
            flags,
        });

        // expect(breadCrumbs.length).toBe(4);
        expect(breadCrumbs[0].url).toBe(getMainPage('/').url);
        expect(breadCrumbs[0].name).toBe('Главная');
        expect(breadCrumbs[1].url).toBe('/suburban');
        expect(breadCrumbs[1].name).toBe('Расписание электричек');
        expect(breadCrumbs[2].url).toBe('/suburban/moscow--suhum/today');
        expect(breadCrumbs[2].name).toBe('Электрички Москва – Сухум');
        expect(breadCrumbs[3].name).toBe(title);
    });

    it('В случае невозможности определить станцию отправления или станцию прибытия кинет exception', () => {
        expect(() =>
            getThreadBreadCrumbs({
                title: 'title',
                stations: [{}],
                tld,
                language,
                flags,
            }),
        ).toThrow();
    });
});
