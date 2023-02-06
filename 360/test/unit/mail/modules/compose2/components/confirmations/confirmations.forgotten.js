describe('attachments-forgotten', function() {

    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        this.confirmation = Daria.ComposeComponents.confirmations.getConfirmation('attachments-forgotten');
        this.model = ns.Model.get('compose-message');
        this.stopWord = 'attach';
    });

    describe('Должен запросить проверку если ->', function() {
        it('Если в теме письма есть стоп слово', function() {
            this.model.setData({ subj: this.stopWord, ids: [] });

            expect(this.confirmation(this.model, {})).to.be.equal('attachments-forgotten');
        });

        it('Если в теле письма есть стоп слово', function() {
            this.model.setData({ send: '<div><p>' + this.stopWord + '</p></div>', ids: [] });

            expect(this.confirmation(this.model, {})).to.be.equal('attachments-forgotten');
        });
    });

    describe('Не должен запрашивать проверку если ->', function() {

        it('в теме письма и в теле нет стоп слов', function() {
            this.model.setData({
                subj: 'No stop words',
                send: '<div><p>There is no stop words here! Seriously!</p></div>',
                ids: []
            });

            expect(this.confirmation(this.model, {})).to.be.equal(false);
        });

        it('есть аттачи', function() {
            this.sinon.stub(this.model, 'hasAttach').returns(true);

            expect(this.confirmation(this.model, {})).to.be.equal(false);
        });

        it('стоп слова в цитате', function() {
            this.model.setData({
                send: '<blockquote>There is one stop word ' + this.stopWord + '</blockquote>',
                ids: []
            });

            expect(this.confirmation(this.model, {})).to.be.equal(false);
        });
    });
});

