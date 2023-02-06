import { assert } from 'chai';
import { beforeEach, describe, it } from 'mocha';
import { stub } from 'sinon';

import { AdapterTestRender } from '../Runtime/Runtime.test/fixtures/AdapterTestRender';
import { IExperimentMeta } from '../typings';
import { Assets } from './Assets';
import { AssetsDevProvider, IAssetsProvider } from './AssetsProvider';

let assets: Assets;
let provider: IAssetsProvider;

describe('Assets', () => {
    beforeEach(() => {
        provider = new AssetsDevProvider();
        assets = new Assets('desktop', provider);
    });

    describe('base assets', () => {
        beforeEach(() => {
            const stubbedProvider = stub(provider, 'getFeatureAssets');

            stubbedProvider
                .withArgs('features/TestRender@desktop')
                .returns([
                    { name: 'stub-js', js: 'stub-js' },
                    { name: 'stub-css', css: 'stub-css' },
                ]);

            stubbedProvider
                .withArgs('features/TestRender@desktop', ['pushed-chunk-a'])
                .returns([
                    { name: 'stub-pushed-a-js', js: 'stub-pushed-a-js' },
                    { name: 'stub-js', js: 'stub-js' },
                    { name: 'stub-css', css: 'stub-css' },
                ]);
        });

        it('should add new asset', () => {
            assert.isEmpty(assets.provideBaseJS());
            assert.isEmpty(assets.provideBaseCSS());
            assets.addBase(AdapterTestRender, []);

            assert.deepEqual(assets.provideBaseJS(), [{ name: 'stub-js', js: 'stub-js' }]);
            assert.deepEqual(assets.provideBaseCSS(), [{ name: 'stub-css', css: 'stub-css' }]);
        });

        it('should return empty array after clear', () => {
            assets.addBase(AdapterTestRender, []);
            assets.clear();

            assert.isEmpty(assets.provideBaseJS());
            assert.isEmpty(assets.provideBaseCSS());
        });

        it('should put pushed chunks before entrypoint when adapter is rendered multiple times', () => {
            assert.isEmpty(assets.provideBaseJS());

            assets.addBase(AdapterTestRender, []);
            assets.addBase(AdapterTestRender, ['pushed-chunk-a']);

            assert.deepEqual(assets.provideBaseJS(), [
                { name: 'stub-pushed-a-js', js: 'stub-pushed-a-js' },
                { name: 'stub-js', js: 'stub-js' },
            ]);
        });
    });

    describe('experimental assets', () => {
        const experimentData: IExperimentMeta = {
            name: 'renderFlag',
            val: 1,
        };

        beforeEach(() => {
            const stubbedProvider = stub(provider, 'getFeatureAssets');

            stubbedProvider
                .withArgs('experiments/renderFlag/TestRender@desktop')
                .returns([
                    { name: 'stub-js', js: 'stub-js' },
                    { name: 'stub-css', css: 'stub-css' },
                ]);

            stubbedProvider
                .withArgs('experiments/renderFlag/TestRender@desktop', ['pushed-chunk-a'])
                .returns([
                    { name: 'stub-pushed-a-js', js: 'stub-pushed-a-js' },
                    { name: 'stub-js', js: 'stub-js' },
                    { name: 'stub-css', css: 'stub-css' },
                ]);
        });

        it('should add new asset', () => {
            assert.isEmpty(assets.provideExperimentalJS());
            assert.isEmpty(assets.provideExperimentalCSS());
            assets.addExperimental(AdapterTestRender, [], experimentData);

            assert.deepEqual(assets.provideExperimentalJS(), [{ name: 'stub-js', js: 'stub-js' }]);
            assert.deepEqual(assets.provideExperimentalCSS(), [{ name: 'stub-css', css: 'stub-css' }]);
        });

        it('should clear all assets', () => {
            assets.addExperimental(AdapterTestRender, [], experimentData);
            assets.clear();

            assert.isEmpty(assets.provideExperimentalJS());
            assert.isEmpty(assets.provideExperimentalCSS());
        });

        it('should put pushed chunks before entrypoint when adapter is rendered multiple times', () => {
            assert.isEmpty(assets.provideBaseJS());

            assets.addExperimental(AdapterTestRender, [], experimentData);
            assets.addExperimental(AdapterTestRender, ['pushed-chunk-a'], experimentData);

            assert.deepEqual(assets.provideExperimentalJS(), [
                { name: 'stub-pushed-a-js', js: 'stub-pushed-a-js' },
                { name: 'stub-js', js: 'stub-js' },
            ]);
        });
    });

    describe('experimental client static assets', () => {
        const experimentData: IExperimentMeta = {
            name: 'renderFlag',
            val: 1,
        };

        beforeEach(() => {
            stub(provider, 'getClientStaticExperimentsAssets')
                .withArgs('components-experiments/renderFlag/desktop')
                .returns([
                    { name: 'stub-js', js: 'stub-js' },
                    { name: 'stub-css', css: 'stub-css' },
                ]);
        });

        it('should add new asset', () => {
            assert.isEmpty(assets.provideBaseJS());
            assert.isEmpty(assets.provideExperimentalCSS());
            assets.addExperimentalClientStatic(experimentData, []);

            assert.deepEqual(assets.provideExperimentalJS(), [{ name: 'stub-js', js: 'stub-js' }]);
            assert.isEmpty(assets.provideBaseCSS());
            assert.deepEqual(assets.provideExperimentalCSS(), [{ name: 'stub-css', css: 'stub-css' }]);
        });

        it('should clear all assets', () => {
            assets.addExperimentalClientStatic(experimentData, []);
            assets.clear();

            assert.isEmpty(assets.provideBaseJS());
        });
    });
});
