const { expect } = require('chai');
const nock = require('nock');
const nockTvm = require('tests/helpers/nockTvm');
const config = require('yandex-config');
const catchError = require('tests/helpers/catchError').generator;

const tvm = require('helpers/tvm');

describe('Tvm helper', () => {
    describe('parseTicket', () => {
        afterEach(nock.cleanAll);

        it('should make request to tvm and parse ticket', function *() {
            nockTvm.checkTicket({ src: 1234, other: 'field' });
            const res = yield tvm.parseTicket('some_ticket');

            expect(res).to.deep.equal({ src: 1234 });
        });

        it('should catch error and return error message', function *() {
            nockTvm.checkTicket({ error: 'message' }, 403);
            const res = yield tvm.parseTicket('some_ticket');

            expect(res.src).to.be.undefined;
            expect(res.err.statusCode).to.equal(403);

        });
    });

    describe('getTicket', () => {
        afterEach(() => {
            nock.cleanAll();
            tvm.cache.reset();
        });

        it('should request to tvm', function *() {
            nockTvm.getTicket({
                'direct-api-testing': { ticket: 'some_ticket' }
            });

            const actual = yield tvm.getTicket(config.tvm.direct);

            expect(actual).to.equal('some_ticket');
        });

        it('should get ticket from cache', function *() {
            nockTvm.getTicket({
                'direct-api-testing': { ticket: 'some_ticket' }
            });

            tvm.getTicket(config.tvm.direct);
            const actual = yield tvm.getTicket(config.tvm.direct);

            expect(actual).to.equal('some_ticket');
        });

        it('should throw error if tvm answer with error', function *() {
            nock(config.tvm.host)
                .get('/tickets')
                .query(true)
                .reply(500, { err: 'wer' });

            const error = yield catchError(tvm.getTicket.bind({}, config.tvm.direct));

            expect(error.statusCode).to.equal(500);
        });
    });
});
