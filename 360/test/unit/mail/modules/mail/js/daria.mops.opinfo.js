describe('Daria.MOPS.Opinfo', function() {

    beforeEach(function() {
        this.opinfo = new Daria.MOPS.Opinfo();
    });

    afterEach(function() {
        delete this.opinfo;
    });

    describe('#addAdjust / #getAdjust', function() {

        it('должен добавить смещение для счетчиков', function() {
            this.opinfo.addAdjust('1', 2);
            expect(this.opinfo.getAdjust()).to.eql({
                '1': 2
            });
        });

        it('не должен добавить смещение для счетчиков, если их сбросили', function() {
            this.opinfo.resetAdjust();
            this.opinfo.addAdjust('1', 2);
            expect(this.opinfo.getAdjust()).to.equal(null);
        });

    });

    describe('#addMid / #getIds', function() {

        it('должен добавить mid в список затронутых', function() {
            this.opinfo.addMid('1');
            expect(this.opinfo.getIds()).to.be.eql({
                ids: ['1'],
                tids: []
            });
        });

    });

    describe('#addTid / #getIds', function() {

        it('должен добавить tid в список затронутых', function() {
            this.opinfo.addTid('t1');
            expect(this.opinfo.getIds()).to.be.eql({
                ids: [],
                tids: ['t1']
            });
        });

    });

    describe('#checkFidAdjust', function() {

        beforeEach(function() {
            this.opinfo.addAdjust('1', 1);
            this.opinfo.addAdjust('2', 2);
        });

        it('должен вернуть false, если смещение уже сброшено', function() {
            this.opinfo.resetAdjust();
            expect(this.opinfo.checkFidAdjust(1, '1')).to.be.equal(false);
        });

        it('должен вернуть false, если смещение не совпадает', function() {
            expect(this.opinfo.checkFidAdjust(3, '2')).to.be.equal(false);
        });

        it('должен сбросить смещение, если смещение не совпадает', function() {
            this.opinfo.checkFidAdjust(3, '2');
            expect(this.opinfo.hasAdjust()).to.be.equal(false);
        });

        it('должен вернуть true, если смещение совпадает', function() {
            expect(this.opinfo.checkFidAdjust(2, '2')).to.be.equal(true);
        });

        it('не должен сбросить смещение, если смещение совпадает', function() {
            this.opinfo.checkFidAdjust(2, '2');
            expect(this.opinfo.hasAdjust()).to.be.equal(true);
        });

    });

    describe('#checkTidAdjust', function() {

        beforeEach(function() {
            this.opinfo.addAdjust('1', 1);
            this.opinfo.addAdjust('2', 2);
            this.opinfo.addMid('1');
        });

        it('должен вернуть false, если смещение уже сброшено', function() {
            this.opinfo.resetAdjust();
            expect(this.opinfo.checkTidAdjust(1, '1')).to.be.equal(false);
        });

        describe('Смещение совпадает ->', function() {

            beforeEach(function() {
                this.result = this.opinfo.checkTidAdjust(3, 't1');
            });

            it('должен вернуть false', function() {
                expect(this.result).to.be.equal(true);
            });

            it('не должен сбросить смещение', function() {
                expect(this.opinfo.hasAdjust()).to.be.equal(true);
            });

            it('не должен удалять mid', function() {
                expect(this.opinfo.getIds()).to.be.eql({
                    ids: ['1'],
                    tids: []
                });
            });

        });

        describe('Смещение не совпадает ->', function() {

            beforeEach(function() {
                this.result = this.opinfo.checkTidAdjust(1, 't1');
            });

            it('должен вернуть false', function() {
                expect(this.result).to.be.equal(false);
            });

            it('должен сбросить смещение', function() {
                expect(this.opinfo.hasAdjust()).to.be.equal(false);
            });

            it('должен удалить mid и заменить его на tid', function() {
                expect(this.opinfo.getIds()).to.be.eql({
                    ids: [],
                    tids: ['t1']
                });
            });

        });

    });

    describe('#hasAdjust', function() {

        it('должен вернуть true, если смещение не было сброшено', function() {
            expect(this.opinfo.hasAdjust()).to.be.equal(true);
        });

    });

    describe('#merge', function() {

        beforeEach(function() {
            this.opinfo.addAdjust('1', 1);
            this.opinfo.addAdjust('2', 2);
            this.opinfo.addMid('1');
            this.opinfo.addTid('t1');

            this.opinfo2 = new Daria.MOPS.Opinfo();
            this.opinfo2.addAdjust('1', 1);
            this.opinfo2.addAdjust('2', 2);
            this.opinfo2.addAdjust('3', 3);
            this.opinfo2.addMid('2');
            this.opinfo2.addTid('t2');
        });

        describe('Смещение было сброшено ->', function() {

            it('не должен объединять смещения', function() {
                this.opinfo.resetAdjust();
                this.opinfo.merge(this.opinfo2);

                expect(this.opinfo.hasAdjust()).to.be.equal(false);
            });

            it('должен объединять mid и tid', function() {
                this.opinfo.resetAdjust();
                this.opinfo.merge(this.opinfo2);

                expect(this.opinfo.getIds()).to.be.eql({
                    ids: ['1', '2'],
                    tids: ['t1', 't2']
                });
            });

        });

        describe('Объединение с Opinfo cо сброшенным смещением ->', function() {

            it('должен сбросить свое смещение', function() {
                this.opinfo2.resetAdjust();
                this.opinfo.merge(this.opinfo2);

                expect(this.opinfo.hasAdjust()).to.be.equal(false);
            });

            it('должен объединять mid и tid', function() {
                this.opinfo.resetAdjust();
                this.opinfo.merge(this.opinfo2);

                expect(this.opinfo.getIds()).to.be.eql({
                    ids: ['1', '2'],
                    tids: ['t1', 't2']
                });
            });

        });

        describe('Объединение с Opinfo cо смещением ->', function() {

            it('должен объединить смещения', function() {
                this.opinfo.merge(this.opinfo2);

                expect(this.opinfo.getAdjust()).to.be.eql({
                    '1': 2,
                    '2': 4,
                    '3': 3
                });
            });

            it('должен объединять mid и tid', function() {
                this.opinfo.merge(this.opinfo2);

                expect(this.opinfo.getIds()).to.be.eql({
                    ids: ['1', '2'],
                    tids: ['t1', 't2']
                });
            });

        });


    });

    describe('#resetAdjust', function() {

        it('должен сбросить смещение', function() {
            this.opinfo.resetAdjust();
            expect(this.opinfo.hasAdjust()).to.be.equal(false);
        });

    });

    describe('#setModelsCount / #getModelsCount', function() {

        it('должен добавить количество моделей', function() {
            this.opinfo.setModelsCount(2);
            expect(this.opinfo.getModelsCount()).to.equal(2);
        });

    });
});
