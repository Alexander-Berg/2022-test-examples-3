describe('b-moderation-doc-informer', function() {
    var server,
        clock,
        mockupData = [
            'med_services',
            'med_equipment',
            'pharmacy',
            'acids',
            'psychology',
            'forex_broker'
        ];

    before(function() {
        u.stubDMParams();
    });

    beforeEach(function() {
        server = sinon.fakeServer.create();
        clock = sinon.useFakeTimers();

        sinon.stub(u, 'getUrl').callsFake(function() { return 'yandex.ru'; });
    });

    afterEach(function() {
        u.getUrl.restore();
    });

    after(function() {
        u.restoreConsts();
    });

    describe('Без данных в шаблоне и без указания id кампании', function() {
        var block;

        beforeEach(function() {
            block = createBlock({});
        });

        afterEach(function() {
            block.destruct();
        });

        it('для b-moderation-doc-informer выставлен _hidden_yes', function() {
            expect(block.hasMod('hidden', 'yes')).to.be.equal(true);
        });

        it('__thematics не содержит блоки b-list', function() {
            expect(block.findBlocksInside('thematics', 'b-list')).to.be.empty;
        });

    });

    describe('Состояние с данными в шаблоне', function() {
        var block,
            defaultColumnsCount = 3,
            thematicsCount = Object.keys(mockupData).length;

        beforeEach(function() {
            block = createBlock({ moderationDocs: mockupData });
        });

        afterEach(function() {
            block.destruct();
        });

        describe('для b-moderation-doc-informer', function() {

            it('не выставлен _hidden_yes', function() {
                expect(block.hasMod('hidden', 'yes')).to.be.equal(false);
            });

        });

        describe('__thematics содержит блоки b-list', function() {
            it('в количестве по умолчанию: ' + defaultColumnsCount, function() {
                expect(block.findBlocksInside('thematics', 'b-list').length).to.equal(defaultColumnsCount);
            });

            it('с общим количеством элементов: ' + thematicsCount, function() {
                var count = block.findBlocksInside('thematics', 'b-list').reduce(function(count, list) {
                    return count + list.findElem('item').length;
                }, 0);

                expect(count).to.equal(thematicsCount);
            });
        });
    });

    describe('В информере в две колонки с данными в шаблоне', function() {
        var block,
            columnsCount = 2,
            thematicsCount = Object.keys(mockupData).length;

        beforeEach(function() {
            block = createBlock({ columnsCount: columnsCount, moderationDocs: mockupData });
        });

        afterEach(function() {
            block.destruct();
        });

        describe('для b-moderation-doc-informer', function() {

            it('не выставлен _hidden_yes', function() {
                expect(block.hasMod('hidden', 'yes')).to.be.equal(false);
            });

        });

        describe('__thematics содержит блоки b-list', function() {
            it('в количестве: ' + columnsCount, function() {
                expect(block.findBlocksInside('thematics', 'b-list').length).to.be.equal(columnsCount);
            });

            it('с общим количеством элементов: ' + thematicsCount, function() {
                var count = block.findBlocksInside('thematics', 'b-list').reduce(function(count, list) {
                    return count + list.findElem('item').length;
                }, 0);

                expect(count).to.be.equal(thematicsCount);
            });
        });

    });

    /**
     * Создание блока
     * @param ctxData
     */
    function createBlock(ctxData) {
        return BEM.DOM
            .init(u
                .getDOMTree(createBEMJSON(ctxData))
                .appendTo(document.body)
                .css({ visibility: 'hidden', position: 'absolute', top: 0, left: -65555 }))
            .bem('b-moderation-doc-informer');
    }

    /**
     * Создание типизированного BEMJSON
     * @returns {Object}
     */
    function createBEMJSON(ctx) {
        return {
            block: 'b-moderation-doc-informer',
            mods: ctx.mods,
            columnsCount: ctx.columnsCount,
            campaign: ctx.campaign,
            moderationDocs: ctx.moderationDocs
        };
    }

});
