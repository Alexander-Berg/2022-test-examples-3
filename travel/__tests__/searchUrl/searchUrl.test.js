import {FilterTransportType} from '../../../transportType';

import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';
import DateSpecialValue from '../../../../interfaces/date/DateSpecialValue';

import searchUrl from '../../searchUrl';

const originUrl = 'https://rasp.yandex.ru';
const context = {
    from: {
        slug: 'moscow',
    },
    to: {
        slug: 'yekaterinburg',
    },
    transportType: FilterTransportType.train,
    when: {
        text: '5 апреля',
        date: '2018-04-05',
    },
    userInput: {
        from: {
            slug: 'moscow',
        },
        to: {
            slug: 'yekaterinburg',
        },
    },
    plan: 'g19',
};

const tld = Tld.ru;
const language = Lang.ru;

describe('searchUrl', () => {
    it('Поиск поездом на дату', () => {
        expect(
            decodeURIComponent(
                searchUrl(
                    {
                        context,
                    },
                    tld,
                    language,
                ),
            ),
        ).toBe('/search/train/?when=5+апреля');
    });

    it('Поиск поездом на особенное время', () => {
        expect(
            decodeURIComponent(
                searchUrl(
                    {
                        context: {
                            ...context,
                            when: {
                                ...context.when,
                                text: 'завтра',
                            },
                        },
                    },
                    tld,
                    language,
                ),
            ),
        ).toBe('/search/train/?when=завтра');
    });

    it('Поиск поездом на все дни', () => {
        expect(
            searchUrl(
                {
                    context: {
                        ...context,
                        when: {
                            ...context.when,
                            text: 'на все дни',
                            special: DateSpecialValue.allDays,
                        },
                    },
                },
                tld,
                language,
            ),
        ).toBe('/train/moscow--yekaterinburg');
    });

    it('Поиск ласточкой на сегодня', () => {
        expect(
            searchUrl({
                context: {
                    ...context,
                    when: {
                        ...context.when,
                        text: 'сегодня',
                        special: DateSpecialValue.today,
                    },
                    transportType: FilterTransportType.suburban,
                },
                filtering: {filters: {lastochka: {value: true}}},
            }),
        ).toBe('/lastochka/moscow--yekaterinburg?plan=g19');
    });

    it('Поиск электричкой на все дни с планом', () => {
        expect(
            searchUrl(
                {
                    context: {
                        ...context,
                        transportType: FilterTransportType.suburban,
                        when: {
                            ...context.when,
                            text: 'на все дни',
                            special: DateSpecialValue.allDays,
                        },
                    },
                },
                tld,
                language,
            ),
        ).toBe('/suburban/moscow--yekaterinburg?plan=g19');
    });

    it('Поиск любым транспортом на дату', () => {
        expect(
            decodeURIComponent(
                searchUrl(
                    {
                        context: {
                            ...context,
                            transportType: FilterTransportType.all,
                        },
                    },
                    tld,
                    language,
                ),
            ),
        ).toBe('/search/?when=5+апреля');
    });

    it('Поиск любым транспортом на особенное время', () => {
        expect(
            decodeURIComponent(
                searchUrl(
                    {
                        context: {
                            ...context,
                            transportType: FilterTransportType.all,
                            when: {
                                ...context.when,
                                text: 'завтра',
                            },
                        },
                    },
                    tld,
                    language,
                ),
            ),
        ).toBe('/search/?when=завтра');
    });

    it('Подстановка originUrl, если есть', () => {
        expect(
            searchUrl(
                {
                    context: {
                        ...context,
                        when: {
                            ...context.when,
                            text: 'на все дни',
                            special: 'all-days',
                        },
                    },
                },
                tld,
                language,
                originUrl,
            ),
        ).toBe(`${originUrl}/train/moscow--yekaterinburg`);
    });

    it(
        'Слаги берутся из originalFrom и originalTo, потому что' +
            'в них записывается информация до сужения в parseContext',
        () => {
            expect(
                searchUrl(
                    {
                        context: {
                            ...context,
                            when: {
                                ...context.when,
                                text: 'на все дни',
                                special: 'all-days',
                            },
                            from: {
                                slug: 'moscow',
                            },
                            to: {
                                slug: 'yekaterinburg-station',
                            },
                            originalFrom: {
                                slug: 'moscow',
                            },
                            originalTo: {
                                slug: 'yekaterinburg',
                            },
                            userInput: {
                                from: {
                                    slug: 'moscow',
                                },
                                to: {
                                    slug: 'yekaterinburg',
                                },
                            },
                        },
                    },
                    tld,
                    language,
                    originUrl,
                ),
            ).toBe(`${originUrl}/train/moscow--yekaterinburg`);
        },
    );
});
