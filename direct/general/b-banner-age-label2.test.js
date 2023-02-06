describe('Возрастная метка', function() {
    var block,
        server,
        constStub,
        clock,
        createBlock = function(value, can) {
            return u.getInitedBlock({
                block: 'b-banner-age-label2',
                flags: value || {},
                bid: 1231,
                can: can || { addRemove: true,  edit: true }
            });
        };

    before(function() {
        constStub = sinon.stub(u, 'consts');

        constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
    });

    after(function() {
        constStub.restore();
    });

    describe('Отрисовка блока', function() {
        var itBlockVisibleIs = function(value) {
                if (value) {
                    it('Блок должен быть видимым', function() {
                        expect(block.domElem.height()).to.not.equal(0);
                    });
                }
                else {
                    it('Блок должен быть невидимым', function() {
                        expect(block.domElem.height()).to.equal(0);
                    });
                }
            },
            itLabelTetxIs = function(text) {
                it(text ? 'Текущая метка указана и равна ' + text : 'Текущая метка не указана', function() {
                    expect(block.elem('value').text()).to.equal(text || '');
                });
            };

        describe('Со значением', function() {
            describe('C правами на редактирование', function() {
                before(function() {
                    block = createBlock({ age: 12 }, { edit: true });
                });

                itBlockVisibleIs(true);

                itLabelTetxIs('12');

                it('Есть кнопка изменить', function() {
                    expect(block).to.haveElem('link', 'action', 'edit');
                });

                after(function() {
                    block.destruct();
                });
            });

            describe('Без прав на редактирование', function() {
                before(function() {
                    block = createBlock({ age: 18 }, { edit: false });
                });

                itBlockVisibleIs(true);

                itLabelTetxIs('18');

                it('Нет ссылки изменить', function() {
                    expect(block).to.not.haveElem('link', 'action', 'edit');
                });

                after(function() {
                    block.destruct();
                });
            });

            describe('С правами на добавление/удаление', function() {
                before(function() {
                    block = createBlock({ age: 12 }, { addRemove: true });
                });

                itBlockVisibleIs(true);

                itLabelTetxIs('12');

                it('Есть ссылка удалить', function() {
                    expect(block).to.haveElem('link', 'action', 'remove');
                });

                after(function() {
                    block.destruct();
                });
            });

            describe('Без прав на добавление/удаление', function() {
                before(function() {
                    block = createBlock({ age: 6 }, { addRemove: false });
                });

                itBlockVisibleIs(true);

                itLabelTetxIs('6');

                it('Нет ссылки удалить', function() {
                    expect(block).to.not.haveElem('link', 'action', 'remove');
                });

                after(function() {
                    block.destruct();
                });
            });
        });

        describe('Без значения', function() {
            describe('C правами на редактирование', function() {
                before(function() {
                    block = createBlock({}, { edit: true });
                });

                itBlockVisibleIs(false);

                itLabelTetxIs('');

                after(function() {
                    block.destruct();
                });
            });

            describe('Без прав на редактирование', function() {
                before(function() {
                    block = createBlock({}, { edit: false });
                });

                itBlockVisibleIs(false);

                itLabelTetxIs('');

                after(function() {
                    block.destruct();
                });
            });

            describe('С правами на добавление/удаление', function() {
                before(function() {
                    block = createBlock({}, { addRemove: true });
                });

                itBlockVisibleIs(true);

                itLabelTetxIs('');

                it('Есть ссылка добавить', function() {
                    expect(block).to.haveElem('link', 'action', 'add');
                });

                after(function() {
                    block.destruct();
                });
            });

            describe('Без прав на добавление/удаление', function() {
                before(function() {
                    block = createBlock({}, { addRemove: false });
                });

                itBlockVisibleIs(false);

                itLabelTetxIs('');

                it('Нет ссылки добавить', function() {
                    expect(block).to.not.haveElem('link', 'action', 'add');
                });

                after(function() {
                    block.destruct();
                });
            });
        });
    });

    describe('Работа с блоком', function() {
        var serverRespond = function(data, code) {
            server.respondWith([code || 200, { 'Content-Type': 'application/json' }, JSON.stringify([data])]);
        };

        before(function() {
            clock = sinon.useFakeTimers();
            server = sinon.fakeServer.create();
            server.autoRespond = true;
        });

        after(function() {
            server.restore();
            clock.restore();
        });

        describe('Удачные ответы сервера', function() {
            beforeEach(function() {
                serverRespond({ success: 1 });
            });

            it('Добавляем возрастную метку', function() {
                block = createBlock();

                expect(block).to.triggerEvent(
                    'flags-change',
                    { value: { age: 18 } },
                    function() {
                        block.domElem.find('.b-banner-age-label2__link_action_add').click();
                        clock.tick(10);
                    }
                );

                block.destruct();
            });

            it('Снимаем возрастную метку', function() {
                block = createBlock({ alcohol: true });

                expect(block).to.triggerEvent(
                    'flags-change',
                    { value: { age: -1 } },
                    function() {
                        block.domElem.find('.b-banner-age-label2__link_action_remove').trigger('click');
                        clock.tick(10);
                    }
                );

                block.destruct();
            });

            [0, 6, 12, 16, 18].forEach(function(age) {
                it('Меняем возрастную метку на `' + age + '`', function() {
                    block = createBlock({ alcohol: true });

                    expect(block).to.triggerEvent(
                        'flags-change',
                        { value: { age: age } },
                        function() {
                            block.domElem.find('.b-banner-age-label2__link_action_edit').click();
                            clock.tick(0);

                            var linkDomElem = block._getPopup().domElem.find('.b-banner-age-label2__item')
                                .filter(function(i, item) { return $(item).text().match(/\d+/)[0] == age });

                            linkDomElem.click();
                            clock.tick(10);
                        }
                    );

                    block._getPopup().destruct();
                    block.destruct();
                });
            });
        });
    });

});
