describe('lazy-image_type_bounds', function() {
    var block,
        bemjsonBase = {
            block: 'lazy-image',
            mods: { type: 'bounds' },
            js: true
        },
        itemsBase = [
            { elem: 'item', js: { src: 'http://1', type: 'bg' } },
            { elem: 'item', js: { src: 'http://2' } },
            { elem: 'item', js: { src: 'http://3' } },
            { elem: 'item', js: { src: 'http://4', type: 'bg' } }
        ];

    var verticalItems = itemsBase.map(function(item) {
            return $.extend({}, item, { attrs: { style: 'height: 100px; width: 100px;' } });
        }),
        horizontalItems = itemsBase.map(function(item) {
            return $.extend({}, item, { attrs: { style: 'height: 100px; width: 100px; display: inline-block;' } });
        }),
        lazyImageWithVerticalItems = $.extend({}, bemjsonBase, {
            content: verticalItems,
            attrs: {
                style: 'height: 200px; width: 100px; overflow: auto;'
            }
        }),
        lazyImageWithHorizontalItems = $.extend({}, bemjsonBase, {
            content: horizontalItems,
            attrs: {
                style: 'height: 100px; width: 200px; overflow: auto; white-space: nowrap;'
            }
        });

    function commonChecks() {
        it('should return true if lazy-image__item is within block bounds', function() {
            var elem = block.elem('item:nth-child(1)');
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.isTrue(block.__self.isWithinBounds(visibleRect, elem[0]));
        });

        it('should return false if lazy-image__item isn`t within block bounds', function() {
            var elem = block.elem('item:nth-child(3)');
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.notOk(block.__self.isWithinBounds(visibleRect, elem[0]));
        });
    }

    function commonVerticalChecks() {
        it('should return false if lazy-image__item isn`t within block bounds after vertical scroll', function() {
            var elem = block.elem('item:nth-child(1)');
            block.domElem.scrollTop(100);
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.notOk(block.__self.isWithinBounds(visibleRect, elem[0]));
        });

        it('should return true if lazy-image__item is within block bounds after vertical scroll', function() {
            var elem = block.elem('item:nth-child(3)');
            block.domElem.scrollTop(100);
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.isTrue(block.__self.isWithinBounds(visibleRect, elem[0]));
        });
    }

    function commonHorizontalChecks() {
        it('should return false if lazy-image__item isn`t within block bounds after horizontal scroll', function() {
            var elem = block.elem('item:nth-child(1)');
            block.domElem.scrollLeft(100);
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.notOk(block.__self.isWithinBounds(visibleRect, elem[0]));
        });

        it('should return true if lazy-image__item is within block bounds after horizontal scroll', function() {
            var elem = block.elem('item:nth-child(3)');
            block.domElem.scrollLeft(100);
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.isTrue(block.__self.isWithinBounds(visibleRect, elem[0]));
        });
    }

    describe('#isWithinBounds vertical items', function() {
        beforeEach(function() {
            block = buildDomBlock('lazy-image', lazyImageWithVerticalItems);
        });

        afterEach(function() {
            block.destruct();
            block = null;
        });

        commonChecks();
        commonVerticalChecks();
    });

    describe('#isWithinBounds shifted left', function() {
        var wrapper,
            wrapperjson = {
                block: 'lazy-image-wrapper',
                content: [lazyImageWithHorizontalItems],
                attrs: {
                    style: 'position: absolute; left: -100px;'
                }
            };

        beforeEach(function() {
            wrapper = buildDomBlock('lazy-image-wrapper', wrapperjson);
            block = wrapper.findBlockInside('lazy-image');
        });

        afterEach(function() {
            block.destruct();
            block = null;
        });

        it('should return true if lazy-image__item is within block bounds and viewport', function() {
            var elem = block.elem('item:nth-child(2)');
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.isTrue(block.__self.isWithinBounds(visibleRect, elem[0]));
        });

        it('should return false if lazy-image__item isn`t within viewport', function() {
            var elem = block.elem('item:nth-child(1)');
            var visibleRect = block._getVisibleRect(block.domElem[0].getBoundingClientRect());

            assert.notOk(block.__self.isWithinBounds(visibleRect, elem[0]));
        });
    });

    describe('#isWithinBounds horizontal items', function() {
        beforeEach(function() {
            block = buildDomBlock('lazy-image', lazyImageWithHorizontalItems);
        });

        afterEach(function() {
            block.destruct();
            block = null;
        });

        commonChecks();
        commonHorizontalChecks();
    });

    describe('#isWithinBounds shifted top', function() {
        var wrapper,
            wrapperjson = {
                block: 'lazy-image-wrapper',
                content: lazyImageWithVerticalItems,
                attrs: {
                    style: 'position: absolute; top: 500px;'
                }
            };

        beforeEach(function() {
            wrapper = buildDomBlock('lazy-image-wrapper', wrapperjson);
            block = wrapper.findBlockInside('lazy-image');
        });

        afterEach(function() {
            block.destruct();
            block = null;
        });

        commonChecks();
        commonVerticalChecks();
    });
});
