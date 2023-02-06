import { describe, it, beforeEach } from 'mocha';
import { assert } from 'chai';
import { stub } from 'sinon';
import { AssetsArray } from './AssetsArray';
import { AssetsDevProvider, IAssetsProvider } from './AssetsProvider';

const provider: IAssetsProvider = new AssetsDevProvider();

const getFeatureAssetsStub = stub(provider, 'getFeatureAssets');
getFeatureAssetsStub
    .withArgs('features/TestRender1@desktop', [])
    .returns([
        { name: 'j0', js: 'base-js-0' },
        { name: 'c0', css: 'base-css-0' },
    ]);
getFeatureAssetsStub
    .withArgs('features/TestRender2@desktop', [])
    .returns([{ name: 'c0', css: 'base-css-0', js: 'base-js-0' }]);
getFeatureAssetsStub
    .withArgs('features/TestRender3@desktop', [])
    .returns([{ name: 'c1', css: 'base-css-1', js: 'base-js-1' }]);

let assetsArray: AssetsArray;

describe('AssetsArray', () => {
    beforeEach(() => {
        assetsArray = new AssetsArray(provider);
    });

    describe('add', () => {
        it('should add asset', () => {
            assetsArray.add('features/TestRender1@desktop', { pushedChunks: [] });

            assert.deepEqual(assetsArray.provideCSS(), [{ name: 'c0', css: 'base-css-0' }]);
            assert.deepEqual(assetsArray.provideJS(), [{ name: 'j0', js: 'base-js-0' }]);
        });

        it('should add asset only once', () => {
            assetsArray.add('features/TestRender1@desktop', { pushedChunks: [] });
            assetsArray.add('features/TestRender1@desktop', { pushedChunks: [] });

            assert.lengthOf(assetsArray.provideCSS(), 1);
            assert.lengthOf(assetsArray.provideJS(), 1);
        });

        it('should add and split js&css asset', () => {
            assetsArray.add('features/TestRender2@desktop', { pushedChunks: [] });

            assert.deepEqual(assetsArray.provideCSS(), [{ name: 'c0-css', css: 'base-css-0' }]);
            assert.deepEqual(assetsArray.provideJS(), [{ name: 'c0-js', js: 'base-js-0' }]);
        });

        it('should preserve order', () => {
            assetsArray.add('features/TestRender3@desktop', { pushedChunks: [] });
            assetsArray.add('features/TestRender3@desktop', { pushedChunks: [] });
            assetsArray.add('features/TestRender2@desktop', { pushedChunks: [] });

            assert.deepEqual(assetsArray.provideCSS(), [{ name: 'c1-css', css: 'base-css-1' }, { name: 'c0-css', css: 'base-css-0' }]);
            assert.deepEqual(assetsArray.provideJS(), [{ name: 'c1-js', js: 'base-js-1' }, { name: 'c0-js', js: 'base-js-0' }]);
        });
    });

    it('should clear assets', () => {
        assetsArray.add('features/TestRender1@desktop', { pushedChunks: [] });

        assetsArray.clearCSS();
        assert.lengthOf(assetsArray.provideCSS(), 0);

        assetsArray.clearJS();
        assert.lengthOf(assetsArray.provideJS(), 0);

        assetsArray.add('features/TestRender1@desktop', { pushedChunks: [] });

        assetsArray.clear();
        assert.lengthOf(assetsArray.provideCSS(), 0);
        assert.lengthOf(assetsArray.provideJS(), 0);
    });
});
