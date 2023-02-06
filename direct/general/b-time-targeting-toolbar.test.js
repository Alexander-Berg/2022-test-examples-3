describe('b-time-targeting-toolbar', function() {
    var block;

    function getBlock() {
        return u.getInitedBlock({
            block: 'b-time-targeting-toolbar',
            value: 100
        }, true);
    }

    beforeEach(function() {
        block = getBlock();
    });

    afterEach(function() {
        BEM.DOM.destruct(block.domElem);
    });

    describe('Допустим', function() {
        describe('Если кликнуть на "все"', function() {
            it('То должно сработать событие selected со значением "all"', function(done) {
                block.on('selected', function(e, data) {
                    expect(data).to.be.equal('all');
                    done();
                });

                block.findBlockOn($(block.elem('quick-select').get(0)), 'link').trigger('click');
            });
        });

        describe('Если кликнуть на "рабочее время"', function() {
            it('То должно сработать событие selected со значением "workdays"', function(done) {
                block.on('selected', function(e, data) {
                    expect(data).to.be.equal('workdays');
                    done();
                });

                block.findBlockOn($(block.elem('quick-select').get(1)), 'link').trigger('click');
            });
        });

        describe('Если произошел `setTotalHours` со значение оставшихся часов и признаком из валидности', function() {
            it('То у элемента `total-count` должен установиться заданный контент', function() {
                block.setTotalHours(1000);

                expect(block.elem('total-count').text()).to.be.equal('1000');
            });

            it('То у элемента `total` должен установиться модификатор соответсвующий признаку валидности значения', function() {
                block.setTotalHours(1000, false);

                expect(block).to.haveMod('total', 'error', 'yes');
            });
        });
    });
});
