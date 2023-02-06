describe('b-feed-filters-group', function() {
    var models = [],
        groupBEMJSON,
        block;

    beforeEach(function() {
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
    });

    function getGroupBemJSON(adgroupType, strategy) {
        return {
            block: 'b-feed-filters-group',
            mods: {
                state: 'active',
                strategy: strategy,
                'editable-price': 'yes'
            },
            group: {
                feed_filters: [{
                    filter_id: 1,
                    adgroup_id: 1,
                    adgroup_type: adgroupType,
                    retargetings: []
                }],
                group_multiplier_stats: {},
                adgroup_id: 1,
                adgroup_type: adgroupType
            }
        }
    }

    function createBlock(JSON) {
        return u.createBlock(JSON, { inject: true });
    }

    function createModels(adgroupType) {
        models.push(BEM.MODEL.create({ name: u.campaign.getCampaignModelName(adgroupType), id: 1 }));
        models.push(BEM.MODEL.create({ name: u.campaign.getGroupModelName(adgroupType), id: 1 },
            { cid: 1, adgroup_id: 1, adgroup_type: adgroupType }));

        models.push(BEM.MODEL.create({
            name: 'dm-feed-filter',
            id: 1,
            parentName: u.campaign.getGroupModelName(adgroupType),
            parentId: 1
        }, {
            adgroup_id: 1,
            adgroup_type: adgroupType
        }));
    }

    afterEach(function() {
        models.forEach(function(item) {
            item.destruct();
        });

        block.destruct();
    });

    describe('Отображение информации о корректировках ставок', function() {
        groupBEMJSON = getGroupBemJSON('performance', 'avg-cpa');

        beforeEach(function() {
            createModels('performance');
        });

        describe('если корректировки не заданы ', function() {
            beforeEach(function() { block = createBlock(groupBEMJSON); });

            it('не должен отрисовывать елемент adjustment-rates', function() {
                expect(block).to.haveElems('adjustment-rates', 0);
            });
        });

        describe('если заданы корректировки ставок ', function() {
            var groupWithAdjustments = u._.merge({}, groupBEMJSON),
                block;

            groupWithAdjustments.group.group_multiplier_stats = { adjustments_lower_bound: 1 };

            beforeEach(function() { block = createBlock(groupWithAdjustments); });

            it('должен отрисовать елемент adjustment-rates', function() {
                expect(block).to.haveElem('adjustment-rates');
            });
        });
    });


    describe('Отрисовка с разными модификаторами _strategy_', function() {
        [
            {
                strategy: 'different-places',
                adgroupType: 'dynamic',
                captions: ['На поиске', 'В сетях'],
                hasCaption: true
            },
            {
                strategy: 'search',
                adgroupType: 'dynamic',
                hasCaption: false
            },
            {
                strategy: 'default',
                adgroupType: 'dynamic',
                hasCaption: false
            },
            {
                strategy: 'avg-cpa',
                adgroupType: 'performance',
                captions: ['CPA'],
                hasCaption: true
            }
        ].forEach(function(strategyData) {
            describe('_strategy_' + strategyData.strategy, function() {
                beforeEach(function() {
                    createModels(strategyData.adgroupType);
                    block = createBlock(getGroupBemJSON(strategyData.adgroupType, strategyData.strategy));

                });

                it('Блок ' + (strategyData.hasCaption ? 'содержит' : 'не содержит ') + '__row_type_caption', function() {
                    strategyData.hasCaption ?
                        expect(block).to.haveElem('row', 'type', 'caption') :
                        expect(block).not.to.haveElem('row', 'type', 'caption');
                });

                strategyData.hasCaption && it('Блок _strategy_' + strategyData.strategy + ' содержит __cell_type_column-caption с содержимым ' + strategyData.captions, function() {
                    var captionElems = block.findElem('cell', 'type', 'column-caption');

                    strategyData.captions.forEach(function(title, i) {
                        expect($(captionElems[i]).text()).to.be.equal(title);
                    });
                });
            })
        });
    });
});
