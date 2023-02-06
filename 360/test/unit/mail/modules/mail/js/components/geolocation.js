describe('Daria.Geolocation', function() {

    describe('#getData', function() {

        beforeEach(function() {
            if (!navigator.geolocation) {
                _.extend(navigator, {
                    geolocation: {
                        getCurrentPosition: this.sinon.stub()
                    }
                });
            } else {
                this.sinon.stub(navigator.geolocation, 'getCurrentPosition');
            }
        });

        it('Должен вызвать getCurrentPosition для определения геопозиции', function() {
            Daria.Geolocation.getData();

            expect(navigator.geolocation.getCurrentPosition).to.have.callCount(1);
        });
    });

    describe('#checkUserRelevance', function() {

        beforeEach(function() {
            Modernizr.geolocation = true;
            Modernizr.mozilla = true;
            this.sinon.stub(Math, 'round');

            this._uid = Daria.uid;
            Daria.uid = '';
        });

        afterEach(function() {
            Daria.uid = this._uid;
        });

        it('Должен вернуть false если geolocation не поддерживается', function() {
            Modernizr.geolocation = false;

            expect(Daria.Geolocation.checkUserRelevance()).to.be.equal(false);
        });

        it('Должен вернуть false если browser не firefox', function() {
            Modernizr.mozilla = false;

            expect(Daria.Geolocation.checkUserRelevance()).to.be.equal(false);
        });

        it('Должен вернуть false если не входит в 5% рандома', function() {
            this.sinon.stub(Daria, 'uid').value('00');
            expect(Daria.Geolocation.checkUserRelevance()).to.be.equal(false);
        });

        it('Должен вернуть true, если входит в 5% рандома', function() {
            this.sinon.stub(Daria, 'uid').value('39');
            expect(Daria.Geolocation.checkUserRelevance()).to.be.equal(true);
        });
    });

    describe('#monitoring', function() {

        var deferred;

        beforeEach(function() {
            deferred = $.Deferred();
            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria.Geolocation, 'checkUserRelevance').returns(true);
            this.sinon.stub(Daria.Geolocation, 'getData').returns(deferred);
        });

        it('Должен вызвать проверку возможности получения геоданных', function() {
            Daria.Geolocation.checkUserRelevance.returns(false);
            Daria.Geolocation.monitoring();

            expect(Daria.Geolocation.checkUserRelevance).to.have.callCount(1);
        });

        it('Не должен получать геоданные, если проверка вернула false', function() {
            Daria.Geolocation.checkUserRelevance.returns(false);
            Daria.Geolocation.monitoring();

            expect(Daria.Geolocation.getData).to.have.callCount(0);
        });

        it('Должен получать геоданные, если проверка вернула true', function() {
            Daria.Geolocation.monitoring();

            expect(Daria.Geolocation.getData).to.have.callCount(1);
        });

        it('Должен передать данные через ErrorLog', function() {
            Daria.Geolocation.monitoring();
            deferred.resolve({});

            expect(Jane.ErrorLog.send).to.have.callCount(1);
        });

        it('Должен передать result: success, если данные получены', function() {
            Daria.Geolocation.monitoring();
            deferred.resolve({});

            expect(Jane.ErrorLog.send).to.be.calledWith({'event': 'geolocation', result: 'success'});
        });

        it('Должен передать result: failure, если данные не получены', function() {
            Daria.Geolocation.monitoring();
            deferred.reject({});

            expect(Jane.ErrorLog.send).to.be.calledWith({'event': 'geolocation', result: 'failure'});
        });
    });
});
