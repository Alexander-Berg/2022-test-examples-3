describe.skip('b-time-targeting-holidays', function() {
    var sandbox;

    function getBlock(intoAccount, dontShow) {
        var tree = u.getDOMTree({
            block: 'b-time-targeting-holidays',
            data: {
                intoAccountWeekend: intoAccount,
                intoAccountHolidays: intoAccount,
                dontShowOnHolidays: dontShow,
                holidaysTimeTargetLevel: 100
            }
        });
        return $(tree).bem('b-time-targeting-holidays');
    }

    function check(block, type) {
        block.findBlockInside(block.elem('checkbox', 'type', type), 'checkbox').toggle();
    }

    before(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    after(function() {
        sandbox.restore();
    });

    describe('Допустим чекбоксы  не отмечены', function() {
        var block;

        beforeEach(function() {
            block = getBlock(false, false);
            //для использования to.triggerEvent
            sandbox.spy(block, 'trigger');
        });

        describe('Если мы отметим чекбокс "Учитывать рабочие выходные"', function() {
            it('То должно сработать событие change ' + JSON.stringify({ type: 'intoAccountWeekend', value: true }), function() {
                check(block, 'intoAccountWeekend');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'intoAccountWeekend', value: true });
            });
        });

        describe('Если мы отметим чекбокс "Учитывать праздничные дни"', function() {
            it('То должно сработать событие change с данными ' + JSON.stringify({ type: 'intoAccountHolidays', value: true }), function() {
                check(block, 'intoAccountHolidays');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'intoAccountHolidays', value: true });
            });

            it('То должны показаться контролы показа в праздничные дни', function() {
                check(block, 'intoAccountHolidays');
                sandbox.clock.tick(2000);
                expect(block).to.not.haveMod('holidays-controls', 'hidden', 'yes' );
            });
        });
    });

    describe('Допустим чекбоксы отмечены', function() {
        var block;

        beforeEach(function() {
            block = getBlock(true, false);
            //для использования to.triggerEvent
            sandbox.spy(block, 'trigger');
        });

        describe('Если мы снимем чекбокс "Учитывать рабочие выходные"', function() {
            it('То должно сработать событие change с данными ' + JSON.stringify({ type: 'intoAccountWeekend', value: false }), function() {
                check(block, 'intoAccountWeekend');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'intoAccountWeekend', value: false });
            });
        });

        describe('Если мы снимем чекбокс "Учитывать праздничные дни"', function() {
            it('То должно сработать событие change с данными ' + JSON.stringify({ type: 'intoAccountHolidays', value: false }), function() {
                check(block, 'intoAccountHolidays');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'intoAccountHolidays', value: false });
            });

            it('То контролы показа в праздничные дни должны скрыться', function() {
                check(block, 'intoAccountHolidays');
                sandbox.clock.tick(2000);
                expect(block).to.haveMod('holidays-controls', 'hidden', 'yes');
            });
        });

        describe('Если мы выставим радиобаттону "не показывать"', function() {
            it('То должно сработать событие с данными ' + JSON.stringify({ type: 'dontShowOnHolidays', value: true }), function() {
                block.findBlockInside('radiobox').val(0);
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'dontShowOnHolidays', value: true });
            });

            it('То селекты выбора времени должны быть недоступны', function() {
                block.findBlockInside('radiobox').val(0);
                sandbox.clock.tick(10);
                var res = block.findBlockInside({ block: 'select', modName: 'name', modVal: 'from' }).hasMod('disabled', 'yes') &&
                    block.findBlockInside({ block: 'select', modName: 'name', modVal: 'to' }).hasMod('disabled', 'yes');

                expect(res).to.be.true;
            });
        });

        describe('Если мы выставим радиобаттону "показывать с"', function() {
            var block;

            beforeEach(function() {
                block = getBlock(false, true);
                //для использования to.triggerEvent
                sandbox.spy(block, 'trigger');
            });

            it('То должно сработать событие с данными ' + JSON.stringify({ type: 'dontShowOnHolidays', value: false }), function() {
                block.findBlockInside('radiobox').val(1);
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'dontShowOnHolidays', value: false });
            });

            it('То селекты выбора времени должны доступны', function() {
                block = getBlock(false, true);
                block.findBlockInside('radiobox').val(1);

                var res = !block.findBlockInside({ block: 'select', modName: 'name', modVal: 'from' }).hasMod('disabled', 'yes') &&
                    !block.findBlockInside({ block: 'select', modName: 'name', modVal: 'to' }).hasMod('disabled', 'yes');

                expect(res).to.be.true;
            });
        });

        describe('Если мы изменим селект с уровнем цены', function() {
            it('То должно сработать событие с данными ' + JSON.stringify({ type: 'holidaysLevel', value: '50' }), function() {
                block.findBlockInside({ block: 'select', modName: 'name', modVal: 'level' }).val('50');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'holidaysLevel', value: '50' });
            });
        });

        describe('Если мы изменим селект начала времени показа', function() {
            it('То должно сработать событие с данными ' + JSON.stringify({ type: 'holidaysRange', value: [15, 20] }), function() {
                block.findBlockInside({ block: 'select', modName: 'name', modVal: 'from' }).val('15');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'holidaysRange', value: [15, 20] });
            });

            it('То значения селекта конца должно начинаться с выбранного значения + 1', function() {
                block.findBlockInside({ block: 'select', modName: 'name', modVal: 'from' }).val('15');
                var opts = block.findBlockInside({ block: 'select', modName: 'name', modVal: 'to' }).elem('option');

                expect($(opts.get(0)).val()).to.be.equal('16');
            });
        });

        describe('Если мы изменим селект конца времени показа', function() {
            it('То должно сработать событие с данными ' + JSON.stringify({ type: 'holidaysRange', value: [8, 15] }), function() {
                block.findBlockInside({ block: 'select', modName: 'name', modVal: 'to' }).val('15');
                sandbox.clock.tick(10);
                expect(block).to.triggerEvent('change', { type: 'holidaysRange', value: [8, 15] });
            });
        });
    });

});
