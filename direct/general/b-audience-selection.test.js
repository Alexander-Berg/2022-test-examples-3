describe('b-audience-selection', function() {

    var block,
        sandbox,
        defaultGoalsResponse = {"result":[{"id":32777679,"type":"goal","name":"Кнопка выбрать регион","classType":null,"time":null,"allow_to_use":true,"domain":"mono-card.ru","owner":161951595,"counter_name":"mono-card.ru","counter_id":45407085,"subtype":null,"uploading_source_id":null}],"success":true},
        defaultResponse = {
            success: 1,
            result: [
                {
                    id: 2499000001,
                    parent_id: 0,
                    name: 'Мужчины',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000002,
                    parent_id: 0,
                    name: 'Женщины',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000003,
                    parent_id: 0,
                    name: 'до 18',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000004,
                    parent_id: 0,
                    name: '18–24',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000009,
                    parent_id: 0,
                    name: 'Низкий',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000010,
                    parent_id: 0,
                    name: 'Средний',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000110,
                    parent_id: 0,
                    name: 'Средний',
                    description: '',
                    type: 'family'
                },
                {
                    id: 2499000210,
                    parent_id: 0,
                    name: 'Средний',
                    description: '',
                    type: 'interests'
                }
            ]
        },
        stubCryptaRequest = function(action, response) {
            sandbox.stub(BEM.blocks['i-web-api-request'].crypta, action)
                .callsFake(function() {
                    return Promise.resolve(response);
                });
        },
        stubRetRequest = function(action, response) {
            sandbox.stub(BEM.blocks['i-web-api-request'].retargeting, action).callsFake(function() {
                return Promise.resolve(response);
            });
        },
        stubCryptaSocDemMapping = {
            gender: [2499000001, 2499000002],
            age: [2499000003, 2499000004, 2499000005, 2499000006, 2499000007, 2499000008],
            finance: [2499000009, 2499000010, 2499000011, 2499000012]
        };

    function initSandbox() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        sandbox.stub(u, 'consts')
            .withArgs('cryptaSocDemMapping').returns(stubCryptaSocDemMapping)
            .withArgs('cryptaNameMaxLength').returns(255);
        stubCryptaRequest('getCryptaSegments', defaultResponse);
        stubRetRequest('getGoals', defaultGoalsResponse);

    }

    function createModel(options) {
        options || (options = {});
        return BEM.MODEL.create(
            { name: 'dm-retargeting', id: options.ret_cond_id },
            {
                ret_cond_id: options.ret_cond_id,
                type: options.type || 'interests',
                condition_name: options.condition_name || 'Новая ауд'
            }
        )
    }

    function createBlock(model) {
        block = u.getInitedBlock({
            block: 'b-audience-selection',
            js: {
                modelName: model.name,
                modelId: model.id
            }
        });
        return new Promise(function (resolve, reject){
            block.findBlockInside('b-segments-group').on('ready', function() {
                resolve();
            })
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('Инициализация', function() {
        var testName = 'Тест имени';

        beforeEach(function() {
            var model = createModel({ condition_name: testName });

            initSandbox();
            return createBlock(model);
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('Должен иметь элемент с именем', function() {
            expect(block).to.haveElem('name');
        });

        it('Имя должно совпадать со значением в модели', function() {
            expect(block.findBlockInside('name', 'b-editable-label').getValue())
                .to.be.eq(testName);
        });

        it('Должена быть группа сегментов Соц.дем', function() {
            expect(block).to.haveBlock({
                block: 'b-segments-group',
                modName: 'type',
                modVal: 'social-demo'
            });
        });

        it('Должена быть группа сегментов Доп.соц.дем', function() {
            expect(block).to.haveBlock({
                block: 'b-segments-group',
                modName: 'type',
                modVal: 'social-demo'
            });
        });

        it('Должена быть группа Метрики', function() {
            expect(block).to.haveBlock({
                block: 'b-segments-group',
                modName: 'type',
                modVal: 'metrika'
            });
        });

    });

    describe('Поведение', function() {

        beforeEach(function() {
            var model = createModel({ });

            initSandbox();
            return createBlock(model);
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        describe('При изменении в интерфейсе', function() {

            it('Имени. Должна обновляется модель', function() {
                var name = 'Новое тестовое имя';

                block.findBlockInside('name', 'b-editable-label').setValue(name);

                expect(block.getModel().get('condition_name'))
                    .to.be.eq(name);
            });

        });

        describe('При изменении модели', function() {

            it('При изменении имени в модели, оно меняется в интерфейсе', function() {
                var name = 'Новое тестовое имя2';

                block.getModel().set('condition_name', name);

                expect(block.findBlockInside('name', 'b-editable-label').getValue())
                    .to.be.eq(name);
            });

        });

    });

    describe('Методы', function() {
        var labelMethodShowError,
            labelMethodHidePopup;

        beforeEach(function() {
            var model = createModel({ }),
                blockPromise;

            initSandbox();
            blockPromise = createBlock(model);
            labelMethodShowError = sinon.spy(block._label, 'showError');
            labelMethodHidePopup = sinon.spy(block._label, 'hidePopup');
            return blockPromise;
        });

        afterEach(function() {
            sandbox.restore();
            labelMethodShowError.restore();
            labelMethodHidePopup.restore();
            destructBlock();
        });

        it('showErrors. Должен показать ошибку', function() {
            block._model.set('condition_name', '');
            block.showErrors();

            expect(labelMethodShowError.called).to.be.true;
        });


        it('clearErrors. Должен спрятать ошибку', function() {
            block.clearErrors();

            expect(labelMethodHidePopup.called).to.be.true;
        });

    });

    describe('Вычисление открытой группы', function() {

        beforeEach(function() {
            initSandbox();
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        describe('соц.дем', function(){
            beforeEach(function() {
                var model = createModel({ });

                return createBlock(model);
            });

            it('По умолчанию открыт ', function() {
                expect(block.findBlockInside({
                    block: 'b-segments-group',
                    modName: 'type',
                    modVal: 'social-demo'
                })).to.haveMod('open', 'yes');
            });
        });

        describe('доп.соц.дем', function(){
            beforeEach(function() {
                var model = createModel({ });

                model.set('family', [{ type: 'or', goals: [{ id: 1 }] }]);
                return createBlock(model);
            });

            it('Если доп.соц.дем выбран, а соц.дем нет то он открыт', function() {
                expect(block.findBlockInside({
                    block: 'b-segments-group',
                    modName: 'type',
                    modVal: 'family'
                })).to.haveMod('open', 'yes');
            });
        });

        describe('метрика', function(){
            beforeEach(function() {
                var model = createModel({ });

                model.set('metrika', [{ type: 'or', goals: [{ type: 'goal', id: 1 }] }]);
                return createBlock(model);
            });

            it('Если цели метрики выбраны, а остальне нет, то метрика открыта', function() {
                expect(block.findBlockInside({
                    block: 'b-segments-group',
                    modName: 'type',
                    modVal: 'metrika'
                })).to.haveMod('open', 'yes');
            });
        });

        describe('интересы', function(){
            beforeEach(function() {
                var model = createModel({ });

                model.set('interests', [{ type: 'or', goals: [{ id: 1 }] }]);
                return createBlock(model);
            });

            it('Если интересы выбраны, а остальне нет, то интересы открыта', function() {
                expect(block.findBlockInside({
                    block: 'b-segments-group',
                    modName: 'type',
                    modVal: 'interests'
                })).to.haveMod('open', 'yes');
            });
        });
    });
});
