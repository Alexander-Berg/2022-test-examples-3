import { assert } from 'chai';
import { beforeEach, describe, it } from 'mocha';

import { AssetsJsonProvider, IAssetsProvider } from './AssetsProvider';

const assetsJson = {
    main: 'main-js-url',
    entries: {
        'features/TestRender/TestRender@desktop': {
            main: { ru: '0' },
            chunks: ['1', '2'],
        },
        'experiments/renderFlag/TestRender/TestRender@desktop': {
            main: { ru: '1' },
            chunks: ['2'],
        },
    },
    chunks: {
        '0': [{ name: 'c0', js: 'c0-js' }],
        '1': [{ name: 'c1', js: 'c1-js' }],
        '2': [{ name: 'c2', css: 'c2-css' }],
        chunk1: [{ name: 'chunk1', js: 'chunk1-js-1' }],
        chunk2: [{ name: 'chunk2', css: 'chunk2-css-2' }],
        'module1-base-chunk': [{ name: 'module1-base-chunk', js: 'module1-base-chunk-js-3' }],
        'module1-dependent-chunk': [{ name: 'module1-dependent-chunk', js: 'module1-dependent-chunk-js-4' }],
        'module2-base-chunk': [{ name: 'module2-base-chunk', js: 'module2-base-chunk-css-5' }],
        'module2-dependent-chunk-1': [{ name: 'module2-dependent-chunk-1', js: 'module2-dependent-chunk-1-js-6' }],
        'module2-dependent-chunk-2': [{ name: 'module2-dependent-chunk-2', js: 'module2-dependent-chunk-2-css-7' }],
    },
    componentsExperiments: {
        'components-experiments/renderFlag/desktop': {
            chunks: ['1', '2'],
        },
    },
};

describe('AssetsProvider', () => {
    describe('AssetsJsonProvider', () => {
        let provider: IAssetsProvider;

        beforeEach(() => {
            provider = new AssetsJsonProvider(assetsJson);
        });

        it('should return main-chunk asset', () => {
            const actual = provider.getMainChunk();
            const expected = {
                name: 'main-react-js',
                js: {
                    url: 'main-js-url',
                },
            };

            assert.deepEqual(actual, expected);
        });

        it('should return feature assets', () => {
            const actual = provider.getFeatureAssets('features/TestRender/TestRender@desktop', []);
            const expected = [
                { name: 'c1', js: 'c1-js' },
                { name: 'c2', css: 'c2-css' },
                { name: 'c0', js: 'c0-js' },
            ];

            assert.deepEqual(actual, expected);
        });

        it('should return experimental feature assets', () => {
            const actual = provider.getFeatureAssets('experiments/renderFlag/TestRender/TestRender@desktop', []);
            const expected = [
                { name: 'c2', css: 'c2-css' },
                { name: 'c1', js: 'c1-js' },
            ];

            assert.deepEqual(actual, expected);
        });

        it('should return experimental components assets', () => {
            const actual = provider.getClientStaticExperimentsAssets('components-experiments/renderFlag/desktop', []);
            const expected = [
                { name: 'c1', js: 'c1-js' },
                { name: 'c2', css: 'c2-css' },
            ];

            assert.deepEqual(actual, expected);
        });
    });
});
