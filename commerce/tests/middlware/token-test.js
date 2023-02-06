const sinon = require('sinon');
const { expect } = require('chai');
const catchError = require('tests/helpers/catchError').generator;
const tokenMiddleware = require('middleware/token');

describe('Token middleware', () => {
    it('should call `next` in success case', function *() {
        const context = { header: { 'auth-token': '1234567890asdfgh' } };
        const spy = sinon.spy();

        yield tokenMiddleware.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should error when auth token is not valid', function *() {
        const context = { header: { 'auth-token': 'invalid' } };
        const error = yield catchError(tokenMiddleware.bind(context, {}));

        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('Auth token is not valid');
        expect(error.options).to.deep.equal({ internalCode: '403_TNV' });
    });
});
