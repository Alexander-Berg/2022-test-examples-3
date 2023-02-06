describe('b-metrika-counters-popup_type_mass-actions', function() {
    var block,
        ctx = {
            block: 'b-metrika-counters-popup',
            mods: { type: 'mass-actions' },
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

            ['header', 'body', 'footer', 'replace-button', 'decline-button'].forEach(function(elem) {
                it('В верстке есть элемент ' + elem, function() {
                    expect(block).to.haveElem(elem);
                });
            });

            it('Текст в заголовке - "Счётчики Метрики"', function() {
                expect(block.elem('header-text').text()).to.equal('Изменить счётчик Метрики');
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

            it('Текст в подписи - "Номера счётчиков указываются через запятую или пробел.' +
                'Счётчики будут добавлены к уже имеющимся на выбранных кампаниях"', function() {
                expect(block.elem('metrika-note').text())
                    .to.equal('Номера счётчиков указываются через запятую или пробел.' +
                    'Счётчики будут добавлены к уже имеющимся на выбранных кампаниях.');
            });

            it('В блоке b-metrika-counters лимит - 100', function() {
                expect(block.findBlockInside('b-metrika-counters')._limit).to.equal(100);
            });

            ['add-button', 'delete-button'].forEach(function(elem) {
                it('В верстке есть элемент ' + elem, function() {
                    expect(block).to.haveElem(elem);
                });
            });
        });

        describe('Для Смарт кампаний', function() {
            beforeEach(function() {
                block = u.createBlock(u._.extend({}, ctx, { isPerformance: true }));
            });

            afterEach(function() {
                block.destruct();
            });

            it('Текст в подписи - "Счетчик метрики можно только заменить."', function() {
                expect(block.elem('metrika-note').text())
                    .to.equal('Счетчик метрики можно только заменить.');
            });

            it('В блоке b-metrika-counters лимит - 1', function() {
                expect(block.findBlockInside('b-metrika-counters')._limit).to.equal(1);
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

        describe('Если не нужна валидация', function() {
            beforeEach(function() {
                block = u.createBlock(u._.extend({}, ctx, { needCheckCountersOwner: false }));
                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            ['add', 'replace'].forEach(function(buttonName) {
                it('Кнопка ' +
                    (buttonName == 'add' ? 'Добавить' : 'Заменить') + ' дергает событие ' + buttonName, function() {
                    block.findBlockOn(buttonName + '-button', 'button').trigger('click');

                    expect(block.trigger.calledWith(buttonName)).to.be.true;
                });

                it('Кнопка ' +
                    (buttonName == 'add' ? 'Добавить' : 'Заменить') + ' ставит модификтор processing', function() {
                    block.findBlockOn(buttonName + '-button', 'button').trigger('click');

                    expect(block).to.haveMod('processing', 'yes');
                });
            });
        });

        describe('Если нужна валидация', function() {
            var addBtn,
                replaceBtn;

            beforeEach(function() {
                block = u.createBlock(ctx);
                addBtn = block.findBlockOn('add-button', 'button');
                replaceBtn = block.findBlockOn('replace-button', 'button');
                metrikaCounters = block.findBlockInside('b-metrika-counters');

                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            ['add', 'replace'].forEach(function(buttonName) {
                it('Если разрешена валидация, кнопка ' + (buttonName == 'add' ? 'Добавить' : 'Заменить' ) +
                    ' вызывает валидацию счетчиков', function() {
                    var button = buttonName == 'add' ? addBtn : replaceBtn;
                    sandbox.spy(metrikaCounters, 'validate');

                    button.trigger('click');

                    expect(metrikaCounters.validate.called).to.be.true;
                });

                it('Если валидация успешна, дергается событие ' + buttonName, function() {
                    var button = buttonName == 'add' ? addBtn : replaceBtn;
                    sandbox.stub(metrikaCounters, 'validate').callsFake(function() {
                        return {
                            done: function(resolver) {
                                resolver(true);
                            }
                        }
                    });

                    button.trigger('click');
                    sandbox.clock.tick(1000);

                    expect(block.trigger.calledWith(buttonName)).to.be.true;
                });


                it('Если валидация неуспешна, событие не дергается', function() {
                    var button = buttonName == 'add' ? addBtn : replaceBtn;
                    sandbox.stub(metrikaCounters, 'validate').callsFake(function() {
                        return {
                            done: function(resolver) {
                                resolver(false);
                            }
                        }
                    });

                    button.trigger('click');
                    sandbox.clock.tick(1000);

                    expect(block.trigger.calledWith(buttonName)).to.be.false;
                });

                it('Кнопка ' +
                    (buttonName == 'add' ? 'Добавить' : 'Заменить') + ' ставит модификтор processing', function() {
                    var button = buttonName == 'add' ? addBtn : replaceBtn;
                    button.trigger('click');

                    expect(block).to.haveMod('processing', 'yes');
                });

                it('Если валидация неуспешна, снимается модификатор processing', function() {
                    var button = buttonName == 'add' ? addBtn : replaceBtn;
                    sandbox.stub(metrikaCounters, 'validate').callsFake(function() {
                        return {
                            done: function(resolver) {
                                resolver(false);
                            }
                        }
                    });

                    button.trigger('click');
                    sandbox.clock.tick(1000);

                    expect(block).not.to.haveMod('processing', 'yes');
                })
            });
        });

        describe('Общие события', function() {
            beforeEach(function() {
                block = u.createBlock(ctx);
                metrikaCounters = block.findBlockInside('b-metrika-counters');

                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            it('При нажатии на Удалить отправляется событие deleteAll', function() {
                block.findBlockOn('delete-button', 'button').trigger('click');

                expect(block.trigger.calledWith('deleteAll')).to.be.true;
            });

            it('При нажатии на Удалить ставится модификатор processing', function() {
                block.findBlockOn('delete-button', 'button').trigger('click');

                expect(block).to.haveMod('processing', 'yes');
            });
        });

        describe('Дизейбл-энейбл кнопок', function() {
            beforeEach(function() {
                block = u.createBlock(u._.extend({}, ctx, { needCheckCountersOwner: false }));
                metrikaCounters = block.findBlockInside('b-metrika-counters');

                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            describe('Изменение state блока b-metrika-counters', function() {
                ['', 'yes'].forEach(function(modVal) {
                    describe('Если processing: "' + modVal + '"', function() {
                        beforeEach(function() {
                            block.setMod('processing', modVal);
                        });

                        // кнопки add, replace дизейблятся, если canSave: false (невалидный счетчик)
                        // или если уже идет обработка
                        ['add', 'replace'].forEach(function(buttonName) {
                            it('Если приходит state: canSave: false, кнопка ' + buttonName + '-button задизейблена', function() {
                                metrikaCounters.findBlockInside('input').val('12345');
                                metrikaCounters.trigger('state', { canSave: false });

                                expect(block.findBlockOn(buttonName + '-button', 'button')).to.haveMod('disabled', 'yes');
                            });

                            it('Если приходит state: canSave: true, кнопка ' + buttonName + '-button ' +
                                (modVal == 'yes' ? 'задизейблена' : 'энэйблена'), function() {
                                var isDisabled = modVal == 'yes';
                                metrikaCounters.trigger('state', { canSave: true });

                                isDisabled ?
                                    expect(block.findBlockOn(buttonName + '-button', 'button')).to.haveMod('disabled', 'yes') :
                                    expect(block.findBlockOn(buttonName + '-button', 'button')).not.to.haveMod('disabled');
                            });
                        });

                        // кнопки delete, decline НЕ дизейблятся, если canSave: false (невалидный счетчик)
                        // но дизейблятся, если уже идет обработка
                        ['delete', 'decline'].forEach(function(buttonName) {
                            it('Если приходит state: canSave: false, кнопка ' + buttonName + '-button ' +
                                (modVal == 'yes' ? 'задизейблена' : 'энэйблена'), function() {
                                var isDisabled = modVal == 'yes';

                                metrikaCounters.findBlockInside('input').val('12345');
                                metrikaCounters.trigger('state', { canSave: false });

                                isDisabled ?
                                    expect(block.findBlockOn(buttonName + '-button', 'button')).to.haveMod('disabled', 'yes') :
                                    expect(block.findBlockOn(buttonName + '-button', 'button')).not.to.haveMod('disabled');
                            });
                        });
                    });
                });
            });

            describe('Добавление модификатора processing', function() {
                ['add', 'replace', 'delete', 'decline'].forEach(function(buttonName) {
                    it('Если processing: "yes", кнопка ' + buttonName + '-button задизейблена', function() {
                        block.setMod('processing', 'yes');

                        expect(block.findBlockOn(buttonName + '-button', 'button')).to.haveMod('disabled', 'yes');
                    });

                    it('Если  processing: "", кнопка ' + buttonName + '-button энэйблена', function() {
                        block.delMod('processing');

                        expect(block.findBlockOn(buttonName + '-button', 'button')).not.to.haveMod('disabled');
                    });
                });
            });

            describe('Наличие счетчиков в инпуте', function() {
                describe('Если счетчика нет', function() {
                    beforeEach(function() {
                        metrikaCounters.findBlockInside('input').val('');
                        metrikaCounters.trigger('state', { canSave: false });
                    });

                    ['add', 'replace'].forEach(function(buttonName) {
                        it('Кнопка ' + buttonName + ' задизейблена', function() {
                            expect(block.findBlockOn(buttonName + '-button', 'button')).to.haveMod('disabled', 'yes');
                        });
                    });

                    ['delete', 'decline'].forEach(function(buttonName) {
                        it('Кнопка ' + buttonName + ' энейблена', function() {
                            expect(block.findBlockOn(buttonName + '-button', 'button')).not.to.haveMod('disabled');
                        });
                    });
                });

                describe('Если счетчик есть', function() {
                    beforeEach(function() {
                        metrikaCounters.findBlockInside('input').val('123456');
                        metrikaCounters.trigger('state', { canSave: true });
                    });

                    ['add', 'replace', 'delete', 'decline'].forEach(function(buttonName) {
                        it('Кнопка ' + buttonName + ' энейблена', function() {
                            expect(block.findBlockOn(buttonName + '-button', 'button')).not.to.haveMod('disabled');
                        });
                    });
                });
            });
        });

        describe('Изменение текста на кнопках', function() {
            var addBtn,
                replaceBtn,
                deleteBtn;

            beforeEach(function() {
                block = u.createBlock(u._.extend({}, ctx, { needCheckCountersOwner: false }));
                addBtn = block.findBlockOn('add-button', 'button');
                replaceBtn = block.findBlockOn('replace-button', 'button');
                deleteBtn = block.findBlockOn('delete-button', 'button');
                metrikaCounters = block.findBlockInside('b-metrika-counters');

                sandbox.spy(block, 'trigger');
            });

            afterEach(function() {
                block.destruct();
            });

            it('При Добавлении текст на кнопке становится "Добавляется..."', function() {
                addBtn.trigger('click');

                expect(addBtn.getText()).to.equal('Добавляется...');
            });

            it('При Замене текст на кнопке становится "Изменяется..."', function() {
                replaceBtn.trigger('click');

                expect(replaceBtn.getText()).to.equal('Изменяется...');
            });

            it('При Удалении текст на кнопке становится "Удаляется..."', function() {
                deleteBtn.trigger('click');

                expect(deleteBtn.getText()).to.equal('Удаляется...');
            });

            it('Метод onCountersSaveFinished после доабвления возвращает текст Добавить', function() {
                addBtn.trigger('click');
                block.onCountersSaveFinished();

                expect(addBtn.getText()).to.equal('Добавить');
            });

            it('Метод onCountersSaveFinished после замены возвращает текст Заменить', function() {
                replaceBtn.trigger('click');
                block.onCountersSaveFinished();

                expect(replaceBtn.getText()).to.equal('Заменить');
            });

            it('Метод onCountersSaveFinished после удаления возвращает текст Удалить все счётчики', function() {
                deleteBtn.trigger('click');
                block.onCountersSaveFinished();

                expect(deleteBtn.getText()).to.equal('Удалить все счётчики');
            });
        });
    });
});
