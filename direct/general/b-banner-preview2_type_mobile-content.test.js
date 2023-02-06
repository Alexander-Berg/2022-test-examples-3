describe('b-banner-preview2_type_mobile-content', function() {
    var objCampaign = {
        statusOpenStat: 'No',
        status_click_track: 'No',
        cid: '46068371',
        no_title_substitute: undefined,
        groups: [
            {
                modelId: '580206659',
                adgroup_id: '580206659',
                store_content_href: '"https: //itunes.apple.com/ru/app/teamo.ru-teamo.ru-no1-sajt/id725508998?mt=8',
                adgroup_type: 'mobile_content',
                geo: '225',
                reflected_attrs: [
                    'rating',
                    'icon'
                ],
                geo_names: 'Россия',
                isBidable: true,
                mobile_content: {
                    icon_url: '//avatars.mds.yandex.net/get-itunes-icon/39164/4758a3c6f611a054a41b5326141ac661/icon',
                    icon_hash: '39164/4758a3c6f611a054a41b5326141ac661',
                    publisher_domain: 'teamo.ru',
                    create_time: '2015-08-26 16: 24: 20',
                    rating: 4.5,
                    prices: { download: { price: 100, price_currency: 'RUB' } },
                    rating_votes: 10000
                },
                banners: [
                    {
                        href: 'http://okna.ru',
                        title: 'Кислые лимоны',
                        body: 'Ледяной борщ! Креветка уже умерла',
                        bid: 1388445106,
                        is_template_banner: 0,
                        image: 'ZwhVOu6KRacOMlGvYMdHkg',
                        domain: 'okna.ru',
                        hash_flags: {},
                        disclaimer: '',
                        geo_exception: {},
                        primary_action: 'get'
                    }
                ],
                phrases: [
                    {
                        id: 4291642927,
                        key_words: 'детская кровать автомобиль',
                        param1: '',
                        param2: ''
                    }
                ]
            }
        ]
    },
    objGroup = objCampaign.groups[0],
    objMobileContent = objCampaign.groups[0].mobile_content,
    objBanner = objCampaign.groups[0].banners[0],
    objPhrase = objGroup.phrases[0],
    modelCampaign,
    modelGroup,
    modelPhrase,
    modelBanner,
    modelMobileContent,
    modelGeo,
    ctxDataFromServer,
    viewModel,
    ctxDataFromModels,
    sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: [+new Date()] });

        sandbox.stub(u, 'consts').callsFake(function(name) {
            if (name == 'rights') {
                return {};
            } else {
                return u.getCurrenciesForStub(name);
            }
        });

        modelCampaign = BEM.MODEL.create('dm-mobile-content-campaign', objCampaign);
        modelGroup = BEM.MODEL.create({
            name: 'dm-mobile-content-group',
            id: objGroup.modelId
        }, u['dm-mobile-content-group'].transformData({
            group: objGroup,
            campaign: objCampaign
        }));

        modelBanner = modelGroup.getBanners()[0];
        modelMobileContent = modelGroup.get('mobile_content');
        modelGeo = BEM.MODEL.create({ name: 'm-geo-regions', id: objGroup.modelId, parentModel: modelGroup }, {
            geo: objGroup.geo,
            geoText: objGroup.geo_names
        });
        modelPhrase = BEM.MODEL.create({
            name: 'm-phrase-bidable',
            id: objPhrase.id,
            parentModel: modelGroup
        }, objPhrase);

        ctxDataFromServer = u['b-banner-preview2_type_mobile-content'].fromServer({
            campaign: objCampaign,
            group: objGroup,
            mobileContent: objMobileContent,
            banner: objBanner,
            phrase: objPhrase
        });
        viewModel = BEM.MODEL.create('b-banner-preview2_type_mobile-content');

        viewModel.init({
            campaignId: modelCampaign.id,
            groupId: modelGroup.id,
            mobileContentId: modelMobileContent.id,
            bannerId: modelBanner.id,
            phraseId: modelPhrase.id
        });

        ctxDataFromModels = viewModel.toJSON();
    });

    afterEach(function() {
        sandbox.restore();
        modelMobileContent.destruct();
        modelCampaign.destruct();
        modelGroup.destruct();
        modelPhrase.destruct();
        modelBanner.destruct();
        modelGeo.destruct();
        viewModel.destruct();
    });

    ['getFlagsSettings', 'getEmptyStoreData'].forEach(function(funcName) {
        it('В API блока должна быть функция ' + funcName, function() {
            expect(u['b-banner-preview2_type_mobile-content']).to.have.ownProperty(funcName);
        });
    });

    [
        'url', 'icon', 'phrase', 'price', 'rating', 'ratingVotes', 'body',
        'bid', 'title', 'showRating', 'showPrice', 'showIcon', 'showRatingVotes',
        'isTemplateBanner', 'flags', 'flagsSettings', 'cid', 'isArchived',
        'geo', 'actionType'
    ].forEach(function(key) {
        describe(key, function() {
            it('Есть в данных сгенерированных на сервере', function() {
                expect(ctxDataFromServer).to.have.ownProperty(key);
            });

            it('Есть в данных из цепочки dm -> vm', function() {
                expect(ctxDataFromModels).to.have.ownProperty(key);
            });

            it('Серверное значение совпадает с клиентским', function() {
                if (Array.isArray(ctxDataFromServer[key])) {
                    expect(ctxDataFromServer[key]).to.deep.include.members(ctxDataFromModels[key]);
                } else {
                    expect(ctxDataFromServer[key]).to.eql(ctxDataFromModels[key]);
                }
            });
        });
    });

    describe('Обновления DM должны повлечь обновление VM', function() {
        it('Обновление селекта "кнопка действия" должно обновить поле actionType', function() {
            modelBanner.set('primary_action', 'play');
            sandbox.clock.tick(500);

            expect(viewModel.get('actionType')).to.equal('play');
        });

        it('Изменение поля video_resources DM -> обновляется поле videoExtension VM', function() {
                var videoExtensionStub = {
                    resource_type : 'media',
                    type : 'video',
                    name : 'ddggdg',
                    id : '101',
                    urls: [
                        {
                            delivery: 'progressive',
                            type: 'video/webm',
                            bitrate: 1086,
                            height: 720,
                            url: 'https://storage.mdst.yandex.net/get-video-videodirekt/4857/158f91c9ed6/a7d654bd593bd369/720p.webm?redirect=yes&sign=7ab8d9be25137a6be2d4aba522a7d0f43122c4a22792bba6f75c7dc6f5ae5f5b&ts=6b1c2b8b',
                            id: '720p.webm',
                            width: 1280
                        }
                    ]
                };

                modelBanner.set('video_resources', videoExtensionStub);
                sandbox.clock.tick(500);

                expect(viewModel.get('videoExtension')).to.eql(videoExtensionStub);
        });
    });
});
