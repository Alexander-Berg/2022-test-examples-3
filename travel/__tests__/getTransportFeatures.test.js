import {
    MODEL,
    DENSITY,
    ETICKET,
    SUBTYPE,
    COMPANY,
    TROUGH_TRAIN,
    DELUXE_TRAIN,
    DYNAMIC_PRICING,
    ENDPOINT_EVENT_DATA,
} from '../features';

import {TransportType} from '../../../transportType';
import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';

import getEndpointsData from '../../endpointEvents/getEndpointsData';
import getTransportFeatures from '../getTransportFeatures';

jest.mock('../../../../i18n/segment', () =>
    jest.fn(name => {
        switch (name) {
            case 'express':
                return 'экспресс';
            case 'two-storey':
                return 'двухэтажный вагон';
            case 'through-train':
                return 'беспересадочный вагон';
        }
    }),
);

jest.mock('../../endpointEvents/getEndpointsData', () => jest.fn());

const segment = {
    thread: {},
    transport: {code: TransportType.train},
    company: {},
};

const tld = Tld.ru;
const language = Lang.ru;

describe('getTransportFeatures', () => {
    it('вернёт пустой список, если соответствующие поля не определены', () => {
        expect(getTransportFeatures(segment, false, tld, language)).toEqual([]);
    });

    it('вернёт признак ENDPOINT_EVENT_DATA', () => {
        getEndpointsData.mockReturnValueOnce({
            type: 'possible_delay',
            text: 'возможно опаздание',
        });
        expect(getTransportFeatures(segment, false, tld, language)).toEqual([
            {
                type: ENDPOINT_EVENT_DATA,
                props: {
                    type: 'possible_delay',
                    text: 'возможно опаздание',
                },
            },
        ]);
    });

    it('вернёт признак ETICKET', () => {
        expect(
            getTransportFeatures(
                {
                    ...segment,
                    tariffs: {
                        electronicTicket: true,
                    },
                },
                false,
                tld,
                language,
            ),
        ).toEqual([
            {
                type: ETICKET,
            },
        ]);
    });

    it('вернёт признак DYNAMIC_PRICING', () => {
        expect(
            getTransportFeatures(
                {
                    ...segment,
                    hasDynamicPricing: true,
                },
                false,
                tld,
                language,
            ),
        ).toEqual([
            {
                type: DYNAMIC_PRICING,
            },
        ]);
    });

    describe('COMPANY', () => {
        it('вернёт признак COMPANY', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        company: {
                            title: 'Pepsi Co',
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);

            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        company: {
                            title: 'Pepsi Co',
                            hidden: false,
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);
        });

        it('вернёт признак COMPANY вместе с ссылкой', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        company: {
                            id: '33',
                            title: 'Pepsi Co',
                            hidden: false,
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: COMPANY,
                    props: {
                        href: '/info/company/33',
                    },
                    content: 'Pepsi Co',
                },
            ]);
        });

        it('вернёт список признаков COMPANY для метасегментов', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
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
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: COMPANY,
                    props: {
                        href: '/info/company/33',
                    },
                    content: 'Pepsi Co',
                },
                {
                    type: COMPANY,
                    props: {
                        href: '/info/company/34',
                    },
                    content: 'Coca-Cola',
                },
            ]);
        });

        it('вернёт признак COMPANY без ссылки, потому что hidden = true', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        company: {
                            id: '33',
                            title: 'Pepsi Co',
                            hidden: true,
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: COMPANY,
                    props: {},
                    content: 'Pepsi Co',
                },
            ]);
        });
    });

    describe('MODEL', () => {
        it('вернёт признак MODEL', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        transport: {
                            model: {
                                title: 'Rover',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: MODEL,
                    content: 'Rover',
                },
            ]);
        });

        it('если соответствующее поле пустое - не возвращаем признак MODEL', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        transport: {
                            model: {
                                title: '',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([]);
        });
    });

    describe('SUBTYPE', () => {
        it('вернёт признак SUBTYPE если задан подтип', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        transport: {
                            id: '22',
                            subtype: {
                                id: '23',
                                title: 'aeroplane',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: SUBTYPE,
                    props: {
                        style: {},
                    },
                    content: 'aeroplane',
                },
            ]);
        });

        it('если соответствующие поля пустые - не возвращаем признак SUBTYPE', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        transport: {
                            id: '22',
                            subtype: {
                                id: '23',
                                title: '',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([]);
        });

        it('вернёт признак SUBTYPE для экспрессов', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        thread: {
                            isExpress: true,
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: SUBTYPE,
                    props: {
                        style: {},
                    },
                    content: 'экспресс',
                },
            ]);
        });

        it('если идентификаторы типа и подтипа совпадают - не возвращаем признак SUBTYPE', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        transport: {
                            id: '22',
                            subtype: {
                                id: '22',
                                title: 'aeroplane',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([]);
        });

        it('вернёт признак SUBTYPE с указанием цвета', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        transport: {
                            id: '22',
                            subtype: {
                                id: '23',
                                titleColor: '#666',
                                title: 'aeroplane',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: SUBTYPE,
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
                getTransportFeatures(
                    {
                        ...segment,
                        isThroughTrain: true,
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: TROUGH_TRAIN,
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
                getTransportFeatures(eastExpress, false, tld, language),
            ).toEqual([
                {
                    type: DELUXE_TRAIN,
                    content: 'East Express',
                },
            ]);
        });

        it('если соответствующее поле пустое - не возвращаем признак DELUXE_TRAIN', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        thread: {
                            deluxeTrain: {
                                shortTitle: '',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([]);
        });

        it('Для фирменных вагонов возвращаем ссылку, если она есть', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        thread: {
                            deluxeTrain: {
                                shortTitle: 'Сапсан',
                                pagePath: '/trains/sapsan',
                            },
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    content: 'Сапсан',
                    props: {
                        href: '/trains/sapsan',
                    },
                    type: 'deluxeTrain',
                },
            ]);
        });
    });

    it('вернёт признак с наибольшим приоритетом - SUBTYPE', () => {
        const features = getTransportFeatures(
            {
                ...segment,
                isThroughTrain: true,
                thread: {
                    isExpress: true,
                    deluxeTrain: {
                        shortTitle: 'East Express',
                    },
                },
            },
            false,
            tld,
            language,
        );

        expect(features.map(({type}) => type)).toEqual([SUBTYPE]);
    });

    describe('TRAIN_CATEGORY', () => {
        const firmSegment = {
            ...segment,
            rawTrainCategory: 'СК ФИРМ',
        };

        it('для обычного окружения не возвращаем признак TRAIN_CATEGORY', () => {
            expect(
                getTransportFeatures(firmSegment, false, tld, language),
            ).toEqual([]);
        });
    });

    describe('DENSITY', () => {
        it('вернёт признак DENSITY', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        thread: {
                            density: 'every 20 minutes',
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([
                {
                    type: DENSITY,
                    content: 'every 20 minutes',
                },
            ]);
        });

        it('не вернёт признак DENSITY для мобильных устройств', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        thread: {
                            density: 'every 20 minutes',
                        },
                    },
                    true,
                    tld,
                    language,
                ),
            ).toEqual([]);
        });

        it('если соответствующее поле пустое - не возвращаем признак DENSITY', () => {
            expect(
                getTransportFeatures(
                    {
                        ...segment,
                        thread: {
                            density: '',
                        },
                    },
                    false,
                    tld,
                    language,
                ),
            ).toEqual([]);
        });
    });
});
