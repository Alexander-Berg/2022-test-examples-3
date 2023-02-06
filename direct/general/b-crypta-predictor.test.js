describe('b-crypta-predictor', function() {

    var block,
        sandbox,
        goalsHashResponse = {
            "93798": {
                "id": 93798,
                "type": "goal",
                "name": "Контакты",
                "classType": null,
                "time": null,
                "allow_to_use": true,
                "domain": "www.clewear.ru",
                "owner": 567196,
                "counter_name": "",
                "counter_id": 248303,
                "subtype": null,
                "uploading_source_id": null
            },
            "93799": {
                "id": 93799,
                "type": "goal",
                "name": "Смотрибельность",
                "classType": null,
                "time": null,
                "allow_to_use": true,
                "domain": "www.clewear.ru",
                "owner": 567196,
                "counter_name": "",
                "counter_id": 248303,
                "subtype": null,
                "uploading_source_id": null
            },
            "129253": {
                "id": 129253,
                "type": "goal",
                "name": "Цель №1",
                "classType": null,
                "time": null,
                "allow_to_use": true,
                "domain": "beletag.com",
                "owner": 567196,
                "counter_name": "Бельетаж",
                "counter_id": 518867,
                "subtype": null,
                "uploading_source_id": null
            }
        },
        segmentsHashResponse = {
            "2499000001": {
                "id": 2499000001,
                "type": "social_demo",
                "name": "Мужчины",
                "classType": null,
                "parent_id": 2499000021,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000002": {
                "id": 2499000002,
                "type": "social_demo",
                "name": "Женщины",
                "classType": null,
                "parent_id": 2499000021,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000003": {
                "id": 2499000003,
                "type": "social_demo",
                "name": "<18",
                "classType": null,
                "parent_id": 2499000022,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000004": {
                "id": 2499000004,
                "type": "social_demo",
                "name": "18-24",
                "classType": null,
                "parent_id": 2499000022,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000005": {
                "id": 2499000005,
                "type": "social_demo",
                "name": "25-34",
                "classType": null,
                "parent_id": 2499000022,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000006": {
                "id": 2499000006,
                "type": "social_demo",
                "name": "35-44",
                "classType": null,
                "parent_id": 2499000022,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000007": {
                "id": 2499000007,
                "type": "social_demo",
                "name": "45-54",
                "classType": null,
                "parent_id": 2499000022,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000008": {
                "id": 2499000008,
                "type": "social_demo",
                "name": "55+",
                "classType": null,
                "parent_id": 2499000022,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000009": {
                "id": 2499000009,
                "type": "social_demo",
                "name": "Низкий",
                "classType": null,
                "parent_id": 2499000023,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000010": {
                "id": 2499000010,
                "type": "social_demo",
                "name": "Средний",
                "classType": null,
                "parent_id": 2499000023,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000012": {
                "id": 2499000012,
                "type": "social_demo",
                "name": "Высокий",
                "classType": null,
                "parent_id": 2499000023,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000013": {
                "id": 2499000013,
                "type": "social_demo",
                "name": "Премиум",
                "classType": null,
                "parent_id": 2499000023,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000021": {
                "id": 2499000021,
                "type": "social_demo",
                "name": "Пол",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000022": {
                "id": 2499000022,
                "type": "social_demo",
                "name": "Возраст",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000023": {
                "id": 2499000023,
                "type": "social_demo",
                "name": "Доход",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000100": {
                "id": 2499000100,
                "type": "family",
                "name": "Семейное положение",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": null
            },
            "2499000101": {
                "id": 2499000101,
                "type": "family",
                "name": "Состоят в браке",
                "classType": null,
                "parent_id": 2499000100,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000102": {
                "id": 2499000102,
                "type": "family",
                "name": "Не состоят в браке",
                "classType": null,
                "parent_id": 2499000100,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000110": {
                "id": 2499000110,
                "type": "family",
                "name": "Наличие детей",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": null
            },
            "2499000111": {
                "id": 2499000111,
                "type": "family",
                "name": "Планируют беременность или ждут рождение ребенка",
                "classType": null,
                "parent_id": 2499000110,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000112": {
                "id": 2499000112,
                "type": "family",
                "name": "Есть дети до 1 года",
                "classType": null,
                "parent_id": 2499000110,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000113": {
                "id": 2499000113,
                "type": "family",
                "name": "Есть дети 1–3 лет",
                "classType": null,
                "parent_id": 2499000110,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000114": {
                "id": 2499000114,
                "type": "family",
                "name": "Есть дети 3-6 лет",
                "classType": null,
                "parent_id": 2499000110,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000115": {
                "id": 2499000115,
                "type": "family",
                "name": "Есть дети 6-11 лет",
                "classType": null,
                "parent_id": 2499000110,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000116": {
                "id": 2499000116,
                "type": "family",
                "name": "Есть дети 11-16 лет",
                "classType": null,
                "parent_id": 2499000110,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000120": {
                "id": 2499000120,
                "type": "family",
                "name": "Профессии",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": null
            },
            "2499000121": {
                "id": 2499000121,
                "type": "family",
                "name": "Таксисты",
                "classType": null,
                "parent_id": 2499000120,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000122": {
                "id": 2499000122,
                "type": "family",
                "name": "Маркетологи",
                "classType": null,
                "parent_id": 2499000120,
                "description": null,
                "interest_type": "long_term"
            },
            "2499000123": {
                "id": 2499000123,
                "type": "family",
                "name": "Дизайнеры",
                "classType": null,
                "parent_id": 2499000120,
                "description": null,
                "interest_type": "long_term"
            },
            "2499001105": {
                "id": 2499001105,
                "type": "interests",
                "name": "Бытовая техника",
                "classType": null,
                "parent_id": 0,
                "description": null,
                "interest_type": "all"
            }
        },
        reachResponses200 = {
            success: {"result":{"requestId":"SuccessRequestId","basic":{"reach_less_than":null,"reach":78049000,"errors":null},"detailed":{"reach_less_than":null,"reach":35702750,"errors":null}},"success":true},
            basicLess: {"result":{"requestId":"BasicLessRequestId","basic":{"reach_less_than":5000,"reach":null,"errors":null},"detailed":null},"success":true},
            detailedLess: {"result":{"requestId":"DetailedLessRequestId","basic":{"reach_less_than":null,"reach":39000,"errors":null},"detailed":{"reach_less_than":5000,"reach":null,"errors":null}},"success":true}
        },
        reachResponses501 = {
            detailedError: { status: 501, obj: {"result":{"requestId":"DetailedErrorRequestId","basic":{"reach_less_than":null,"reach":78049000,"errors":null},"detailed":{"reach_less_than":null,"reach":null,"errors":[{"goal_id":2499001126,"type":"UNKNOWN_SEGMENTS"}]}},"success":true}},
        };

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-crypta-predictor'
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('js', function() {
        var stubSegments, subGoals;

        before(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });
            stubSegments = sandbox.stub(BEM.blocks['i-crypta-segments-data'], 'getSegmentsHash')
                .resolves(segmentsHashResponse);
            subGoals = sandbox.stub(BEM.blocks['i-retargeting-goals-data'], 'getGoalsHash')
                .resolves(goalsHashResponse);
        });

        after(function() {
            sandbox.restore();
        });

        function testNumber(name, status, type) {
            var text = {
                detailed: 'Левая',
                basic: 'Правая'
            }[name];

            it(text + ' цифра со статусом ' + status, function() {
                block.update();

                expect(block.findElem(block.elem(name), 'predict-value', 'status', status).length).to.be.eq(1);
            });

            type && it(text + ' цифра с типом ' + type, function() {
                block.update();

                expect(block.findElem(block.elem(name), 'number', 'type', type).length).to.be.eq(1);
            });
        }

        describe('Успешный ответ', function() {
            var reachStub;

            beforeEach(function() {
                reachStub = sinon
                    .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReach')
                    .resolves(reachResponses200.success);

                createBlock();
                return new Promise(function(resolve, reject) {
                    block.update();
                    block.on('ready', function() { resolve(); });
                });
            });

            afterEach(function() {
                reachStub.restore();
                destructBlock();
            });

            testNumber('detailed', 'normal', 'of');
            testNumber('basic', 'normal', 'out-of');

            it('Подсказка содержит верный requestId', function() {
                expect(
                    block
                        .blockInside('help', 'b-hintable-popup')
                        ._hint
                        .indexOf(reachResponses200.success.result.requestId) != -1
                ).to.be.true;
            });

        });

        describe('Правая цифра очень маленькая', function() {
            var reachStub;

            beforeEach(function() {
                reachStub = sinon
                    .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReach')
                    .resolves(reachResponses200.basicLess);

                createBlock();
                return new Promise(function(resolve, reject) {
                    block.update();
                    block.on('ready', function() { resolve(); });
                });
            });

            afterEach(function() {
                reachStub.restore();
                destructBlock();
            });

            testNumber('detailed', 'empty', null);
            testNumber('basic', 'less', 'of');

            it('Подсказка содержит верный requestId', function() {
                expect(
                    block
                        .blockInside('help', 'b-hintable-popup')
                        ._hint
                        .indexOf(reachResponses200.basicLess.result.requestId) != -1
                ).to.be.true;
            });

        });

        describe('Левая цифра очень маленькая', function() {
            var reachStub;

            beforeEach(function() {
                reachStub = sinon
                    .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReach')
                    .resolves(reachResponses200.detailedLess);

                createBlock();
                return new Promise(function(resolve, reject) {
                    block.update();
                    block.on('ready', function() { resolve(); });
                });
            });

            afterEach(function() {
                reachStub.restore();
                destructBlock();
            });

            testNumber('detailed', 'less', 'of');
            testNumber('basic', 'normal', 'out-of');

            it('Подсказка содержит верный requestId', function() {
                expect(
                    block
                        .blockInside('help', 'b-hintable-popup')
                        ._hint
                        .indexOf(reachResponses200.detailedLess.result.requestId) != -1
                ).to.be.true;
            });

        });

        describe('Для левой цифры невозможно сделать прогноз', function() {
            var reachStub;

            beforeEach(function() {
                reachStub = sinon
                    .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReach')
                    .rejects(reachResponses501.detailedError);

                createBlock();
                return new Promise(function(resolve, reject) {
                    block.update();
                    block.on('ready', function() { resolve(); });
                });
            });

            afterEach(function() {
                reachStub.restore();
                destructBlock();
            });

            testNumber('detailed', 'error', null);
            testNumber('basic', 'normal', 'of');

            it('Подсказка содержит верный requestId', function() {
                expect(
                    block
                        .blockInside('help', 'b-hintable-popup')
                        ._hint
                        .indexOf(reachResponses501.detailedError.obj.result.requestId) != -1
                ).to.be.true;
            });

        });

        describe('Сервер вернул 504', function() {
            var reachStub;

            beforeEach(function() {
                reachStub = sinon
                    .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReach')
                    .rejects({ status: 504, obj: { code: 504, text: 'error-text', requestId: 'RequestId504' } });

                createBlock();
                return new Promise(function(resolve, reject) {
                    block.update();
                    block.on('ready', function() { resolve(); });
                });
            });

            afterEach(function() {
                reachStub.restore();
                destructBlock();
            });

            testNumber('basic', 'error', null);

            it('Подсказка содержит верный requestId', function() {
                expect(
                    block
                        .blockInside('help', 'b-hintable-popup')
                        ._hint
                        .indexOf('RequestId504') != -1
                ).to.be.true;
            });

            it('Блок содержит текст ошибки из ответа сервера', function() {
                expect( block.findElem('error').text()).to.be.eq('error-text');
            });

        });


    });

});
