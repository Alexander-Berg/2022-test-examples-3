describe('dm-retargeting', function() {
    var model;

    function createModel(data) {
        model = BEM.MODEL.create('dm-retargeting', data);
    }

    function destructModel() {
        model.destruct && model.destruct();
    }

    before(function() {
        window.CONSTS.cryptaSocDemMapping = {
            gender: [2499000001, 2499000002],
            age: [2499000003, 2499000004, 2499000005, 2499000006, 2499000007, 2499000008],
            finance: [2499000009, 2499000010, 2499000011, 2499000012]
        };
    });

    describe('Инициализация', function() {

        describe('Пустой модели', function() {
            var content = {
                ret_cond_id: undefined,
                ret_id: undefined,
                condition_name: '',
                description: '',
                isNegative: false,
                type: 'interests'
            };

            before(function() {
                createModel();
            });

            after(function() {
                destructModel();
            });

            Object.keys(content)
                  .forEach(function(field) {
                      it('Поле ' + field + ' имеет значение ' + content[field], function() {
                          expect(model.get(field)).to.be.eq(content[field]);
                      });
                  });

            it('Поле groups содержит пустой массив', function() {
                expect(model.get('groups').length).to.be.eq(0);
            });

        });

    });

    describe('Вычисление зависимостей', function() {

        var data = {
            ret_cond_id: 1,
            condition_name: 'Какое-то название',
            type: 'interests',
            segments: [
                {
                    type: 'social-demo',
                    segments: [
                        {
                            'id': 2499000003, 'type': 'social_demo', 'name': '<18', 'classType': null,
                            'parent_id': null, 'description': null
                        },
                        {
                            'id': 2499000004, 'type': 'social_demo', 'name': '18-24', 'classType': null,
                            'parent_id': null, 'description': null
                        }, {
                            'id': 2499000005, 'type': 'social_demo', 'name': '25-34', 'classType': null,
                            'parent_id': null, 'description': null
                        }
                    ]
                }
            ]
        };

        beforeEach(function() {
            createModel(data);
        });

        afterEach(function() {
            destructModel();
        });

        describe('groups', function() {

            it('Изначально пустое', function() {
                expect(model.get('groups').length).to.be.eq(0);
            });

            it('Вычисляется если меняется поле social-demo', function() {
                model.set('social-demo', [{ type: 'or', goals: [{ id: 1 }] }]);

                expect(model.get('groups').length).to.be.eq(1);
            });

            it('Вычисляется если меняется поле family', function() {
                model.set('family', [{ type: 'or', goals: [{ id: 1 }] }]);

                expect(model.get('groups').length).to.be.eq(1);
            });

            it('Вычисляется если меняется поле metrika', function() {
                model.set('family', [{ type: 'or', goals: [{ id: 1 }] }]);

                expect(model.get('groups').length).to.be.eq(1);
            });

        });

    });

});
