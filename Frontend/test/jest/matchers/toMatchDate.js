import moment from 'moment';
import { DateTime } from 'luxon';

/**
 * Сравнивает две даты
 * @memberof jest.Expect
 * @instance
 *
 * @param {moment.Moment | luxon.DateTime | Date} received
 * @param {moment.Moment | luxon.DateTime | Date} expected
 *
 * @returns {jest.CustomMatcherResult}
 */
export default function toMatchDate(received, expected) {
    let pass;

    if (received instanceof DateTime) {
        if (expected instanceof Date) {
            pass = received.hasSame(DateTime.fromJSDate(expected), 'second');
        } else {
            pass = received.hasSame(expected, 'second');
        }
    } else {
        pass = moment(received).isSame(expected);
    }

    let message;

    if (pass) {
        message = () => `expected '${String(received)}' not to match '${String(expected)}'`;
    } else {
        message = () => `expected '${String(received)}' to match '${String(expected)}'`;
    }

    return { pass, message };
}
