describe('b-user-dialog', function() {

    var sandbox = sinon.sandbox.create({ useFakeTimers: false });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Окно подтверждения (confirm):', function() {
        var block;

        beforeEach(function() {
            block = BEM.DOM.blocks['b-user-dialog'].confirm('Точно? Может подумаешь еще?');
        });

        afterEach(function() {
            block.destruct();
        });

        it('В попапе есть кнопка Подтверждения', function() {
            expect(block.elem('confirm').length).to.be.equal(1);
        });
    });

    describe('Окно нотификации (alert)', function() {
        var block;

        beforeEach(function() {
            block = BEM.DOM.blocks['b-user-dialog'].alert('Привет!');
        });

        afterEach(function() {
            block.destruct();
        });

        it('В попапе отсутсвует кнопка Подтверждения', function() {
            expect(block.elem('confirm').length).to.be.equal(0);
        });
    });

    describe('Кнопка Подтверждения', function() {
        var callback;

        beforeEach(function() {
            callback = sandbox.spy();

            block = BEM.DOM.blocks['b-user-dialog'].confirm({
                message: 'Точно? Может подумаешь еще?',
                confrimButtonText: 'Ага',
                onConfirm: callback
            });
        });

        afterEach(function() {
            block.destruct();
        });

        it('При указании confrimButtonText в параметрах меняет текст кнопки на кастомный', function() {
            expect(block._confirmButton.elem('text').text()).to.be.equal('Ага');
        });

        it('Выполняет callback onConfirm (если он указан в параметрах) при клике на нее', function() {
            block._confirmButton.trigger('click');

            expect(callback.called);
        });

        it('Закрывает диалоговое окно', function() {
            block._confirmButton.trigger('click');

            expect(block._getPopup().isShown()).to.be.false;
        });
    });

    describe('Кнопка Отмены', function() {
        var callback;

        beforeEach(function() {
            callback = sandbox.spy();

            block = BEM.DOM.blocks['b-user-dialog'].alert({
                message: 'Привет!',
                cancelButtonText: 'Привет',
                onCancel: callback
            });
        });

        afterEach(function() {
            block.destruct();
        });

        it('При указании cancelButtonText в параметрах меняет текст кнопки на кастомный', function() {
            expect(block._cancelButton.elem('text').text()).to.be.equal('Привет');
        });

        it('Выполняет callback onCancel (если он указан в параметрах) при клике на нее', function() {
            block._cancelButton.trigger('click');

            expect(callback.called);
        });

        it('Закрывается при клике на кнопку Отмены', function() {
            block._cancelButton.trigger('click');

            expect(block._getPopup().isShown()).to.be.false;
        });
    });
});
