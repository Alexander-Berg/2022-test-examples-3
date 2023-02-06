describe('b-external-file-uploader_type_modal', function() {
    var ctx = {
            block: 'b-external-file-uploader',
            mods: { type: 'modal' },
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

    describe('Стейт кнопки', function() {
        it ('Изначально кнпока задизейблена', function() {
            expect(block._submitButton).to.haveMod('disabled', 'yes');
        });

        it('При непустом инпуте кнопка энейблится', function() {
            block.setValue('http://ya.ru');

            expect(block._submitButton).not.to.haveMod('disabled');
        });

        it('При пустом инпуте кнопка дизейблится', function() {
            block.setValue('');

            expect(block._submitButton).to.haveMod('disabled', 'yes');
        });
    });

    describe('Валидация формы', function() {
        it('При введении правильной ссылки триггерится событие сохранения', function() {
            block.setValue('http://ya.ru');


            expect(block).to.triggerEvent('save', function() { block._submitButton.trigger('click'); });
        });

        it('При невведенной ссылке появляется ошибка "Укажите ссылку"', function() {
            block.setValue('');
            block._submitButton.trigger('click');

            expect(block.elem('error-message').text()).to.equal('Укажите ссылку');
        });

        it('При введении  не-ссылки появляется ошибка "Указана некорректная ссылка"', function() {
            block._inputControl.val('blabla');
            block._submitButton.trigger('click');

            expect(block.elem('error-message').text()).to.equal('Указана некорректная ссылка');
        })
    });

    describe('Установка модификатора loading', function() {
        it('Изначально нет спиннера и паранжи', function() {
            expect(block.findBlockOn('spin', 'spin')).not.to.haveMod('progress');
            expect(block).not.to.haveMod(block.elem('paranja'), 'visible');
        });

        it('При установке модификатора loading ставлятся спиннер и паранжа', function() {
            block.setMod('loading', 'yes');

            expect(block.findBlockOn('spin', 'spin')).to.haveMod('progress', 'yes');
            expect(block).to.haveMod(block.elem('paranja'), 'visible', 'yes');
        });

        it('При снятии модификатора loading снимаются спиннер и паранжа', function() {
            block.delMod('loading');

            expect(block.findBlockOn('spin', 'spin')).not.to.haveMod('progress');
            expect(block).not.to.haveMod(block.elem('paranja'), 'visible');
        });
    });
});
