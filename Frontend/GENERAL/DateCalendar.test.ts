import { format } from './DateCalendar';
import i18n from '../../../shared/lib/i18n';
import * as en from '../../../langs/yamb/en.json';
import * as ru from '../../../langs/yamb/ru.json';

/*        01.2018
    ПН ВТ СР ЧТ ПТ СБ ВС
    1  2  3  4  5  6  7
    8  9  10 11 12 13 14
    15 16 17 18 19 20 21
    22 23 24 25 26 27 28
    29 30 31
 */

describe('calendar', () => {
    describe('humanize', () => {
        describe('lang en', () => {
            beforeAll(() => {
                i18n.locale('en', en);
            });

            it('Больше недели показываются в полном формате', () => {
                expect(format(
                    new Date(1990, 10, 1, 6, 0, 0),
                    new Date(2018, 0, 1, 0, 0, 0),
                )).toBe('01/11/1990');

                expect(format(
                    new Date(2018, 0, 1, 1, 0, 0),
                    new Date(2018, 0, 10, 0, 0, 0),
                )).toBe('01/01/2018');

                expect(format(
                    new Date(2017, 0, 1, 1, 0, 0),
                    new Date(2018, 0, 1, 1, 0, 0),
                )).toBe('01/01/2017');

                expect(format(
                    new Date(2018, 0, -1, 2, 0, 0),
                    new Date(2018, 0, 10, 0, 0, 0),
                )).toBe('30/12/2017');

                expect(format(
                    new Date(2018, 0, 8, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('08/01/2018');
            });

            it('В рамках недели до показываются в формате последнего дня недели + время', () => {
                expect(format(
                    new Date(2018, 0, 9, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Last Tuesday at 09:00');

                expect(format(
                    new Date(2018, 0, 10, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Last Wednesday at 09:00');

                expect(format(
                    new Date(2018, 0, 11, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Last Thursday at 09:00');

                expect(format(
                    new Date(2018, 0, 12, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Last Friday at 09:00');

                expect(format(
                    new Date(2018, 0, 13, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Last Saturday at 09:00');
            });

            it('±1 день ключевого слова + время', () => {
                expect(format(
                    new Date(2018, 0, 14, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Yesterday at 09:00');

                expect(format(
                    new Date(2018, 0, 15, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Today at 09:00');

                expect(format(
                    new Date(2018, 0, 16, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Tomorrow at 09:00');
            });

            it('Даты в рамках недели после показываются в формате дня недели + время', () => {
                expect(format(
                    new Date(2018, 0, 17, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('On Wednesday at 09:00');

                expect(format(
                    new Date(2018, 0, 18, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('On Thursday at 09:00');

                expect(format(
                    new Date(2018, 0, 19, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('On Friday at 09:00');

                expect(format(
                    new Date(2018, 0, 20, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('On Saturday at 09:00');

                expect(format(
                    new Date(2018, 0, 21, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('On Sunday at 09:00');
            });

            it('Больше недели - полный формат', () => {
                expect(format(
                    new Date(2018, 0, 22, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('22/01/2018');

                expect(format(
                    new Date(2018, 0, 23, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('23/01/2018');

                expect(format(
                    new Date(2018, 0, 24, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('24/01/2018');
            });
        });

        describe('lang ru', () => {
            beforeAll(() => {
                i18n.locale('ru', ru);
            });

            it('Больше недели показываются в полном формате', () => {
                expect(format(
                    new Date(1990, 10, 1, 6, 0, 0),
                    new Date(2018, 0, 1, 0, 0, 0),
                )).toBe('01.11.1990');

                expect(format(
                    new Date(2018, 0, 1, 1, 0, 0),
                    new Date(2018, 0, 10, 0, 0, 0),
                )).toBe('01.01.2018');

                expect(format(
                    new Date(2017, 0, 1, 1, 0, 0),
                    new Date(2018, 0, 1, 1, 0, 0),
                )).toBe('01.01.2017');

                expect(format(
                    new Date(2018, 0, -1, 2, 0, 0),
                    new Date(2018, 0, 10, 0, 0, 0),
                )).toBe('30.12.2017');

                expect(format(
                    new Date(2018, 0, 8, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('08.01.2018');
            });

            it('В рамках недели до показываются в формате последнего дня недели + время', () => {
                expect(format(
                    new Date(2018, 0, 9, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В прошлый вторник в 09:00');

                expect(format(
                    new Date(2018, 0, 10, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В прошлую среду в 09:00');

                expect(format(
                    new Date(2018, 0, 11, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В прошлый четверг в 09:00');

                expect(format(
                    new Date(2018, 0, 12, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В прошлую пятницу в 09:00');

                expect(format(
                    new Date(2018, 0, 13, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В прошлую субботу в 09:00');
            });

            it('±1 день ключевого слова + время', () => {
                expect(format(
                    new Date(2018, 0, 14, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Вчера в 09:00');

                expect(format(
                    new Date(2018, 0, 15, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Сегодня в 09:00');

                expect(format(
                    new Date(2018, 0, 16, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('Завтра в 09:00');
            });

            it('Даты в рамках недели после показываются в формате дня недели + время', () => {
                expect(format(
                    new Date(2018, 0, 17, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В среду в 09:00');

                expect(format(
                    new Date(2018, 0, 18, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В четверг в 09:00');

                expect(format(
                    new Date(2018, 0, 19, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В пятницу в 09:00');

                expect(format(
                    new Date(2018, 0, 20, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В субботу в 09:00');

                expect(format(
                    new Date(2018, 0, 21, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('В воскресенье в 09:00');
            });

            it('Больше недели - полный формат', () => {
                expect(format(
                    new Date(2018, 0, 22, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('22.01.2018');

                expect(format(
                    new Date(2018, 0, 23, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('23.01.2018');

                expect(format(
                    new Date(2018, 0, 24, 9, 0, 0),
                    new Date(2018, 0, 15, 9, 0, 0),
                )).toBe('24.01.2018');
            });
        });
    });
});
