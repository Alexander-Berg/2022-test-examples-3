describe('b-metrika-counters', function() {
    var block,
        ctx = {
            block: 'b-metrika-counters',
            limit: 100,
            js: {
                dataModel: {
                    id: '8027269',
                    name: 'm-campaign'
                },
                needCheck: true
            }
        },
        model,
        input,
        sandbox,
        createBlock = function(extendedCtx) {
            block = u.createBlock(u._.extend({}, ctx, extendedCtx));
            input = block.findBlockInside('input');
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });
        model = BEM.MODEL.create(ctx.js.dataModel);
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct();
        model.destruct();
    });

    describe('DOM', function() {
        it('Если задан лимит в 1 счетчик, то блок имеет модификатор single', function() {
            createBlock({ limit: 1 });

            expect(block).to.haveMod('single', 'yes');
        });

        it('Если задан лимит в 100 счетчиков, то блок не имеет модификатор single', function() {
            createBlock();

            expect(block).not.to.haveMod('single');
        });

        it('По умолчанию лимит в 100 счетчиков (если не задан)', function() {
            createBlock({ limit: undefined });

            expect(block.getLimit()).to.equal(100);
        });

        it('Если литмит в 1 счетчик, то в плейсхолдере инпута текст "номер счетчика"', function() {
            createBlock({ limit: 1 });

            expect(block.findBlockInside('input').elem('control').attr('placeholder')).to.equal('Номер счётчика');
        });

        it('Если литмит в 100 счетчиков, то в плейсхолдере инпута текст "номера счетчиков"', function() {
            createBlock();

            expect(block.findBlockInside('input').elem('control').attr('placeholder')).to.equal('Номера счётчиков');
        });
    });

    describe('Публичные методы', function() {
        describe('Прочие методы', function() {
            beforeEach(function() {
                createBlock();
            });

            it('Метод updateModel выставляет данные в модель (если она есть)', function() {
                input.val('123456');

                block.updateModel();

                expect(model.get('metrika_counters')).to.equal('123456');
            });

            it('Метод getLimit возвращает лимит счетчиков', function() {
                expect(block.getLimit()).to.equal(100);
            });

            it('Метод getCounters возвращает список счетчиков в строке', function() {
                input.val('123456, 1665656');

                expect(block.getCounters()).to.equal('123456, 1665656');
            });
        });

        describe('Метод prepareToShow', function() {
            it('Если есть модель, записывает в инпут список счетчиков из модели', function() {
                model.set('metrika_counters', '654321');

                createBlock();
                block.prepareToShow();

                expect(input.val()).to.equal('654321');
            });

            it('Если нет модели, пишет в список счетчиков данные из параметров', function() {
                createBlock({ js: { dataModel: undefined, counters: '565656' } });
                block.prepareToShow();

                expect(input.val()).to.equal('565656');
            });

            it('Если нет ни модели, ни данных, оставляет инпут пустыи', function() {
                createBlock({ js: { dataModel: undefined } });
                block.prepareToShow();

                expect(input.val()).to.equal('');
            });

            it('Если нет ни модели, ни данных, триггерит событие state: canSave: false', function() {
                createBlock({ js: { dataModel: undefined } });
                sandbox.spy(block, 'trigger');
                block.prepareToShow();

                expect(block.trigger.calledWith('state', { canSave: false })).to.be.true;
            });
        });
    });

    describe('События: изменение счетчика', function() {
        [1, 100].forEach(function(limit) {
            describe('При лимите в ' + limit + ' счетчиков', function() {
                beforeEach(function() {
                    createBlock({ limit: limit });
                });

                it('Если есть другие символы, кроме цифр, пробела и запятых, пишет ошибку', function() {
                    input.val('abz');

                    expect(block.elem('errors').text()).to.equal(limit > 1 ?
                        'Неверно указаны дополнительные счётчики Метрики' :
                        'Номер счётчика может состоять только из цифр'
                    )
                });

                it('Если счетчиков больше, чем лимит, пишет ошибку', function() {
                    var counters = limit > 1 ?
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                        '123, 456, 678, 789, 543, 211, 321, 322, 323, 324, 111' : '123, 345';

                    input.val(counters);

                    expect(block.elem('errors').text()).to.equal(limit > 1 ?
                        'Можно указать не более 100 дополнительных счётчиков Метрики' :
                        'Можно указать только один дополнительный счётчик Метрики'
                    )
                });
            });
        });

        describe('Для любого лимита', function() {
            beforeEach(function() {
                createBlock();
            });

            it('Новый счетчик записывается возвращается в getCounters', function() {
                input.val('ax');

                expect(block.getCounters()).to.equal('ax');
            });

            it('Если нет ошибок, очищается поле с ошибками', function() {
                input.val('ax');
                expect(block.elem('errors').text()).not.to.equal('');

                input.val('12345');
                expect(block.elem('errors').text()).to.equal('');
            });

            it('Если есть ошибки, триггерится state: canSave: false', function() {
                sandbox.spy(block, 'trigger');
                input.val('абв');

                expect(block.trigger.calledWith('state', { canSave: false })).to.be.true;
            });

            it('Если нет ошибок, триггерится state: canSave: true', function() {
                sandbox.spy(block, 'trigger');
                input.val('12345');

                expect(block.trigger.calledWith('state', { canSave: true })).to.be.true;
            });
        });
    });

    describe('Валидация', function() {
        describe('Клиентская валидация', function() {
            beforeEach(function() {
                // без серверной валидации
                createBlock({ js: { needCheck: false } });
            });

            it('Если есть другие символы, кроме цифр, пробела и запятых, возвращает false', function() {
                input.val('abz');

                expect(block.validate()).to.be.false;
            });

            it('Если счетчиков больше, чем лимит, то возвращает false', function() {
                input.val('123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324,' +
                    '123, 456, 678, 789, 543, 211, 321, 322, 323, 324, 111');

                expect(block.validate()).to.be.false;
            });

            it('В остальных случаях счетчики валидны', function() {
                input.val('123456');

                expect(block.validate()).to.be.true;
            });

            it('Пустая строка валидна', function() {
                input.val('');

                expect(block.validate()).to.be.true;
            });
        });

        it('Если клиентская валидация пройдена, и needCheck: true, запускает серверную', function() {
            createBlock();

            input.val('12345');
            block.validate();

            expect(sandbox.server.requests[0].url)
                .to.equal('/registered/main.pl?cmd=ajaxCheckUserCounters&counter=12345');
        });

        it('Если клиентская валидация пройдена, и needCheck: false, не запускает серверную валидацию', function() {
            createBlock({ js: { needCheck: false } });
            input.val('12345');

            expect(sandbox.server.requests.length).to.equal(0);
        });

        describe('Серверная валидация', function() {
            beforeEach(function() {
                createBlock();
            });

            it('В начале валидации ставится state: canSave: false', function() {
                sandbox.spy(block, 'trigger');
                input.val('12345');

                block.validate();

                expect(block.trigger.calledWith('state', { canSave: false })).to.be.true;
            });

            it('Если ajax не выполнился, отрисовывает ошибку', function() {
                sandbox.server.respondWith([404, {
                    'Content-Type': 'application/json' },
                    '{"result": "123456"}']
                );
                input.val('12345');

                block.validate();
                sandbox.server.respond();

                expect(block.elem('errors').text())
                    .to.equal('Ошибка при проверке возможности указания счётчика метрики');
            });

            it('Если ajax не выполнился, ставится state: canSave:true', function() {
                sandbox.spy(block, 'trigger');
                sandbox.server.respondWith([404, {
                    'Content-Type': 'application/json' },
                    '{"result": "123456"}']
                );
                input.val('12345');

                block.validate();
                sandbox.server.respond();

                expect(block.trigger.calledWith('state', { canSave: true })).to.be.true;
            });

            it('Если проверка вернула ошибку, отрисовывает ошибку', function() {
                sandbox.server.respondWith([200, {
                    'Content-Type': 'application/json' },
                    '{"error": {"message": "Тестовая ошибка"}}']
                );
                input.val('12345');

                block.validate();
                sandbox.server.respond();

                expect(block.elem('errors').text()).to.equal('Тестовая ошибка');
            });

            describe('Ошибка отстутсвия прав', function() {
                it('Если указан 1 счетчик, и на него нет прав, сообщение об ошибке корректно', function() {
                    sandbox.server.respondWith([200, {
                        'Content-Type': 'application/json' },
                        '{"result":{"12345":{"allow":false}}}']
                    );

                    input.val('12345');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.elem('errors').text())
                        .to.equal('Права на указанный счётчик Яндекс.Метрики отсутствуют. Если счётчик принадлежит другому логину, пожалуйста, получите к нему доступ.'); //eslint-disable-line
                });

                it('Если указано 1+ счетчика, и на 1 из них нет прав, сообщение об ошибке корректно', function() {
                    sandbox.server.respondWith([200, {
                        'Content-Type': 'application/json' },
                        '{"result":{"12345":{"allow":false}}}']
                    );

                    input.val('12345, 67890');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.elem('errors').text())
                        .to.equal('Права на один из указанных счётчиков Яндекс.Метрики отсутствуют. Если счётчик 12345 принадлежит другому логину, пожалуйста, получите к нему доступ.'); //eslint-disable-line
                });

                it('Если указано 1+ счетчиков, и на 1+ нет прав, сообщение об ошибке корректно', function() {
                    sandbox.server.respondWith([200, {
                        'Content-Type': 'application/json' },
                        '{"result":{"12345":{"allow":false},"67890":{"allow":false}}}']
                    );

                    input.val('12345, 67890, 55556');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.elem('errors').text())
                        .to.equal('Права на часть указанных счётчиков Яндекс.Метрики отсутствуют. Если счётчики 12345, 67890 принадлежат другому логину, пожалуйста, получите к ним доступ.'); //eslint-disable-line
                });

                it('Если указан 1+ счетчик, и на все нет прав, сообщение об ошибке корректно', function() {
                    sandbox.server.respondWith([200, {
                        'Content-Type': 'application/json' },
                        '{"result":{"12345":{"allow":false},"67890":{"allow":false}}}']
                    );

                    input.val('12345, 67890');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.elem('errors').text())
                        .to.equal('Права на указанные счётчики Яндекс.Метрики отсутствуют. Если счётчики принадлежат другому логину, пожалуйста, получите к ним доступ.'); //eslint-disable-line
                });

                it('Если все ок, не рисует ошибку', function() {
                    sandbox.server.respondWith([200, {
                        'Content-Type': 'application/json' },
                        '{"result":{"12345":{"allow":true,"goals":[]},"67890":{"allow":true}}}']
                    );

                    input.val('12345, 67890');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.elem('errors').text()).to.equal('');
                });
            });

            describe('Успешная валидация', function() {
                beforeEach(function() {
                    sandbox.server.respondWith([200,
                        { 'Content-Type': 'application/json' },
                        '{"result":{"12345":{"allow":true,"goals":[{"goal_id":"3453646"}]},"67890":{"allow":true}}}'
                    ]);
                });

                it('Если все ок, не рисует ошибку', function() {
                    input.val('12345, 67890');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.elem('errors').text()).to.equal('');
                });

                it('Если все ок, обновляет модель', function() {
                    input.val('12345, 67890');

                    block.validate();
                    sandbox.server.respond();

                    expect(model.get('metrika').get('campaign_goals').length()).to.equal(1);
                    expect(model.get('metrika').get('campaign_goals').getByIndex(0).get('goal_id')).to.equal('3453646');
                });

                it('После валидации ставится state: canSave: true', function() {
                    sandbox.spy(block, 'trigger');
                    input.val('12345, 67890');

                    block.validate();
                    sandbox.server.respond();

                    expect(block.trigger.calledWith('state', { canSave: true })).to.be.true;
                });
            });
        });
    });
});
