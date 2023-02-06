describe('send-without-to', function() {

    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        this.confirmation = Daria.ComposeComponents.confirmations.getConfirmation('send-without-to');
        this.model = ns.Model.get('compose-message');
    });

    describe('Должен запросить проверку если ->', function() {

        it('Поле to пустое, а поле cc заполнено', function() {
            this.model.setData({
                'to': '',
                'cc': 'test',
                'bcc': ''
            });

            expect(this.confirmation(this.model)).to.be.equal('send-without-to');
        });

        it('Поле to пустое, а поле bcc заполнено', function() {
            this.model.setData({
                'to': '',
                'cc': '',
                'bcc': 'test'
            });

            expect(this.confirmation(this.model)).to.be.equal('send-without-to');
        });
    });

    describe('Не должен запрашивать проверку если ->', function() {

        it('Все поля пустые', function() {
            this.model.setData({
                'to': '',
                'cc': '',
                'bcc': ''
            });

            expect(this.confirmation(this.model)).to.be.equal(false);
        });

        it('Поле to и сс не пустое', function() {
            this.model.setData({
                'to': 'test',
                'cc': 'test',
                'bcc': ''
            });

            expect(this.confirmation(this.model)).to.be.equal(false);
        });
    });
});
