import {TransportType, FilterTransportType} from '../../transportType';
import IState from '../../../interfaces/state/IState';
import SearchSegment from '../../../interfaces/state/search/SearchSegment';

import analyseSegments from '../analyseSegments';

const getState = (transport): IState =>
    ({
        search: {
            context: {
                transportType: transport,
            },
        },
    } as IState);

const state = getState(FilterTransportType.all);

const defaultAnswer = {
    segments: [
        {
            transport: {code: TransportType.train},
        },
    ],
    transportTypes: [TransportType.train],
    isSuburbanSearchResult: false,
    isBusSearchResult: false,
    stats: {
        hasElectronicTicket: false,
        hasDynamicPricing: false,
    },
};

describe('analyseSegments', () => {
    it('Для пустой выдачи выдаст стандартный объект с аггрегированными данными', () => {
        const emptySegments = [];

        expect(analyseSegments(emptySegments, state)).toEqual({
            ...defaultAnswer,
            segments: emptySegments,
            transportTypes: [],
        });
    });

    it('Для смешанной выдачи выдаст список типов транспортов', () => {
        const mixedSegments = [
            {transport: {code: TransportType.train}},
            {transport: {code: TransportType.suburban}},
            {transport: {code: TransportType.train}},
        ] as SearchSegment[];

        expect(analyseSegments(mixedSegments, state)).toEqual({
            ...defaultAnswer,
            segments: mixedSegments,
            transportTypes: [TransportType.train, TransportType.suburban],
        });
    });

    it('Для электричечной выдачи выдаст флаг isSuburbanSearchResult', () => {
        const suburbanSegments = [
            {transport: {code: TransportType.suburban}},
            {transport: {code: TransportType.suburban}},
        ] as SearchSegment[];

        const res = {
            ...defaultAnswer,
            segments: suburbanSegments,
            transportTypes: [TransportType.suburban],
            isSuburbanSearchResult: true,
        };

        expect(analyseSegments(suburbanSegments, state)).toEqual(res);
        expect(
            analyseSegments(suburbanSegments, getState(TransportType.suburban)),
        ).toEqual(res);
    });

    it('Для автобусной выдачи выдаст флаг isBusSearchResult', () => {
        const busSegments = [
            {transport: {code: TransportType.bus}},
            {transport: {code: TransportType.bus}},
        ] as SearchSegment[];

        const res = {
            ...defaultAnswer,
            segments: busSegments,
            transportTypes: [TransportType.bus],
            isBusSearchResult: true,
        };

        expect(analyseSegments(busSegments, state)).toEqual(res);
        expect(
            analyseSegments(busSegments, getState(TransportType.bus)),
        ).toEqual(res);
    });

    it('Для поездов с динамическим ценообразованием выдаст флаг stats.hasDynamicPricing', () => {
        const trainSegments = [
            {transport: {code: TransportType.train}, hasDynamicPricing: true},
            {transport: {code: TransportType.train}},
        ] as SearchSegment[];

        expect(analyseSegments(trainSegments, state)).toEqual({
            ...defaultAnswer,
            segments: trainSegments,
            transportTypes: [TransportType.train],
            stats: {
                ...defaultAnswer.stats,
                hasDynamicPricing: true,
            },
        });

        // Учтет флаг и для пересадок
        const transferSegments = [
            {
                isTransfer: true,
                transport: {code: TransportType.train},
                segments: [
                    {
                        isTransferSegment: true,
                        transport: {code: TransportType.train},
                        hasDynamicPricing: true,
                    },
                ],
            },
        ] as SearchSegment[];

        expect(analyseSegments(transferSegments, state)).toEqual({
            ...defaultAnswer,
            segments: transferSegments,
            transportTypes: [TransportType.train],
            stats: {
                ...defaultAnswer.stats,
                hasDynamicPricing: true,
            },
        });
    });

    it('Для поездов с электронной регистрацией выдаст флаг stats.hasElectronicTicket', () => {
        const trainSegments = [
            {
                transport: {code: TransportType.train},
                tariffs: {electronicTicket: true},
            },
            {transport: {code: TransportType.train}},
        ] as SearchSegment[];

        expect(analyseSegments(trainSegments, state)).toEqual({
            ...defaultAnswer,
            segments: trainSegments,
            transportTypes: [TransportType.train],
            stats: {
                ...defaultAnswer.stats,
                hasElectronicTicket: true,
            },
        });
    });
});
