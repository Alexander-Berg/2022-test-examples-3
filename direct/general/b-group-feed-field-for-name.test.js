describe('b-group-feed-field-for-name', function() {
    var createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-group-feed-field-for-name' }, options || {}),
                { inject: true }
            );
        },
        createSandbox = function() {
            return sinon.sandbox.create({
                useFakeTimers: true
            });
        };

    describe('Инициализация', function() {
        describe('Передали title', function() {
            it('Чекбокс отмечен', function() {
                var block = createBlock({ title: 'Test' });

                expect(block.findBlockOn('switcher', 'checkbox').isChecked()).to.be.true;

                block.destruct();
            });

            it('Отображается поле с названием', function() {
                var block = createBlock({ title: 'Test' });

                expect(block.getMod(block.elem('name-controls'), 'hidden')).to.equal('');

                block.destruct();
            });

            it('Поле с названием заполнено значением title', function() {
                var block = createBlock({ title: 'Test' });

                expect(block.findBlockOn('name-controls-field', 'input').val()).to.equal('Test');

                block.destruct();
            });
        });

        describe('НЕ передали title', function() {
            it('Чекбокс НЕ отмечен', function() {
                var block = createBlock();

                expect(block.findBlockOn('switcher', 'checkbox').isChecked()).to.be.false;

                block.destruct();
            });

            it('Поле с названием скрыто', function() {
                var block = createBlock();

                expect(block.getMod(block.elem('name-controls'), 'hidden')).to.equal('yes');

                block.destruct();
            });

            it('Поле с названием пустое', function() {
                var block = createBlock();

                expect(block.findBlockOn('name-controls-field', 'input').val()).to.equal('');

                block.destruct();
            });
        });

        describe('Изменение состояния чекбокса', function() {
            it('Чекбокc переведен в состояние чекнут -> поле с названием отображено', function() {
                var block = createBlock();

                block.findBlockOn('switcher', 'checkbox').setMod('checked', 'yes');

                expect(block.getMod(block.elem('name-controls'), 'hidden')).to.equal('');

                block.destruct();
            });

            it('Чекбокc переведен в состояние анчекнут -> поле с названием скрыто', function() {
                var block = createBlock();

                block.findBlockOn('switcher', 'checkbox').setMod('checked', '');

                expect(block.getMod(block.elem('name-controls'), 'hidden')).to.equal('yes');

                block.destruct();
            });

            it('Чекбокc переведен в состояние чекнут и поле с названием не заполнено -> установлен фокус на поле с названием', function() {
                var block = createBlock();

                block.findBlockOn('switcher', 'checkbox').setMod('checked', 'yes');

                expect(block.findBlockOn('name-controls-field', 'input')).to.haveMod('focused', 'yes');

                block.destruct();
            });

            it('Всегда -> тригеррится событие change c текущим состоянием контролов', function() {
                var block = createBlock(),
                    sandbox = createSandbox();

                sandbox.spy(block, 'trigger');

                block.findBlockOn('switcher', 'checkbox').setMod('checked', 'yes');

                expect(block.trigger.calledWith('change', { value: '', isChecked: true }));

                block.destruct();
                sandbox.restore();
            });
        });
    });

    it('Изменение значения поля с названием -> тригеррится событие change c текущим состоянием контролов', function() {
        var block = createBlock({ title: 'init' }),
            sandbox = createSandbox();

        sandbox.spy(block, 'trigger');

        block.findBlockOn('name-controls-field', 'input').val('Test');

        expect(block.trigger.calledWith('change', { value: 'Test', isChecked: false }));

        block.destruct();
        sandbox.restore();
    });

    it('Метод getValue -> возвращает текущее состояние контролов', function() {
        var block = createBlock({ title: 'Test' });

        expect(block.getValue()).to.deep.equal({ value: 'Test', isChecked: true });

        block.destruct();
    });

    describe('Метод setValue', function() {
        it('Устанавливает состояние чекбокса', function() {
            var block = createBlock();

            block.setValue({ value: 'Test', isChecked: true });

            expect(block.findBlockOn('switcher', 'checkbox')).to.haveMod('checked', 'yes');

            block.destruct();
        });

        it('Устанавливает значение поля с названием', function() {
            var block = createBlock();

            block.setValue({ value: 'Test', isChecked: true });

            expect(block.findBlockOn('name-controls-field', 'input').val()).to.equal('Test');

            block.destruct();
        });

        it('Тригеррит событие change блока', function() {
            var block = createBlock(),
                sandbox = createSandbox();

            sandbox.spy(block, 'trigger');

            block.setValue({ value: 'Test', isChecked: true });

            expect(block).to.triggerEvent('change');

            block.destruct();
            sandbox.restore();
        });
    });
});
