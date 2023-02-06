jest.disableAutomock();

import {
    setOriginalTitle,
    getSingleStationTitle,
    setMetriks,
    stationTypeToMetrikaTarget,
    setFinalTitle,
    prepareStationsForIndexPage,
} from '../prepareStationsForIndexPage';
import {
    stationsForPreparing,
    preparedStations,
} from './prepareStationsForIndexPage.const';
import {TRAIN_TYPE, PLANE_TYPE} from '../transportType';

const city = {
    title: 'Екатеринбург',
};

describe('setOriginalTitle', () => {
    it('Должна вернуть массив станций с установленным полем originalTitle', () => {
        const station = {title: 'Station 1'};
        const resultedStation = {
            title: 'Station 1',
            originalTitle: 'Station 1',
        };

        expect(setOriginalTitle(station)).toEqual(resultedStation);
    });
});

describe('getSingleStationTitle', () => {
    const cityTitleLowercase = city.title.toLowerCase();

    it('Должна вернуть заголовок типа станции, если заголовок станции равен названию города', () => {
        const station = {
            title: 'Екатеринбург',
            station_type: {
                title: 'Аэропорт',
                code: 'airport',
            },
        };

        expect(getSingleStationTitle(station, cityTitleLowercase)).toBe(
            station.station_type.title,
        );
    });

    it(`Должна вернуть заголовок типа станции, если заголовок станции равен
    названию типа станции соединенным с городом`, () => {
        const station = {
            title: 'Аэропорт Екатеринбург',
            station_type: {
                title: 'Аэропорт',
                code: 'airport',
            },
        };

        expect(getSingleStationTitle(station, cityTitleLowercase)).toBe(
            station.station_type.title,
        );
    });

    it('Должна вернуть заголовок станции, если название станции отличается от названия города', () => {
        const station = {
            title: 'Екатеринбург Пасс',
            station_type: {
                title: 'Аэропорт',
                code: 'airport',
            },
        };

        expect(getSingleStationTitle(station, cityTitleLowercase)).toBe(
            station.title,
        );
    });

    it('Должна вернуть заголовок станции, если code станции не входит в stationTypeCodes', () => {
        const station = {
            title: 'Екатеринбург Пасс',
            station_type: {
                title: 'Аэропорт',
                code: 'another',
            },
        };

        expect(getSingleStationTitle(station, cityTitleLowercase)).toBe(
            station.title,
        );
    });
});

describe('setMetriks', () => {
    it('Должна вернуть массив станций с добавленными значениями метрик', () => {
        const station = {t_type: TRAIN_TYPE};

        const result = {
            t_type: TRAIN_TYPE,
            metrikaTarget: stationTypeToMetrikaTarget[TRAIN_TYPE],
        };

        expect(setMetriks(station)).toEqual(result);
    });

    it('Должна дополнительно установить метрику аэроэкспресса для аэропортов', () => {
        const station = {t_type: PLANE_TYPE};
        const result = {
            t_type: PLANE_TYPE,
            metrikaTarget: stationTypeToMetrikaTarget[PLANE_TYPE],
            aeroexpressMetrikaTarget: stationTypeToMetrikaTarget.aeroexpress,
        };

        expect(setMetriks(station)).toEqual(result);
    });
});

describe('setFinalTitle', () => {
    it("Должна удалить 'м. ' у станции метро", () => {
        const station = {
            is_metro: true,
            title: 'м. Геологическая',
        };
        const resultedStation = {
            is_metro: true,
            title: 'Геологическая',
        };

        expect(setFinalTitle(station)).toEqual(resultedStation);
    });

    it("Не удаляет 'м. ' у станций которые не являются станциями метро", () => {
        const station = {
            is_metro: false,
            title: 'м. Геологическая',
        };
        const resultedStation = {
            is_metro: false,
            title: 'м. Геологическая',
        };

        expect(setFinalTitle(station)).toEqual(resultedStation);
    });
});

describe('prepareStationsForIndexPage', () => {
    // TODO: Дописать кейсы
    it('Должна вернуть приоритезированный массив груп станций', () => {
        expect(prepareStationsForIndexPage(stationsForPreparing, city)).toEqual(
            preparedStations,
        );
    });
});
