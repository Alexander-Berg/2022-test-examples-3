import * as parseImpl from '../parseImpl';
import * as build from '../build';

jest.mock('../build');

const timezone = 'Asia/Yekaterinburg';

// язык пользовательского интерфейса
const language = 'uk';

// язык, используемый парсером: украинцы могут вводить текст в календаре как на русском, так и на ураинском языках.
const currentLanguage = 'ru';

const time = {
    now: 1455662700000, // 2016-02-17T03:45:00+05:00
    timezone,
};

// Намеренно используем не bool-значение, чтобы проверить, что оно корректно передается в вызов buildSpecial()
const forceFormat = 'force_format';

const expectedValue = {
    date: '2016-02-16',
};

describe('parseImpl', () => {
    describe('parseDate', () => {
        describe('month number', () => {
            it('01.01', () => {
                const text = '01.01';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2017-01-01', {
                    text,
                    hint: text,
                    language,
                    forceFormat,
                });
            });

            it('29-02', () => {
                const text = '29.02';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-02-29', {
                    text,
                    hint: text,
                    language,
                    forceFormat,
                });
            });

            it('31/12', () => {
                const text = '31/12';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-12-31', {
                    text,
                    hint: text,
                    language,
                    forceFormat,
                });
            });

            it('31.02', () => {
                const text = '31.02';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('01.15', () => {
                const text = '01.15';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('00.03', () => {
                const text = '00.03';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('10.00', () => {
                const text = '10.00';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });
        });

        describe('month name', () => {
            it('1 января', () => {
                const text = '01 янва';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2017-01-01', {
                    text,
                    hint: '1 января',
                    language,
                    forceFormat,
                });
            });

            it('29 февраля', () => {
                const text = '29 фев';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-02-29', {
                    text,
                    hint: '29 февраля',
                    language,
                    forceFormat,
                });
            });

            it('31 декабря', () => {
                const text = '31 дека';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-12-31', {
                    text,
                    hint: '31 декабря',
                    language,
                    forceFormat,
                });
            });

            it('12марта (без пробела)', () => {
                const text = '12мар';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-03-12', {
                    text,
                    hint: '12 марта',
                    language,
                    forceFormat,
                });
            });

            it('31 февраля', () => {
                const text = '31 февра';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('1 феееевраля', () => {
                const text = '01 феееевра';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('0 марта', () => {
                const text = '0 мар';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('1 января 2017', () => {
                const text = '1 января 2017';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2017-01-01', {
                    text,
                    hint: '1 января',
                    language,
                    forceFormat,
                });
            });

            it('29 февраля 2020', () => {
                const text = '29 февраля';
                const expected = '2020-02-22';
                const timeNow = {
                    now: 1573631466220, // 2019-11-13
                    timezone: 'Asia/Yekaterinburg',
                };

                build.buildDate.mockReturnValue(expected);

                expect(
                    parseImpl.parseDate(
                        text,
                        timeNow,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expected);

                expect(build.buildDate).toBeCalledWith('2020-02-29', {
                    text,
                    hint: text,
                    language,
                    forceFormat,
                });
            });
        });

        describe('no month', () => {
            it('1', () => {
                const text = '1';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-02-17', {
                    text,
                    hint: '17 февраля',
                    language,
                    forceFormat,
                });
            });

            it('2', () => {
                const text = '2';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-02-20', {
                    text,
                    hint: '20 февраля',
                    language,
                    forceFormat,
                });
            });

            it('3', () => {
                const text = '3';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-03-03', {
                    text,
                    hint: '3 марта',
                    language,
                    forceFormat,
                });
            });

            it('29', () => {
                const text = '29';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-02-29', {
                    text,
                    hint: '29 февраля',
                    language,
                    forceFormat,
                });
            });

            it('31', () => {
                const text = '31';

                build.buildDate.mockReturnValue(expectedValue);

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBe(expectedValue);

                expect(build.buildDate).toBeCalledWith('2016-03-31', {
                    text,
                    hint: '31 марта',
                    language,
                    forceFormat,
                });
            });

            it('32', () => {
                const text = '32';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });

            it('0', () => {
                const text = '0';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });
        });

        describe('invalid text', () => {
            it('qwerty', () => {
                const text = 'qwerty';

                expect(
                    parseImpl.parseDate(
                        text,
                        time,
                        language,
                        currentLanguage,
                        forceFormat,
                    ),
                ).toBeUndefined();
            });
        });
    });
});
