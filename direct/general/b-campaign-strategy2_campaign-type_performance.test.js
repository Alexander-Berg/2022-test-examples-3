describe('b-campaign-strategy2_campaign-type_performance', function() {

    var currency = 'RUB';

    describe('Интеграционные тесты: выбор и настройка стратегий для Смарт-баннеров', function() {
        var modelId = 2,
            ctx = {
                block: 'b-campaign-strategy2',
                mods: {
                    'with-hidden-input': 'yes',
                    'campaign-type': 'performance'
                },
                campDataModel: {
                    name: 'dm-dynamic-media-campaign',
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
                mediaType: 'performance',
                currency: currency,
                rmpCounters: {
                    allow_autobudget_avg_cpi: 1
                },
                metrika: {
                    campaign_goals: [{
                        goal_id: 1,
                        counter_status: 'Active',
                        goal_status: 'Active'
                    }]
                },
                strategy2: {
                    name: 'autobudget_optimization_cpc',
                    options: {
                        bid: null,
                        filter_avg_bid : 20,
                        originName: "autobudget_avg_cpc_per_filter",
                        sum: null,
                        target: "filter"
                    },
                    is_net_stopped: false,
                    is_search_stopped: true
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
                u['b-strategy2-settings'] = {
                    getHelpLinkUrl: function() {},
                    isMetrikaDisabled: function() {
                        var fn = u.getMetrikaWarning;

                        return fn.isSinonProxy && fn() === 'no-metrika';
                    },
                    fetchConversionsByGoalId: function() {
                        return Promise.resolve();
                    }
                };
                dm = BEM.MODEL.create({ name: 'dm-dynamic-media-campaign', id: modelId }, modelData);
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

                it('Корректно сохраняется стратегия Оптимизация количества кликов', function() {
                    var settingsBlock,
                        cpcInput;

                    bChooser.check('autobudget-optimization-cpc');
                    settingsBlock = chooseBlock.findBlockOn(
                        chooseBlock.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');

                    cpcInput = settingsBlock.findBlockOn(
                        settingsBlock.findElem(settingsBlock.elem('input-wrap-row', 'name', 'cpc-per-filter'), 'model-field'),
                        'input');

                    cpcInput.val(30);
                    saveButton.trigger('click');
                    clock.tick(500);

                    expect(JSON.parse(strategySettingsInput.val()))
                        .to
                        .deep
                        .equal({
                            is_net_stop: 0,
                            'name': 'autobudget_avg_cpc_per_filter',
                            'search': {
                                name: 'stop'
                            },
                            'net': {
                                bid: null,
                                filter_avg_bid : 30,
                                name: "autobudget_avg_cpc_per_filter",
                                sum: null
                            }
                        });
                });
            });
        });
});
