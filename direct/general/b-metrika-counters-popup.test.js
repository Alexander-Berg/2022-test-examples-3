describe('b-metrika-counters-popup', function() {
    var block,
        ctx = {
            block: 'b-metrika-counters-popup',
            counters: '1234567',
            isPerformance: false,
            needCheckCountersOwner: true
        },
        sandbox,
        metrikaCounters;

    describe('DOM', function() {
        describe('Общее', function() {
            beforeEach(function() {
                block = u.createBlock(ctx);
            });

            afterEach(function() {
                block.destruct();
            });

            ['header', 'body', 'footer', 'accept-button', 'decline-button'].forEach(function(elem) {
                it('В верстке есть элемент ' + elem, function() {
                    expect(block).to.haveElem(elem);
                });
            });

            it('В теле лежит блок b-metrika-counters', function() {
               expect(block.findBlockInside('b-metrika-counters')).not.to.be.null;
           });
        });

        describe('Для не-Смарт кампаний', function() {
            beforeEach(function() {
                block = u.createBlock(ctx);
            });

            afterEach(function() {
                block.destruct();
            });

            it('Текст в заголовке - "Счётчики Метрики"', function() {
                expect(block.elem('header-text').text()).to.equal('Счётчики Метрики');
            });

            it('Текст в подписи - "Номера счётчиков указываются через запятую или пробел."', function() {
                expect(block.elem('metrika-note').text())
                    .to.equal('Номера счётчиков указываются через запятую или пробел.');
            });

            it('В блоке b-metrika-counters лимит - 100', function() {
                expect(block.findBlockInside('b-metrika-counters').getLimit()).to.equal(100);
            });
        });

        describe('Для Смарт кампаний', function() {
            beforeEach(function() {
                block = u.createBlock(u._.extend({}, ctx, { isPerformance: true }));
            });

            afterEach(function() {
                block.destruct();
            });

            it('Текст в заголовке - "Номер счётчика Метрики"', function() {
                expect(block.elem('header-text').text()).to.equal('Номер счётчика Метрики');
            });

            it('Текст в подписи - "Указать можно только один дополнительный счётчик."', function() {
                expect(block.elem('metrika-note').text())
                    .to.equal('Указать можно только один дополнительный счётчик.');
            });

            it('В блоке b-metrika-counters лимит - 1', function() {
               expect(block.findBlockInside('b-metrika-counters').getLimit()).to.equal(1);
            });
        });
    });

    describe('События на блоке', function() {
        beforeEach(function() {
            sandbox = sinon.sandbox.create({
                useFakeTimers: true
            });
        });

        afterEach(function() {
            sandbox.restore();
        });

        it('Если не нужна валидация, кнопка Сохранить дергает событие save', function() {
            block = u.createBlock(u._.extend({}, ctx, { needCheckCountersOwner: false }));
            sandbox.spy(block, 'trigger');

            block.findBlockOn('accept-button', 'button').trigger('click');

            expect(block.trigger.calledWith('save')).to.be.true;

            block.destruct();
        });

        describe('Если разрешена валидация', function() {
            var saveBtn;

            beforeEach(function() {
                block = u.createBlock(ctx);
                saveBtn = block.findBlockOn('accept-button', 'button');
                metrikaCounters = block.findBlockInside('b-metrika-counters');

                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            it('Если разрешена валидация, кнопка Сохранить вызывает валидацию счетчиков', function() {
                sandbox.spy(metrikaCounters, 'validate');

                saveBtn.trigger('click');

                expect(metrikaCounters.validate.called).to.be.true;
            });

            it('Если валидация успешна, дергается событие save', function() {
                sandbox.stub(metrikaCounters, 'validate').callsFake(function() {
                    return {
                        done: function(resolver) {
                            resolver(true);
                        }
                    }
                });

                saveBtn.trigger('click');
                sandbox.clock.tick(2000);

                expect(block.trigger.calledWith('save')).to.be.true;
            });

            it('Если валидация неуспешна, событие не дергается', function() {
                sandbox.stub(metrikaCounters, 'validate').callsFake(function() {
                    return {
                        done: function(resolver) {
                            resolver(false);
                        }
                    }
                });

                saveBtn.trigger('click');
                sandbox.clock.tick(2000);

                expect(block.trigger.calledWith('save')).to.be.false;
            });
        });

        describe('Общее', function() {
            var saveBtn,
                metrikaCounters;

            beforeEach(function() {
                block = u.createBlock(ctx);
                saveBtn = block.findBlockOn('accept-button', 'button');
                metrikaCounters = block.findBlockInside('b-metrika-counters');

                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            it('Кнопка Отмены дергает событие cancel', function() {
                block.findBlockOn('decline-button', 'button').trigger('click');

                expect(block.trigger.calledWith('cancel')).to.be.true;
            });

            it('При проверке счетчика меняется текст на кнопке сохранения', function() {
                metrikaCounters.setMod('check-request', 'yes');

                expect(saveBtn.getText()).to.equal('Сохраняется...');
            });

            it('При окончании проверки текст меняется обратно', function() {
                metrikaCounters.setMod('check-request', '');

                expect(saveBtn.getText()).to.equal('Сохранить');
            });

            it('При невозможности сохранения дизейблится кнопка сохранения', function() {
                metrikaCounters.trigger('state', { canSave: false });

                expect(saveBtn).to.haveMod('disabled', 'yes');
            });

            it('При возможности сохранения кнопка сохранения энейблится', function() {
                metrikaCounters.trigger('state', { canSave: true });

                expect(saveBtn).not.to.haveMod('disabled');
            });
        });
    });

    describe('Внешний АПИ', function() {
        var metrikaCounters;

        beforeEach(function() {
            block = u.createBlock(ctx);
            metrikaCounters = block.findBlockInside('b-metrika-counters');
            block.prepareData();
        });

        afterEach(function() {
            block.destruct();
        });

        it('Метод prepareData вызывает prepareToShow на b-metrika-counters', function() {
            sandbox.spy(metrikaCounters, 'prepareToShow');
            block.prepareData();

            expect(metrikaCounters.prepareToShow.called).to.be.true;
        });

        it('Метод update вызывает updateModel на b-metrika-counters', function() {
            sandbox.spy(metrikaCounters, 'updateModel');
            block.update();

            expect(metrikaCounters.updateModel.called).to.be.true;
        });

        it('Функция isChanged возвращает промис с true при наличии изменений', function() {
            sandbox.stub(metrikaCounters, 'getCounters').callsFake(function() {
                return '98989';
            });

            block.isChanged().done(function(isChanged) {
                expect(isChanged).to.be.true;
            });
        });

        it('Функция isChanged возвращает промис с false при отсутствии изменений', function() {
            sandbox.stub(metrikaCounters, 'getCounters').callsFake(function() {
                return '1234567';
            });
            block.isChanged().done(function(isChanged) {
                expect(isChanged).to.be.false;
            });
        });
    });
});
