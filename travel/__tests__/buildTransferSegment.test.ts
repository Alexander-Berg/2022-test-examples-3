import {TransportType, FilterTransportType} from '../../../transportType';
import Lang from '../../../../interfaces/Lang';
import {OrderUrlOwner} from '../../tariffClasses';
import CurrencyCode from '../../../../interfaces/CurrencyCode';

import {buildTransferSegment} from '../buildSegmentsFromTransfers';
import getAggregatedInfo from '../getAggregatedInfo';
import ITransferFromBackend from '../../../../interfaces/transfer/ITransferFromBackend';

jest.mock('../getAggregatedInfo');

const aggregatedInfo = {
    transferStations:
        'Каменск-Уральский - Екатеринбург-Пасс. - Ижевск - Чайковский',
    transferTitle: 'через Екатеринбург, Ижевск',
};

const context = {
    from: {timezone: 'Asia/Yekaterinburg'},
    to: {timezone: 'Asia/Yekaterinburg'},
    language: Lang.ru,
    transportType: FilterTransportType.all,
};

const transfer: ITransferFromBackend = {
    id: '9607404-2000003-2019-08-06T05:12-train_2000003-9612913-2019-08-07T07:42-train',
    segments: [
        {
            id: '9607404-2000003-2019-08-06T05:12-train',
            arrival: '2019-08-09T04:10:00+00:00',
            isThroughTrain: false,
            thread: {
                comment: '',
                displaceYabus: null,
                uid: '064J_1_2',
                density: '',
                title: 'Москва — Димитровград',
                number: '064Й',
                schedulePlanCode: null,
                beginTime: null,
                isAeroExpress: false,
                endTime: null,
                isBasic: true,
                isExpress: false,
            },
            price: null,
            provider: 'P1',
            departure: '2019-08-08T12:30:00+00:00',
            isInterval: false,
            convenience: 0,
            transport: {
                code: TransportType.train,
                id: 1,
                title: 'Поезд',
            },
            stationFrom: {
                settlement: {
                    title: 'Москва',
                    titleGenitive: 'Москвы',
                    titleLocative: 'Москве',
                    preposition: 'в',
                    titleAccusative: 'Москву',
                    id: 213,
                },
                countryId: 225,
                title: 'Москва (Казанский вокзал)',
                titleGenitive: 'Москвы (Казанский вокзал)',
                railwayTimezone: 'Europe/Moscow',
                titleLocative: 'Москве (Казанский вокзал)',
                preposition: 'в',
                popularTitle: 'Казанский вокзал',
                timezone: 'Europe/Moscow',
                titleAccusative: 'Москву (Казанский вокзал)',
                id: 2000003,
            },
            company: {
                shortTitle: 'РЖД/ФПК',
                title: 'РЖД/ФПК',
                url: 'http://www.rzd.ru/',
                yandexAviaUrl: null,
                hidden: false,
                id: 112,
            },
            stationTo: {
                settlement: {
                    title: 'Ульяновск',
                    titleGenitive: 'Ульяновска',
                    titleLocative: 'Ульяновске',
                    preposition: 'в',
                    titleAccusative: 'Ульяновск',
                    id: 195,
                },
                countryId: 225,
                title: 'Ульяновск-Центр.',
                titleGenitive: 'Ульяновска',
                railwayTimezone: 'Europe/Moscow',
                titleLocative: 'Ульяновске',
                preposition: 'в',
                popularTitle: '',
                timezone: 'Europe/Astrakhan',
                titleAccusative: 'Ульяновск',
                id: 9606620,
            },
            tariffs: {
                electronicTicket: true,
                classes: {
                    compartment: {
                        trainOrderUrlOwner: OrderUrlOwner.trains,
                        price: {
                            currency: CurrencyCode.rub,
                            value: 2612.2,
                        },
                        orderUrl:
                            'https://trains.yandex.ru/order/?toId=s9606620&toName=%D0%A3%D0%BB%D1%8C%D1%8F%D0%BD%D0%BE%D0%B2%D1%81%D0%BA-%D0%A6%D0%B5%D0%BD%D1%82%D1%80.&fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0+%28%D0%9A%D0%B0%D0%B7%D0%B0%D0%BD%D1%81%D0%BA%D0%B8%D0%B9+%D0%B2%D0%BE%D0%BA%D0%B7%D0%B0%D0%BB%29&fromId=s2000003&time=15%3A30&when=2019-08-08&number=064%D0%99&coachType=compartment',
                        seats: 119,
                        severalPrices: true,
                    },
                },
            },
        },
        {
            id: '2000003-9612913-2019-08-07T07:42-train',
            arrival: '2019-08-09T11:20:00+00:00',
            isThroughTrain: false,
            thread: {
                comment: '',
                displaceYabus: null,
                uid: '347ZH_9_2',
                density: '',
                title: 'Санкт-Петербург — Уфа',
                number: '347Ж',
                schedulePlanCode: null,
                beginTime: null,
                isAeroExpress: false,
                endTime: null,
                isBasic: true,
                isExpress: false,
            },
            price: null,
            provider: 'P1',
            departure: '2019-08-09T04:44:00+00:00',
            isInterval: false,
            convenience: 60,
            transport: {
                code: TransportType.train,
                id: 1,
                title: 'Поезд',
            },
            stationFrom: {
                settlement: {
                    title: 'Ульяновск',
                    titleGenitive: 'Ульяновска',
                    titleLocative: 'Ульяновске',
                    preposition: 'в',
                    titleAccusative: 'Ульяновск',
                    id: 195,
                },
                countryId: 225,
                title: 'Ульяновск-Центр.',
                titleGenitive: 'Ульяновска',
                railwayTimezone: 'Europe/Moscow',
                titleLocative: 'Ульяновске',
                preposition: 'в',
                popularTitle: '',
                timezone: 'Europe/Astrakhan',
                titleAccusative: 'Ульяновск',
                id: 9606620,
            },
            company: {
                shortTitle: 'РЖД/ФПК',
                title: 'РЖД/ФПК',
                url: 'http://www.rzd.ru/',
                yandexAviaUrl: null,
                hidden: false,
                id: 112,
            },
            stationTo: {
                settlement: {
                    title: 'Бугульма',
                    titleGenitive: 'Бугульмы',
                    titleLocative: 'Бугульме',
                    preposition: 'в',
                    titleAccusative: 'Бугульму',
                    id: 11122,
                },
                countryId: 225,
                title: 'Бугульма',
                titleGenitive: 'Бугульмы',
                railwayTimezone: 'Europe/Moscow',
                titleLocative: 'Бугульме',
                preposition: 'в',
                popularTitle: '',
                timezone: 'Europe/Moscow',
                titleAccusative: 'Бугульму',
                id: 9606503,
            },
            tariffs: {
                electronicTicket: true,
                classes: {
                    compartment: {
                        trainOrderUrlOwner: OrderUrlOwner.trains,
                        price: {
                            currency: CurrencyCode.rub,
                            value: 1469.5,
                        },
                        orderUrl:
                            'https://trains.yandex.ru/order/?toId=s9606503&toName=%D0%91%D1%83%D0%B3%D1%83%D0%BB%D1%8C%D0%BC%D0%B0&fromName=%D0%A3%D0%BB%D1%8C%D1%8F%D0%BD%D0%BE%D0%B2%D1%81%D0%BA-%D0%A6%D0%B5%D0%BD%D1%82%D1%80.&fromId=s9606620&time=08%3A44&when=2019-08-09&number=347%D0%96&coachType=compartment',
                        seats: 63,
                        severalPrices: true,
                    },
                },
            },
        },
    ],
};

describe('buildTransferSegment', () => {
    it('Pass normal transfer', () => {
        (getAggregatedInfo as jest.Mock).mockReturnValue(aggregatedInfo);
        const {language, transportType} = context;

        expect(
            buildTransferSegment({transfer, language, transportType}),
        ).toEqual({
            ...transfer,
            segments: transfer.segments.map(segment => ({
                isTransferSegment: true,
                title: segment.thread.title,
                number: segment.thread.number,
                ...segment,
            })),
            transport: transfer.segments[0].transport,
            isInterval: false,
            isTransfer: true,
            departure: '2019-08-08T12:30:00+00:00',
            arrival: '2019-08-09T11:20:00+00:00',
            duration: 82200,
            stationFrom: transfer.segments[0].stationFrom,
            stationTo:
                transfer.segments[transfer.segments.length - 1].stationTo,
            title: aggregatedInfo.transferTitle,
            ...aggregatedInfo,
        });

        expect(getAggregatedInfo).toBeCalledWith({
            transfer,
            language,
            transportType,
        });
    });
});
