describeBlock('adapter-companies__bcard', function(block) {
    const context = {};
    const params = {};

    describe('adapter-companies__bcard-thumb-travel-mark', () => {
        const badgeText = 'Реклама';
        const mirPromoText = {
            block: 'i18n',
            key: 'Возврат 20% по карте «Мир»',
            keyset: 'adapter-companies'
        };

        it('should return adv flag if one of badges has id === "ads"', function() {
            const itemWithAdvBadge = {
                travelBadges: [
                    {
                        Id: 'ads',
                        Text: badgeText
                    },
                    {
                        Id: 'another'
                    }
                ]
            };

            assert.deepEqual(blocks['adapter-companies__bcard-thumb-travel-mark'](context, itemWithAdvBadge, params).text, badgeText);
        });

        it('should return adv flag if mirPromoAvailable is true', function() {
            const itemWithAdvBadge = {
                travelBadges: [
                    {
                        Id: 'ads',
                        Text: badgeText
                    },
                    {
                        Id: 'another'
                    }
                ],
                mirPromoAvailable: true
            };

            assert.deepEqual(blocks['adapter-companies__bcard-thumb-travel-mark'](context, itemWithAdvBadge, params).text, badgeText);
        });

        it('should return mirPromoAvailable flag if mirPromoAvailable && badges hasn\'t id === "ads"', function() {
            const itemWithoutAdvBadge = {
                travelBadges: [
                    {
                        Id: 'another'
                    }
                ],
                mirPromoAvailable: true
            };

            assert.deepEqual(blocks['adapter-companies__bcard-thumb-travel-mark'](context, itemWithoutAdvBadge, params).text, mirPromoText);
        });

        it('should works if travelBadges is undefined', () => {
            const itemWithoutTravelBadges = {};
            assert.isUndefined(blocks['adapter-companies__bcard-thumb-travel-mark'](context, itemWithoutTravelBadges, params));
        });
    });
});
