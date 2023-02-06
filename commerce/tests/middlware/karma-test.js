const sinon = require('sinon');
const { expect } = require('chai');
const catchError = require('tests/helpers/catchError').generator;
const checkKarma = require('middleware/karma');

describe('Check karma middleware', () => {
    it('should call `next` when user is not authorized', function *() {
        const context = { state: { user: {} } };
        const spy = sinon.spy();

        yield checkKarma.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should call `next` when user is not spammer', function *() {
        const context = { state: { user: {
            karma: { value: 0 },
            uid: { value: 1234 }
        } } };
        const spy = sinon.spy();

        yield checkKarma.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should throw error when user is spammer', function *() {
        const context = { state: { user: {
            karma: { value: 85 },
            uid: { value: 12345 }
        } } };
        const error = yield catchError(checkKarma.bind(context, {}));

        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('User is spammer');
        expect(error.options).to.deep.equal({
            internalCode: '403_UIS',
            uid: 12345
        });
    });
});
