describe('b-banner-preview2_type_text', function() {
    var clock,
        constsStub,
        genYclidStub,
        yclid = 48313024715480424;

    before(function() {

        constsStub = sinon.stub(u, 'consts');
        constsStub.withArgs('SITELINKS_NUMBER').returns(4);

        genYclidStub = sinon.stub(u, 'generateYclid').callsFake(function() { return yclid; });

        clock = sinon.useFakeTimers();
    });

    after(function() {
        constsStub.restore();
        genYclidStub.restore();
        clock.restore();
    });

    describe('Генерация входных данных', function() {
        var objCampaign = {
                statusOpenStat: 'Yes',
                status_click_track: true,
                cid: '46068371',
                no_title_substitute: 0,
                groups: [
                    {
                        modelId: '580206659',
                        adgroup_id: '580206659',
                        adgroup_type: 'text',
                        geo: '225',
                        geo_names: 'Россия',
                        isBidable: true,
                        banners: [
                            {
                                href: 'http://okna.ru?text={param1}',
                                title: 'Кислые лимоны',
                                body: 'Ледяной борщ! Креветка уже умерла',
                                bid: 1388445106,
                                is_template_banner: 0,
                                sitelinks: [
                                    {
                                        title: 'быстрая ссылка 1',
                                        href: 'http://ya.ru'
                                    },
                                    {
                                        title: 'быстрая ссылка 2',
                                        href: 'http://direct.yandex.ru'
                                    }
                                ],
                                image: 'ZwhVOu6KRacOMlGvYMdHkg',
                                domain: 'okna.ru',
                                hash_flags: { age: 12, baby_food: 5 },
                                disclaimer: '',
                                market_rating: -1,
                                loadVCardFromClient: undefined,
                                geo_exception: {}
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
            modelGeo,
            ctxDataFromServer,
            viewModel,
            ctxDataFromModels;

        before(function() {
            modelCampaign = BEM.MODEL.create('m-campaign', objCampaign);
            modelGroup = BEM.MODEL.create('m-group', u['m-group'].transformData({
                group: objGroup
            }));
            modelBanner = modelGroup.getBanners()[0];
            modelGeo = BEM.MODEL.create({ name: 'm-geo-regions', id: objGroup.modelId, parentModel: modelGroup }, {
                geo: objGroup.geo,
                geoText: objGroup.geo_names
            });
            modelPhrase = BEM.MODEL.create(
                {
                    name: 'm-phrase-bidable',
                    parentModel: modelGroup,
                    id: objPhrase.id
                },
                objPhrase);

            ctxDataFromServer = u['b-banner-preview2_type_text'].fromServer({
                campaign: objCampaign,
                group: objGroup,
                banner: objBanner,
                phrase: objPhrase
            });

            viewModel = BEM.MODEL.create('b-banner-preview2_type_text');

            viewModel.init({
                campaignId: modelCampaign.id,
                groupId: modelGroup.id,
                bannerId: modelBanner.id,
                phraseId: modelPhrase.id
            });

            ctxDataFromModels = viewModel.toJSON();
        });

        after(function() {
            modelCampaign.destruct();
            modelGroup.destruct();
            modelPhrase.destruct();
            modelBanner.destruct();
            modelGeo.destruct();
            viewModel.destruct();
        });

        [
            'cid',
            'bid',
            'title',
            'body',
            'url',
            'rating',
            'domain',
            'isTemplateBanner',
            'isHrefHasParams',
            'vcard',
            'image',
            'sitelinks',
            'phone',
            'worktime',
            'flags',
            'geo',
            'city',
            'metro',
            'phrase',
            'loadVCardFromClient',
            'titleSubstituteOn',
            'isArchived'
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
    });

    describe('Обновления DM должны повлечь обновление VM', function() {
        var modelCampaign,
            modelGroup,
            modelPhrase,
            modelBanner,
            modelGeo,
            viewModel,
            phraseId = 11;

        beforeEach(function() {
            modelCampaign = BEM.MODEL.create('m-campaign');
            modelGroup = BEM.MODEL.create('m-group', {
                cid: modelCampaign.id,
                banners: [{}],
                phrasesIds: [phraseId]
            });
            modelBanner = modelGroup.getBanners()[0];
            modelGeo = BEM.MODEL.create({ name: 'm-geo-regions', id: modelGroup.id, parentModel: modelGroup });
            modelPhrase = BEM.MODEL.create({ name: 'm-phrase-text', id: phraseId, parentModel: modelGroup });

            viewModel = BEM.MODEL.create('b-banner-preview2_type_text');

            viewModel.init({
                campaignId: modelCampaign.id,
                groupId: modelGroup.id,
                bannerId: modelBanner.id,
                phraseId: modelPhrase.id
            });

        });

        afterEach(function() {
            modelCampaign.destruct();
            modelGroup.destruct();
            modelPhrase.destruct();
            modelBanner.destruct();
            modelGeo.destruct();
            viewModel.destruct();
        });

        it('Обновления заголовка должны обновить title', function() {
            modelBanner.set('title', 'Заголовок');
            clock.tick(500);
            expect(viewModel.get('title')).to.equal('Заголовок');
        });

        it('Обновления описания должны обновить body', function() {
            modelBanner.set('body', 'Описание');
            clock.tick(500);
            expect(viewModel.get('body')).to.equal('Описание');
        });

        describe('При отсутствии заголовка или описания - должны быть стандартные тексты', function() {
            it('title', function() {
                modelBanner.set('title', '');
                clock.tick(500);
                expect(viewModel.get('title')).to.equal('Заголовок объявления');
            });

            it('body', function() {
                modelBanner.set('body', '');
                clock.tick(500);
                expect(viewModel.get('body')).to.equal('Текст вашего объявления о рекламе услуги или товара.');
            });
        });

        describe('Изменение ссылки должно корректно обновить параметр url', function() {
            it('неправильный урл', function() {
                modelBanner.set('href_model', { href: 'qqq' });
                clock.tick(500);
                expect(viewModel.get('url')).to.equal('');
            });

            it('без парметров', function() {
                modelCampaign.update({
                    status_click_track: false,
                    statusOpenStat: 'No'
                });
                modelBanner.set('href_model', { href: 'ya1.ru' });
                clock.tick(500);

                expect(viewModel.get('url')).to.equal('https://ya1.ru');
            });

            it('statusOpenStat', function() {
                modelCampaign.update({
                    status_click_track: false,
                    statusOpenStat: 'Yes'
                });
                modelBanner.set('href_model', {
                    href: 'ya2.ru',
                    url_protocol: 'https://'
                });
                clock.tick(500);

                expect(viewModel.get('url')).to.equal('https://ya2.ru?_openstat=dGVzdDsxOzE7');
            });

            it('status_click_track', function() {
                modelCampaign.update({
                    status_click_track: true,
                    statusOpenStat: 'No'
                });
                modelBanner.set('href_model', {
                    href: 'ya3.ru',
                    url_protocol: 'http://'
                });
                clock.tick(500);

                expect(viewModel.get('url')).to.equal('http://ya3.ru?yclid=' + yclid);
            });

            it('param1, param2', function() {
                modelCampaign.update({
                    status_click_track: false,
                    statusOpenStat: 'No'
                });
                modelBanner.set('href_model', {
                    href: 'ya4.ru?text={param1}&{param2}=20',
                    url_protocol: 'http://'
                });
                modelPhrase.update({
                    param1: 'qqq',
                    param2: 'ccc'
                });
                clock.tick(500);

                expect(viewModel.get('url')).to.equal('http://ya4.ru?text=qqq&ccc=20');
            });
        });

        it('Изменения поля domain берется из поля domain модели m-banner-href', function() {
            modelBanner.set('href_model', {
                domain: 'ozon.ru'
            });
            clock.tick(500);

            expect(viewModel.get('domain')).to.equal('ozon.ru');
        });

        describe('Наличие шаблона в тексте/описании', function() {
            it('шаблон в загловоке', function() {
                modelBanner.set('title', 'Заголовок #шаблон#');
                clock.tick(500);
                expect(viewModel.get('isTemplateBanner')).to.equal(true);
            });

            it('шаблон в описании', function() {
                modelBanner.update({
                    title: 'Заголовок',
                    body: '#Шаблон#'
                });
                clock.tick(500);
                expect(viewModel.get('isTemplateBanner')).to.equal(true);
            });

            it('без шаблона', function() {
                modelBanner.update({
                    title: 'Заголовок',
                    body: 'Заголовк'
                });
                clock.tick(500);
                expect(viewModel.get('isTemplateBanner')).to.equal(false);
            });

            it('шаблон в ссылке должен быть проигнорирован', function() {
                modelBanner.update({
                    title: 'Заголовок',
                    body: 'Шаблон',
                    href_model: { href: 'ya.ru?text=#шаблон#' }
                });
                clock.tick(500);
                expect(viewModel.get('isTemplateBanner')).to.equal(false);
            });
        });

        describe('Наличие параметров/шаблона в урле', function() {
            it('шаблон в сслыке', function() {
                modelBanner.update({
                    href_model: { href: 'ya.ru?text=#шаблон#' }
                });
                clock.tick(500);
                expect(viewModel.get('isHrefHasParams')).to.equal(true);
            });

            it('парметры в сслыке', function() {
                modelBanner.set('href_model', { href: 'ya4.ru?text={param1}&{param2}=20' });
                clock.tick(500);

                expect(viewModel.get('isHrefHasParams')).to.equal(true);
            });

            it('ни шаблона, ни параметров', function() {
                modelBanner.update({
                    href_model: { href: 'ya.ru?text=обычный текст' }
                });
                clock.tick(500);
                expect(viewModel.get('isHrefHasParams')).to.equal(false);
            });
        });

        describe('Поле vcard', function() {
            it('визитки нет', function() {
                modelBanner.update({
                    is_vcard_open: 0,
                    isVCardEmpty: true
                });
                clock.tick(500);

                expect(viewModel.get('vcard')).to.equal(false);
            });

            it('визитка есть, но пустая', function() {
                modelBanner.update({
                    is_vcard_open: 1,
                    isVCardEmpty: true
                });
                clock.tick(500);

                expect(viewModel.get('vcard')).to.equal(false);
            });

            it('визитка есть и не пустая', function() {
                modelBanner.update({
                    is_vcard_open: 1,
                    isVCardEmpty: false
                });
                clock.tick(500);

                expect(viewModel.get('vcard')).to.equal(true);
            });
        });

        it('Поле phone', function() {
            modelBanner.set('vcard', {
                phone: '796-62-15'
            });
            clock.tick(500);
            expect(viewModel.get('phone')).to.equal('796-62-15');
        });

        it('Поле worktime', function() {
            modelBanner.set('vcard', { worktime: '0#4#10#00#18#00' });
            clock.tick(500);
            expect(viewModel.get('worktime')).to.equal('0#4#10#00#18#00');
        });

        it('Поле metro', function() {
            modelBanner.set('vcard', { metro: '20490' });
            clock.tick(500);
            expect(viewModel.get('metro')).to.equal('20490');
        });

        it('Поле city', function() {
            modelBanner.set('vcard', { city: 'Щелково' });
            clock.tick(500);
            expect(viewModel.get('city')).to.equal('Щелково');
        });

        describe('Поле sitelinks', function() {
            var objSitelinks = {
                title0: 'Сайтлинка 1',
                href0: 'ya.ru',
                url_protocol0: 'http://',
                description0: 'Описание',
                turbolanding0: undefined,
                title1: 'Сайтлинка 2',
                href1: 'yandex.ru',
                url_protocol1: 'http://',
                description1: '',
                turbolanding1: undefined
            };

            it('без openstsat', function() {
                modelCampaign.set('statusOpenStat', 'No');
                modelBanner.set('sitelinks', objSitelinks);
                clock.tick(500);

                expect(viewModel.get('sitelinks')[0]).to.eql({
                    title: 'Сайтлинка 1',
                    url: 'http://ya.ru',
                    description: 'Описание',
                    turbolanding: undefined
                });

                expect(viewModel.get('sitelinks')[1]).to.eql({
                    title: 'Сайтлинка 2',
                    url: 'http://yandex.ru',
                    description: '',
                    turbolanding: undefined
                });
            });

            it('с openstsat', function() {
                modelCampaign.set('statusOpenStat', 'Yes');
                modelBanner.set('sitelinks', {});
                modelBanner.set('sitelinks', objSitelinks);
                clock.tick(500);

                expect(viewModel.get('sitelinks')[0]).to.eql({
                    title: 'Сайтлинка 1',
                    url: 'http://ya.ru?_openstat=dGVzdDsxOzE7',
                    description: 'Описание',
                    turbolanding: undefined
                });

                expect(viewModel.get('sitelinks')[1]).to.eql({
                    title: 'Сайтлинка 2',
                    url: 'http://yandex.ru?_openstat=dGVzdDsxOzE7',
                    description: '',
                    turbolanding: undefined
                });
            });
        });

        it('Поле isArchived', function() {
            modelBanner.set('archive', 'Yes');
            clock.tick(500);

            expect(viewModel.get('isArchived')).to.equal(true);
        });

        it('Поле flags', function() {
            modelBanner.set('hash_flags', {
                age: 12,
                alcohol: true
            });
            clock.tick(500);

            expect(viewModel.get('flags')).to.eql({
                age: 12,
                alcohol: true
            });
        });

        it('Замена фразы на другую', function() {
            var modelPhrase2 = BEM.MODEL.create(
                { name: 'm-phrase-text', id: 22, parentModel: modelGroup },
                { key_words: 'фраза1' });

            modelGroup.set('phrasesIds', [22]);

            modelPhrase.destruct();

            clock.tick(2000);

            expect(viewModel.get('phrase')).to.equal('фраза1');
            modelPhrase = viewModel.getDM()['phrase'];
        });

        it('Поле videoExtension', function() {
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
            clock.tick(500);

            expect(viewModel.get('videoExtension')).to.eql(videoExtensionStub);
        });
    });

    describe('Настройки предупреждений', function() {
        describe('dontShow', function() {
            it('Должен быть равен true, если группа скопирована', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ isCopyGroup: true });

                expect(settings.age.dontShow).to.equal(true);
                expect(settings.other.dontShow).to.equal(true);
            });

            it('Должен быть равен true, если группа новая', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ isNewGroup: true });

                expect(settings.age.dontShow).to.equal(true);
                expect(settings.other.dontShow).to.equal(true);
            });

            it('Должен быть равен true, если баннер новый', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ bid: 0 });

                expect(settings.age.dontShow).to.equal(true);
                expect(settings.other.dontShow).to.equal(true);
            });

            it('Должен быть равен false, если баннер и группа не новые/скопирована', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ bid: 12 });

                expect(settings.age.dontShow).to.equal(false);
                expect(settings.other.dontShow).to.equal(false);
            });
        });

        describe('edit должен быть равен true всегда', function() {
            it('Даже если группа новая', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ isNewGroup: 0 });

                expect(settings.age.edit).to.equal(true);
                expect(settings.other.edit).to.equal(true);
            });

            it('Даже если баннер новый', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ bid: 0 });

                expect(settings.age.edit).to.equal(true);
                expect(settings.other.edit).to.equal(true);
            });

            it('Даже если  ни баннер, ни группа не новые', function() {
                var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ bid: 12 });

                expect(settings.age.edit).to.equal(true);
                expect(settings.other.edit).to.equal(true);
            });
        });

        it('addRemove должен быть равен false (добавлять/удалять предупреждения можно в модерации)', function() {
            var settings = u['b-banner-preview2_type_text'].getFlagsSettings({ bid: 12 });

            expect(settings.age.addRemove).to.equal(false);
            expect(settings.other.addRemove).to.equal(false);
        });
    });

});
