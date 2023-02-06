describe('b-offline-set-phrases-prices', function() {
    var block,
        campaignModel,
        savedData = {},
        sandbox,
        constStub,
        strategiesHash = {
            'default': {
                name: '',
                search: { name: 'default' },
                net: { name: 'default' }
            },
            'search-only': {
                name: '',
                search: { name: 'autobudget_avg_click' },
                net: { name: 'stop' }
            },
            'diff-both': {
                name: 'different_places',
                search: { name: 'default' },
                net: { name: 'maximum_coverage' }
            },
            'diff-stop': {
                name: 'different_places',
                search: { name: 'stop' },
                net: { name: 'maximum_coverage' }
            },
            'cpm-banner-strategy': {
                name: '',
                search: { name: 'stop' },
                net: { name: 'default_cpm' }
            }
        },
        clock;

    function createBlock(strategyName, mediaType, modelParams) {
        var tree;

        modelParams =  u._.extend({
            name: 'm-campaign',
            id: 1
        }, modelParams || {});

        tree = $(BEMHTML.apply({
            block: 'b-offline-set-phrases-prices',
            mods: {
                type: mediaType || ''
            },
            js: {
                modelParams: modelParams
            },
            content: [
                {
                    elem: 'popup',
                    content: {
                        block: 'popup',
                        content: [
                            { elem: 'tail' },
                            {
                                elem: 'content',
                                mix: [{ block: 'b-offline-set-phrases-prices', elem: 'content' }],
                                content: [
                                    {
                                        elem: 'title',
                                        mix: [{ block: 'b-offline-set-phrases-prices', elem: 'title' }],
                                        content: 'Запрос на изменение цен принят'
                                    },
                                    {
                                        elem: 'body',
                                        content: [
                                            {
                                                block: 'b-offline-set-phrases-prices',
                                                elem: 'edit-block',
                                                content: [
                                                    {
                                                        elem: 'warning',
                                                        content: 'Обратите внимание: новые цены вступят в силу не ранее чем через 5&nbsp;&ndash;&nbsp;10 минут.'
                                                    },
                                                    {
                                                        block: 'tabs',
                                                        mods: {
                                                            control: 'menu',
                                                            size: 's',
                                                            theme: 'normal-red',
                                                            layout: 'horiz'
                                                        },
                                                        panes: 'offline-set-phrases',
                                                        content: [
                                                            {
                                                                elem: 'tab',
                                                                content: {
                                                                    block: 'link',
                                                                    content: 'Единая цена'
                                                                }
                                                            },
                                                            {
                                                                elem: 'tab',
                                                                content: {
                                                                    block: 'link',
                                                                    content: 'Мастер цен'
                                                                }
                                                            }
                                                        ]
                                                    },
                                                    {
                                                        block: 'tabs-panes',
                                                        id: 'offline-set-phrases',
                                                        mix: [{ block: 'b-offline-set-phrases-prices', elem: 'constructor-wrapper' }],
                                                        content: [
                                                            {
                                                                elem: 'pane',
                                                                elemMods: { value: 'simple' }
                                                            },
                                                            {
                                                                elem: 'pane',
                                                                elemMods: { value: 'wizard' }
                                                            }
                                                        ]
                                                    }

                                                ]
                                            },
                                            {
                                                block: 'b-offline-set-phrases-prices',
                                                elem: 'nobs-block',
                                                content: {
                                                    elem: 'nobs-body',
                                                    content: 'По техническим причинам нельзя установить цены для фраз кампании. Пожалуйста, попробуйте через несколько минут.'
                                                }

                                            },
                                            {
                                                block: 'b-offline-set-phrases-prices',
                                                elem: 'success-block',
                                                content: {
                                                    elem: 'success-body',
                                                    content: {
                                                        block: 'icon-text',
                                                        mods: { size: 'ms', theme: 'alert' },
                                                        content: [
                                                            'Ставки сохранятся через 5 %s 10 минут.', '&ndash;',
                                                            '&nbsp;',
                                                            'Во избежание путаницы рекомендуем в это время воздержаться от изменения цен в кампании.',
                                                            '&nbsp;',
                                                            'Активизация изменений займет еще до 30 минут.'
                                                        ]
                                                    }
                                                }
                                            }
                                        ]
                                    },
                                    {
                                        elem: 'controls',
                                        content: [
                                            {
                                                block: 'button',
                                                mods: { size: 's' },
                                                mix: [{ block: 'b-offline-set-phrases-prices', elem: 'save' }],
                                                content: 'Установить'
                                            },
                                            {
                                                block: 'button',
                                                mods: { size: 's' },
                                                mix: [{ block: 'b-offline-set-phrases-prices', elem: 'close' }],
                                                content: 'Отмена'
                                            }
                                        ]
                                    }
                                ]
                            },
                            { elem: 'action-foot' }
                        ]
                    }

                },
                {
                    elem: 'toggle',
                    js: { modelParams: modelParams },
                    content: 'Нажми'
                }
            ]
        }));

        $('body').append(tree);

        block = BEM.DOM.init(tree).bem('b-offline-set-phrases-prices');

        block._getConstructorData = function(callback) {
            return callback.call(block, savedData);
        };

        block._campModel = createCampaignModel(modelParams, strategyName);

        block._sendRequestToCampOptions = function(cid, data, promise) {
            promise.resolve();
        };

        block._getRequest = function() {
            return {
                get: function(data, onSuccess, onError) {
                    (typeof onSuccess == 'function') && onSuccess.call(block, { success: 1 });
                }
            };
        };

    }

    function createCampaignModel(modelParams, strategyName) {
        strategyName = strategyName || 'default';

        campaignModel = BEM.MODEL.create(modelParams, {
            strategy: strategiesHash[strategyName],
            currency: 'YND_FIXED',
            cid: 1
        });

        return campaignModel;
    }

    before(function() {
        sandbox = sinon.sandbox.create();

        constStub = u.stubCurrencies2(sandbox);
    });

    after(function() {
        sandbox.restore();
    });

    beforeEach(function() {
        clock = sinon.useFakeTimers();
    });

    afterEach(function() {
        clock.tick(0); //даем время отработать поставленным в очередь обработчикам
        block._getPopup().destruct();
        block && block.destruct();

        campaignModel && campaignModel.destruct();
        clock.restore();
    });

    describe('#__onNewDataLoaded()', function() {
        it('Обычная стратегия, отрисованы формы конструктора ставок common-simple и common-wizard', function() {
            createBlock('default');
            sinon.spy(block, '_getConstructorJSONForm');

            block.elem('toggle').click();

            expect(block._getConstructorJSONForm.calledWith('simple', 'common')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('wizard', 'common')).to.be.equal(true);

            block._getConstructorJSONForm.restore();
        });

        it('Отдельное размещение, отрисованы формы конструктора context-simple, context-wizard и search-simple, search-wizard', function() {
            createBlock('diff-both');
            sinon.spy(block, '_getConstructorJSONForm');

            block.elem('toggle').click();

            expect(block._getConstructorJSONForm.calledWith('simple', 'search')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('simple', 'context')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('wizard', 'search')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('wizard', 'context')).to.be.equal(true);

            block._getConstructorJSONForm.restore();
        });

        it('Отдельное размещение, поиск отключен, отрисованы формы конструктора context-simple, context-wizard', function() {
            createBlock('diff-stop');

            sinon.spy(block, '_getConstructorJSONForm');

            block.elem('toggle').click();
            expect(block._getConstructorJSONForm.calledWith('simple', 'context')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('wizard', 'context')).to.be.equal(true);

            block._getConstructorJSONForm.restore();
        });

        it('Если включена стратегия Только на поиске, то отрисуются формы конструктора common-simple, common-wizard', function() {
            createBlock('search-only');

            sinon.spy(block, '_getConstructorJSONForm');

            block.elem('toggle').click();
            expect(block._getConstructorJSONForm.calledWith('simple', 'common')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('wizard', 'common')).to.be.equal(true);

            block._getConstructorJSONForm.restore();
        });
    });

    describe('#_prepareData', function() {
        var data;

        it('Старые данные, обычная стратегия, вкладка "единая цена", везде', function() {
            savedData = JSON.parse('{"tab_simple":1,"wizard_search_position_ctr_correction":"100","wizard_search_proc":30,"wizard_context_phrases":1,"wizard_search_phrases":1,"wizard_context_retargetings":1,"wizard_search_max_price":100,"wizard_network_max_price":50,"wizard_network_scope":"100","simple_platform":"both","wizard_platform":"both","simple_price":100}');
            createBlock('default');

            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['common-simple'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });

        });

        it('Старые данные, обычная стратегия, вкладка "мастер цен", в сетях', function() {
            savedData = JSON.parse('{"tab_simple":0,"wizard_search_position_ctr_correction":"100","wizard_search_proc":30,"wizard_context_phrases":0,"wizard_search_phrases":1,"wizard_context_retargetings":1,"wizard_search_max_price":100,"wizard_network_max_price":40,"wizard_network_scope":"60","simple_platform":"network","wizard_platform":"network","simple_price":40}');
            createBlock('default');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['common-wizard'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Старые данные, отдельное размещение, единая цена', function() {
            savedData = JSON.parse('{"tab_simple":1,"wizard_search_phrases":1,"search_position_ctr_correction":"100","search_proc":304,"search_max_price":43,"wizard_search":1,"single_price":43,"search_proc_ctx":303,"wizard_ctx_retargetings":1,"wizard_ctx_phrases":0,"wizard_ctx":1,"ctx_scope":100,"ctx_proc":303,"ctx_max_price":13,"single_price_ctx":13}');
            createBlock('diff-both');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['both-simple'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Старые данные, отдельное размещение, мастер цен', function() {
            savedData = JSON.parse('{"tab_simple":0,"wizard_search_phrases":1,"search_position_ctr_correction":"100","search_proc":43,"search_max_price":2,"wizard_search":1,"single_price":2,"search_proc_ctx":303,"wizard_ctx_retargetings":0,"wizard_ctx_phrases":1,"wizard_ctx":1,"ctx_scope":"60","ctx_proc":303,"ctx_max_price":13,"single_price_ctx":13}');
            createBlock('diff-both');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['both-wizard'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Старые данные, отдельное размещение, отключен поиск, единая цена', function() {
            savedData = JSON.parse('{"tab_simple":1,"search_position_ctr_correction":"100","search_proc":30,"search_max_price":300,"wizard_search":0,"single_price":300,"search_proc_ctx":30,"wizard_ctx_retargetings":1,"wizard_ctx_phrases":1,"wizard_ctx":1,"ctx_scope":"100","ctx_proc":30,"ctx_max_price":40,"single_price_ctx":40}');
            createBlock('diff-stop');
            block._campModel = campaignModel;
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['context-simple'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Старые данные, отдельное размещение, отключен поиск, мастер цен', function() {
            savedData = JSON.parse('{"tab_simple":0,"search_position_ctr_correction":"100","search_proc":30,"search_max_price":300,"wizard_search":0,"single_price":300,"search_proc_ctx":330,"wizard_ctx_retargetings":0,"wizard_ctx_phrases":1,"wizard_ctx":1,"ctx_scope":"40","ctx_proc":330,"ctx_max_price":40,"single_price_ctx":40}');
            createBlock('diff-stop');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['context-wizard'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Новые данные, обычная стратегия, вкладка "единая цена", везде', function() {
            savedData = JSON.parse('{"price_context":40,"price_search":100,"platform":"both","position_ctr_correction":"100","context_scope":"60","proc_search":30,"proc_context":30,"phrases_context":0,"phrases_search":1,"retargetings_context":1,"search_toggle":1,"context_toggle":1,"common_toggle":1,"is_simple":1,"collapsed":1,"current_camp_cid":"261"}');
            createBlock('default');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['common-simple'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Новые данные, обычная стратегия, вкладка "мастер цен", в сетях', function() {
            savedData = JSON.parse('{"price_context":40,"price_search":100,"platform":"context","position_ctr_correction":"100","context_scope":"50","proc_search":30,"proc_context":30,"phrases_context":0,"phrases_search":1,"retargetings_context":1,"search_toggle":1,"context_toggle":1,"common_toggle":1,"is_simple":0,"collapsed":1}');
            createBlock('default');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['common-wizard'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Новые данные, отдельное размещение, единая цена', function() {
            savedData = JSON.parse('{"price_context":13,"price_search":2,"platform":"search","position_ctr_correction":"100","context_scope":"60","proc_search":43,"proc_context":303,"phrases_context":1,"phrases_search":1,"retargetings_context":0,"search_toggle":1,"context_toggle":1,"common_toggle":1,"is_simple":1,"collapsed":1}');
            createBlock('diff-both');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['both-simple'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Новые данные, отдельное размещение, мастер цен', function() {
            savedData = JSON.parse('{"price_context":13,"price_search":2,"platform":"search","position_ctr_correction":"100","context_scope":"60","proc_search":43,"proc_context":303,"phrases_context":1,"phrases_search":1,"retargetings_context":0,"search_toggle":1,"context_toggle":1,"common_toggle":1,"is_simple":0,"collapsed":1,"current_camp_cid":"263"}');
            createBlock('diff-both');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['both-wizard'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Новые данные, отдельное размещение, отключен поиск, единая цена', function() {
            savedData = JSON.parse('{"price_context":40,"price_search":300,"platform":"search","position_ctr_correction":"100","context_scope":"40","proc_search":30,"proc_context":330,"phrases_context":1,"phrases_search":1,"retargetings_context":0,"search_toggle":0,"context_toggle":1,"common_toggle":1,"is_simple":1,"collapsed":1}');
            createBlock('diff-stop');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['context-simple'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

        it('Новые данные, отдельное размещение, отключен поиск, мастер цен', function() {
            savedData = JSON.parse('{"price_context":40,"price_search":300,"platform":"search","position_ctr_correction":"100","context_scope":"40","proc_search":30,"proc_context":330,"phrases_context":1,"phrases_search":1,"retargetings_context":0,"search_toggle":0,"context_toggle":1,"common_toggle":1,"is_simple":1,"collapsed":1,"current_camp_cid":"1578"}');
            createBlock('diff-stop');
            data = block._prepareData(savedData);

            block.CLIENT_WHITE_LIST['context-wizard'].forEach(function(name) {
                expect(data[name]).not.to.be.undefined;
            });
        });

    });

    describe('Сохранение на сервер', function() {
        var constructorTypes = [
            {
                title: 'простая стратегия, вкладка "единая цена"',
                strategyName: 'default',
                tab: 'simple',
                expectedAjaxAutoPriceFields: ['tab_simple', 'simple_platform', 'simple_price'],
                expectedAjaxCampOptionsFields: ['price_context', 'price_search', 'platform', 'is_simple']
            },
            {
                title: 'простая стратегия, вкладка "мастер цен"',
                strategyName: 'default',
                tab: 'wizard',
                expectedAjaxAutoPriceFields: ['tab_simple', 'simple_platform', 'simple_price'],
                expectedAjaxCampOptionsFields: [
                    'price_context',
                    'price_search',
                    'platform',
                    'is_simple',
                    'position_ctr_correction',
                    'context_scope',
                    'proc_search',
                    'proc_base_search'
                ]
            },
            {
                title: 'отдельное размещение, вкладка "единая цена"',
                strategyName: 'diff-both',
                tab: 'simple',
                expectedAjaxAutoPriceFields: [
                    'tab_simple',
                    'wizard_ctx',
                    'single_price_ctx',
                    'single_price',
                    'wizard_search'
                ],
                expectedAjaxCampOptionsFields: [
                    'price_context',
                    'price_search',
                    'platform',
                    'is_simple',
                    'context_toggle',
                    'search_toggle'
                ]
            },
            {
                title: 'отдельное размещение, вкладка "мастер цен"',
                strategyName: 'diff-both',
                tab: 'wizard',
                expectedAjaxAutoPriceFields: [
                    'tab_simple',
                    'wizard_ctx',
                    'ctx_max_price',
                    'search_proc_ctx',
                    'wizard_ctx_retargetings',
                    'wizard_ctx_phrases',
                    'ctx_scope',
                    'ctx_proc',
                    'search_max_price',
                    'wizard_search',
                    'wizard_search_phrases',
                    'search_position_ctr_correction',
                    'search_proc'
                ],
                expectedAjaxCampOptionsFields: [
                    'price_context',
                    'price_search',
                    'is_simple',
                    'context_toggle',
                    'search_toggle',
                    'position_ctr_correction',
                    'context_scope',
                    'proc_search',
                    'proc_context',
                    'proc_base_search'
                ]
            },
            {
                title: 'отдельное размещение, поиск отключен, вкладка "единая цена"',
                strategyName: 'diff-stop',
                tab: 'simple',
                expectedAjaxAutoPriceFields: ['tab_simple', 'single_price_ctx', 'wizard_ctx'],
                expectedAjaxCampOptionsFields: ['price_context', 'context_toggle', 'is_simple']
            },
            {
                title: 'отдельное размещение, поиск отключен, вкладка "мастер цен"',
                strategyName: 'diff-stop',
                tab: 'wizard',
                expectedAjaxAutoPriceFields: [
                    'tab_simple',
                    'wizard_ctx',
                    'ctx_max_price',
                    'search_proc_ctx',
                    'wizard_ctx_retargetings',
                    'wizard_ctx_phrases',
                    'ctx_scope',
                    'ctx_proc'
                ],
                expectedAjaxCampOptionsFields: [
                    'price_context',
                    'proc_context',
                    'is_simple',
                    'context_toggle',
                    'context_scope'
                ]
            },
            {
                title: 'стратегия только на поиске, вкладка "единая цена"',
                strategyName: 'search-only',
                tab: 'simple',
                expectedAjaxAutoPriceFields: [
                    'tab_simple',
                    'simple_platform',
                    'simple_price'
                ],
                expectedAjaxCampOptionsFields: [
                    'price_context',
                    'price_search',
                    'platform',
                    'is_simple'
                ]
            },
            {
                title: 'стратегия только на поиске, вкладка "мастер цен"',
                strategyName: 'search-only',
                tab: 'simple',
                expectedAjaxAutoPriceFields: [
                    'tab_simple',
                    'wizard_search_phrases',
                    'wizard_network_scope',
                    'wizard_context_retargetings',
                    'wizard_context_phrases',
                    'wizard_platform',
                    'wizard_search_proc',
                    'wizard_search_max_price',
                    'wizard_network_max_price',
                    'wizard_search_position_ctr_correction'
                ],
                expectedAjaxCampOptionsFields: [
                    'price_context',
                    'price_search',
                    'platform',
                    'is_simple',
                    'position_ctr_correction',
                    'context_scope',
                    'proc_search'
                ]
            }
        ];

        describe('Данные отдаваемые в campOptions', function() {
            constructorTypes.forEach(function(strategyInfo) {
                it(strategyInfo.title, function() {
                    createBlock(strategyInfo.strategyName);
                    block.elem('toggle').click();

                    var data = block.filterToCampOptionsFormat(
                        block.getDataFromPricesConstructor(),
                        strategiesHash[strategyInfo.strategyName],
                        strategyInfo.tab
                    );


                    Object.keys(data).forEach(function(fieldName) {
                        expect(strategyInfo.expectedAjaxCampOptionsFields.indexOf(fieldName)).not.to.be.equal('-1');
                    });
                });
            });
        });

        describe('Данные, передаваемые в setAutoPriceAjax', function() {
            constructorTypes.forEach(function(strategyInfo) {
                it(strategyInfo.title, function() {
                    createBlock(strategyInfo.strategyName);
                    block.elem('toggle').click();

                    var data = block.convertToServerFormat(
                        strategiesHash[strategyInfo.strategyName],
                        strategyInfo.tab,
                        block.getDataFromPricesConstructor()
                    );

                    Object.keys(data).forEach(function(fieldName) {
                        expect(strategyInfo.expectedAjaxAutoPriceFields.indexOf(fieldName)).not.to.be.equal('-1');
                    });
                });
            });
        });
    });

    describe('b-offline-set-phrases-prices_type_cpm-banner', function() {
        beforeEach(function() {
            createBlock('cpm-banner-strategy', 'cpm-banner', { name: 'dm-cpm-banner-campaign' });
        });

        it('Должна быть отрисована только форма конструктора ставок context-simple', function() {
            sinon.spy(block, '_getConstructorJSONForm');

            block.elem('toggle').click();

            expect(block._getConstructorJSONForm.calledWith('simple', 'context')).to.be.equal(true);
            expect(block._getConstructorJSONForm.calledWith('wizard', 'context')).to.be.equal(false);

            block._getConstructorJSONForm.restore();
        });

        it('Данные отдаваемые в campOptions корректны', function() {
            block.elem('toggle').click();

            var data = block.filterToCampOptionsFormat(
                block.getDataFromPricesConstructor(),
                strategiesHash['cpm-banner-strategy'],
                'simple'
            );

            expect(data.price_context).not.to.be.undefined;
        });

        it('Данные отдаваемые в setAutoPriceAjax корректны', function() {
            block.elem('toggle').click();

            var data = block.convertToServerFormat(
                strategiesHash['cpm-banner-strategy'],
                'simple',
                block.getDataFromPricesConstructor()
            );

            Object.keys(data).forEach(function(fieldName) {
                expect(['tab_simple', 'simple_platform', 'simple_price'].indexOf(fieldName)).not.to.be.equal('-1');
            });
        });
    })
});
