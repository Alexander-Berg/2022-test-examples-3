describe('b-day-budget2-settings', function() {
    var ctx = {
            block: 'b-day-budget2-settings',
            maxDailyChangeCount: "3",
            sum: 459,
            mode: 'default',
            timesChangedToday: 1,
            isSet: false,
            currency: 'RUB'
        },
        block,
        vm,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });

        u.stubCurrencies();

        block = u
            .getDOMTree(ctx)
            .css({
                position: 'absolute',
                left: '-5000px'
            })
            .appendTo(document.body)
            .bem('b-day-budget2-settings');

        vm = block.model;
    });

    afterEach(function() {
        // должны отработать afterCurrentEvent
        sandbox.clock.tick(1);
        block.destruct();
        vm.destruct();
        sandbox.restore();
        u.restoreCurrencies();
    });

    it('Для неустановленного ДБ закрыт коллапсер', function() {
        expect(block).to.haveMod('collapsed', 'yes');
    });

    it.skip('Для установленного ДБ открыт коллапсер', function() {
        block.findBlockInside('checkbox').setMod('checked', 'yes');
        sandbox.clock.tick(100);
        expect(block).not.to.haveMod('collapsed');
    });

    describe('При превышении допустимого количества смен ДБ', function() {
        beforeEach(function() {
            vm.set('todayChangingCount', 3);
        });

        it('При попытке включения ДБ отрабатывает внешний колбек', function() {
            block.setOnMaximumClicksExceededEvent(function(cb) {
                alert('ДБ менять нельзя');
                cb();
            });

            block.findBlockInside('checkbox').setMod('checked', 'yes');
            sandbox.clock.tick(100);

            expect(vm.get('dayBudgetEnabled')).to.equal(false);
        });

        describe('При изначально устнаовленном ДБ', function() {
            beforeEach(function() {
                vm.set('todayChangingCount', 2);
                vm.set('isSet', true);
                block.params.isSet = true;
                vm.set('todayChangingCount', 3);
            });

            it('При попытке смены ДБ отрабатывает внешний колбек', function() {
                block.setOnMaximumClicksExceededEvent(function(cb) {
                    alert('ДБ менять нельзя');
                    cb();
                });

                block.findBlockInside('input').val(500);
                sandbox.clock.tick(100);

                expect(vm.get('sum')).to.equal('459');
                expect(block.findBlockInside('input').val()).to.equal('459');
            });
        })
    })
});
