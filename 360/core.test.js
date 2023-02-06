'use strict';

jest.mock('@yandex-int/duffman');
jest.mock('@ps-int/mail-lib');
jest.mock('../config/index.js');
jest.mock('./console.js', () => {});
jest.mock('./secrets.js');

const config = require('../config/index.js');
config.__setMockData({ 'duffman.methods': { foo: 1 } });

const { requireInstance } = require('@ps-int/mail-lib');
requireInstance.mockReturnValue({ toDuffmanModel: () => 2 });

const Core = require('./core.js');

describe('Core', () => {
    describe('models', () => {
        it('returns model list', () => {
            const core = new Core();

            expect(core.models).toEqual({ foo: 2 });

            core.models = {};

            expect(core.models).toEqual({ foo: 2 });
        });
    });

    describe('getMethodHeaders', () => {
        it('returns common method headers', () => {
            const core = new Core();
            core.req = {
                config: { duffman: { methodHeaders: { m: 1 } } },
                tvm: { headers: { t: 2 } }
            };

            expect(core.getMethodHeaders()).toEqual({
                m: 1,
                t: 2
            });
        });
    });

    describe('httpCommonArgs', () => {
        let core;
        beforeEach(() => {
            core = new Core();
            core.req = {
                config: { duffman: { commonLogOptions: { project: 'PRJ' } } }
            };
        });

        it('adds user-agent', () => {
            expect(core.httpCommonArgs({})).toEqual({
                _http_log: {
                    project: 'PRJ'
                },
                headers: { 'user-agent': 'lite-node' }
            });
        });

        it('doesn\'t modify other headers', () => {
            expect(core.httpCommonArgs({
                other: 42,
                headers: {
                    'ignore': 'me',
                    'user-agent': 'replace me'
                }
            })).toEqual({
                _http_log: {
                    project: 'PRJ'
                },
                other: 42,
                headers: {
                    'ignore': 'me',
                    'user-agent': 'lite-node'
                }
            });
        });
    });

    describe('logCommonArgs', () => {
        it('returns fields for logging', () => {
            const core = new Core();
            core.req = { config: { duffman: { commonLogOptions: { foo: 1, bar: 2 } } } };

            const result = core.logCommonArgs({ foo: 3 });

            expect(result).toEqual({ foo: 3, bar: 2 });
        });
    });
});
