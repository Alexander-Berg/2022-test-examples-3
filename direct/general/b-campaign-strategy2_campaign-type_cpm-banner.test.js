describe('b-campaign-strategy2__hint-forecast', function() {
    var currency = 'RUB',
        startStrategyName = 'autobudget_avg_click',
        block;

    function createBlock(ctx) {
        return u.createBlock(ctx, {
            inject: true
        });
    }

    describe('Интеграционные тесты: отображение светофора прогноза в превьюшке', function() {
        var modelId = 2,
            ctx = {
                block: 'b-campaign-strategy2',
                mods: {
                    'with-hidden-input': 'yes',
                    'campaign-type': 'cpm-banner'
                },
                mode: 'strategies',

                modelName: 'b-campaign-strategy2',
                campDataModel: {
                    name: 'dm-cpm-banner-campaign',
                    id: modelId
                },

                dayBudgetSettings:{
                    isEnabled: true,
                    currency: 'RUB',
                    maxDailyChangeCount: '3',
                    showOptionsHint: true
                },
                dataForecast: {
                    color: 'grey'
                }
            },

            modelData = {
                currency: currency,
                rmpCounters: {
                    allow_autobudget_avg_cpi: 1
                },
                mediaType: 'cpm_banner',
                allAdgroupIds: ['1234'],
                strategy2: {
                    name: startStrategyName,
                    options: {
                        avg_bid: 100,
                        budget: 330,
                        goal_id: '4',
                        net: {
                            auto_prolongation: 1,
                            avg_cpm: 52.25,
                            budget: 32423,
                            finish: "2030-03-07",
                            name: "autobudget_max_impressions_custom_period",
                            start: "2019-02-06"
                        }
                    },
                    is_net_stopped: false
                },
                cid: 2
            },
            modelDataWithOutGroups = Object.assign({}, modelData, {allAdgroupIds: []}),
            block,
            groupDm,
            dm,
            clock;

        beforeEach(function() {
            clock = sinon.useFakeTimers();
            groupDm = BEM.MODEL.create({ name: 'dm-cpm-banner-group', id: '1234' }, {});
            dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
            block = createBlock(ctx);

        });

        afterEach(function() {
            block.destruct();
            dm.destruct();
            groupDm.destruct();
            clock.restore();
        });

        describe('Отображение', function () {
            it('Сфетофор отрендерен на странице', function() {
                expect(block).to.haveElem('simple-forecast');
            });
        });

        describe('Модификаторы', function () {
            it('Сфетофор ставит модификатор — loading', function() {
                var strategyPreviewLights = block.elemInstance('simple-forecast');
                expect(strategyPreviewLights).to.haveMod('loading', 'yes');
            });

            it('Сфетофор ставит модификатор — серый по умолчанию', function() {
                var strategyPreviewLights = block.elemInstance('simple-forecast');
                expect(strategyPreviewLights).to.haveMod('color', 'grey');
            });

            it('Сфетофор ставит модификатор — красный', function() {
                block._updateForecastInDom({
                    color: 'red',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                    text: 'Увеличьте цену, чтобы получить больше показов за период в рамках заданного бюджета.'
                });

                var strategyPreviewLights = block.elemInstance('simple-forecast');
                expect(strategyPreviewLights).to.haveMod('color', 'red');
            });

            it('Сфетофор ставит модификатор — желтый', function() {
                block._updateForecastInDom({
                    color: 'yellow',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                    text: 'Увеличьте цену, чтобы получить больше показов за период в рамках заданного бюджета.'
                });

                var strategyPreviewLights = block.elemInstance('simple-forecast');
                expect(strategyPreviewLights).to.haveMod('color', 'yellow');
            });

            it('Сфетофор ставит модификатор — зеленый', function() {
                block._updateForecastInDom({
                    color: 'green',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                    text: 'Увеличьте цену, чтобы получить больше показов за период в рамках заданного бюджета.'
                });

                var strategyPreviewLights = block.elemInstance('simple-forecast');
                expect(strategyPreviewLights).to.haveMod('color', 'green');
            });
        });

        describe('Тексты', function () {
            var iget2Spy;

            beforeEach(function() {
                iget2Spy = sinon.spy(window, 'iget2');
                dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
                block = createBlock(ctx);
            });

            afterEach(function() {
                iget2Spy.restore();
                block.destruct();
                dm.destruct();
            });

            it('Сфетофор содержит текст по умолчанию: "загрузка..."', function() {
                var strategyPreviewText = block.elemInstance('simple-forecast-text');
                expect(isIget2CalledWithKey(iget2Spy, 'loading')).to.be.true;
            });

            it('Сфетофор содержит текст с рекомендацией цены, если красный', function() {
                block._updateForecastInDom({
                    color: 'red',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'}
                });

                expect(isIget2CalledWithKey(iget2Spy, 'hint-forecast-warning')).to.be.true;
            });

            it('Сфетофор содержит текст с рекомендацией цены, если желтый', function() {
                block._updateForecastInDom({
                    color: 'yellow',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                });

                expect(isIget2CalledWithKey(iget2Spy, 'hint-forecast-warning')).to.be.true;
            });

            it('Сфетофор содержит текст об оптимальной цене, если зеленый', function() {
                block._updateForecastInDom({
                    color: 'green',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'}
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-good')).to.be.true;
            });
        });

        describe('Попап с подсказкой', function () {
            var iget2Spy;

            beforeEach(function() {
                iget2Spy = sinon.spy(window, 'iget2');
                dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
                block = createBlock(ctx);
            });

            afterEach(function() {
                iget2Spy.restore();
                block.destruct();
                dm.destruct();
            });

            it('Попап сфетофора содержит ID прогноза, если зеленый', function() {
                block._updateForecastInDom({
                    color: 'green',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'}
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-id')).to.be.true;
            });

            it('Попап сфетофора содержит ID прогноза, если желтый', function() {
                block._updateForecastInDom({
                    color: 'yellow',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                    text: 'Увеличьте цену, чтобы кампания эффективно открутилась за указанный период'
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-id')).to.be.true;
            });

            it('Попап сфетофора содержит ID прогноза, если красный', function() {
                block._updateForecastInDom({
                    color: 'red',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                    text: 'Увеличьте цену, чтобы кампания эффективно открутилась за указанный период'
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-id')).to.be.true;
            });

            it('Попап сфетофора содержит текст об оптимальной цене, если зеленый', function() {
                block._updateForecastInDom({
                    color: 'green',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'}
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-good')).to.be.true;
            });

            it('Попап сфетофора содержит текст-предупреждение, если желтый', function() {
                block._updateForecastInDom({
                    color: 'yellow',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-warning')).to.be.true;
            });

            it('Попап сфетофора содержит текст-предупреждение, если красный', function() {
                block._updateForecastInDom({
                    color: 'red',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'},
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-warning')).to.be.true;
            });

        });

        describe('Попап, если нет охвата', function () {
            var iget2Spy;

            beforeEach(function() {
                iget2Spy = sinon.spy(window, 'iget2');
                dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
                block = createBlock(ctx);
            });

            afterEach(function() {
                iget2Spy.restore();
                block.destruct();
                dm.destruct();
            });

            it('Попап сфетофора содержит ID прогноза, если ошибка', function() {
                block._updateForecastInDom({
                    color: 'grey',
                    errorCode: 'InventoriDefectIds.Gen.NO_SUITABLE_ADGROUPS',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'}
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-id')).to.be.true;
            });

            it('Попап сфетофора содержит текст о причине невозможности построить прогноз', function() {
                block._updateForecastInDom({
                    color: 'grey',
                    errorCode: 'InventoriDefectIds.Gen.NO_SUITABLE_ADGROUPS',
                    hint: {id: '775D5AED-2714-4D09-B7B9-46A18EBC973C'}
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-groups')).to.be.true;
            });

        });

        describe('Попап, если нет групп', function () {
            var iget2Spy;

            beforeEach(function() {
                iget2Spy = sinon.spy(window, 'iget2');
                dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelDataWithOutGroups);
                block = createBlock(ctx);
            });

            afterEach(function() {
                iget2Spy.restore();
                block.destruct();
                dm.destruct();
            });

            it('Попап сфетофора не содержит ID прогноза, если ошибка', function() {
                block._updateForecastInDom({
                    color: 'grey',
                    errorCode: 'InventoriDefectIds.Gen.NO_SUITABLE_ADGROUPS'
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-id')).to.be.false;
            });

            it('Попап сфетофора содержит текст о причине невозможности построить прогноз', function() {
                block._updateForecastInDom({
                    color: 'grey',
                    errorCode: 'InventoriDefectIds.Gen.NO_SUITABLE_ADGROUPS'
                });

                expect(isIget2CalledWithKey(iget2Spy, 'forecast-help-groups')).to.be.true;
            });

        });

        describe('SPY', function () {
            var getHintForecastSpy;

            beforeEach(function() {
                getHintForecastSpy = sinon.spy(u['b-campaign-strategy2'], '_getHintForecast');
            });

            afterEach(function() {
                getHintForecastSpy.restore();
            });

            describe('с группами', function() {
                beforeEach(function() {
                    dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
                    block = createBlock(ctx);
                });

                afterEach(function() {
                    block.destruct();
                    dm.destruct();
                });

                it('Должен вызываться метод _getHintForecast при запросе превью стратегии', function() {
                    var strategyData = JSON.parse('{"strategyData":{"name":"different_places","options":{"name":"different_places","net":{"avg_cpm":22.25,"name":"autobudget_max_impressions_custom_period","finish":"2030-03-07","budget":232423,"start":"2019-02-06","auto_prolongation":1},"search":{"name":"stop"}},"is_autobudget":true,"is_net_stopped":false,"is_search_stopped":false},"currency":"RUB","metrika":{},"onlyNet":true,"onlySearch":false}');

                    block._getStrategyHint(strategyData);

                    expect(getHintForecastSpy.called).to.be.true;
                });

                it('Должен вызываться метод _updateForecastInDom при запросе превью стратегии', function() {
                    var updateForecastInDomSpy = sinon.spy(block, '_updateForecastInDom'),
                        strategyData = JSON.parse('{"strategyData":{"name":"different_places","options":{"name":"different_places","net":{"avg_cpm":22.25,"name":"autobudget_max_impressions_custom_period","finish":"2030-03-07","budget":232423,"start":"2019-02-06","auto_prolongation":1},"search":{"name":"stop"}},"is_autobudget":true,"is_net_stopped":false,"is_search_stopped":false},"currency":"RUB","metrika":{},"onlyNet":true,"onlySearch":false}');

                    block._getStrategyHint(strategyData);

                    expect(updateForecastInDomSpy.called).to.be.true;
                });
            });

            describe('без групп', function() {
                var ctx = {
                    block: 'b-campaign-strategy2',
                    mods: {
                        'with-hidden-input': 'yes',
                        'campaign-type': 'cpm-banner',
                        'on-page': 'campaign'
                    },
                    mode: 'strategies',

                    modelName: 'b-campaign-strategy2',
                    campDataModel: {
                        name: 'dm-cpm-banner-campaign',
                        id: modelId
                    },

                    dayBudgetSettings:{
                        isEnabled: true,
                        currency: 'RUB',
                        maxDailyChangeCount: '3',
                        showOptionsHint: true
                    },
                    dataForecast: {
                        color: 'grey'
                    }
                };

                beforeEach(function() {
                    dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelDataWithOutGroups);
                    block = createBlock(ctx);
                });

                afterEach(function() {
                    block.destruct();
                    dm.destruct();
                });

                it('Не должен вызываться метод _getHintForecast при запросе превью стратегии если нет групп на странице кампании', function() {
                    var strategyData = JSON.parse('{"strategyData":{"name":"different_places","options":{"name":"different_places","net":{"avg_cpm":22.25,"name":"autobudget_max_impressions_custom_period","finish":"2030-03-07","budget":232423,"start":"2019-02-06","auto_prolongation":1},"search":{"name":"stop"}},"is_autobudget":true,"is_net_stopped":false,"is_search_stopped":false},"currency":"RUB","metrika":{},"onlyNet":true,"onlySearch":false}');

                    block._getStrategyHint(strategyData);

                    expect(getHintForecastSpy.called).to.be.false;
                });
            });
        });

        describe('Ручная стратегия', function() {
            beforeEach(function() {
                ctx = {
                    block: 'b-campaign-strategy2',
                    mods: {
                        'with-hidden-input': 'yes',
                        'campaign-type': 'cpm-banner'
                    },
                    campDataModel: {
                        name: 'dm-cpm-banner-campaign',
                        id: modelId
                    },

                    dayBudgetSettings:{}
                };

                modelData = {
                    currency: currency,
                    rmpCounters: {
                        allow_autobudget_avg_cpi: 1
                    },
                    allAdgroupIds: ['1234'],
                    mediaType: 'cpm_banner',
                    strategy2: {
                        name: 'different_places',
                        options: {
                            name: "different_places",
                            net: {
                                name: 'cpm_default',
                            },

                            search: {
                                name: 'stop'
                            }
                        },
                        is_net_stopped: false
                    },
                    cid: 2
                };

                clock = sinon.useFakeTimers();
                groupDm = BEM.MODEL.create({ name: 'dm-cpm-banner-group', id: '1234' }, {});
                dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
                block = createBlock(ctx);
            });

            afterEach(function() {
                block.destruct();
                groupDm.destruct();
                dm.destruct();
                clock.restore();
            });

            it('Для ручной стратегии светофор не должен строиться.', function() {
                expect(block).not.to.haveElem('simple-forecast-text');
            });
        });

        describe('Стратегия с истекшим сроком', function() {
            var clock,
                iget2Spy,
                modelData = {
                currency: currency,
                rmpCounters: {
                    allow_autobudget_avg_cpi: 1
                },
                mediaType: 'cpm_banner',
                allAdgroupIds: ['1234'],
                strategy2: {
                    name: startStrategyName,
                    options: {
                        avg_bid: 100,
                        budget: 330,
                        goal_id: '4',
                        net: {
                            auto_prolongation: 1,
                            avg_cpm: 52.25,
                            budget: 32423,
                            finish: "2015-01-01",
                            name: "autobudget_max_impressions_custom_period",
                            start: "2014-01-01"
                        }
                    },
                    is_net_stopped: false
                },
                cid: 2
            };

            beforeEach(function() {
                clock = sinon.useFakeTimers(+new Date('2016-01-01'));
                iget2Spy = sinon.spy(window, 'iget2');
                groupDm = BEM.MODEL.create({ name: 'dm-cpm-banner-group', id: '1234' }, {});
                dm = BEM.MODEL.create({ name: 'dm-cpm-banner-campaign', id: modelId }, modelData);
                block = createBlock(ctx);
            });

            afterEach(function() {
                iget2Spy.restore();
                clock.restore();
                block.destruct();
                dm.destruct();
            });

            it('Сфетофор содержит текст об истекшей дате стратегии', function() {
                expect(isIget2CalledWithKey(iget2Spy, 'strategy-warning-not-relevant-period')).to.be.true;
            });
        });

    });
});


function isIget2CalledWithKey(iget2Spy, key) {
    return iget2Spy.args.some(function(item) {
        return item[1] === key;
    });
}
