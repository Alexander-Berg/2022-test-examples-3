describe('ns.Model', function() {

    describe('#isErrorCanBeFixed', function() {

        beforeEach(function() {
            this.model = ns.Model.get('do-messages');
        });

        it('Должен вернуть true, если ошибка "ckey"', function() {
            expect(this.model.isErrorCanBeFixed('ckey')).to.be.equal(true);
        });

        it('Должен вернуть false, если ошибка не "ckey"', function() {
            expect(this.model.isErrorCanBeFixed({type: 'http'})).to.be.equal(false);
        });

        it('Должен вернуть false, если это модель account-information', function() {
            var model = ns.Model.get('account-information');
            expect(model.isErrorCanBeFixed('ckey')).to.be.equal(false);
        });

    });

    describe('#fixError', function() {

        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest');
        });

        it('должен сделать запрос за account-information', function() {
            ns.Model.get('do-messages').fixError({id: 'NO_DATA'});

            expect(ns.forcedRequest).to.be.calledWith(['account-information']);
        });
    });

    describe('integration tests ->', function() {

        // Это конечно проверка работы NS. В самом NS тоже есть этот тест.
        // Но функциональность настолько важная, что не стыдно ее проверить еще раз.

        beforeEach(function() {
            ns.request.models.restore();

            var aiResponse = {
                models: [{
                    data: {ckey: 'new_valid_ckey'}
                }]
            };
            var invalidCkeyResponse = {
                models: [{
                    error: 'ckey'
                }]
            };
            var validCkeyResponse = {
                models: [{
                    data: {status: 'ok'}
                }]
            };

            this.doModel = ns.Model.get('do-messages', {
                action: 'mark',
                ids: ['1']
            });

            this.sinon.stub(ns, 'http')
                .withArgs(Daria.api.models + '?_m=account-information')
                    .returns(Vow.resolve(aiResponse))
                .withArgs(Daria.api.models + '?_m=do-messages')
                    .onCall(0).returns(Vow.resolve(invalidCkeyResponse))
                    .onCall(1).returns(Vow.resolve(validCkeyResponse));
        });

        it('должен успешно завершить запрос', function() {
            return ns.forcedRequest([this.doModel]);
        });

        it('должен перевести do-модель в валидное состояние', function() {
            return ns.forcedRequest([this.doModel]).then(function() {
                expect(this.doModel.isValid()).to.be.equal(true);
            }, this);
        });

        it('должен сделать правильное количество перезапросов', function() {
            return ns.forcedRequest([this.doModel]).then(function() {
                expect(ns.http).to.have.callCount(3);
            }, this);
        });

    });

});
