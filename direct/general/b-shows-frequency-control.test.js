describe('b-shows-frequency-control', function() {
    var block,
        sandbox,
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-shows-frequency-control' }, options || {}),
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

    describe('Корректная инициализация', function() {

        it('С параметрами по умолчанию', function() {
            block = createBlock();

            expect(block.model.get('showsValue')).to.be.equal('');
            expect(block.model.get('periodValue')).to.be.equal('');
            expect(block.model.get('periodName')).to.be.equal('days');
        });

        it('При задании параметров блоку', function() {
            block = createBlock({
                showsValue: 100,
                periodName: 'showsPeriod'
            });

            expect(block.model.get('showsValue')).to.be.equal(100);
            expect(block.model.get('periodName')).to.be.equal('showsPeriod');
        });

        it('Если при инициализации задан periodName = showsPeriod, то не показывается контрол редактирования значения периода', function() {
            block = createBlock({
                periodName: 'showsPeriod'
            });

            expect(block).to.haveMod(block.elem('period-value'), 'hidden', 'yes');
        });

        it('Если при инициализации задан periodName = days, то показывается контрол редактирования значения периода', function() {
            block = createBlock({
                periodName: 'days'
            });

            expect(block).not.to.haveMod(block.elem('period-value'), 'hidden', 'yes');
        });
    });

    it('При установке модификатора focused устанавливается фокус на поле количества показов', function() {
        block = createBlock();
        block.setMod('focused', 'yes');
        expect(block.findBlockInside('shows-value', 'input')).to.haveMod('focused', 'yes');
    });

    describe('Изменение селектора периода', function() {
        it('Если выбрали "Период размещения" в селекте, то контрол редактирования значения периода пропадает', function() {
            block = createBlock();

            block.model.set('periodName', 'showsPeriod');

            expect(block).to.haveMod(block.elem('period-value'), 'hidden', 'yes');
        });

        it('Если выбрали "Дней" в селекте, то контрол редактирования значения периода появляется', function() {
            block = createBlock({
                periodName: 'showsPeriod'
            });

            block.model.set('periodName', 'days');

            expect(block).not.to.haveMod(block.elem('period-value'), 'hidden', 'yes');
        });
    });

    describe('Валидация', function() {
        beforeEach(function() {
            block = createBlock({
                showsValue: 20,
                periodValue: 20,
                periodName: 'days'
            });
        });

        it('Метод validate валидирует модель блока', function() {
            var validateModelSpy = block.model.validate;

            block.validate();

            expect(validateModelSpy).to.be.called;
        });

        describe('Поле количества показов', function() {
            var showsValueInput;

            beforeEach(function() {
                showsValueInput = block.findBlockInside('shows-value', 'input');
            });

            it('При вводе в поле не числа - поле очищается', function() {
                block.model.set('showsValue', 'aaa');

                sandbox.clock.tick(100);

                expect(block.model.get('showsValue')).to.be.equal('');
            });

            it('Если в поле значение больше 1000, то при валидации оно подсвечивается красным и появляется текст с описанием ошибки', function() {
                block.model.set('showsValue', 1001);

                block.validate();

                expect(showsValueInput).to.haveMod('error', 'yes');
                expect(block.elem('error').length > 0).to.be.true;
            });

            it('Если в поле значение < 1, то при валидации оно подсвечивается красным и появляется текст с описанием ошибки', function() {
                block.model.set('showsValue', 0);

                block.validate();

                expect(showsValueInput).to.haveMod('error', 'yes');
                expect(block.elem('error').length > 0).to.be.true;
            });

            it('Если в поле значение число в диапозоне от 1 до 1000, то оно валидно.', function() {
                block.model.set('showsValue', 1);

                block.validate();

                expect(showsValueInput).not.to.haveMod('error');
                expect(block.elem('error').length > 0).to.be.false;
            });
        });

        describe('Поле значение периода размещения', function() {
            var periodValueInput;

            beforeEach(function() {
                periodValueInput = block.findBlockInside('period-value', 'input');
            });

            it('При вводе в поле не числа - поле очищается', function() {
                block.model.set('periodValue', 'aaa');

                sandbox.clock.tick(100);

                expect(block.model.get('periodValue')).to.be.equal('');
            });

            it('Если в поле значение больше 30, то при валидации оно подсвечивается красным и появляется текст с описанием ошибки', function() {
                block.model.set('periodValue', 31);

                block.validate();

                expect(periodValueInput).to.haveMod('error', 'yes');
                expect(block.elem('error').length > 0).to.be.true;
            });

            it('Если в поле значение < 1, то при валидации оно подсвечивается красным и появляется текст с описанием ошибки', function() {
                block.model.set('periodValue', 0);

                block.validate();

                expect(periodValueInput).to.haveMod('error', 'yes');
                expect(block.elem('error').length > 0).to.be.true;
            });

            it('Если в поле значение число в диапозоне от 1 до 30, то оно валидно.', function() {
                block.model.set('periodValue', 1);

                block.validate();

                expect(periodValueInput).not.to.haveMod('error');
                expect(block.elem('error').length > 0).to.be.false;
            });

            it('Поле не валидируется, если выбран "Период размещения" в селекте', function() {
                block.model.set('periodName', 'showsPeriod');
                block.model.set('periodValue', 40);

                block.validate();

                expect(periodValueInput).not.to.haveMod('error');
                expect(block.elem('error').length > 0).to.be.false;
            });
        });

        it('Если несколько полей невалидно - показываем ошибку для каждого поля', function() {
            block = createBlock({
                showsValue: 0,
                periodValue: 0,
                periodName: 'days'
            });

            block.validate();

            expect(block.elem('error').length).to.equal(2);
        });
    });

    describe('Метод getData', function() {
        it('Возвращает корректные данные при установленном periodName = showsPeriod', function() {
            block = createBlock({
                showsValue: 100,
                periodValue: 20,
                periodName: 'showsPeriod'
            });

            expect(block.getData()).to.deep.equal({
                showsValue: 100,
                periodName: 'showsPeriod'
            });
        });

        it('Возвращает корректные данные при установленном periodName = days', function() {
            block = createBlock({
                showsValue: 100,
                periodValue: 20,
                periodName: 'days'
            });

            expect(block.getData()).to.deep.equal({
                showsValue: 100,
                periodValue: 20,
                periodName: 'days'
            });
        });
    });

    describe('Метод reset', function() {
        beforeEach(function() {
            block = createBlock({
                showsValue: 100,
                periodValue: 20,
                periodName: 'showsPeriod'
            });

            block.reset();
        });

        it('Устанавливает значения контролов в дефолтное состояние', function() {
            expect(block.findBlockInside('shows-value', 'input').val()).to.equal('');
            expect(block.findBlockInside('period-value', 'input').val()).to.equal('');
            expect(block.findBlockInside('period-name', 'select').val()).to.equal('days');
        });

        it('Устанавливает значения модели в дефолтное состояние', function() {
            expect(block.model.toJSON()).to.deep.equal({
                showsValue: '',
                periodValue: '',
                periodName: 'days',
                periodValueHiddenMod: ''
            });
        });
    });
});
