import {TRAIN_TYPE, PLANE_TYPE, BUS_TYPE} from '../transportType';
import {stationTypeToMetrikaTarget} from '../prepareStationsForIndexPage';

export const stationsForPreparing = [
    {
        t_type: TRAIN_TYPE,
        title: 'Екатеринбург Пасс',
        station_type: {
            title: 'Вокзал',
            code: 'train_station',
        },
    },
    {
        t_type: BUS_TYPE,
        title: 'м. Уральская',
        station_type: {
            title: 'Автовокзал',
            code: 'bus_station',
        },
        is_metro: true,
    },
    {
        t_type: PLANE_TYPE,
        title: 'Екатеринбург',
        station_type: {
            title: 'Аэропорт',
            code: 'airport',
        },
    },
];

export const preparedStations = [
    {
        t_type: PLANE_TYPE,
        stations: [
            {
                t_type: PLANE_TYPE,
                title: 'Аэропорт',
                originalTitle: 'Екатеринбург',
                station_type: {
                    title: 'Аэропорт',
                    code: 'airport',
                },
                metrikaTarget: stationTypeToMetrikaTarget[PLANE_TYPE],
                aeroexpressMetrikaTarget:
                    stationTypeToMetrikaTarget.aeroexpress,
            },
        ],
    },
    {
        t_type: TRAIN_TYPE,
        stations: [
            {
                t_type: TRAIN_TYPE,
                title: 'Екатеринбург Пасс',
                originalTitle: 'Екатеринбург Пасс',
                station_type: {
                    title: 'Вокзал',
                    code: 'train_station',
                },
                metrikaTarget: stationTypeToMetrikaTarget[TRAIN_TYPE],
            },
        ],
    },
    {
        t_type: BUS_TYPE,
        stations: [
            {
                t_type: BUS_TYPE,
                title: 'Уральская',
                originalTitle: 'м. Уральская',
                station_type: {
                    title: 'Автовокзал',
                    code: 'bus_station',
                },
                is_metro: true,
                metrikaTarget: stationTypeToMetrikaTarget[BUS_TYPE],
            },
        ],
    },
];
