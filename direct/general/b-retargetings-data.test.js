describe('b-retargetings-data', function() {
    var metrikaGoalsList = [
            //недоступная цель
            {
                id: 4000292098,
                name: 'посетил сайт',
                counter_id: 99414,
                type: 'goal',
                allow_to_use: 0,
                domain: 'direct.yandex.ru'
            },
            //доступная цель
            {
                id: 4000099414,
                name: 'посетил сайт',
                counter_id: 292098,
                time: 15,
                type: 'goal',
                allow_to_use: 1,
                domain: 'wordstat.yandex.ru'
            }
        ],
        metrikaSegmentsList = [
            {
                owner : 2644954,
                type : 'segment',
                domain : 'wordstat.yandex.ru',
                counter_id : 292098,
                id : 1000006260,
                counter_name : '',
                name : 'Посетители 1',
                allow_to_use : 0
            },
            {
                owner : 26449545,
                type : 'segment',
                domain : 'wordstat.yandex.ru',
                counter_id : 292098,
                id : 1000006261,
                counter_name : '',
                name : 'Посетители 2',
                allow_to_use : 1
            },
            {
                owner : 26449545,
                type : 'segment',
                domain : 'wordstat.yandex.ru',
                counter_id : 292098,
                id : 1000006269,
                counter_name : '',
                name : 'Посетители 3',
                allow_to_use : 1
            }
        ],
        audienceSegmentsList = [
            {
                allow_to_use : 0,
                name : 'nn',
                type : 'audience',
                owner : 198415469,
                counter_name: '',
                id: 2000043975,
                counter_id: 0,
                domain: ''
            },
            {
                allow_to_use : 1,
                name : 'nn',
                type : 'audience',
                owner : 198415469,
                counter_name: '',
                id: 2000043976,
                counter_id: 0,
                domain: ''
            }
        ],
        sandbox,
        result,
        allGoalsList = [].concat(metrikaGoalsList).concat(metrikaSegmentsList).concat(audienceSegmentsList);

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Тесты на BEM.blocks[\'b-retargetings-data\']', function() {
        [false, true].forEach(function(isAudienceEnabled) {
            describe('Если для клиента ' + (isAudienceEnabled ? '' : ' не ') + 'доступны Я.Аудитории', function() {
                beforeEach(function() {
                    sandbox.stub(u, 'consts').callsFake(function(name) {
                        switch (name) {
                            case 'is_audience_enabled':
                                return isAudienceEnabled;
                        }
                    });
                });

                it('Результат работы u[\'b-retargetings-data\'].formatGoalList().lists ' + (isAudienceEnabled ? '' : ' не ') + 'будет содержать audience', function() {
                    var result = u['b-retargetings-data'].formatGoalList(allGoalsList);

                    isAudienceEnabled ?
                        expect(result.lists.audience).to.not.be.undefined :
                        expect(result.lists.audience).to.be.undefined;

                });
            });
        });

        describe('Работа метода formatGoalList, содержимое result', function() {
            before(function() {
                result = u['b-retargetings-data'].formatGoalList(allGoalsList);
            });

            it('Цели в result.lists, которые недоступны, должны быть отфильтрованы вниз списка', function() {
                expect(result.lists.goal[0].id).to.be.equal(metrikaGoalsList[1].id);
                expect(result.lists.goal[1].id).to.be.equal(metrikaGoalsList[0].id);
            });

            it('В result.available должны содержаться id доступных целей', function() {
                expect(result.available).to.eql([4000099414, 1000006261, 1000006269]);
            });

            it('В result.notAvailable должны содержаться id недоступных целей', function() {
                expect(result.notAvailable).to.eql([4000292098, 1000006260]);
            });
        });
    });


    describe('Тесты на u[\'b-retargetings-data\']', function() {
        var preparedGoalsList;

        beforeEach(function() {
            sandbox.stub(u, 'consts').callsFake(function(name) {
                switch (name) {
                    case 'is_audience_enabled':
                        return true;
                }
            });
        });


        describe('Метод getGoalsTypesList', function() {
            it('Если есть цели по goal, segment и audience, то типы показываются в порядке => [goal, segment, audience]', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList(allGoalsList);

                expect(u['b-retargetings-data'].getGoalsTypesList(preparedGoalsList)).to.eql(['goal', 'segment', 'audience']);
            });

            it('Если есть цели по segment и audience, но нет по goal то типы показываются в порядке => [segment, audience, goal]', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(metrikaSegmentsList).concat(audienceSegmentsList));

                expect(u['b-retargetings-data'].getGoalsTypesList(preparedGoalsList)).to.eql(['segment', 'audience', 'goal']);
            });

            it('Если есть цели по goal и audience, но нет по segment то типы показываются в порядке => [goal, audience, segment]', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(metrikaGoalsList).concat(audienceSegmentsList));

                expect(u['b-retargetings-data'].getGoalsTypesList(preparedGoalsList)).to.eql(['goal', 'audience', 'segment']);
            });

            it('Если есть цели по audience, но нет по goal и segment то типы показываются в порядке => [audience, goal, segment]', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(audienceSegmentsList));

                expect(u['b-retargetings-data'].getGoalsTypesList(preparedGoalsList)).to.eql(['audience', 'goal', 'segment']);
            });

            it('Если нет никаких целей то типы показываются в порядке => [goal, segment, audience]', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([]);

                expect(u['b-retargetings-data'].getGoalsTypesList(preparedGoalsList)).to.eql(['goal', 'segment', 'audience']);
            });
        });

        describe('Метод getFirstGoalInListByType', function() {
            it('Если нет целей данного типа, то возвращается дефолтное значение { goalType: currentGoalType } ', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(audienceSegmentsList));

                result = u['b-retargetings-data'].getFirstGoalInListByType('segment', preparedGoalsList);

                expect(result).to.eql({ goalType: 'segment' });
            });

            it('Если есть цели данного типа, но эти цели недоступны, то возвращается дефолтное значение { goalType: currentGoalType }', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(audienceSegmentsList).concat([metrikaSegmentsList[0]]));

                result = u['b-retargetings-data'].getFirstGoalInListByType('segment', preparedGoalsList);

                expect(result).to.eql({ goalType: 'segment' });
            });

            it('Если есть только одна доступная цель данного типа, то возвращается она', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(audienceSegmentsList).concat([metrikaSegmentsList[1]]));

                result = u['b-retargetings-data'].getFirstGoalInListByType('segment', preparedGoalsList);

                expect(result.id).to.be.equal(metrikaSegmentsList[1].id);
            });

            it('Если есть несколько доступных целей данного типа, то возвращается первая из списка', function() {
                preparedGoalsList = u['b-retargetings-data'].formatGoalList([].concat(metrikaSegmentsList));

                result = u['b-retargetings-data'].getFirstGoalInListByType('segment', preparedGoalsList);

                expect(result.id).to.be.equal(metrikaSegmentsList[1].id);
            });
        });
    });
});
