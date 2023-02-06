const assert = require('assert');
const catchError = require('catch-error-async');
const config = require('yandex-cfg');
const nock = require('nock');
const _ = require('lodash');

const Tvmtool = require('lib/tvmtool');
const { nockTvmCheckTicket } = require('tests/mocks');

const { serverUrl } = config.tvm;

describe('Tvmtool lib', () => {
    describe('getTickets', () => {
        afterEach(nock.cleanAll);

        it('should return tickets for passed dsts', async() => {
            const tvmDst = 'dst1';

            nock(`${serverUrl}/`)
                .get('/tvm/tickets')
                .query(data => Boolean(data.dsts))
                .reply(200, () => _.pick({
                    dst1: { ticket: '123' },
                    dst2: { ticket: '234' },
                }, tvmDst));

            const tickets = await Tvmtool.getTickets(tvmDst);

            assert.deepEqual(tickets, { dst1: { ticket: '123' } });
        });

        // Описание ошибок при работе с tvmtool
        // https://wiki.yandex-team.ru/passport/tvm2/qloud/#oshibki
        it('should throw error when tvmtool is down', async() => {
            nock(`${serverUrl}/`)
                .get('/tvm/tickets')
                .query(data => Boolean(data.dsts))
                .times(3)
                .reply(500, 'Something went wrong');
            const error = await catchError(Tvmtool.getTickets.bind(Tvmtool), 'dst1');

            assert.equal(error.message, 'Tvmtool error');
            assert.equal(error.statusCode, 403);
            assert.deepStrictEqual(error.options, {
                internalCode: '403_TTE',
                args: { path: `${serverUrl}/tvm/tickets`, query: { dsts: 'dst1' } },
                statusCode: 500,
                body: 'Something went wrong',
                message: 'Response code 500 (Internal Server Error)',
            });
        }).timeout(10000);

        it('should throw error when tvmtool token is invalid', async() => {
            nock(`${serverUrl}/`)
                .get('/tvm/tickets')
                .query(data => Boolean(data.dsts))
                .reply(401, 'Invalid authentication token');
            const error = await catchError(Tvmtool.getTickets.bind(Tvmtool), 'dst1');

            assert.equal(error.message, 'Tvmtool error');
            assert.equal(error.statusCode, 403);
            assert.deepStrictEqual(error.options, {
                internalCode: '403_TTE',
                args: { path: `${serverUrl}/tvm/tickets`, query: { dsts: 'dst1' } },
                statusCode: 401,
                body: 'Invalid authentication token',
                message: 'Response code 401 (Unauthorized)',
            });
        });

        it('should throw error when ticket is invalid', async() => {
            nock(`${serverUrl}/`)
                .get('/tvm/tickets')
                .query(data => Boolean(data.dsts))
                .reply(403, {
                    error: 'invalid signature format',
                    debug_string: 'ticket_type=service;expiration_time=1513634501;',
                    logging_string: '3:serv:CKISEMX14NEFIgYI8gEQ8gE:',
                });
            const error = await catchError(Tvmtool.getTickets.bind(Tvmtool), 'dst1');

            assert.equal(error.message, 'Tvmtool error');
            assert.equal(error.statusCode, 403);
            assert.deepStrictEqual(error.options, {
                internalCode: '403_TTE',
                args: { path: `${serverUrl}/tvm/tickets`, query: { dsts: 'dst1' } },
                statusCode: 403,
                body: {
                    error: 'invalid signature format',
                    debug_string: 'ticket_type=service;expiration_time=1513634501;',
                    logging_string: '3:serv:CKISEMX14NEFIgYI8gEQ8gE:',
                },
                message: 'Response code 403 (Forbidden)',
            });
        });
    });

    describe('checkTicket', () => {
        afterEach(nock.cleanAll);

        it('should check ticket for passed destination', async() => {
            const ticketBody = '3:serv:CNZCEIrCjOcFIgcIwY56EN8B:GWQMLZMA6uz6zSOPDS';
            const nockInstance = nockTvmCheckTicket();

            await Tvmtool.checkTicket(ticketBody, 1234);

            assert.ok(nockInstance.isDone());
        });

        it('should throw error if ticket is invalid', async() => {
            const ticketBody = '3:serv:CNZCEIrCjOcFIgcIwY56EN8B:GWQMLZMA6uz6zSOPDS';
            const body = { error: 'internalApply(). invalid signature format - 4' };

            nock(`${serverUrl}/`)
                .get('/tvm/checksrv')
                .query(data => Boolean(data))
                .reply(403, body);

            const error = await catchError(Tvmtool.checkTicket.bind(Tvmtool), ticketBody);

            assert.strictEqual(error.message, 'Tvmtool error');
            assert.strictEqual(error.statusCode, 403);
            assert.deepStrictEqual(error.options, {
                internalCode: '403_TTE',
                args: { path: `${serverUrl}/tvm/checksrv`, query: undefined },
                statusCode: 403,
                body,
                message: 'Response code 403 (Forbidden)',
            });
        });
    });
});
