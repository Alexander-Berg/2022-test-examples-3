/* global describe, beforeEach, it */
const chai = require('chai');
const sinon = require('sinon');
const assert = chai.assert;
chai.use(require('sinon-chai'));
sinon.assert.expose(chai.assert, { prefix: '' });

const BlockStat = require('../lib');
const parseStats = require('./lib/parse-stats');
const SessionStub = require('./stubs/session');

class WritableMock {
    constructor() {
        this.write = sinon.spy();
    }

    getSection(idx) {
        const requiredSection = idx - 1;
        const buf = this.write.lastCall.args[0];
        const untilIdx = buf.length - Buffer.byteLength('\n', 'utf8');
        let completeSection = 0;
        let sectionStart = 0;

        for (let i = 0; i < untilIdx; i++) {
            if (buf[i] === 0xFF) {
                if (requiredSection === completeSection) {
                    return buf.toString('utf8', sectionStart, i);
                }
                ++completeSection;
                sectionStart = i + 1;
            }
        }
        if (requiredSection === completeSection) {
            return buf.toString('utf8', sectionStart, untilIdx);
        }
    }
}

describe('BlockStat', () => {
    let util;
    let params;

    beforeEach(() => {
        util = {};
        params = {
            reqdata: {
                flags: {},
                http_host: 'yandex.ru',
            },
            cgidata: {
                order: [],
            },
        };
    });

    describe('constructor', () => {
        it('should create empty instance', () => {
            const bs = new BlockStat(new Map(), util);

            assert.instanceOf(bs, BlockStat);
            assert.isTrue(bs.isEmpty());
        });

        it('should set key->value mapping', () => {
            const bs = new BlockStat(new Map([['y', '321'], ['z', '456']]), util);

            assert.equal(bs.normalize('y'), '321');
            assert.equal(bs.normalize('z'), '456');
            assert.isUndefined(bs.normalize('x'));
        });
    });

    describe('#normalize()', () => {
        it('should return dictionary value by key', () => {
            const bs = new BlockStat(new Map([['x', '123']]), util);

            assert.equal(bs.normalize('x'), '123');
        });

        it('should return undefined if key does not exist in dictionary', () => {
            const bs = new BlockStat(new Map(), util);

            assert.isUndefined(bs.normalize('x'));
        });
    });

    describe('#write()', () => {
        it('should not write anything to output stream if instance is empty', () => {
            const ws = new WritableMock();
            const bs = new BlockStat(new Map(), util);

            bs.write(ws.write, new SessionStub());

            assert.notCalled(ws.write);
        });

        it('should write record atomically to avoid overlapping of concurrent records', () => {
            const ws = new WritableMock();
            const bs = new BlockStat(new Map(), util);
            bs.counter('/test');
            bs.counter('/test2');

            bs.write(ws.write, new SessionStub());

            assert.calledOnce(ws.write);
        });

        it('should return right stats section from empty blocks', () => {
            const bs = new BlockStat(new Map(), util);
            const blocks = {};
            const stats = bs.getStatsFromBlocks(blocks);

            assert.strictEqual(stats, '\t\t');
        });

        it('should return right stats section from block without vars', () => {
            const bs = new BlockStat(new Map(), util);
            const blocks = {
                '/': [
                    {},
                ],
            };

            const stats = bs.getStatsFromBlocks(blocks);

            assert.strictEqual(stats, '\t/\t0\t');
        });

        it('should return right stats section from block with one var', () => {
            const bs = new BlockStat(new Map(), util);
            const blocks = {
                '/': [
                    {
                        var0: ['value0'],
                    },
                ],
            };

            const stats = bs.getStatsFromBlocks(blocks);

            assert.strictEqual(stats, '\t/\t1\tvar0=value0\t');
        });

        it('should return right stats section from not empty blocks', () => {
            const bs = new BlockStat(new Map(), util);
            const blocks = {
                '/': [
                    {},
                ],
                '/x/y': [
                    {
                        var1: [1, 2, 3, 4],
                        var2: [1],
                    },
                ],
                '/z': [
                    {
                        var0: ['value0'],
                    },
                ],
            };

            const stats = bs.getStatsFromBlocks(blocks);

            assert.strictEqual(stats, '\t/\t0\t/x/y\t5\tvar1=1\tvar1=2\tvar1=3\tvar1=4\tvar2=1\t/z\t1\tvar0=value0\t');
        });

        describe('Log record', () => {
            const ws = new WritableMock();
            const bs = new BlockStat(new Map(), util);
            const session = new SessionStub();
            bs.counter('/test', 'key', 'value', 'key2', 'value2');
            bs.counter('/test\ttab', 'tab\tkey', 'tab\tvalue');
            bs.write(ws.write, session);
            const record = ws.write.lastCall.args[0];

            it('should contain 3 sections separated with \\xFF', () => {
                let separatorsFound = 0;
                for (let i = 0; i < record.length; i++) {
                    if (record[i] === 0xFF) {
                        ++separatorsFound;
                    }
                }
                assert.strictEqual(separatorsFound, 2);
            });

            it('should be terminated with \\n', () => {
                const eol = '\n';
                const eolLen = Buffer.byteLength(eol);

                assert.strictEqual(record.toString('utf8', record.length - eolLen), eol);
            });

            it('should contain tab-separated session info in 1st section', () => {
                assert.equal(ws.getSection(1), session.serialize());
            });

            it('should contain tab-ended 2nd section', () => {
                assert.strictEqual(ws.getSection(2).slice(-1), '\t');
            });

            it('should contain blocks stats as tab-separated sequences in 2nd section', () => {
                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                assert.isTrue(parsed.has('/test'));
                assert.deepEqual(
                    parsed.get('/test'),
                    [
                        { key: 'key', value: 'value' },
                        { key: 'key2', value: 'value2' },
                    ],
                );
            });

            it('should contain root "/" block stats in 2nd section', () => {
                const parsed = parseStats(ws.getSection(2));
                assert.isTrue(parsed.has('/'));
                assert.lengthOf(parsed.get('/'), 0);
            });

            it('should contain empty object in 3rd section if #writeTree() was not called before', () => {
                assert.equal(ws.getSection(3), '{}');
            });
        });

        describe('Escaping', () => {
            let session;
            let ws;
            let bs;

            beforeEach(() => {
                session = new SessionStub();
                ws = new WritableMock();
                bs = new BlockStat(new Map(), util);
            });

            it('should escape tabs "\\t" as "\\\\t" while writing session values', () => {
                session = new SessionStub({ appendToValues: '\txx\t' });
                bs.counter('/test');
                bs.write(ws.write, session);

                assert.equal(ws.getSection(1), session.serialize());
            });

            it('should escape tabs "\\t" as "\\\\t" while writing blocks stats', () => {
                bs.counter('/test\ttab', 'tab\tkey', 'tab\tvalue');
                bs.write(ws.write, session);

                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                const resultPath = '/test\\ttab';
                const record = parsed.get(resultPath);

                assert.isDefined(record);
                assert.equal(record[0].key, 'tab\\tkey');
                assert.equal(record[0].value, 'tab\\tvalue');
            });

            it('should escape tabs "\\\\t" as "\\\\\\\\t" while writing blocks stats', () => {
                bs.counter('/test\\ttab', 'tab\\tkey', 'tab\\tvalue');
                bs.write(ws.write, session);

                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                const resultPath = '/test\\\\ttab';
                const record = parsed.get(resultPath);

                assert.isDefined(record);
                assert.equal(record[0].key, 'tab\\\\tkey');
                assert.equal(record[0].value, 'tab\\\\tvalue');
            });

            it('should replace object\'s brackets to pipes', () => {
                const testValue = '[{"Value":"Закрыть"}]';
                const validValue = '[|"Value":"Закрыть"|]';

                bs.counter('/testtab', 'tabkey', testValue);
                bs.write(ws.write, session);

                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                const resultPath = '/testtab';
                const record = parsed.get(resultPath);

                assert.isDefined(record);
                assert.equal(record[0].key, 'tabkey');
                assert.equal(record[0].value, validValue);
            });

            it('should support values with "="', () => {
                const value = 'value=key=value';

                bs.counter('/testtab', 'tabkey', value);
                bs.write(ws.write, session);

                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                const resultPath = '/testtab';
                const record = parsed.get(resultPath);

                assert.isDefined(record);
                assert.equal(record[0].key, 'tabkey');
                assert.equal(record[0].value, value);
            });

            it('should escape tabs "\\" as "\\\\" while writing blocks stats', () => {
                bs.counter('/test\\\'path', 'test\\"key', 'test\\value');
                bs.write(ws.write, session);

                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                const resultPath = '/test\\\\\'path';
                const record = parsed.get(resultPath);

                assert.isDefined(record);
                assert.equal(record[0].key, 'test\\\\"key');
                assert.equal(record[0].value, 'test\\\\value');
            });

            it('should replace [\\x00-\\x19] with space while writing blocks stats', () => {
                bs.counter('/test\n\r\0\x19path', 'test\n\n\0\x19key', 'test\n\n\0\x19value');
                bs.write(ws.write, new SessionStub());

                let parsed;
                assert.doesNotThrow(() => parsed = parseStats(ws.getSection(2)));
                const resultPath = '/test    path';
                const record = parsed.get(resultPath);

                assert.isDefined(record);
                assert.equal(record[0].key, 'test    key');
                assert.equal(record[0].value, 'test    value');
            });
        });
    });

    describe('#counterDataNoBlockstat()', () => {
        it('should not affect "emptiness" of BlockStat instance', () => {
            const bs = new BlockStat(new Map(), util);

            bs.counterDataNoBlockstat('/some', 'k', 'v');

            assert.isTrue(bs.isEmpty());
        });

        it('should return empty array if odd number of key-value args passed', () => {
            const bs = new BlockStat(new Map(), util);

            const res = bs.counterDataNoBlockstat('/some', 'k', 'v', 'k_no_value');

            assert.isArray(res);
            assert.lengthOf(res, 0);
        });

        it('should return array of two strings if arguments count is 3 or more', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);

            const res = bs.counterDataNoBlockstat('/test', '-key', 'value');

            assert.isArray(res);
            assert.lengthOf(res, 2);
            assert.isString(res[0]);
            assert.isString(res[1]);
        });

        it('should return array of one string if only one argument passed', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);
            const res = bs.counterDataNoBlockstat('/test');

            assert.isArray(res);
            assert.lengthOf(res, 1);
            assert.isString(res[0]);
        });

        it('should normalize counter path (1st arg) using dictionary', () => {
            /**
             * Path normalization explained.
             *
             * dict: { 'counter' => '123', 'path' => '321' }
             * '/counter/path'
             *      .split('/')                       // => [ '', 'counter', 'path' ]
             *      .filter(s => s.length > 0)        // => [ 'counter', 'path' ]
             *      .map(s => BlockStat.normalize(s)) // => [ '123', '321' ]
             *      .join('.');                       // => '123.321'
             */

            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321']]), util);
            const res = bs.counterDataNoBlockstat('/test/moar');

            assert.deepEqual(res, ['123.321']);
        });

        it('should normalize keys and values using dictionary', () => {
            const bs = new BlockStat(new Map([['k1', '81'], ['v1', '91'], ['k2', '82'], ['v2', '92']]), util);
            const res = bs.counterDataNoBlockstat('/test', 'k2', 'v1', 'k1', 'v2');

            assert.equal(res[1], '82=91,81=92');
        });

        it('should not normalize key and paired value if key starts with "-"', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);
            const res = bs.counterDataNoBlockstat('/test', '-key', 'value');

            assert.equal(res[1], '-key=value');
        });

        it('should not normalize values starting with "_", but should remove leading "_"', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['key', '321']]), util);
            const res = bs.counterDataNoBlockstat('/test', 'key', '_value');

            assert.equal(res[1], '321=value');
        });

        it('should accept arguments as an array', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321'], ['key', '555'], ['value', '777']]), util);
            const res = bs.counterDataNoBlockstat(['/test/moar', 'key', 'value', '-dont', '_replace']);

            assert.deepEqual(res, ['123.321', '555=777,-dont=_replace']);
        });

        it('should not affect log output anyway', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321'], ['key', '555'], ['value', '777']]), util);
            const ws = new WritableMock();

            bs.counterDataNoBlockstat(['/test/moar', 'key', 'value', '-dont', '_replace']);

            bs.isEmpty(false);
            bs.write(ws.write, new SessionStub());
            assert.notCalled(ws.write);
        });

        it('has the synonymous method #counter_data_no_blockstat()', () => {
            const bs = new BlockStat(new Map(), util);

            assert.strictEqual(bs.counter_data_no_blockstat, bs.counterDataNoBlockstat);
        });
    });

    describe('#counterData()', () => {
        it('should mark instance as non-empty', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);

            bs.counterData('/test');

            assert.isFalse(bs.isEmpty());
        });

        it('should return empty array if odd number of key-value args passed', () => {
            const bs = new BlockStat(new Map(), util);

            const res = bs.counterData('/some', 'k', 'v', 'k_no_value');

            assert.isArray(res);
            assert.lengthOf(res, 0);
        });

        it('should return array of two strings if arguments count is 3 or more', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);

            const res = bs.counterData('/test', '-key', 'value');

            assert.isArray(res);
            assert.lengthOf(res, 2);
            assert.isString(res[0]);
            assert.isString(res[1]);
        });

        it('should return array of one string if only one argument passed', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);

            const res = bs.counterData('/test');

            assert.isArray(res);
            assert.lengthOf(res, 1);
            assert.isString(res[0]);
        });

        it('should normalize counter path (1st arg) using dictionary', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321']]), util);

            const res = bs.counterData('/test/moar');

            assert.deepEqual(res, ['123.321']);
        });

        it('should normalize keys and values using dictionary', () => {
            const bs = new BlockStat(new Map([['k1', '81'], ['v1', '91'], ['k2', '82'], ['v2', '92']]), util);

            const res = bs.counterData('/test', 'k2', 'v1', 'k1', 'v2');

            assert.equal(res[1], '82=91,81=92');
        });

        it('should not normalize key and paired value if key starts with "-"', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);

            const res = bs.counterData('/test', '-key', 'value');

            assert.equal(res[1], '-key=value');
        });

        it('should not normalize values starting with "_", but should remove leading "_"', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['key', '321']]), util);

            const res = bs.counterData('/test', 'key', '_value');

            assert.equal(res[1], '321=value');
        });

        it('should accept arguments as an array', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321'], ['key', '555'], ['value', '777']]), util);

            const res = bs.counterData(['/test/moar', 'key', 'value', '-dont', 'replace']);

            assert.deepEqual(res, ['123.321', '555=777,-dont=replace']);
        });

        it('should reject falsey key/value', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['value', '777'], ['replace', '555'], ['last', '999']]), util);

            const varKeyFalsey = bs.counterData(['/test/value', 'test', 'replace', '', 'subvalue', 'last', 'value']);
            const varValueFalsey = bs.counterData(['/test/value', 'test', 'replace', 'key', '', 'last', 'value']);

            assert.deepEqual(varKeyFalsey, ['123.777', '123=555,999=777']);
            assert.deepEqual(varValueFalsey, ['123.777', '123=555,999=777']);
        });

        it('should not add leading comma when first key/value is falsey', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['value', '777']]), util);

            const varKeyFalsey = bs.counterData(['/test/value', '', 'subvalue', '-dont', 'replace']);
            const varValueFalsey = bs.counterData(['/test/value', 'key', '', '-dont', 'replace']);

            assert.deepEqual(varKeyFalsey, ['123.777', '-dont=replace']);
            assert.deepEqual(varValueFalsey, ['123.777', '-dont=replace']);
        });

        it('should store non normalized counter data in instance for logging', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321'], ['key', '555'], ['value', '777']]), util);
            const ws = new WritableMock();

            bs.counterData('/test/moar', 'key', 'value', '-dont', 'replace');

            bs.write(ws.write, new SessionStub());
            const parsed = parseStats(ws.getSection(2));
            assert.isTrue(parsed.has('/test/moar'));
            assert.deepEqual(
                parsed.get('/test/moar'),
                [
                    { key: 'key', value: 'value' },
                    { key: '-dont', value: 'replace' },
                ],
            );
        });

        it('has the synonymous method #counter_data()', () => {
            const bs = new BlockStat(new Map(), util);

            assert.strictEqual(bs.counter_data, bs.counterData);
        });
    });

    describe('#counter()', () => {
        it('should return script calling fn w() with encoded path if called with 1 arg', () => {
            const bs = new BlockStat(new Map([['test', '123'], ['moar', '321']]), util);

            assert.equal(bs.counter('/test/moar'), 'w(this,\'123.321\');');
        });

        it('should return script calling fn w() with encoded path and kv-pairs string if args count >= 3', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);

            assert.equal(bs.counter('/test/test', '-key', 'value'), 'w(this,\'123.123\',\'-key=value\');');
        });

        it('should use #counterData() to get path and vars and to store data for logging', () => {
            const bs = new BlockStat(new Map([['test', '123']]), util);
            sinon.spy(bs, 'counterData');

            bs.counter('/test', '-key', 'value');

            assert.isTrue(bs.counterData.calledOnce);
            assert.deepEqual(
                bs.counterData.getCall(0).args,
                ['/test', '-key', 'value'],
            );
        });
    });

    describe('#writeTree()', () => {
        it('should set value for last section of log record', () => {
            const ws = new WritableMock();
            const bs = new BlockStat(new Map(), util);
            bs.counter('/test');
            const value = { test: 'kitties' };

            bs.writeTree(value);

            bs.write(ws.write, new SessionStub());
            assert.equal(ws.getSection(3), JSON.stringify(value));
        });
    });

    describe('#redirPrefix()', () => {
        /**
         * Build kv-map from prefix string
         *
         * @param {String} prefix
         * @returns {Map<String, String>}
         */
        function parsePrefix(prefix) {
            // 'k=v/k=v/.../*' => [['k', 'v'], ['k', 'v'], ..]
            const p = prefix.replace(/\/\*$/, '').split('/')
                .map(kv => {
                    const m = kv.match(/^([^=]*)=(.*)$/);
                    return [m[1], m[2]];
                });
            return new Map(p);
        }

        describe('Field "prefix"', () => {
            it('should be a string terminated with "/*"', () => {
                const bs = new BlockStat(new Map([['test', '123']]), util, params);

                const po = bs.redirPrefix(null, '/test');

                assert.isString(po.prefix);
                assert.match(po.prefix, /\/\*$/);
            });

            it('should contain field "path" with normalized counter path', () => {
                const bs = new BlockStat(new Map([['test', '123'], ['moar', '321']]), util, params);

                const po = bs.redirPrefix(null, '/test/moar');

                assert.equal(parsePrefix(po.prefix).get('path'), '123.321');
            });

            it('should contain field "vars" with normalized counter vars', () => {
                const bs = new BlockStat(new Map([['test', '123'], ['key', '55'], ['val', '77']]), util, params);

                const po = bs.redirPrefix(null, '/test', 'key', 'val', '-src', 'web');

                assert.equal(parsePrefix(po.prefix).get('vars'), '55=77,-src=web');
            });
        });
    });

    describe('#redirFrom()', () => {
        it('should remove yandsearch prefix', () => {
            params.reqdata.http_host = 'yandsearch.yandex.fr';
            params.reqdata.path = 'path';
            params.reqdata.project = 'project';
            const bs = new BlockStat(new Map([['test', '123']]), util, params);

            assert.equal(bs.redirFrom(undefined, '1.2.3'), 'yandex.fr;path;project;;1.2.3');
        });
    });

    describe('Proxied methods', () => {
        it('should proxy #signUrl to util', () => {
            const stub = sinon.stub().returns('res');
            const bs = new BlockStat(new Map(), { signUrl: stub }, params);

            const result = bs.signUrl('args');
            assert.equal(result, 'res');
            assert.calledWith(stub, 'args');
            assert.calledOnce(stub);
        });
    });
});
