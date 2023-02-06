describe('b-dynamic-group-domain', function() {
    var block,
        group,
        input,
        message,
        sandbox,
        ctx = {
            block: 'b-dynamic-group-domain',
            content: [
                {
                    block: 'b-dynamic-group-domain',
                    js: {
                        modelParams: {
                            name: 'b-dynamic-group-domain',
                            id: '12345'
                        },
                        modelData: {
                            lengthLimit: u.consts('MAX_URL_LENGTH')
                        }
                    }
                },
                {
                    block: 'b-dynamic-group-domain',
                    elem: 'controls',
                    group: { main_domain: 'ya.ru' }
                },
                {
                    block: 'b-dynamic-group-domain',
                    elem: 'hint'
                },
                {
                    block: 'b-dynamic-group-domain',
                    elem: 'domain-message'
                }
            ]
        },
        createBlock = function(isDisabled) {
            ctx.content[1].disabled = !!isDisabled;
            block = u.createBlock(ctx);
            input = block.findBlockInside('input');
            message = block.findElem('domain-message');
        },
        tick = function() {
            sandbox.clock.tick(1000);
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });

        group = BEM.MODEL.create({ name: 'dm-dynamic-group', id: '12345' }, { main_domain: 'ya.ru' });
    });

    afterEach(function() {
        sandbox.restore();
        group.destruct();
        block.destruct();
    });

    describe('DOM', function() {
        it('если передано disabled, контрол имеет модификатор disabled', function() {
            createBlock(true);

            expect(input).to.haveMod('disabled', 'yes');
        });

        it('если не передано disabled, блок не имеет модификатор disabled', function() {
            createBlock();

            expect(input).not.to.haveMod('disabled');
        });

        it('value инпута берется из main_domain', function() {
            createBlock();

            expect(input.val()).to.equal('ya.ru');
        });
    });

    describe('События', function() {
        beforeEach(function() {
            createBlock();
            sandbox.spy(block.model, 'syncToDM');
        });

        ['ajaxCheckPassed', 'marketRating', 'domain'].forEach(function(field) {
            it('при изменении поля ' + field + ' вызывается синхронизация с data-model', function() {
                block.model.set(field, !block.model.get(field));
                tick(1000);

                expect(block.model.syncToDM.called).to.be.true;
            });
        });

        describe('При смене домена', function() {
            beforeEach(function() {
                sandbox.spy(u, 'urlCheckAndGetMarketRating');
            });

            it('валидируется модель', function() {
                sandbox.spy(block.model, 'validate');
                input.val('domain', 'vk.com');
                tick(1000);

                expect(block.model.validate.called).to.be.true;
            });

            // Тесты на валидацию модели есть ниже
            it('если модель валидна, вызывается проверка ссылки для непустого домена', function() {
                input.val('vk.com');
                tick(1000);

                expect(u.urlCheckAndGetMarketRating.called).to.be.true;
            });

            it('если модель валидна, но домен пустой, не вызывается проверка ссылки', function() {
                sandbox.stub(block.model, 'validate').callsFake(function() {
                    return { valid: true };
                });

                input.val('');
                tick(1000);

                expect(u.urlCheckAndGetMarketRating.called).to.be.false;
            });

            it('если модель невалидна, не вызывается проверка ссылки', function() {
                input.val('mama');
                tick(1000);

                expect(u.urlCheckAndGetMarketRating.called).to.be.false;
            });
        });
    });

    describe('Ошибки', function() {
        beforeEach(function() {
            createBlock();
        });

        it('Если есть только ошибка в поле domainErrorText, то на элемент domain-message ставится модификатор type error', //eslint-disable-line
            function() {
                block.model.set('domainErrorText', 'ошибонька');

                expect(block).to.haveMod(message, 'type', 'error');
            }
        );

        it('Если есть только ошибка в поле domainErrorText, то в элементе domain-message пишется текст ошибки',
            function() {
                block.model.set('domainErrorText', 'ошибонька');

                expect(message.text()).to.equal('ошибонька');
            }
        );

        it('Если есть только ошибка в поле ajaxErrorText, то на элемент domain-message ставится модификатор type error',
            function() {
                block.model.set('ajaxErrorText', 'ошибонька');

                expect(block).to.haveMod(message, 'type', 'error');
            }
        );

        it('Если есть только ошибка в поле ajaxErrorText, то на элемент domain-message пишется текст ошибки',
            function() {
                block.model.set('ajaxErrorText', 'ошибонька');

                expect(message.text()).to.equal('ошибонька');
            }
        );

        it('Если ajaxCheckPending: true и нет других ошибок, то ставится модификатор pending yes',
            function() {
                block.model.set('ajaxCheckPending', true);

                expect(block).to.haveMod('pending', 'yes');
            }
        );

        it('Если ajaxCheckPending: true и нет других ошибок, то на элемент domain-message ставится модификатор type hint', //eslint-disable-line
            function() {
                block.model.set('ajaxCheckPending', true);

                expect(block).to.haveMod(message, 'type', 'hint');
            }
        );

        it('Если ajaxCheckPending: true и нет других ошибок, то на элемент domain-message пишется текст "Одну секунду, выполняется проверка домена..."', //eslint-disable-line
            function() {
                block.model.set('ajaxCheckPending', true);

                expect(message.text()).to.equal('Одну секунду, выполняется проверка домена...');
            }
        );

        it('Если есть ошибка domainErrorText и ajaxCheckPending: true, то сбрасывается модификатор pending yes',
            function() {
                block.model.set('ajaxCheckPending', true);
                block.model.set('domainErrorText', 'ошибонька');
                
                expect(block).not.to.haveMod('pending', 'yes');
            }
        );

        it('Если есть ошибка ajaxErrorText и ajaxCheckPending, то сбрасывается модификатор pending yes', function() {
            block.model.set('ajaxCheckPending', true);
            block.model.set('ajaxErrorText', 'ошибонька');

            expect(block).not.to.haveMod('pending', 'yes');
        });

        it('Если нет ошибок и ajaxCheckPending: false, то сбрасывается модификатор pending yes',
            function() {
                block.model.set('ajaxErrorText', '');
                block.model.set('domainErrorText', '');
                block.model.set('ajaxCheckPending', false);

                expect(block).not.to.haveMod('pending', 'yes');
            }
        );

        it('Если нет ошибок, то на элементе domain-message ставится модификатор type hidden',
            function() {
                block.model.set('ajaxErrorText', '');
                block.model.set('domainErrorText', '');
                block.model.set('ajaxCheckPending', false);

                expect(block).to.haveMod(message, 'type', 'hidden');
            }
        );

        it('Если нет ошибок, то на элементе domain-message сбрасывается текст',
            function() {
                block.model.set('ajaxErrorText', '');
                block.model.set('domainErrorText', '');
                block.model.set('ajaxCheckPending', false);

                expect(message.text()).to.equal('');
            }
        );
    });

    describe('Валидация ссылки', function() {
        beforeEach(function() {
            createBlock();
        });

        it('если в ссылке есть редирект, появляется ошибка "В поле домен не допускается указание ссылки с редиректом."',
            function() {
                sandbox.server.respondWith([200, {
                    'Content-Type': 'application/json' },
                    JSON.stringify([
                        { requestId: 1 },
                        { requestId: 2 },
                        { requestId: 3 },
                        { requestId: 4, has_redirect: 1 }])
                ]);

                input.val('coloboc.ru');
                tick(1000);
                // второй тик - чтобы отработал debounce на запросе
                tick(1000);
                sandbox.server.respond();

                expect(message.text()).to.equal('В поле домен не допускается указание ссылки с редиректом.');
            }
        );

        it('В начале валидации если домен не пуст, пишется "Одну секунду, выполняется проверка домена..."', function() {
            input.val('medved.ru');
            tick(1000);
            expect(message.text()).to.equal('Одну секунду, выполняется проверка домена...');
        });

        it('В конце валидации текст про проверку домена сбрасывается', function() {
            sandbox.server.respondWith([200, {
                'Content-Type': 'application/json' },
                JSON.stringify([
                    { requestId: 5, code: 1 },
                    { requestId: 6 },
                    { requestId: 7, code: 1 },
                    { requestId: 8, code: 1 }
                ])
            ]);

            input.val('medved.ru');
            tick(1000);
            tick(1000);
            sandbox.server.respond();

            expect(message.text()).to.equal('');
        });

        it('Если бэк вернул ошибку, она пишется в элементе domain-message ', function() {
            sandbox.server.respondWith([200, {
                'Content-Type': 'application/json' },
                JSON.stringify(
                    [{ requestId: 9, text: 'Ошибонька' }, { requestId: 10, text: 'Ошибонька' }]
                )
            ]);

            input.val('volk.ru');
            tick(1000);
            tick(1000);
            sandbox.server.respond();

            expect(message.text()).to.equal('Ошибонька');
        })
    });

    describe('Валидация модели', function() {
        beforeEach(function() {
            createBlock();
        });

        it('Если домен больше MAX_URL_LENGTH символов, то модель невалидна', function() {
            var text = new Array(u.consts('MAX_URL_LENGTH') + 1).join('a') + '.ru';
            input.val(text);
            tick(1000);

            expect(block.model.validate().errors[0].rule).to.equal('maxLength');
        });

        it('если в поле домен введен не урл, то модель невалидна', function() {
            input.val('coconut');
            tick(1000);

            expect(block.model.validate().errors[0].rule).to.equal('isCorrectURL');
        });

        it('Если модель невалидна, то ошибка ставится в поле domainErrorText', function() {
            input.val('coconut');
            tick(1000);

            expect(block.model.get('domainErrorText')).to.equal('Домен указан неверно.');
        });

        it('Если домен меньше 1024 символов и это урл, то модель валидна', function() {
            input.val('coconut.ru');
            tick(1000);

            expect(block.model.validate().valid).to.be.true;
        });

        it('Если модель валидна, сбрасываются ошибки', function() {
            input.val('coconut');
            tick(1000);

            input.val('coconut.ru');
            tick(1000);
            expect(block.model.get('domainErrorText')).to.equal('');
        });
    });

    describe('Методы модели', function() {
        beforeEach(function() {
            createBlock();
        });

        it('При инициализации модели ставится ajaxCheckPassed: true', function() {
            expect(block.model.get('ajaxCheckPassed')).to.be.true;
        });

        it('При изменении поля main_domain data-model-и меняется домен', function() {
            expect(block.model.get('domain')).not.to.equal('tutu.ru');

            group.set('main_domain', 'tutu.ru');

            expect(block.model.get('domain')).to.equal('tutu.ru');
        });

        it('метод syncToDM триггерит событие sync:ok', function() {
            sandbox.spy(block.model, 'trigger');
            block.model.syncToDM();

            expect(block.model.trigger.calledWith('sync:ok')).to.be.true;
        });

        it('Метод prepareDataToDM возвращает корректные данные', function() {
            block.model.update({
                domain: 'nana.ru',
                ajaxCheckPassed: true,
                marketRating: 8
            });

            expect(block.model.prepareDataToDM(block.model.toJSON())).to.deep.equal({
                main_domain: 'nana.ru',
                market_rating: 8,
                isMainDomainValid: true
            });
        });

        it('Метод prepareDataFromDM возвращает корректные данные', function() {
            group.set('main_domain', 'lala.ru');
            group.set('market_rating', '2');

            expect(block.model.prepareDataFromDM(group.toJSON())).to.deep.equal({ domain: 'lala.ru', marketRating: 2 });
        });
    });
});
