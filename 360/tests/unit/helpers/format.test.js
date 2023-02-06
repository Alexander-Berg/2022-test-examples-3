import _ from 'lodash';
import formatHelper from '../../../components/helpers/format';

describe('formatHelper', () => {
    describe('Форматеры:', () => {
        describe('форматер интервалов (`numberToInterval`)', () => {
            let interval;
            beforeEach(() => {
                interval = [0, 20, 30, 40, 50];
            });
            afterEach(() => {
                interval = null;
            });

            describe('определяя значение внутри интервалов', () => {
                it('должен вернуть 1 - 20, если значение 10', () => {
                    expect(formatHelper.numberToInterval(10, interval)).toBe('1 - 20');
                });
                it('должен вернуть 31 - 40, если значение 40', () => {
                    expect(formatHelper.numberToInterval(40, interval)).toBe('31 - 40');
                });
            });

            describe('определяя значение вне интервалов', () => {
                it('должен вернуть <= 0, если значение -100', () => {
                    expect(formatHelper.numberToInterval(-100, interval)).toBe('<= 0');
                });
                it('должен вернуть > 50, если значение 100', () => {
                    expect(formatHelper.numberToInterval(100, interval)).toBe('> 50');
                });
            });

            describe('определяя значения на границе интервалов', () => {
                it('должен вернуть <= 0, если значение 0', () => {
                    expect(formatHelper.numberToInterval(0, interval)).toBe('<= 0');
                });
                it('должен вернуть 41 - 50, если значение 50', () => {
                    expect(formatHelper.numberToInterval(50, interval)).toBe('41 - 50');
                });
                it('должен вернуть 31 - 40, если значение 40', () => {
                    expect(formatHelper.numberToInterval(40, interval)).toBe('31 - 40');
                });
            });
        });

        describe('форматер дат (`timestampToDate`)', () => {
            (() => {
                const date = '2015-06-02T14:15:16';
                const timestamp = Math.floor(Date.parse(date) / 1000);
                [
                    ['D.M.YY', '2.6.15'],
                    ['DD.MM.YYYY', '02.06.2015'],
                    ['H:m:s', '14:15:16'],
                    ['HH:mm:ss', '14:15:16'],
                    ['h:mm p', '2:15 pm'],
                    ['D mmmm', '2 июня'],
                    ['MMMM YYYY', 'Июнь 2015']
                ].forEach((test) => {
                    it(`[${date}] формат [${test[0]}] должен вернуть "${test[1]}"`, () => {
                        expect(formatHelper.timestampToDate(timestamp, test[0])).toBe(test[1]);
                    });
                });
            })();

            (() => {
                const date = '2015-12-25T00:00:00';
                const timestamp = Math.floor(Date.parse(date) / 1000);
                [
                    ['D.M.YY', '25.12.15'],
                    ['DD.MM.YYYY', '25.12.2015'],
                    ['H:m:s', '0:0:0'],
                    ['HH:mm:ss', '00:00:00'],
                    ['h:mm p', '12:00 am'],
                    ['D mmmm', '25 декабря'],
                    ['MMMM YYYY', 'Декабрь 2015']
                ].forEach((test) => {
                    it(`[${date}] формат [${test[0]}] должен вернуть "${test[1]}"`, () => {
                        expect(formatHelper.timestampToDate(timestamp, test[0])).toBe(test[1]);
                    });
                });
            })();

            (() => {
                const date = '2015-12-25T01:00:00';
                const timestamp = Math.floor(Date.parse(date) / 1000);
                [
                    ['h:mm p', '1:00 am']
                ].forEach((test) => {
                    it(`[${date}] формат [${test[0]}] должен вернуть "${test[1]}"`, () => {
                        expect(formatHelper.timestampToDate(timestamp, test[0])).toBe(test[1]);
                    });
                });
            })();

            (() => {
                const date = '2015-12-25T12:00:00';
                const timestamp = Math.floor(Date.parse(date) / 1000);
                [
                    ['h:mm p', '12:00 pm']
                ].forEach((test) => {
                    it(`[${date}] формат [${test[0]}] должен вернуть "${test[1]}"`, () => {
                        expect(formatHelper.timestampToDate(timestamp, test[0])).toBe(test[1]);
                    });
                });
            })();

            (() => {
                const date = '2015-12-25T13:00:00';
                const timestamp = Math.floor(Date.parse(date) / 1000);
                [
                    ['h:mm p', '1:00 pm']
                ].forEach((test) => {
                    it(`[${date}] формат [${test[0]}] должен вернуть "${test[1]}"`, () => {
                        expect(formatHelper.timestampToDate(timestamp, test[0])).toBe(test[1]);
                    });
                });
            })();
        });

        describe('форматер дат (`timestampToDate`) с кастомной локализацией', () => {
            let format;
            beforeEach(() => {
                const timestampToDate = formatHelper.create(
                    _.range(1, 13).map((x) => `(${x})`),
                    _.range(1, 13).map((x) => `[${x}]`)
                ).timestampToDate;
                format = function(timestamp, format) {
                    return timestampToDate(timestamp, format);
                };
            });

            afterEach(() => {
                format = null;
            });

            const date = '2015-06-02T14:15:16';
            const timestamp = Math.floor(Date.parse(date) / 1000);
            [
                ['D mmmm', '2 [6]'],
                ['MMMM YYYY', '(6) 2015']
            ].forEach((test) => {
                it(`[${date}] формат [${test[0]}] должен вернуть "${test[1]}"`, () => {
                    expect(format(timestamp, test[0])).toBe(test[1]);
                });
            });
        });
    });
});
