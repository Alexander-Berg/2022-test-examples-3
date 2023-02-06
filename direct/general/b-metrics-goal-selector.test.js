describe('b-metrics-goal-selector', function() {
    var goals = {
            '1': {
                id: '1',
                domain: 'www.ya.ru',
                name: 'Ооооооооченььььььь ддддддлииииннннаааааааааааяяяяяяяя Цель 1',
                status: 'Active'
            },
            '2': {
                id: '2',
                domain: 'www.ya.ru',
                name: 'Цель 2',
                status: 'Active'
            },
            '3': {
                id: '3',
                name: 'Цель 3',
                status: 'Active'
            },
            '4': {
                id: '4',
                domain: 'www.ya.ru',
                name: 'Цель 4',
                status: 'Active'
            },
            '5': {
                id: '5',
                domain: 'www.ya.ru',
                status: 'Deleted'
            },
            '6': {
                id: '6',
                domain: 'www.ya.ru',
                name: 'Цель 6',
                status: 'Active'
            },
            '7': {
                id: '7',
                domain: 'www.ya.ru',
                name: 'Цель 7',
                status: 'Active'
            },
            '8': {
                id: '8',
                domain: 'www.ya.ru',
                name: 'Цель 8',
                status: 'Active'
            },
            '9': {
                id: '9',
                domain: 'www.ya.ru',
                name: 'Цель 9',
                status: 'Active'
            },
            '10': {
                id: '10',
                domain: 'www.ya.ru',
                name: 'Цель 10',
                status: 'Active'
            },
            '11': {
                id: '11',
                domain: 'www.ya.ru',
                name: 'Цель 11',
                status: 'Active'
            },
            '12': {
                id: '12',
                domain: 'www.ya.ru',
                name: 'Цель 12',
                status: 'Active'
            }
        },
        getChooser = function(block) {
            return block.findBlockOn('chooser', 'b-chooser')
        },
        getBlockDisabledGoalsIds = function(block) {
            return getChooser(block).getAll().reduce(function(res, chooserItem) {
                if (chooserItem.disabled) {
                    res.push(chooserItem.name);
                }

                return res;
            }, []);
        },
        createSandbox = function() {
            return sinon.sandbox.create({
                useFakeTimers: true
            });
        },
        createBlock = function(options) {
            return u.createBlock(
                u._.extend({ block: 'b-metrics-goal-selector' }, options || {}),
                { inject: true }
            );
        };

    describe('Инициализация', function() {
        it('цель не задана -> кнопка открытия селектора устанавливлена в неактивное состояние', function() {
            var block = createBlock({
                    goals: goals
                });

            expect(block.getMod(block.elem('opener'),'selected')).to.equal('no');

            block.destruct();
        });

        it('цель не задана -> текст на кнопке открытия селектора "выберите цель"', function() {
            var block = createBlock({
                    goals: goals
                });

            expect(block.elem('opener').text()).to.equal('выберите цель');

            block.destruct();
        });

        it('цель задана -> кнопка открытия селектора установлена в активное состояние', function() {
            var block = createBlock({
                    goals: goals,
                    selectedGoalId: '2'
                });

            expect(block.getMod(block.elem('opener'),'selected')).to.equal('yes');

            block.destruct();
        });

        it('цель задана, домен для цели задан -> текст на кнопке открытия селектора: домен :: имя_цели', function() {
            var block = createBlock({
                    goals: goals,
                    selectedGoalId: '2'
                });

            expect(block.elem('opener').text()).to.equal('www.ya.ru :: Цель 2');

            block.destruct();
        });

        it('цель задана, домен для цели не задан -> текст на кнопке открытия селектора: имя_цели', function() {
            var block = createBlock({
                    goals: goals,
                    selectedGoalId: '3'
                });

            expect(block.elem('opener').text()).to.equal('Цель 3');

            block.destruct();
        });

        it('задана удаленная цель и у нее нет имени -> текст на кнопке открытия селектора: -(id: идентификатор_цели)', function() {
            var block = createBlock({
                goals: goals,
                selectedGoalId: '5'
            });

            expect(block.elem('opener').text()).to.equal('— (id: 5)');

            block.destruct();
        });

        it('общее количество целей > 10 -> показываем поле для поиска в селекторе', function() {
            var block = createBlock({
                    goals: goals
                });

            expect(block.elem('search').length).to.equal(1);

            block.destruct();
        });

        it('общее количество целей <= 10 -> поле для поиска в селекторе недоступно', function() {
            var block = createBlock({
                    goals: {
                        '1': {
                            id: '1',
                            domain: 'www.ya.ru',
                            name: 'Ооооооооченььььььь ддддддлииииннннаааааааааааяяяяяяяя Цель 1',
                            status: 'Active'
                        }
                    }
                });

            expect(block.elem('search').length).to.equal(0);

            block.destruct();
        });

        it('в параметрах указаны недоступные для выбора цели -> цели не доступны для выбора в селекторе', function() {
            var block = createBlock({
                    goals: goals,
                    disabledGoalsIds: ['1', '3']
                });

            expect(getBlockDisabledGoalsIds(block)).to.deep.equal(['1', '3']);

            block.destruct();
        });
    });

    describe('При изменении выбора цели', function() {
        it('всегда -> тригеррится событие change c корректным goalId', function() {
            var block = createBlock({
                    goals: goals
                }),
                sandbox = createSandbox();

            sandbox.spy(block, 'trigger');

            getChooser(block).trigger('select-item', {
                name: '1',
                text: 'Ооооооооченььььььь ддддддлииииннннаааааааааааяяяяяяяя Цель 1'
            });

            expect(block.trigger.calledWith('change', {
                goalId: '1'
            }));

            block.destruct();
            sandbox.restore();
        });

        it('всегда -> кнопка открытия селектора устанавливается в активное состояние', function() {
            var block = createBlock({
                    goals: goals
                });

            getChooser(block).trigger('select-item', {
                name: '1',
                text: 'Ооооооооченььььььь ддддддлииииннннаааааааааааяяяяяяяя Цель 1'
            });

            expect(block.getMod(block.elem('opener'),'selected')).to.equal('yes');

            block.destruct();
        });

        it('всегда -> текст на кнопке открытия селектора соответсвует имени выбранной цели', function() {
            var block = createBlock({
                    goals: goals
                });

            getChooser(block).trigger('select-item', {
                name: '1',
                text: 'Цель 1'
            });

            expect(block.elem('opener').text()).to.equal('Цель 1');

            block.destruct();
        });
    });

    it('Вызов метода updateGoalsAvailability -> корректно обновляются цели не доступные для выбора в селекторе', function() {
        var block = createBlock({
                goals: goals,
                disabledGoalsIds: ['1', '3']
            });

        block.updateGoalsAvailability(['2']);

        expect(getBlockDisabledGoalsIds(block)).to.deep.equal(['2']);

        block.destruct();
    })
});
