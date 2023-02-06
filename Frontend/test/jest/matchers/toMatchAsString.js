/**
 * Сравнивает строку с приведённым к строке объектом
 * @memberof jest.Expect
 * @instance
 *
 * @param {*} received
 * @param {String} expected
 *
 * @returns {jest.CustomMatcherResult}
 */
export default function toMatchAsString(received, expected) {
    const pass = String(received) === expected;

    let message;

    if (pass) {
        message = () => `expected '${String(received)}' not to match '${expected}'`;
    } else {
        message = () => `expected '${String(received)}' to match '${expected}'`;
    }

    return { pass, message };
}
