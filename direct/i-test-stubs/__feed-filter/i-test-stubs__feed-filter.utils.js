(function() {
    var filterId = u._.uniqueId(),
        adgroupId = u._.uniqueId(),
        dynamicStub = {
            filter_id: filterId,
            real_filter_id: filterId,
            from_tab: 'condition',
            available: false,
            adgroup_id: adgroupId,
            adgroupModelId: adgroupId,
            adgroup_type: 'dynamic',
            filter_name: 'Смартфоны',
            condition_name: '',
            retargetings: [],
            price: 0.25,
            condition: [
                {
                    field: 'categoryId',
                    relation: '==',
                    value: [
                        '3537'
                    ]
                }
            ],
            condition_tree: [],
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
        performanceStub = {
            filter_id: filterId,
            real_filter_id: filterId,
            from_tab: 'condition',
            available: false,
            adgroup_id: adgroupId,
            adgroupModelId: adgroupId,
            adgroup_type: 'performance',
            filter_name: 'Смартфоны',
            condition_name: '',
            retargetings: [],
            now_optimizing_by: 'CPC',
            price_cpc: 0.25,
            price_cpa: 0.43,
            condition: [],
            condition_tree: [],
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
        performanceTreeStub = {
            filter_id: filterId,
            real_filter_id: filterId,
            from_tab: 'tree',
            available: false,
            adgroup_id: adgroupId,
            adgroupModelId: adgroupId,
            adgroup_type: 'performance',
            filter_name: 'Смартфоны',
            condition_name: '',
            retargetings: [],
            now_optimizing_by: 'CPC',
            price_cpc: 0.25,
            price_cpa: 0.43,
            condition: [],
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
        };
    u.register({
        'i-test-stubs__feed-filter': function() {
            return {
                dynamic: u._.extend({}, dynamicStub),
                performance:  u._.extend({}, performanceStub),
                'performance-tree':  u._.extend({}, performanceTreeStub)
            }
        }
    });
})();
