describe('dm-cpm-banner-group', function() {
    var mGroup,
        mCampaign,
        mPhrases,
        phrasesIds = [1, 2, 3],
        phrasesData = [
            {
                state: 'new',
                is_deleted: false,
                is_suspended: false,
                key_words: 'фраза1'
            },
            {
                state: 'new',
                is_deleted: false,
                is_suspended: false,
                key_words: 'фраза2'
            },
            {
                state: 'new',
                is_deleted: false,
                is_suspended: false,
                key_words: 'фраза3'
            }
        ];

    function createModel(data) {

        mCampaign = BEM.MODEL.create({
            name: 'dm-cpm-banner-campaign',
            id: 1
        }, {
            maxKeywordLimit: 2
        });

        mGroup = BEM.MODEL.create({
            name: 'dm-cpm-banner-group',
            id: 1,
            parentName: 'dm-cpm-banner-campaign',
            parentId: mCampaign.id
        }, data || { group_name: 'test' });

    }

    function createPhraseModels(modalName, data) {
        data || (data = {});

        var ids = data.phrasesIds || phrasesIds,
            phrases = data.phrasesData || phrasesData;

        mPhrases = ids.map(function(id, index) {
            return BEM.MODEL.create({
                name: modalName,
                id: id,
                parentModel: mGroup
            }, phrases[index]);
        });

    }

    function destroyModel() {
        mGroup && mGroup.destruct && mGroup.destruct();
        mCampaign && mCampaign.destruct && mCampaign.destruct();

        if (mPhrases && mPhrases.length) {
            mPhrases.forEach(function(model) {
                model.destruct();
            });
        }
    }

    before(function() {
        u.stubCurrencies();
        u.stubDMParams(u.consts);
    });

    after(function() {
        destroyModel();
        u.restoreConsts();
    });

    describe('Инициализация', function() {

        before(function() {
            createModel({});
        });

        after(function() {
            mGroup.destruct && mGroup.destruct();
            mCampaign.destruct && mCampaign.destruct();
        });

        it('В поле displayConditions крипта', function() {
            expect(mGroup.get('displayConditions')).to.be.eq('crypta');
        });
    });

    describe('Валидация', function() {

        function getValidationErrorRules(validation, field) {
            return (u._.get(validation, 'errorsData[' + field + ']', [])).map(function(error) {
                return error.rule;
            });
        }

        afterEach(function() {
            mGroup.destruct && mGroup.destruct();
            mCampaign.destruct && mCampaign.destruct();
        });

        describe('new_phrases', function() {

            describe('hasActive', function() {

                it('Должна сработать валидация', function() {
                    createModel({
                        displayConditions: 'keywords'
                    });

                    expect(getValidationErrorRules(mGroup.validate(), 'new_phrases'))
                        .to.include('hasActive');
                });

                it('Валидация не сработает, если есть хоть одна новая фраза', function() {
                    createModel({ group_name: 'test', new_phrases: ['test'] });

                    expect(getValidationErrorRules(mGroup.validate(), 'new_phrases'))
                        .to.not.include('hasActive');
                });

                it('если есть активная фраза, валидация не сработает c текстом фразы', function() {
                    var mPhrase;

                    createModel({ group_name: 'test', phrasesIds: [1] });
                    mPhrase = BEM.MODEL.create({
                        name: 'm-phrase-text',
                        id: 1,
                        parentModel: mGroup
                    }, {
                        state: 'new',
                        is_deleted: false,
                        is_suspended: false,
                        key_words: 'кракозябра'
                    });

                    expect(getValidationErrorRules(mGroup.validate(), 'new_phrases'))
                        .to.not.include('hasActive');

                    mPhrase.destruct();
                });
            });

            describe('phraseCount', function() {

                it('Должна сработать валидация, так как достигнут лимит', function() {

                    createModel({
                        group_name: 'test',
                        new_phrases: ['test', 'test2', 'test3'],
                        displayConditions: 'keywords'
                    });

                    expect(getValidationErrorRules(mGroup.validate(), 'new_phrases'))
                        .to.include('phraseCount');
                });

                it('Валидация не сработает, так как фраз нет', function() {
                    createModel();

                    expect(getValidationErrorRules(mGroup.validate(), 'new_phrases'))
                        .to.not.include('phraseCount');
                });

            });
        });

        describe('general_limit_price', function() {

            it(
                'Должна сработать валидация «Укажите ставку для новых условий», если добавились новые фразы',
                function() {
                    createModel({
                        group_name: 'test',
                        has_general_limit_price: true,
                        new_phrases: ['test'],
                        displayConditions: 'keywords'
                    });

                    mGroup.get('new_phrases').push('test2');

                    expect(getValidationErrorRules(mGroup.validate(), 'general_limit_price'))
                        .to.include('required');
                }
            );

            it(
                'Должна сработать валидация «Укажите ставку для новых условий», если изменили сохраненные фразы',
                function() {
                    createModel({
                        group_name: 'test',
                        phrasesIds: [1],
                        has_general_limit_price: true,
                        new_phrases: ['test'],
                        displayConditions: 'keywords'
                    });

                    createPhraseModels('m-phrase-text', {
                        phrasesIds: [1],
                        phrasesData: [
                            {
                                state: 'active',
                                is_deleted: false,
                                is_suspended: false,
                                key_words: 'фраза'
                            }
                        ]
                    });

                    mGroup.fix();

                    mPhrases[0].set('key_words', 'новая фраза');

                    expect(getValidationErrorRules(mGroup.validate(), 'general_limit_price'))
                        .to.include('required');
                }
            );

            it(
                'Не должна сработать валидация «Укажите ставку для новых условий», если удалили сохраненные фразы и больше никак не меняли другие условия показа',
                function() {
                    createModel({
                        group_name: 'test',
                        phrasesIds: [1, 2],
                        has_general_limit_price: true,
                        new_phrases: ['test'],
                        displayConditions: 'keywords'
                    });

                    createPhraseModels('m-phrase-text', {
                        phrasesIds: [1, 2],
                        phrasesData: [
                            {
                                state: 'active',
                                is_deleted: false,
                                is_suspended: false,
                                key_words: 'фраза'
                            },
                            {
                                state: 'active',
                                is_deleted: false,
                                is_suspended: false,
                                key_words: 'фраза 2'
                            }
                        ]
                    });

                    mGroup.fix();

                    mPhrases[0].destruct();
                    mGroup.set('phrasesIds', mGroup.get('phrasesIds').slice(1));

                    expect(getValidationErrorRules(mGroup.validate(), 'general_limit_price'))
                        .to.not.include('required');
                }
            );

            // TODO @skywhale ретаргетинг переехал в крипу, необходиы написать новые тесты
            it.skip(
                'Должна сработать валидация «Укажите ставку для новых условий», если добавили новое условие ретаргетинга',
                function() {
                    createModel({
                        group_name: 'test',
                        has_general_limit_price: true,
                        retargetingsIds: [{ ret_id: '123', ret_cond_id: '111' }],
                        displayConditions: 'keywords'
                    });

                    mGroup
                        .set(
                            'retargetingsIds',
                            [{ ret_id: '321', ret_cond_id: '222' }].concat(mGroup.get('retargetingsIds'))
                        )
                        .set(
                            'newRetargetingsIds',
                            [{ ret_id: '321', ret_cond_id: '222' }]
                        );

                    expect(getValidationErrorRules(mGroup.validate(), 'general_limit_price'))
                        .to.include('required');
                }
            );

            // TODO @skywhale ретаргетинг переехал в крипу, необходиы написать новые тесты
            it.skip(
                'Не должна сработать валидация «Укажите ставку для новых условий», если удалили сохраненное условие ретаргетинга и больше никак не меняли другие условия показа',
                function() {
                    createModel({
                        group_name: 'test',
                        has_general_limit_price: true,
                        retargetingsIds: [
                            { ret_id: '123', ret_cond_id: '111' },
                            { ret_id: '456', ret_cond_id: '345' }
                        ],
                        displayConditions: 'keywords'
                    });

                    mGroup.set('retargetingsIds', [{ ret_id: '123', ret_cond_id: '111' }]);

                    expect(getValidationErrorRules(mGroup.validate(), 'general_limit_price'))
                        .to.not.include('required');
                }
            );

        });

    });

    describe('Методы', function() {

        describe('Получение фраз', function() {

            describe('Общее про фразы', function() {
                before(function() {
                    createModel({
                        group_name: 'test',
                        phrasesIds: phrasesIds,  // текущие
                        new_phrases: ['test'], // новые
                        isBidable: true
                    });
                    createPhraseModels('m-phrase-bidable');
                });

                after(function() {
                    mPhrases.forEach(function(model) { model.destruct(); });
                    destroyModel();
                });

                it('getPhrases Должен вернуть фразы', function() {
                    expect(mGroup.getPhrases()).to.be.eql(['фраза1', 'фраза2', 'фраза3']);
                });

                it('getPhrasesData Должен вернуть данные фраз старые + новые', function() {
                    expect(mGroup.getPhrasesData().length).to.be.eq(4);
                });

                it('isPhrasesChanged. Фразы не менялись. Должен вернуть false', function() {
                    expect(mGroup.isPhrasesChanged()).to.be.false;
                });

                it('isPhrasesChanged. Фразы менялись. Должен вернуть true', function() {
                    mPhrases[0].set('phrase', 'changed');

                    expect(mGroup.isPhrasesChanged()).to.be.true;
                });
            });

            [true, false].forEach(function(isBidable) {
                it('isBidable = ' + isBidable.toString() +
                    '. getPhrasesModels() Должен вернуть модели фраз ' +
                    (isBidable ? 'm-phrase-bidable (3 шт)' : 'm-phrase-text (3 шт)'), function() {

                    createModel({
                        group_name: 'test',
                        phrasesIds: phrasesIds,  // текущие
                        new_phrases: ['test'], // новые
                        isBidable: isBidable
                    });
                    createPhraseModels(isBidable ? 'm-phrase-bidable' : 'm-phrase-text');


                    var modelNames = mGroup.getPhrasesModels().map(function(model) {
                        return model.name;
                    });

                    isBidable ?
                        expect(modelNames).to.be.eql(['m-phrase-bidable', 'm-phrase-bidable', 'm-phrase-bidable']) :
                        expect(modelNames).to.be.eql(['m-phrase-text', 'm-phrase-text', 'm-phrase-text']);

                    mPhrases.forEach(function(model) { model.destruct(); });
                    destroyModel();
                });
            });

            describe('isBidable = false', function() {

                before(function() {
                    createModel({
                        group_name: 'test',
                        phrasesIds: phrasesIds,  // текущие
                        new_phrases: ['test'], // новые
                        isBidable: false
                    });
                    createPhraseModels('m-phrase-text');
                });

                after(function() {
                    mPhrases.forEach(function(model) { model.destruct(); });
                    destroyModel();
                });

                it('getPhrasesLength. Должен вернуть общую длину фраз', function() {
                    expect(mGroup.getPhrasesLength()).to.be.eq(phrasesIds.length * 5 + phrasesIds.length * 2 - 1 );
                });

                it('getAllPhrases. Должен вернуть и новые и текущие фразы (4 шт)', function() {
                    expect(mGroup.getAllPhrases().length).to.be.eq(4);
                });
            });

        });

        describe('Работа с кампанией', function() {

            afterEach(function() {
                destroyModel();
            });

            it('getCampaignModel', function() {
                createModel();

                expect(mGroup.getCampaignModel()).to.have.property('name', 'dm-cpm-banner-campaign');
            });

            describe('Если задана', function() {
                var campModel;

                before(function() {
                    campModel = BEM.MODEL.create({ name:'dm-cpm-banner-campaign', id: '666' });
                    createModel({ cid: '666' });
                });

                after(function() {
                    campModel.destruct();
                });

                it('getCampaignModel вернет кампанию c заданным id, как cid у группы', function() {
                    expect(mGroup.getCampaignModel()).to.have.property('id', '666');
                });

            });

            it('getKeywordLimit', function() {
                createModel();

                expect(mGroup.getKeywordLimit()).to.be.eq(2);
            });

        });

        describe('isLastActive', function() {
            var checkedPhraseModel,
                checkedRetargetingModel;

            function createCheckedPhraseModel(data) {
                 return BEM.MODEL.create({
                     name: 'm-phrase-text',
                     id: 12,
                     parentModel: mGroup
                 }, data || {});
            }

            function createCheckedRetargetingModel(data) {
                 return BEM.MODEL.create({
                     name: 'm-retargeting-bidable',
                     id: 12,
                     parentModel: mGroup
                 }, data || {});
            }

            afterEach(function() {
                destroyModel();
                (checkedPhraseModel || {}).destruct && checkedPhraseModel.destruct();
                (checkedRetargetingModel || {}).destruct && checkedRetargetingModel.destruct();
            });

            describe('Вернет true, в случае если', function() {

                it('передана последня активная фраза', function() {
                    createModel({ group_name: 'test' });
                    checkedPhraseModel = createCheckedPhraseModel({
                        state: 'active',
                        is_suspended: false,
                        key_words: 'фраза',
                        modelId: 'someModelId'
                    });

                    expect(mGroup.isLastActive(checkedPhraseModel)).to.be.true;
                });

                it('передан последий активный ретаргетинг', function() {
                    createModel({ group_name: 'test' });
                    checkedRetargetingModel = createCheckedRetargetingModel({
                        state: 'active',
                        is_suspended: false,
                        is_deleted: false,
                        modelId: 'someModelId'
                    });

                    expect(mGroup.isLastActive(checkedRetargetingModel)).to.be.true;
                });

                describe('есть еще один ретаргетинг', function() {
                    var mRetargeting;

                    function createRetargetingModel(data) {
                        mRetargeting = BEM.MODEL.create({
                            name: 'm-retargeting-bidable',
                            id: 114,
                            parentModel: mGroup
                        }, data || {});
                    }

                    beforeEach(function() {
                        createModel({ group_name: 'test', retargetingsIds:[114] });
                        checkedRetargetingModel = createCheckedRetargetingModel({
                            state: 'active',
                            is_suspended: false,
                            is_deleted: false,
                            modelId: 'someModelId'
                        });
                    });

                    afterEach(function() {
                        mRetargeting.destruct && mRetargeting.destruct();
                    });

                    it('но отключенный', function() {
                        createRetargetingModel({
                            state: 'active',
                            is_suspended: true,
                            is_deleted: false,
                            modelId: 'someAnotherModelId'
                        });

                        expect(mGroup.isLastActive(checkedRetargetingModel)).to.be.true;
                    });

                    it('но удаленный', function() {
                        createRetargetingModel({
                            state: 'active',
                            is_suspended: false,
                            is_deleted: true,
                            modelId: 'someAnotherModelId'
                        });

                        expect(mGroup.isLastActive(checkedRetargetingModel)).to.be.true;
                    });

                });

                describe('есть еще одна модель фразы', function() {
                    var mPhrase;

                    function createPhraseModel(data) {
                        mPhrase = BEM.MODEL.create({
                            name: 'm-phrase-text',
                            id: 14,
                            parentModel: mGroup
                        }, data || {});
                    }

                    beforeEach(function() {
                        createModel({ group_name: 'test', phrasesIds:[14] });
                        checkedPhraseModel = createCheckedPhraseModel({
                            state: 'active',
                            is_suspended: false,
                            key_words: 'фраза',
                            modelId: 'someModelId'
                        });
                    });

                    afterEach(function() {
                        mPhrase.destruct && mPhrase.destruct();
                    });

                    it('но отклоненная, то проверяемая фраза считается последней активной', function() {
                        createPhraseModel({
                            state: 'declined',
                            is_suspended: false,
                            is_deleted: false,
                            modelId: 'someAnotherModelId'
                        });

                        expect(mGroup.isLastActive(checkedPhraseModel)).to.be.true;
                    });

                    it('но с малым ctr, то проверяемая фраза считается последней активной', function() {
                        createPhraseModel({
                            state: 'low_ctr',
                            is_suspended: false,
                            is_deleted: false,
                            modelId: 'someAnotherModelId'
                        });

                        expect(mGroup.isLastActive(checkedPhraseModel)).to.be.true;
                    });

                    it('но выключенная, то проверяемая фраза считается последней активной', function() {
                        createPhraseModel({
                            state: 'context',
                            is_suspended: true,
                            is_deleted: false,
                            modelId: 'someAnotherModelId'
                        });

                        expect(mGroup.isLastActive(checkedPhraseModel)).to.be.true;
                    });

                    it('но удаленная, то проверяемая фраза считается последней активной', function() {
                        createPhraseModel({
                            state: 'context',
                            is_suspended: false,
                            is_deleted: true,
                            modelId: 'someAnotherModelId'
                        });

                        expect(mGroup.isLastActive(checkedPhraseModel)).to.be.true;
                    });
                });

            });

            describe('Вернет false, в случае если хотябы', function() {

                it('Есть хоть одна новая фраза', function() {
                    createModel({ group_name: 'test', new_phrases: ['test']});
                    checkedPhraseModel = createCheckedPhraseModel({
                        state: 'active',
                        is_suspended: false,
                        key_words: 'фраза',
                        modelId: 'someModelId'
                    });

                    expect(mGroup.isLastActive(checkedPhraseModel)).to.be.false;
                });

                it('У модели is_suspended == true', function() {
                    createModel();
                    checkedPhraseModel = createCheckedPhraseModel({
                        state: 'active',
                        is_suspended: true,
                        key_words: 'фраза',
                        modelId: 'someModelId'
                    });

                    expect(mGroup.isLastActive(checkedPhraseModel)).to.be.false;
                });

                it('У модели state != active, context, new', function() {
                    createModel();
                    checkedPhraseModel = createCheckedPhraseModel({
                        state: 'declined',
                        is_suspended: false,
                        key_words: 'фраза',
                        modelId: 'someModelId'
                    });

                    expect(mGroup.isLastActive(checkedPhraseModel)).to.be.false;
                });

                describe('Есть другая активная фраза', function() {
                    var mAnotherPhrase;

                    before(function() {
                        createModel({ group_name: 'test', phrasesIds:[14] });
                        checkedPhraseModel = createCheckedPhraseModel({
                            state: 'active',
                            is_suspended: false,
                            key_words: 'фраза',
                            modelId: 'someModelId'
                        });
                        mAnotherPhrase = BEM.MODEL.create({
                            name: 'm-phrase-text',
                            id: 14,
                            parentModel: mGroup
                        }, {
                            state: 'active',
                            is_suspended: false,
                            is_deleted: false,
                            modelId: 'someAnotherModelId'
                        });
                    });

                    after(function() {
                        mAnotherPhrase.destruct();
                    });

                    it('Вернет false', function() {
                        expect(mGroup.isLastActive(checkedPhraseModel)).to.be.false;
                    });
                });
            });

        });

        describe('getMultipliersData', function() {
            var testModel;

            before(function() {
                createModel();
                testModel = BEM.MODEL.create({ name: 'm-adjustment-rates', id: 1 });
            });

            after(function() {
                destroyModel();
                testModel.destruct();
            });

            it('Должен вернуть объект данных модели m-adjustment-rates', function() {
                expect(Object.keys(mGroup.getMultipliersData())).to.be.deep.eq(Object.keys(testModel.provideData()));
            });
        });

        describe('getGeoModel', function() {

            before(function() {
                createModel();
            });

            after(function() {
                destroyModel();
            });

            it('Должен вернуть модель m-geo-regions', function() {
                expect(mGroup.getGeoModel().name).to.be.eq('m-geo-regions');
            });

        });

        describe('addBanner', function() {

            before(function() {
                createModel();
            });

            after(function() {
                destroyModel();
            });

            it('Если в группе баннеров нет, должен вернуть модель с id = new1', function() {
                expect(mGroup.addBanner()).to.have.property('id', 'new1');
            });

            it('Если в группе баннер есть, должен вернуть модель с id = new2', function() {
                expect(mGroup.addBanner()).to.have.property('id', 'new2');
            });

        });

        describe('provideData', function() {

            before(function() {
                createModel();
            });

            after(function() {
                destroyModel();
            });

            it('Должен вернуть объект', function() {
                expect(mGroup.provideData()).to.be.an('object');
            });

            ['modelId', 'group_name', 'banners_quantity', 'banners_arch_quantity', 'group_banners_types', 'adgroup_id',
                'adgroup_type', 'banners', 'minus_words', 'statusModerate', 'statusPostModerate', 'statusMetricaStop',
                'edit_banners_quantity', 'tags', 'shownBids', 'phrases', 'showNewBanners', 'day_budget',
                'autobudget', 'status', 'statusActive', 'statusShow', 'isBannersEditable', 'is_camp_archived',
                'newGroupIndex', 'isNewGroup', 'geo'].forEach(function(field) {
                it('Должен вернуть объект с полем ' + field, function() {
                    expect(Object.keys(mGroup.provideData()).indexOf(field)).not.to.equal(-1);
                });
            });
        });

        describe('dataToModelData', function() {
            var sandbox;

            beforeEach(function() {
                sandbox = sinon.sandbox.create();
                createModel();
            });

            afterEach(function() {
                sandbox.restore();
                destroyModel();
            });

            it('Должен вызвать метод u[dm-cpm-banner-group].transformData', function() {
                var spyTransformData = sandbox.spy(u['dm-cpm-banner-group'], 'transformData'),
                    data = { group: { group_name: 'test' } };

                mGroup.dataToModelData(data);

                expect(spyTransformData.getCall(0).args[0]).to.have.property('group');
            });

        });
    });

    describe('utils', function() {

        describe('transformData', function() {

            it('Должен вернуть объект', function() {
                expect(u['dm-cpm-banner-group'].transformData({ group: {} })).to.be.an('object');
            });

            it('Если имя группы не передано, должно подставится "Новая группа объявлений"', function() {
                expect(u['dm-cpm-banner-group'].transformData({ group: {} })).
                    to.have.property('group_name', 'Новая группа объявлений');
            });

            it('Если теги заданы массивом, то возвращается undefined', function() {
                var tags = [
                    {
                        id: 'id1',
                        value: 'test1'
                    },
                    {
                        id: 'id2',
                        value: '321312'
                    }
                ];

                expect(u['dm-cpm-banner-group'].transformData({ group: { tags: tags } }).tags).to.be.eqls(undefined);
            });

            it('Если теги заданы объектом, то должен сформироваться массив с данными из campaignTags', function() {
                var campaignTags = [
                        {
                            id: 'id1',
                            value: 'test1'
                        }
                    ],
                    transformedTags = u['dm-cpm-banner-group'].transformData(
                        {
                            group: { tags: { id1: '', id2: '' } },
                            campaignTags: campaignTags
                        }
                    ).tags;

                expect(transformedTags).to.be.eqls([ campaignTags[0], { id: 'id2', value: undefined }]);
            });


            it('Прокидывается поле displayConditions', function() {
                var transformed = u['dm-cpm-banner-group'].transformData(
                    {
                        group: {
                            displayConditions: 'crypta'
                        }
                    }
                );

                expect(transformed).to.have.property('displayConditions');
            });

        });

        describe('transformBannerData', function() {
            var sandbox;

            beforeEach(function() {
                sandbox = sinon.sandbox.create();
            });

            afterEach(function() {
                sandbox.restore();
            });

            it('Должен вызвать метод bannerData', function() {
                var spyBannerData = sandbox.spy(u, 'bannerData');

                u['dm-cpm-banner-group'].transformBannerData({ banner: {
                    bid: '1'
                }, group: {} });

                // значение параметра не проверяем, так как он модифицируется
                // внутри функции
                expect(spyBannerData.getCall(0).args[0]).to.have.property('banner');
            });

            it('Должен вызвать метод bannerModelData', function() {
                var spyBannerModelData = sandbox.spy(u, 'bannerModelData');

                u['dm-cpm-banner-group'].transformBannerData({ banner: {}, group: {} });

                expect(Object.keys(spyBannerModelData.getCall(0).args[0]))
                        .to.be.eqls(['banner', 'group', 'loadVCardFromClient']);
            });

            it('Если у баннера отсутствует url_protocol, он должен там появиться', function() {
                var data = u['dm-cpm-banner-group'].transformBannerData({ banner: {
                    href: 'http://ya.ru'
                }, group: {} });

                expect(data.url_protocol).to.be.eq('http://');
            });
        });
    });
});
