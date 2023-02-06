describe('b-banner-preview2_type_dynamic', function() {
    var clock,
        constsStub,
        genYclidStub,
        yclid = 48313024715480424,
        objCampaign = {
        cid: '46068371',
        no_title_substitute: 0,
        groups: [
            {
                modelId: '580206659',
                adgroup_id: '580206659',
                adgroup_type: 'dynamic',
                geo: '225',
                geo_names: 'Россия',
                isBidable: true,
                main_domain: 'okna.ru',
                banners: [
                    {
                        title: 'Кислые лимоны',
                        body: 'Ледяной борщ! Креветка уже умерла',
                        bid: 1388445106,
                        is_template_banner: 0,
                        sitelinks: [
                            {
                                title: 'быстрая ссылка 1',
                                href: 'http://okna.ru/big'
                            }, {
                                title: 'быстрая ссылка 2',
                                href: 'http://okna.ru/small'
                            }
                        ],
                        image: 'ZwhVOu6KRacOMlGvYMdHkg',
                        domain: 'okna.ru',
                        hash_flags: {
                            age: 12,
                            baby_food: 5
                        },
                        market_rating: -1,
                        loadVCardFromClient: undefined,
                        geo_exception: {},
                        callouts: [
                            {
                                additions_item_id: "1",
                                callout_text: "Пункт самовывоза",
                                status_moderate: "Yes"
                            }, {
                                additions_item_id: "2",
                                callout_text: "Розница и опт",
                                status_moderate: "Yes"
                            }
                        ]
                    }
                ]
            }
        ]
    };

    before(
        function() {

            constsStub = sinon.stub(u, 'consts');
            constsStub.withArgs('rights').returns({ addRemoveFlags: true });

            constsStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
            constsStub.withArgs('SITELINKS_NUMBER').returns(4);
            constsStub.withArgs('MAX_BODY_LENGTH').returns(81);

            genYclidStub = sinon.stub(u, 'generateYclid').callsFake(function() { return yclid; });

            clock = sinon.useFakeTimers();
        });

    after(
        function() {
            constsStub.restore();
            genYclidStub.restore();
            clock.restore();
        });

    describe('Генерация входных данных', function() {
        var objGroup = objCampaign.groups[0],
            objBanner = objGroup.banners[0],
            modelCampaign,
            modelGroup,
            modelBanner,
            modelGeo,
            ctxDataFromServer,
            viewModel,
            ctxDataFromModels;

        before(
            function() {
                modelCampaign = BEM.MODEL.create('m-campaign', objCampaign);
                modelGroup = BEM.MODEL.create(
                    'dm-dynamic-group', u['dm-dynamic-group'].transformData({ group: objGroup }));
                modelBanner = modelGroup.getBanners()[0];
                modelGeo = BEM.MODEL.create(
                    {
                        name: 'm-geo-regions',
                        id: objGroup.modelId,
                        parentModel: modelGroup
                    },
                    {
                        geo: objGroup.geo,
                        geoText: objGroup.geo_names
                    });

                ctxDataFromServer = u['b-banner-preview2_type_dynamic'].fromServer(
                    {
                        campaign: objCampaign,
                        group: objGroup,
                        banner: objBanner
                    });

                viewModel = BEM.MODEL.create('b-banner-preview2_type_dynamic');

                viewModel.init(
                    {
                        campaignId: modelCampaign.id,
                        groupId: modelGroup.id,
                        bannerId: modelBanner.id
                    });

                ctxDataFromModels = viewModel.toJSON();
            });

        after(
            function() {
                modelCampaign.destruct();
                modelGroup.destruct();
                modelBanner.destruct();
                modelGeo.destruct();
                viewModel.destruct();
            });

        [
            'cid', 'bid', 'body', 'url', 'rating', 'domain', 'isHrefHasParams', 'vcard', 'image', 'sitelinks', 'phone',
            'worktime', 'flags', 'geo', 'city', 'metro', 'loadVCardFromClient', 'callouts'
        ].forEach(
            function(key) {
                describe(
                    key, function() {
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
        });
    describe('Проверка наличия публичных функций в API', function() {
        ['fromServer', 'getObjectHrefParams', 'getDynamicBannerUrl', 'getFlagsSettings'].forEach(
            function(funcName) {
                it('В API блока должна быть функция ' + funcName, function() {
                    expect(u['b-banner-preview2_type_dynamic']).to.have.ownProperty(funcName);
                });
            });
    });

    describe('Обновления DM должны повлечь обновление VM', function() {
        var modelCampaign,
            modelGroup,
            modelBanner,
            modelGeo,
            viewModel;

        beforeEach(
            function() {
                modelCampaign = BEM.MODEL.create('m-campaign', objCampaign);
                modelGroup = BEM.MODEL.create(
                    'dm-dynamic-group', u['dm-dynamic-group'].transformData({ group: objCampaign.groups[0] }));
                modelBanner = modelGroup.getBanners()[0];
                modelGeo = BEM.MODEL.create({
                    name: 'm-geo-regions',
                    id: modelGroup.id,
                    parentModel: modelGroup
                });

                viewModel = BEM.MODEL.create('b-banner-preview2_type_dynamic');

                viewModel.init({
                    campaignId: modelCampaign.id,
                    groupId: modelGroup.id,
                    bannerId: modelBanner.id
                });

            });

        afterEach(
            function() {
                modelCampaign.destruct();
                modelGroup.destruct();
                modelBanner.destruct();
                modelGeo.destruct();
                viewModel.destruct();
            });

        it('Обновления описания должны обновить body', function() {
            modelBanner.set('body', 'Описание');
            clock.tick(500);
            expect(viewModel.get('body')).to.equal('Описание');
        });

        it('Поле phone', function() {
            modelBanner.set('vcard', { phone: '796-62-15' });
            clock.tick(500);
            expect(viewModel.get('phone')).to.equal('796-62-15');
        });

        it('Поле worktime', function() {
            modelBanner.set('vcard', { worktime: '0#4#10#00#18#00' });
            clock.tick(500);
            expect(viewModel.get('worktime')).to.equal('0#4#10#00#18#00');
        });

        it('Поле metro', function() {
            //в модели b-banner-preview2_type_dynamic metro это строка
            //а в модели dm-vcard  metro это число
            modelBanner.set('vcard', { metro: '20490' });
            clock.tick(500);
            expect(viewModel.get('metro')).to.equal(20490);
        });

        it('Поле city', function() {
            modelBanner.set('vcard', { city: 'Щелково' });
            clock.tick(500);
            expect(viewModel.get('city')).to.equal('Щелково');
        });

        it('Поле isArchived', function() {
            modelBanner.set('archive', 'Yes');
            clock.tick(500);

            expect(viewModel.get('isArchived')).to.equal(true);
        });

        it('Поле flags', function() {
            modelBanner.set(
                'hash_flags', {
                    age: 12,
                    alcohol: true
                });
            clock.tick(500);

            expect(viewModel.get('flags')).to.eql({
                age: 12,
                alcohol: true
            });
        });

        it('Поле callouts при удалении уточнений', function() {
            modelBanner.set('callouts', []);
            clock.tick(500);
            expect(viewModel.get('callouts').length).to.equal(0);
        });

        describe('Поле callouts при добавлении уточнений', function() {
            var calloutsArr = [
                {
                    additions_item_id: '1',
                    callout_text: 'Пункт самовывоза',
                    status_moderate: 'Yes'
                },
                {
                    additions_item_id: '2',
                    callout_text: 'Розница и опт',
                    status_moderate: 'Yes'
                }
            ];

            before(function() {
                modelBanner.set('callouts', calloutsArr);

                clock.tick(500);
            });

            calloutsArr.forEach(function(item, i) {
                it('callouts №' + i, function() {
                    expect(item.callout_text).to.equal(viewModel.get('callouts')[i].callout_text);
                });
            });

            it('callouts проверка количества уточнений', function() {
                expect(viewModel.get('callouts').length).to.equal(calloutsArr.length);
            });
        });


        describe('При отсутствии заголовка или описания - должны быть стандартные тексты', function() {
            it('title', function() {
                expect(viewModel.get('title')).to.equal('{Динамический заголовок}');
            });

            it('body', function() {
                modelBanner.set('body', '');
                clock.tick(500);
                expect(viewModel.get('body')).to.equal(
                    'Текст вашего объявления о рекламе услуги или товара.');
            });
        });

        describe('Проверить публичные методы', function() {
            afterEach(
                function() {
                    modelCampaign.destruct();
                    modelGroup.destruct();
                    modelBanner.destruct();
                    modelGeo.destruct();
                    viewModel.destruct();
                });
            it('getObjectHrefParams при наличии параметров', function() {
                var obj = u['b-banner-preview2_type_dynamic'].getObjectHrefParams('?a=b&&b=c');

                expect(obj.a).to.equal('b');
                expect(obj.b).to.equal('c');
            });
            it('getObjectHrefParams при пустой строке', function() {
                var obj = u['b-banner-preview2_type_dynamic'].getObjectHrefParams('');

                expect(obj).to.be.empty;
            });

            it('getDynamicBannerUrl без параметров в урле', function() {
                var opts = {
                        main_domain: 'ya.ru',
                        objectHrefParams: null
                    },
                    url = u['b-banner-preview2_type_dynamic'].getDynamicBannerUrl(opts);

                expect(url).to.equal('http://ya.ru');
            });
            it('getDynamicBannerUrl https', function() {
                var opts = {
                        main_domain: 'https://ya.ru'
                    },
                    url = u['b-banner-preview2_type_dynamic'].getDynamicBannerUrl(opts);

                expect(url).to.equal('https://ya.ru');
            });
            it('getDynamicBannerUrl с параметрами в урле', function() {
                var opts = {
                        main_domain: 'ya.ru',
                        objectHrefParams: {
                            a: 'b',
                            b: 'c'
                        },
                        isOpenStat: true,
                        isClickTrack: true
                    },
                    url = u['b-banner-preview2_type_dynamic'].getDynamicBannerUrl(opts);

                expect(url).to.equal('http://ya.ru?a=b&b=c&_openstat=dGVzdDsxOzE7&yclid=' + yclid);
            });
        });

        describe('Наличие параметров в урле', function() {
            it('параметры в сслыке', function() {
                modelGroup.update({ href_params: 'utm_source=yandex&utm_medium=cpc&utm_term={keyword}&utm_campaign=DTO_{campaign_id}&utm_content={source}|{source_type}' });
                clock.tick(500);
                expect(viewModel.get('isHrefHasParams')).to.equal(true);
            });

            it('параметры в сслыке', function() {
                var url = viewModel.get('url'), href_params = 'utm_source=yandex&utm_medium=cpc&utm_term={keyword}&utm_campaign=DTO_{campaign_id}&utm_content={source}|{source_type}';

                modelGroup.update({ href_params: href_params });
                clock.tick(500);
                expect(viewModel.get('url')).to.equal(url + '?' + href_params);
            });


            it('удаление параметров', function() {
                modelGroup.update({ href_params: '' });
                clock.tick(500);
                expect(viewModel.get('isHrefHasParams')).to.be.false;
            });

            it('без параметров', function() {
                modelGroup.update({
                    href_model: { href: 'ya.ru?text=обычный текст' }
                });
                clock.tick(500);
                expect(viewModel.get('isHrefHasParams')).to.be.false;
            });
        });

        describe('Поле vcard', function() {
            it('визитки нет', function() {
                modelBanner.update({
                    is_vcard_open: 0,
                    isVCardEmpty: true
                });
                clock.tick(500);

                expect(viewModel.get('vcard')).to.be.false;
            });

            it('визитка есть, но пустая', function() {
                modelBanner.update({
                    is_vcard_open: 1,
                    isVCardEmpty: true
                });
                clock.tick(500);

                expect(viewModel.get('vcard')).to.be.false;
            });

            it('визитка есть и не пустая', function() {
                modelBanner.update({
                    is_vcard_open: 1,
                    isVCardEmpty: false
                });
                clock.tick(500);

                expect(viewModel.get('vcard')).to.be.true;
            });
        });

        describe('Поле sitelinks', function() {
            var objSitelinks = {
                title0: 'Сайтлинка 1',
                href0: 'ya.ru',
                url_protocol0: 'http://',
                description0: 'Описание',
                title1: 'Сайтлинка 2',
                href1: 'yandex.ru',
                url_protocol1: 'http://',
                description1: ''
            };

            it('с openstsat', function() {
                modelCampaign.set('statusOpenStat', 'Yes');
                modelBanner.set('sitelinks', {});
                modelBanner.set('sitelinks', objSitelinks);
                clock.tick(500);

                expect(viewModel.get('sitelinks')[0]).to.eql({
                    title: 'Сайтлинка 1',
                    url: 'http://ya.ru?_openstat=dGVzdDsxOzE7',
                    description: 'Описание'
                });

                expect(viewModel.get('sitelinks')[1]).to.eql({
                    title: 'Сайтлинка 2',
                    url: 'http://yandex.ru?_openstat=dGVzdDsxOzE7',
                    description: ''
                });
            });
        });
    });
});
