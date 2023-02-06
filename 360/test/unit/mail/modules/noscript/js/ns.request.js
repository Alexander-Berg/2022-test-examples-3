describe('ns.request', function() {

    describe('' +
        'Daria.parseLoginInfo()', function() {

        beforeEach(function() {
            this.sinon.stub(Jane, 'doLogin').callsFake(Jane.Common.nop);
            this.sinon.stub(Jane, 'locate').callsFake(Jane.Common.nop);

            this.mAI = ns.Model.get('account-information');
        });

        afterEach(function() {
            delete this.result;
        });

        describe('Нет авторизации (ошибка из хендлера)', function() {

            describe('models.error.code == AUTH_NO_AUTH', function() {

                beforeEach(function() {
                    var response = {
                        "models": [
                            {"error": {"code": "AUTH_NO_AUTH"}, "name": "labels", "status": "error"},
                            {"error": {"code": "AUTH_NO_AUTH"}, "name": "folders", "status": "error"}
                        ],
                        "login": "", "timestamp": 1387888324982, "cookieRenew": false, "versions": {"jane": "7.39.9", "mail": "7.39.8", "disk": "3.30.3", "todo": "1.6.1"}};
                    this.result = Daria.parseLoginInfo(response);
                });

                checkNoAuth();

            });

            describe('models.error.code == AUTH_WRONG_GUARD', function() {

                beforeEach(function() {
                    var response = {
                        "models": [
                            {"error": {"code": "AUTH_WRONG_GUARD"}, "name": "labels", "status": "error"},
                            {"name": "folders", "status": "ok", "data":{}}
                        ],
                        "login": "", "timestamp": 1387888324982, "cookieRenew": false, "versions": {"jane": "7.39.9", "mail": "7.39.8", "disk": "3.30.3", "todo": "1.6.1"}};
                    this.result = Daria.parseLoginInfo(response);
                });

                checkNoAuth();

            });

            describe('error.code == AUTH_NO_AUTH', function() {

                beforeEach(function() {
                    var response = {"error": {"code": "AUTH_NO_AUTH"}};
                    this.result = Daria.parseLoginInfo(response);
                });

                checkNoAuth();

            });

            function checkNoAuth() {
                it('должен вернуть false', function() {
                    expect(this.result).to.be.equal(false);
                });

                it('не должен вызвать Jane.locate', function() {
                    expect(Jane.locate.called).to.be.equal(false);
                });

                it('должен вызвать Jane.doLogin', function() {
                    expect(Jane.doLogin.calledOnce).to.be.equal(true);
                });

                it('должен вызвать Jane.doLogin("parseLoginInfo")', function() {
                    expect(Jane.doLogin).to.be.calledWith('parseLoginInfo');
                });

            }

        });

        describe('Смена логина', function() {

            beforeEach(function() {
                Daria.uid = '42';

                this.result = Daria.parseLoginInfo({
                    uid: '43'
                });
            });

            afterEach(function() {
                Daria.uid = null;
            });

            it('должен вернуть false', function() {
                expect(this.result).to.be.equal(false);
            });

            it('Если в ответе пришел uid, отличный от текущего, то происходит refresh', function() {
                expect(Jane.locate.calledOnce).to.be.equal(true);
            });

            it('Если в ответе пришел uid, отличный от текущего, то происходит refresh на /', function() {
                expect(Jane.locate.calledWithExactly('/')).to.be.equal(true);
            });

        });

        it('Если в ответе не пришел uid и нет дополнительных аккаунтов refresh не должен произойти', function() {
            Daria.parseLoginInfo({});
            expect(Jane.locate.called).to.be.equal(false);
        });

        describe('Подновление куки', function() {
            // TODO:
        });

    });

    describe('#addRequestParams', function() {

        it('в запрос добавляются параметры _locale, _product, _timestamp, _connection_id и _ckey', function() {
            var params = {};
            ns.request.addRequestParams(params);
            expect(params).to.include.keys('_locale', '_product', '_timestamp', '_ckey', '_connection_id');
        });

    });

});
