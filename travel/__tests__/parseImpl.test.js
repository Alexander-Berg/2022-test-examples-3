import {momentTimezone as moment} from '../../../../reexports';

import {parseSpecial, parseWeekday} from '../parseImpl';
import {buildSpecial, buildWeekday} from '../build';
import {getToday} from '../utils';

jest.mock('../build');
jest.mock('../utils');

const timezone = 'Asia/Yekaterinburg';

// язык пользовательского интерфейса
const language = 'uk';

// язык, используемый парсером: украинцы могут вводить текст в календаре как на русском, так и на ураинском языках.
const currentLanguage = 'ru';

const time = {
    now: '2016-02-17T03:45:00+05:00',
    timezone,
};

const today = moment.tz('2016-02-17', timezone);

// Намеренно используем не bool-значение, чтобы проверить, что оно корректно передается в вызов buildSpecial()
const forceFormat = 'force_format';

const expectedValue = {
    date: '2016-02-16',
};

// В тестах предполагаем, что text, hint введены на русском языке, но текущий язык интерфейса - украинский.
describe('parseImpl', () => {
    describe('parseSpecial', () => {
        it('today', () => {
            const text = 'сег';

            buildSpecial.mockReturnValue(expectedValue);

            expect(
                parseSpecial(
                    text,
                    time,
                    language,
                    currentLanguage,
                    forceFormat,
                ),
            ).toBe(expectedValue);

            expect(buildSpecial).toBeCalledWith('today', {
                text,
                hint: 'сегодня',
                time,
                language,
                forceFormat,
            });
        });

        it('yesterday', () => {
            const text = 'вче';

            buildSpecial.mockReturnValue(expectedValue);

            expect(
                parseSpecial(
                    text,
                    time,
                    language,
                    currentLanguage,
                    forceFormat,
                ),
            ).toBe(expectedValue);

            expect(buildSpecial).toBeCalledWith('yesterday', {
                text,
                hint: 'вчера',
                time,
                language,
                forceFormat,
            });
        });

        it('in a week', () => {
            const text = 'через';

            buildSpecial.mockReturnValue(expectedValue);

            expect(
                parseSpecial(
                    text,
                    time,
                    language,
                    currentLanguage,
                    forceFormat,
                ),
            ).toBe(expectedValue);

            expect(buildSpecial).toBeCalledWith('in-a-week', {
                text,
                hint: 'через неделю',
                time,
                language,
                forceFormat,
            });
        });

        it('in a month', () => {
            const text = 'через мес';

            buildSpecial.mockReturnValue(expectedValue);

            expect(
                parseSpecial(
                    text,
                    time,
                    language,
                    currentLanguage,
                    forceFormat,
                ),
            ).toBe(expectedValue);

            expect(buildSpecial).toBeCalledWith('in-a-month', {
                text,
                hint: 'через месяц',
                time,
                language,
                forceFormat,
            });
        });
    });

    describe('parseWeekday', () => {
        beforeEach(() => {
            getToday.mockReturnValue(today);
        });

        describe('thursday', () => {
            const thursdayIndex = 3;

            it('full', () => {
                const text = 'четв';

                buildWeekday.mockReturnValue(expectedValue);

                expect(
                    parseWeekday(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(buildWeekday).toBeCalledWith(thursdayIndex, {
                    text,
                    hint: 'четверг',
                    time,
                    language,
                    forceFormat,
                });
            });

            it('short', () => {
                const text = 'чт';

                buildWeekday.mockReturnValue(expectedValue);

                expect(
                    parseWeekday(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(buildWeekday).toBeCalledWith(thursdayIndex, {
                    text,
                    hint: 'чт',
                    time,
                    language,
                    forceFormat,
                });
            });
        });

        describe('monday', () => {
            const mondayIndex = 0;

            it('full', () => {
                const text = 'понед';

                buildWeekday.mockReturnValue(expectedValue);

                expect(
                    parseWeekday(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(buildWeekday).toBeCalledWith(mondayIndex, {
                    text,
                    hint: 'понедельник',
                    time,
                    language,
                    forceFormat,
                });
            });

            it('short', () => {
                const text = 'пн';

                buildWeekday.mockReturnValue(expectedValue);

                expect(
                    parseWeekday(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(buildWeekday).toBeCalledWith(mondayIndex, {
                    text,
                    hint: 'пн',
                    time,
                    language,
                    forceFormat,
                });
            });

            describe('friday', () => {
                const fridayIndex = 4;

                it('full', () => {
                    // Важная деталь. "Сегодня" - среда, см. const today.
                    // По тексту "п" должна находится Пятница, а не Понедельник -
                    // ближайший следующий день на букву "п", начиная отсчет с "сегодня", то есть со среды.
                    const text = 'п';

                    buildWeekday.mockReturnValue(expectedValue);

                    expect(
                        parseWeekday(
                            text,
                            time,
                            language,
                            currentLanguage,
                            forceFormat,
                        ),
                    ).toBe(expectedValue);

                    expect(buildWeekday).toBeCalledWith(fridayIndex, {
                        text,
                        hint: 'пятница',
                        time,
                        language,
                        forceFormat,
                    });
                });
            });
        });
    });
});
