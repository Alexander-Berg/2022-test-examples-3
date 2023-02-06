import {FilterTransportType, TransportType} from '../../transportType';
import SegmentSubtypeCode from '../../../interfaces/segment/SegmentSubtypeCode';
import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';
import CurrencyCode from '../../../interfaces/CurrencyCode';
import Lang from '../../../interfaces/Lang';

import {
    shouldShowSearchDescription,
    getYears,
    buildSearchDescription,
} from '../search';

const allDayContext = {when: {special: DateSpecialValue.allDays}};
const todayContext = {when: {special: DateSpecialValue.today}};
const otherContext = {};
const foundSegments = [{}, {}];
const noSegments = [];

const language = Lang.ru;

describe('shouldShowSearchDescription', () => {
    it('Должен вернуть false для пустого результата.', () => {
        const search = {
            context: allDayContext,
            segments: noSegments,
            querying: {},
        };
        const page = {
            fetching: null,
        };

        expect(shouldShowSearchDescription(search, page)).toBe(false);
    });

    it('Должен вернуть false для контекста не на все дни и не на сегодня.', () => {
        const search = {
            context: otherContext,
            segments: foundSegments,
            querying: {},
        };
        const page = {
            fetching: null,
        };

        expect(shouldShowSearchDescription(search, page)).toBe(false);
    });

    it('Должен вернуть true для контекста на все дни если ещё идет опрос.', () => {
        const search = {
            context: allDayContext,
            segments: noSegments,
            querying: {},
        };
        const page = {
            fetching: true,
        };

        expect(shouldShowSearchDescription(search, page)).toBe(true);
    });

    it('Должен вернуть true для контекста на все дни.', () => {
        const search = {
            context: allDayContext,
            segments: foundSegments,
            querying: {},
        };
        const page = {
            fetching: null,
        };

        expect(shouldShowSearchDescription(search, page)).toBe(true);
    });

    it('Должен вернуть true для контекста на сегодня.', () => {
        const search = {
            context: todayContext,
            segments: foundSegments,
            querying: {},
        };
        const page = {
            fetching: null,
        };

        expect(shouldShowSearchDescription(search, page)).toBe(true);
    });
});

const aprilTime = 1522824164000; // 04.04.2018
const borderlineTime = 1541014244000; // 01.11.2018 00:30 ('Asia/Yekaterinburg')
const decemberTime = 1543905746429; // 04.12.2018
const timezone = 'Asia/Yekaterinburg';
const singleYearAnswer = [2018];
const doubleYearAnswer = [2018, 2019];

describe('getYears', () => {
    it('Должен вернуть один год для апреля - без таймзоны.', () => {
        expect(getYears({now: aprilTime})).toEqual(singleYearAnswer);
    });

    it('Должен вернуть один год для апреля - с таймзоной.', () => {
        expect(getYears({now: aprilTime, timezone})).toEqual(singleYearAnswer);
    });

    it('Должен вернуть один год для промежуточного времени - без таймзоны.', () => {
        expect(getYears({now: borderlineTime})).toEqual(singleYearAnswer);
    });

    it('Должен вернуть два года для промежуточного времени - с таймзоной', () => {
        expect(getYears({now: borderlineTime, timezone})).toEqual(
            doubleYearAnswer,
        );
    });

    it('Должен вернуть два года для декабря - без таймзоны', () => {
        expect(getYears({now: decemberTime})).toEqual(doubleYearAnswer);
    });

    it('Должен вернуть два года для декабря - с таймзоной', () => {
        expect(getYears({now: decemberTime, timezone})).toEqual(
            doubleYearAnswer,
        );
    });
});

const testContext = {
    suburban: {
        from: {title: 'Москва (Ленинградский вокзал)'},
        to: {title: 'Тверь'},
        time: {now: aprilTime},
        transportType: FilterTransportType.suburban,
    },
    train: {
        from: {title: 'Москва'},
        to: {title: 'Нижний Новгород'},
        time: {now: aprilTime},
        transportType: FilterTransportType.train,
    },
    all: {
        from: {title: 'Москва'},
        to: {title: 'Нижний Новгород'},
        time: {now: aprilTime},
        transportType: FilterTransportType.all,
    },
};

const baseSuburbanSegment = {
    title: 'Москва (Ленинградский вокзал) — Тверь',
    duration: 9000,
    transport: {
        code: FilterTransportType.suburban,
        title: 'Пригородный поезд',
        subtype: {
            code: FilterTransportType.suburban,
            title: 'Пригородный поезд',
        },
    },
};

const fastSuburbanSegment = {
    ...baseSuburbanSegment,
    duration: 5340,
};

const cheapSuburbanSegment = {
    ...baseSuburbanSegment,
    tariffs: {
        classes: {
            suburban: {
                price: {currency: CurrencyCode.rub, value: 300},
            },
        },
    },
};

const lastochkaSuburbanSegment = {
    ...baseSuburbanSegment,
    transport: {
        ...baseSuburbanSegment.transport,
        subtype: {
            code: SegmentSubtypeCode.lastochka,
            title: 'Ласточка',
        },
    },
};

const baseTrainSegment = {
    title: 'Москва — Нижний Новгород',
    duration: 15000,
    transport: {code: TransportType.train, title: 'Поезд'},
};

const fastTrainSegment = {
    ...baseTrainSegment,
    duration: 12900,
    tariffs: {
        classes: {
            sitting: {price: {currency: CurrencyCode.rub, value: 1528}},
        },
    },
};

const lastochkaTrainSegment = {
    ...baseTrainSegment,
    tariffs: {
        classes: {
            sitting: {price: {currency: CurrencyCode.rub, value: 528}},
        },
    },

    thread: {
        deluxeTrain: {
            id: '252',
            isDeluxe: false,
            isHighSpeed: true,
            title: 'Ласточка',
        },
    },
};

const sapsanTrainSegment = {
    ...baseTrainSegment,
    tariffs: {
        classes: {
            suite: {price: {currency: CurrencyCode.rub, value: 2943}},
        },
    },

    thread: {
        deluxeTrain: {
            id: '258',
            isDeluxe: false,
            isHighSpeed: true,
            title: 'Сапсан',
        },
    },
};

const minPriceData = {
    suburban: {
        transportType: FilterTransportType.suburban,
        price: {
            class: FilterTransportType.suburban,
            currency: CurrencyCode.rub,
            value: 300,
        },
    },

    train: {
        transportType: FilterTransportType.train,
        price: {
            class: FilterTransportType.train,
            currency: CurrencyCode.rub,
            value: 600,
        },
    },
};

describe('buildSearchDescription', () => {
    it('Описание для электричек.', () => {
        const input = {
            context: testContext.suburban,
            language,
            segments: [fastSuburbanSegment, baseSuburbanSegment],
            transportTypes: [FilterTransportType.suburban],
        };

        expect(buildSearchDescription(input)).toEqual({
            title: 'Расписание электричек Москва (Ленинградский вокзал) — Тверь с изменениями, цена билетов',
            text: [
                'Расписание электричек Москва (Ленинградский вокзал) — Тверь на сегодня, завтра, все дни с изменениями и отменами. Самая быстрая электричка Москва (Ленинградский вокзал) — Тверь доезжает за 1 ч 29 мин. Яндекс Расписания показывают график движения электричек с учётом отмен и других изменений.',
            ],
        });
    });

    it('Описание для электричек с ласточками.', () => {
        const input = {
            context: testContext.suburban,
            language,
            segments: [fastSuburbanSegment, lastochkaSuburbanSegment],
            transportTypes: [FilterTransportType.suburban],
        };

        expect(buildSearchDescription(input)).toEqual({
            title: 'Расписание электричек Москва (Ленинградский вокзал) — Тверь с изменениями, цена билетов',
            text: [
                'Расписание электричек и «Ласточек» Москва (Ленинградский вокзал) — Тверь на сегодня, завтра, все дни с изменениями и отменами. Самая быстрая электричка Москва (Ленинградский вокзал) — Тверь доезжает за 1 ч 29 мин. Яндекс Расписания показывают график движения электричек с учётом отмен и других изменений.',
            ],
        });
    });

    it('Описание для электричек с ценами.', () => {
        const input = {
            context: testContext.suburban,
            language,
            segments: [fastSuburbanSegment, baseSuburbanSegment],
            transportTypes: [FilterTransportType.suburban],
            minPriceData: minPriceData.suburban,
        };

        expect(buildSearchDescription(input)).toEqual({
            title: 'Расписание электричек Москва (Ленинградский вокзал) — Тверь с изменениями, цена билетов',
            text: [
                'Расписание электричек Москва (Ленинградский вокзал) — Тверь на сегодня, завтра, все дни с изменениями и отменами. Самая быстрая электричка Москва (Ленинградский вокзал) — Тверь доезжает за 1 ч 29 мин. Цена билета по полному тарифу — от 300 ₽. Яндекс Расписания показывают график движения электричек с учётом отмен и других изменений.',
            ],
        });
    });

    it('Описание для поездов без подтипов.', () => {
        const input = {
            context: testContext.train,
            language,
            segments: [fastTrainSegment, baseTrainSegment],
            transportTypes: [FilterTransportType.train],
        };

        expect(buildSearchDescription(input)).toEqual({
            title: 'Поезда Москва — Нижний Новгород: расписание, цены и ж/д билеты',
            text: [
                'Расписание поездов Москва — Нижний Новгород со всеми изменениями от РЖД, 2018 год. Стоимость билетов, покупка ж/д билетов онлайн. Время в пути на поезде от 3 ч 35 мин. Купить билет на поезд онлайн, чтобы не стоять в очереди у кассы.',
            ],
        });
    });

    it('Описание для поездов с подтипами, классами и ценами.', () => {
        const input = {
            context: testContext.train,
            language,
            segments: [
                lastochkaTrainSegment,
                sapsanTrainSegment,
                fastTrainSegment,
            ],
            transportTypes: [FilterTransportType.train],
            minPriceData: minPriceData.train,
        };

        expect(buildSearchDescription(input)).toEqual({
            title: 'Поезда Москва — Нижний Новгород: расписание, цены и ж/д билеты',
            text: [
                'Расписание поездов Москва — Нижний Новгород со всеми изменениями от РЖД, 2018 год. Стоимость билетов, покупка ж/д билетов онлайн. Билеты на поезда Ласточка от 528 ₽, Сапсан от 2 943 ₽. Цена билета в СВ от 2 943 ₽, в сидячий вагон от 528 ₽. Время в пути на поезде от 3 ч 35 мин. Купить билет на поезд онлайн, чтобы не стоять в очереди у кассы.',
            ],
        });
    });

    it('Описание для всех типов с ценами.', () => {
        const input = {
            context: testContext.all,
            language,
            segments: [fastTrainSegment, cheapSuburbanSegment],
            transportTypes: [
                FilterTransportType.train,
                FilterTransportType.suburban,
            ],
            minPriceData: minPriceData.suburban,
        };

        expect(buildSearchDescription(input)).toEqual({
            title: 'Расписание транспорта Москва — Нижний Новгород: поезда и электрички',
            text: [
                'Расписание транспорта Москва — Нижний Новгород: поезда и электрички. Цена билета на поезд от 1 528 ₽, на электричку от 300 ₽. Время в пути: поезд от 3 ч 35 мин, электричка от 2 ч 30 мин. Купить билет онлайн.',
            ],
        });
    });
});
