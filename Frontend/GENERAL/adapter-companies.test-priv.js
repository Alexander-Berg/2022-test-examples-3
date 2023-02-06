describeBlock('adapter-companies__prepare-state-org-one', function(block) {
    let context;
    let snippet;

    beforeEach(function() {
        context = {
            expFlags: {
            }
        };
        snippet = {
            serp_info: {
                subtype: 1
            },
            data: {
                GeoMetaSearchData: {
                    features: [
                        {
                            properties: {
                                CompanyMetaData: {
                                }
                            }
                        }
                    ]
                }
            }
        };

        sinon.stub(blocks, 'adapter-companies__prepare-state').returns({ subtype: snippet.serp_info.subtype });
    });

    afterEach(function() {
        blocks['adapter-companies__prepare-state'].restore();
    });

    it('should return organization state', function() {
        assert.equal(block(context, snippet).subtype, 1);
        assert.calledOnce(blocks['adapter-companies__prepare-state']);
    });

    it('should return nothing if there are no organization', function() {
        snippet.data.GeoMetaSearchData.features = [];

        assert.isUndefined(block(context, snippet));

        assert.notCalled(blocks['adapter-companies__prepare-state']);
    });
});

describeBlock('adapter-companies__condition', function(block) {
    it('should return "unknown" if there`re properties closed and unreliable in meta and unreliable falsy', function() {
        const meta = { unreliable: undefined, closed: 'permanent' };
        const businessProperties = {};

        assert.equal(block(meta, businessProperties), 'unknown');
    });

    it('should return "open" if meta property is undefined', function() {
        const meta = undefined;
        const businessProperties = {};

        assert.equal(block(meta, businessProperties), 'open');
    });

    it('should return "closed:permanent"', function() {
        const meta = { closed: 'permanent' };
        const businessProperties = {};

        assert.equal(block(meta, businessProperties), 'closed:permanent');
    });

    it('should return "closed:temporary"', function() {
        const meta = { closed: 'temporary' };
        const businessProperties = {};

        assert.equal(block(meta, businessProperties), 'closed:temporary');
    });

    it('should return "open" if there is property unreliable in meta and its falsy', function() {
        const meta = { unreliable: undefined };
        const businessProperties = {};

        assert.equal(block(meta, businessProperties), 'open');
    });

    it('should return "open" if there isn`t property unreliable in meta', function() {
        const meta = {};
        const businessProperties = {};

        assert.equal(block(meta, businessProperties), 'open');
    });

    it('should return "remotely" if closed for visitors', function() {
        const meta = {};
        const businessProperties = { closed_for_visitors: '1' };

        assert.equal(block(meta, businessProperties), 'remotely');
    });

    it('should return "quarantine" if closed for quarantine', function() {
        const meta = {};
        const businessProperties = { closed_for_quarantine: '1' };

        assert.equal(block(meta, businessProperties), 'quarantine');
    });

    it('should return "remotely" if closed for visitors and closed for quarantine', function() {
        const meta = {};
        const businessProperties = { closed_for_visitors: '1', closed_for_quarantine: '1' };

        assert.equal(block(meta, businessProperties), 'remotely');
    });
});

describeBlock('adapter-companies__get-stars-count', function(block) {
    it('should return stars count', function() {
        assert.equal(block({ star: { id: 'star', values: ['4 звезды'] } }), '4');
        assert.equal(block({ star: { id: 'star', values: ['45 звезд'] } }), '4');
        assert.equal(block({ star: { id: 'star', values: ['Stars count: 4'] } }), '4');
    });

    it('should return unefined', function() {
        assert.isUndefined(block({}));
        assert.isUndefined(block({ star: {} }));
        assert.isUndefined(block({ star: { id: 'star', values: ['Звезд нет'] } }));
        assert.isUndefined(block({ unknown: {} }));
    });
});

describeBlock('adapter-companies__condition', function(block) {
    it('should return "moved"', function() {
        const meta = { closed: 'permanent' };
        const businessProperties = { moved: '1' };

        assert.equal(block(meta, businessProperties), 'moved');
    });

    it('should return "moved"', function() {
        const meta = { closed: 'permanent' };
        const businessProperties = { moved_to: '1193587013' };

        assert.equal(block(meta, businessProperties), 'moved');
    });
});

describeBlock('adapter-companies__prepare-addresses', function(block) {
    let orgProperties;
    let business;
    let toponym;

    beforeEach(function() {
        orgProperties = {
            description: 'description'
        };

        toponym = {};

        sinon.stub(blocks, 'adapter-companies__address-line').returns('formatted address');
    });

    afterEach(function() {
        blocks['adapter-companies__address-line'].restore();
    });

    it('should return {address: \'description\', ...} if business=empty', function() {
        business = {};

        assert.deepEqual(block(orgProperties, business, toponym), {
            address: 'description',
            addressShort: 'formatted address',
            addressFormatted: 'formatted address'
        });
    });

    it('should return {address: \'address\', ...} if business.address=\'address\'', function() {
        business = {
            address: 'address'
        };

        assert.deepEqual(block(orgProperties, business, toponym), {
            address: 'address',
            addressShort: 'formatted address',
            addressFormatted: 'formatted address'
        });
    });

    it('should return {addressShort: \'base address (additional address)\', ' +
        'addressFormatted: \'base address (additional address)\', ...} ' +
        'if business.Address.additional=\'additional address\'', function() {
        business = {
            address: 'address',
            Address: {
                additional: 'additional address'
            }
        };

        assert.deepEqual(block(orgProperties, business, toponym), {
            address: 'address (additional address)',
            addressShort: 'formatted address (additional address)',
            addressFormatted: 'formatted address (additional address)'
        });
    });
});

describeBlock('adapter-companies__distance', function(block) {
    let context;
    let targetLocation;

    beforeEach(function() {
        context = {
            userLocation: {
                lon: 30.328158,
                lat: 59.962124
            }
        };
    });

    it('should return undefined if the distance is greater then 20 kilometers', function() {
        targetLocation = [30.429792, 59.723189];

        assert.isUndefined(block(context, targetLocation));
    });

    it('should return undefined if the distance is zero', function() {
        targetLocation = [30.328158, 59.962124];

        assert.isUndefined(block(context, targetLocation));
    });

    it('should return string if the distance is less than 20 kilometers', function() {
        targetLocation = [30.375516, 59.829681];

        assert.equal(block(context, targetLocation), '15 км');
    });

    it('should return string if the distance is less than a kilometer', function() {
        targetLocation = [30.329219, 59.962976];

        assert.equal(block(context, targetLocation), '100 м');
    });
});

describeBlock('adapter-companies__prepare-hotel-and-travel', function(block) {
    let context;
    let snippet;
    let org;
    let rawFeatures;

    beforeEach(function() {
        context = {};
        snippet = {};
        org = {};
        rawFeatures = {};
    });

    it('should return emptyArray if no full offers exist', function() {
        _.set(snippet, 'data.GeoMetaSearchData.features.0.properties.YandexTravel.Prices', [
            {
                OperatorOfferCount: 1,
                Price: 4108
            }
        ]);

        const result = block(context, snippet, org, rawFeatures);
        assert.isArray(result.hotelOffers);
        assert.isEmpty(result.hotelOffers);
    });

    // Тест на применение метки промокода на первый заказ https://st.yandex-team.ru/SERP-131393
    it('should return link with utm_term=welcomehotelpromo if promo available', function() {
        _.set(org, 'properties.YandexTravel',
            {
                Prices: [
                    {
                        PartnerLink: 'https://travel.yandex.ru/redir?Token=YRwiXJed',
                        Price: 3200,
                        RoomType: 'Стандарт с двуспальной кроватью • Business access • WiFi в номере • Приветственный напиток • Регистрация в программе лояльности',
                        OperatorId: '1'
                    }
                ],
                WelcomePromocodeAvailable: true
            }
        );

        _.set(snippet, 'data.GeoMetaSearchData.properties.ResponseMetaData.SearchResponse.InternalResponseInfo.YandexTravel',
            {
                ForceUtmTerm: 'welcomehotelpromo',
                Operators: {
                    '1': {
                        IsBookOnYandex: true,
                        GreenUrl: 'travel.yandex.ru'
                    }
                }
            }
        );

        const result = block(context, snippet, org, rawFeatures);

        assert.strictEqual(result.hotelOffers[0].PartnerLink, 'https://travel.yandex.ru/redir?Token=YRwiXJed&utm_source=unisearch_ru_hotels&utm_campaign=common&utm_medium=one-company&utm_content=offer&utm_term=welcomehotelpromo');
    });

    it('should not return link with utm_term=welcomehotelpromo if no promo available', function() {
        _.set(org, 'properties.YandexTravel',
            {
                Prices: [
                    {
                        PartnerLink: 'https://travel.yandex.ru/redir?Token=YRwiXJed',
                        Price: 3200,
                        RoomType: 'Стандарт с двуспальной кроватью • Business access • WiFi в номере • Приветственный напиток • Регистрация в программе лояльности',
                        OperatorId: '1'
                    }
                ],
                WelcomePromocodeAvailable: false
            }
        );

        _.set(snippet, 'data.GeoMetaSearchData.properties.ResponseMetaData.SearchResponse.InternalResponseInfo.YandexTravel',
            {
                ForceUtmTerm: 'welcomehotelpromo',
                Operators: {
                    '1': {
                        IsBookOnYandex: true,
                        GreenUrl: 'travel.yandex.ru'
                    }
                }
            }
        );

        const result = block(context, snippet, org, rawFeatures);

        assert.strictEqual(result.hotelOffers[0].PartnerLink, 'https://travel.yandex.ru/redir?Token=YRwiXJed&utm_source=unisearch_ru_hotels&utm_campaign=common&utm_medium=one-company&utm_content=offer');
    });
});

describeBlock('adapter-companies__is-advert', function(block) {
    it('should return false if `Advert` does not exist', function() {
        assert.isFalse(block(context, {}));
    });

    it('should return true if `Advert` exists, but `highlighted` is not "false"', function() {
        assert.isTrue(block(context, { Advert: {} }));
        assert.isTrue(block(context, { Advert: { properties: {} } }));
        assert.isTrue(block(context, { Advert: { properties: { highlighted: false } } }));
        assert.isTrue(block(context, { Advert: { properties: { highlighted: 42 } } }));
    });

    it('should return false if `Advert` exists and `highlighted` is "false"', function() {
        assert.isFalse(block(context, { Advert: { properties: { highlighted: 'false' } } }));
    });
});
