describe('b-statistics-template-saving', function() {
    var block,
        clock,
        wrap = $('<div>'),
        /**
         * Создает блок
         * @param {jQuery} [parent] - jQuery нода в которую будет добавлен построенный блок
         */
        createBlock = function(parent) {
            var bemjson = {
                block: 'b-statistics-template-saving',
                input: { value: 'новый шаблон' }
            };

            block = BEM.DOM.init(u.getDOMTree(bemjson).appendTo(parent || wrap)).bem('b-statistics-template-saving');
        };

    describe('', function() {

        beforeEach(function() {
            createBlock($('body'));

            clock = sinon.useFakeTimers();
        });

        afterEach(function() {
            clock.tick(0);
            block.destruct();
            clock.restore();
        });

        it('Должен открываться popup при клике на кнопку(елемент switcher)', function() {
            block.findElem('switcher').trigger('click');

            expect(block._dropdown.getPopup().isShown()).to.be.equal(true);
        });

        it('Должен быть фокус на инпуте после открытия popup-a', function() {
            block.findElem('switcher').trigger('click');

            expect(block._input.hasMod('focused', 'yes')).to.be.equal(true);
        });

        it('Должно происходить событие save по нажатию на кнопку(save)', function() {
            sinon.spy(block, 'trigger');
            block.findElem('save').trigger('click');

            clock.tick(0);

            expect(block.trigger.calledWith('save')).to.be.equal(true);
            expect(block.trigger.callCount).to.be.equal(1);
            expect(block.trigger.firstCall.args[1]).to.be.equal('новый шаблон');

            block.trigger.restore();
        });

        it([
            'Событие save должно содержать значеине инпута в котором удалены пустые места',
            'с начала и конца строки.'
        ].join(' '), function() {
            sinon.spy(block, 'trigger');
            block.setValue('  новый шаблон  ');
            block.findElem('save').trigger('click');

            clock.tick(0);
            expect(block.trigger.firstCall.args[1]).to.be.equal('новый шаблон');

            block.trigger.restore();
        });

        it('Должна дизейблиться кнопка кнопку(save) если инпут не содержит значение', function() {
            block.setValue('');

            expect(block._getButton('save').isDisabled()).to.be.equal(true);
        });

        describe('hidePopup()', function() {

            it('Должен скрывать попап', function() {
                block.findElem('switcher').trigger('click');
                block.hidePopup();

                expect(block._dropdown.getPopup().isShown()).to.be.equal(false);
            });

        });

    });

    describe('', function() {

        beforeEach(function() {
            createBlock();
        });

        afterEach(function() {
            block.destruct();
        });

        describe('setValue()', function() {

            it('Должен устанавливает новое значение инпуту', function() {
                block.setValue('новое значение');

                expect(block._input.val()).to.be.equal('новое значение');
            });

        });

        describe('showError()', function() {

            it('Должен показываеться ошибку(элемент error)', function() {
                block.showError('упс!');

                expect(block.hasMod(block._errorElem, 'show', 'yes')).to.be.equal(true);
            });

        });

        describe('hideError()', function() {

            it('Должен скрывать ошибку(элемент error)', function() {
                block
                    .showError('упс!')
                    .hideError();

                expect(block.hasMod(block._errorElem, 'show', 'yes')).to.be.equal(false);
            });

        });

        describe('disableButton()', function() {

            it('Должен делать кнопку неактивной', function() {
                block.disableButton('save');

                expect(block._getButton('save').isDisabled()).to.be.equal(true);
            });

        });

        describe('enableButton()', function() {

            it('Должен делать кнопку активной', function() {
                block
                    .disableButton('save')
                    .enableButton('save');

                expect(block._getButton('save').isDisabled()).to.be.equal(false);
            });

        });

        describe('getInput()', function() {

            it('Должен вернуть экземпляр блока input', function() {
                expect(block.getInput().domElem.length).to.be.equal(1);
            });

        });

    });

    describe('Отложенная инициализация:', function() {

        beforeEach(function() {
            block = BEM.DOM.init(u.getDOMTree({ block: 'b-statistics-template-saving' }).appendTo($('body')));
        });

        afterEach(function() {
            block = block.bem('b-statistics-template-saving');

            block.destruct();
        });

        it('Не должен инититься при создании', function() {
            expect($('.b-statistics-template-saving').hasClass('b-statistics-template-saving_js_inited')).to.be.equal(false);
        });

        it('Должен инититься вместе с блоком dropdown', function() {
            block.bem('dropdown');

            expect($('.b-statistics-template-saving').hasClass('b-statistics-template-saving_js_inited')).to.be.equal(true);
        });

    });

});
