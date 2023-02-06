const dateToString = require('../../lib/date/date-to-string');
const expect = require('expect');

describe('date/date-to-string', () => {
    // Сдвиг (в милисекундах) времени на GMT+0 относительно нашего.
    // Нужен, чтобы считать выводить форматированное время на GMT+0,
    // иначе на компах в разных часовых поясах будут разные результаты тестов.
    const TIMEZONE_OFFSET_MS = (new Date()).getTimezoneOffset() * 60000;

    const defaultMonthsNominative = ['январь', 'февраль', 'март', 'апрель', 'май', 'июнь',
        'июль', 'август', 'сентябрь', 'октябрь', 'ноябрь', 'декабрь'];
    const defaultMonthsGenitive = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
        'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
    const defaultWeekDays = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];

    const runTest = (dateOrMs, format, expectedResult, monthsNominative, monthsGenitive, weekDays) => {
        const formatStr = format ? `and "${format}" format ` : '';
        const testName = `${dateOrMs} ms ${formatStr}should return "${expectedResult}"`;

        // TIMEZONE_OFFSET_MS - поправка на часовой пояс (выводим время на GMT+0)
        const d = dateOrMs instanceof Date ? dateOrMs : dateOrMs + TIMEZONE_OFFSET_MS;
        it(testName, () => {
            expect(dateToString(
                d,
                format,
                monthsNominative,
                monthsGenitive,
                weekDays
            )).toEqual(expectedResult);
        });
    };

    const SECOND = 1000;
    const MINUTE = SECOND * 60;
    const HOUR = MINUTE * 60;
    const DAY = HOUR * 24;

    runTest(new Date(1945, 4, 9, 0, 43, 15), undefined, '09.05.1945 00:43');
    runTest(new Date(2016, 8, 7, 6, 0, 4), 'M/D/YY h:m:s', '9/7/16 6:0:4');

    runTest(0, undefined, '01.01.1970 00:00');
    runTest(1488374400105, undefined, '01.03.2017 13:20');
    runTest(1488374400105, 'YYYYMMDD', '20170301');

    const curMs = 1488379495903; // '2017-03-01 14:44:55.903'
    runTest(curMs, undefined, '01.03.2017 14:44');
    runTest(curMs, 'YYYY-MM-DD HH:mm:ss', '2017-03-01 14:44:55');

    runTest(curMs, 'M', '3');
    runTest(curMs, 'MM', '03');
    runTest(curMs, 'MMMM', defaultMonthsNominative[2], defaultMonthsNominative);
    runTest(curMs, 'mmmm', defaultMonthsGenitive[2], undefined, defaultMonthsGenitive);

    it('"MMMM" format should throw if monthsNominative is not defined', () => {
        expect(() => dateToString(curMs, 'MMMM'))
            .toThrow('helpers/date/date-to-string::monthsNominative must be defined for MMMM format');
    });
    it('"mmmm" format should throw if monthsGenitive is not defined', () => {
        expect(() => dateToString(curMs, 'mmmm'))
            .toThrow('helpers/date/date-to-string::monthsGenitive must be defined for mmmm format');
    });

    const february = curMs - DAY;
    runTest(february, 'MMMM', defaultMonthsNominative[1], defaultMonthsNominative);

    const september = curMs + 185 * DAY;
    runTest(september, 'mmmm', defaultMonthsGenitive[8], undefined, defaultMonthsGenitive);

    runTest(curMs, 'D', '1');
    runTest(curMs, 'DD', '01');
    runTest(curMs, 'YY', '17');
    runTest(curMs, 'YYYY', '2017');
    runTest(curMs, 'H', '14');
    runTest(curMs, 'HH', '14');
    runTest(curMs, 'h', '2');
    runTest(curMs, 'p', 'pm');

    const hour12 = curMs - 2 * HOUR;
    const hour11 = curMs - 3 * HOUR;
    const hour9 = curMs - 5 * HOUR;
    const hour0 = curMs - 14 * HOUR;

    runTest(hour9, 'H', '9');
    runTest(hour9, 'HH', '09');

    runTest(hour12, 'h', '12');
    runTest(hour11, 'h', '11');
    runTest(hour9, 'h', '9');
    runTest(hour0, 'h', '12');

    runTest(hour12, 'p', 'pm');
    runTest(hour11, 'p', 'am');
    runTest(hour0, 'p', 'am');

    runTest(curMs, 'm', '44');
    runTest(curMs, 'mm', '44');

    const minute4 = curMs - 40 * MINUTE;
    runTest(minute4, 'm', '4');

    runTest(curMs, 's', '55');
    runTest(curMs, 'ss', '55');

    const second0 = curMs + 5 * SECOND;
    runTest(second0, 's', '0');

    runTest(
        curMs,
        // eslint-disable-next-line max-len
        'Московская дата D mmmm YYYY года', 'Московская дата 1 марта 2017 года',
        undefined,
        defaultMonthsGenitive
    );
    runTest(curMs, 'Московское время H часов m минут s секунд', 'Московское время 14 часов 44 минут 55 секунд');

    const someDate = Number(new Date(1945, 4, 9, 0, 43, 1, 567)) - TIMEZONE_OFFSET_MS;
    runTest(someDate, 'hp', '12am');
    runTest(someDate, 'h:m:sp', '12:43:1am');
    runTest(someDate, 'h:mm:ssp', '12:43:01am');
    runTest(someDate, 'MM/DD/YYYY', '05/09/1945');
    runTest(someDate, 'M/D/YY', '5/9/45');

    const someDecemberDay = Number(new Date(2100, 11, 2));
    runTest(someDecemberDay, 'За окном то MMMM!', 'За окном то декабрь!', defaultMonthsNominative);
    const someJulyDay = Number(new Date(5000, 6, 30));
    runTest(someJulyDay, 'Как дождаться бы mmmm?', 'Как дождаться бы июля?', undefined, defaultMonthsGenitive);

    runTest(someDate, 'Некорректный шаблон: DD.MMM.YYY', 'Некорректный шаблон: 09.MMM.YYY');

    it('"E" format should throw if weekDays is not defined', () => {
        expect(() => dateToString(curMs, 'E'))
            .toThrow('helpers/date/date-to-string::weekDays must be defined for E format');
    });
    runTest(someDate, 'E', 'Ср', undefined, undefined, defaultWeekDays);
    runTest(1544347783525, 'E', 'Вс', undefined, undefined, defaultWeekDays);
});
