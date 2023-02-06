import { assert } from 'chai';
import { describe, it, beforeEach } from 'mocha';
import { SinonSpy, stub, assert as sinonAssert } from 'sinon';

import base from './.registry/base';
import { adaptersExpRegistry, componentsExpRegistry, cssExperiments } from './.registry/exp';
import { AdapterTestFull } from './fixtures/AdapterTestFull';
import { AdapterTestRender } from './fixtures/AdapterTestRender';
import {
    IContext,
    ISnippet,
    IExternalSnippet,
    IRuntimeData,
} from '../../typings';
import { AdaptersRuntime, Assets, AssetsDevProvider } from '../..';

const PLATFORM = 'desktop';
let assets: Assets;
let assetsAddBaseSpy: SinonSpy;
let assetsAddExperimentalSpy: SinonSpy;
let assetsAddExperimentalClientStatic: SinonSpy;
let runtime: AdaptersRuntime<Record<string, string | number>, string, string | number>;
let context: IContext<Record<string, string | number>>;
let snippet: ISnippet;

describe('Runtime.select', () => {
    beforeEach(() => {
        assets = new Assets(PLATFORM, new AssetsDevProvider());

        assetsAddBaseSpy = stub(assets, 'addBase');
        assetsAddExperimentalSpy = stub(assets, 'addExperimental');
        assetsAddExperimentalClientStatic = stub(assets, 'addExperimentalClientStatic');

        runtime = new AdaptersRuntime<Record<string, string | number>, string, string | number>({
            adapters: {
                base,
                experiments: adaptersExpRegistry,
                componentsExperiments: componentsExpRegistry,
                cssExperiments,
            },
            platform: PLATFORM,
            assets,
        });
        context = { expFlags: {} };
    });

    describe('with unsupported input', () => {
        it('should not select adapter with name not from registry', () => {
            snippet = { type: 'fake-adapter' };
            const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

            assert.isUndefined(result);
        });

        it('should not select adapter if adapter returns null', () => {
            snippet = { type: 'test-empty' };
            const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

            assert.isUndefined(result);
        });

        it('should return undefined if snippet is undefined', () => {
            snippet.type = undefined;
            const dataset = runtime.select({ context, snippet }) as IRuntimeData<string>;

            assert.isUndefined(dataset);
        });

        it('should return undefined if snippet.type is invalid', () => {
            const invalidSnippet: IExternalSnippet = { type: ['fake-snippet'] };
            const dataset = runtime.select({
                context,
                snippet: invalidSnippet,
            }) as IRuntimeData<string>;

            assert.isUndefined(dataset);
        });
    });

    describe('meta', () => {
        it('should return meta for snippet only with type', () => {
            snippet = { type: 'test-render' };
            const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

            assert.deepEqual(result.meta, {
                type: 'test-render',
                subtype: undefined,
                rendered: true,
            });
        });

        it('should return meta for snippet with type and subtype', () => {
            snippet = { type: 'test-transform', subtype: 'sub' };
            const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

            assert.deepEqual(result.meta, {
                type: 'test-transform',
                subtype: 'sub',
                rendered: false,
            });
        });
    });

    describe('with supported input', () => {
        describe('ajax', () => {
            beforeEach(() => {
                snippet = { type: 'test-ajax' };
            });

            it('should call getSnippetForAjax and ajax methods', () => {
                const result = runtime.select(
                    { context, snippet, isAjax: true },
                ) as IRuntimeData<string>;

                assert.equal(result.data, 'ajax');
            });

            it('should call experimental ajax method', () => {
                context.expFlags.ajaxFlag = 1;

                const result = runtime.select(
                    { context, snippet, isAjax: true },
                ) as IRuntimeData<string>;

                assert.equal(result.data, 'exp-ajax');
            });

            it('should not select adapter without isAjax option', () => {
                const result = runtime.select({ context, snippet });

                assert.isUndefined(result);
            });

            it('should not create adapter instance when snippet from ajax is null', () => {
                const result = runtime.select({ context, snippet, preventAjaxMethod: true, isAjax: true });

                assert.isUndefined(result);
            });
        });

        describe('render', () => {
            beforeEach(() => {
                snippet = { type: 'test-render' };
            });

            it('data should contain render() result', () => {
                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'render');
            });

            it('data should contain render() result for subtype', () => {
                snippet = { ...snippet, subtype: 'sub' };

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'sub-render');
            });

            it('data should contain experimental render() result', () => {
                context.expFlags.renderFlag1 = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'render, exp-1(name: renderFlag1, val: 1)');
            });

            it('data should contain experimental render() result for subtype', () => {
                snippet = { ...snippet, subtype: 'sub' };
                context.expFlags.renderSubFlag = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'exp-sub-render');
            });

            it('data should contain render() result when transform method is implemented', () => {
                snippet = { type: 'test-full' };

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'full-render');
            });

            it('should apply exp with condition if condition returns true', () => {
                snippet = { type: 'test-render', subtype: 'condition' };

                context.expFlags.renderCondition = 1;
                context.expFlags.test_condition = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'render, exp(name: renderCondition, val: 1), __condition: true');
            });

            it('should not apply exp with condition if condition returns false', () => {
                snippet = { type: 'test-render', subtype: 'condition' };

                context.expFlags.renderCondition = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'render');
            });
        });

        describe('transform', () => {
            beforeEach(() => {
                snippet = { type: 'test-transform' };
            });

            it('data should contain transform() result', () => {
                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'transform');
            });

            it('data should contain transform() result for subtype', () => {
                snippet = { type: 'test-transform', subtype: 'sub' };

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'sub-transform');
            });

            it('data should contain experimental transform() result', () => {
                context.expFlags.transformFlag1 = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'transform, exp-1(name: transformFlag1, val: 1)');
            });

            it('data should contain experimental transform() result for subtype', () => {
                snippet = { ...snippet, subtype: 'sub' };
                context.expFlags.transformSubFlag = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(result.data, 'exp-sub-transform');
            });

            it('data should contain experimental transform() result when two experiments are on', () => {
                context.expFlags.transformFlag1 = 1;
                context.expFlags.transformFlag2 = 1;

                const result = runtime.select({ context, snippet }) as IRuntimeData<string>;

                assert.equal(
                    result.data,
                    'transform, ' +
                    'exp-1(name: transformFlag1, val: 1), ' +
                    'exp-2(name: transformFlag2, val: 1)',
                );
            });
        });
    });

    describe('assets', () => {
        it('should add base assets for base adapter', () => {
            snippet = { type: 'test-full' };
            runtime.select({ context, snippet });

            sinonAssert.calledOnce(assetsAddBaseSpy);
            sinonAssert.calledWith(assetsAddBaseSpy, AdapterTestFull);
        });

        it('should add base assets for exp adapter when base is not default', () => {
            snippet = { type: 'test-render' };
            context.expFlags.renderFlag1 = 1;

            runtime.select({ context, snippet });

            sinonAssert.calledOnce(assetsAddBaseSpy);
            sinonAssert.calledWith(assetsAddBaseSpy, AdapterTestRender);
        });

        it('should add exp assets for exp adapter', () => {
            snippet = { type: 'test-render' };
            context.expFlags.renderFlag1 = 1;
            context.expFlags.renderFlag2 = 1;

            runtime.select({ context, snippet });

            sinonAssert.calledTwice(assetsAddExperimentalSpy);

            const [firstCallAdapter, firstPushedModulesIds, firstCallExp] = assetsAddExperimentalSpy.getCall(0).args;
            const [secondCallAdapter, secondPushedModulesIds, secondCallExp] = assetsAddExperimentalSpy.getCall(1).args;

            assert.equal(firstCallAdapter.name, 'AdapterTestRender');
            assert.deepEqual(firstPushedModulesIds, []);
            assert.deepEqual(firstCallExp, { name: 'renderFlag1', val: 1 });

            assert.equal(secondCallAdapter.name, 'AdapterTestRender');
            assert.deepEqual(secondPushedModulesIds, []);
            assert.deepEqual(secondCallExp, { name: 'renderFlag2', val: 1 });
        });

        it('should not add base assets for exp adapter when base is default', () => {
            snippet = { type: 'test-only-experimental' };
            context.expFlags.onlyExperimentalFlag = 1;

            runtime.select({ context, snippet });

            sinonAssert.notCalled(assetsAddBaseSpy);
        });

        it('should add component exp assets', () => {
            snippet = { type: 'test-render' };
            context.expFlags.componentsFlag = 1;
            runtime.select({ context, snippet });

            sinonAssert.calledOnce(assetsAddExperimentalClientStatic);
            sinonAssert.calledWith(assetsAddExperimentalClientStatic, {
                name: 'componentsFlag',
                val: 1,
            });
        });

        it('should not add component exp assets if features is not matches', () => {
            snippet = { type: 'test-full' };
            context.expFlags.componentsFlag = 1;
            runtime.select({ context, snippet });

            sinonAssert.notCalled(assetsAddExperimentalClientStatic);
        });

        it('should add css-only exp styles', () => {
            snippet = { type: 'test-render' };
            context.expFlags.cssExpFlag = 1;
            runtime.select({ context, snippet });

            assert.isNotEmpty(assets.provideCssOnlyExperiments());
        });

        it('should add global css-only exp styles', () => {
            snippet = { type: 'test-full' };
            context.expFlags.globalCssExpFlag = 1;
            runtime.select({ context, snippet });

            assert.isNotEmpty(runtime.getGlobalCssExperiments(context, {}));
        });

        it('should correctly separate feature and global css-only exp styles', () => {
            snippet = { type: 'test-render' };
            context.expFlags.cssExpFlag = 1;
            context.expFlags.globalCssExpFlag = 1;
            runtime.select({ context, snippet });

            assert.isNotEmpty(assets.provideCssOnlyExperiments());
            assert.isNotEmpty(runtime.getGlobalCssExperiments(context, {}));
        });

        it('should add only "forced" asset', () => {
            snippet = { type: 'test-render' };

            // для теста нужно
            // - больше 2х адаптеров
            // - одинаковые имена функций-адаптеров
            context.expFlags.renderFlag = 1;
            context.expFlags.forcedAssets = 1;
            context.expFlags.nonForcedAssets = 1;

            runtime.select({ context, snippet });

            sinonAssert.calledOnce(assetsAddExperimentalSpy);

            const exp = assetsAddExperimentalSpy.getCall(0).args[2];

            assert.deepEqual(exp, { name: 'forcedAssets', val: 1 });
        });
    });
});
