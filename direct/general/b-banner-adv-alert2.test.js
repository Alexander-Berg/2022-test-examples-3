describe('b-banner-adv-alert2', function() {

    describe('Содержание блока в зависимости от входных данных', function() {

        var sandbox,
            block,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create();
            constStub = sandbox.stub(u, 'consts');

            constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('Должен содержать элемент wargnings', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1
                }
            }, true);

            expect(block.findElem('warnings').length > 0).to.be.true;
        });

        it('Должен содержать ссылку на изменение предупреждений can:{addRemove:true}', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1
                },
                can: { // Возможности пользователя
                    addRemove: true
                }
            }, true);

            expect(block.findElem('link', 'action', 'change').length > 0).to.be.true;
        });

        it('Должен содержать кнопку на скрытие панели предупреждений can:{addRemove:true}', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1
                },
                can: { // Возможности пользователя
                    addRemove: true
                }
            }, true);

            expect(block.findElem('link', 'action', 'hide').length > 0).to.be.true;
        });

        it('Не должен содержать ссылку на изменение предупреждений can:{addRemove:false}', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1
                },
                can: { // Возможности пользователя
                    addRemove: false
                }
            }, true);

            expect(block.findElem('link', 'action', 'change').length > 0).to.be.false;
        });

        it('Не должен содержать кнопку на скрытие панели предупреждений can:{addRemove:false}', function() {
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1
                },
                can: { // Возможности пользователя
                    addRemove: false
                }
            }, true);

            expect(block.findElem('link', 'action', 'hide').length > 0).to.be.false;
        });

    });

    describe('Содержание элемента warnings в зависимости от входных данных', function() {

        var sandbox,
            block,
            constStub;

        beforeEach(
            function() {
                sandbox = sinon.sandbox.create();
                constStub = sandbox.stub(u, 'consts');

                constStub.withArgs('AD_WARNINGS').returns(
                    {
                        "project_declaration": {
                            "long_text": "Проектная декларация на рекламируемом сайте",
                            "short_text": "Проектная декларация"
                        },
                        "med_services": {
                            "short_text": "мед. услуги",
                            "parent": "medicine",
                            "long_text": "Имеются противопоказания. Посоветуйтесь с врачом"
                        },
                        "med_equipment": {
                            "long_text": "Имеются противопоказания. Посоветуйтесь с врачом",
                            "parent": "medicine",
                            "short_text": "мед. оборудование"
                        },
                        "pharmacy": {
                            "long_text": "Имеются противопоказания. Посоветуйтесь с врачом",
                            "short_text": "лекарства",
                            "parent": "medicine"
                        },
                        "pseudoweapon": {
                            "long_text": "Конструктивно сходные с оружием изделия",
                            "short_text": "не оружие"
                        },
                        "abortion": {
                            "long_text": "Есть противопоказания. Посоветуйтесь с врачом. Возможен вред здоровью.",
                            "short_text": "аборты"
                        },
                        "baby_food": {
                            "long_text": "Проконсультируйтесь со специалистом. Для питания детей с %d месяцев",
                            "short_text": "детское питание",
                            "postfix": "months",
                            "variants": [12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0],
                            "age_limit": "1",
                            "default": "11"
                        },
                        "dietarysuppl": {
                            "short_text": "БАД",
                            "long_text": "Не является лекарством"
                        },
                        "alcohol": {
                            "long_text": "Чрезмерное потребление вредно.",
                            "short_text": "алкоголь"
                        },
                        "age": {
                            "variants": [18, 16, 12, 6, 0],
                            "default": 18,
                            "is_common_warn": true
                        }
                    });
            });

        afterEach(
            function() {
                block.destruct();
                sandbox.restore();
            });

        it('Содержит елемент warn', function() {
            block = u.getInitedBlock(
                {
                    block: 'b-banner-adv-alert2',
                    value: {
                        alcohol: 1
                    }
                }, false);

            expect(block.elem('warn').length).to.be.gt(0);
        });

        it('Содержит елемент warn с текстом из AD_WARNINGS', function() {
            block = u.getInitedBlock(
                {
                    block: 'b-banner-adv-alert2',
                    value: {
                        alcohol: 1
                    }
                }, false);

            expect(block.elem('warn').html()).to.be.eq(u.consts('AD_WARNINGS').alcohol.long_text);
        });

        it('Содержит несколько елементов warn', function() {
            block = u.getInitedBlock(
                {
                    block: 'b-banner-adv-alert2',
                    value: {
                        alcohol: 1,
                        med_equipment: 1
                    }
                }, false);

            expect(block.elem('warn').length).to.be.eq(2);
        });

        it('Не содержит елемент warn, если он общий', function() {
            block = u.getInitedBlock(
                {
                    block: 'b-banner-adv-alert2',
                    value: {
                        age: 18
                    }
                }, false);

            expect(block.elem('warn').length).to.be.eq(0);
        });

        it('Содержит елемент baby-food, если он есть в value и пользователь может редактировать изменения',
            function() {
                block = u.getInitedBlock(
                    {
                        block: 'b-banner-adv-alert2',
                        value: {
                            baby_food: '10'
                        },
                        can: {
                            edit: true
                        }
                    }, false);

                expect(block.elem('baby-food').length).to.be.gt(0);
            }
        );

        it('Не содержит елемент baby-food, если он есть в value и пользователь не может его менять',
            function() {
                block = u.getInitedBlock(
                    {
                        block: 'b-banner-adv-alert2',
                        value: {
                            baby_food: '10'
                        }
                    }, false);

                expect(block.elem('baby-food').length).to.be.eq(0);
            }
         );

    });

    describe('Методы i-bem', function() {
        var sandbox,
            block,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({
                useFakeTimers: true,
                useFakeServer: true
            });
            constStub = sandbox.stub(u, 'consts');

            constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
            block = u.getInitedBlock({
                block: 'b-banner-adv-alert2',
                value: {
                    alcohol: 1,
                    med_services: 1
                },
                can: { // Возможности пользователя
                    addRemove: true, // Добавлять или удалять предупреждения
                    edit: true // Редактировать выставленные флаги
                }
            }, false);
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        describe('changeValue', function() {

            it('Должен выставить модификатор loading_yes', function() {
                block.changeValue('abortion', 1);
                expect(block.hasMod('loading','yes')).to.be.true;
            });

            it('Должен вызвать метод trigger, c параметром flags-change', function() {

                sandbox.server.respondWith([200, { "Content-Type": "application/json" },
                            '{"success": 1}']);
                sandbox.spy(block, 'trigger');
                block.changeValue('abortion', 1);
                sandbox.server.respond();
                expect(block).to.triggerEvent('flags-change');
            });

            it('Должен показать alert с ошибкой', function() {
                sandbox.stub(BEM.blocks['b-confirm'], 'alert');

                sandbox.server.respondWith([200, { "Content-Type": "application/json" },
                            '{"success": 0}']);

                block.changeValue('abortion', 1);
                sandbox.server.respond();

                expect(BEM.blocks['b-confirm'].alert.called).to.be.true;
            });


        });

        describe('Live методы', function() {
            it('Должен показать панель изменения предупреждений', function() {
                $('.b-banner-adv-alert2__link_action_change').click();
                sandbox.clock.tick(500);
                expect($('.b-banner-adv-alert2__edit-panel').length).to.be.eq(1);
            });

            it('Должен выставить модификатор edit-panel-visible блоку', function() {
                $('.b-banner-adv-alert2__link_action_change').click();
                sandbox.clock.tick(500);
                expect(block).to.haveMod('edit-panel-visible', 'yes');
            });

            it('Должен показать панель изменения предупреждений', function() {
                // сначала ее показываем
                $('.b-banner-adv-alert2__link_action_change').click();
                sandbox.clock.tick(500);
                expect($('.b-banner-adv-alert2__edit-panel').length).to.be.eq(1);
            });

            it('Должен скрыть панель изменения предупреждений', function() {
                // сначала ее показываем
                $('.b-banner-adv-alert2__link_action_change').click();
                sandbox.clock.tick(500);
                // скрываем
                $('.b-banner-adv-alert2__link_action_hide').click();
                sandbox.clock.tick(500);
                expect(block).to.not.haveMod('edit-panel-visible');
            });

            it('Должен вызвать метод changeValue, по нажатию на чекбокс', function() {
                // Показываем панель
                sandbox.stub(block, 'changeValue').callsFake(function() {});
                $('.b-banner-adv-alert2__link_action_change').click();
                sandbox.clock.tick(500);
                block.findBlockInside({ block: 'checkbox', modName: 'name', modVal: 'abortion' }).domElem.click();
                sandbox.clock.tick(500);

                expect(block.changeValue.calledWith('abortion', 1)).to.be.true;
            });

        })
    });
});
