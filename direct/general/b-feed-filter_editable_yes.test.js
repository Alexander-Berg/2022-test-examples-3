describe('b-feed-filter_editable_yes', function() {

    var sandbox,

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

        getGroupData = function(cid, groupId, adgroupType) {
            var bid = +u._.uniqueId();

            adgroupType = adgroupType || 'performance';

            return {
                modelId: groupId,
                cid: cid,
                group_name: '003 Мск Main',
                banners_quantity: 17,
                banners_arch_quantity: 0,
                adgroup_id: '1414050903',
                adgroup_type: adgroupType,
                banners: [
                    {
                        modelId: bid,
                        bid: bid,
                        adgroup_id: groupId,
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
                        adgroup_type: adgroupType,
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
                        is_settings_editable: true,
                        is_price_editable: true,
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
                        adgroup_type: adgroupType,
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
                        is_settings_editable: true,
                        is_price_editable: true,
                        is_suspended: false,
                        is_deleted: false,
                        currency: 'YND_FIXED',
                        isIncorrect: false
                    }
                ]
            };
        },
        createCampaignModel = function() {
            var cid = +u._.uniqueId();

            return BEM.MODEL.create({ name: 'dm-dynamic-media-campaign', id: cid }, getCampaignData(cid));
        },
        createGroupModel = function(campaignDM) {
            var groupId = +u._.uniqueId();

            return BEM.MODEL.create(
                { name: 'dm-dynamic-media-group', id: groupId },
                getGroupData(campaignDM.get('cid'), groupId));
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });

    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Для редактируемых', function() {
        var block,
            campaignDM,
            groupDM,
            filter,
            spy,
            preparations = [
                function(filter) { },
                function(filter) { },
                function(filter) { },
                function(filter) { filter.set('filter_name', 'filter <script>alert(window.title); </script> name'); },
                function(filter) { filter.set('target_funnel', 'same_products'); }
            ];

        beforeEach(function() {
            spy = sandbox.spy(BEM.DOM.blocks['b-modal-popup-decorator'], 'create2');

            campaignDM = createCampaignModel();

            groupDM = createGroupModel(campaignDM);

            filter = groupDM.get('feed_filters').getByIndex(0);

            block = u.createBlock({
                block: 'b-feed-filter',
                filter: filter.toJSON()
            }, {
                inject: true
            });

            preparations.shift().call(this, filter);
        });

        afterEach(function() {
            sandbox.restore();
            [groupDM.getFeedDM(), groupDM, campaignDM, block].forEach(function(item) { item && item.destruct(); });
        });

        describe('при изменении поля модели', function() {
            it('filter_name - обновляется значение __name', function() {
                expect(block.elem('name').text()).be.equal(filter.get('filter_name'));
            });

            it('target_funnel - обновляется значение __target-audience-value', function() {
                expect(block.elem('target-audience-value').text())
                    .be.equal(u.feedFilterData.targetAudienceTitles[filter.get('target_funnel')]);
            });
        });

    });

});
