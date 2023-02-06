'use strict';

const util = require('util');
const counterHelpers = require('../../utils/counter');
const BlockStat = require('../../utils/blockstat');

describe('counter-helpers', () => {
    let sandbox;

    beforeEach(function() {
        sandbox = sinon.createSandbox();
        sandbox.stub(BlockStat, 'dictionary').callsFake([
            ['snippet', '254'],
            ['images', '277'],
            ['title', '82'],
            ['source', '186'],
            ['wizard', '358'],
            ['news', '3'],
            ['market', '4'],
            ['tab', '2'],
            ['ad', '1'],
            ['pos', '84'],
            ['p0', '85'],
            ['upper', '134'],
            ['pimportant', '613'],
            ['top', '77']
        ]);
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('decode', function() {
        it('should return valid decoded counter for encoded fields', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '254.277.82',
                vars: '186=358'
            }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });
        });

        it('should return valid decoded counter for already decoded fields', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: 'source=wizard'
            }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });
        });

        it('should throw error for invalid counter', function() {
            assert.throws(() => counterHelpers.decode({
                path: 'snippet/images/title',
                vars: 'source=wizard'
            }), Error);
        });

        it('should not decode vars with "-" prefix', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '254.277.82',
                vars: '-source=12345'
            }), {
                path: '/snippet/images/title',
                vars: { '-source': '12345' }
            });
        });

        it('should decode counter without vars', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '254.277.82'
            }), {
                path: '/snippet/images/title'
            });
        });

        it('should decode when path encoded, vars decoded', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '254.277.82',
                vars: 'source=wizard'
            }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });
        });

        it('should decode when path decoded, vars encoded', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: '186=358'
            }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });
        });

        it('should decode counter with multiple vars', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '254.277.82',
                vars: '186=358,3=4,2=1'
            }), {
                path: '/snippet/images/title',
                vars: { news: 'market', source: 'wizard', tab: 'ad' }
            });
        });

        it('should skip vars mentioned in ignoreVars', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: '186=358,3=4,2=1'
            }, { ignoreVars: ['news', 'tab'] }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });

            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: '186=358,3=4,2=1'
            }, { ignoreVars: ['186', '2'] }), {
                path: '/snippet/images/title',
                vars: { news: 'market' }
            });

            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: 'news=market,source=wizard,tab=ad'
            }, { ignoreVars: ['186', '2'] }), {
                path: '/snippet/images/title',
                vars: { news: 'market' }
            });

            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: 'news=market,source=wizard,tab=ad'
            }, { ignoreVars: ['news', 'tab'] }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });
        });

        it('should ignore wildcard', function() {
            assert.deepEqual(counterHelpers.decode({
                path: '/snippet/images/title',
                vars: 'source=wizard,pos=*,image=*'
            }), {
                path: '/snippet/images/title',
                vars: { source: 'wizard' }
            });
        });
    });

    describe('checkPos', () => {
        it('should not throw for valid pos', function() {
            ['p0', 'p10', 'pupper', 'ptop', 'pimportant'].forEach(posVal => {
                assert.doesNotThrow(
                    () => counterHelpers.checkPos(posVal)
                );
            });
        });

        it('should throw error with invalid or missing pos', function() {
            ['p50', 'p-1', 'pmedium', 'ololo', '*', undefined].forEach(posVal => {
                assert.throws(
                    () => counterHelpers.checkPos(posVal),
                    'Значение pos в vars должно быть в диапазоне p0-p49 или {,p}top, {,p}upper, {,p}important\n' +
                    `Сработавшие счётчики имеют значение pos=${posVal}`
                );
            });
        });
    });

    describe('checkVars', () => {
        const path = '/test';

        it('should not throw error with no patterns', function() {
            assert.doesNotThrow(() => counterHelpers.checkVars({ path, vars: { pos: 'p0' } }));
        });

        it('should throw error with empty vars', function() {
            assert.throws(
                () => counterHelpers.checkVars({ path }, { varPatterns: {} }),
                'В проверяемом счётчике /test нет переменных'
            );
        });

        it('should throw error with missing var value', function() {
            assert.throws(
                () => counterHelpers.checkVars({ path, vars: { pos: 'p0' } }, { varPatterns: { source: /^wiz/ } }),
                'У переменной source не задано значение'
            );
        });

        it('should throw error with invalid var value', function() {
            assert.throws(
                () => counterHelpers.checkVars(
                    { path, vars: { pos: 'p0', source: 'fake' } },
                    { varPatterns: { source: /^wiz/ } }
                ),
                // TODO: почему-то теряется третий аргумент assert.match
                'expected value to match\n    expected = /^wiz/\n    actual = fake'
            );
        });

        it('should not throw error with valid var value', function() {
            assert.doesNotThrow(
                () => counterHelpers.checkVars(
                    { path, vars: { pos: 'p0', source: 'wizard' } },
                    { varPatterns: { source: /^wiz/ } }
                )
            );
        });
    });

    describe('prepareTriggeredCounters', () => {
        it('should only decode counter if counter with same "path" was not expected', () => {
            assert.deepEqual(
                counterHelpers.prepareTriggeredCounters(
                    { path: '/test', vars: '186=358' },
                    [{ path: '/test2' }]
                ),
                [{ path: '/test', vars: { source: 'wizard' } }]
            );
        });

        it('should keep "vars" if option "varPatterns" is set', () => {
            assert.deepEqual(
                counterHelpers.prepareTriggeredCounters(
                    { path: '/test', vars: '186=358' },
                    [{ path: '/test' }],
                    { varPatterns: {} }
                ),
                [{ path: '/test', vars: { source: 'wizard' } }]
            );
        });

        describe('"checkPos"', () => {
            beforeEach(() => sandbox.stub(counterHelpers, 'checkPos').returns(undefined));

            it('should be called if pos is present but expected vars is empty', () => {
                counterHelpers.prepareTriggeredCounters(
                    { path: '/test', vars: '84=85' },
                    [{ path: '/test' }]
                );

                assert.called(counterHelpers.checkPos);
            });

            it('should be called if pos is present and expected vars is not empty', () => {
                counterHelpers.prepareTriggeredCounters(
                    { path: '/test', vars: '84=85,test=1' },
                    [{ path: '/test', vars: { test: '1' } }]
                );

                assert.calledOnce(counterHelpers.checkPos);
                assert.calledWith(counterHelpers.checkPos, 'p0');
            });

            it('should not be called if pos is present in expected "vars"', () => {
                counterHelpers.prepareTriggeredCounters(
                    { path: '/test', vars: '84=85' },
                    [{ path: '/test', vars: { pos: 'p0' } }]
                );

                assert.notCalled(counterHelpers.checkPos);
            });

            it('should not be called if pos is not present', () => {
                counterHelpers.prepareTriggeredCounters(
                    { path: '/test', vars: '' },
                    [{ path: '/test', vars: { test: '1' } }]
                );

                assert.notCalled(counterHelpers.checkPos);
            });
        });
    });

    describe('prepareExpectedCounters', () => {
        it('should clean mixed fields that are not fields of redir/blockstat itself', () => {
            assert.deepEqual(
                counterHelpers.prepareExpectedCounters({
                    path: '/test',
                    vars: 'test=1',
                    message: 'test',
                    ignoreVars: ['-test'],
                    varPatterns: { source: /^wiz/ }
                }),
                [{
                    path: '/test',
                    vars: { test: 'ad' }
                }]
            );
        });
    });

    describe('findCounters', () => {
        describe('should correctly filter triggered counters', () => {
            it('by path', () => {
                const expected = [{ path: '/test/1' }];
                const triggered = [{ path: '/test/1' }, { path: '/test/2' }];

                assert.deepEqual(
                    counterHelpers.findCounters(triggered, expected),
                    [{
                        triggered: [expected[0]],
                        count: 1,
                        expected: expected[0]
                    }]
                );
            });

            it('by path and vars', () => {
                const expected = [{ path: '/test/1', vars: { someVar: 1, anotherVar: 2 } }];
                const triggered = [
                    { path: '/test/1', vars: { someVar: 1, anotherVar: 2 } },
                    { path: '/test/1', vars: { someVar: 1 } }
                ];

                assert.deepEqual(
                    counterHelpers.findCounters(triggered, expected),
                    [{
                        triggered: [expected[0]],
                        count: 1,
                        expected: expected[0]
                    }]
                );
            });
        });

        describe('should correctly count identical triggered counters', () => {
            const expected = [{ path: '/test/1' }, { path: '/test/2' }];
            const triggered = [
                { path: '/test/1' }, { path: '/test/2' }, { path: '/test/1' }, { path: '/test/2' }
            ];

            assert.deepEqual(
                counterHelpers.findCounters(triggered, expected),
                [
                    {
                        triggered: [expected[0], expected[0]],
                        count: 2,
                        expected: expected[0]
                    },
                    {
                        triggered: [expected[1], expected[1]],
                        count: 2,
                        expected: expected[1]
                    }
                ]
            );
        });

        describe('should respect all fields from counters (not only path and vars)', () => {
            it('if they match', () => {
                const expected = [{ path: '/test/1', vars: { test: '1' }, url: 'https://yandex.ru' }];
                const triggered = [{ path: '/test/1', vars: { test: '1' }, url: 'https://yandex.ru' }];

                assert.deepEqual(
                    counterHelpers.findCounters(triggered, expected),
                    [{
                        triggered: [expected[0]],
                        count: 1,
                        expected: expected[0]
                    }]
                );
            });

            it('if they mismatch', () => {
                const expected = [{ path: '/test/1', vars: { test: '1' }, url: 'https://yandex.ru' }];
                const triggered = [{ path: '/test/1', vars: { test: '1' }, url: 'https://yandex.ru/404' }];

                assert.deepEqual(
                    counterHelpers.findCounters(triggered, expected),
                    [{
                        triggered: [],
                        count: 0,
                        expected: expected[0]
                    }]
                );
            });
        });
    });

    describe('isValid', () => {
        beforeEach(() => {
            sandbox.spy(counterHelpers, 'expectedCountersTriggeredOnce');
            sandbox.spy(counterHelpers, 'expectedCountersTriggeredAtLeastOnce');
        });

        it('should call expectedCountersTriggeredOnce by default', () => {
            const found = [{
                triggered: [{ path: '/test/1' }],
                count: 1
            }];

            counterHelpers.isValid(found);

            assert.calledOnce(counterHelpers.expectedCountersTriggeredOnce);
        });

        it('should call expectedCountersTriggeredAtLeastOnce if allowMultipleTriggering is set', () => {
            const found = [{
                triggered: [{ path: '/test/1' }, { path: '/test/1' }],
                count: 2
            }];
            const triggered = [];

            counterHelpers.isValid(found, triggered, { allowMultipleTriggering: true });

            assert.calledOnce(counterHelpers.expectedCountersTriggeredAtLeastOnce);
        });
    });

    describe('expectedCountersTriggeredOnce', () => {
        it('should return true if all counters were triggered once', () => {
            const found = [{
                triggered: [{ path: '/test/1' }],
                count: 1
            }];

            assert.isTrue(counterHelpers.expectedCountersTriggeredOnce(found));
        });

        it('should return false if some counters were not triggered', () => {
            const found = [{
                triggered: [],
                count: 0
            }];

            assert.isFalse(counterHelpers.expectedCountersTriggeredOnce(found));
        });

        it('should return false if some counters were triggered more than once', () => {
            const found = [{
                triggered: [{ path: '/test/1' }, { path: '/test/1' }],
                count: 2
            }];

            assert.isFalse(counterHelpers.expectedCountersTriggeredOnce(found));
        });
    });

    describe('expectedCountersTriggeredAtLeastOnce', () => {
        it('should return true if all counters were triggered once', () => {
            const found = [{
                triggered: [{ path: '/test/1' }],
                count: 1
            }];

            assert.isTrue(counterHelpers.expectedCountersTriggeredAtLeastOnce(found));
        });

        it('should return false if some counters were not triggered', () => {
            const found = [{
                triggered: [],
                count: 0
            }];

            assert.isFalse(counterHelpers.expectedCountersTriggeredAtLeastOnce(found));
        });

        it('should return true if some counters were triggered more than once', () => {
            const found = [{
                triggered: [{ path: '/test/1' }, { path: '/test/1' }],
                count: 2
            }];

            assert.isTrue(counterHelpers.expectedCountersTriggeredAtLeastOnce(found));
        });
    });

    describe('getFailMessage', () => {
        let triggered;
        let found;

        beforeEach(() => {
            triggered = [
                { path: '/test/1' },
                { path: '/test/2' }
            ];
            found = [];
        });

        afterEach(() => {
            delete process.env.CLIENT_COUNTERS_VERBOSE;
            delete process.env.SERVER_COUNTERS_VERBOSE;
        });

        it('should list all triggered counters if CLIENT_COUNTERS_VERBOSE is set', () => {
            process.env.CLIENT_COUNTERS_VERBOSE = 1;

            assert.include(
                counterHelpers.getFailMessage(triggered, found),
                `все сработавшие счетчики:\n${util.inspect(triggered, { compact: true })}`
            );
        });

        it('should list all triggered counters if SERVER_COUNTERS_VERBOSE is set', () => {
            process.env.SERVER_COUNTERS_VERBOSE = 1;

            assert.include(
                counterHelpers.getFailMessage(triggered, found),
                `все сработавшие счетчики:\n${util.inspect(triggered, { compact: true })}`
            );
        });

        it('should add custom message if provided', () => {
            assert.include(
                counterHelpers.getFailMessage(triggered, found, 'my custom message'),
                'my custom message'
            );
        });

        it('should add information about non triggerred counter', () => {
            found = [{
                triggered: [],
                count: 0,
                expected: { path: '/test/1' }
            }];

            assert.include(
                counterHelpers.getFailMessage(triggered, found),
                `не сработал счетчик ${JSON.stringify({ path: '/test/1' })}`
            );
        });

        it('should add information about multiple triggering', () => {
            found = [{
                triggered: [{ path: '/test/1' }, { path: '/test/1' }],
                count: 2,
                expected: { path: '/test/1' }
            }];

            assert.include(
                counterHelpers.getFailMessage(triggered, found, 'my custom message'),
                `ожидаемый счетчик ${JSON.stringify({ path: '/test/1' })} сработал 2 раз вместо 1`
            );
        });

        it('should not include info on counters triggered multiple times if allowMultipleTriggering is set', () => {
            found = [{
                triggered: [{ path: '/test/1' }, { path: '/test/1' }],
                count: 2,
                expected: { path: '/test/1' }
            }];

            assert.notInclude(
                counterHelpers.getFailMessage(triggered, found, 'my custom message', { allowMultipleTriggering: true }),
                `ожидаемый счетчик ${JSON.stringify({ path: '/test/1' })} сработал 2 раз вместо 1`
            );
        });
    });

    describe('getValidTriggered', () => {
        let checkVarsSpy;

        beforeEach(() => {
            checkVarsSpy = sandbox.spy(counterHelpers, 'checkVars');
        });

        it('should check and return all counters', () => {
            const found = [
                {
                    triggered: [
                        { path: '/test/1', vars: { source: 'wizard' } },
                        { path: '/test/1', vars: { source: 'wizard' } }
                    ],
                    count: 2,
                    expected: { path: '/test/1', source: 'wizard' }
                },
                {
                    triggered: [
                        { path: '/test/2', vars: { source: 'wizard' } }
                    ],
                    count: 1,
                    expected: { path: '/test/2', source: 'wizard' }
                }
            ];
            const options = { varPatterns: { source: /^wiz/ } };

            const result = counterHelpers.getValidTriggered(found, options);

            assert.deepEqual(result, [
                { path: '/test/1', vars: { source: 'wizard' } },
                { path: '/test/1', vars: { source: 'wizard' } },
                { path: '/test/2', vars: { source: 'wizard' } }
            ]);

            assert.calledThrice(counterHelpers.checkVars);

            assert.deepEqual(checkVarsSpy.firstCall.args, [{ path: '/test/1', vars: { source: 'wizard' } }, options]);
            assert.deepEqual(checkVarsSpy.secondCall.args, [{ path: '/test/1', vars: { source: 'wizard' } }, options]);
            assert.deepEqual(checkVarsSpy.thirdCall.args, [{ path: '/test/2', vars: { source: 'wizard' } }, options]);
        });
    });

    describe('compare', () => {
        describe('softMode', () => {
            it('should return true by partial counters data', () => {
                assert.isTrue(counterHelpers.compare(
                    { path: 'test', url: '//yandex.ru', vars: { a: 1, b: 2 } },
                    { path: 'test', vars: { a: 1 } }
                ));
            });
        });

        describe('strictMode', () => {
            beforeEach(() => {
                process.env.HERMIONE_COUNTERS_STRICT = 1;
            });

            it('should return false for partial counters data', () => {
                assert.isFalse(counterHelpers.compare(
                    { path: 'test', url: '//yandex.ru', vars: { a: 1, b: 2 } },
                    { path: 'test', url: '//yandex.ru', vars: { a: 1 } }
                ));
            });

            it('should return true for identical counters data', () => {
                assert.isTrue(counterHelpers.compare(
                    { path: 'test', url: '//yandex.ru', vars: { a: 1, b: 2 } },
                    { path: 'test', url: '//yandex.ru', vars: { a: 1, b: 2 } }
                ));
            });
        });
    });
});
