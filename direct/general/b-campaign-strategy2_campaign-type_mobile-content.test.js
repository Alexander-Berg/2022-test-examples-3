describe('b-campaign-strategy2_campaign-type_mobile-content', function() {

    var currency = 'RUB',
        startStrategyName = 'autobudget_avg_click';

    describe('Интеграционные тесты: выбор и настройка стратегий для РМП кампаний', function() {
        var modelId = 2,
            ctx = {
                block: 'b-campaign-strategy2',
                mods: {
                    'with-hidden-input': 'yes',
                    'campaign-type': 'mobile-content'
                },
                campDataModel: {
                    name: 'dm-mobile-content-campaign',
                    id: modelId
                },
                script: 'localhost/',
                ulogin: 'super-holodilnik',
                dayBudgetSettings: {
                    isEnabled: true,
                    currency: 'RUB'
                }
            },
            modelData = {
                currency: currency,
                rmpCounters: {
                    allow_autobudget_avg_cpi: 1
                },
                strategy2: {
                    name: startStrategyName,
                    options: {
                        avg_bid: 100,
                        sum: 330,
                        goal_id: '4'
                    },
                    is_net_stopped: false
                },
                cid: 2
            },
            block,
            dm,
            vm,
            clock,
            chooseBlock,
            bChooser,
            saveButton,
            switcherButton,
            strategySettingsInput;

        beforeEach(function() {
            clock = sinon.useFakeTimers();
            dm = BEM.MODEL.create({ name: 'dm-mobile-content-campaign', id: modelId }, modelData);
            block = u
                .getDOMTree(ctx)
                .css({
                    position: 'absolute',
                    left: '-5000px'
                })
                .appendTo(document.body)
                .bem('b-campaign-strategy2');
            vm = block.model;
            switcherButton = block.findBlockOn('switcher', 'button');
            switcherButton.trigger('click');

            chooseBlock = block._popup.findBlockInside('b-strategy2-choose');
            bChooser = chooseBlock.findBlockInside('b-chooser');
            saveButton = chooseBlock.findBlockOn('save', 'button');
            strategySettingsInput = block.elem('input').filter('[name="json_strategy"]');

        });

        afterEach(function() {
            clock.restore();
        });

        describe('Корректное сохранение', function () {

            it('Корректно сохраняется стратегия Средняя цена установки приложения', function() {
                var settingsBlock,
                    cpiInput;

                bChooser.check('autobudget-avg-cpi');
                settingsBlock = chooseBlock.findBlockOn(
                    chooseBlock.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');

                cpiInput = settingsBlock.findBlockOn(
                    settingsBlock.findElem(settingsBlock.elem('row').first(), 'model-field'),
                    'input');

                cpiInput.val(20);
                saveButton.trigger('click');
                clock.tick(500);

                expect(JSON.parse(strategySettingsInput.val()))
                    .to
                    .deep
                    .equal({
                        is_net_stop: 0,
                        'name': '',
                        'search': {
                            'name': 'autobudget_avg_cpi',
                            'avg_cpi': 20,
                            'bid': '',
                            'sum': 330,
                            'goal_id': '4',
                            'pay_for_conversion': 0
                        },
                        'net': { 'name': 'default' }
                    });
            });

        });
    });
});
