describe('Jane.Services', function() {
    describe('#hasNewVersion', function() {
        it('должен вернуть true', function() {
            this.sinon.stub(Jane.Services, '__needUpdate').value(true);
            expect(Jane.Services.hasNewVersion()).to.be.equal(true);
        });

        it('должен вернуть false', function() {
            this.sinon.stub(Jane.Services, '__needUpdate').value(false);
            expect(Jane.Services.hasNewVersion()).to.be.equal(false);
        });
    });

    describe('#setUpdateNeeded', function() {
        beforeEach(function() {
            this.clock = this.sinon.useFakeTimers();
        });

        it('не должен ничего делать, если таймер уже стоит', function() {
            const timerId = 1;
            this.sinon.stub(Jane.Services, '__updateTimer').value(timerId);

            Jane.Services.setUpdateNeeded();
            expect(Jane.Services.__updateTimer).to.be.equal(timerId);
        });

        it('должен выставить таймер на установку признака "нужно обновить вкладку почты"', function() {
            this.sinon.stub(Jane.Services, '__updateTimer').value(null);

            Jane.Services.setUpdateNeeded();
            expect(Jane.Services.__updateTimer).not.to.be.equal(null);
        });

        it('должен выставить признак "нужно обновить вкладку почты" по истечении таймаута', function() {
            expect(Jane.Services.__needUpdate).to.be.equal(false);

            Jane.Services.setUpdateNeeded();
            expect(Jane.Services.__needUpdate).to.be.equal(false);

            this.clock.tick(2 * 60 * 1000);
            expect(Jane.Services.__needUpdate).to.be.equal(true);
        });
    });
});
