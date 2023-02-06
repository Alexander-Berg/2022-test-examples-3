describe('Daria.vComposeSendButtonComplexPopup', function() {

    beforeEach(function() {
        this.view = ns.View.create('compose-send-button-complex-popup');

        this.sinon.spy(this.view, '_updateDisplay');

        return this.view.open();
    });

    afterEach(function() {
        this.view.close();
    });

    describe('#open', function() {
        beforeEach(function() {
            this.view2 = ns.View.create('compose-send-button-complex-popup');
            this.mComposeMessage = this.view2.getModel('compose-message');

            this.sendTime = new Date();
            this.sinon.stub(this.mComposeMessage, 'getPassportSendDate').returns(this.sendTime);

            this.sinon.stub(this.view2, '_dateValidation');
            this.sinon.stub(this.view2, '_onAfterRender');
            this.sinon.stub(this.view2, 'update').returns(vow.resolve());

            return this.view2.open({ extra: true });
        });

        it('должен взять начальное значение даты из модели compose-message', function() {
            expect(this.view2._date).to.be.eql(this.sendTime);
        });

        it('должен установить опции', function() {
            expect(this.view2._options).to.be.eql({
                withoutTail: true,
                autofocus: true,
                autoclose: true,
                how: {
                    my: 'center bottom',
                    at: 'center top'
                },
                extra: true
            });
        });

        it('должен запустить валидацию даты', function() {
            expect(this.view2._dateValidation).to.have.callCount(1);
        });

        it('должен выполнить на себе локальный update c нужными опциями', function() {
            expect(this.view2.update)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly(this.view2.params, { execFlag: ns.U.EXEC.PARALLEL });

            expect(this.view2._onAfterRender).to.have.callCount(1);
        });
    });

    describe('#_onShow', function() {
        it('Должен обновить вывод даты при открытии окна', function() {
            expect(this.view._updateDisplay).to.have.callCount(1);
        });
    });

    describe('#_onSetInputVal', function() {
        it('Не должен изменить дату и не выполнять обновление вывода, если новая дата равна текущей', function() {
            var date = new Date();
            this.view._date = new Date(date);

            this.view._onSetInputVal(this.view._dateToString(this.view._date));

            expect(this.view._date.getTime()).to.be.equal(date.getTime());
            expect(this.view._updateDisplay).to.have.callCount(1);
        });

        it('Должен изменить дату и выполнить обновление вывода, если дата отличается от текущей', function() {
            var date = new Date();
            this.view._date = new Date(date);

            date.setFullYear(date.getFullYear() + 1);

            this.view._onSetInputVal(this.view._dateToString(date));

            expect(this.view._date.getTime()).to.be.equal(date.getTime());
            expect(this.view._updateDisplay).to.have.callCount(2);
        });
    });

    describe('#_dateValidation', function() {
        it('Должен вернуть false, если выбранная дата больше текущей браузерной', function() {
            var date = new Date();
            this.view._date = new Date(date);
            this.view._date.setHours(this.view._date.getHours() + 1, 0, 0, 0);

            this.sinon.stub(Daria, 'passportNow').returns(date);
            expect(this.view._dateValidation()).not.to.be.ok;
        });

        it('Должен вернуть true и изменить выбранную дату на текущую браузерную, если выбранная дата меньше текущей', function() {
            var date = new Date();
            date.setHours(date.getHours(), 0, 0, 0);

            var newDate = new Date(date);
            newDate.setHours(newDate.getHours() + 1, 0, 0, 0);

            this.view._date = new Date(date);
            this.view._date.setFullYear(this.view._date.getFullYear() - 1);
            this.sinon.stub(Daria, 'passportNow').returns(date);

            expect(this.view._dateValidation()).to.be.ok;
            expect(this.view._date.getTime()).to.be.equal(newDate.getTime());
        });
    });

    describe('#_updateData', function() {
        beforeEach(function() {
            this.date = new Date(2018, 9, 18, 1, 2, 3);
            this.passportTimestamp = this.date.getTime() + 180 * 60 * 1000;

            this.sinon.stub(Daria, 'convertPassportDateToTimestamp')
                .withArgs(this.date).returns(this.passportTimestamp);

            this.view._date = this.date;
            this.view._nbActivate = {
                isChecked: this.sinon.stub().returns(true)
            };

            this.mComposeMessage = this.view.getModel('compose-message');
            this.sinon.stub(this.mComposeMessage, 'setIfChanged');
        });

        it('Должен установить непустое время отправки', function() {
            this.view._updateData();
            expect(this.mComposeMessage.setIfChanged).to.be.calledWithExactly('.send_time', this.passportTimestamp);
        });

        it('Должен установить пустое время отправки', function() {
            this.view._nbActivate.isChecked.returns(false);

            this.view._updateData();
            expect(this.mComposeMessage.setIfChanged).to.be.calledWithExactly('.send_time', null);
        });
    });
});
