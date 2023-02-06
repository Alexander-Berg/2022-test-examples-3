describe('b-editable-label', function() {

    var block,
        sandbox;

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-editable-label',
            value: options.value
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('Инициализация', function() {

        afterEach(function() {
            destructBlock();
        });

        it('Если value не передано, то подставляется значение по-умолчанию', function() {
            createBlock();

            expect(block.getValue()).to.be.eq('Новая');
        });

        it('Если value передано, то подставляется переданное значение', function() {
            createBlock({ value: 'Тестовое значение' });

            expect(block.getValue()).to.be.eq('Тестовое значение');
        });

        it('Поле ввода скрыто', function() {
            createBlock({ value: 'Тестовое значение' });

            expect(block.elem('input').is(':visible')).to.be.false;
        });

        it('Значение показано', function() {
            createBlock({ value: 'Тестовое значение' });

            expect(block.elem('label').is(':visible')).to.be.true;
        });

    });

    describe('Поведение', function() {
        var DEFAULT_VALUE = 'Значение по-умолчанию';

        beforeEach(function() {
            createBlock({ value: DEFAULT_VALUE });
        });

        afterEach(function() {
            destructBlock();
        });

        describe('При нажатии на имя', function() {

            function clickName() {
                block.findBlockInside('edit-button', 'button2').trigger('click');
            }

            it('появляется input', function() {
                clickName();

                expect(block.elem('input').is(':visible')).to.be.true;
            });

            it('пропадает лейбл и иконка карандаша', function() {
                clickName();

                expect(block.elem('label').is(':visible')).to.be.false;
            });

            it('input в фокусе', function() {
                clickName();

                expect(block.findBlockInside('input')).to.haveMod('focused', 'yes');
            });

            it('после изменения значения внутри инпута генерируется change', function() {
                clickName();

                expect(block).to.triggerEvent('change', { value: 'new_val' }, function() {
                    block.findBlockInside('input').elem('control').val('new_val');
                    block.findBlockInside('input').trigger('change');
                });
            });

            describe('далее нажмаем Enter', function() {
                beforeEach(function(){
                    clickName();
                });

                function pressEnter() {
                    var e = $.Event('keydown');

                    e.which = BEM.blocks.keycodes.ENTER;
                    block.findBlockInside('input').elem('control').trigger(e);
                }

                it('скрывается input', function() {
                    pressEnter();

                    expect(block.elem('input').is(':visible')).to.be.false;
                });

                it('показывается лейбл', function() {
                    pressEnter();

                    expect(block.elem('label').is(':visible')).to.be.true;
                });
            });

            it('позиция курсора в конце текста', function() {
                clickName();

                expect(block.findBlockInside('input').getSelectionEnd()).to.be.eq(DEFAULT_VALUE.length);
            });
        });

    });

});
