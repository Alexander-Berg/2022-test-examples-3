describe('dm-mobile-content-group', function() {
    var dmMobContGroup, dmGroupBanner;

    beforeEach(function() {
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
    });

    describe('Проверка API', function() {
        before(function(){
            u.stubCurrencies();
            dmMobContGroup = BEM.MODEL.create('dm-mobile-content-group');
            u.restoreCurrencies();
        });
        after(function(){
            dmMobContGroup.destruct();
        });
        [
            'init', 'toJSONForErrors', 'getAllPhrases', 'getPhrasesLength', 'getPhrasesData', 'getPhrasesLeft',
            'getRetargetingsData', 'getMultipliersData', 'isPhrasesChanged', 'getCampaignModel',
            'getRetargetingsModels', 'getPhrasesModels', 'getPhrases', 'provideData', 'getGeoModel', 'getBanners',
            'getRelevanceMatchModel', 'getRelevanceMatchData',


            'getBannersWhere', 'getBanners', 'getBannerByBid', 'getBannerByModelId', 'bulkSetBannerStatusById',
            'archiveBannerById', 'archiveBannerByIdWithRedirect', 'unArchiveBannerById', 'unArchiveAllBanners', 'unArchiveAllBannersWithRedirect',
            'unArchiveBannerByIdWithRedirect', 'deleteBannerById', 'deleteBannerByIdWithRedirect', 'requestGroupDataAndUpdate',
            'addBannerToList', 'bannerDataToModelData', 'dataToModelData', 'isLastBanner',
            'isLastActive'

        ].forEach(function(name) {
            it('В API должна присутствовать функция ' + name, function() {
                expect(dmMobContGroup[name]).to.be.an.instanceof(Function);
            });
        });

    });

    describe('поле mobile_content', function() {
        var dmCamp, dmGroup, time,
            dataStub = {
                icon_url: 'icon1',
                rating: '4.59',
                available_actions: ['download'],
                min_os_version: '4.0',
                rating_votes: '2896',
                prices: { 'download': { 'price_currency': 'RUB', 'price': '95.22' } }
            };

        beforeEach(function() {
            dmCamp = BEM.MODEL.create({ name: 'dm-mobile-content-campaign', id: 1 }, {
                cid: 1
            });
            dmGroup = BEM.MODEL.create({
                name: 'dm-mobile-content-group',
                id: 1,
                parentName: 'dm-mobile-content-campaign',
                parentId: 1
            }, {
                store_content_href: 'default',
                adgroup_type: 'mobile_content'
            });

            dmGroup.init();

            time = sinon.useFakeTimers();
        });

        afterEach(function() {
            dmCamp.destruct();

            time.restore();
        });

        describe('при пустом store_content_href', function() {
            beforeEach(function() {
                dmGroup.get('mobile_content').update(dataStub);

                dmGroup.set('store_content_href', '');
                time.tick(100);
            });

            it('должен очищать rating', function() {
                expect(dmGroup.get('mobile_content').get('rating')).to.be.undefined;
            });
            it('должен очищать icon_url', function() {
                expect(dmGroup.get('mobile_content').get('icon_url')).to.be.empty;
            });
            it('должен очищать rating_votes', function() {
                expect(dmGroup.get('mobile_content').get('rating_votes')).to.be.undefined;
            });
            it('должен очищать min_os_version', function() {
                expect(dmGroup.get('mobile_content').get('min_os_version')).to.be.empty;
            });
            it('должен очищать prices', function() {
                expect(dmGroup.get('mobile_content').get('prices')).to.be.undefined;
            });
        });

        describe('updateMobileContentInfo - при получении информации по приложению', function() {
            var sandbox,
                stubRequest,
                data = { result: dataStub, success: true };

            beforeEach(function() {
                sandbox = sinon.sandbox.create({ useFakeTimers: true });
                stubRequest = sandbox
                    .stub(BEM.blocks['i-web-api-request'].mobileApps, 'getAppContent')
                    .resolves(Promise.resolve(data));
            });

            afterEach(function() {
                sandbox.restore();
            });

            it('должен выставлять rating', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('rating')).to.be.equal(4.59);

                    done();
                });
            });

            it('должен выставлять icon_url', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('icon_url')).to.be.equal('icon1');

                    done();
                });
            });

            it('должен выставлять rating_votes', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('rating_votes')).to.be.equal(2896);

                    done();
                });
            });

            it('должен выставлять min_os_version', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('min_os_version')).to.be.equal('4.0');

                    done();
                });
            });

            it('должен выставлять prices', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content')
                        .get('prices'))
                        .to.be.eql({ 'download': { 'price_currency': 'RUB', 'price': '95.22' } });

                    done();
                });
            });
        });

        describe('updateMobileContentInfo - при ошибке получения информации по приложению', function() {
            var sandbox,
                stubRequest,
                data = { result: dataStub, success: false };

            beforeEach(function() {
                sandbox = sinon.sandbox.create({ useFakeTimers: true });
                stubRequest = sandbox
                    .stub(BEM.blocks['i-web-api-request'].mobileApps, 'getAppContent')
                    .resolves(Promise.resolve(data));
            });

            afterEach(function() {
                sandbox.restore();
            });

            it('должен очищать rating', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('rating')).to.be.undefined;

                    done();
                });
            });

            it('должен очищать icon_url', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('icon_url')).to.be.empty;

                    done();
                });
            });

            it('должен очищать rating_votes', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('rating_votes')).to.be.undefined;

                    done();
                });
            });

            it('должен очищать min_os_version', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('min_os_version')).to.be.empty;

                    done();
                });
            });

            it('должен очищать prices', function(done) {
                dmGroup.updateMobileContentInfo('new1').then(function() {
                    expect(dmGroup.get('mobile_content').get('prices')).to.be.undefined;

                    done();
                });
            });
        });

        describe('при копировании баннера', function(){
            var bannerData = {
                modelId: 'new1-new1',
                bid: 0,
                BannerID: 'new2',
                isNewBanner: true,
                'banner_type': 'mobile',
                'is_bs_rarely_loaded': '',
                statusShow: 'Yes',
                groupStatusModerate: 'New',
                statusModerate: 'New',
                statusPostModerate: 'No',
                status: '',
                statusActive: 'No',
                archive: 'No',
                can_delete_banner: false,
                can_archive_banner:false,
                disabledAutocorrectionWarning:false,
                autobudget:'',
                statusAutobudgetShow:'',
                day_budget:'',
                day_budget_show_mode:'',
                wallet_day_budget:'',
                wallet_day_budget_show_mode:'',
                phoneflag:'',
                statusMetricaStop: '',
                'geo_id': '',
                title: 'Яндекс.Браузер',
                body: '',
                'reflected_attrs': ['rating','icon','rating_votes'],
                'primary_action': 'download',
                'url_protocol': 'https://',
                href: '',
                approvedTrackingSystemErrorType: '',
                enable: '',
                isAvailableMobileContent: false,
                ignoreIconStatusModerate: true,
                iconStatusModerate: 'Yes',
                'icon_hash': '',
                'templ_href': '',
                'templ_body': '',
                statusBsSynced: 'Yes',
                statusSitelinksModerate: 'New',
                newBannerIndex: 1,
                'image_model': {'image':'','image_name':'','image_type':'','source_image':'','image_source_url':'','image_processing_state':'','mds_group_id':''},
                'hash_flags': {'age':'18'},
                disclaimer: '',
                approvedTrackingSystem: true,
                'video_resources': {},
                'disable_videomotion': true,
                'ad_type':'image_ad',
                'image_ad':{'hash':'','name':'','group_id':''},
                'creative':{'creative_id':'2122282232','name':'Новая группа креативов','width':300,'height':250,'scale':0.8,'preview_url':'https://avatars.mds.yandex.net/get-media-adv-screenshooter/41244/595926e3-5165-436a-b5d5-89fa4a4275c9/orig','creative_type':'canvas','composed_from':'','live_preview_url':'https://qanvas-devtest.qams.yandex.ru/creatives/2122282232/preview?isCompactPreview=true'},
                'image':'',
                'image_name':'',
                'image_type':'',
                'source_image':'',
                'image_source_url':'',
                'image_processing_state':'',
                'mds_group_id':''
            }

            beforeEach(function(){
               dmGroupBanner = dmMobContGroup.addBannerToList(
                   BEM.MODEL.create('dm-mobile-content-banner', bannerData)
               );
            });

            afterEach(function(){
                dmMobContGroup.removeBanner(dmGroupBanner.id);
            });

            ['ad_type', 'image_ad', 'creative'].forEach(function(modelField){
                it('Метод _getPrevNewBannerData возвращает данные без поля ' + modelField, function(){
                    var prevBannerData = dmMobContGroup._getPrevNewBannerData();
                    expect(prevBannerData[modelField]).to.be.undefined;
                });
            });
        });

    });

    describe('Тарегтинг на интересы', function() {
        var dmGroup,
            dmCamp;

        describe('Валидация', function() {
            beforeEach(function() {
                dmCamp = BEM.MODEL.create({ name: 'dm-mobile-content-campaign', id: 1 }, {
                    cid: 1
                });
                dmGroup = BEM.MODEL.create({
                    name: 'dm-mobile-content-group',
                    id: 1,
                    parentName: 'dm-mobile-content-campaign',
                    parentId: 1
                }, {
                    target_interests: ['41', '43'],
                    phrasesIds: [],
                    retargetingsIds: [],
                    store_content_href: 'default',
                    adgroup_type: 'mobile_content'
                });

                dmGroup.init();

                sinon.stub(dmGroup, 'getKeywordLimit').returns(20);
            });

            it('При создании группы только с интересами, поле new_phrases валидно', function() {
                expect(dmGroup.validate('new_phrases').valid).to.equal(true);
            });

            describe('При удалении последнего интереса поле new_phrases невалидно', function() {
                beforeEach(function() {
                    dmGroup.set('target_interests', []);
                });

                it('Ошибка про неуказание ключевиков', function() {
                    expect(dmGroup.validate('new_phrases').errors[0].text).to.equal('Не указаны ключевые фразы');
                });

                it('Ошибка про отсутствие условий показа', function() {

                    expect(dmGroup.validate('new_phrases').errors[1].text).to.equal('У группы должна быть указана хотя бы ' +
                        'одна из следующих настроек: ключевые фразы, условия подбора аудитории, интересы ' +
                        'либо автотаргетинг');
                });
            });

            it('При удалении последнего интереса, но наличии фраз, поле new_phrases валидно', function() {
                var phrase = BEM.MODEL.create({ name: 'm-phrase-text', id: '1', parentModel: dmGroup });
                phrase.set('phrase', 'ыть');
                dmGroup.set('phrasesIds', ['1']);
                dmGroup.set('target_interests', []);

                expect(dmGroup.validate('new_phrases').valid).to.equal(true);
            });

            it('При удалении последнего интереса, но наличии условий ретаргетинга, поле new_phrases валидно', function() {
                dmGroup.set('retargetingsIds', ['1']);
                dmGroup.set('target_interests', []);

                expect(dmGroup.validate('new_phrases').valid).to.equal(true);
            })
        });

        describe('Совпадение данных интересов со спецификацией сервера', function() {
            describe('Если интересы есть', function() {
                beforeEach(function() {
                    dmGroup = BEM.MODEL.create({
                        name: 'dm-mobile-content-group',
                        id: 1,
                        parentName: 'dm-mobile-content-campaign',
                        parentId: 1
                    }, {
                        target_interests: [
                            { target_category_id: '585341' },
                            { target_category_id: '67' },
                            { target_category_id: '45' },
                            { target_category_id: '78' }
                        ],
                        phrasesIds: [],
                        retargetingsIds: [],
                        store_content_href: 'default',
                        adgroup_type: 'mobile_content'
                    });

                    dmGroup.init();
                });

                it('Метод provideData возвращает заданные инетерсы в массиве', function() {
                    var data = dmGroup.provideData();

                    expect(u._.isArray(data.target_interests)).to.equal(true);
                });

                it('Метод provideData возвращает все заданные инетерсы', function() {
                    var data = dmGroup.provideData();

                    expect(data.target_interests.length).to.equal(4);
                });


                it('Метод provideData возвращает массив интересов в формате target_category_id:idNum (если они есть), требуемый сервером', function() {
                    var data = dmGroup.provideData(),
                        result = data.target_interests.every(function(interest) {
                            return interest.target_category_id != 'undefined' && typeof interest.target_category_id == 'string';
                    });

                    expect(result).to.equal(true);
                });
            });

            describe('Если интересов нет', function() {
                beforeEach(function() {
                    dmGroup = BEM.MODEL.create({
                        name: 'dm-mobile-content-group',
                        id: 1,
                        parentName: 'dm-mobile-content-campaign',
                        parentId: 1
                    }, {
                        target_interests: [],
                        phrasesIds: [],
                        retargetingsIds: [],
                        store_content_href: 'default',
                        adgroup_type: 'mobile_content'
                    });

                    dmGroup.init();
                });

                it('Метод provideData возвращает заданные инетерсы в массиве', function() {
                    var data = dmGroup.provideData();

                    expect(u._.isArray(data.target_interests)).to.equal(true);
                });

                it('Метод provideData->target_interests возвращает пустой массив', function() {
                    var data = dmGroup.provideData();

                    expect(data.target_interests.length).to.equal(0);
                })
            })
        });

        describe('Автотаргетинг', function() {
            beforeEach(function() {
                dmGroup = BEM.MODEL.create({
                    name: 'dm-mobile-content-group',
                    id: 1,
                    parentName: 'dm-mobile-content-campaign',
                    parentId: 1
                }, {
                    target_interests: [],
                    phrasesIds: [],
                    retargetingsIds: [],
                    store_content_href: 'default',
                    adgroup_type: 'mobile_content'
                });

                dmGroup.init();
                BEM.MODEL.create({ name: 'm-relevance-match', id: dmGroup.get('modelId'), parentModel: dmGroup }, { bid_id: '5', price: '12' });
            });

            it('При наличии БТ и отключенных фразах isLastActive возвращает false', function() {
                expect(dmGroup.isLastActive(dmGroup.getRelevanceMatchModel())).to.be.false;
            });

            it('Метод getRelevanceMatchData возвращает данные bid_id, price, is_suspended', function() {
                var data = dmGroup.getRelevanceMatchData();

                expect(data).to.deep.equal([{
                    bid_id: '5',
                    is_suspended: false,
                    price: '12.00',
                    price_context: '',
                    search_stop: 0,
                    net_stop: 0
                }]);
            })
        });
    });
});
