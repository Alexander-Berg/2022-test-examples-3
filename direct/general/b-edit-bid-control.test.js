describe('b-edit-bid-control', function() {
    var block,
        sandbox,
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-edit-bid-control' }, options || {}),
                { inject: true }
            );
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('При установке модификатора focused устанавливается фокус на поле со значением ставки', function() {
        block = createBlock();
        block.setMod('focused', 'yes');
        expect(block._bidValueInput).to.haveMod('focused', 'yes');
    });

    it('При изменении селектора режима ставки поле автоматически устанавливается на поле со значением ставки', function() {
        block = createBlock();
        block._modeSelect.val('down');
        expect(block._bidValueInput).to.haveMod('focused', 'yes');
    });

    it('Значение подсказки для значения ставки корректно меняется в зависимости от установленного режима', function() {
        block = createBlock();

        expect(block.model.get('inputHint')).to.be.equal('max 1200');

        block._modeSelect.val('down');

        expect(block.model.get('inputHint')).to.be.equal('max 100');
    });

    describe('Корректная инициализация', function() {

        it('С параметрами по умолчанию', function() {
            block = createBlock();

            expect(block.model.get('mode')).to.be.equal('up');
            expect(block.model.get('value')).to.be.equal('');
            expect(block.model.get('maxValue')).to.be.equal(1200);
        });

        it('При задании положительного значения ставки', function() {
            block = createBlock({ value: 70 });

            expect(block.model.get('mode')).to.be.equal('up');
            expect(block.model.get('value')).to.be.equal(70);
            expect(block.model.get('maxValue')).to.be.equal(1200);
        });

        it('При задании отрицательного значения ставки', function() {
            block = createBlock({ value: -70 });

            expect(block.model.get('mode')).to.be.equal('down');
            expect(block.model.get('value')).to.be.equal(70);
            expect(block.model.get('maxValue')).to.be.equal(100);
        });

        it('При задании максимального положительного значения ставки', function() {
            block = createBlock({ value: 70, maxUpValue: 200 });

            expect(block.model.get('maxValue')).to.be.equal(200);
        });

        it('При задании максимального отрицательного значения ставки', function() {
            block = createBlock({ value: -70, maxDownValue: 200 });

            expect(block.model.get('maxValue')).to.be.equal(200);
        });

        it('Если блок инициализирован с параметром isCommon, значение режима по умолчиню будет noValue', function() {
            block = createBlock({ isCommon: true });

            expect(block.model.get('mode')).to.be.equal('noValue');
        });
    });

    describe('Валидация', function() {
        beforeEach(function() {
            block = createBlock();
        });

        it('Значение ставки может быть пустым', function() {
            block._bidValueInput.val('');
            expect(block._bidValueInput).not.to.haveMod('error');
        });

        it('Значение ставки валидно при вводе положительного числа', function() {
            block._bidValueInput.val('10');
            expect(block._bidValueInput).not.to.haveMod('error');
        });

        it('Значение ставки невалидно при вводе отрицательного числа', function() {
            block._bidValueInput.val('-70');
            expect(block._bidValueInput).to.haveMod('error', 'yes');
        });

        describe('При установленном режиме увеличить', function() {
            it('Значение ставки невалидно при вводе числа выше заданной максимальной ставки', function() {
                block._bidValueInput.val('1201');
                expect(block._bidValueInput).to.haveMod('error', 'yes');
            });

            it('Значение ставки валидно при вводе числа ниже или равного заданной максимальной ставки', function() {
                block._bidValueInput.val('1200');
                expect(block._bidValueInput).not.to.haveMod('error');
            });

            it ('Если блок инициализирован с параметром isCommon значение ставки валидно при вводе числа выше заданной максимальной ставки', function() {
                var commonBlock = createBlock({ isCommon: true });
                commonBlock._bidValueInput.val('1201');
                expect(commonBlock._bidValueInput).not.to.haveMod('error');
                commonBlock.destruct();
            });
        });

        describe('При установленном режиме уменьшить', function() {
            beforeEach(function() {
                block._modeSelect.val('down');
            });

            it('Значение ставки невалидно при вводе числа выше заданной максимальной ставки', function() {
                block._bidValueInput.val('101');
                expect(block._bidValueInput).to.haveMod('error', 'yes');
            });

            it('Значение ставки валидно при вводе числа ниже или равного заданной максимальной ставки', function() {
                block._bidValueInput.val('100');
                expect(block._bidValueInput).not.to.haveMod('error');
            });

            it ('Если блок инициализирован с параметром isCommon значение ставки валидно при вводе числа выше заданной максимальной ставки', function() {
                var commonBlock = createBlock({ isCommon: true });
                commonBlock._bidValueInput.val('101');
                expect(commonBlock._bidValueInput).not.to.haveMod('error');
                commonBlock.destruct();
            });
        });
    });

    describe('События блока', function() {
        beforeEach(function() {
            block = createBlock();
            sandbox.spy(block, 'trigger');
        });

        it('При изменении полей блока триггерится событие change', function() {
            [
                { block: block._modeSelect, value: 'down'},
                { block: block._bidValueInput, value: '10'}
            ].forEach(function(field) {
                field.block.val(field.value);
                expect(block).to.triggerEvent('change');
            });
        });

        it('При валидации модели блока триггерится событие validated c корректной информацией о результатах валидации', function() {
            var validationRezult = block.model.validate();

            expect(block.trigger.calledWith('validated', validationRezult));
        });
    });

    describe('Метод getData', function() {
        it('Возвращает корректные данные о значении ставки при режиме ставки Увеличить', function() {
            block = createBlock({ value: 10 });

            expect(block.getData()).to.deep.equal({
                value: 10,
                mode: 'up',
                absValue: 10
            });
        });

        it('Возвращает корректные данные о значении ставки при режиме ставки Уменьшить', function() {
            block = createBlock({ value: -20 });

            expect(block.getData()).to.deep.equal({
                value: -20,
                mode: 'down',
                absValue: 20
            });
        });
    });
});
