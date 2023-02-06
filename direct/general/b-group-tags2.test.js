describe('b-group-tags2', function() {
    var block;

    function createBlock(options) {
        options || (options = {
            tags: []
        });

        block = u.getInitedBlock({
            block: 'b-group-tags2',
            tags: options.tags
        });
    }

    afterEach(function() {
        block.destruct && block.destruct();
    })

    describe('Блок', function() {

        it('имеет переключатель', function() {
            createBlock();

            expect(block).to.haveElem('switcher');
        });

        it('имеет элемент со списком тегов', function() {
            createBlock();

            expect(block).to.haveElem('tags-list');
        });

    });

    describe('Взаимодействие', function() {

        it('при нажатии на переключатель генерирует событие toggleRequested', function() {
            createBlock();

            expect(block).to.triggerEvent('toggleRequested', function() {
                block.findBlockOn('switcher', 'button').trigger('click');
            });
        });

    });

    describe('Методы i-bem', function() {

        describe('val()', function() {

            it('без аргументов возвращает значение', function() {
                createBlock({ tags: ['колбаска'] });

                expect(block.val()).to.be.deep.equal(['колбаска']);
            });

            it('c аргументом выставляет значение', function() {
                createBlock({ tags: ['колбаска'] });
                block.val(['шпикачка']);
                expect(block.val()).to.be.deep.equal(['шпикачка']);
            });

        });

    })

    describe('utils', function() {

        describe('makeTagsArray', function() {
            var id = { id: 1 },
                tagId = { tag_id: 2 };

            it('должен вернуть пустой массив если пустой второй аргумент', function() {
                expect(u.groupTags2.makeTagsArray([ id, tagId ], {})).to.be.deep.equal([]);
            });
            it('должен вернуть пустой массив если пустой первый аргумент', function() {
                expect(u.groupTags2.makeTagsArray([], { 1:1 })).to.be.deep.equal([]);
            });

            it('должен вернуть массив с { id: 1 }', function() {
                expect(u.groupTags2.makeTagsArray([ id, tagId ], { 1:1 })).to.be.deep.equal([id]);
            });

            it('должен вернуть массив с { tag_id: 1 }', function() {
                expect(u.groupTags2.makeTagsArray([ id, tagId ], { 2:1 })).to.be.deep.equal([tagId]);
            });

        });

    })

});
