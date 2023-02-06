describe('dm-base-campaign', function() {
    var campaignDm,
        MAX_MONEY_WARNING_VALUE = u.consts('MAX_MONEY_WARNING_VALUE'),
        defaultData = {
            fio: 'FIO',
            money_warning_value: 1,
            name: 'name',
            start_date: '04-02-2016'
        };

    describe('Проверка валидации', function() {
        var validationResult;

        beforeEach(function() {
            campaignDm = BEM.MODEL.create('dm-base-campaign', defaultData);
        });

        afterEach(function() {
            campaignDm.destruct();
        });

        it('Настройки кампании валидные', function() {
            validationResult = campaignDm.validate();

            expect(validationResult.valid).to.be.ok;
        });

        describe('Кампания без ID общего счета', function() {
            beforeEach(function() {
                campaignDm.set('wallet_cid', '');
            });

            function moneyWarningValueTest(moneyWarningValue) {
                campaignDm.set('money_warning_value', moneyWarningValue);
                validationResult = campaignDm.validate();

                expect(validationResult).to.have.deep.property('errorsData.money_warning_value');
            }

            it('Остаток средств должен быть больше или равен 1', function() {
                moneyWarningValueTest(0);
            });

            it('Остаток средств должен быть меньше или равен ' + MAX_MONEY_WARNING_VALUE, function() {
                moneyWarningValueTest(MAX_MONEY_WARNING_VALUE + 1);
            })
        });
    });
});
