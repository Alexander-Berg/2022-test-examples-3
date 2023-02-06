import React from 'react';
import { mount } from 'enzyme';

import { DateInterval, format, IInterval } from './DateInterval';

// мок формирует простейшее представление времени, без ведущих нулей
jest.mock('../Time/Time');
// убираем мок, потому что функциональность основана на переводах: там лежат форматы дат
jest.unmock('@yandex-int/i18n');

// в тестах expected значения в английской локали с русским форматированием:
// - 1 января - это русские локаль и форматирование
// - January 1 - это английские локаль и форматирование
// - 1 January - такие expected значения в тестах, в проде такого нет
//
// Форматирование и локаль смешаны, потому что для тестов у luxon выставлена дефолтная локаль "en",
// иначе появляются неконсистентности в зависимости от окружения,
// а форматирование основано на BEM_LANG, который для тестов всегда "ru"
describe.each(['ru', 'en'] as Array<typeof process.env.BEM_LANG>)('Date intervals formatting - %s', lang => {
    beforeEach(() => {
        require('@yandex-int/i18n').setI18nLang(lang);
    });

    let year: number;

    it('Empty interval', () => {
        expect(format({})).toBe('');
    });

    it('Start only', () => {
        const date = new Date(2000, 0, 1);
        expect(format({ start: date })).toBe(format({ start: date, end: date }));
    });

    describe('Current year', () => {
        beforeAll(() => {
            year = new Date().getFullYear();
        });

        describe('Without time', () => {
            it('Same day', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 0, 1);

                expect(format({ start, end })).toEqual({
                    ru: '1 January',
                    en: 'January 1',
                }[lang]);
            });

            it('Same month', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 0, 14);

                expect(format({ start, end })).toEqual({
                    ru: 'с 1 по 14 January',
                    en: 'from January 1 to 14',
                }[lang]);
            });

            it('Same year', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 1, 1);

                expect(format({ start, end })).toEqual({
                    ru: 'с 1 January по 1 February',
                    en: 'from January 1 to February 1',
                }[lang]);
            });

            it('Same day, forcing full format', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 0, 1);

                expect(format({ start, end, forceFullDate: true })).toEqual({
                    ru: `с 1 January ${year} по 1 January ${year}`,
                    en: `from January 1, ${year} to January 1, ${year}`,
                }[lang]);
            });
        });

        describe('With time', () => {
            it('Same day', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 0, 1, 15);

                expect(format({ start, end, includeTime: true })).toEqual({
                    ru: '1 January с 12:0 по 15:0',
                    en: 'January 1 from 12:0 to 15:0',
                }[lang]);
            });

            it('Same month', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 0, 14, 15);

                expect(format({ start, end, includeTime: true })).toEqual({
                    ru: 'с 1 January 12:0 по 14 January 15:0',
                    en: 'from January 1 12:0 to January 14 15:0',
                }[lang]);
            });

            it('Same year', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 1, 1, 15);

                expect(format({ start, end, includeTime: true })).toEqual({
                    ru: 'с 1 January 12:0 по 1 February 15:0',
                    en: 'from January 1 12:0 to February 1 15:0',
                }[lang]);
            });

            it('Same day, forcing full format', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 0, 1, 15);

                expect(format({
                    start,
                    end,
                    includeTime: true,
                    forceFullDate: true,
                })).toEqual({
                    ru: `с 1 January ${year} 12:0 по 1 January ${year} 15:0`,
                    en: `from January 1, ${year} 12:0 to January 1, ${year} 15:0`,
                }[lang]);
            });
        });
    });

    describe('Another year', () => {
        beforeAll(() => {
            year = 2010;
        });

        describe('Without time', () => {
            it('Same day', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 0, 1);

                expect(format({ start, end })).toEqual({
                    ru: '1 January 2010',
                    en: 'January 1, 2010',
                }[lang]);
            });

            it('Same month', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 0, 14);

                expect(format({ start, end })).toEqual({
                    ru: 'с 1 по 14 January 2010',
                    en: 'from January 1 to 14, 2010',
                }[lang]);
            });

            it('Same year', () => {
                const start = new Date(year, 0, 1);
                const end = new Date(year, 1, 1);

                expect(format({ start, end })).toEqual({
                    ru: 'с 1 January по 1 February 2010',
                    en: 'from January 1 to February 1, 2010',
                }[lang]);
            });

            it('Different years', () => {
                const start = new Date(year, 11, 1);
                const end = new Date(year + 1, 1, 1);

                expect(format({ start, end })).toEqual({
                    ru: 'с 1 December 2010 по 1 February 2011',
                    en: 'from December 1, 2010 to February 1, 2011',
                }[lang]);
            });
        });

        describe('With time', () => {
            it('Same day', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 0, 1, 15);

                expect(format({ start, end, includeTime: true })).toEqual({
                    ru: '1 January 2010 с 12:0 по 15:0',
                    en: 'January 1, 2010 from 12:0 to 15:0',
                }[lang]);
            });

            it('Same month', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 0, 14, 15);

                expect(format({
                    start,
                    end,
                    includeTime: true,
                })).toEqual({
                    ru: 'с 1 January 2010 12:0 по 14 January 2010 15:0',
                    en: 'from January 1, 2010 12:0 to January 14, 2010 15:0',
                }[lang]);
            });

            it('Same year', () => {
                const start = new Date(year, 0, 1, 12);
                const end = new Date(year, 1, 1, 15);

                expect(format({
                    start,
                    end,
                    includeTime: true,
                })).toEqual({
                    ru: 'с 1 January 2010 12:0 по 1 February 2010 15:0',
                    en: 'from January 1, 2010 12:0 to February 1, 2010 15:0',
                }[lang]);
            });

            it('Different years', () => {
                const start = new Date(year, 11, 1, 12);
                const end = new Date(year + 1, 1, 1, 15);

                expect(format({
                    start,
                    end,
                    includeTime: true,
                })).toEqual({
                    ru: 'с 1 December 2010 12:0 по 1 February 2011 15:0',
                    en: 'from December 1, 2010 12:0 to February 1, 2011 15:0',
                }[lang]);
            });
        });
    });

    describe('With different syntax', () => {
        it('With dash between numbers', () => {
            const start = new Date(2000, 0, 1);
            const end = new Date(2000, 0, 4);

            expect(format({ start, end, interval: IInterval.dash })).toEqual({
                ru: '1—4 January 2000',
                en: 'January 1—4, 2000',
            }[lang]);
        });

        it('With dash between words', () => {
            const start = new Date(2000, 0, 1);
            const end = new Date(2000, 1, 1);

            expect(format({ start, end, interval: IInterval.dash })).toEqual({
                ru: '1 January — 1 February 2000',
                en: 'January 1 — February 1, 2000',
            }[lang]);
        });

        it('With prepositions', () => {
            const start = new Date(2000, 0, 1);
            const end = new Date(2001, 0, 1);

            expect(format({ start, end, interval: IInterval.words })).toEqual({
                ru: 'с 1 January 2000 по 1 January 2001',
                en: 'from January 1, 2000 to January 1, 2001',
            }[lang]);
        });
    });

    describe('End date nuances', () => {
        it('Should opt in to not include the last day, if it is 00:00 of that day', () => {
            const start = new Date(2000, 0, 1);
            // с 00:00 1 числа до 00:00 8 числа — это «с 1 по 7»
            const end = new Date(2000, 0, 8);

            expect(format({ start, end, interval: IInterval.words, endMidnightAsPrevDay: true }))
                .toBe({
                    ru: 'с 1 по 7 January 2000',
                    en: 'from January 1 to 7, 2000',
                }[lang]);
        });

        it('Should always include the last day, if it is not 00:00 of that day', () => {
            const start = new Date(2000, 0, 1);
            // если конечная точка захватывает последние сутки даже на миллисекунду, то писать уже надо «с 1 по 8»
            const end = new Date(2000, 0, 8, 0, 0, 0, 1);

            expect(format({ start, end, interval: IInterval.words, endMidnightAsPrevDay: true }))
                .toBe({
                    ru: 'с 1 по 8 January 2000',
                    en: 'from January 1 to 8, 2000',
                }[lang]);
        });

        it('Should always include the last day for intervals with time displayed', () => {
            const start = new Date(2000, 0, 1);
            // если отображается время, то интервал однозначен, надо отобразить то, что передано
            const end = new Date(2000, 0, 8);

            expect(format({ start, end, interval: IInterval.words, includeTime: true, endMidnightAsPrevDay: true }))
                .toBe({
                    ru: 'с 1 January 2000 0:0 по 8 January 2000 0:0',
                    en: 'from January 1, 2000 0:0 to January 8, 2000 0:0',
                }[lang]);
        });
    });
});

describe.each(['ru', 'en'] as Array<typeof process.env.BEM_LANG>)('Date interval component - %s', lang => {
    beforeEach(() => {
        require('@yandex-int/i18n').setI18nLang(lang);
    });

    it('Should render DateInterval', () => {
        const wrapper = mount(
            <DateInterval
                start={new Date(Date.UTC(2000, 0, 1))}
                end={new Date(Date.UTC(2000, 1, 1))}
            />,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });
});
