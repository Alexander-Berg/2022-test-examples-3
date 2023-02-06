const dynamicDateFormat = require('../../lib/date/dynamic-date-format');
const expect = require('expect');

describe('date/dynamic-date-format', () => {
    // Сдвиг (в милисекундах) времени на GMT+0 относительно нашего.
    // Нужен, чтобы считать выводить форматированное время на GMT+0,
    // иначе на компах в разных часовых поясах будут разные результаты тестов.
    const TIMEZONE_OFFSET_MS = (new Date()).getTimezoneOffset() * 60000;

    const mockedNow = 1544552479004 + TIMEZONE_OFFSET_MS;
    const defaultWeekDays = ['Вс', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];

    const runTest = (date, testPrefix, expectedResult, weekDays = defaultWeekDays) => {
        const testName = `${testPrefix} should return "${expectedResult}"`;

        // TIMEZONE_OFFSET_MS - поправка на часовой пояс (выводим время на GMT+0)
        it(testName, () => {
            expect(dynamicDateFormat(
                date,
                weekDays,
                mockedNow
            )).toEqual(expectedResult);
        });
    };

    runTest(new Date(1945, 4, 9, 0, 43, 15), 'день победы', '09.05.45');

    runTest(new Date(0), 'начало эры', '01.01.70');

    runTest(new Date(mockedNow), '"сейчас"', '18:21');

    let date = new Date(mockedNow);
    date.setMinutes(date.getMinutes() - 5);
    runTest(date, '5 минут назад', '18:16');

    date = new Date(mockedNow);
    date.setHours(date.getHours() - 8);
    runTest(date, '8 часов назад', '10:21');

    date = new Date(mockedNow);
    date.setDate(date.getDate() - 1);
    runTest(date, 'вчера', 'Пн');

    date = new Date(mockedNow);
    date.setDate(date.getDate() - 2);
    runTest(date, 'позавчера', 'Вс');

    date = new Date(mockedNow);
    date.setDate(date.getDate() - 4);
    runTest(date, '4 дня назад', 'Пт');

    date = new Date(mockedNow);
    date.setDate(date.getDate() - 6);
    runTest(date, '6 дней назад', 'Ср');

    date = new Date(mockedNow);
    date.setDate(date.getDate() - 7);
    runTest(date, 'неделю назад', '04.12.18');

    date = new Date(mockedNow);
    date.setMonth(date.getMonth() - 1);
    runTest(date, 'месяц назад', '11.11.18');

    date = new Date(mockedNow);
    date.setMonth(date.getMonth() - 6);
    runTest(date, '6 месяцев назад', '11.06.18');

    date = new Date(mockedNow);
    date.setMonth(0);
    date.setDate(1);
    runTest(date, 'январь того же года', '01.01.18');

    date = new Date(mockedNow);
    date.setDate(31);
    date.setFullYear(date.getFullYear() - 1);
    runTest(date, 'конец предыдущего год', '31.12.17');

    date = new Date(mockedNow);
    date.setFullYear(date.getFullYear() - 1);
    runTest(date, 'год назад', '11.12.17');

    date = new Date(mockedNow);
    date.setFullYear(date.getFullYear() - 5);
    runTest(date, '5 лет назад', '11.12.13');

    date = new Date(mockedNow);
    date.setFullYear(date.getFullYear() - 100);
    runTest(date, '100 лет назад', '11.12.18');

    date = new Date(mockedNow);
    date.setHours(23);
    date.setMinutes(59);
    date.setSeconds(59);
    runTest(date, 'в будущем, но сегодня)', '23:59');

    date = new Date(mockedNow);
    date.setDate(date.getDate() + 1);
    runTest(date, 'завтра', '12.12.18');

    date = new Date(mockedNow);
    date.setMonth(date.getMonth() + 1);
    runTest(date, 'через месяц', '11.01.19');

    date = new Date(mockedNow);
    date.setFullYear(date.getFullYear() + 1);
    runTest(date, 'через год', '11.12.19');

    runTest(new Date(2045, 8, 15, 12), 'сильно в будущем', '15.09.45');
});
