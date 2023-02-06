describe('Daria.vComposeCaptcha', function() {

    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        this.view = ns.View.create('compose-captcha');
    });

    describe('#onSnow', function() {

        it('Должен зафокусть input после отрисовки', function() {
            this.sinon.stub(this.view, 'focusInput');
            this.view.onShow();

            expect(this.view.focusInput).to.have.callCount(1);
        });
    });

    describe('#showAnotherImage', function() {

        beforeEach(function() {
            this.sinon.stub(this.view.getModel('captcha'), 'invalidate');
            this.sinon.stub(this.view, 'update');

            this.view.showAnotherImage();
        });

        it('Должен инвалидировать модель', function() {
            expect(this.view.getModel('captcha').invalidate).to.have.callCount(1);
        });

        it('Должен запустить update', function() {
            expect(this.view.update).to.have.callCount(1);
        });
    });

    describe('#getInputValue', function() {

        beforeEach(function() {
            this.view.$node = $('<div><input class="js-captcha-input" value=" test_value "></div>');
        });

        it('Должен вернуть значние поля input в виде строки', function() {
            expect(this.view.getInputValue()).to.be.a('string');
        });

        it('Должен вернуть значение без учета пробелов в начале и в конце', function() {
            expect(this.view.getInputValue()).to.be.equal('test_value');
        });
    });

    describe('#send', function() {

        beforeEach(function() {
            this.sinon.stub(this.view, 'getInputValue');
            this.sinon.stub(this.view, 'focusInput');
            this.sinon.stub(Daria.Dialog, 'close');
            this.sinon.stub(this.view.getModel('compose-fsm'), 'setState');

            this.view.send();
        });

        it('Должен получеть значение поля input', function() {
            expect(this.view.getInputValue).to.have.callCount(1);
        });

        describe('Если value пустая строка ->', function() {

            beforeEach(function() {
                this.view.getInputValue.returns('');
            });

            it('Должен вызвать фокус на input', function() {
                expect(this.view.focusInput).to.have.callCount(1);
            });

            it('Должен прекратить выполнение и вернуть false', function() {
                expect(this.view.send()).to.be.equal(false);
            });
        });

        describe('Если value не пустая строка ->', function() {

            beforeEach(function() {
                this.view.getInputValue.returns('test');
                this.view.send();
            });

            it('Должен вызвать перевести compose в состояние отправки письма', function() {
                expect(this.view.getModel('compose-fsm').setState).to.be.calledWithExactly('sending', {force: true});
            });

            it('Должен закрыть диалог', function() {
                expect(Daria.Dialog.close).to.have.callCount(1);
            });
        });
    });
});
