import { assert } from 'chai';
import { stub } from 'sinon';
import { beforeEach, describe, it } from 'mocha';
import { ClientStaticExpAssetsArray } from './ClientStaticExpAssetsArray';
import { AssetsDevProvider, IAssetsProvider } from './AssetsProvider';

const provider: IAssetsProvider = new AssetsDevProvider();

const getClientStaticExperimentsAssetsStub = stub(provider, 'getClientStaticExperimentsAssets');
getClientStaticExperimentsAssetsStub
    .withArgs('features/TestRender2@desktop')
    .returns([{ name: 'c0', css: 'base-css-0', js: 'base-js-0' }]);
getClientStaticExperimentsAssetsStub
    .withArgs('features/TestRender3@desktop')
    .returns([{ name: 'c1', css: 'base-css-1', js: 'base-js-1' }]);

let assetsArray: ClientStaticExpAssetsArray;

describe('ClientStaticExpAssetsArray', () => {
    beforeEach(() => {
        assetsArray = new ClientStaticExpAssetsArray(provider);
    });

    describe('add', () => {
        it('should preserve order', () => {
            assetsArray.add('features/TestRender3@desktop');
            assetsArray.add('features/TestRender2@desktop');
            assetsArray.add('features/TestRender3@desktop');

            assert.deepEqual(assetsArray.provideCSS(), [{ name: 'c1-css', css: 'base-css-1' }, { name: 'c0-css', css: 'base-css-0' }]);
            assert.deepEqual(assetsArray.provideJS(), [{ name: 'c1-js', js: 'base-js-1' }, { name: 'c0-js', js: 'base-js-0' }]);
        });
    });
});
