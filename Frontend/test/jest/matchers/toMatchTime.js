import moment from 'moment';

/**
 * Сравнивает время двух дат
 * @memberof jest.Expect
 * @instance
 *
 * @param {moment.Moment | Date} received
 * @param {moment.Moment | Date} expected
 *
 * @returns {jest.CustomMatcherResult}
 */
export default function toMatchTime(received, expected) {
    const pass = moment(received).format('HHmm') === moment(expected).format('HHmm');

    let message;

    if (pass) {
        message = () => `expected '${String(received)}' time not to match '${String(expected)}'`;
    } else {
        message = () => `expected '${String(received)}' time to match '${String(expected)}'`;
    }

    return { pass, message };
}
