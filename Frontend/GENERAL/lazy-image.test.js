describe('lazy-image', function() {
    var block,
        bemjson = {
            block: 'lazy-image',
            js: true,
            content: [
                { elem: 'item', js: { src: 'http://1', type: 'bg' } },
                { elem: 'item', js: { src: 'http://2' } },
                { elem: 'item', js: { src: 'http://3' } },
                { elem: 'item', js: { src: 'http://4', type: 'bg' } }
            ]
        };

    beforeEach(function() {
        block = buildDomBlock('lazy-image', bemjson);
        block._imagesState = [];
        block._loadedCount = 0;
    });

    describe('#isLoaded', function() {
        it('should return true if lazy-image__item has mod loaded_yes', function() {
            block._imagesState[0] = true;

            assert.isTrue(block.__self.isLoaded(block, 0));
        });

        it('should return false if lazy-image__item doesn`t have mod loaded_yes', function() {
            var elem = block.elem('item')[0];

            assert.notOk(block.__self.isLoaded(block, elem));
        });
    });

    describe('#isAllLoaded', function() {
        it('should check if all items loaded', function() {
            block._loadedCount = 4;

            assert.isTrue(block.__self.isAllLoaded(block));
        });
    });

    describe('#loadImage', function() {
        it('should load passed element image', function() {
            var elem = block.elem('item')[0];

            block.__self.loadImage(block, elem, 0);

            assert.isTrue(block.__self.isLoaded(block, 0));
        });

        it('should set background-image if item type equals bg', function() {
            var elem = block.elem('item')[0];

            block.__self.loadImage(block, elem, 0);

            assert.equal(elem.style.backgroundImage, 'url(http://1/)');
        });

        it('should set src if item type not specified', function() {
            var elem = block.elem('item')[1];

            block.__self.loadImage(block, elem, 1);

            assert.equal(elem.src, 'http://2');
        });
    });

    describe('#loadAllImages', function() {
        it('should load all images in container', function() {
            block.__self.loadAllImages(block);

            assert.isTrue(block.__self.isAllLoaded(block));
        });
    });

    describe('#loadNextNth', function() {
        it('should load passed number of images', function() {
            block.__self.loadNextNth(block, 2);

            assert.isTrue(block.__self.isLoaded(block, 0), '1 image should be loaded');
            assert.isTrue(block.__self.isLoaded(block, 1), '2 image should be loaded');
            assert.notOk(block.__self.isLoaded(block, 2), '3 image should not be loaded');
            assert.notOk(block.__self.isLoaded(block, 3), '4 image should not be loaded');
        });
    });

    describe('#findNotLoadedElem', function() {
        it('should return first not loaded elem', function() {
            block.__self.loadImage(block, block.elem('item')[0], 0);

            assert.equal(block.__self.findNotLoadedElem(block), block.elem('item')[1]);
        });

        it('should return first not loaded elem + offset', function() {
            block.__self.loadImage(block, block.elem('item')[0], 0);

            assert.equal(block.__self.findNotLoadedElem(block, 1), block.elem('item')[2]);
        });
    });
});
