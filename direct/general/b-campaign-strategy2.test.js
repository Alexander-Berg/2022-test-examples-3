describe('b-campaign-strategy2', function() {

    var currency = 'RUB',
        startStrategyName = 'autobudget_avg_click',
        modelId = 1,
        ctx = {
            block: 'b-campaign-strategy2',
            mods: {
                'with-hidden-input': 'yes',
                'campaign-type': 'text'
            },
            campDataModel: {
                name: 'm-campaign',
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
            metrika: {
                campaign_goals: [{
                    goal_id: 1,
                    counter_status: 'Active',
                    goal_status: 'Active',
                    goal_name: 'Цель'
                }]
            },
            strategy2: {
                name: startStrategyName,
                options: {
                    avg_bid: 100,
                    sum: 330
                },
                is_net_stopped: false
            },
            cid: 1,
            metrika_counters: '123'
        },
        block,
        dm,
        vm,
        clock,
        sandbox,
        chooseBlock,
        tabBlock,
        bChooser,
        saveButton,
        switcherButton,
        strategySettingsInput;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });
        sandbox.server.respondWith('POST', '/', JSON.stringify({ available_goals: [] }));
        clock = sandbox.clock;
        u.stubCurrencies();
        dm = BEM.MODEL.create({ name: 'm-campaign', id: modelId }, modelData);
        block = u.createBlock(ctx),
        vm = block.model;
        switcherButton = block.findBlockOn('switcher', 'button');
        switcherButton.trigger('click');

        chooseBlock = block._popup.findBlockInside('b-strategy2-choose');
        bChooser = chooseBlock.findBlockInside('b-chooser');
        tabBlock = chooseBlock.findBlockInside('tabs');
        saveButton = chooseBlock.findBlockOn('save', 'button');
        strategySettingsInput = block.elem('input').filter('[name="json_strategy"]');
    });

    afterEach(function() {
        u.restoreCurrencies();
        clock.restore();
        // блок не уничтожаем, так как возникает падение из-за afterCurrentEvent-ов
        // блок закрывается сам при сохранении/отмене
    });

    describe('Интеграционные тесты: выбор и настройка стратегий для текстово-графических кампаний', function() {
        describe('Корректное сохранение', function () {

            it('Корректно сохраняется текущая выбранная стратегия (средняя цена клика)', function() {
                saveButton.trigger('click');
                expect(JSON.parse(strategySettingsInput.val()))
                    .to
                    .deep
                    .equal({
                        is_net_stop: 0,
                        name: '',
                        search: {
                            avg_bid: 100,
                            sum: 330,
                            name: 'autobudget_avg_click'
                        },
                        net: { name: 'default' }
                    });
            });

            describe('Корректно сохраняется стратегия Ручное управление ставками', function() {
                beforeEach(function() {
                    bChooser.check('max-clicks');
                });

                describe('без раздельного управления', function() {
                    it('с маппингом в показ на минимальной позиции в спецразмещении', function() {

                        chooseBlock.trigger('save', { name: 'maximum_clicks', options: { name: 'min_price', options: { place: 'premium' } }, platform: 'all' });
                        clock.tick(500);

                        expect(JSON.parse(strategySettingsInput.val()))
                            .to
                            .deep
                            .equal({
                                is_net_stop: 0,
                                name: '',
                                search: {
                                    name: 'min_price',
                                    place: 'premium'
                                },
                                net: { name: 'default' }
                            });
                    });

                    it('с маппингом в наивысшую доступную позицию', function() {

                        chooseBlock.trigger('save', { name: 'maximum_clicks', options: { name: 'default', options: {} }, platform: 'all' });
                        clock.tick(500);

                        expect(JSON.parse(strategySettingsInput.val()))
                            .to
                            .deep
                            .equal({
                                is_net_stop: 0,
                                name: '',
                                search: {
                                    name: 'default'
                                },
                                net: {
                                    name: 'default'
                                }
                            });
                    });
                });

                describe ('c раздельным управлением', function() {
                    it('с маппингом в показ на минимальной позиции в спецразмещении', function() {

                        chooseBlock.trigger('save', {
                            name: 'different_places',
                            options: {
                                net: { name: 'maximum_coverage' },
                                search: { name: 'min_price', place: 'premium' }
                            } });
                        clock.tick(500);

                        expect(JSON.parse(strategySettingsInput.val()))
                            .to
                            .deep
                            .equal({
                                is_net_stop: 0,
                                name: 'different_places',
                                search: {
                                    name: 'min_price',
                                    place: 'premium'
                                },
                                net: { name: 'maximum_coverage' }
                            });
                    });

                    it('с маппингом в наивысшую доступную позицию', function() {

                        chooseBlock.trigger('save', {
                            name: 'different_places',
                            options: {
                                net: { name: 'maximum_coverage' },
                                search: { name: 'default' }
                            }, platform: 'all' });
                        clock.tick(500);

                        expect(JSON.parse(strategySettingsInput.val()))
                            .to
                            .deep
                            .equal({
                                is_net_stop: 0,
                                name: 'different_places',
                                search: {
                                    name: 'default'
                                },
                                net: {
                                    name: 'maximum_coverage'
                                }
                            });
                    });

                    it('с маппингом в сетевую стратегию', function () {
                        chooseBlock.trigger('save', {
                            name: 'maximum_coverage',
                            platform: 'net',
                            options: {}
                        });
                        clock.tick(500);

                        expect(JSON.parse(strategySettingsInput.val()))
                            .to
                            .deep
                            .equal({
                                is_net_stop: 0,
                                name: 'different_places',
                                search: {
                                    name: 'stop'
                                },
                                net: { name: 'maximum_coverage' }
                            });
                    });
                });
            });

            it('Корректно сохраняется стратегия Средняя цена конверсии', function() {
                var settingsBlock,
                    cpaInput;

                bChooser.check('autobudget-avg-cpa');
                settingsBlock = chooseBlock.findBlockOn(
                    chooseBlock.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');

                settingsBlock.model.set('metrikaRequestRunning', false);

                cpaInput = settingsBlock.findBlockOn(
                    settingsBlock.findElem(settingsBlock.elem('row'), 'model-field'),
                    'input');

                cpaInput.val(100);
                settingsBlock.model.set('goalId', '1');
                saveButton.trigger('click');
                clock.tick(500);

                expect(JSON.parse(strategySettingsInput.val()))
                    .to
                    .deep
                    .equal({
                        is_net_stop: 0,
                        name: "",
                        search: {
                            name: 'autobudget_avg_cpa',
                            avg_cpa: 100,
                            goal_id: '1',
                            sum: 330,
                            bid: '',
                            pay_for_conversion: 0
                        },
                        net: { name: 'default' }
                    });
            });

            it('Корректно сохраняется стратегия Средняя рентабельность инвестиций', function() {
                var settingsBlock,
                    roiInput,
                    weekBidCheckbox,
                    weekBidInput;

                bChooser.check('autobudget-roi');
                settingsBlock = chooseBlock.findBlockOn(
                    chooseBlock.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');

                settingsBlock.model.set('metrikaRequestRunning', false);

                roiInput = settingsBlock.findBlockOn(settingsBlock.elem('roi-input'), 'input');
                weekBidCheckbox = settingsBlock.findBlockInside(
                    settingsBlock.findElem('week-bid-control'),
                    'checkbox');
                weekBidInput = settingsBlock.findBlockInside(
                    settingsBlock.findElem('week-bid-control'),
                    'input');

                roiInput.val(3);
                weekBidCheckbox.setMod('checked', 'yes');
                weekBidInput.val(350);
                settingsBlock.model.set('goalId', '1');
                clock.tick(100);
                saveButton.trigger('click');
                clock.tick(500);

                expect(JSON.parse(strategySettingsInput.val()))
                    .to
                    .deep
                    .equal({
                        name: '',
                        search: {
                            roi_coef: 3,
                            profitability: null,
                            reserve_return: 100,
                            bid: null,
                            sum: 350,
                            goal_id: '1',
                            name: 'autobudget_roi'
                        },
                        net: { name: 'default' },
                        is_net_stop: 0
                    });
            });

            it('Корректно сохраняется стратегия Недельный бюджет', function() {
                var settingsBlock,
                    weekBidInput;

                bChooser.check('autobudget');
                settingsBlock = chooseBlock.findBlockOn(
                    chooseBlock.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');

                settingsBlock.model.set('name', 'autobudget');

                weekBidInput = settingsBlock.findBlockOn(
                    settingsBlock.findElem('week-bid-input'),
                    'input');

                weekBidInput.val(350);
                saveButton.trigger('click');
                clock.tick(500);

                expect(JSON.parse(strategySettingsInput.val()))
                    .to
                    .deep
                    .equal({
                        is_net_stop: 0,
                        name: '',
                        search: {
                            name: 'autobudget',
                            goal_id: '',
                            sum: 350,
                            bid: ''
                        },
                        net: { name: 'default' }
                    });
            });

            it('Корректно сохраняется стратегия Недельный пакет кликов', function() {
                var settingsBlock,
                    clicksLimitInput;

                bChooser.check('autobudget-week-bundle');
                settingsBlock = chooseBlock.findBlockOn(
                    chooseBlock.findElem('strategy-settings', 'show', 'yes'), 'b-strategy2-settings');

                settingsBlock.model.set('name', 'autobudget_week_bundle');

                clicksLimitInput = settingsBlock.findBlockOn(settingsBlock.elem('clicks-limit-input'), 'input');

                clicksLimitInput.val(300);
                saveButton.trigger('click');
                clock.tick(500);

                expect(JSON.parse(strategySettingsInput.val()))
                    .to
                    .deep
                    .equal({
                        is_net_stop: 0,
                        name: '',
                        search: {
                            name: 'autobudget_week_bundle',
                            limit_clicks: 300,
                            bid: '',
                            avg_bid: 100
                        },
                        net: { name: 'default' }
                    });
            });
        });
    });

    describe('Корректно сохраняются стратегии на платформе', function() {

        it('На поиске и в сетях', function() {
            tabBlock.activate(0);
            saveButton.trigger('click');
            clock.tick(500);

            expect(JSON.parse(strategySettingsInput.val()))
                .to
                .deep
                .equal({
                    name:"",
                    search: {
                        avg_bid: 100,
                        sum: 330,
                        name: "autobudget_avg_click"
                    },
                    net:{
                        name:"default"
                    },
                    is_net_stop:0
                });
        });

        it('Только на поиске', function() {
            tabBlock.activate(1);
            saveButton.trigger('click');
            clock.tick(500);

            expect(JSON.parse(strategySettingsInput.val()))
                .to
                .deep
                .equal({
                    name: '',
                    search: {
                        name: 'autobudget_avg_click',
                        avg_bid: 100,
                        sum: 330
                    },
                    net: { name: 'stop' },
                    is_net_stop: 1
                });
        });

        it('Только в сетях', function() {
            tabBlock.activate(2);
            saveButton.trigger('click');
            clock.tick(500);

            expect(JSON.parse(strategySettingsInput.val()))
                .to
                .deep
                .equal({
                    search: { name: 'stop' },
                    net: {
                        name: 'autobudget_avg_click',
                        avg_bid: 100,
                        sum: 330
                    },
                    name: 'different_places',
                    is_net_stop: 0
                });
        })
    });
});

// todo выпилить в DIRECT-77130
describe('b-campaign-strategy2.utils', function () {
    it('Показ под результатами поиска маппится на дефолтную стратегию', function () {
        expect(u['b-campaign-strategy2'].buildStrategy({
            strategy: {
                net: { name: 'default' },
                name: '',
                is_net_stop: 0,
                is_search_stop: 0,
                is_autobudget: 0,
                search: { name: 'no_premium' }
            }
        })).to.eql({
            options: { name: 'default' },
            is_autobudget: 0,
            is_net_stopped: 0,
            is_search_stopped: 0,
            name: 'default',
            lastStrategyChange: undefined,
            strategy_with_custom_period_min_available_budget: undefined
        })
    });

    it('Показ под результатами поиска при раздельном размещении маппится на дефолтную стратегию', function () {
        expect(u['b-campaign-strategy2'].buildStrategy({
            strategy: {
                net: { name: 'maximum_coverage' },
                name: 'different_places',
                is_autobudget: 0,
                is_net_stop: 0,
                is_search_stop: 0,
                search: { name: 'default' }
            }
        })).to.eql({
            options: {
                name: 'different_places',
                net: { name: 'maximum_coverage' },
                search: { name: 'default' }
            },
            is_autobudget: 0,
            is_net_stopped: 0,
            is_search_stopped: 0,
            name: 'different_places',
            lastStrategyChange: undefined,
            strategy_with_custom_period_min_available_budget: undefined
        })
    });
});
