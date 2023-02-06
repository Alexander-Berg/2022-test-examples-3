const { expect } = require('chai');
const _ = require('lodash');

const telegramUser = require('middleware/telegramUser');

describe('Telegram User test', () => {
    it('should correct forward user when all field are filled', function *() {
        const userData = {
            firstname: 'Vasya',
            lastname: 'Pupkin',
            username: 'vasya'
        };
        const cookies = _.assign(userData, { uid: '123456789' });
        const context = {
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield telegramUser.call(context, {});

        const userState = _.assign(userData, { uid: { value: '123456789' } });

        expect(context.state.user).to.deep.equal(userState);
        expect(context.state.authType).to.deep.equal('telegram');
    });

    it('should return "" in `lastname` and in `username` when these fields are empty', function *() {
        const cookies = {
            uid: '123456789',
            firstname: 'Vasya'
        };
        const context = {
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield telegramUser.call(context, {});

        const userState = {
            uid: { value: '123456789' },
            firstname: 'Vasya',
            lastname: '',
            username: ''
        };

        expect(context.state.user).to.deep.equal(userState);
        expect(context.state.authType).to.deep.equal('telegram');
    });

    it('should correct decode cookies', function *() {
        const cookies = {
            uid: '123456789',
            firstname: '%D0%90%D0%BD%D1%8F',
            lastname: '%D0%91%D0%B0%D0%B6%D0%B5%D0%BD%D0%BE%D0%B2%D0%B0',
            username: 'anyok'
        };
        const context = {
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield telegramUser.call(context, {});

        const userState = {
            uid: { value: '123456789' },
            firstname: 'Аня',
            lastname: 'Баженова',
            username: 'anyok'
        };

        expect(context.state.user).to.deep.equal(userState);
    });

    it('should remove invalid characters', function *() {
        const cookies = {
            uid: '123456789',
            firstname: 'drop database;'
        };
        const context = {
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield telegramUser.call(context, {});

        const userState = {
            uid: { value: '123456789' },
            firstname: 'drop database',
            lastname: '',
            username: ''
        };

        expect(context.state.user).to.deep.equal(userState);
    });

    it('should return `INVALID` when uid or username contain invalid characters', function *() {
        const cookies = {
            uid: '1drop database;1',
            firstname: 'Sergey'
        };
        const context = {
            cookies: { get: key => cookies[key] },
            state: {}
        };

        yield telegramUser.call(context, {});

        const expected = { status: { value: 'INVALID', id: 0 } };

        expect(context.state.user).to.deep.equal(expected);
    });
});
