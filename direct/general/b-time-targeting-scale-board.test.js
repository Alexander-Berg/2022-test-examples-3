describe('b-time-targeting-scale-board', function() {
    var block;

    function getBlock(isChecked) {
        var tree = u.getDOMTree({
            block: 'b-time-targeting-scale-board',
            content: [
                { elem: 'hour', elemMods: { code: 'A', day: 1 } },
                { elem: 'hour', elemMods: { code: 'B', day: 1 } },
                { block: 'checkbox', js: { day: 1 }, mods: { checked: isChecked ? 'yes' : '', name: 'day' } },
                { block: 'checkbox', js: { code: 'A' }, mods: { checked: isChecked ? 'yes' : '', name: 'hour' } }
            ]
        });

        return $(tree).bem('b-time-targeting-scale-board');
    }


    describe('Допустим все чекбокс отмечены', function() {
        beforeEach(function() {
            block = getBlock(true);
        });

        afterEach(function() {
            BEM.DOM.destruct(block.domElem);
        });

        describe('Если отметить чекбокс отвечающий за день', function() {
            var chk;

            beforeEach(function() {
                chk = block.findBlockInside({ block: 'checkbox', modName: 'name', modVal: 'day' });
            });

            it('То должно сработать событие "unchecked-line" c типом day', function(done) {
                block.on('unchecked-line', function(e, data) {
                    expect(data.type).to.be.equal('day');
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });

            it('То значение события должно равняться 1', function(done) {
                block.on('unchecked-line', function(e, data) {
                    expect(data.value).to.be.equal(1);
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });
        });

        describe('Если отметить чекбокс отвечающий за час', function() {
            var chk;

            beforeEach(function() {
                chk = block.findBlockInside({ block: 'checkbox', modName: 'name', modVal: 'hour' });
            });

            it('То должно сработать событие "unchecked-line" c типом hour', function(done) {
                block.on('unchecked-line', function(e, data) {
                    expect(data.type).to.be.equal('hour');
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });

            it('То значение события должно равняться А', function(done) {
                block.on('unchecked-line', function(e, data) {
                    expect(data.value).to.be.equal('A');
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });
        });
    });

    describe('Допустим все чекбоксы не отмечены', function() {
        beforeEach(function() {
            block = getBlock(false);
        });

        describe('Если отметить чекбокс отвечающий за день', function() {
            var chk;

            beforeEach(function() {
                chk = block.findBlockInside({ block: 'checkbox', modName: 'name', modVal: 'day' });
            });

            it('То должно сработать событие "checked-line" c типом day', function(done) {
                block.on('checked-line', function(e, data) {
                    expect(data.type).to.be.equal('day');
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });

            it('То значение события должно равняться 1', function(done) {
                block.on('checked-line', function(e, data) {
                    expect(data.value).to.be.equal(1);
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });
        });

        describe('Если отметить чекбокс отвечающий за час', function() {
            var chk;

            beforeEach(function() {
                chk = block.findBlockInside({ block: 'checkbox', modName: 'name', modVal: 'hour' });
            });

            it('То должно сработать событие "checked-line" c типом hour', function(done) {
                block.on('checked-line', function(e, data) {
                    expect(data.type).to.be.equal('hour');
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });

            it('То значение события должно равняться А', function(done) {
                block.on('checked-line', function(e, data) {
                    expect(data.value).to.be.equal('A');
                    done();
                });

                chk.toggle();
                chk.trigger('click');
            });
        });
    });

    describe('Допустим все ячейки имею значение 100', function() {
        beforeEach(function() {
            block = getBlock(true);
        });

        var hourA, hourB;

        beforeEach(function() {
            hourA = block.elem('hour', 'code', 'A');
            hourB = block.elem('hour', 'code', 'B');
        });

        describe('Если мы начнем выделять ячейки', function() {
            it('То должно сработать событие brush-down', function(done) {
                block.on('brush-down', function(e) {
                    expect(e.type).to.be.equal('brush-down');
                    done();
                });

                hourA.trigger('mousedown');
            });

            it('То должно сработать событие set c координатами ячейки', function(done) {
                block.on('set', function(e, data) {
                    expect((data.day === '1' && data.hourCode == 'A')).to.be.equal(true);
                    done();
                });

                hourA.trigger('mousedown');
            });

            it('То должно сработать событие set со значением isGroupSet равным false', function(done) {
                block.on('set', function(e, data) {
                    expect(data.isGroupSet).to.be.equal(false);
                    done();
                });

                hourA.trigger('mousedown');
            });
        });

        describe('Если мы выделим несколько ячеек', function() {
            it('То должно сработать событие set со значением isGroupSet равным true', function(done) {
                hourA.trigger('mousedown');

                block.on('set', function(e, data) {
                    expect(data.isGroupSet).to.be.equal(true);
                    done();
                });

                hourB.trigger('mousedown');
            });
        });

        describe('Если мы закончим выделение ячеек', function() {
            it('То должно сработать событие brush-up', function(done) {
                block.on('brush-up', function(e) {
                    expect(e.type).to.be.equal('brush-up');
                    done();
                });

                hourA.trigger('mouseup');
            });
        });

        describe('Если мы вызовем метод setHourLevel со значением отличным от 0', function() {
            beforeEach(function() {
                block.setHourLevel(hourA, 50);
            });

            it('То должен поменяться модификатор level', function() {
                block.hasMod(hourA, 'level', '50');
            });

            it('То контент ячейки должен быть "50" ("+" отображается в псевдоэлементе по модификатору)', function() {
                expect(hourA.text()).to.be.equal('50');
            });

            it('То должен поменяться атрибут title', function() {
                expect(hourA.attr('title')).to.be.equal('50%');
            });
        });

        describe('Если мы вызовем метод setHourLevel со значением  0', function() {
            it('То контент ячейки должен быть "0" ("-" отображается в псевдоэлементе по модификатору)', function() {
                block.setHourLevel(hourA, 0);
                expect(hourA.text()).to.be.equal('0');
            });
        });
    });

});
