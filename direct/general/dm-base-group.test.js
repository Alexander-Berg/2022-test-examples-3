describe('dm-base-group', function() {
    // специальные тестовые баннер, фраза и список фраз - это просто стабы
    // и группа, чтобы переопределить banners.modelName
    // а иначе инициализировать dm-base-group нельзя
    BEM.MODEL.decl('dm-test-image-model', {
        href: 'string',
        width: 'number',
        height: 'number'
    });

    BEM.MODEL.decl('dm-test-creative-model', {
        creative_id: 'string',
        width: 'number',
        height: 'number'
    });

    BEM.MODEL.decl({ model: 'dm-test-banner', baseModel: 'dm-base-banner' }, {
        ad_type: {
            type: 'string',
            default: 'text'
        },
        image_ad: {
            type: 'model',
            modelName: 'dm-test-image-model'
        },
        creative: {
            type: 'model',
            modelName: 'dm-test-creative-model'
        }
    }, {
        toJSONForCopy: function() {
            return {
                id: this.get('modelId'),
                ad_type: this.get('ad_type')
            }
        }
    });

    BEM.MODEL.decl('dm-test-phrase', {
        phrase: 'string',
        is_suspended: 'boolean',
        phraseId: 'string',
        state: 'string'
    });

    BEM.MODEL.decl('dm-test-phrases', {
        phrases: {
            type: 'models-list',
            modelName: 'dm-test-phrase'
        }
    });

    BEM.MODEL.decl({ model: 'dm-test-group', baseModel: 'dm-base-group' }, {
        banners: {
            type: 'models-list',
            modelName: 'dm-test-banner'
        },

        isSingleGroup: 'boolean',

        phrases: 'object'
    }, {
        getPhrasesModels: function() {
            var phraseModel = getPhrases();
            return phraseModel.get('phrases');
        },
        addBannerToList: function() {
            //это переопределение для тестов, оно ничего не делает
        },
        dataToModelData: function() {
            //это переопределение для тестов, оно ничего не делает
        }
    });

    function getPhrases() {
        var phrasesList = groupModel.get('phrases');
        return BEM.MODEL.create({ name: 'dm-test-phrases' }, phrasesList)
    }

    var groupData = {
            adgroup_type: 'text',
            cid: 1
        },
        campaignData = {
            cid: 1,
            currency: 'RUB',
            strategy: {
                search: {
                    name: 'default'
                }
            }
        },
        groupModel,
        campaignModel,
        sandbox;

    beforeEach(function() {
        campaignModel = BEM.MODEL.create({ name: 'm-campaign', id: 1 }, campaignData);
        groupModel = BEM.MODEL.create({ name: 'dm-test-group', id: 1, parentName: 'm-campaign', parentId: 1 }, groupData);
        sandbox = sinon.sandbox.create({ useFakeTimers: true, useFakeServer: true });
        sandbox.stub(u.campaign, 'getBannerModelName').callsFake(function() {
            return 'dm-test-banner'
        })
    });

    afterEach(function() {
        sandbox.restore();
        groupModel.destruct();
        campaignModel.destruct();
    });

    describe('Методы getRandomActivePhrase', function() {
        function makePhrases(phrasesIds, suspendedNum, declinedNum) {
            var phrases = phrasesIds.map(function(num) {
                var phrase = {
                    id: num,
                    phrase: 'phrase' + num,
                    is_suspended: suspendedNum > 0,
                    state: declinedNum > 0 ? 'declined' : 'active'
                };

                suspendedNum --;
                declinedNum --;

                return phrase;
            });

            return { phrases: phrases };
        }

        var phrases = makePhrases([1, 2, 3], 1, 0);

        it('Метод getRandomActivePhraseModel триггерит метод утилит getRandomActivePhrase', function() {
            sandbox.spy(u['dm-base-group'], 'getRandomActivePhrase');
            groupModel.set('phrases', phrases);

            groupModel.getRandomActivePhraseModel();

            expect(u['dm-base-group'].getRandomActivePhrase.called).to.be.true;
        });

        it('Метод getRandomActivePhrase возвращает 1 фразу', function() {
            groupModel.set('phrases', phrases);
            var phrase = groupModel.getRandomActivePhraseModel();

            expect(phrase.name).to.equal('dm-test-phrase');
        });

        it('Метод getRandomActivePhrase возвращает 1 активную фразу', function() {
            groupModel.set('phrases', phrases);

            var phrase = groupModel.getRandomActivePhraseModel();

            expect(phrase.get('is_suspended')).to.equal(false);
        });

        it('Метод getRandomActivePhrase вовзращает undefined, если активных фраз нет', function() {
            groupModel.set('phrases', makePhrases([1, 2, 3], 3, 0));

            var phrase = groupModel.getRandomActivePhraseModel();

            expect(phrase).to.equal(undefined);
        });

        it('Метод getRandomActivePhrase возвращает undefined, если все фразы отклонены модератором', function() {
            groupModel.set('phrases', makePhrases([1, 2, 3], 0, 3));

            var phrase = groupModel.getRandomActivePhraseModel();

            expect(phrase).to.equal(undefined);
        });

        it('Метод getRandomActivePhrase вовзращает undefined, если фраз нет', function() {
            groupModel.set('phrases', {
                phrases: []
            });

            var phrase = groupModel.getRandomActivePhraseModel();

            expect(phrase).to.equal(undefined);
        });
    });

    describe('Методы на получение баннеров', function() {
        beforeEach(function() {
            groupModel.set('banners', [
                {
                    modelId: 10,
                    bid: 6
                },
                {
                    modelId: 34,
                    bid: 9
                }
            ]);
        });

        [
            {
                method: 'getBanners',
                args: [],
                expectancePosTitle: 'модели баннеров',
                expectancePos: { value: 'length', equal: 2 },
                expectanceNegTitle: 'пустой массив',
                expectanceNeg: { value: 'length', equal: 0 }
            },
            {
                method: 'getBannersWhere',
                args: { bid: 6 },
                expectancePosTitle: 'модель баннера с id - 6',
                expectancePos: { value: 'bid', equal: 6 },
                expectanceNegTitle: 'пустой массив',
                expectanceNeg: { value: 'length', equal: 0 }
            },
            {
                method: 'getBannerByBid',
                args: 6,
                expectancePosTitle: 'модель баннера с bid - 6',
                expectancePos: { value: 'bid', equal: 6 },
                expectanceNegTitle: 'undefined',
                expectanceNeg: { value: 'bid', equal: undefined }
            },
            {
                method: 'getBannerByModelId',
                args: 34,
                expectancePosTitle: 'модель баннера с modelId - 34',
                expectancePos: { value: 'modelId', equal: 34 },
                expectanceNegTitle: 'undefined',
                expectanceNeg: { value: 'modelId', equal: undefined }
            }
        ].forEach(function(testData) {
            it('Метод ' + testData.method + ' c аргументами: ' + JSON.stringify(testData.args) + ' возвращает ' + testData.expectancePosTitle, function() {
                var resData = groupModel[testData.method](testData.args),
                    expValue = testData.expectancePos.value,
                    resValue = expValue == 'length' ? resData.length : (resData[0] || resData).get(expValue);

                expect(resValue).to.equal(testData.expectancePos.equal)
            });

            it('Если баннера(ов) нет, метод ' + testData.method + ' c аргументами: ' + JSON.stringify(testData.args) + ' возвращает ' + testData.expectanceNegTitle, function() {
                groupModel.set('banners', []);

                var resData = groupModel[testData.method](testData.args),
                    expValue = testData.expectanceNeg.value,
                    resValue = expValue == 'length' ? resData.length : (resData && resData.length || resData);

                expect(resValue).to.equal(testData.expectanceNeg.equal);
            });
        });
    });

    describe('Метод bulkSetBannerStatusById', function() {
        var statusesArray = [
            { bid: '6', value: true },
            { bid: '7', value: false },
            { bid: '8', value: true }
        ];
        beforeEach(function() {
            groupModel.update({
                'adgroup_id': '1',
                'banners': [
                    { bid: 6, statusShow: 'No' },
                    { bid: 7, statusShow: 'Yes' },
                    { bid: 8, statusShow: 'No' }
                ]
            });
        });

        it('Изначально статусы корректно установлены', function() {
            expect(groupModel.getBannerByBid('6').get('statusShow')).to.equal('No');
            expect(groupModel.getBannerByBid('7').get('statusShow')).to.equal('Yes');
            expect(groupModel.getBannerByBid('8').get('statusShow')).to.equal('No');
        });

        it('При вызове дергается ручка setBannersStatuses', function() {
            groupModel.bulkSetBannerStatusById(statusesArray);

            expect(sandbox.server.requests[0].requestBody.indexOf('setBannersStatuses')).not.to.equal(-1);
        });

        it('При положительном результате баннерам выставляется соответствующий статус', function() {
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, success: true } )]);

            groupModel.bulkSetBannerStatusById(statusesArray);

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(groupModel.getBannerByBid('6').get('statusShow')).to.equal('Yes');
            expect(groupModel.getBannerByBid('7').get('statusShow')).to.equal('No');
            expect(groupModel.getBannerByBid('8').get('statusShow')).to.equal('Yes');
        });

        it('При ошибке статус баннеров не меняется', function() {
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, success: false } )]);

            groupModel.bulkSetBannerStatusById(statusesArray);

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(groupModel.getBannerByBid('6').get('statusShow')).to.equal('No');
            expect(groupModel.getBannerByBid('7').get('statusShow')).to.equal('Yes');
            expect(groupModel.getBannerByBid('8').get('statusShow')).to.equal('No');
        });
    });

    describe('Методы архивации и удаления баннеров', function() {
        beforeEach(function() {
            sandbox.stub(u, 'getUrl').callsFake(function() {
                return '#';
            });

            groupModel.update({
                'adgroup_id': '1',
                'banners_arch_quantity': 1,
                'banners': [{
                    bid: 6,
                    statusShow: 'No'
                }, {
                    bid: 7,
                    statusShow: 'Yes'
                }, {
                    bid: 8,
                    statusShow: 'No',
                    archive: 'Yes'
                }]
            });
        });

        [
            {
                methodName: 'archiveBannerById',
                redirectMethodName: 'archiveBannerByIdWithRedirect',
                queryName: 'archiveBanner',
                arg: 6,
                checks: [
                    {
                        toCheck: 'banner',
                        field: 'archive',
                        value: 'Yes'
                    }, {
                        toCheck: 'model',
                        field: 'banners_arch_quantity',
                        value: 2
                    }
                ]
            }, {
            methodName: 'unArchiveBannerById',
            redirectMethodName: 'unArchiveBannerByIdWithRedirect',
            queryName: 'unarchiveBanner',
            arg: 8,
            checks: [
                {
                    toCheck: 'banner',
                    field: 'archive',
                    value: 'No'
                }, {
                    toCheck: 'model',
                    field: 'banners_arch_quantity',
                    value: 0
                }
            ]
        }, {
            methodName: 'unArchiveAllBanners',
            redirectMethodName: 'unArchiveAllBannersWithRedirect',
            queryName: 'unarchiveBanner',
            arg: undefined,
            checks: [
                {
                    toCheck: 'bannerWhere',
                    field: 'archive',
                    value: 'No'
                }, {
                    toCheck: 'model',
                    field: 'banners_arch_quantity',
                    value: 0
                }
            ]
        }, {
            methodName: 'deleteBannerById',
            redirectMethodName: 'deleteBannerByIdWithRedirect',
            queryName: 'delBanner',
            arg: 6,
            checks: [
                {
                    toCheck: 'banner',
                    notExists: true
                }, {
                    toCheck: 'model',
                    condition: 'archived',
                    field: 'banners_arch_quantity',
                    value: 0
                }, {
                    toCheck: 'model',
                    condition: 'unarchived',
                    field: 'banners_arch_quantity',
                    value: 0
                }
            ]
        }].forEach(function(test) {
            it('При вызове метода ' + test.methodName + ' дергается ручка ' + test.queryName, function() {
                groupModel[test.methodName](test.arg);

                expect(sandbox.server.requests[0].url.indexOf(test.queryName)).not.to.equal(-1);
            });

            it('При вызове метода ' + test.redirectMethodName + ' дергается метод u.getUrl(из doCmd) с правильными аргументами', function() {
                var argsObject = u._.extend({
                    adgroup_ids: "1",
                    cid: 1,
                    csrf_token: undefined,
                    ulogin: undefined
                }, test.arg ? { bid: test.arg } : { unarchive_whole_group: 1 });

                groupModel[test.redirectMethodName](test.arg);

                expect(u.getUrl.calledWith(test.queryName, argsObject)).to.equal(true);
            });

            test.checks.forEach(function(check) {
                var checkTitle = '';
                switch (check.toCheck) {
                    case 'banner':
                        check.notExists && (checkTitle += 'баннер не существует');
                        check.field &&
                        (checkTitle += 'поле баннера ' + check.field + ' принимает значение ' + check.value);
                        break;
                    case 'model':
                        checkTitle += 'поле модели ';
                        check.condition &&
                        (checkTitle += '(при условии, что у баннера статус ' + check.condition + ') ');
                        checkTitle += check.field + ' принимает значение ' + check.value;
                        break;
                    case 'bannerWhere':
                        checkTitle += 'все баннеры получают archive:No';
                        break;
                }

                it('При вызове метода ' + test.methodName + ' и успешном ответе, ' + checkTitle, function() {
                    var isArchived;

                    if (check.condition) {
                        isArchived = check.condition == 'archived';
                        groupModel.getBannerByBid('6').set('archive', isArchived ? 'Yes' : 'No');
                        groupModel.set('banners_arch_quantity', +isArchived);
                    }

                    sandbox.server.respondWith([200, {"Content-Type":"application/json"},
                        JSON.stringify({ "requestId": 1, status: 'success' } )]);

                    groupModel[test.methodName](test.arg);

                    sandbox.clock.tick(100);
                    sandbox.server.respond();
                    sandbox.clock.tick(100);

                    switch (check.toCheck) {
                        case 'banner':
                            check.notExists &&
                            (expect(groupModel.getBannerByBid(test.arg)).to.equal(undefined));
                            check.field &&
                            (expect(groupModel.getBannerByBid(test.arg).get(check.field)).to.equal(check.value));
                            break;
                        case 'model':
                            expect(groupModel.get(check.field)).to.equal(check.value);
                            break;
                        case 'bannerWhere':
                            expect(groupModel.getBannersWhere({ archive: 'No' }).length).to.equal(3);
                            expect(groupModel.getBannersWhere({ archive: 'Yes' }).length).to.equal(0);
                            break;
                    }
                });
            });
        });
    });

    describe('Метод requestGroupDataAndUpdate', function() {
        it('Если передано onlyBanners: true и все баннеры подгружены, сразу возвращается deferred, метод _requestGroupData не дергается', function() {
            groupModel.update({
                'banners': [
                    {
                        bid: 6,
                        archive: 'No'
                    },
                    {
                        bid: 8,
                        archive: 'No'
                    }
                ],
                'banners_quantity': 2,
                'banners_arch_quantity': 0
            });
            sandbox.spy(groupModel, '_requestGroupData');
            var deferred = groupModel.requestGroupDataAndUpdate({ onlyBanners: true, isArchive: false });

            expect(deferred).not.to.equal(undefined);
            expect(groupModel._requestGroupData.called).to.be.equal(false);
        });

        it('Если onlyBanners: false, дергается ручка getAdGroup', function() {
            groupModel.update({
                'banners': [
                    {
                        bid: 6,
                        archive: 'No'
                    },
                    {
                        bid: 8,
                        archive: 'No'
                    }
                ],
                'banners_quantity': 2,
                'banners_arch_quantity': 0
            });

            groupModel.requestGroupDataAndUpdate({ onlyBanners: false, isArchive: false });

            expect(sandbox.server.requests[0].url.indexOf('cmd=getAdGroup')).not.to.equal(-1);
        });

        [
            { onlyBanners: true, isArchive: false },
            { onlyBanners: true, isArchive: true }
        ].forEach(function(test) {
            it('Если ' + JSON.stringify(test) + ', но количество баннеров в модели и счетчике не совпадает, дергается ручка getAdGroup', function() {
                groupModel.update({
                    'banners': [
                        {
                            bid: 6,
                            archive: 'Yes'
                        },
                        {
                            bid: 8,
                            archive: 'No'
                        }
                    ],
                    'banners_quantity': 2,
                    'banners_arch_quantity': 0
                });

                groupModel.requestGroupDataAndUpdate(test);

                expect(sandbox.server.requests[0].url.indexOf('cmd=getAdGroup')).not.to.equal(-1);
            });
        });

        it('При ошибке в ajax-запросе deferred reject-ится', function() {
            groupModel.update({
                'banners': [
                    {
                        bid: 6,
                        archive: 'Yes'
                    },
                    {
                        bid: 8,
                        archive: 'No'
                    }
                ],
                'banners_quantity': 2,
                'banners_arch_quantity': 0
            });
            sandbox.server.respondWith([200, {"Content-Type":"application/json"}, 'false']);

            var deferred = groupModel.requestGroupDataAndUpdate({ onlyBanners: true, isArchive: true });

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(deferred.state()).to.equal('rejected');
        });

        describe('При успешном ajax-запросе', function() {
            var data = {
                banners: [
                    {
                        bid: 10,
                        archive: 'No'
                    },
                    {
                        bid: 20,
                        arhive: 'Yes'
                    }
                ],
                banners_quantity: 4,
                banners_arch_quantity: 1,
                adgroup_type: 'test-type'
            };

            beforeEach(function() {
                sandbox.spy(groupModel, 'dataToModelData');
                groupModel.update({
                    'banners': [
                        {
                            bid: 6,
                            archive: 'No'
                        },
                        {
                            bid: 8,
                            archive: 'No'
                        }
                    ],
                    'banners_quantity': 1,
                    'banners_arch_quantity': 0
                });

                sandbox.server.respondWith([200, {"Content-Type":"application/json"}, JSON.stringify(data)
                ]);
            });

            it('Если выбран режим onlyBanners, то дергается метод addBannerToList', function() {
                sandbox.spy(groupModel, 'addBannerToList');
                groupModel.requestGroupDataAndUpdate({ onlyBanners: true, isArchive: false });

                sandbox.clock.tick(100);
                sandbox.server.respond();
                sandbox.clock.tick(100);

                expect(groupModel.addBannerToList.called).to.equal(true);
            });

            it('Если не выбран режим onlyBanners, то дергается метод dataToModelData', function() {
                groupModel.requestGroupDataAndUpdate({ onlyBanners: false, isArchive: false });

                sandbox.clock.tick(100);
                sandbox.server.respond();
                sandbox.clock.tick(100);

                expect(groupModel.dataToModelData.called).to.equal(true);
            });

            it('Если не выбран режим onlyBanners, то дергается метод dataToModelData c параметром, содержащим поле minus_words', function() {
                groupModel.requestGroupDataAndUpdate({ onlyBanners: false, isArchive: false });

                sandbox.clock.tick(100);
                sandbox.server.respond();
                sandbox.clock.tick(100);

                expect(groupModel.dataToModelData.calledWith(u._.extend(data, { minus_words: [] }))).to.equal(true);
            })
        });
    });

    describe('Метод getPrevBannerData', function() {
        it('Если указан первый в списке баннер, создает баннер с id:prev', function() {
            groupModel.update({
                'banners': [{
                    bid: 6,
                    modelId: 5,
                    ad_type: 'text'
                }],
                isSingleGroup: true
            });

            expect(groupModel.getPrevBannerData(5, 'text').id).to.equal('prev');
        });

        describe('Если предыдущий баннер существует', function() {
            beforeEach(function() {
                groupModel.update({
                    'banners': [
                        {
                            bid: 12,
                            modelId: 12,
                            ad_type: 'text'
                        },
                        {
                            bid: 6,
                            modelId: 6,
                            ad_type: 'text'
                        },
                        {
                            bid: 7,
                            modelId: 7,
                            ad_type: 'text'
                        }
                    ]
                });
            });
            it('Если у предыдущего баннера тип совпадает с запрошенным, возвращаем этот баннер', function() {
                expect(groupModel.getPrevBannerData(7, 'text').id).to.equal(6);
            });

            it('Если у предыдущего баннера тип не совпадает с запрошенным, возвращаем ближайший баннер запрошенного типа', function() {
                groupModel.getBannerByBid('6').set('ad_type', 'test');

                expect(groupModel.getPrevBannerData(7, 'text').id).to.equal(12);
            });

            it('Если нет ни одного баннера совпадающего типа, возвращаем пустой объект', function() {
                expect(groupModel.getPrevBannerData(7, 'test')).to.deep.equal({});
            });

            describe('Допустим баннеров два, и предыдущий баннер - графический', function() {
                var imageModel,
                    creativeModel;

                beforeEach(function() {
                    imageModel = BEM.MODEL.create({ name: 'dm-test-image-model' });
                    creativeModel = BEM.MODEL.create({ name: 'dm-test-creative-model' });

                    groupModel.update({
                        'banners': [
                            {
                                bid: 12,
                                modelId: 12,
                                ad_type: 'image_ad'
                            },
                            {
                                bid: 5,
                                modelId: 5,
                                ad_type: 'text'
                            }
                        ]
                    });

                });

                it('Если у текущего баннера нет изображения и КК, возвращаем пустой объект', function() {
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего баннера есть изображение, не заполнено поле href, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('12').set('image_ad', BEM.MODEL.create({ name: 'dm-test-image-model' }, {
                        href: 'aa'
                    }));

                    groupModel.getBannerByBid('5').set('image_ad', imageModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у предыдушего баннера не заполнено поле href, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('5').set('image_ad', BEM.MODEL.create({ name: 'dm-test-image-model' }, {
                        href: 'aa'
                    }));

                    groupModel.getBannerByBid('12').set('image_ad', imageModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего и предыдущего баннера разные высоты, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('5').set('image_ad', BEM.MODEL.create({ name: 'dm-test-image-model' }, {
                        href: 'aa',
                        height: 12,
                        width: 40
                    }));

                    imageModel.update({
                        href: 'bb',
                        height: 15,
                        width: 40
                    });

                    groupModel.getBannerByBid('12').set('image_ad', imageModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего и предыдущего баннера разные ширины, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('5').set('image_ad', BEM.MODEL.create({ name: 'dm-test-image-model' }, {
                        href: 'aa',
                        height: 12,
                        width: 40
                    }));

                    imageModel.update({
                        href: 'bb',
                        height: 12,
                        width: 45
                    });

                    groupModel.getBannerByBid('12').set('image_ad', imageModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего и предыдущего баннера есть ссылки и одинаковые размеры, возвращаем предущий баннер', function() {
                    groupModel.getBannerByBid('5').set('image_ad', BEM.MODEL.create({ name: 'dm-test-image-model' }, {
                        href: 'aa',
                        height: 12,
                        width: 40
                    }));

                    imageModel.update({
                        href: 'bb',
                        height: 12,
                        width: 40
                    });

                    groupModel.getBannerByBid('12').set('image_ad', imageModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({ id: 12, ad_type: 'image_ad' });
                });

                it('Если у текущего баннера есть КК, но не заполнено поле creative_id, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('12').set('creative', BEM.MODEL.create({ name: 'dm-test-creative-model' }, {
                        creative_id: 'aa'
                    }));

                    groupModel.getBannerByBid('5').set('creative', creativeModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у предыдушего баннера есть КК, но не заполнено поле creative_id, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('5').set('creative', BEM.MODEL.create({ name: 'dm-test-creative-model' }, {
                        creative_id: 'aa'
                    }));

                    groupModel.getBannerByBid('12').set('creative', creativeModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего и предыдущего баннера разные высоты в КК, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('5').set('creative', BEM.MODEL.create({ name: 'dm-test-creative-model' }, {
                        creative_id: 'aa',
                        height: 12,
                        width: 40
                    }));

                    creativeModel.update({
                        creative_id: 'bb',
                        height: 15,
                        width: 40
                    });

                    groupModel.getBannerByBid('12').set('creative', creativeModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего и предыдущего баннера разные ширины в КК, возвращаем пустой объект', function() {
                    groupModel.getBannerByBid('5').set('creative', BEM.MODEL.create({ name: 'dm-test-creative-model' }, {
                        creative_id: 'aa',
                        height: 12,
                        width: 40
                    }));

                    creativeModel.update({
                        creative_id: 'bb',
                        height: 12,
                        width: 45
                    });

                    groupModel.getBannerByBid('12').set('creative', creativeModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({});
                });

                it('Если у текущего и предыдущего баннера есть creative_id и одинаковые размеры в КК, возвращаем предущий баннер', function() {
                    groupModel.getBannerByBid('5').set('creative', BEM.MODEL.create({ name: 'dm-test-creative-model' }, {
                        creative_id: 'aa',
                        height: 12,
                        width: 40
                    }));

                    creativeModel.update({
                        creative_id: 'bb',
                        height: 12,
                        width: 40
                    });

                    groupModel.getBannerByBid('12').set('creative', creativeModel);
                    expect(groupModel.getPrevBannerData(5, 'image_ad')).to.deep.equal({ id: 12, ad_type: 'image_ad' });
                });

            });
        });
    });

    describe('Прочие методы', function() {
        [
            { bannersNum: [1], expect: true },
            { bannersNum: [1, 2], expect: false },
            { bannersNum: [], expect: false }
        ].forEach(function(test) {
            it('Метод isLastBanner возвращает ' +test.expect + ', если в группе ' + test.bannersNum.length + ' баннер/а/ов', function() {
                var banners = test.bannersNum.map(function(num) {
                    return {
                        bid: num,
                        modelId: num,
                        ad_type: 'image_ad'
                    }
                });

                groupModel.update({
                    'banners': banners,
                    banners_quantity: test.bannersNum
                });

                expect(groupModel.isLastBanner()).to.equal(test.expect);
            });
        });

        it('Метод getCampaignModel возвращает модель кампании', function() {
            expect(groupModel.getCampaignModel().name).to.equal('m-campaign');
        });

        it('Метод getCurrency возвращает валюту кампании', function() {
            expect(groupModel.getCurrency()).to.equal('RUB');
        });

        it('Метод getStrategy возвращает модель стратегии кампании', function() {
            expect(groupModel.getStrategy().search.name).to.equal('default');
        });
    });

});
