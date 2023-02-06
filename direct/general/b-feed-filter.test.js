describe('b-feed-filter', function() {
    var sandbox,
        campaignDM,
        groupDM,
        /**
         * Возвращает данные для модели кампании
         * @param {Number} cid идентификатор кампании
         * @returns {Object}
         */
        getCampaignData = function(cid) {
            return {
                cid: cid,
                currency: 'RUB',
                is_autobudget: 1,
                strategy: {
                    name: 'autobudget_avg_cpc_per_camp',
                    is_search_stop: 1,
                    is_net_stop: 0,
                    is_autobudget: 1,
                    search: { name: 'stop' },
                    net: {
                        name: 'autobudget_avg_cpc_per_camp',
                        avg_bid: '',
                        bid: '',
                        sum: ''
                    }
                },
                autobudget: 'Yes'
            };
        },

        /**
         * Возвращает данные для модели группы
         * @param {Number} cid идентификатор кампании
         * @param {Number} groupId идентификатор группы
         * @returns {Object}
         */
        getGroupData = function(cid, groupId) {
            var bid = +u._.uniqueId();

            return {
                modelId: groupId,
                cid: cid,
                group_name: '003 Мск Main',
                banners_quantity: 17,
                banners_arch_quantity: 0,
                adgroup_id: '1414050903',
                adgroup_type: 'performance',
                banners: [
                    {
                        modelId: bid,
                        bid: bid,
                        adgroup_id: '1414050903',
                        cid: '17935634',
                        creative: {
                            creative_id: 'uniq2151',
                            name: '1'
                        },
                        enable: true,
                        autobudget: 'Yes'
                    }
                ],
                feed_filters: [
                    {
                        filter_id: 10023,
                        real_filter_id: 10023,
                        from_tab: 'tree',
                        available: false,
                        adgroup_id: groupId,
                        adgroupModelId: groupId,
                        adgroup_type: 'performance',
                        filter_name: 'Смартфоны',
                        condition_name: '',
                        retargetings: [],
                        now_optimizing_by: 'CPC',
                        price_cpc: 0.25,
                        condition: [
                            {
                                field: 'categoryId',
                                relation: '==',
                                value: [
                                    '3537'
                                ]
                            }
                        ],
                        condition_tree: [
                            {
                                field: 'categoryId',
                                relation: '==',
                                value: [
                                    3537
                                ],
                                originValue: ''
                            }
                        ],
                        target_funnel: 'same_products',
                        has_default_price: true,
                        use_default_price: false,
                        ctx_shows: 5,
                        ctx_clicks: 0,
                        ctx_ctr: 0,
                        is_suspended: false,
                        is_deleted: true,
                        currency: 'YND_FIXED',
                        isIncorrect: false
                    },
                    {
                        filter_id: 10036,
                        real_filter_id: 10036,
                        from_tab: 'tree',
                        available: false,
                        adgroup_id: groupId,
                        adgroupModelId: groupId,
                        adgroup_type: 'performance',
                        filter_name: 'Пылесосы с пылесборником',
                        condition_name: '',
                        retargetings: [],
                        now_optimizing_by: 'CPC',
                        price_cpc: 0.25,
                        price_cpa: 3,
                        condition: [
                            {
                                field: 'categoryId',
                                relation: '==',
                                value: [
                                    '3045'
                                ]
                            }
                        ],
                        condition_tree: [
                            {
                                field: 'categoryId',
                                relation: '==',
                                value: [
                                    '3045'
                                ],
                                originValue: ''
                            },
                            {
                                field: 'price',
                                relation: '<->',
                                value: [],
                                originValue: ''
                            },
                            {
                                field: 'vendor',
                                relation: 'ilike',
                                value: [],
                                originValue: ''
                            }
                        ],
                        target_funnel: 'same_products',
                        has_default_price: true,
                        use_default_price: false,
                        ctx_shows: 0,
                        ctx_clicks: 0,
                        ctx_ctr: 0,
                        is_suspended: false,
                        is_deleted: false,
                        currency: 'YND_FIXED',
                        isIncorrect: false
                    }
                ]
            };

        },
        /**
         * Создаёт модель кампании
         * @param {Number} cid идентификатор кампании
         * @returns {BEM.MODEL<dm-dynamic-media-campaign>}
         */
        createCampaignModel = function(cid) {
            return BEM.MODEL.create({ name: 'dm-dynamic-media-campaign', id: cid }, getCampaignData(cid));
        },
        /**
         * Создаёт модель группы
         * @param {Number} cid идентификатор кампании
         * @param {Number} groupId идентификатор группы
         * @returns {BEM.MODEL<dm-dynamic-media-group>}
         */
        createGroupModel = function(cid, groupId) {
            return BEM.MODEL.create(
                { name: 'dm-dynamic-media-group', id: groupId },
                getGroupData(cid, groupId));
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create();

        campaignDM = createCampaignModel(+u._.uniqueId());
        groupDM = createGroupModel(campaignDM.get('cid'), +u._.uniqueId());
    });

    afterEach(function() {
        [groupDM, campaignDM].forEach(function(item) { item.destruct(); });

        sandbox.restore();
    });

    describe('Проверка на наличие элементов и состояния', function() {
        var block;

        beforeEach(function() {
            block = u.createBlock({
                block: 'b-feed-filter',
                filter: groupDM.get('feed_filters').getByIndex(0).toJSON()
            });
        });

        afterEach(function() {
            block.destruct();
        });

        it('не выставлен _editable_yes', function() {
            expect(block).not.haveMod('editable', 'yes');
        });

        ['name', 'target-audience', 'target-audience-value'].forEach(function(elem) {
            it('есть элемент ' + elem, function() {
                expect(block).to.haveElem(elem);
            });
        });
    });

    describe('Отрисовка опциональных элементов', function() {
        var block;

        afterEach(function() {
            block.destruct();
        });

        [
            { ctxElem: 'noTargetAudience', elemName: 'target-audience'},
            { ctxElem: 'noRetargeting', elemName: 'retargeting'}
        ].map(function(info) {
            [true, false].forEach(function(val) {
                it('Если ' + (val ? ' передано ' : 'не передано ') + info.ctxElem + ' в контексте блока  то ' + (val ? 'не ' : '') + 'должен отрисоваться блок ' + info.elemName, function() {
                    var blockData = {
                        block: 'b-feed-filter',
                        filter: groupDM.get('feed_filters').getByIndex(0).toJSON()
                    };

                    blockData[info.ctxElem] = val;
                    block = u.createBlock(blockData);

                    val ?
                        expect(block).to.not.haveElem(info.elemName) :
                        expect(block).to.haveElem(info.elemName);
                });
            })
        })
    });

    describe('Проверка на соответствие данных и состояния в модели и представления', function() {
        var block,
            filter,
            preparations = [
                function(filter) { filter.set('is_suspended', true); },
                function(filter) { filter.set('is_suspended', false); },
                function(filter) { filter.set('filter_name', 'filter <script>var alert(window.title); </script> name'); },
                function(filter) { filter.set('target_funnel', 'new_auditory'); },
                function(filter) { filter.set('target_funnel', 'product_page_visit'); },
                function(filter) { filter.set('target_funnel', 'same_products'); }
            ];

        beforeEach(function() {
            filter = groupDM.get('feed_filters').getByIndex(0);

            preparations.shift().call(this, filter);

            block = u.createBlock({
                block: 'b-feed-filter',
                filter: filter.toJSON()
            });
        });

        afterEach(function() {
            block.destruct();
        });

        [0, 1].forEach(function(i) {
            it(['', 'не '][i] + 'выставлен _is-suspended_yes', function() {
                (i ? expect(block).not : expect(block))
                    .haveMod('is-suspended', 'yes');
            });
        });

        it('__name содержит экранированное значение поля filter_name', function() {
            expect(block.elem('name').html()).be.equal(u.escapeHtmlSafe(filter.get('filter_name')));
        });

        [0, 1].forEach(function() {
            it('__target-audience-value содержит название уровня воронки фильтра согласно значению поля target_funnel', function() {
                expect(block.elem('target-audience-value').html())
                    .be.equal(u.feedFilterData.targetAudienceTitles[filter.get('target_funnel')]);
            });
        });

    });

});
