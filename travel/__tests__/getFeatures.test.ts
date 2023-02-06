import {TRAIN_FEATURES_TYPES} from 'projects/trains/lib/segments/features/types';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {getTrainFeatures} from '../getTrainFeatures';

const segment = {
    thread: {},
    transport: {},
    company: {},
};

describe('getTransportFeatures', () => {
    it('вернёт пустой список, если соответствующие поля не определены', () => {
        expect(getTrainFeatures(segment as ITrainsTariffApiSegment)).toEqual(
            [],
        );
    });

    it('вернёт признак ETICKET', () => {
        expect(
            getTrainFeatures({
                ...segment,
                tariffs: {
                    electronicTicket: true,
                },
            } as ITrainsTariffApiSegment),
        ).toEqual([
            {
                type: TRAIN_FEATURES_TYPES.ETICKET,
            },
        ]);
    });

    it('вернёт признак DYNAMIC_PRICING', () => {
        expect(
            getTrainFeatures({
                ...segment,
                hasDynamicPricing: true,
            } as ITrainsTariffApiSegment),
        ).toEqual([
            {
                type: TRAIN_FEATURES_TYPES.DYNAMIC_PRICING,
            },
        ]);
    });

    describe('COMPANY', () => {
        it('вернёт признак COMPANY', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    company: {
                        title: 'Pepsi Co',
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);

            expect(
                getTrainFeatures({
                    ...segment,
                    company: {
                        title: 'Pepsi Co',
                        hidden: false,
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);
        });

        it('вернёт признак COMPANY для рейса пополнения', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    company: {
                        ufsTitle: 'Pepsi Co',
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);
        });

        it('вернёт список признаков COMPANY для метасегментов', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    subSegments: [{}, {}],
                    companies: [
                        {
                            id: '33',
                            title: 'Pepsi Co',
                            hidden: false,
                        },
                        {
                            id: '34',
                            title: 'Coca-Cola',
                            hidden: false,
                        },
                    ],
                } as any),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
                {
                    type: TRAIN_FEATURES_TYPES.COMPANY,
                    props: {},
                    content: 'Coca-Cola',
                },
            ]);
        });

        it('вернёт признак COMPANY без ссылки, потому что hidden = true', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    company: {
                        id: '33',
                        title: 'Pepsi Co',
                        hidden: true,
                    },
                } as any),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);
        });
    });

    describe('MODEL', () => {
        it('вернёт признак MODEL', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    transport: {
                        model: {
                            title: 'Rover',
                        },
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.MODEL,
                    content: 'Rover',
                },
            ]);
        });

        it('если соответствующее поле пустое - не возвращаем признак MODEL', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    transport: {
                        model: {
                            title: '',
                        },
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([]);
        });
    });

    describe('SUBTYPE', () => {
        it('вернёт признак SUBTYPE если задан подтип', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    transport: {
                        id: '22',
                        subtype: {
                            id: '23',
                            title: 'aeroplane',
                        },
                    },
                } as any),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.SUBTYPE,
                    props: {
                        style: {},
                    },
                    content: 'aeroplane',
                },
            ]);
        });

        it('если соответствующие поля пустые - не возвращаем признак SUBTYPE', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    transport: {
                        id: '22',
                        subtype: {
                            id: '23',
                            title: '',
                        },
                    },
                } as any),
            ).toEqual([]);
        });

        it('вернёт признак SUBTYPE для экспрессов', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    thread: {
                        isExpress: true,
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.SUBTYPE,
                    props: {
                        style: {},
                    },
                    content: 'экспресс',
                },
            ]);
        });

        it('если идентификаторы типа и подтипа совпадают - не возвращаем признак SUBTYPE', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    transport: {
                        id: '22',
                        subtype: {
                            id: '22',
                            title: 'aeroplane',
                        },
                    },
                } as any),
            ).toEqual([]);
        });

        it('вернёт признак SUBTYPE с указанием цвета', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    transport: {
                        id: '22',
                        subtype: {
                            id: '23',
                            titleColor: '#666',
                            title: 'aeroplane',
                        },
                    },
                } as any),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.SUBTYPE,
                    props: {
                        style: {
                            color: '#666',
                        },
                    },
                    content: 'aeroplane',
                },
            ]);
        });
    });

    describe('TROUGH_TRAIN', () => {
        it('вернёт признак TROUGH_TRAIN', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    isThroughTrain: true,
                } as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.TROUGH_TRAIN,
                    content: 'беспересадочный вагон',
                },
            ]);
        });
    });

    describe('DELUXE_TRAIN', () => {
        const eastExpress = {
            ...segment,
            thread: {
                deluxeTrain: {
                    shortTitle: 'East Express',
                },
            },
        };

        it('вернёт признак DELUXE_TRAIN', () => {
            expect(
                getTrainFeatures(eastExpress as ITrainsTariffApiSegment),
            ).toEqual([
                {
                    type: TRAIN_FEATURES_TYPES.DELUXE_TRAIN,
                    content: 'East Express',
                },
            ]);
        });

        it('если соответствующее поле пустое - не возвращаем признак DELUXE_TRAIN', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    thread: {
                        deluxeTrain: {
                            shortTitle: '',
                        },
                    },
                } as ITrainsTariffApiSegment),
            ).toEqual([]);
        });

        /* TODO: вернуть когда будет страница сапсанов
        it('Для фирменных вагонов возвращаем ссылку, если она есть', () => {
            expect(
                getTrainFeatures({
                    ...segment,
                    thread: {
                        deluxeTrain: {
                            shortTitle: 'Сапсан',
                            id: 258
                        }
                    }
                })
            ).toEqual([
                {
                    content: 'Сапсан',
                    props: {
                        href: '/sapsan'
                    },
                    type: 'deluxeTrain'
                }
            ]);
        });
        */
    });

    it('вернёт признак с наибольшим приоритетом - SUBTYPE', () => {
        const features = getTrainFeatures({
            ...segment,
            isThroughTrain: true,
            thread: {
                isExpress: true,
                deluxeTrain: {
                    shortTitle: 'East Express',
                },
            },
        } as ITrainsTariffApiSegment);

        expect(features.map(({type}) => type)).toEqual([
            TRAIN_FEATURES_TYPES.SUBTYPE,
        ]);
    });
});
