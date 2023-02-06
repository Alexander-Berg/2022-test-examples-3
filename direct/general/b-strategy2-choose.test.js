describe('b-strategy2-choose', function() {

    var currency = 'RUB',
        startStrategyName = 'autobudget_avg_click',
        ctx = {
            block: 'b-strategy2-choose',
            strategies: {
                hand: ['maximum_clicks','maximum_coverage'],
                auto: ['autobudget', 'autobudget_avg_click']
            },
            strategyModel: {
                name: startStrategyName,
                platform: 'all',
                options: {
                    avg_bid: 100,
                    sum: 330
                },
                dataName: startStrategyName,
                isDifferentPlaces: false,
                campaign: { currency: 'RUB', mediaType: 'text' },
                where: 'search'
            },
            dayBudgetSettings: {
                currency: "RUB",
                isEnabled: true,
                isSet: false,
                maxDailyChangeCount: "3",
                showMode: "default",
                showOptionsHint: true,
                sum: 459,
                timesChangedToday: 1
            }
        },
        block,
        vm,
        clock;

    u['b-strategy2-settings'] = {
        getMetrikaWarningsText: function() {},
        getCommonMoreText: function() {},
        getHelpLinkUrl: function() {},
        isMetrikaDisabled: function() {
            var fn = u.getMetrikaWarning;

            return fn.isSinonProxy && fn() === 'no-metrika';
        },
        fetchConversionsByGoalId: function() {
            return Promise.resolve();
        }
    };

    BEM.MODEL.decl({ model: 'm-campaign' }, {});

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        u.stubCurrencies();

        BEM.MODEL.create({ name: 'm-campaign', id: 1 });

        block = u
            .getDOMTree(ctx)
            .css({
                position: 'absolute',
                left: '-5000px'
            })
            .appendTo(document.body)
            .bem('b-strategy2-choose');

        vm = block.model;
    });

    afterEach(function() {
        // должны отработать afterCurrentEvent
        clock.tick(1);
        block.destruct();
        vm.destruct();
        clock.restore();
        u.restoreCurrencies();
    });

    it('Корректно инициализируется vm и интерфейс', function() {
        var settingsBlock = block.findBlockOn(block.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings'),
            modName = BEM.blocks['b-strategy2-settings'].getModNameByStrategyName(startStrategyName);

        expect(vm.get('strategyName')).to.equal(startStrategyName);
        expect(settingsBlock.getMod('name')).to.equal(modName);
        expect(block.getMod(block.elem('settings'), 'with-day-budget')).to.be.empty;
    });

    it('При установке модификатора loading блокируются кнопки', function() {
            block.setMod('loading', 'yes');

            expect(block._saveButton.getMod('disabled')).to.equal('yes');
            expect(block._cancelButton.getMod('disabled')).to.equal('yes');
        });

    it('Корректно инициализируются отключенные платформы', function() {
        ctx = {
            block: 'b-strategy2-choose',
            strategies: {
                hand: ['maximum_clicks','maximum_coverage'],
                auto: ['autobudget', 'autobudget_avg_click'],
                disabledPlatforms: ['net']
            },
            strategyModel: {
                name: startStrategyName,
                platform: 'all',
                options: {
                    avg_bid: 100,
                    sum: 330
                },
                dataName: startStrategyName,
                isDifferentPlaces: false,
                campaign: { currency: 'RUB', mediaType: 'text' },
                where: 'search'
            },
            dayBudgetSettings: {
                currency: "RUB",
                isEnabled: true,
                isSet: true,
                maxDailyChangeCount: "3",
                showMode: "default",
                showOptionsHint: true,
                sum: 459,
                timesChangedToday: 1
            }
        };

        block = u
            .getDOMTree(ctx)
            .css({
                position: 'absolute',
                left: '-5000px'
            })
            .appendTo(document.body)
            .bem('b-strategy2-choose');

        expect(block.hasMod(block.elem('tab-net'), 'disabled', 'yes')).to.equal(true);
    });

    describe('При включенной изначально автоматической стратегии', function() {
        beforeEach(function () {
            vm.set('strategyName', 'autobudget_avg_click');
            block.save();
            block._savedDataName = 'autobudget_avg_click';
        });

        describe('Смена стратегии', function() {
            it('Перерисовывается стратегия при смене с автоматической на ручную', function() {
                var settingsBlock,
                    chooserBlock,
                    modName,
                    newStrategyName = 'maximum_clicks';

                vm.set('strategyName', newStrategyName);
                settingsBlock = block.findBlockOn(block.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');
                chooserBlock = block.findBlockInside('b-chooser');
                modName = BEM.blocks['b-strategy2-settings'].getModNameByStrategyName(newStrategyName);

                expect(settingsBlock.getMod('name')).to.equal(modName);
                // через дефис, потому что b-chooser меняет землю на дефис
                expect(chooserBlock.getSelected().name).to.equal(newStrategyName.replace(/_/g, '-'));
            });

        });
    });

    describe('При включенной изначально ручной стратегии', function() {
        beforeEach(function () {
            vm.set('strategyName', 'maximum_clicks');
            block.save();
            block._savedDataName = 'default';
        });

        describe('Смена стратегии', function(){
            it('Перерисовывается стратегия при смене с ручной на автоматическую', function() {
                var settingsBlock,
                    chooserBlock,
                    modName,
                    newStrategyName = 'autobudget_avg_click';

                vm.set('strategyName', newStrategyName);
                settingsBlock = block.findBlockOn(block.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');
                chooserBlock = block.findBlockInside('b-chooser');
                modName = BEM.blocks['b-strategy2-settings'].getModNameByStrategyName(newStrategyName);

                expect(settingsBlock.getMod('name')).to.equal(modName);
                // через дефис, потому что b-chooser меняет землю на дефис
                expect(chooserBlock.getSelected().name).to.equal(newStrategyName.replace(/_/g, '-'));
            });
        });
    });
});
