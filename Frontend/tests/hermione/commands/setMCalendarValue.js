/**
 * Выбор значения в m-calendar
 * @param {Object} options
 * @param {String} options.block - селектор контрола календаря
 * @param {Object} options.calendar - PO для попапа календаря
 * @param {String} options.date - дата dd-mm-yyyy
 * @returns {Object}
 */

module.exports = function setMCalendarValue(options) {
    const {
        block,
        calendar,
        date,
    } = options;

    const [
        day,
        month,
        year,
    ] = date.split('-');

    const monthSelector = `${calendar.datepicker.months.item()} .radio-button__control[value="${Number(month) - 1}"]`;

    return this
        .disableAnimations('.popup2')
        .disableAnimations('.m-datepicker__great-chooser')
        .click(`${block} .textinput__box`)
        .waitForVisible(calendar())
        .click(calendar.datepicker.title())
        .waitForVisible(calendar.datepicker.year())
        .setValue(calendar.datepicker.year.input(), year)
        .click(monthSelector)
        .click(calendar.datepicker.ok.btn())
        .waitForVisible(calendar.datepicker.dates())
        .click(`${calendar()} .m-datepicker__day[data-content="${Number(day)}"]`)
        .waitForHidden(calendar());
};
