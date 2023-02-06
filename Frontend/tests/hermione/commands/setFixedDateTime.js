/**
 * Установить фиксированное время в браузере.
 *
 * @param {object} params
 * @param {number} params.year
 * @param {number} params.month
 * @param {number} params.day
 * @param {number} params.hour
 * @param {number} params.min
 * @param {number} params.sec
 */

module.exports = function setFixedDateTime(params = {}) {
    const {
        year = 2019,
        month = 2,
        day = 24,
        hour = 0,
        min = 0,
        sec = 0,
    } = params;

    return this.execute(date => {
        window.MockDate.set(date);
    }, new Date(Date.UTC(year, month, day, hour, min, sec)));
};
