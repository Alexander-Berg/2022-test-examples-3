describe('b-strategy2-settings_media-type_performance', function() {

    var vm,
        currency = 'RUB',
        minAutobudget = u.currencies.getConst(currency, 'MIN_AUTOBUDGET'),
        maxAutobudget = u.currencies.getConst(currency, 'MAX_AUTOBUDGET'),
        minAvClick = u.currencies.getConst(currency, 'MIN_CPC_CPA_PERFORMANCE'),
        maxPrice = u.currencies.getConst(currency, 'MAX_PRICE'),
        maxBid = u.currencies.getConst(currency, 'MAX_AUTOBUDGET_BID'),
        validate;

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_media-type_performance', {
            "currency": "RUB",
            "mediaType": "performance",
            "disabledMetrika": false,
            "target": "camp",
            "maxClickBidEnabled": false,
            "weekBidEnabled": false,
            "cpcPerFilter": 3
        });
    });

    afterEach(function() {
        vm.destruct();
    });

    describe('Валидация', function() {
        it('Если weekBidEnabled: false, то поле weekBid валидно', function() {
            vm.set('weekBidEnabled', false);

            expect(vm.validate('weekBid').valid).to.equal(true);
        });

        it('Если maxClickBidEnabled: false, то поле maxClickBid валидно', function() {
            vm.set('maxClickBidEnabled', false);

            expect(vm.validate('maxClickBid').valid).to.equal(true);
        });


        describe('Поле weekBid', function() {
            beforeEach(function() {
                vm.set('weekBidEnabled', true);
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

            it('Недельный бюджет должен быть больше максимальной цены клика', function() {
                vm.update({
                    target: 'camp',
                    cpcPerCamp: 500,
                    weekBid: 304
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('weekBid');
                expect(validate.errors[0].rule).to.equal('lowerThenCPC');
            });

            it('При нормальном недельном бюджете модель валидна', function() {
                vm.update({
                    target: 'camp',
                    weekBid: maxAutobudget - 1,
                    cpcPerCamp: 500,
                    maxClickBidEnabled: true,
                    maxClickBid: 550
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(true);
            });
        });

        describe('Поле maxClickBid', function() {
            beforeEach(function() {
                vm.set('maxClickBidEnabled', true);
            });

            it('Максимальная цена клика должна быть больше или равна  ' + minAvClick, function() {
                vm.set('maxClickBid', minAvClick - 1);
                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('maxClickBid');
                expect(validate.errors[0].rule).to.equal('lte');
            });

            it('Максимальная цена клика должна быть меньше или равна ' + maxPrice, function() {
                vm.set('maxClickBid', maxPrice + 1);
                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('maxClickBid');
                expect(validate.errors[0].rule).to.equal('gte');
            });

            it('Значение средней цены клика должно быть меньше или равно максимальной ставке.', function() {
                vm.update({
                    maxClickBidEnabled: true,
                    maxClickBid: 305,
                    cpcPerCamp: 601,
                    target: 'camp'
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(false);
                expect(validate.errorFields[0]).to.equal('maxClickBid');
                expect(validate.errors[0].rule).to.equal('lowerThenCPC');
            });

            it('При нормальной максимальной цене клика модель валидна', function() {
                vm.update({
                    target: 'camp',
                    cpcPerCamp: 500,
                    maxClickBidEnabled: true,
                    maxClickBid: 550
                });

                validate = vm.validate();

                expect(!!validate.valid).to.equal(true);
            })
        });

        [
            {
                field: 'cpcPerCamp',
                target: 'camp'
            },
            {
                field: 'cpcPerFilter',
                target: 'filter'
            }
        ].forEach(function(test) {
            describe('Поле ' + test.field, function() {
                beforeEach(function() {
                    vm.set('target', test.target);
                });

                it('Поле cpcPerCamp обязательно', function() {
                    vm.set(test.field, undefined);

                    validate = vm.validate();

                    expect(!!validate.valid).to.equal(false);
                    expect(validate.errorFields[0]).to.equal(test.field);
                    expect(validate.errors[0].rule).to.equal('required');
                });

                it('Минимальное значение средней цены клика ' + minAvClick, function() {
                    vm.set(test.field, minAvClick - 1);

                    validate = vm.validate();

                    expect(!!validate.valid).to.equal(false);
                    expect(validate.errorFields[0]).to.equal(test.field);
                    expect(validate.errors[0].rule).to.equal('lte');
                });

                it('Максимальное значение средней цены клика ' + maxBid, function() {
                    vm.set(test.field, maxBid + 1);

                    validate = vm.validate();

                    expect(!!validate.valid).to.equal(false);
                    expect(validate.errorFields[0]).to.equal(test.field);
                    expect(validate.errors[0].rule).to.equal('gte');
                });

                it('При нормальном значении средней цены клика модель валидна', function() {
                    vm.set(test.field, maxBid - 1);

                    validate = vm.validate();

                    expect(validate.valid).to.equal(true);
                })
            });
        })
    });
});
