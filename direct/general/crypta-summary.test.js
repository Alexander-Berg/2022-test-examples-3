describe('crypta-summary', function() {

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
        };

    function createBlock(data) {

        block = u.getInitedBlock({
            block: 'crypta-summary',
            data: data || {},
            segments: segmentsHashResponse,
            goals: goalsHashResponse
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('js', function() {

        before(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });
        });

        after(function() {
            sandbox.restore();
        });

        describe('Сборка сводки', function() {

            afterEach(function() {
                destructBlock();
            });

            it('Пустое состояние', function() {
                createBlock({});

                expect(block).to.not.haveElem('item');
            });

            it('Гео', function() {
                createBlock({ geo: { ids: '255', text: 'Россия' } });

                sandbox.clock.tick(500);

                expect(block).to.haveElem('item', 'type', 'geo');
            });

            it('Размеры баннеров', function() {
                createBlock({ banners: [{ height: 100, width: 100 }] });

                expect(block).to.haveElem('item', 'type', 'banner');
            });

            it('Соцдем', function() {
                createBlock({ interests:[{ type:"or", goals:[{ id:2499000003 }]}] });

                expect(block).to.haveElem('item', 'type', 'social_demo');
            });

            it('Доп. Соцдем', function() {
                createBlock({ interests:[{ type:"or", goals:[{ id:2499000101 }]}] });

                expect(block).to.haveElem('item', 'type', 'family');
            });

            it('Интересы', function() {
                createBlock({ interests:[{ type:"or", goals:[{ id:2499001105 }]}] });

                expect(block).to.haveElem('item', 'type', 'interests');
            });

            it('Аудитории', function() {
                createBlock({ interests:[{ type:"or", goals:[{ type:"goal", id:93798, time:90 }]}] });

                expect(block).to.haveElem('item', 'type', 'metrika');
            });

        });
    });

});
