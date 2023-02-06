const Cookies = require('../../core/utils/cookies');

describe('Класс Cookies', () => {
    const reportContext = {
        appendResponseHeader: jest.fn(),
    };

    test('Передает установленные cookie в котекст RR', () => {
        const cookies = new Cookies(reportContext);
        const expires = new Date().toUTCString();

        cookies.set({
            name: 'first',
            value: 'first_value',
            expires,
            domain: 'example.com',
            path: '/',
            isSecure: true,
            isHttpOnly: true,
        });

        cookies.set({
            name: 'second',
            value: 'second_value',
            expires,
            domain: 'another.example.com',
            path: '/path',
            isSecure: false,
            isHttpOnly: false,
        });

        cookies.send();

        expect(reportContext.appendResponseHeader).toBeCalledTimes(2);
        expect(reportContext.appendResponseHeader)
            .toHaveBeenNthCalledWith(1, 'Set-Cookie', `first=first_value; expires=${expires}; domain=example.com; path=/; SameSite=none; Secure; HttpOnly;`);
        expect(reportContext.appendResponseHeader)
            .toHaveBeenNthCalledWith(2, 'Set-Cookie', `second=second_value; expires=${expires}; domain=another.example.com; path=/path; SameSite=none;`);
    });
});
