/* eslint-disable strict */
/* global Experimentarium:true, blocks */
const path = require('path');
const fs = require('fs');

const assert = require('chai').assert;
const sinon = require('sinon');

const assetsPath = path.resolve(__dirname, 'experimentarium.js');

/* Дети, не повторяйте это дома */
/* С новой версий node включен 'use strict' по-умолчанию и поэтому надо докинуть в объявления в global*/
eval(fs.readFileSync(assetsPath).toString() + '; global.Experimentarium = Experimentarium; global.blocks = undefined');

describe('Experimentarium', () => {
    let /** @type Experimentarium */ experimentarium;
    let a;
    let b;
    let c;

    beforeEach(() => {
        a = sinon.stub();
        b = sinon.stub();
        c = sinon.stub();
    });

    afterEach(() => {
        a = undefined;
        b = undefined;
        c = undefined;
    });

    describe('When experiments registered after corresponding original methods', () => {
        beforeEach(() => {
            blocks = { a, b, c, 'wrap-block-try-catch': fn => fn };
            experimentarium = new Experimentarium(blocks);
        });

        afterEach(() => {
            blocks = undefined;
            experimentarium = undefined;
        });

        describe('Core', () => {
            let localMethod;
            let localMethod2;

            beforeEach(() => {
                localMethod = sinon.stub();
                localMethod2 = sinon.stub();
                experimentarium.registerExperimentalBlocks('test', {
                    a: localMethod,
                    e: localMethod2
                });
                experimentarium.submit();
            });

            afterEach(() => {
                localMethod = undefined;
            });

            it('should correctly work with experimental blocks', () => {
                experimentarium.activate({ test: 1 });
                blocks.a();
                blocks.e();

                assert.isFalse(a.called, 'Non experimental method should not be called');
                assert.isTrue(localMethod.called, 'Experimental method should be called');
                assert.isTrue(localMethod2.called, 'Experimental method should be called');
            });

            it('should not activate experiment if exp value is "null"', () => {
                experimentarium.activate({ test: 'null' });

                blocks.a();

                assert.isTrue(a.called, 'Non experimental method should be called');
                assert.isFalse(localMethod.called, 'Experimental method should not be called');
            });

            it('should correctly deactivate', () => {
                experimentarium.activate({ test: 1 });
                experimentarium.deactivate();
                blocks.a();

                assert.isUndefined(blocks.e, 'Methods which should exists only in exp should be undefined');
                assert.isTrue(a.called, 'Non experimental method should be called after deactivation');
                assert.isFalse(localMethod.called, 'Experimental methods should not be called after deactivation');
            });

            it('should call original methods if unknown experiment is activated', () => {
                experimentarium.activate({ unknown: 1 });
                blocks.a();

                assert.isTrue(a.called, 'Non experimental method should be called if we activate unknown experiment');
                assert.isFalse(
                    localMethod.called,
                    'Experimental method should not be called if unknown experiment is activated'
                );
            });
        });

        describe('Access to base blocks', () => {
            let localMethod1;
            let localMethod2;

            const register = (expName, method) => {
                experimentarium.registerExperimentalBlocks(expName, {
                    a: method
                });
            };

            beforeEach(() => {
                localMethod1 = sinon.spy(() => {
                    // eslint-disable-next-line no-undef
                    localMethod1.__base.apply(this, arguments);
                });

                localMethod2 = sinon.spy(() => {
                    // eslint-disable-next-line no-undef
                    localMethod2.__base.apply(this, arguments);
                });
            });

            afterEach(() => {
                localMethod1 = undefined;
                localMethod2 = undefined;
            });

            it('you should have access to a base blocks', () => {
                register('test', localMethod1);
                experimentarium.submit();

                experimentarium.activate({ test: 1 });

                blocks.a();

                assert.isTrue(a.called, 'Non experimental method should be called');
                assert.isTrue(localMethod1.called, 'Experimental method itself should be called');
            });

            it('should be able to register methods multiple times', () => {
                experimentarium.registerExperimentalBlocks('test', { a: localMethod1 });
                experimentarium.registerExperimentalBlocks('test', { b: localMethod2 });
                experimentarium.submit();

                experimentarium.activate({ test: 1 });

                blocks.a();
                blocks.b();

                assert.isTrue(localMethod1.called, 'Frist experimental method should be called');
                assert.isTrue(localMethod2.called, 'Second experimental method should be called');
            });

            it('should have access to previosly registred method as base if available', () => {
                register('test', localMethod1);
                register('test', localMethod2);
                experimentarium.submit();

                experimentarium.activate({ test: 1 });

                blocks.a();

                assert.isTrue(a.called);
                assert.isTrue(localMethod1.called, 'Both experimental methods should be called');
                assert.isTrue(localMethod2.called, 'Both experimental methods should be called');
            });

            it('should remove link to base block after deactivation, if this base block not in the same exp, ', () => {
                register('test', localMethod1);
                register('test', localMethod2);
                experimentarium.submit();

                experimentarium.activate({ test: 1 });
                experimentarium.deactivate();

                assert.isUndefined(
                    localMethod1.__base,
                    'Base link between non experimental and experimental methods should be broken'
                );
                assert.equal(
                    localMethod2.__base, localMethod1,
                    'Base link between two experimental methods within the same experiment should be available'
                );
            });

            it('should have access to previosly registred method as base if available in multiple experiments', () => {
                register('test1', localMethod1);
                register('test2', localMethod2);
                experimentarium.submit();

                experimentarium.activate({ test1: 1, test2: 1 });

                blocks.a();

                assert.isTrue(a.called);
                assert.isTrue(localMethod1.called, 'Both experimental methods should be called');
                assert.isTrue(localMethod2.called, 'Both experimental methods should be called');
            });

            it('should correctly remove link to base block after deactivation, in multiple experiments', () => {
                register('test1', localMethod1);
                register('test2', localMethod2);
                experimentarium.submit();

                experimentarium.activate({ test1: 1, test2: 1 });
                experimentarium.deactivate();

                assert.isUndefined(
                    localMethod1.__base,
                    'Base link between two methods in two different experiments should be broken'
                );
                assert.isUndefined(
                    localMethod2.__base,
                    'Base link between two methods in two different experiments should be broken'
                );
            });

            it('should override in lexicographical order', () => {
                register('test1', () => 't1');
                register('test2', function bl() {
                    return bl.__base() + 't2';
                });
                experimentarium.submit();

                const flags = { test2: 1 };

                // флаг, который должен примениться первым, выставляем в последнюю очередь
                flags.test1 = 1;

                experimentarium.activate(flags);

                assert.strictEqual(blocks.a(), 't1t2');
            });
        });

        describe('Access method to experimental flag value', () => {
            let sandbox;

            beforeEach(() => {
                sandbox = sinon.createSandbox();
            });

            afterEach(() => {
                sandbox.restore();
            });

            it('should exist before activation', () => {
                const localMethod = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                assert.isFunction(localMethod.__expVal, 'Access flag method should exist before activation');
            });

            it('should not exist on original base method', () => {
                const localMethod = sandbox.spy();
                const a = blocks.a;

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                assert.typeOf(a.__expVal, 'undefined', 'Should not exist before activation');

                experimentarium.activate({ test: 1 });

                assert.typeOf(a.__expVal, 'undefined', 'Should not exist after activation');
            });

            it('should return undefined value before activation', () => {
                const localMethod = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                assert.typeOf(localMethod.__expVal(), 'undefined', 'The flag value must be strictly undefined');
            });

            it('should return correct experimental flag value after activation', () => {
                const localMethod = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                experimentarium.activate({ test: 1 });

                assert.strictEqual(
                    localMethod.__expVal(), '1',
                    'Method `__expVal` should has correct experimental flag value'
                );
            });

            it('should return correct experimental flag value after reactivation', () => {
                const localMethod = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                experimentarium.activate({ test: 1 });
                experimentarium.deactivate();
                experimentarium.activate({ test: 0 });

                assert.strictEqual(
                    localMethod.__expVal(), '0',
                    'Method `__expVal` should has correct experimental flag value after reactivation'
                );
            });

            it('should return correct experimental flag value for each multiple method override', () => {
                const localMethod1 = sandbox.spy();
                const localMethod2 = sandbox.spy();
                const localMethod3 = sandbox.spy();

                [localMethod1, localMethod2, localMethod3].forEach(method => {
                    experimentarium.registerExperimentalBlocks('test', { a: method });
                });
                experimentarium.submit();

                experimentarium.activate({ test: 1 });

                [localMethod1, localMethod2, localMethod3].forEach(method => {
                    assert.strictEqual(
                        method.__expVal(), '1',
                        'Method `__expVal` should has correct experimental flag value for one method'
                    );
                });
            });

            it('should return correct experimental flag value for different experiments', () => {
                const localMethod1 = sandbox.spy();
                const localMethod2 = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test1', { a: localMethod1 });
                experimentarium.registerExperimentalBlocks('test2', { a: localMethod2 });
                experimentarium.submit();

                experimentarium.activate({ test1: 1, test2: 'test value' });

                assert.strictEqual(
                    localMethod1.__expVal(), '1',
                    'Method `__expVal` should has correct experimental flag value for one experiment'
                );

                assert.strictEqual(
                    localMethod2.__expVal(), 'test value',
                    'Method `__expVal` should has correct experimental flag value for another experiment'
                );
            });

            it('should exist after deactivation', () => {
                const localMethod = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                experimentarium.activate({ test: 1 });
                experimentarium.deactivate();

                assert.isFunction(localMethod.__expVal, 'Access flag method should exist after deactivation');
            });

            it('should return undefined value after deactivation', () => {
                const localMethod = sandbox.spy();

                experimentarium.registerExperimentalBlocks('test', { a: localMethod });
                experimentarium.submit();

                experimentarium.activate({ test: 1 });
                experimentarium.deactivate();

                assert.typeOf(localMethod.__expVal(), 'undefined', 'The flag value must be strictly undefined');
            });
        });

        describe('Config', () => {
            it('should retrieve prop after addConfig', () => {
                experimentarium.addConfig('test', { foo: 'bar' });
                experimentarium.submit();

                assert.strictEqual(experimentarium._getConfigProp('test', 'foo'), 'bar');
            });
        });

        describe('Auto pushbundle', () => {
            beforeEach(() => {
                experimentarium.registerExperimentalBlocks('test', { a: () => {} });
                experimentarium.submit();
                experimentarium.activate({ test: 1 });
            });

            it('should push evaluated', () => {
                blocks.a();

                assert.deepEqual(experimentarium.getBundlesToPush(), ['test']);
            });

            it('should not push not evaluated', () => {
                blocks.b();

                assert.deepEqual(experimentarium.getBundlesToPush(), []);
            });

            it('should not push configured as `false`', () => {
                experimentarium.addConfig('test', { bundleAutoPush: false });
                blocks.a();

                assert.deepEqual(experimentarium.getBundlesToPush(), []);
            });

            it('should work with deactivate', () => {
                blocks.a();

                experimentarium.deactivate();
                experimentarium.activate({ test: 1 });

                blocks.b();

                assert.deepEqual(experimentarium.getBundlesToPush(), []);
            });

            it('should push in lexicographical order', () => {
                experimentarium.deactivate();
                experimentarium.registerExperimentalBlocks('test2', { b: () => {} });
                experimentarium.submit();

                const flags = { test2: 1 };

                // флаг, который должен примениться первым, выставляем в последнюю очередь
                flags.test = 1;

                experimentarium.activate(flags);
                blocks.a();
                blocks.b();

                assert.deepEqual(experimentarium.getBundlesToPush(), ['test', 'test2']);
            });

            it('should push the last exp block when there are collisions', () => {
                experimentarium.deactivate();
                // теперь `a` переопределён для двух флагов, test и test2
                experimentarium.registerExperimentalBlocks('test2', { a: () => {} });
                experimentarium.submit();

                const flags = { test: 1, test2: 1 };

                experimentarium.activate(flags);
                blocks.a();

                assert.deepEqual(experimentarium.getBundlesToPush(), ['test2']);
            });
        });
    });

    describe('When experiments registered before corresponding original methods', () => {
        let localMethod;
        let localMethod2;

        beforeEach(() => {
            blocks = { 'wrap-block-try-catch': fn => fn };
            localMethod = sinon.stub();
            localMethod2 = sinon.stub();

            experimentarium = new Experimentarium(blocks);
            experimentarium.registerExperimentalBlocks('test', {
                a: localMethod
            });
            experimentarium.submit();

            blocks.a = a;
        });

        afterEach(() => {
            blocks = undefined;
            experimentarium = undefined;
        });

        it('should correctly activate', () => {
            experimentarium.activate({ test: 1 });
            blocks.a();

            assert.isTrue(localMethod.calledOnce, 'local method should be called');
            assert.isFalse(a.called, 'original method should not be called');
        });

        it('should correctly deactivate', () => {
            experimentarium.activate({ test: 1 });
            experimentarium.deactivate();
            blocks.a();

            assert.isTrue(a.calledOnce, 'original method should not be called');
            assert.isFalse(localMethod.called, 'local method should not be called');
        });

        it('should correctly deactivate even if multiple experiments were active', () => {
            experimentarium.registerExperimentalBlocks('test2', {
                a: localMethod2
            });
            experimentarium.submit();

            experimentarium.activate({ test: 1, test2: 1 });
            experimentarium.deactivate();
            blocks.a();

            assert.isTrue(a.calledOnce, 'original method should not be called');
            assert.isFalse(localMethod.called, 'local method should not be called');
            assert.isFalse(localMethod2.called, 'local method should not be called');
        });

        it('should correctly deactivate even if multiple experiments were active and base function does not exist',
            () => {
                assert.isUndefined(blocks.nonexistent_method, 'nonexistent method should be undefined initially');

                experimentarium.registerExperimentalBlocks('test', { nonexistent_method: localMethod });
                experimentarium.registerExperimentalBlocks('test2', { nonexistent_method: localMethod2 });
                experimentarium.submit();
                experimentarium.activate({ test: 1, test2: 1 });

                assert.isFunction(blocks.nonexistent_method, 'nonexistent method should be function after activate');

                experimentarium.deactivate();

                assert.isUndefined(blocks.nonexistent_method, 'nonexistent method should be undefined after deactivate');
            });
    });

    describe('Performance', function() {
        function doTest(OPERATIONS_BATCH, fn) {
            let operations = 0;
            let milliseconds;
            const startTime = Date.now();

            do {
                for (let i = 0; i < OPERATIONS_BATCH; i++) {
                    fn();
                }

                operations += OPERATIONS_BATCH;
                milliseconds = Date.now() - startTime;
            } while (milliseconds < 500);

            return { operations, milliseconds, speed: Math.round(1000 * milliseconds / operations) };
        }

        beforeEach(() => {
            blocks = { 'wrap-block-try-catch': fn => fn };
        });

        afterEach(() => {
            blocks = undefined;
            experimentarium = undefined;
        });

        it('addConfig + _getConfigProp', function() {
            const EXP_COUNT = 100;
            const OPERATIONS_BATCH = 1000;
            let result = doTest(OPERATIONS_BATCH, () => {
                experimentarium = new Experimentarium(blocks);
                for (let exp = 0; exp < EXP_COUNT; exp++) {
                    experimentarium.addConfig(`exp-${exp}`, { bundleAutoPush: false });
                }
                experimentarium.submit();
            });

            console.log(`  addConfig:\t ${result.speed}μs/op`);

            result = doTest(OPERATIONS_BATCH, () => {
                for (let exp = 0; exp < EXP_COUNT; exp++) {
                    experimentarium._getConfigProp(`exp-${exp}`, 'bundleAutoPush');
                }
            });

            console.log(`  _getConfigProp:\t ${result.speed}μs/op`);
        });

        it('sort', () => {
            const EXP_COUNT = 400;

            for (let i = 0; i < EXP_COUNT; i++) {
                blocks[`a-${i}`] = () => {};
            }

            experimentarium = new Experimentarium(blocks);

            const experiments = {};

            for (let i = EXP_COUNT - 1; i >= 0; i--) {
                const exp = `exp-${i}`;
                // заполняем в обратном порядке
                experiments[exp] = 1;
                if (i < 200) continue;
                experimentarium.registerExperimentalBlocks(exp, { [`a-${i}`]: () => {} });
            }

            experimentarium.submit();

            const OPERATIONS_BATCH = 100000;
            const { speed } = doTest(OPERATIONS_BATCH, () => {
                experimentarium.activate(experiments);
                experimentarium.getBundlesToPush();
            });

            console.log(`  sort:\t ${speed}μs/op`);
        });
    });
});
