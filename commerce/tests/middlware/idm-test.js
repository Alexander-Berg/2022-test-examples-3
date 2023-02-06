const idm = require('middleware/idm');
const { expect } = require('chai');

describe('IDM middlware', () => {
    it('should get result from `next` and add `code`', function *() {
        const context = {};
        const next = Promise.resolve({ someKey: 'someValue' });

        yield idm.call(context, next);

        expect(context.body).to.deep.equal({ code: 0, someKey: 'someValue' });
    });

    it('should add error message', function *() {
        const context = {};
        const next = Promise.reject(new Error('Unknown error'));

        yield idm.call(context, next);

        expect(context.body).to.deep.equal({ code: 1, error: 'Unknown error' });
    });

    it('should add fatal message', function *() {
        const context = {};
        const error = new Error('Handled error');

        error.status = 401;
        const next = Promise.reject(error);

        yield idm.call(context, next);

        expect(context.body).to.deep.equal({ code: 401, fatal: 'Handled error' });
    });
});
