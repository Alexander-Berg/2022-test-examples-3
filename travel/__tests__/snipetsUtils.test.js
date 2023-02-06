jest.disableAutomock();

import {
    BUS_TYPE,
    TRAIN_TYPE,
    PLANE_TYPE,
    WATER_TYPE,
    SUBURBAN_TYPE,
} from '../../transportType';
import {YBUS} from '../tariffSources';

import {TrainPartner} from '../../order/trainPartners';
import {OrderUrlOwner} from '../tariffClasses';

import {getBuyLinkText} from '../snippetUtils';

const busSegment = {
    transport: {
        code: BUS_TYPE,
    },
};
const trainTariffsIM = {
    tariffs: {
        classes: {
            compartment: {
                trainOrderUrl: 'someUrl',
                trainOrderUrlOwner: OrderUrlOwner.trains,
            },
        },
    },
};
const trainTariffsUFS = {
    tariffs: {
        classes: {
            compartment: {
                trainOrderUrl: 'someUrl',
                trainOrderUrlOwner: OrderUrlOwner.ufs,
            },
        },
    },
};
const trainSegmentForDate = {
    transport: {
        code: TRAIN_TYPE,
    },
};
const trainSegmentForAllDays = {
    transport: {
        code: TRAIN_TYPE,
    },
    runDays: {},
    trainPartners: [TrainPartner.ufs],
    thread: {
        firstCountryCode: 'RU',
        lastCountryCode: 'RU',
    },
};
const suburbanSegment = {
    transport: {
        code: SUBURBAN_TYPE,
    },
};

describe('getBuyLinkText', () => {
    it('Вернёт текст ссылки для самолёта', () => {
        expect(
            getBuyLinkText({
                transport: {
                    code: PLANE_TYPE,
                },
            }),
        ).toEqual('Купить билет');
    });

    it('Вернёт текст ссылки для автобуса от Я.Автобусов', () => {
        expect(
            getBuyLinkText({
                ...busSegment,
                source: YBUS,
            }),
        ).toEqual('Выбрать место');
    });

    it('Вернёт текст ссылки для автобуса от партнёра', () => {
        expect(getBuyLinkText(busSegment)).toEqual('Купить');
    });

    it('Вернёт текст ссылки для поезда (продажа на конкретную дату через сервис поездов', () => {
        expect(
            getBuyLinkText({
                ...trainSegmentForDate,
                ...trainTariffsIM,
            }),
        ).toEqual('Выбрать место');
    });

    it('Вернёт текст ссылки для поезда (продажа на конкретную дату на стороне УФС)', () => {
        expect(
            getBuyLinkText({
                ...trainSegmentForDate,
                ...trainTariffsUFS,
            }),
        ).toEqual('Купить на УФС');
    });

    it('Вернёт текст ссылки для поезда (поиск на все дни. Ведем на УФС)', () => {
        expect(getBuyLinkText(trainSegmentForAllDays)).toEqual('Купить на УФС');
    });

    it('Вернёт текст ссылки для поезда (поиск на все дни. Продажа через сервис поездов)', () => {
        expect(
            getBuyLinkText({
                ...trainSegmentForAllDays,
                trainPartners: [TrainPartner.ufs, TrainPartner.im],
            }),
        ).toEqual('Выбрать место');
    });

    it('Вернёт текст ссылки для электричек с возможностью продажи', () => {
        expect(
            getBuyLinkText({
                ...suburbanSegment,
                hasTrainTariffs: true,
            }),
        ).toEqual('Выбрать место');
    });

    it('Для всех других типов транспорта вернёт дефолтный текст ссылки', () => {
        expect(
            getBuyLinkText({
                transport: {
                    code: WATER_TYPE,
                },
            }),
        ).toEqual('Купить');
    });
});
