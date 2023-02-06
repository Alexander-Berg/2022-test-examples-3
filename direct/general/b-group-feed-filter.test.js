describe('b-group-feed-filter', function() {
    var sandbox,
        /**
         * Хэш вариантов net поля стратегий
         * @type {Object}
         */
        strategies = [
            {
                name: 'autobudget_avg_cpc_per_camp',
                //дополнительное поле, чтобы определять для какого типа группы данную стратегию брать
                adgroup_type: 'performance',
                is_search_stop: 1,
                is_net_stop: 0,
                is_autobudget: 1,
                search: { name: 'stop' },
                net: {
                    name: 'autobudget_avg_cpc_per_camp',
                    avg_bid: '', // средняя цена клика
                    bid: '', // максимальная цена клика
                    sum: '' // недельный бюджет
                }
            },
            {
                name: 'autobudget_avg_cpc_per_filter',
                is_search_stop: 1,
                adgroup_type: 'performance',
                is_net_stop: 0,
                is_autobudget: 1,
                search: { name: 'stop' },
                net: {
                    name: 'autobudget_avg_cpc_per_filter',
                    filter_avg_bid: '', // цена на фильтр по умолчанию
                    bid: '', // максимальная цена клика
                    sum: '' // недельный бюджет
                }
            },
            {
                name: 'autobudget_avg_cpa_per_camp',
                is_search_stop: 1,
                adgroup_type: 'performance',
                is_net_stop: 0,
                is_autobudget: 1,
                search: { name: 'stop' },
                net: {
                    name: 'autobudget_avg_cpa_per_camp',
                    goal_id: '',
                    avg_cpa: '',
                    bid: '', // максимальная цена клика
                    sum: '', // недельный бюджет
                    now_optimizing_by: 'CPA' // required if autobudget_avg_cpa
                }

            },
            {
                name: 'autobudget_avg_cpa_per_filter',
                is_search_stop: 1,
                adgroup_type: 'performance',
                is_net_stop: 0,
                is_autobudget: 1,
                search: { name: 'stop' },
                net: {
                    name: 'autobudget_avg_cpa_per_filter',
                    goal_id: '',
                    filter_avg_cpa: '',
                    bid: '', // максимальная цена клика
                    sum: '', // недельный бюджет
                    now_optimizing_by: 'CPA' // required if autobudget_avg_cpa
                }

            },
            //автобюджетная стратегия
            {
                name: 'default',
                adgroup_type: 'dynamic',
                is_search_stop: 1,
                is_net_stop: 0,
                is_autobudget: 1,
                search: { name: 'stop' },
                net: {
                    name: 'default',
                    goal_id: ''
                }

            },
            //неавтобюджетная стратегий
            {
                "is_autobudget": false,
                adgroup_type: 'dynamic',
                "name": "default",
                "is_search_stop": false,
                "is_net_stop": false,
                "net": {
                  "name": "default"
                },
                "search": {
                  "name": "default"
                }
            },
            //отдельное размещение с показами на поиске
            {
                "is_autobudget": false,
                "name": "different_places",
                adgroup_type: 'dynamic',
                "is_search_stop": false,
                "is_net_stop": false,
                "net": {
                  "name": "default"
                },
                "search": {
                  "name": "default"
                }
            },
            //отдельное ращмещение, показы на поиске отключены
            {
                "is_autobudget": false,
                "name": "different_places",
                adgroup_type: 'dynamic',
                "is_search_stop": true,
                "is_net_stop": false,
                "net": {
                  "name": "default"
                },
                "search": {
                  "name": "stop"
                }
            }
        ],
        /**
         * Список вариантов возможностей редактирования компонентов фильтров
         * @type {Object[]}
         */
        editableVariants = [
            { settings: true, price: true },
            { settings: true, price: false },
            { settings: false, price: false },
            { settings: false, price: true }
        ],

        /**
         * Возвращает массив объектов фильтров для создания моделей фильтров
         * @param {Number} groupId идентификатор группы
         * @param {String} adgroupType тип группы
         * @param {Object} [editable] хэш с настройками возможности редактировать компоненты фильтра
         * @param {Boolean} [editable.settings=true] флаг о том, что можно редактировать настройки фильтра
         * @param {Boolean} [editable.price=true] флаг о том, что можно редактировать ставки фильтра
         * @returns {Object[]}
         */
        getFiltersData = function(groupId, adgroupType, editable) {
            editable || (editable = {});

            // по умолчанию true
            editable.settings === false || (editable.settings = true);
            editable.price === false || (editable.price = true);

            return ({
                // набор фильтров при варианте, что редактировать можно всё
                true: [
                    // is_suspended: false
                    // has_default_price: true
                    // use_default_price: false
                    {
                        filter_id: 12340 + groupId + 50,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: 12,
                        price_cpa: 13,
                        price: 14,
                        price_context: 15,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: true,
                        is_price_editable: true,
                        has_default_price: true,
                        use_default_price: false,
                        is_suspended: false,
                        is_deleted: false
                    },
                    // is_suspended: false
                    // has_default_price: true
                    // use_default_price: true
                    {
                        filter_id: 12340 + groupId + 100,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: undefined,
                        price_cpa: undefined,
                        price: undefined,
                        price_context: undefined,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: true,
                        is_price_editable: true,
                        has_default_price: true,
                        use_default_price: true,
                        is_suspended: false,
                        is_deleted: false
                    },
                    // is_suspended: false
                    // has_default_price: false
                    // use_default_price: false
                    {
                        filter_id: 12340 + groupId + 150,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: 12,
                        price_cpa: 13,
                        price: 14,
                        price_context: 15,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: true,
                        is_price_editable: true,
                        has_default_price: false,
                        use_default_price: false,
                        is_suspended: false,
                        is_deleted: false
                    },
                    // is_suspended: true
                    // has_default_price: true
                    // use_default_price: true
                    {
                        filter_id: 12340 + groupId + 200,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: undefined,
                        price_cpa: undefined,
                        price: undefined,
                        price_context: undefined,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: true,
                        is_price_editable: true,
                        has_default_price: true,
                        use_default_price: true,
                        is_suspended: true,
                        is_deleted: false
                    },
                    // is_suspended: true
                    // has_default_price: false
                    // use_default_price: false
                    {
                        filter_id: 12340 + groupId + 250,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: 12,
                        price_cpa: 13,
                        price: 14,
                        price_context: 15,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: true,
                        is_price_editable: true,
                        has_default_price: false,
                        use_default_price: false,
                        is_suspended: true,
                        is_deleted: false
                    }
                ],
                // набор фильтров при варианте, при котором редактирование компонентов фильтров или полностью запрещено
                // или запрещено редактирование либо ставки либо настроек фильтров
                false: [
                    // has_default_price: false
                    // use_default_price: false
                    {
                        filter_id: 12340 + groupId + 350,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: 12,
                        price_cpa: 13,
                        price: 14,
                        price_context: 15,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: editable.settings,
                        is_price_editable: editable.price,
                        has_default_price: false,
                        use_default_price: false,
                        is_suspended: false,
                        is_deleted: false
                    },
                    // has_default_price: true
                    // use_default_price: false
                    {
                        filter_id: 12340 + groupId + 300,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: undefined,
                        price_cpa: undefined,
                        price: undefined,
                        price_context: undefined,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: editable.settings,
                        is_price_editable: editable.price,
                        has_default_price: true,
                        use_default_price: false,
                        is_suspended: false,
                        is_deleted: false
                    },
                    // has_default_price: true,
                    // use_default_price: true
                    {
                        filter_id: 12340 + groupId + 300,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: 12,
                        price_cpa: 13,
                        price: 14,
                        price_context: 15,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: editable.settings,
                        is_price_editable: editable.price,
                        has_default_price: true,
                        use_default_price: true,
                        is_suspended: false,
                        is_deleted: false
                    },
                    // is_suspended: true
                    // has_default_price: true,
                    // use_default_price: true
                    {
                        filter_id: 12340 + groupId + 350,
                        adgroup_id: groupId,
                        adgroup_type: adgroupType,
                        filter_name: 'filter One1 - ' + groupId,
                        price_cpc: 12,
                        price_cpa: 13,
                        price: 14,
                        price_context: 15,
                        target_funnel: 'product_page_visit',
                        now_optimizing_by: 'CPC',
                        ctx_shows: 19,
                        ctx_clicks: 91,
                        ctx_ctr: 7.45,
                        condition: [
                            {
                                relation: 'less',
                                value: 9876446623,
                                field: 'id'
                            }
                        ],
                        is_settings_editable: editable.settings,
                        is_price_editable: editable.price,
                        has_default_price: true,
                        use_default_price: true,
                        is_suspended: true,
                        is_deleted: false
                    }
                ]
            })[editable.settings && editable.price];
        },

        /**
         * Возвращает массив объектов баннеров
         * @returns {Object[]}
         */
        getBannerData = function() {
            return [
                {
                    bid: 55875,
                    creative: {
                        width: '640',
                        sum_geo: '225',
                        creative_id: '2001',
                        statusModerate: 'New',
                        name: 'creative name',
                        height: '480',
                        alt_text: 'alt_text',
                        preview_url: '//creative.org/preview',
                        href: 'http://creative.org/'
                    },
                    enable: 1,
                    BannerID: '0',
                    statusShow: 'Yes',
                    newBannerIndex: 1,
                    modelId: 55875,
                    cid: 'new'
                }
            ];
        },

        /**
         * Возвращает объект для создания модели группы
         * @param {Number} cid идентификатор кампании
         * @param {Number} groupId идентификатор группы
         * @param {'dynamic', 'performance'} adgroupType тип группы
         * @param {Object} [editable] хэш с настройками возможности редактировать компоненты фильтра
         * @param {Boolean} [editable.settings=true] флаг о том, что можно редактировать настройки фильтра
         * @param {Boolean} [editable.price=true] флаг о том, что можно редактировать ставки фильтра
         * @returns {Object}
         */
        getGroupData = function(cid, groupId, adgroupType, editable) {
            var banners = getBannerData();

            return {
                cid: cid,
                adgroup_id: groupId,
                adgroup_type: adgroupType,
                is_completed_group: 1,
                banners_quantity: '1',
                banners_arch_quantity: '0',
                geo: '1',
                minus_words: [],
                banners: banners,
                feed_filters: getFiltersData(groupId, adgroupType, editable),
                group_name: 'Новая группа объявлений',
                firstBid: banners[0].bid,
                newGroupIndex: null,
                modelId: groupId,
                isNotEmptyGroup: true,
                isEmptyGroup: false
            };
        },

        /**
         * Создаёт и возвращает блок фильтра
         * @param {Object} params набор параметров для блока
         * @param {Object} params.strategy объект стратегии
         * @param {Object} params.filter объект фильтра для шаблонизации
         * @param {Boolean} [params.showStat] флаг о том, что необходимо показать колонки статистики в блоке
         * @param {Boolean} [inject] флаг о том, что блок надо встроить в DOM (для проверки live событий необходимо помещать блок в DOM)
         * @returns {BEM.DOM}
         */
        createBlock = function(params, inject) {
            var template = {
                block: 'b-group-feed-filter',
                mods: { strategy: u.campaign.getFilterPricesControlsMod(params.group.adgroup_type,  params.strategy) },
                group: params.group,
                filter: params.filter,
                showStat: params.showStat
            };

            return u.createBlock(template, {
                // только для отладки в браузере
                //sandboxElement: { block: 'b-style-table', mods: { theme: 'bordered' }, tag: 'table' },
                //visible: true,
                sandboxElement: { tag: 'table' },
                inject: inject
            });
        },


        getGroupModel = function(cid, groupId, adgroupType, editable) {
            return BEM.MODEL.create(
                { name: u.campaign.getGroupModelName(adgroupType), id: groupId },
                getGroupData(cid, groupId, adgroupType, editable));
        },

        getCampaignModel = function(cid, mediaType, strategy) {
            return BEM.MODEL.create({ name: u.campaign.getCampaignModelName(mediaType), id: cid }, {
                cid: cid,
                currency: 'RUB',
                is_autobudget: 1,
                strategy: strategy,
                autobudget: 'Yes'
            });
        },

        /**
         * Находит и возвращает блок редактирования ставки внутри блока фильтра
         * @param {BEM.DOM} block блок фильтра
         * @param {String} type тип блока редактирования ставки (его модификатор control-type)
         * @returns {BEM.DOM|null}
         */
        getEditPriceControl = function(block, type) {
            return block.findBlockOn('edit-price', { block: 'b-edit-phrase-price', modName: 'control-type', modVal: type })
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
        sandbox.restore();
    });

    strategies.forEach(function(strategy) {
        var adgroupType = strategy.adgroup_type;

        describe('Для стратегии ' + JSON.stringify(strategy), function() {
            var commonCells = u.campaign.getFilterPricesControlsNames(adgroupType, strategy) || ['autobudget'],
                hasDefaultPrice = ({
                    autobudget_avg_cpc_per_camp: true,
                    autobudget_avg_cpa_per_camp: true
                })[strategy.name] || false;

            editableVariants.forEach(function(editable, variantIndex) {
                var title = [
                    'Нет прав на редактирование фильтров и ставок',
                    'Нет прав на редактирование фильтров, но есть на ставки',
                    'Есть права на редактирование фильтров, но не ставок',
                    'Есть права на редактирование фильтров и ставок'
                ][2 * editable.settings + editable.price];

                describe(title, function() {
                    var campaignDM,
                        groupDM,
                        filtersModelsList;

                    beforeEach(function() {
                        var cid = +u._.uniqueId(variantIndex),
                            groupId = +u._.uniqueId(variantIndex);

                        campaignDM = getCampaignModel(cid, adgroupType, strategy);

                        groupDM = getGroupModel(cid, groupId, adgroupType, editable);

                        filtersModelsList = groupDM.get('feed_filters');
                    });

                    afterEach(function() {
                        [groupDM, campaignDM].forEach(function(model) { model.destruct(); });
                    });

                    describe('состав блока:', function() {

                        describe('общее:', function() {
                            var block;

                            beforeEach(function() {
                                block = createBlock({
                                    group: groupDM.toJSON(),
                                    strategy: strategy,
                                    filter: filtersModelsList.getByIndex(1).toJSON()
                                });
                            });

                            afterEach(function() {
                                block.destruct();
                            });

                            it('элемент блока имеет якорь f-<значение идентификатора фильтра>', function() {
                                expect(block.domElem.attr('id')).to.be.equal('f-' + block.model.get('filter_id'));
                            });

                        });

                        [true, false].forEach(function(showStat) {

                            describe(['без колонок', 'с колонками'][+showStat] + ' статистики:', function() {
                                //DIRECT-54211 - временно скрываем ctr и click для смартов
                                var statCells = { stat: adgroupType == 'performance' ? ['clicks'] : ['shows', 'clicks', 'ctr']},
                                    block;

                                beforeEach(function() {
                                    block = createBlock({
                                        group: groupDM.toJSON(),
                                        strategy: strategy,
                                        filter: filtersModelsList.getByIndex(1).toJSON(),
                                        showStat: showStat
                                    });
                                });

                                afterEach(function() {
                                    block.destruct();
                                });

                                u.forOwnMods(statCells, function(modVal, modName) {

                                    it(['отсутствует', 'имеется'][+showStat] + ' элемент cell_' + [modName, modVal].join('_'), function() {
                                        (showStat ? expect(block) : expect(block).not)
                                            .to.haveElem('cell', modName, modVal);
                                    });

                                });

                            });

                        });

                    });

                    if (editable.settings) {

                        describe('реакция на изменение в модели', function() {

                            describe('действия с полем is_deleted', function() {
                                var block;

                                beforeEach(function() {
                                    block = createBlock({
                                        group: groupDM.toJSON(),
                                        strategy: strategy,
                                        filter: filtersModelsList.getByIndex(1).toJSON()
                                    });
                                });

                                afterEach(function() {
                                    block.destruct();
                                });

                                it('при изменении поля is_deleted в значение true блок имеет модификатор _is-deleted_yes', function() {
                                    block.model.set('is_deleted', true);

                                    expect(block).to.haveMod('is-deleted', 'yes');
                                });

                                it('при изменении поля is_deleted в значение true появляется элемент __restore-row', function() {
                                    block.model.set('is_deleted', true);

                                    expect(block).to.haveElem('restore-row');
                                });

                                it('при изменении поля is_deleted в значение true появляется элемент __restore', function() {
                                    block.model.set('is_deleted', true);

                                    expect(block).to.haveElem('restore');
                                });

                                it('при изменении поля is_deleted из значения true в false с блока снимается модификатор _is-deleted_yes', function() {
                                    block.model
                                        .set('is_deleted', true)
                                        .set('is_deleted', false);

                                    expect(block).not.to.haveMod('is-deleted', 'yes');
                                });

                            });

                            describe('действия с полем is_suspended', function() {

                                [false, true].forEach(function(isSuspended) {

                                    describe('значение поля при создании блока ' + isSuspended, function() {
                                        var block;

                                        beforeEach(function() {
                                            block = createBlock({
                                                group: groupDM.toJSON(),
                                                strategy: strategy,
                                                filter: filtersModelsList.getByIndex(isSuspended ? 3 : 2).toJSON()
                                            });
                                        });

                                        afterEach(function() {
                                            block.destruct();
                                        });

                                        it('при изменении поля is_suspended в значение ' + !isSuspended + ' блок' + [' ', ' не '][+isSuspended] + 'имеет модификатор _is-suspended_yes', function() {
                                            block.model.set('is_suspended', !isSuspended);

                                            (isSuspended ? expect(block).not : expect(block))
                                                .to.haveMod('is-suspended', 'yes');
                                        });

                                        it('при изменении поля is_suspended в значение ' + !isSuspended + ' блок b-feed-filter_editable_yes' + [' ', ' не '][+isSuspended] + 'имеет модификатор _is-suspended_yes', function() {
                                            var filterBlock = block.findBlockInside({ block: 'b-feed-filter', modName: 'editable', modVal: 'yes' });

                                            block.model.set('is_suspended', !isSuspended);

                                            (isSuspended ? expect(filterBlock).not : expect(filterBlock))
                                                .to.haveMod('is-suspended', 'yes');
                                        });
                                    });
                                });
                            });

                        });
                    }

                    if (editable.settings && editable.price) {

                        describe('состав блока:', function() {

                            describe('общее:', function() {
                                var block;

                                beforeEach(function() {
                                    block = createBlock({
                                        group: groupDM.toJSON(),
                                        strategy: strategy,
                                        filter: filtersModelsList.getByIndex(hasDefaultPrice ? 1 : 2).toJSON()
                                    });
                                });

                                afterEach(function() {
                                    block.destruct();
                                });

                                it('существует блок b-feed-filter_editable_yes на элементе contents', function() {
                                    expect(block).to.haveBlocks(
                                        'contents',
                                        { block: 'b-feed-filter', modName: 'editable', modVal: 'yes' });
                                });

                                commonCells.forEach(function(type) {
                                    if (type == 'autobudget') return;

                                    it('существует блок b-edit-phrase-price_control-type_' + type + ' на элементе edit-price', function() {
                                        expect(block).to.haveBlock(
                                            'edit-price',
                                            { block: 'b-edit-phrase-price', modName: 'control-type', modVal: type });
                                    });

                                });

                                it(['не ', ''][+hasDefaultPrice] + 'существует элемент price-table', function() {
                                    (hasDefaultPrice ? expect(block) : expect(block).not)
                                        .to.haveElem('price-table');
                                });

                                it(['не ', ''][+hasDefaultPrice] + 'существует элемент price-mode-toggle-link', function() {
                                    (hasDefaultPrice ? expect(block) : expect(block).not)
                                        .to.haveElem('price-mode-toggle-link');
                                });


                                commonCells.forEach(function(type) {
                                    // для стратегии с установкой ставки на всю кампанию отдельный тест
                                    if (hasDefaultPrice) return;

                                    if (type == 'autobudget') return;

                                    it('значение блока b-edit-phrase-price_control-type_' + type + ' меняется при изменении ставки в модели', function () {
                                        var editPriceControl = getEditPriceControl(block, type),
                                            field = editPriceControl.getFieldName();

                                        block.model.set(field, (block.model.get(field) || 0) + 1);

                                        expect(editPriceControl.getControlValue()).to.be.equal(block.model.get(field, 'format'));
                                    });
                                });

                            });

                        });

                        hasDefaultPrice && describe('дополнительно для стратегии', function() {

                            [true, false].forEach(function(useDefaultPrice) {

                                describe('если ' + ['', 'не '][+useDefaultPrice] + 'заданы ставки на фильтре', function() {

                                    describe('то на элементе price-table', function() {
                                        var block;

                                        beforeEach(function() {
                                            block = createBlock({
                                                group: groupDM.toJSON(),
                                                strategy: strategy,
                                                filter: filtersModelsList.getByIndex(useDefaultPrice ? 1 : 0).toJSON()
                                            });
                                        });

                                        afterEach(function() {
                                            block.destruct();
                                        });

                                        [false, true].forEach(function(inverted) {
                                            var titleTail = inverted ? ' при инверсии значения поля use_default_price в модели фильтра' : '',
                                                isDefaultPrice = inverted ? !useDefaultPrice : useDefaultPrice;

                                            it(['не ', ''][+isDefaultPrice] + 'имеется модификатор _collapsed_yes' + titleTail, function() {
                                                inverted && block.model.set('use_default_price', !block.model.get('use_default_price'));

                                                (isDefaultPrice ? expect(block) : expect(block).not)
                                                    .to.haveMod(block.elem('price-table'), 'collapsed', 'yes');
                                            });

                                        });

                                    });

                                    describe('то в блоках редактирования ставок', function() {
                                        var block;

                                        beforeEach(function() {
                                            block = createBlock({
                                                group: groupDM.toJSON(),
                                                strategy: strategy,
                                                filter: filtersModelsList.getByIndex(useDefaultPrice ? 1 : 0).toJSON()
                                            });
                                        });

                                        afterEach(function() {
                                            block.destruct();
                                        });

                                        commonCells.forEach(function(type) {

                                            describe('значение блока b-edit-phrase-price_control-type_' + type, function() {
                                                var editPriceControl;

                                                beforeEach(function() {
                                                    editPriceControl = getEditPriceControl(block, type);
                                                });

                                                [false, true].forEach(function(useDefaultPrice) {

                                                    useDefaultPrice ?
                                                        it('не меняется при инверсии значения поля use_default_price в модели фильтра', function() {
                                                            var expectedValue = editPriceControl.getControlValue();

                                                            block.model.set('use_default_price', !block.model.get('use_default_price'));

                                                            expect(editPriceControl.getControlValue()).to.be.equal(expectedValue);
                                                        }) :
                                                        it('при отрисовке эквивалентно значению в модели', function() {
                                                            expect(editPriceControl.getControlValue())
                                                                .to.be.equal(block.model.get(editPriceControl.getFieldName(), 'format'));
                                                        });

                                                    useDefaultPrice ?
                                                        it('не меняется при изменении ставки в модели и заданном в модели поля use_default_price как ' + useDefaultPrice, function() {
                                                            var field = editPriceControl.getFieldName(),
                                                                expectedValue = editPriceControl.getControlValue();

                                                            block.model.set('use_default_price', true);

                                                            block.model.set(field, (block.model.get(field) || 0) + 1);

                                                            expect(editPriceControl.getControlValue()).to.be.equal(expectedValue);
                                                        }) :
                                                        it('меняется при изменении ставки в модели и заданном в модели поля use_default_price как ' + useDefaultPrice, function() {
                                                            var field = editPriceControl.getFieldName();

                                                            block.model.set('use_default_price', false);

                                                            block.model.set(field, (block.model.get(field) || 0) + 1);

                                                            expect(editPriceControl.getControlValue()).to.be.equal(block.model.get(field, 'format'));
                                                        });

                                                });

                                            });

                                        });

                                    });

                                });

                            });

                        });
                    } else {

                        describe('общее:', function() {
                            var block;

                            beforeEach(function() {
                                block = createBlock({
                                    group: groupDM.toJSON(),
                                    strategy: strategy,
                                    filter: filtersModelsList.getByIndex(1).toJSON()
                                });
                            });

                            afterEach(function() {
                                block.destruct();
                            });

                            if (editable.settings) {
                                it('существует блок b-feed-filter_editable_yes на элементе contents', function() {
                                    expect(block).to.haveBlocks(
                                        'contents',
                                        { block: 'b-feed-filter', modName: 'editable', modVal: 'yes' });
                                });
                            } else {
                                it('существует блок b-feed-filter на элементе contents', function() {
                                    expect(block).to.haveBlocks(
                                        'contents',
                                        { block: 'b-feed-filter' });
                                });

                                it('блок b-feed-filter не имеет модификатора _editable_yes', function() {
                                    expect(block.findBlockOn('contents', 'b-feed-filter'))
                                        .not.to.haveMod('editable', 'yes');
                                });
                            }

                            if (!editable.price) {
                                commonCells.forEach(function(type) {
                                    if (type == 'autobudget') return;

                                    it('не существует блок b-edit-phrase-price_control-type_' + type + ' на элементе edit-price', function() {
                                        expect(block).not.to.haveBlock(
                                            'edit-price',
                                            { block: 'b-edit-phrase-price', modName: 'control-type', modVal: type });
                                    });
                                });

                                it('не существует элемент price-table', function() {
                                    expect(block).not.to.haveElem('price-table');
                                });

                                it('не существует элемент price-mode-toggle-link', function() {
                                    expect(block).not.to.haveElem('price-mode-toggle-link');
                                });

                            }
                        });

                        if (editable.price) {
                            describe('Есть подсветка ошибок/предупреждений на ставках при отрисовке блока', function() {
                                (adgroupType == 'dynamic' ?
                                    [
                                        { k: 1.5, modName: 'warning', constName: 'BIG_RATE', title: 'если ставки больше BIG_RATE' },
                                        { k: 0.5, modName: 'error', constName: 'MIN_PRICE', title: 'если ставки меньше MIN_PRICE' },
                                        { k: 1.5, modName: 'error', constName: 'MAX_PRICE', title: 'если ставки больше MAX_PRICE' }
                                    ]:
                                    [
                                        { k: 1.5, modName: 'warning', constName: 'BIG_RATE', title: 'если ставки больше BIG_RATE' },
                                        { k: 0.5, modName: 'error', constName: 'MIN_CPC_CPA_PERFORMANCE', title: 'если ставки меньше MIN_CPC_CPA_PERFORMANCE' },
                                        { k: 1.5, modName: 'error', constName: 'MAX_PRICE', title: 'если ставки больше MAX_PRICE' }
                                    ]
                                ).forEach(function(variant) {

                                    describe(variant.title, function() {
                                        var block;

                                        beforeEach(function() {
                                            var filter = filtersModelsList.getByIndex(1),
                                                warningValue = variant.k * u.currencies.getConst(filter.get('currency'), variant.constName);

                                            commonCells.forEach(function(type) {
                                                if (type == 'autobudget') return;

                                                var fieldName = type == 'cpc' || type == 'cpa' ?
                                                    'price_' + type :
                                                    type == 'context' ? 'price_context' : 'price';
                                                filter.set(fieldName, warningValue);
                                            });

                                            block = createBlock({
                                                group: groupDM.toJSON(),
                                                strategy: strategy,
                                                filter: filter.toJSON()
                                            }, true);
                                        });

                                        afterEach(function() {
                                            block.destruct();
                                        });

                                        commonCells.forEach(function(type) {

                                            if (type == 'autobudget') return;

                                            it('блок b-edit-phrase-price_control-type_' + type + ' имеет модификатор _' + variant.modName + '_yes', function() {
                                                expect(getEditPriceControl(block, type)).haveMod(variant.modName, 'yes');
                                            });
                                        });

                                    });

                                });

                            });
                        } else {
                            hasDefaultPrice ?
                                describe('при условии, что есть общая ставка на кампанию', function() {

                                    describe('если ставки на фразах не заданы', function() {
                                        var block;

                                        beforeEach(function() {
                                            block = createBlock({
                                                group: groupDM.toJSON(),
                                                strategy: strategy,
                                                filter: filtersModelsList.getByIndex(1).toJSON()
                                            });
                                        });

                                        afterEach(function() {
                                            block.destruct();
                                        });

                                        commonCells.forEach(function(type) {

                                            it('контент блока __cell_edit-price-type_' + type + ' - прочерк', function() {
                                                expect(block.elem('cell', 'edit-price-type', type).html()).to.be.equal('—');
                                            });

                                        });

                                    });

                                    describe('если ставки на фразах заданы', function() {
                                        var block;

                                        beforeEach(function() {
                                            block = createBlock({
                                                group: groupDM.toJSON(),
                                                strategy: strategy,
                                                filter: filtersModelsList.getByIndex(2).toJSON()
                                            });
                                        });

                                        afterEach(function() {
                                            block.destruct();
                                        });

                                        commonCells.forEach(function(type) {

                                            it('контент блока __cell_edit-price-type_' + type + ' соответствует значению модели', function() {
                                                var field = BEM.DOM.blocks['b-edit-phrase-price'].getFieldName(type);

                                                expect(block.elem('cell', 'edit-price-type', type).text()).to.be.equal(block.model.get(field, 'format'));
                                            });

                                        });

                                    });

                                }) :
                                describe('при условии что нет общей ставки на кампании', function() {
                                    var block;

                                    beforeEach(function() {
                                        block = createBlock({
                                            group: groupDM.toJSON(),
                                            strategy: strategy,
                                            filter: filtersModelsList.getByIndex(0).toJSON()
                                        });
                                    });

                                    afterEach(function() {
                                        block.destruct();
                                    });

                                    commonCells.forEach(function(type) {

                                        if (type == 'autobudget') return;

                                        it('контент блока __cell_edit-price-type_' + type + ' соответствует значению модели', function () {
                                            var field = BEM.DOM.blocks['b-edit-phrase-price'].getFieldName(type);

                                            expect(block.elem('cell', 'edit-price-type', type).text()).to.be.equal(block.model.get(field, 'format'));
                                        });
                                    });

                                });
                        }

                    }

                    if (editable.settings || editable.price) {

                        describe('live события', function() {
                            var block;

                            beforeEach(function() {
                                block = createBlock({
                                    group: groupDM.toJSON(),
                                    strategy: strategy,
                                    filter: filtersModelsList.getByIndex(1).toJSON()
                                }, true);
                            });

                            afterEach(function() {
                                block.destruct();
                            });

                            if (editable.settings) {
                                it('после изменении поля is_deleted в значение true клик на элемент __restore возвращает значение поля is_deleted в false', function() {
                                    block.model.set('is_deleted', true);

                                    block.elem('restore').click();

                                    expect(block.model.get('is_deleted')).to.be.equal(false);
                                });
                            }

                            if (editable.price && hasDefaultPrice) {
                                it('при клике на элементе price-mode-toggle-link инвертируется значение поля use_default_price в модели', function() {
                                    var currentValue = block.model.get('use_default_price');

                                    block.elem('price-mode-toggle-link').click();

                                    expect(block.model.get('use_default_price')).to.be.equal(!currentValue);
                                });
                            }

                        });

                    }

                    describe('при destruct DM фильтра', function() {
                        var filterDM,
                            block;

                        beforeEach(function() {
                            filterDM = filtersModelsList.getByIndex(1);

                            block = createBlock({
                                group: groupDM.toJSON(),
                                strategy: strategy,
                                filter: filterDM.toJSON()
                            });
                        });

                        afterEach(function() {
                            block.destruct();
                        });

                        it('вызывается destruct блока', function() {
                            sandbox.spy(block, 'destruct');

                            filterDM.destruct();

                            expect(block.destruct.called).to.be.equal(true);
                        });

                    });

                });

            });

        });

    });

});
