import {CHAR_NBSP, CHAR_EM_DASH} from '../../../stringUtils';

import getAggregatedInfo from '../getAggregatedInfo';
import stationsMock from './stations.mock';

const separator = `${CHAR_NBSP}${CHAR_EM_DASH} `;

const context = {
    transportType: 'all',
    language: 'ru',
};

describe('getAggregatedInfo', () => {
    it('Pass an empty segments transfer', () => {
        const transfer = {
            segments: [],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: '',
            transferTitle: null,
        });
    });

    it('Pass normal transfer Kamensk-Uralskiy - Chaikovskiy', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Каменск-Уральский'),
                    stationTo: stationsMock('Екатеринбург-Пасс.'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Екатеринбург-Пасс.'),
                    stationTo: stationsMock('Ижевск'),
                    transport: {code: 'train'},
                },
                {
                    stationFrom: stationsMock('Ижевск'),
                    stationTo: stationsMock('Чайковский'),
                    transport: {code: 'bus'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Каменск-Уральский${separator}Екатеринбург-Пасс.${separator}Ижевск${separator}Чайковский`,
            transferTitle: 'через Екатеринбург и Ижевск',
        });
    });

    it('Pass different stations in one city transfer Abramcevo - Elektrostal', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Абрамцево'),
                    stationTo: stationsMock('м. Щелковская'),
                    transport: {code: 'bus'},
                },
                {
                    stationFrom: stationsMock('м. Измайловская'),
                    stationTo: stationsMock('Проспект Ленина'),
                    transport: {code: 'bus'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Абрамцево${separator}м. Щёлковская (Москва)${separator}м. Измайловская (Москва)${separator}Проспект Ленина (Электросталь)`,
            transferTitle: 'с пересадкой в Москве',
        });
    });

    it('Pass normal suburban transfer Barviha - Akulovo', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Барвиха'),
                    stationTo: stationsMock('Рабочий поселок'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Рабочий поселок'),
                    stationTo: stationsMock('Кубинка-1'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Кубинка-1'),
                    stationTo: stationsMock('Акулово'),
                    transport: {code: 'suburban'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Барвиха${separator}Рабочий поселок (Москва)${separator}Кубинка-1${separator}Акулово`,
            transferTitle: 'через Рабочий поселок и Кубинку-1',
        });
    });

    it('Pass different stations in one city suburban transfer Barviha - Akulovo', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Барвиха'),
                    stationTo: stationsMock('Рабочий поселок'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Киевский вокзал'),
                    stationTo: stationsMock('Бекасово-1'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Бекасово-1'),
                    stationTo: stationsMock('Акулово'),
                    transport: {code: 'suburban'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Барвиха${separator}Рабочий поселок (Москва)${separator}Киевский вокзал (Москва)${separator}Бекасово-1${separator}Акулово`,
            transferTitle: 'через Москву и Бекасово-1',
        });
    });

    it('Каменск-Уральский - Храмцовская - Марамзино (без падежей) - Екатеринбург-Пасс.', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Каменск-Уральский'),
                    stationTo: stationsMock('ст. Храмцовская'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('ст. Храмцовская'),
                    stationTo: stationsMock('Марамзино'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Марамзино'),
                    stationTo: stationsMock('Екатеринбург-Пасс.'),
                    transport: {code: 'suburban'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Каменск-Уральский${separator}ст. Храмцовская${separator}Марамзино${separator}Екатеринбург-Пасс.`,
            transferTitle: 'пересадки: ст. Храмцовская, Марамзино',
        });
    });

    it('Каменск-Уральский - Марамзино (без падежей) - Екатеринбург-Пасс.', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Каменск-Уральский'),
                    stationTo: stationsMock('Марамзино'),
                    transport: {code: 'suburban'},
                },
                {
                    stationFrom: stationsMock('Марамзино'),
                    stationTo: stationsMock('Екатеринбург-Пасс.'),
                    transport: {code: 'suburban'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Каменск-Уральский${separator}Марамзино${separator}Екатеринбург-Пасс.`,
            transferTitle: 'пересадка: Марамзино',
        });
    });

    it('Каменск-Уральский - ст. Храмцовская (город без падежей) - Екатеринбург-Пасс.', () => {
        const transfer = {
            segments: [
                {
                    stationFrom: stationsMock('Каменск-Уральский'),
                    stationTo: stationsMock('ст. Храмцовская'),
                    transport: {code: 'bus'},
                },
                {
                    stationFrom: stationsMock('ст. Храмцовская'),
                    stationTo: stationsMock('Екатеринбург-Пасс.'),
                    transport: {code: 'suburban'},
                },
            ],
        };

        expect(getAggregatedInfo({transfer, ...context})).toEqual({
            transferStations: `Каменск-Уральский${separator}ст. Храмцовская${separator}Екатеринбург-Пасс.`,
            transferTitle: 'пересадка: Храмцовская',
        });
    });
});
