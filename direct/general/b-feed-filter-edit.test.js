describe('b-feed-filter-edit', function() {
    var block,
        cid = 11,
        adgroupId = 222,
        filterId = 3333,
        groupModel,
        sandbox;

    function getFeedStub() {
        return u['i-test-stubs__feed-data']();
    }

    function getFeedFilterStub(data) {
        var adgroupType = data.adgroupType || 'performance',
            stub = u['i-test-stubs__feed-filter']()[adgroupType];

        return u._.extend(stub, data, {
            filter_id: filterId,
            adgroup_id: adgroupId,
            adgroupModelId: adgroupId
        });
    }

    function getGroupStub(data, filters) {
        var adgroupType = data.adgroupType || 'performance';

        return u._.extend({
            modelId: adgroupId,
            cid: cid,
            group_name: '003 Мск Main',
            banners_quantity: 17,
            banners_arch_quantity: 0,
            adgroup_id: adgroupId,
            adgroup_type: adgroupType,
            banners: [],
            feed_filters: filters
        }, data);
    }

    function createBlock(filterData, ctxData, ctxMods) {
        ctxData = ctxData || {};

        block = u.createBlock({
            block: 'b-feed-filter-edit',
            mods: ctxMods || {},
            noRetargeting: ctxData.noRetargeting,
            noTargetAudience: ctxData.noTargetAudience,
            filter: u._.extend(filterData, { feed: getFeedStub() }),
            businessType: 'retail',
            feedType: 'YandexMarket'
        }, { inject: true });

        sandbox.stub(block, '_getFeedData').callsFake(function() {
            return getFeedStub()['items'][0];
        });
    }

    before(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });

        BEM.blocks['i-filter-edit'].loadConfig('retail_YandexMarket');
        sandbox.server.respond(JSON.stringify(u['i-test-stubs__feed-filters-config']()));
    });

    afterEach(function() {
        groupModel && groupModel.destruct();
        sandbox.restore();
    });

    describe('Блок __filter-prices', function() {
        [
            {
                adgroupType: 'dynamic',
                strategiesArray: [
                    { strategy: { is_autobudget: false }, elems: [{ elem: 'setting', elemModName: 'field', elemModVal: 'price' }] },
                    { strategy: { is_autobudget: true }, elems: [] },
                    { strategy: { is_autobudget: true, name: 'different_places' }, elems: [] },
                    {
                        strategy: { name: 'different_places', is_search_stop: false },
                        elems: [
                            { elem: 'setting', elemModName: 'field', elemModVal: 'price_context' },
                            { elem: 'setting', elemModName: 'field', elemModVal: 'price' }
                        ]
                    },
                    {
                        strategy: { name: 'different_places', is_search_stop: true },
                        elems: [
                            { elem: 'setting', elemModName: 'field', elemModVal: 'price_context' }
                        ]
                    }
                ]
            },
            {
                adgroupType: 'performance',
                strategiesArray: [
                    {
                        strategy: { name: 'autobudget_avg_cpa_per_filter' },
                        elems: [
                            { elem: 'setting', elemModName: 'field', elemModVal: 'price-cpa' }
                        ]
                    },
                    {
                        strategy: { name: 'autobudget_avg_cpa_per_camp' },
                        elems: [
                            { elem: 'setting', elemModName: 'field', elemModVal: 'price-cpa' }
                        ]
                    },
                    {
                        strategy: { name: 'autobudget_avg_cpc_per_filter' },
                        elems: [{ elem: 'setting', elemModName: 'field', elemModVal: 'price-cpc' }]
                    },
                    {
                        strategy: { name: 'autobudget_avg_cpc_per_camp' },
                        elems: [{ elem: 'setting', elemModName: 'field', elemModVal: 'price-cpc' }]
                    },
                    {
                        strategy: { name: 'autobudget_roi' },
                        elems: [{ elem: 'setting', elemModName: 'field', elemModVal: 'price-cpc' }]
                    }
                ]
            }
        ].forEach(function(opt) {
            describe('Блок b-feed-filter-edit для adgroup_type = ' + opt.adgroupType, function() {
                var adgroupType = opt.adgroupType;

                opt.strategiesArray.forEach(function(info) {
                    describe('Отрисовка блока __filter-prices со стратегией с данными ' + JSON.stringify(info.strategy), function() {
                        var filterData = getFeedFilterStub({
                            adgroup_type: adgroupType,
                            strategy: info.strategy
                        }),
                        groupData = getGroupStub({
                            adgroup_type: adgroupType
                        }, [filterData]);

                        beforeEach(function() {
                            groupModel = BEM.MODEL.create({ name: u.campaign.getGroupModelName(adgroupType), id: adgroupId }, groupData);

                            createBlock(filterData);
                        });

                        afterEach(function() {
                            groupModel.destruct();
                            block.destruct();
                        });

                        it('Должен отрисоваться элемент __filter-prices_media-type_' + adgroupType, function() {
                            expect(block).to.haveElem('filter-prices', 'media-type', adgroupType);
                        });

                        it('В блоке должны содержаться элементы: ' + JSON.stringify(info.elems), function() {
                            info.elems.forEach(function(elem) {
                                expect(block).to.haveElem(elem.elem, elem.elemModName, elem.elemModVal);
                            });
                        })
                    });
                });
            });
        });
    });

    describe('Проверка на наличие блоков', function() {
        var adgroupType = 'dynamic',
            filterData = getFeedFilterStub({
                adgroup_type: adgroupType,
                strategy: { is_autobudget: false }
            }),
        groupData = getGroupStub({
            adgroup_type: adgroupType
        }, [filterData]);

        beforeEach(function() {
            groupModel = BEM.MODEL.create({ name: u.campaign.getGroupModelName(adgroupType), id: adgroupId }, groupData);
        });

        afterEach(function() {
            groupModel.destruct();
            block.destruct();
        });

        [true, false].forEach(function(flag) {
            [
                {
                    ctxData: 'noTargetAudience',
                    elem: { name: 'setting', elemModName: 'field', elemModVal: 'filter-target-audience' }
                },
                {
                    ctxData: 'noRetargeting',
                    elem: { name: 'filter-retargeting-area' }
                }
            ].forEach(function(info) {
                it('Если в контекст блока передано ' + info.ctxData + ' = ' + flag + ', то ' + (flag ? 'не ' : '') + 'должен содержаться элемент ' + JSON.stringify(info.elem), function() {
                    var ctxData = {};

                    ctxData[info.ctxData] = flag;
                    createBlock(filterData, ctxData);

                    flag ?
                        expect(block).not.to.haveElem(info.elem.name, info.elem.elemModName, info.elem.elemModVal) :
                        expect(block).to.haveElem(info.elem.name, info.elem.elemModName, info.elem.elemModVal)
                });

            });
        });
    });

    /**
     * TODO: необходим рефакторинг @belyanskii
     * Задача DIRECT-63309
     */
    describe.skip('Реакция на события в модели', function() {
        var adgroupType = 'dynamic',
            filterData = getFeedFilterStub({
                adgroup_type: adgroupType,
                strategy: { is_autobudget: false }
            }),
            groupData = getGroupStub({
                adgroup_type: adgroupType
            }, [filterData]);


        before(function() {
            groupModel = BEM.MODEL.create({ name: u.campaign.getGroupModelName(adgroupType), id: adgroupId }, groupData);

            createBlock(filterData);

            block.initialize({});
            //чтобы успел проинициализироваться блок
            sandbox.clock.tick(100);
        });

        after(function() {
            groupModel.destruct();
            block.destruct();
        });

        [
            'condition',
            'conditionTree',
            'filter_name',
            'price_cpc',
            'price_cpa',
            'price',
            'price_context',
            'available',
            'target_funnel',
            'is_suspended',
            'retargetings'
        ].map(function(currentField) {
            it('При изменении поля view-модели ' + currentField + ' изменяется поле модели hasChanges', function() {

                sandbox.stub(block.model, 'isChanged').callsFake(function(field) {
                    return currentField == field;
                });

                block.model.trigger(currentField, 'change', { value: '' });

                expect(block.model.get('hasChanges')).to.be.true;

                block.model.isChanged.restore();
            });
        });
    });

    /**
     * TODO: Починить в DIRECT-66061
     */
    describe.skip('DIRECT-58968', function() {
        var adgroupType = 'performance',
            feedModel;

        before(function() {
            feedModel = BEM.MODEL.create({ name: 'dm-dynamic-media-feed', id: u._.get(getFeedStub(), 'items[0].feed_id') });
        });

        afterEach(function() {
            feedModel.destruct();
            groupModel.destruct();
            block.destruct();
        });

        it('Если в данных фильтра  пришло target_funnel = same_products блок "Условия подбора" должен иметь модификатор visible = yes', function() {
            var filterData = getFeedFilterStub({
                    target_funnel: 'same_products',
                    strategy: { name: 'autobudget_avg_cpa_per_camp' }
                }),
                groupData = getGroupStub({
                    adgroup_type: adgroupType
                }, [filterData]);

            groupModel = BEM.MODEL.create({ name: u.campaign.getGroupModelName(adgroupType), id: filterData.adgroup_id }, groupData);
            createBlock(filterData);

            block.initialize({});
            //чтобы успел проинициализироваться блок
            sandbox.clock.tick(100);

            expect(block).to.haveMod(block.findElem('row', 'setting', 'retargeting'), 'visible', 'yes');
        });

        it('Если в данных фильтра  пришло target_funnel != same_products блок "Условия подбора" не должен иметь модификатор visible = yes', function() {
            var filterData = getFeedFilterStub({
                    target_funnel: 'new_auditory',
                    strategy: { name: 'autobudget_avg_cpa_per_camp' }
                }),
                groupData = getGroupStub({
                    adgroup_type: adgroupType
                }, [filterData]);

            groupModel = BEM.MODEL.create({ name: u.campaign.getGroupModelName(adgroupType), id: filterData.adgroup_id }, groupData);
            createBlock(filterData);

            block.initialize({});
            //чтобы успел проинициализироваться блок
            sandbox.clock.tick(100);

            expect(block).not.to.haveMod(block.findElem('row', 'setting', 'retargeting'), 'visible', 'yes');
        })

    })

});
