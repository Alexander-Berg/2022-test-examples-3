describe('b-banner-preview2_type_image', function() {
    describe('Соответствие модели, в зависимости от входных данных', function() {
        var viewModel,
            modelCampaign,
            campaignId = 'campaign.id';

        before(function() {
            modelCampaign = BEM.MODEL.create('m-campaign', { cid: campaignId });

            viewModel = BEM.MODEL.create('b-banner-preview2_type_image');
        });

        after(function() {
            modelCampaign.destruct();
            viewModel.destruct();
        });

        it('Должен содержать модель баннера m-banner', function() {
            viewModel.init({
                campaignId: campaignId,
                groupId: 'modelGroup.id',
                bannerId: 'modelBanner.id',
                phraseId: 'modelPhrase.id'
            }, 'text');

            expect(viewModel.fields._dmDecl._value.banner.name).to.be.eq('m-banner');
        });

        it('Должен содержать модель баннера dm-mobile-content-banner', function() {
            viewModel.init({
                campaignId: campaignId,
                groupId: 'modelGroup.id',
                bannerId: 'modelBanner.id',
                phraseId: 'modelPhrase.id'
            }, 'mobile_content');

            expect(viewModel.fields._dmDecl._value.banner.name).to.be.eq('dm-mobile-content-banner');
        });

        it('Должен содержать модель группы m-group', function() {
            viewModel.init({
                campaignId: campaignId,
                groupId: 'modelGroup.id',
                bannerId: 'modelBanner.id',
                phraseId: 'modelPhrase.id'
            }, 'text');

            expect(viewModel.fields._dmDecl._value.group.name).to.be.eq('m-group');
        });

        it('Должен содержать модель группы dm-mobile-content-group', function() {
            viewModel.init({
                campaignId: campaignId,
                groupId: 'modelGroup.id',
                bannerId: 'modelBanner.id',
                phraseId: 'modelPhrase.id'
            }, 'mobile_content');

            expect(viewModel.fields._dmDecl._value.group.name).to.be.eq('dm-mobile-content-group');
        });

    });

    describe('Генерация входных данных', function() {
        var objCampaign = {
                statusOpenStat: 'No',
                status_click_track: true,
                cid: '20644478',
                no_title_substitute: 0,
                groups: [
                    {
                        modelId: '1788082175',
                        adgroup_id: '1788082175',
                        adgroup_type: 'text',
                        geo: '225',
                        geo_names: 'Россия',
                        banners: [
                            {
                                BannerID: '0',
                                ClientID: '1725824',
                                ad_type: 'text',
                                href: 'momondo.ru',
                                has_href: 1,
                                has_vcard: 0,
                                adgroup_type: 'base',
                                title: '#Путешествия с удовольствием#',
                                body: 'Ледяной борщ! Креветка уже умерла',
                                bid: 2609625487,
                                modelId: '2609625487',
                                pid: '1788082175',
                                url_protocol: 'http://',
                                sitelinks: [],
                                image_ad: {test:'123'},
                                creative: {},
                                image: 'MZDQVSFjzaTDgVWYKU7Zuw',
                                image_BannerID: '0',
                                image_PriorityID: '0',
                                image_id: '2775319228',
                                image_name: 'zR8TGpuztP6myzGVvcxQXWSLG8JDkVEswLeUyKVlbLC_A-cyd1dd5UFSIv1muQ_0yS8=h900',
                                image_statusModerate: 'Yes',
                                image_type: 'wide',
                                domain: 'momondo.ru',
                                hash_flags: {},
                                disclaimer: '',
                                isVCardEmpty: true
                            }
                        ],
                        phrases: [
                            {
                                id: 4291642927,
                                key_words: 'кровать',
                                param1: 'fff',
                                param2: 'ggg'
                            }
                        ]
                    }
                ]
            },
            objGroup = objCampaign.groups[0],
            objBanner = objGroup.banners[0],
            objPhrase = objGroup.phrases[0],
            modelCampaign,
            modelGroup,
            modelPhrase,
            modelBanner,
            ctxDataFromServer,
            viewModel,
            ctxDataFromModels;

        before(function() {
            modelCampaign = BEM.MODEL.create('m-campaign', objCampaign);
            modelGroup = BEM.MODEL.create('m-group', u['m-group'].transformData({
                group: objGroup
            }));
            modelBanner = modelGroup.getBanners()[0];
            modelPhrase = BEM.MODEL.create(
                {
                    name: 'm-phrase-bidable',
                    parentModel: modelGroup,
                    id: objPhrase.id
                },
                objPhrase);

            ctxDataFromServer = u['b-banner-preview2_type_image'].fromServer({
                campaign: objCampaign,
                group: objGroup,
                banner: objBanner,
                phrase: objPhrase
            });

            viewModel = BEM.MODEL.create('b-banner-preview2_type_image');

            viewModel.init({
                campaignId: modelCampaign.get('cid'),
                groupId: modelGroup.get('modelId'),
                bannerId: modelBanner.get('modelId'),
                phraseId: modelPhrase.get('id')
            }, 'text');

            ctxDataFromModels = viewModel.toJSON();
        });

        after(function() {
            modelCampaign.destruct();
            modelGroup.destruct();
            modelPhrase.destruct();
            modelBanner.destruct();
            viewModel.destruct();
        });

        ['isArchived', 'imageAd', 'creative', 'geo'].forEach(function(key) {
            describe(key, function() {
                it('Есть в данных сгенерированных на сервере', function() {
                    expect(ctxDataFromServer).to.have.ownProperty(key);
                });

                it('Есть в данных из цепочки dm -> vm', function() {
                    expect(ctxDataFromModels).to.have.ownProperty(key);
                });
            });
        });

    });

});
