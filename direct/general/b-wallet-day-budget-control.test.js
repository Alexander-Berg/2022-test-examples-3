describe('b-wallet-day-budget-control', function() {
    var block,
        sandbox,
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-wallet-day-budget-control' }, options || {}),
                { inject: true }
            );
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });

        u.stubCurrencies2(sandbox);
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('Корректная инициализация', function() {

        it('По умолчанию тумблер выключен, поля задизэйблены', function() {
            block = createBlock();

            expect(block._switcher.hasMod('checked', 'yes')).to.be.false;
            expect(block._valueInput).to.haveMod('disabled', 'yes');
            expect(block._modeSelect).to.haveMod('disabled', 'yes');
        });

        it('Если передан параметр isActive тумблер должен быть включен, поля активны', function() {
            block = createBlock({ isActive: true });

            expect(block._switcher.hasMod('checked', 'yes')).to.be.true;
            expect(block._valueInput).not.to.haveMod('disabled', 'yes');
            expect(block._modeSelect).not.to.haveMod('disabled', 'yes');
        });
    });

    describe('При установке тумблера в состояние "выключен"', function() {
        beforeEach(function() {
            block = createBlock();
            block._switcher.delMod('checked');
            sandbox.clock.tick(200);
        });

        it('Поле суммы дневного бюджета задизэйблено', function() {
            expect(block._valueInput).to.haveMod('disabled', 'yes');
        });

        it('Поле режима показа задизэйблено', function() {
            expect(block._modeSelect).to.haveMod('disabled', 'yes');
        });
    });

    describe('При установке тумблера в состояние "включен"', function() {
        beforeEach(function() {
            block = createBlock();
            block._switcher.setMod('checked', 'yes');
            sandbox.clock.tick(200);
        });

        it('Поле суммы дневного бюджета активно', function() {
            expect(block._valueInput).not.to.haveMod('disabled', 'yes');
        });

        it('Поле суммы дневного бюджета в фокусе', function() {
            expect(block._valueInput).to.haveMod('focused', 'yes');
        });

        it('Поле режима показа активно', function() {
            expect(block._modeSelect).not.to.haveMod('disabled', 'yes');
        });
    });

    describe('Метод validate', function() {
        beforeEach(function() {
            block = createBlock();
        });

        describe('Если данные корректно заполнены', function() {
            var validationResult;

            beforeEach(function() {
                sandbox.stub(block.model, 'validate').callsFake(function() {
                    var result = {
                        valid: true
                    };

                    this.trigger('validated', result);

                    return result;
                });

                validationResult = block.validate();
            });

            it('Возвращает true', function() {
                expect(validationResult).to.be.true;
            });

            it('Ошибки валидации не показываются', function() {
                expect(block.getMod(block.elem('errors'), 'hidden')).to.equal('yes');
            });
        });

        describe('Если данные некорректно заполнены', function() {
            var validationResult;

            beforeEach(function() {
                sandbox.stub(block.model, 'validate').callsFake(function() {
                    var result = {
                        errors: [
                            {
                                rule: 'required',
                                text: 'Не указана сумма дневного бюджета'
                            }
                        ]
                    };

                    this.trigger('validated', result);

                    return result;
                });

                validationResult = block.validate();
            });

            it('Возвращает false', function() {
                expect(validationResult).to.be.false;
            });

            it('Показываются ошибки валидации', function() {
                expect(block.getMod(block.elem('errors'), 'hidden')).to.equal('');
            });
        });
    });

    describe('Метод getData корректно возвращает данные', function() {
        it('При инициализации без параметров', function() {
            block = createBlock();

            expect(block.getData()).to.deep.equal({
                value: undefined,
                mode: 'default',
                isActive: false
            })
        });

        it('При инициализации c параметрами', function() {
            block = createBlock({
                value: 10,
                mode: 'stretched',
                isActive: true
            });

            expect(block.getData()).to.deep.equal({
                value: 10,
                mode: 'stretched',
                isActive: true
            })
        });

        it('При редактировании пользователем параметров', function() {
            block = createBlock();

            block._switcher.setMod('checked', 'yes');
            block._valueInput.val('100');

            expect(block.getData()).to.deep.equal({
                value: 100,
                mode: 'default',
                isActive: true
            })
        });
    });
});
