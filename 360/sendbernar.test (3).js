'use strict';

const DollarConfig = require('dollar-config');
jest.mock('@ps-int/sendbernar');

const SendbernarService = require('./sendbernar.js');
const config = new DollarConfig({
    'headers': {},
    'errors': {},
    'x-prepare': {}
});
const service = new SendbernarService(config, '/path/to/sendbernar.yaml');

describe('sendbernar service', () => {
    describe('.fetch', () => {
        let core;

        beforeEach(() => {
            core = {
                got: jest.fn(() => Promise.resolve({ status: 'ok' }))
            };
        });

        it('fetch throws Unknown method', () => {
            expect(() => service.fetch({ core, headers: {} }, { path: '/NOT_EXIST' })).toThrow(/Unknown method/);
        });

        it('fetch throws bad_request', () => {
            expect(() => service.fetch({ core, headers: {} }, { path: '/bad_request' })).toThrow('bad_request');
        });

        it('fetch sends prepared request', () => {
            return service
                .fetch({ core, headers: {} }, { path: '/good' })
                .then((result) => {
                    expect(core.got).toHaveBeenCalledTimes(1);
                    expect(core.got.mock.calls[0][0]).toMatch(/\/good_method$/);
                    expect(core.got.mock.calls[0][1]).toMatchObject({
                        headers: { header: 'value' },
                        getRaw: true,
                        logPostArgs: false,
                        form: false
                    });
                    expect(result).toEqual([
                        { status: 'ok' },
                        'good'
                    ]);
                });
        });

        it('fetch set form flag', () => {
            return service
                .fetch({ core, headers: {} }, { path: '/send_service' })
                .then((result) => {
                    expect(core.got).toHaveBeenCalledTimes(1);
                    expect(core.got.mock.calls[0][0]).toMatch(/\/send_service$/);
                    expect(core.got.mock.calls[0][1]).toMatchObject({
                        headers: { header: 'value' },
                        getRaw: true,
                        logPostArgs: false,
                        form: true
                    });
                    expect(result).toEqual([
                        { status: 'ok' },
                        'send_service'
                    ]);
                });
        });
    });

    describe('.parse', () => {
        it('parse throws if not an array', () => {
            expect(() => service.parse({})).toThrow(/Unexpected response/);
        });

        it('parse throws if bad_response', () => {
            expect(() => service.parse([ {}, 'bad_response' ])).toThrow(/Parse method failed/);
        });

        it('parse returns parsed response', () => {
            expect(service.parse([ {}, 'good' ])).toEqual({
                status: 'ok'
            });
        });
    });
});
