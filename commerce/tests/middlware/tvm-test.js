const sinon = require('sinon');
const { expect } = require('chai');
const dbHelper = require('tests/helpers/clear');

const { generator: catchError } = require('tests/helpers/catchError');
const nockTvm = require('tests/helpers/nockTvm');
const tvmMiddleware = require('middleware/tvm');
const nock = require('nock');

const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

describe('TVM middleware', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });
    afterEach(nock.cleanAll);

    it('should call `next` when `skipTvmCheck` is true and `force-tvm-check` is false', function *() {
        const tvmRequest = nockTvm.checkTicket({ src: 1234 });
        const context = { header: {} };
        const spy = sinon.spy();

        yield tvmMiddleware.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
        expect(tvmRequest.isDone()).to.be.false;
    });

    it('should throw 400 if `x-ya-service-ticket` header is absent', function *() {
        const context = { header: { 'force-tvm-check': 1 } };
        const error = yield catchError(tvmMiddleware.bind(context, {}));

        expect(error.statusCode).to.equal(400);
        expect(error.message).to.equal('Ticket is not defined');
        expect(error.options).to.deep.equal({ internalCode: '400_TIN' });
    });

    it('should throw 403 if cannot parse tvm ticket', function *() {
        const tvmRequest = nockTvm.checkTicket({}, 400);

        const context = { header: { 'force-tvm-check': 1, 'x-ya-service-ticket': 'ticket' } };
        const error = yield catchError(tvmMiddleware.bind(context, {}));

        tvmRequest.done();
        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('Cannot parse ticket');
        expect(error.options.internalCode).to.equal('403_CPT');
    });

    it('should throw 403 if client has no access', function *() {
        nockTvm.checkTicket({ src: 1234 });

        yield tvmClientsFactory.create({ clientId: 4321 });

        const context = { header: { 'force-tvm-check': 1, 'x-ya-service-ticket': 'ticket' } };
        const error = yield catchError(tvmMiddleware.bind(context, {}));

        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('Client has no access');
        expect(error.options.internalCode).to.equal('403_CNA');
    });

    it('should call `next` in success case', function *() {
        const tvmRequest = nockTvm.checkTicket({ src: 1234 });

        yield tvmClientsFactory.create({ clientId: 1234, name: 'testTvmClient' });

        const context = {
            header: { 'force-tvm-check': 1, 'x-ya-service-ticket': 'ticket' },
            state: {}
        };
        const spy = sinon.spy();

        yield tvmMiddleware.call(context, function *() {
            tvmRequest.done();
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
        expect(context.state.tvmClient).to.deep.equal('testTvmClient');
    });
});
