/* eslint-disable @typescript-eslint/no-explicit-any */
import { assert } from 'chai';
import { AdapterImageSnippetOne } from './ImageSnippetOne@touch-phone.server';
import { AdapterImageSnippetOne as AdapterImageSnippetOneDesktop } from './ImageSnippetOne@desktop.server';

describe('ImageSnippetOne.src', function() {
    describe('Avatars source:', function() {
        let thumb: any;
        let adapter: any;

        beforeEach(function() {
            adapter = Object.create(AdapterImageSnippetOne.prototype);
            thumb = {
                height: '1856',
                orig: 'https://avatars.mds.yandex.net/get-snippets_images/51761/f03302c3d36d8f419310b410f571e6f0/orig',
                source_type: 'avatars',
                width: '2784',
            };
        });

        it('should return non-retina version of image url by default', function() {
            const url = adapter.src(thumb, 100, {});

            assert.equal(url, 'https://avatars.mds.yandex.net/get-snippets_images/51761/f03302c3d36d8f419310b410f571e6f0/square_108');
        });

        it('should return retina version of image url if pass retina=true', function() {
            const url = adapter.src(thumb, 100, { retina: true });

            assert.equal(url, 'https://avatars.mds.yandex.net/get-snippets_images/51761/f03302c3d36d8f419310b410f571e6f0/square_216');
        });

        it('should add shower params to image url if pass canShower=true', function() {
            thumb.shower = true;

            const url = adapter.src(thumb, 100, { canShower: true });

            assert.include(url, '_shower');
        });

        it('should not add shower params to image url by default', function() {
            thumb.shower = true;

            const url = adapter.src(thumb, 100, {});

            assert.notInclude(url, '_shower');
        });
    });

    describe('Thumbnail source:', function() {
        let thumb: any;
        let adapter: any;

        beforeEach(function() {
            adapter = Object.create(AdapterImageSnippetOne.prototype);
            thumb = {
                height: 320,
                id: '9010e11a12c3dcc42b94a8c055157783',
                source_type: 'thumbnail',
                width: 427,
            };
        });

        it('should return non-retina version of image url by default', function() {
            const url = adapter.src(thumb, 100, {});

            assert.equal(url, '//im2-tub-com.yandex.net/i?id=9010e11a12c3dcc42b94a8c055157783&n=33&h=100&ref=imgsnip');
        });

        it('should return retina version of image url if pass retina=true', function() {
            const url = adapter.src(thumb, 100, { retina: true });

            assert.equal(url, '//im2-tub-com.yandex.net/i?id=9010e11a12c3dcc42b94a8c055157783&n=33&h=200&ref=imgsnip');
        });

        it('should add shower params to image url if pass canShower=true', function() {
            thumb.shower = 2;

            const url = adapter.src(thumb, 100, { canShower: true });

            assert.include(url, 'shower=2');
        });

        it('should not add shower params to image url by default', function() {
            thumb.shower = 2;

            const url = adapter.src(thumb, 100, {});

            assert.notInclude(url, 'shower=2');
        });
    });
});

describe('ImageSnippetOne.src @desktop', function() {
    describe('Avatars source:', function() {
        let thumb: any;
        let adapter: any;

        beforeEach(function() {
            adapter = Object.create(AdapterImageSnippetOneDesktop.prototype);
            thumb = {
                height: '1856',
                orig: 'https://avatars.mds.yandex.net/get-snippets_images/51761/f03302c3d36d8f419310b410f571e6f0/orig',
                source_type: 'avatars',
                width: '2784',
            };
        });

        it('should return retina version of image url if pass retina=true', function() {
            const url = adapter.src(thumb, 100, { retina: true });

            assert.equal(url, 'https://avatars.mds.yandex.net/get-snippets_images/51761/f03302c3d36d8f419310b410f571e6f0/square_166');
        });

        it('should return non-retina version of image url by default', function() {
            const url = adapter.src(thumb, 100, {});

            assert.equal(url, 'https://avatars.mds.yandex.net/get-snippets_images/51761/f03302c3d36d8f419310b410f571e6f0/square_83');
        });
    });
});
