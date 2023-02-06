describe('b-strategy2-settings_name_opt-cpa', function() {

    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        validate;

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_opt-cpa', {
            "currency": "RUB",
            "metrika": {
                "campaign_goals": [
                    {
                        "goal_name": "возвращался",
                        "counter_status": "Active",
                        "goal_id": "18812335",
                        "goal_status": "Active",
                        "goals_count": 0,
                        "context_goals_count": 0
                    }
                ],
                "compaign_domains_count": 0,
                "cpa_deviation": false,
                "apc_deviation": false
            },
            "mediaType": "performance",
            "disabledMetrika": false,
            "target": "camp",
            "goalId": "18812335",
            "maxClickBidEnabled": false,
            "weekBidEnabled": false
        });
    });

    afterEach(function() {
        vm.destruct();
    });

    describe('Валидация модели', function() {
        ['camp', 'filter'].forEach(function(test) {
            function setCamp(value) {
                vm.set('cpaPerCamp', value);
            }

            function setFilter(value) {
                vm.set('target', 'filter');
                vm.set('cpaPerFilter', value);
            }

            it('Если не заполнена средняя цена конверсии, модель невалидна', function() {
                test == 'camp' && setCamp(undefined);
                test == 'filter' && setFilter(undefined);

                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errors[0].text).to.equal('Не указано значение средней цены конверсии')
            });
        });

        it('Перед настройкой стратегии укажите счетчик Метрики и задайте ключевые цели кампании', function() {
            sinon.stub(u, 'getMetrikaWarning').returns('no-metrika');
            vm.set('cpaPerCamp', 10);

            validate = vm.validate();
            u.getMetrikaWarning.restore();

            expect(!!validate.valid).to.equal(false);
            expect(validate.errors[0].text.value).to.equal('Перед настройкой стратегии укажите счетчик Метрики и задайте ключевые цели кампании');
        });

        describe('Недельный бюджет', function() {
            beforeEach(function() {
                vm.set('weekBidEnabled', true);
                vm.set('cpaPerCamp', 10);
            });

            it('Недельный бюджет не может быть меньше ' + minAutobudget, function() {
                vm.set('weekBid', minAutobudget - 1);
                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('weekBid');
                expect(validate.errors[0].rule).to.equal('lte');
            });

            it('Недельный бюджет не может быть больше ' + maxAutobudget, function() {
                vm.set('weekBid', maxAutobudget + 1);
                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('weekBid');
                expect(validate.errors[0].rule).to.equal('gte');
            });

            it('Недельный бюджет должен быть больше максимальной цены клика', function() {
                vm.update({
                    maxClickBidEnabled: true,
                    maxClickBid: 500,
                    weekBid: 304
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('weekBid');
                expect(validate.errors[0].rule).to.equal('lowerClickPrice');
            });

            it('Недельный бюджет должен быть больше средней цены конверсии', function() {
                vm.update({
                    target: 'camp',
                    cpaPerCamp: 500,
                    weekBid: 304
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('weekBid');
                expect(validate.errors[0].rule).to.equal('lowerThenCPA');
            });

            it('При нормальном недельном бюджете модель валидна', function() {
                vm.update({
                    target: 'camp',
                    weekBid: maxAutobudget - 1,
                    cpaPerCamp: 500,
                    maxClickBidEnabled: true,
                    maxClickBid: 550
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(true);
            });
        });
    });

    describe('Поле options', function() {
        var options;
        [
            {
                title: 'кампании',
                options: {
                    target: 'camp',
                    cpaPerCamp: 5
                },
                fields: [
                    { target: 'camp' },
                    { avg_cpa: 5 },
                    { originName: 'autobudget_avg_cpa_per_camp' }
                ]
            },
            {
                title: 'фильтра',
                options: {
                    target: 'filter',
                    cpaPerFilter: 4,
                },
                fields: [
                    { target: 'filter' },
                    { filter_avg_cpa: 4 },
                    { originName: 'autobudget_avg_cpa_per_filter' }
                ]
            },
            {
                title: 'maxClickBid: enabled',
                options: {
                    target: 'filter',
                    cpaPerFilter: 4,
                    maxClickBidEnabled: true,
                    maxClickBid: 500
                },
                fields: [
                    { bid: 500 }
                ]
            },
            {
                title: 'maxClickBid: disabled',
                options: {
                    target: 'filter',
                    cpaPerFilter: 4,
                    maxClickBidEnabled: false
                },
                fields: [
                    { bid: null }
                ]
            },
            {
                title: 'weekBidEnabled: true',
                options: {
                    target: 'filter',
                    cpaPerFilter: 4,
                    weekBidEnabled: true,
                    weekBid: 500
                },
                fields: [
                    { sum: 500 }
                ]
            },
            {
                title: 'weekBidEnabled: false',
                options: {
                    target: 'filter',
                    cpaPerFilter: 4,
                    weekBidEnabled: false
                },
                fields: [
                    { sum: null }
                ]
            }
        ].forEach(function(test) {
            describe('Для ' + test.title + ' возвращает', function() {
                beforeEach(function() {
                    vm.update(test.options);
                    options = vm.get('options');
                });

                test.fields.forEach(function(field) {
                    it(JSON.stringify(field), function() {
                        var key = Object.keys(field)[0];
                        expect(options[key]).to.equal(field[key]);
                    })
                });
            });
        });
    });
});
