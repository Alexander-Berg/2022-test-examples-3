import {FilterTransportType} from '../../transportType';

import notFoundTitle from '../notFoundTitle';

import {searchArchivalH1} from '../searchTitle';

import keyset from '../../../i18n/search-title-notfound';

jest.mock('../../../i18n/search-title-notfound');
jest.mock('../../tanker/tankerParse', () => jest.fn(result => result));
jest.mock('../../routeTitle', () =>
    jest.fn().mockReturnValue({
        titleInfinitive: 'мск - спб',
        titleInflected: 'мска - спба',
    }),
);
jest.mock('../searchTitle', () => ({
    ...require.requireActual('../searchTitle'),
    getAllTypeUrl: jest.fn(),
    getAllDaysUrl: jest.fn(),
    getFormattedDate: jest.fn().mockReturnValue('12-12-2017'),
    searchArchivalH1: jest.fn(),
}));

const sort = {};
const flags = {};
const context = {
    transportType: FilterTransportType.train,
    language: 'ru',
    when: {
        date: '2017-12-12',
    },
    time: {
        // 2017-12-12
        now: 1513020670000,
    },
};
const newPlanContext = {
    transportType: FilterTransportType.suburban,
    language: 'ru',
    when: {
        date: '2017-12-12',
    },
    time: {
        // 2017-12-02
        now: 1512205455000,
    },
};

const data = {sort, flags, context};
const newPlanData = {sort, flags, context: newPlanContext};

const notFoundDateTitle = {
    title: 'На выбранную дату рейсов нет',
    subtitle: 'Попробуйте поиск на все дни',
};
const notFoundDateNewPlanTitle = {
    title: 'На выбранную дату рейсов нет',
    subtitle: 'Попробуйте поиск на все дни. А скоро будут новые планы.',
};
const notFoundAllDaysTitle = {
    title: 'По данному направлению нет рейсов',
    subtitle: 'Выберите другой пункт отправления/прибытия',
};

const dateParams = {
    titleInfinitive: 'мск - спб',
    titleInflected: 'мска - спба',
    newPlanDateWithYear: '10 december 2017',
    newPlanDate: '10 december',
    date: '12-12-2017',
};

const newPlanDateParams = {
    titleInfinitive: 'мск - спб',
    titleInflected: 'мска - спба',
    newPlanDateWithYear: '10 december 2017',
    newPlanDate: '10 december',
    date: '12-12-2017',
};

const allDaysParams = {
    titleInfinitive: 'мск - спб',
    titleInflected: 'мска - спба',
};

const archivalDatаContext = {
    language: 'ru',
    when: {
        date: '2017-12-12',
    },
    time: {
        // 2017-12-12
        now: 1513020670000,
    },
    from: {
        titleGenitive: 'Москвы',
        preposition: 'в',
        key: 'c213',
        popularTitle: 'Москва',
        timezone: 'Europe/Moscow',
        settlement: {slug: 'moscow', title: 'Москва'},
        slug: 'moscow',
        shortTitle: 'Москва',
        title: 'Москва',
        country: {
            railwayTimezone: 'Europe/Moscow',
            code: 'RU',
            title: 'Россия',
        },
        region: {title: 'Москва и Московская область'},
        titleLocative: 'Москве',
        titleAccusative: 'Москву',
    },
    to: {
        titleGenitive: 'Санкт-Петербурга',
        preposition: 'в',
        key: 'c2',
        popularTitle: 'Санкт-Петербург',
        timezone: 'Europe/Moscow',
        settlement: {slug: 'saint-petersburg', title: 'Санкт-Петербург'},
        slug: 'saint-petersburg',
        shortTitle: 'Санкт-Петербург',
        title: 'Санкт-Петербург',
        country: {
            railwayTimezone: 'Europe/Moscow',
            code: 'RU',
            title: 'Россия',
        },
        region: {title: 'Санкт-Петербург и Ленинградская область'},
        titleLocative: 'Санкт-Петербурге',
        titleAccusative: 'Санкт-Петербург',
    },
};

const notFoundArchivalDataAllDays = {
    title: 'Расписание транспорта и билеты на поезд и автобус из Москвы в Санкт-Петербург',
    subtitle: '',
};

const notFoundArchivalDataOnDate = {
    title: 'Расписание транспорта и билеты на поезд и автобус из Москвы в Санкт-Петербург',
    subtitle: '12 december, tuesday',
};

describe('notFoundTitle', () => {
    it('вернёт заголовок для поиска на дату', () => {
        keyset
            .mockReturnValueOnce(notFoundDateTitle.title)
            .mockReturnValueOnce(notFoundDateTitle.subtitle);

        expect(notFoundTitle(data)).toEqual(notFoundDateTitle);
        expect(keyset).toHaveBeenCalledWith('title-date-train', dateParams);
        expect(keyset).toHaveBeenCalledWith('subtitle-date-train', dateParams);
    });

    it('вернёт заголовок для поиска на дату с новым планом', () => {
        keyset
            .mockReturnValueOnce(notFoundDateTitle.title)
            .mockReturnValueOnce(notFoundDateNewPlanTitle.subtitle);

        expect(notFoundTitle(newPlanData)).toEqual(notFoundDateNewPlanTitle);
        expect(keyset).toHaveBeenCalledWith(
            'title-date-suburban',
            newPlanDateParams,
        );
        expect(keyset).toHaveBeenCalledWith(
            'subtitle-date-suburban-new-plan',
            newPlanDateParams,
        );
    });

    it('вернёт заголовок для поиска на все дни', () => {
        keyset
            .mockReturnValueOnce(notFoundAllDaysTitle.title)
            .mockReturnValueOnce(notFoundAllDaysTitle.subtitle);

        expect(
            notFoundTitle({
                ...data,
                context: {
                    ...data.context,
                    when: {},
                },
            }),
        ).toEqual(notFoundAllDaysTitle);
        expect(keyset).toHaveBeenCalledWith(
            'title-alldays-train',
            allDaysParams,
        );
        expect(keyset).toHaveBeenCalledWith(
            'subtitle-alldays-train',
            allDaysParams,
        );
    });

    it('если передан объект archivalDatа для поиска на все дни вернет заголовок и subtitle = ""', () => {
        searchArchivalH1.mockReturnValueOnce(notFoundArchivalDataAllDays);

        expect(
            notFoundTitle({
                ...data,
                context: archivalDatаContext,
                archivalData: {
                    canonical: {
                        pointFrom: 'moscow',
                        pointTo: 'saint-petersburg',
                        transportType: 'all',
                    },
                    transportTypes: ['bus', 'train'],
                },
            }),
        ).toEqual(notFoundArchivalDataAllDays);
    });

    it('если передан объект archivalDatа для поиска на конкретный день вернет заголовок и subtitle с датой', () => {
        searchArchivalH1.mockReturnValueOnce(notFoundArchivalDataOnDate);

        expect(
            notFoundTitle({
                ...data,
                context: archivalDatаContext,
                archivalData: {
                    canonical: {
                        pointFrom: 'moscow',
                        pointTo: 'saint-petersburg',
                        transportType: 'all',
                    },
                    transportTypes: ['bus', 'train'],
                },
            }),
        ).toEqual(notFoundArchivalDataOnDate);
    });
});
