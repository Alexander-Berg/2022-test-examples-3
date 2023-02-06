describe('b-retargeting-condition-edit-name', function() {
    var block, sandbox;

    /**
     * Создание блока, который тестируется
     * @param {{ name, comment }} [params]
     */
    function createBlock(params) {
        return u.createBlock(u._.assign({ block: 'b-retargeting-condition-edit-name' }, params));
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('getData возвращает название и комментарий из соответствующих полей', function() {
        block = createBlock({
            name: 'название',
            comment: 'комментарий'
        });

        expect(block.getData()).to.deep.equal({
            name: 'название',
            comment: 'комментарий'
        });
    });

    it('при создании без комментария, на блоке установлен модификатор comment-hide', function() {
        block = createBlock({
            name: 'название',
            comment: ''
        });
        expect(block).to.haveMod('comment-hide');
    });

    it('при создании с комментарием, на блоке НЕ установлен модификатор comment-hide', function() {
        block = createBlock({
            name: 'название',
            comment: 'комментарий'
        });
        block.addComment();
        sandbox.clock.tick(1);
        expect(block).to.not.haveMod('comment-hide');
    });

    it(' при добавление комментария, удаляется модификатор comment-hide', function() {
        block = createBlock({
            name: 'название',
            comment: ''
        });
        block.addComment();
        sandbox.clock.tick(1);
        expect(block).to.not.haveMod('comment-hide');
    });

    it('при удалении комментария, добавляется модификатор comment-hide: yes', function() {
        block = createBlock({
            name: 'название',
            comment: 'комментарий'
        });
        block.removeComment();
        sandbox.clock.tick(1);
        expect(block).to.haveMod('comment-hide', 'yes');
    });
});
