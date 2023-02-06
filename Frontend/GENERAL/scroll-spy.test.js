describe('scroll-spy', function() {
    var sandbox,
        win, body, head, block, hiddenElem, detachedElem, emptyCollection, spacer;

    before(function() {
        win = $(window);
        body = $(document.body);
        head = $(document.head);

        emptyCollection = $();
        detachedElem = $('<div />', {
            style: 'width: 100px; height: 100px'
        });

        // Используем prependTo вместо appendTo, чтобы не было влияния вывода отчёта по тестам при просмотре в браузере.
        hiddenElem = $('<div />', {
            style: 'width: 100px; height: 100px; position: absolute; top: -100px; left: -100px;'
        }).prependTo(body);

        spacer = $('<div />', {
            style: 'width: 100px; height: 100px'
        }).prependTo(body);

        block = BEM.blocks['scroll-spy'];
    });

    after(function() {
        hiddenElem.remove();
        spacer.remove();
    });

    beforeEach(function() {
        sandbox = sinon.createSandbox();
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('#getElemVisibility()', function() {
        it('should return correct visibility for visible elements', function() {
            var visibility = block.getElemVisibility(spacer);

            assert.property(visibility, 'visible');
            assert.property(visibility, 'x');
            assert.property(visibility, 'y');
            assert.property(visibility, 'area');

            assert.propertyVal(visibility, 'visible', true);
            assert.operator(visibility.x, '>', 0);
            assert.operator(visibility.y, '>', 0);
            assert.operator(visibility.area, '>', 0);
        });

        it('should return correct visibility when elem has null dimensions', function() {
            var visibility = block.getElemVisibility(head);

            assert.propertyVal(visibility, 'visible', false);
            assert.propertyVal(visibility, 'y', 0);
            assert.propertyVal(visibility, 'area', 0);
        });

        it('should return correct visibility when elem is not in viewport', function() {
            var visibility = block.getElemVisibility(hiddenElem);

            assert.propertyVal(visibility, 'visible', false);
            assert.propertyVal(visibility, 'x', 0);
            assert.propertyVal(visibility, 'y', 0);
            assert.propertyVal(visibility, 'area', 0);
        });

        it('should return false when elem is detached', function() {
            var visibility = block.getElemVisibility(detachedElem);

            assert.propertyVal(visibility, 'visible', false);
            assert.propertyVal(visibility, 'x', 0);
            assert.propertyVal(visibility, 'y', 0);
            assert.propertyVal(visibility, 'area', 0);
        });

        it('should return false when elem is empty collection', function() {
            var visibility = block.getElemVisibility(emptyCollection);

            assert.propertyVal(visibility, 'visible', false);
            assert.propertyVal(visibility, 'x', 0);
            assert.propertyVal(visibility, 'y', 0);
            assert.propertyVal(visibility, 'area', 0);
        });
    });

    describe('#getIntersection()', function() {
        var cases = [
            [
                { top: 0, right: 500, bottom: 500, left: 0 },
                { top: 100, right: 200, bottom: 200, left: 100 },
                { xOverlap: 100, yOverlap: 100, overlapArea: 10000 }
            ],
            [
                { top: 300, right: 500, bottom: 500, left: 300 },
                { top: 100, right: 400, bottom: 400, left: 100 },
                { xOverlap: 100, yOverlap: 100, overlapArea: 10000 }
            ],
            [
                { top: 100, right: 500, bottom: 500, left: 100 },
                { top: -100, right: 400, bottom: 400, left: -100 },
                { xOverlap: 300, yOverlap: 300, overlapArea: 90000 }
            ],
            [
                { top: 0, right: 500, bottom: 500, left: 0 },
                { top: 400, right: 600, bottom: 600, left: 400 },
                { xOverlap: 100, yOverlap: 100, overlapArea: 10000 }
            ],
            [
                { top: 300, right: 500, bottom: 500, left: 300 },
                { top: -100, right: -400, bottom: -400, left: -100 },
                { xOverlap: 0, yOverlap: 0, overlapArea: 0 }
            ]
        ];

        cases.forEach(function(data, idx) {
            it('should calc intersection, case ' + idx, function() {
                var res = block.getIntersection(data[0], data[1]);

                assert.deepEqual(res, data[2]);
            });
        });
    });

    describe('#watchElemVisibility()', function() {
        describe('when checking arguments', function() {
            it('should throw error when no domElem passed', function() {
                assert.throws(function() {
                    block.watchVisibility();
                }, 'Dom elem required');
            });

            it('should throw error when no callback passed', function() {
                assert.throws(function() {
                    block.watchVisibility(body);
                }, 'Callback required');
            });

            it('should throw error when empty collection passed', function() {
                assert.throws(function() {
                    block.watchVisibility(emptyCollection, $.noop);
                }, "Can't watch empty collection");
            });
        });

        describe('when elem is in viewport', function() {
            afterEach(function() {
                block.stopWatchingVisibility(body);
            });

            it('should pass true with scroll event', function(done) {
                var cb = sinon.spy(function(visibility) {
                    assert.property(visibility, 'visible');
                    assert.property(visibility, 'x');
                    assert.property(visibility, 'y');
                    assert.property(visibility, 'area');

                    assert.propertyVal(visibility, 'visible', true);
                    assert.operator(visibility.x, '>', 0);
                    assert.operator(visibility.y, '>', 0);
                    assert.operator(visibility.area, '>', 0);
                    done();
                });
                block.watchVisibility(body, cb);

                win.trigger('scroll');
            });
        });

        describe('when elem has null dimensions', function() {
            afterEach(function() {
                block.stopWatchingVisibility(head);
            });

            it('should pass false with scroll event', function(done) {
                var cb = sinon.spy(function(visibility) {
                    assert.propertyVal(visibility, 'visible', false);
                    assert.propertyVal(visibility, 'x', 0);
                    assert.propertyVal(visibility, 'y', 0);
                    assert.propertyVal(visibility, 'area', 0);
                    done();
                });
                block.watchVisibility(head, cb);

                win.trigger('scroll');
            });
        });

        describe('when elem not in viewport', function() {
            afterEach(function() {
                block.stopWatchingVisibility(hiddenElem);
            });

            it('should pass false with scroll event', function(done) {
                var cb = sinon.spy(function(visibility) {
                    assert.propertyVal(visibility, 'visible', false);
                    assert.propertyVal(visibility, 'x', 0);
                    assert.propertyVal(visibility, 'y', 0);
                    assert.propertyVal(visibility, 'area', 0);
                    done();
                });
                block.watchVisibility(hiddenElem, cb);

                win.trigger('scroll');
            });
        });

        describe('when elem detached', function() {
            afterEach(function() {
                block.stopWatchingVisibility(detachedElem);
            });

            it('should pass false with scroll event', function(done) {
                var cb = sinon.spy(function(visibility) {
                    assert.propertyVal(visibility, 'visible', false);
                    assert.propertyVal(visibility, 'x', 0);
                    assert.propertyVal(visibility, 'y', 0);
                    assert.propertyVal(visibility, 'area', 0);
                    done();
                });
                block.watchVisibility(detachedElem, cb);

                win.trigger('scroll');
            });
        });
    });

    describe('#isElemWatched()', function() {
        afterEach(function() {
            block.stopWatchingVisibility(body);
        });

        it('should return true when elem watched with specific callback', function() {
            var cb = $.noop;
            block.watchVisibility(body, cb);

            assert.ok(block.isElemWatched(body, cb), 'Body must be watched');
        });

        it('should return false when elem is not watched', function() {
            assert.notOk(block.isElemWatched(head, $.noop), 'Head must be not watched');
        });

        it('should return false when elem is empty collection', function() {
            assert.notOk(block.isElemWatched(emptyCollection, $.noop), 'Empty collection must be not watched');
        });
    });

    describe('#stopWatchingVisibility()', function() {
        it('should remove watching when elem watched with specific callback', function() {
            var cb = $.noop;
            block.watchVisibility(body, cb);
            assert.ok(block.isElemWatched(body, cb), 'Body must be watched');

            block.stopWatchingVisibility(body, cb);
            assert.notOk(block.isElemWatched(body, cb), 'Body must not be watched');
        });

        it('should remove watching when elem watched without callback', function() {
            var cb = $.noop;
            block.watchVisibility(body, cb);
            assert.ok(block.isElemWatched(body, cb), 'Body must be watched');

            block.stopWatchingVisibility(body);
            assert.notOk(block.isElemWatched(body, cb), 'Body must not be watched');
        });

        it('should not fail when called without arguments', function() {
            assert.doesNotThrow(function() {
                block.stopWatchingVisibility(null);
            });
        });
    });

    describe('#checkVisibilityConditions()', function() {
        describe('when checking one field', function() {
            it('should return false if not visible', function() {
                var condition = {},
                    visibility = { visible: false };
                assert.isFalse(block.checkVisibilityConditions(condition, visibility));
            });

            it('should return true if visible', function() {
                var condition = {},
                    visibility = { visible: true };
                assert.isTrue(block.checkVisibilityConditions(condition, visibility));
            });

            it('should return true if visibility gt condition', function() {
                var condition = { area: 0.1 },
                    visibility = { visible: true, area: 0.2 };
                assert.isTrue(block.checkVisibilityConditions(condition, visibility));
            });

            it('should return true if visibility equal condition', function() {
                var condition = { area: 0.1 },
                    visibility = { visible: true, area: 0.1 };
                assert.isTrue(block.checkVisibilityConditions(condition, visibility));
            });

            it('should return false if visibility less than condition', function() {
                var condition = { area: 0.1 },
                    visibility = { visible: true, area: 0.05 };
                assert.isFalse(block.checkVisibilityConditions(condition, visibility));
            });
        });

        describe('complex check', function() {
            it('should return true if all fields matches conditions', function() {
                var condition = { x: 0.1, y: 0.1, area: 0.1 },
                    visibility = { visible: true, x: 0.2, y: 0.1, area: 0.15 };
                assert.isTrue(block.checkVisibilityConditions(condition, visibility));
            });

            it('should return false if just one field not matches conditions', function() {
                var condition = { x: 0.1, y: 0.1, area: 0.1 },
                    visibility = { visible: true, x: 0.2, y: 0.05, area: 0.15 };
                assert.isFalse(block.checkVisibilityConditions(condition, visibility));
            });
        });
    });

    describe('#notifyBecameVisible()', function() {
        afterEach(function() {
            block.stopWatchingVisibility(body);
        });

        it('should notify once if block is visible at start', function() {
            var cb = sinon.spy(),
                ctx = {},
                clock = sandbox.useFakeTimers();

            block.notifyBecameVisible(body, cb, ctx);
            clock.tick(1);

            assert.calledOnce(cb);
            assert.notOk(block.isElemWatched(body, cb, ctx), 'Body must not be watched');
        });

        it('should notify once and stop watching if block became visible later', function(done) {
            var elem = hiddenElem.clone().prependTo(body),
                ctx = {},
                cb = sinon.spy(function() {
                    assert.calledOn(cb, ctx);
                    assert.notOk(block.isElemWatched(body, cb, ctx), 'Body must not be watched');

                    done();
                }),

                clock = sandbox.useFakeTimers();

            block.notifyBecameVisible(elem, cb, ctx);
            clock.tick(1);
            clock.restore();

            assert.notCalled(cb);

            elem.css({ top: 0, left: 0 });
            win.trigger('scroll');
        });
    });

    describe('instance.onFirstBecameVisible()', function() {
        var instance;

        afterEach(function() {
            if (instance) {
                BEM.DOM.destruct(instance.domElem);
                instance = null;
            }
        });

        it('should call notifyBecameVisible() with correct arguments', function() {
            var cb = $.noop,
                ctx = {};
            sandbox.stub(block, 'notifyBecameVisible');

            instance = BEM.DOM.prepend(body, BEMHTML.apply({
                block: 'scroll-spy',
                js: { visibilityConditions: { area: 0.1, x: 0.2, y: 0.3 } }
            })).bem('scroll-spy');

            instance.onFirstBecameVisible(cb, ctx);

            assert.calledOnce(block.notifyBecameVisible);

            var args = block.notifyBecameVisible.args[0];
            assert.propertyVal(args, '0', instance.domElem);
            assert.propertyVal(args, '1', cb);
            assert.propertyVal(args, '2', ctx);

            assert.nestedPropertyVal(args, '3.area', 0.1);
            assert.nestedPropertyVal(args, '3.x', 0.2);
            assert.nestedPropertyVal(args, '3.y', 0.3);
        });
    });
});
