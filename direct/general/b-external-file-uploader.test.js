describe('b-external-file-uploader', function() {
    var ctx = {
            block: 'b-external-file-uploader',
            title: 'Тест',
            placeholder: 'Тест',
            autoFocus: false
        },
        block,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        block = u.createBlock(ctx);
    });

    afterEach(function() {
        // должны отработать afterCurrentEvent
        sandbox.clock.tick(1);
        block.destruct();
        sandbox.restore();
    });


    describe('Валидация формы', function() {
        it('При введении правильной ссылки и вызове save триггерится событие сохранения', function() {

            block.setValue('http://ya.ru');

            expect(block).to.triggerEvent('save', function() { block.save(); });
        });

        it('При невведенной ссылке появляется ошибка "Укажите ссылку"', function() {
            block.setValue('');
            block.save();

            expect(block.elem('error-message').text()).to.equal('Укажите ссылку');
        });

        it('При введении  не-ссылки появляется ошибка "Указана некорректная ссылка"', function() {
            block.setValue('blabla');
            block.save();

            expect(block.elem('error-message').text()).to.equal('Указана некорректная ссылка');
        })
    });

    it('Функция resetForm очищает инпут и ошибки', function() {
        block.resetForm();

        expect(block._inputControl.val()).to.equal('');
        expect(block.elem('error-message').text()).to.equal('');
    });

    it('Функция showErrors показывает ошибки', function() {
        block.showErrors('тестовое сообщение');

        expect(block.elem('error-message').text()).to.equal('тестовое сообщение');
    });
});
