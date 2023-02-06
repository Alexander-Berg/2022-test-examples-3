describe('b-group-edit-phrase', function() {
    var bemBlock,
        blockTree,
        phraseDefaultData = {
            id: 2,
            modelId: 2,
            phrase: 'text',
            minus_words: 'minus-words',
            state: 'active',
            key_words: 'key-words'
        },
        phraseModel,
        groupModel;

    function createBlock() {
        groupModel = BEM.MODEL.create({
            name: 'm-group',
            id: 'group'
        }, {
            phrasesIds: [1, 5, 3, 2]
        });

        phraseModel = BEM.MODEL.create({
            name: 'm-phrase-text',
            id: phraseDefaultData.modelId,
            parentModel: groupModel
        }, phraseDefaultData);

        blockTree = $(BEMHTML.apply({
            block: 'b-group-edit-phrase',
            modelParams: {
                id: phraseDefaultData.modelId,
                name: 'm-phrase-text'
            },
            adgroupId: 'group',
            type: 'active',
            phrase: phraseDefaultData
        }));

        $('body').append(blockTree);

        bemBlock = BEM.DOM.init(blockTree).bem('b-group-edit-phrase');

        sinon.stub(bemBlock, 'destruct');
    }

    before(function() {
        u.stubDMParams();
    });

    after(function() {
        u.restoreConsts();
    });

    afterEach(function() {
        bemBlock.destruct.restore();

        bemBlock && bemBlock.destruct();
        groupModel && groupModel.destruct();
    });

    beforeEach(function() {
        createBlock();
    });

    it('При клике на "удалить" фраза должна удаляться из модели группы', function() {
        bemBlock.elem('remove').click();

        expect(groupModel.get('phrasesIds')).not.to.include(2);
    });

    it('При клике на "удалить" должен вызваться destruct для фразы', function() {
        bemBlock.elem('remove').click();

        expect(bemBlock.destruct.called).to.be.equal(true);
    });
});
