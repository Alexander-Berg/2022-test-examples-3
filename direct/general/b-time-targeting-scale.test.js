describe('b-time-targeting-scale', function() {
    var block,
        clock;

    function getBlock() {
        var tree = u.getDOMTree({
            block: 'b-time-targeting-scale',
            value: 100
        });

        return $(tree).bem('b-time-targeting-scale');
    }

    beforeEach(function() {
        block = getBlock();
        clock = sinon.useFakeTimers();
    });

    afterEach(function() {
        BEM.DOM.destruct(block.domElem)
        clock.restore();
    });

    describe('Допустим шкала выбора уровня цен показывается', function() {
        beforeEach(function() {
            block.setMod('visible', 'yes');
        });

        describe('Если кликнуть по значению шкалы', function() {
            it('То должно сработать событие change с выбранным значением', function(done) {
                block.on('change', function(e, data) {
                    expect(data).to.be.equal(50);
                    done();
                });

                $(block.elem('item').get(5)).trigger('pointerclick');
            });

            it('То должен установиться данный уровень', function() {
                $(block.elem('item').get(4)).trigger('pointerclick');

                expect(block.hasMod($(block.elem('item').get(4)), 'selected', 'yes')).to.be.equal(true);
            });
        });

        describe('Если мы вызовем метода setLevel с одним аргументом', function() {
            it('То должен установиться данный уровень', function() {
                block.setLevel(70);
                expect(block.hasMod($(block.elem('item').get(3)), 'selected', 'yes')).to.be.equal(true);
            });

            it('То должно сработать событие change c данным значением', function(done) {
                block.on('change', function(e, data) {
                    expect(data).to.be.equal(70);
                    done()
                });

                block.setLevel(70);
            });
        });

        describe('Если мы вызовем метода setLevel с вторым аргументом true', function() {
            it('То должен установиться данный уровень', function() {
                block.setLevel(30);
                expect(block.hasMod($(block.elem('item').get(7)), 'selected', 'yes')).to.be.equal(true);
            });

            it('То не должно сработать событие change c данным значением', function() {
                var spy = sinon.spy();

                block.on('change', spy);

                block.setLevel(90, true);

                clock.tick(5);

                expect(spy.called).to.be.equal(false);
            });
        });
    });

    describe('Допустим шкала выбора уровня цен не показывается', function() {
        beforeEach(function() {
            block.setMod('visible', '');
        });

        describe('Если кликнуть по значению шкалы', function() {
            it('То не должно сработать событие change с выбранным значением', function() {
                var spy = sinon.spy();

                block.on('change', spy);

                $(block.elem('item').get(5)).trigger('pointerclick');

                clock.tick(5);

                expect(spy.called).to.be.equal(false);
            });
        });
    });
});
