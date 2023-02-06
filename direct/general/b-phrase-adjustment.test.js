describe('b-phrase-adjustment', function() {
    var block,
        clock,
        campModel,
        groupModel,
        phraseModel;

    function createBlock() {
        var blockTree = $(BEMHTML.apply({ block: 'b-phrase-adjustment' }));

        $('<div/>').append(blockTree);

        block = BEM.DOM.init(blockTree).bem('b-phrase-adjustment');
    }

    function initBlock() {
        block._setDebouncedFunctions();
        block.attach({
            editPhraseModel: phraseModel,
            bannersGroupModel: groupModel
        });
    }

    before(function() {
        u.stubDMParams();
    });

    after(function() {
        u.restoreConsts();
    });

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        campModel = BEM.MODEL.create({ name: 'm-campaign', id: 'campaign' });
        groupModel = BEM.MODEL.create({ name: 'm-group', id: 'group-1' }, { cid: 'campaign' });
        phraseModel = BEM.MODEL.create(
            { name: 'm-phrase-bidable', id: 'phrase-1', parentModel: groupModel },
            { modelId: 'phrase-1', minus_words: ['word1', 'word2', 'word2'], key_words: 'test' });
        this.server = sinon.fakeServer.create();
    });

    afterEach(function() {
        block.destruct();
        clock.restore();
        campModel.destruct();
        groupModel.destruct();
        phraseModel.destruct();
        this.server.restore();
    });

    it('При инициализации блока должны вызваться три ajax-запросов том числе - ajaxRefineMinusWords', function() {
        createBlock();
        sinon.stub(block._requestRefineMinusWords, 'get');
        initBlock();

        clock.tick(501);

        expect(block._requestRefineMinusWords.get.args[0][0].cmd).to.be.equal('ajaxRefineMinusWords');

        block._requestRefineMinusWords.get.restore();
    });

    it('При инициализации блока должны вызваться три ajax-запроса в том числе - ajaxPhraseStat', function() {
        createBlock();
        sinon.stub(block._requestPhraseStat, 'get');
        initBlock();

        clock.tick(501);
        expect(block._requestPhraseStat.get.args[0][0].cmd).to.be.equal('ajaxPhraseStat');

        block._requestPhraseStat.get.restore();
    });

    it('При инициализации блока должны вызваться три ajax-запросов том числе - ajaxNormWords', function() {
        createBlock();
        sinon.stub(block._requestNormWords, 'get');
        initBlock();

        clock.tick(501);
        expect(block._requestNormWords.get.args[0][0].cmd).to.be.equal('ajaxNormWords');

        block._requestNormWords.get.restore();
    });


    describe('', function() {
        it('При вызове ajaxPhraseStat должен выставляться статус forecast-loading: yes', function() {
            createBlock();

            sinon.stub(block, '_loadMinusWords');
            sinon.stub(block, '_loadNormForms');
            initBlock();

            expect(block.getMod('forecast-loading')).to.be.equal('yes');

            block._loadMinusWords.restore();
            block._loadNormForms.restore();

        });

        it('Если ajaxPhraseStat вернул hits: 100, это значение должно быть записано в элемент forecast-num', function() {
            createBlock();

            sinon.stub(block._requestPhraseStat, 'get');
            sinon.stub(block, '_loadMinusWords');
            sinon.stub(block, '_loadNormForms');
            initBlock();
            clock.tick(501);
            block._requestPhraseStat.get.args[0][1].apply(block, [100]);

            expect(block.elem('forecast-num').text()).to.be.equal('100');
            block._requestPhraseStat.get.restore();

            block._loadMinusWords.restore();
            block._loadNormForms.restore();
        });

        it('При вызове ajaxRefineMinusWords должен выставляться статус first-open: yes', function() {
            createBlock();

            sinon.stub(block, '_loadForecast');
            sinon.stub(block, '_loadNormForms');
            initBlock();
            clock.tick(501);
            expect(block.getMod('first-open')).to.be.equal('yes');

            block._loadForecast.restore();
            block._loadNormForms.restore();
        });

        it('Если вызов ajaxRefineMinusWords вернул 2 элемента, должно отрисоваться группа из 2 чекбоксов', function() {
            createBlock();

            sinon.stub(block._requestRefineMinusWords, 'get');
            sinon.stub(block, '_loadForecast');
            sinon.stub(block, '_loadNormForms');
            initBlock();
            clock.tick(501);
            block._requestRefineMinusWords.get.args[0][1].apply(block, [{ words: [{ word: '-bla1', phrases: [] }, { word: '-bla2', phrases: [] }] }]);
            expect(block.findBlockInside('b-checkboxes-group').getCheckboxes().length).to.be.equal(2);

            block._requestRefineMinusWords.get.restore();
            block._loadForecast.restore();
            block._loadNormForms.restore();

        });

        it('Если вызов ajaxRefineMinusWords вернул 2 элемента и в одном из них существующее минус слово, то его чекбокс должен быть активен', function() {
            createBlock();
            sinon.stub(block, '_loadForecast');
            sinon.stub(block, '_loadNormForms');
            sinon.stub(block._requestRefineMinusWords, 'get');
            initBlock();
            clock.tick(501);
            block._requestRefineMinusWords.get.args[0][1].apply(block, [{ words: [{ word: 'bla', phrases: [] }, { word: 'word1', phrases: [] }] }]);
            expect(block.findBlockInside('b-checkboxes-group').getCheckboxes()[1].getMod('checked')).to.be.equal('yes');

            block._requestRefineMinusWords.get.restore();
            block._loadForecast.restore();
            block._loadNormForms.restore();

        });
    })
});
