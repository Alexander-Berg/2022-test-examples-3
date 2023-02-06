describe('b-edit-groups-mass-actions - массовое редактирование', function() {

    var block,
        clock,
        blockName = 'b-edit-groups-mass-actions',
        phraseModelName = 'm-phrase-text',
        groupModelName = 'm-group',
        cid = 1,
        sandbox,
        constStub,
        // используется в mode('has-banner-with-own-site')
        // если не инициализировано, то падает NPE
        groupsDataFromServerStub = [{ banners: [] }];

    function createBlock() {
        block = u.createBlock({
            block: blockName,
            mods: {
                type: 'text',
                single: 'no',
                visible: 'yes'
            },
            canEditDomain: true,
            clientId: 1,
            imageAdsGallery: [],
            isSingleGroup: false,
            isTurbolandingsEnabled: false,
            hasNoClientRole: true,
            cid: cid,
            adgroupIds: ['group-id-0', 'group-id-1'],
            modelName: groupModelName,
            mediaType: 'text',
            groups: groupsDataFromServerStub
        });
        // открывает вкладку с минус-словами
        block._openAction('minus-words');

        block.trigger('popup-shown');
    }

    function createGroupAndPhraseModels() {
        var phrases = [
            {
                modelParams: {
                    name: phraseModelName,
                    id: 'phrase-id-0',
                    parentId: 'group-id-0',
                    parentName: groupModelName
                },
                data: {}
            },
            {
                modelParams: {
                    name: phraseModelName,
                    id: 'phrase-id-1',
                    parentId: 'group-id-1',
                    parentName: groupModelName
                },
                data: {}
            }],
            groups = [
                {
                    modelParams: { name: groupModelName, id: 'group-id-0' },
                    data: { phrasesIds: ['phrase-id-0'] }
                },
                {
                    modelParams: { name: groupModelName, id: 'group-id-1' },
                    data: { phrasesIds: ['phrase-id-1'] }
                }
            ];

        phrases.forEach(function(p) {
            BEM.MODEL.create(p.modelParams, p.data);
        });

        groups.forEach(function(g) {
            var model = BEM.MODEL.create(g.modelParams, g.data);
            model.getPhrasesModels();
        });
    }

    function resetPhraseModelsData() {
        var models = BEM.MODEL.get('m-phrase-text'),
            phrases = [
                {
                    key_words: 'Купить холодильник +Отечественный',
                    minus_words: ['БУ', 'черный']
                },
                {
                    key_words: 'Новости +Культура',
                    minus_words: ['СПБ', 'Москва']
                }
            ];

        models.forEach(function(model, i) {
            if (phrases[i]) {
                var phrase = phrases[i];
                Object.keys(phrase).forEach(function(key) {
                    model.set(key, phrase[key]);
                })
            }
        });
    }

    function destroyGroupModels() {
        var models = []
            .concat(BEM.MODEL.get(groupModelName))
            .concat(BEM.MODEL.get(phraseModelName));
        models.forEach(function(m){
            m.destruct();
        });
    }

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        sandbox = sinon.sandbox.create();
        constStub = sandbox.stub(u, 'consts');
        constStub.withArgs('rights').returns({});

        createGroupAndPhraseModels();
        resetPhraseModelsData();
        createBlock();
    });

    afterEach(function() {
        sandbox.restore();

        destroyGroupModels();
        block.destruct();
        clock.restore();
    });

    describe('Добавить минус-слова', function() {

        beforeEach(function() {
            clock = sinon.useFakeTimers();
            resetPhraseModelsData();
        });

        afterEach(function() {
            clock.restore();
        });

        it('Добавить минус-слово к фразам каждой группы', function() {
            block._getInput('add-minus-words').val('-минус_слово');
            block._addMinusWords();

            clock.tick(500);

            var models = BEM.MODEL.get(phraseModelName);

            models.forEach(function(m) {
                expect(m.get('minus_words')).to.include('минус_слово');
            });
        });

        it('Еще раз добавить уже записанное минус-слово к фразам группы. Слово дублироваться не должно', function() {
            block._getInput('add-minus-words').val('-минус_слово');
            block._addMinusWords();

            block._getInput('add-minus-words').val('-минус_слово');
            block._addMinusWords();

            clock.tick(500);

            var models = BEM.MODEL.get(phraseModelName);

            models.forEach(function(m) {
                expect(m.get('minus_words').filter(function(word) { return word == 'минус_слово'})).to.have.lengthOf(1);
            });
        });

        it('Добавить еще раз к фразам группы уже записанное минус-слово в качестве плюс-слова. ' +
        'Слово должно задублироваться c плюсом как частью слова', function() {
            block._getInput('add-minus-words').val('-минус_слово');
            block._addMinusWords();

            block._getInput('add-minus-words').val('+минус_слово');
            block._addMinusWords();

            clock.tick(500);

            var models = BEM.MODEL.get(phraseModelName);

            models.forEach(function(m) {
                expect(m.get('minus_words')).to.include('минус_слово');
                expect(m.get('minus_words')).to.include('+минус_слово');
            });
        });
    });
});
