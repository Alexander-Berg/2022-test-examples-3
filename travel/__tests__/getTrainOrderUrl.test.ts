import {momentTimezone as moment} from '../../../../reexports';

import {
    PLATZKARTE,
    COMPARTMENT,
    SUBURBAN,
    SITTING,
} from '../../segments/tariffClasses';

import {FilterTransportType, TransportType} from '../../transportType';
import CurrencyCode from '../../../interfaces/CurrencyCode';
import ISearchContext from '../../../interfaces/state/search/ISearchContext';
import ISegment from '../../../interfaces/segment/ISegment';
import IStateCurrencies from '../../../interfaces/state/IStateCurrencies';
import IStateFlags from '../../../interfaces/state/flags/IStateFlags';
import IStateSeoQueryParams from '../../../interfaces/state/IStateSeoQueryParams';

import getTrainOrderUrl from '../getTrainOrderUrl';
import encodeValue from '../encodeValue';
import applyUtm from '../applyUtm';

jest.mock('../applyUtm', () => jest.fn(url => url));

const segment = {
    thread: {
        number: '036Я',
    },
    originalNumber: '036Э',
    departure: '2017-08-16T10:00:00+00:00',
    stationFrom: {
        timezone: 'Europe/Moscow',
    },
    transport: {
        code: TransportType.train,
    },
    company: {
        id: 59609,
    },
} as ISegment;

const context = {
    from: {
        key: 'c213',
    },
    to: {
        key: 'c23243',
    },
    userInput: {
        from: {
            title: 'Москва',
        },
        to: {
            title: 'Нижний Новгород',
        },
    },
    transportType: FilterTransportType.train,
} as ISearchContext;

const moscowVladimirContext = {
    from: {
        key: 'c213',
    },
    to: {
        key: 's2060340',
    },
    userInput: {
        from: {
            title: 'Москва',
        },
        to: {
            title: 'Владимир',
        },
    },
    transportType: FilterTransportType.suburban,
} as ISearchContext;

const cppkSegment = {
    thread: {
        number: '7082',
    },
    departure: '2020-10-12T04:31:00+00:00',
    stationFrom: {
        timezone: 'Europe/Moscow',
    },
    transport: {
        code: TransportType.suburban,
    },
    trainPurchaseNumbers: ['882M'],
    company: {id: 153},
} as ISegment;

const cppkSegmentTime = '07%3A31';

const seoQueryParams = {reqId: 'reqId'} as IStateSeoQueryParams;
const clientId = '123';

const currencies = {
    nationalCurrency: CurrencyCode.rub,
    preferredCurrency: CurrencyCode.rub,
} as IStateCurrencies;

const flags = {
    __experiment: false,
} as IStateFlags;

const isProduction = true;

const TIME = '13%3A00';
const WHEN = '2017-08-16';

describe('getTrainOrderUrl', () => {
    it('Москва - Нижний Новгород, рубли, плацкарт', () => {
        expect(
            getTrainOrderUrl({
                coachType: PLATZKARTE,
                context,
                segment,
                currencies,
                flags,
                isProduction,
            }),
        ).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${PLATZKARTE}&fromId=${
                context.from.key
            }&fromName=${encodeValue(
                context.userInput.from.title,
            )}&number=${encodeValue(
                segment.originalNumber,
            )}&time=${TIME}&toId=${context.to.key}&toName=${encodeValue(
                context.userInput.to.title,
            )}&transportType=${FilterTransportType.train}&when=${WHEN}`,
        );
    });

    it('Москва - Нижний Новгород, рубли, плацкарт, эксперимент', () => {
        const expectedUrl = `https://travel-test.yandex.ru/trains/order/?__experiment=1&coachType=${PLATZKARTE}&fromId=${
            context.from.key
        }&fromName=${encodeValue(
            context.userInput.from.title,
        )}&number=${encodeValue(segment.originalNumber)}&time=${TIME}&toId=${
            context.to.key
        }&toName=${encodeValue(context.userInput.to.title)}&transportType=${
            FilterTransportType.train
        }&when=${WHEN}`;

        expect(
            getTrainOrderUrl({
                flags: {
                    __experiment: true,
                } as IStateFlags,
                coachType: PLATZKARTE,
                context,
                segment,
                currencies,
                isProduction: false,
            }),
        ).toBe(expectedUrl);

        expect(applyUtm).toBeCalledWith(
            expectedUrl,
            undefined,
            undefined,
            undefined,
        );
    });

    it('Москва - Нижний Новгород, рубли, купе, есть seo параметры', () => {
        const expectedUrl = `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${COMPARTMENT}&fromId=${
            context.from.key
        }&fromName=${encodeValue(
            context.userInput.from.title,
        )}&number=${encodeValue(segment.originalNumber)}&time=${TIME}&toId=${
            context.to.key
        }&toName=${encodeValue(context.userInput.to.title)}&transportType=${
            FilterTransportType.train
        }&when=${WHEN}`;

        expect(
            getTrainOrderUrl({
                coachType: COMPARTMENT,
                context,
                segment,
                currencies,
                flags,
                isProduction,
                seoQueryParams,
            }),
        ).toBe(expectedUrl);

        expect(applyUtm).toBeCalledWith(
            expectedUrl,
            seoQueryParams,
            undefined,
            undefined,
        );
    });

    it('Москва - Нижний Новгород, доллары, плацкарт, переданы сео параметры и clientId', () => {
        const expectedUrl = `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${PLATZKARTE}&currency=${
            CurrencyCode.usd
        }&fromId=${context.from.key}&fromName=${encodeValue(
            context.userInput.from.title,
        )}&number=${encodeValue(segment.originalNumber)}&time=${TIME}&toId=${
            context.to.key
        }&toName=${encodeValue(context.userInput.to.title)}&transportType=${
            FilterTransportType.train
        }&when=${WHEN}`;

        expect(
            getTrainOrderUrl({
                coachType: PLATZKARTE,
                context,
                segment,
                currencies: {
                    ...currencies,
                    preferredCurrency: CurrencyCode.usd,
                },
                flags,
                isProduction,
                seoQueryParams,
                clientId,
            }),
        ).toBe(expectedUrl);

        expect(applyUtm).toBeCalledWith(
            expectedUrl,
            seoQueryParams,
            clientId,
            undefined,
        );
    });

    it('Москва - Нижний Новгород, рубли, плацкарт, все типы транспорта, переданы сео параметры, clientId и utmMedium', () => {
        const expectedUrl = `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${PLATZKARTE}&fromId=${
            context.from.key
        }&fromName=${encodeValue(
            context.userInput.from.title,
        )}&number=${encodeValue(segment.originalNumber)}&time=${TIME}&toId=${
            context.to.key
        }&toName=${encodeValue(context.userInput.to.title)}&transportType=${
            FilterTransportType.all
        }&when=${WHEN}`;

        expect(
            getTrainOrderUrl({
                coachType: PLATZKARTE,
                context: {
                    ...context,
                    transportType: FilterTransportType.all,
                },
                segment,
                currencies,
                flags,
                isProduction,
                seoQueryParams,
                clientId,
                utmMedium: 'test',
            }),
        ).toBe(expectedUrl);

        expect(applyUtm).toBeCalledWith(
            expectedUrl,
            seoQueryParams,
            clientId,
            'test',
        );
    });

    it('Москва - Нижний Новгород, рубли, плацкарт, нет originalNumber', () => {
        expect(
            getTrainOrderUrl({
                coachType: PLATZKARTE,
                context,
                segment: {
                    ...segment,
                    originalNumber: undefined,
                },
                currencies,
                flags,
                isProduction,
            }),
        ).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${PLATZKARTE}&fromId=${
                context.from.key
            }&fromName=${encodeValue(
                context.userInput.from.title,
            )}&number=${encodeValue(
                segment?.thread?.number,
            )}&time=${TIME}&toId=${context.to.key}&toName=${encodeValue(
                context.userInput.to.title,
            )}&transportType=${FilterTransportType.train}&when=${WHEN}`,
        );
    });

    it('Москва - Нижний Новгород, рубли, плацкарт, есть departureMoment', () => {
        expect(
            getTrainOrderUrl({
                coachType: PLATZKARTE,
                context,
                segment: {
                    ...segment,
                    departureMoment: moment
                        .tz(segment.departure, segment.stationFrom.timezone)
                        .month(9),
                },
                currencies,
                flags,
                isProduction,
            }),
        ).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${PLATZKARTE}&fromId=${
                context.from.key
            }&fromName=${encodeValue(
                context.userInput.from.title,
            )}&number=${encodeValue(
                segment.originalNumber,
            )}&time=${TIME}&toId=${context.to.key}&toName=${encodeValue(
                context.userInput.to.title,
            )}&transportType=${FilterTransportType.train}&when=2017-10-16`,
        );
    });

    it('Москва - Нижний Новгород, рубли, плацкарт, задан departure', () => {
        expect(
            getTrainOrderUrl({
                coachType: PLATZKARTE,
                context,
                segment,
                departure: moment
                    .tz(segment.departure, segment.stationFrom.timezone)
                    .month(9),
                currencies,
                flags,
                isProduction,
            }),
        ).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${PLATZKARTE}&fromId=${
                context.from.key
            }&fromName=${encodeValue(
                context.userInput.from.title,
            )}&number=${encodeValue(
                segment.originalNumber,
            )}&time=${TIME}&toId=${context.to.key}&toName=${encodeValue(
                context.userInput.to.title,
            )}&transportType=${FilterTransportType.train}&when=2017-10-16`,
        );
    });

    it('Москва - Владимир, для экспрессов ЦППК, выдача электричками', () => {
        const getTrainOrderUrlParams = {
            coachType: SUBURBAN,
            context: moscowVladimirContext,
            segment: cppkSegment,
            departure: moment.tz(
                cppkSegment.departure,
                cppkSegment.stationFrom.timezone,
            ),
            currencies,
            flags,
            isProduction,
        };

        expect(getTrainOrderUrl(getTrainOrderUrlParams)).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${SUBURBAN}&fromId=${
                moscowVladimirContext.from.key
            }&fromName=${encodeValue(
                moscowVladimirContext.userInput.from.title,
            )}&number=${encodeValue(
                cppkSegment.trainPurchaseNumbers?.[0],
            )}&provider=P2&time=${cppkSegmentTime}&toId=${
                moscowVladimirContext.to.key
            }&toName=${encodeValue(
                moscowVladimirContext.userInput.to.title,
            )}&transportType=${FilterTransportType.suburban}&when=2020-10-12`,
        );
    });

    it('Москва - Владимир, для экспрессов ЦППК, выдача поездами', () => {
        const getTrainOrderUrlParams = {
            coachType: SITTING,
            context: {
                ...moscowVladimirContext,
                transportType: FilterTransportType.train,
            } as ISearchContext,
            segment: {
                ...cppkSegment,
                transport: {code: TransportType.train},
                trainPurchaseNumbers: undefined,
                thread: {number: '882M'},
            } as ISegment,
            departure: moment.tz(
                cppkSegment.departure,
                cppkSegment.stationFrom.timezone,
            ),
            currencies,
            flags,
            isProduction,
        };

        expect(getTrainOrderUrl(getTrainOrderUrlParams)).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${SITTING}&fromId=${
                moscowVladimirContext.from.key
            }&fromName=${encodeValue(
                moscowVladimirContext.userInput.from.title,
            )}&number=${encodeValue(
                '882M',
            )}&provider=P2&time=${cppkSegmentTime}&toId=${
                moscowVladimirContext.to.key
            }&toName=${encodeValue(
                moscowVladimirContext.userInput.to.title,
            )}&transportType=${FilterTransportType.train}&when=2020-10-12`,
        );
    });

    it('Москва - Владимир, для экспрессов ЦППК, выдача всеми видами транспорта', () => {
        const getTrainOrderUrlParams = {
            coachType: SITTING,
            context: {
                ...moscowVladimirContext,
                transportType: FilterTransportType.all,
            },
            segment: cppkSegment,
            departure: moment.tz(
                cppkSegment.departure,
                cppkSegment.stationFrom.timezone,
            ),
            currencies,
            flags,
            isProduction,
        };

        expect(getTrainOrderUrl(getTrainOrderUrlParams)).toBe(
            `https://travel.yandex.ru/trains/order/?__experiment=0&coachType=${SITTING}&fromId=${
                moscowVladimirContext.from.key
            }&fromName=${encodeValue(
                moscowVladimirContext.userInput.from.title,
            )}&number=${encodeValue(
                cppkSegment.trainPurchaseNumbers?.[0],
            )}&provider=P2&time=${cppkSegmentTime}&toId=${
                moscowVladimirContext.to.key
            }&toName=${encodeValue(
                moscowVladimirContext.userInput.to.title,
            )}&transportType=${FilterTransportType.all}&when=2020-10-12`,
        );
    });
});
