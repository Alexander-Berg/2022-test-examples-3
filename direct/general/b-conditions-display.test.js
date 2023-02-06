describe('b-conditions-display', function() {

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
        defaultRetResponse = {"result":[{"id":1369841,"name":"Демо с метрикой","type":"interests","description":"","conditions":[{"type":"or","goals":[{"id":2499000001,"type":"social_demo","name":"Мужчины","classType":null,"parent_id":null,"description":null}]},{"type":"or","goals":[{"id":2499000201,"type":"family","name":"В браке","classType":null,"parent_id":null,"description":null}]},{"type":"or","goals":[{"id":2499000301,"type":"interests","name":"Транспорт","classType":null,"parent_id":null,"description":null}]},{"type":"not","goals":[{"id":32777679,"type":"goal","name":"Кнопка выбрать регион","classType":null,"time":90,"allow_to_use":true,"domain":"mono-card.ru","owner":161951595,"counter_name":"mono-card.ru","counter_id":45407085,"subtype":null,"uploading_source_id":null}]}],"properties":null,"client_id":450488,"is_accessible":1}],"success":true},
        stubCryptaRequest = function(action, response) {
            sandbox.stub(BEM.blocks['i-web-api-request'].crypta, action).callsFake(function() {
                return Promise.resolve(response);
            });
        },
        stubRetRequest = function(action, response) {
            sandbox.stub(BEM.blocks['i-web-api-request'].retargeting, action).callsFake(function() {
                return Promise.resolve(response);
            });
        };

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-conditions-display',
            ulogin: options.ulogin || 'ulogin',
            retCondId: options.retCondId,
            retId: options.retId
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('js', function() {

        before(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });

            stubCryptaRequest('getCryptaSegments', defaultResponse);
            stubRetRequest('getGoals', defaultGoalsResponse);
            stubRetRequest('getConditions', defaultRetResponse);

            window.CONSTS.cryptaSocDemMapping = {
                gender: [2499000001, 2499000002],
                age: [2499000003, 2499000004, 2499000005, 2499000006, 2499000007, 2499000008],
                finance: [2499000009, 2499000010, 2499000011, 2499000012]
            };
        });

        after(function() {
            sandbox.restore();
        });

        describe('При инициализации пустой формы', function() {

            before(function() {
                createBlock();
                return new Promise(function(resolve, reject) {
                    block.on('ready', function() { resolve(); });
                });
            });

            after(function() {
                destructBlock();
            });

            it ('Выполняется запрос на получение сегментов', function() {
                expect(BEM.blocks['i-web-api-request'].crypta.getCryptaSegments.called).to.be.true;
            });

            it ('Создается модель dm-retargeting', function() {
                expect(block._model).to.be.not.undefined;
            });

            describe('Модель имеет значения', function() {

                var values = {
                    type: 'interests'
                };

                Object.keys(values).forEach(function(key) {
                    it(key + ' = ' + values[key], function() {
                        expect(block._model.get(key)).to.be.eq(values[key]);
                    });
                });

                describe('Пустые массивы', function() {

                    it('metrika', function() {
                       expect(block._model.get('metrika').length).to.be.eq(1);
                    });

                    it('social-demo', function() {
                       expect(block._model.get('social-demo').length).to.be.eq(0);
                    });

                    it('family', function() {
                       expect(block._model.get('family').length).to.be.eq(0);
                    });

                    it('interests', function() {
                       expect(block._model.get('interests').length).to.be.eq(0);
                    });
                });


            });

            it ('Создается форма', function() {
                expect(block).to.haveBlock('form', 'b-audience-selection');
            });


        });

        describe('При инициализации формы для редактирования', function() {

            before(function() {
                createBlock({ retCondId: 1369841, retId: 321 });
                return new Promise(function(resolve, reject) {
                    block.on('ready', function() {
                        resolve();
                    });
                });
            });

            after(function() {
                destructBlock();
            });

            it ('Выполняется запрос на получение условия', function() {
                expect(BEM.blocks['i-web-api-request'].retargeting.getConditions.called).to.be.true;
            });

            describe('Модель имеет значения', function() {

                var values = {
                    condition_name: 'Демо с метрикой',
                    type: 'interests',
                    ret_cond_id: 1369841,
                    ret_id: 321
                };

                Object.keys(values).forEach(function(key) {
                    it(key + ' = ' + values[key], function() {
                        expect(block._model.get(key)).to.be.eq(values[key]);
                    });
                });

                describe('metrika', function() {

                    it('В нем один набор правил', function() {
                       expect(block._model.get('metrika').length).to.be.eq(1);
                    });

                    it('с правилом 32777679', function() {
                       expect(block._model.get('metrika')[0].goals[0].id).to.be.eq(32777679);
                    });

                });

                describe('social-demo', function() {

                    it('В нем один набор правил', function() {
                       expect(block._model.get('social-demo').length).to.be.eq(1);
                    });

                    it('с правилом 2499000001', function() {
                       expect(block._model.get('social-demo')[0].goals[0].id).to.be.eq(2499000001);
                    });

                });

                describe('family', function() {

                    it('В нем один набор правил', function() {
                       expect(block._model.get('family').length).to.be.eq(1);
                    });

                    it('с правилом 2499000201', function() {
                       expect(block._model.get('family')[0].goals[0].id).to.be.eq(2499000201);
                    });

                });

                describe('interests', function() {

                    it('В нем один набор правил', function() {
                       expect(block._model.get('interests').length).to.be.eq(1);
                    });

                    it('с правилом 2499000301', function() {
                       expect(block._model.get('interests')[0].goals[0].id).to.be.eq(2499000301);
                    });

                });

            });

        });

    });

});
