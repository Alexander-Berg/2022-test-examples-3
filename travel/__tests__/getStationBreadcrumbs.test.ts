import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import StationType from '../../../interfaces/state/station/StationType';
import StationSubtype from '../../../interfaces/state/station/StationSubtype';
import StationEventList from '../../../interfaces/state/station/StationEventList';
import CityStations from '../../../interfaces/state/station/CityStations';
import IStateFlags from '../../../interfaces/state/flags/IStateFlags';

import getStationBreadcrumbs from '../getStationBreadcrumbs';

const tld = Tld.ru;
const language = Lang.ru;
const flags = {} as IStateFlags;
const id = 1;
const title = 'Вокзал им. Энгельса';
const planeTitle = 'Внуково';
const type = StationType.railroad;
const mainSubtype = StationSubtype.train;
const settlement = {
    id: 1,
    title: 'Москва',
    slug: 'moscow',
};
const cityStationsSingle = {
    [StationType.railroad]: [{}],
} as CityStations;
const cityStations = {
    [StationType.railroad]: [{}, {}],
} as CityStations;

const railroadCityStations = {
    [StationType.railroad]: [
        {mainSubtype: StationSubtype.train},
        {mainSubtype: StationSubtype.suburban},
        {mainSubtype: StationSubtype.suburban},
    ],
} as CityStations;

const planeCityStations = {
    [StationType.plane]: [
        {id: 9600216, title: 'Домодедово', subtypes: []},
        {id: 9850865, title: 'Жуковский', subtypes: []},
        {id: 9600213, title: 'Шереметьево', subtypes: []},
        {id: 9600215, title: 'Внуково', subtypes: []},
    ],
};

const planeCityStationsSingleStation = {
    [StationType.plane]: [{id: 9600216, title: 'Домодедово', subtypes: []}],
};

const trainCityStationsWithSubtypes = {
    [StationType.railroad]: [
        {
            id: 2000006,
            title: 'Белорусский вокзал',
            mainSubtype: StationSubtype.train,
            subtypes: [
                StationSubtype.train,
                StationSubtype.suburban,
                StationSubtype.tablo,
            ],
        },
        {
            id: 2000003,
            title: 'Казанский вокзал',
            mainSubtype: StationSubtype.train,
            subtypes: [
                StationSubtype.train,
                StationSubtype.suburban,
                StationSubtype.tablo,
            ],
        },
        {
            id: 2000007,
            title: 'Киевский вокзал',
            mainSubtype: StationSubtype.train,
            subtypes: [
                StationSubtype.train,
                StationSubtype.suburban,
                StationSubtype.tablo,
            ],
        },
        {
            id: 2000001,
            title: 'Курский вокзал',
            mainSubtype: StationSubtype.train,
            subtypes: [
                StationSubtype.train,
                StationSubtype.suburban,
                StationSubtype.tablo,
            ],
        },
    ],
};

const terminal = {
    id: 1,
    name: 'B',
};

describe('getStationBreadcrumbs', () => {
    it('Без данных города', () => {
        const breadCrumbs = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.train,
            mainSubtype,
        );

        expect(breadCrumbs.length).toBe(2);
        expect(breadCrumbs[0].name).toBe('Расписание поездов');
        expect(breadCrumbs[0].url).toBe('/train');
        expect(breadCrumbs[1].name).toBe(title);
    });

    it('С данными города', () => {
        const breadCrumbsTrain = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.train,
            mainSubtype,
            settlement,
            undefined,
            cityStations,
        );

        expect(breadCrumbsTrain.length).toBe(3);
        expect(breadCrumbsTrain[0].name).toBe('Расписание поездов');
        expect(breadCrumbsTrain[0].url).toBe('/train');
        expect(breadCrumbsTrain[1].name).toBe('Москва');
        expect(breadCrumbsTrain[1].url).toBe('/train/moscow');
        expect(breadCrumbsTrain[2].name).toBe(title);

        const breadCrumbsSuburban = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.suburban,
            mainSubtype,
            settlement,
            undefined,
            cityStations,
        );

        expect(breadCrumbsSuburban.length).toBe(3);
        expect(breadCrumbsSuburban[0].name).toBe('Расписание электричек');
        expect(breadCrumbsSuburban[0].url).toBe('/suburban');
        expect(breadCrumbsSuburban[1].name).toBe('Москва');
        expect(breadCrumbsSuburban[1].url).toBe('/suburban/moscow');
        expect(breadCrumbsSuburban[2].name).toBe(title);

        const breadCrumbsSuburbanSingle = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.suburban,
            mainSubtype,
            settlement,
            undefined,
            cityStationsSingle,
        );

        expect(breadCrumbsSuburbanSingle.length).toBe(2);
        expect(breadCrumbsSuburbanSingle[0].name).toBe('Расписание электричек');
        expect(breadCrumbsSuburbanSingle[0].url).toBe('/suburban');
        expect(breadCrumbsSuburbanSingle[1].name).toBe(title);
    });

    it('Для вида транспорта без страниц транспорта', () => {
        const breadCrumbs = getStationBreadcrumbs(
            id,
            title,
            StationType.water,
            tld,
            language,
            flags,
            StationSubtype.schedule,
            mainSubtype,
            settlement,
        );

        expect(breadCrumbs.length).toBe(2);
        expect(breadCrumbs[0].name).toBe('Расписание транспорта Москва');
        expect(breadCrumbs[0].url).toBe('/city/1');
        expect(breadCrumbs[1].name).toBe(title);
    });

    it('Случай, когда нет страницы города для данной станции и страницы для данного типа транспорта', () => {
        const breadCrumbs = getStationBreadcrumbs(
            id,
            title,
            StationType.water,
            tld,
            language,
            flags,
            StationSubtype.schedule,
            mainSubtype,
        );

        expect(breadCrumbs.length).toBe(2);
        expect(breadCrumbs[0].name).toBe('Главная');
        expect(breadCrumbs[0].url).toBe('/');
        expect(breadCrumbs[1].name).toBe(title);
    });

    it('Для страницы прибытия', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.train,
            mainSubtype,
            settlement,
            StationEventList.arrival,
            cityStations,
        );

        expect(breadcrumbs.length).toBe(4);
        expect(breadcrumbs[0].name).toBe('Расписание поездов');
        expect(breadcrumbs[0].url).toBe('/train');
        expect(breadcrumbs[1].name).toBe('Москва');
        expect(breadcrumbs[1].url).toBe('/train/moscow');
        expect(breadcrumbs[2].name).toBe(title);
        expect(breadcrumbs[2].url).toBe('/station/1/');
        expect(breadcrumbs[3].name).toBe('Прибытие');
    });

    it('Для страницы аэпорорта, отправление, если в городе больше одного аэропорта', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.departure,
            planeCityStations,
        );

        expect(breadcrumbs.length).toBe(3);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe('Москва');
        expect(breadcrumbs[1].url).toBe('/plane/moscow');
        expect(breadcrumbs[2].name).toBe(planeTitle);
    });

    it('Для страницы аэпорорта, отправление, если нет страницы города', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.departure,
            planeCityStationsSingleStation,
        );

        expect(breadcrumbs.length).toBe(2);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe(planeTitle);
    });

    it('Для страницы аэпорорта, прибытие, если в городе больше одного аэропорта', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.arrival,
            planeCityStations,
        );

        expect(breadcrumbs.length).toBe(4);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe('Москва');
        expect(breadcrumbs[1].url).toBe('/plane/moscow');
        expect(breadcrumbs[2].name).toBe(planeTitle);
        expect(breadcrumbs[2].url).toBe('/station/1/');
        expect(breadcrumbs[3].name).toBe('Прибытие');
    });

    it('Для страницы аэпорорта, прибытие, если нет страницы города', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.arrival,
            planeCityStationsSingleStation,
        );

        expect(breadcrumbs.length).toBe(3);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe(planeTitle);
        expect(breadcrumbs[1].url).toBe('/station/1/');
        expect(breadcrumbs[2].name).toBe('Прибытие');
    });

    it('Для страницы аэпорорта + терминал, отправление, если в городе больше одного аэропорта', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.departure,
            planeCityStations,
            terminal,
        );

        expect(breadcrumbs.length).toBe(4);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe('Москва');
        expect(breadcrumbs[1].url).toBe('/plane/moscow');
        expect(breadcrumbs[2].name).toBe(planeTitle);
        expect(breadcrumbs[2].url).toBe('/station/1/');
        expect(breadcrumbs[3].name).toBe('Терминал B');
    });

    it('Для страницы аэпорорта + терминал, отправление, если нет страницы города', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.departure,
            planeCityStationsSingleStation,
            terminal,
        );

        expect(breadcrumbs.length).toBe(3);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe(planeTitle);
        expect(breadcrumbs[1].url).toBe('/station/1/');
        expect(breadcrumbs[2].name).toBe('Терминал B');
    });

    it('Для страницы аэпорорта + терминал, прибытие, если в городе больше одного аэропорта', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.arrival,
            planeCityStations,
            terminal,
        );

        expect(breadcrumbs.length).toBe(4);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe('Москва');
        expect(breadcrumbs[1].url).toBe('/plane/moscow');
        expect(breadcrumbs[2].name).toBe(planeTitle);
        expect(breadcrumbs[2].url).toBe('/station/1/?event=arrival');
        expect(breadcrumbs[3].name).toBe('Терминал B');
    });

    it('Для страницы аэпорорта + терминал, прибытие, если нет страницы города', () => {
        const breadcrumbs = getStationBreadcrumbs(
            id,
            planeTitle,
            StationType.plane,
            tld,
            language,
            flags,
            StationSubtype.plane,
            StationSubtype.plane,
            settlement,
            StationEventList.arrival,
            planeCityStationsSingleStation,
            terminal,
        );

        expect(breadcrumbs.length).toBe(3);
        expect(breadcrumbs[0].name).toBe('Расписание самолётов');
        expect(breadcrumbs[0].url).toBe('/plane');
        expect(breadcrumbs[1].name).toBe(planeTitle);
        expect(breadcrumbs[1].url).toBe('/station/1/?event=arrival');
        expect(breadcrumbs[2].name).toBe('Терминал B');
    });

    it('Для страницы жд станции/табло, если есть только одна жд станция, а остальные электричечные', () => {
        const breadCrumbsTrain = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.train,
            mainSubtype,
            settlement,
            undefined,
            railroadCityStations,
        );

        expect(breadCrumbsTrain.length).toBe(2);
        expect(breadCrumbsTrain[0].name).toBe('Расписание поездов');
        expect(breadCrumbsTrain[0].url).toBe('/train');
        expect(breadCrumbsTrain[1].name).toBe(title);
    });

    it('Для страницы жд станции, если mainSubtype не совпадает, но есть совпадение в списке subtypes', () => {
        const breadCrumbsTrain = getStationBreadcrumbs(
            id,
            title,
            type,
            tld,
            language,
            flags,
            StationSubtype.train,
            mainSubtype,
            settlement,
            undefined,
            trainCityStationsWithSubtypes,
        );

        expect(breadCrumbsTrain.length).toBe(3);
        expect(breadCrumbsTrain[0].name).toBe('Расписание поездов');
        expect(breadCrumbsTrain[0].url).toBe('/train');
        expect(breadCrumbsTrain[1].name).toBe('Москва');
        expect(breadCrumbsTrain[1].url).toBe('/train/moscow');
        expect(breadCrumbsTrain[2].name).toBe(title);
    });
});
