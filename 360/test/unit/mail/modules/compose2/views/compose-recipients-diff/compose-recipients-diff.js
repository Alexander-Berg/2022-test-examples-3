describe('Daria.vComposeRecipientsDiff', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message').setData({});
        this.mComposeState = ns.Model.get('compose-state').setData({});

        this.view = ns.View.create('compose-recipients-diff');
        this.view.$node = $("<div></div>");

        var stubModel = this.sinon.stub(this.view, 'getModel');

        stubModel.withArgs('compose-message').returns(this.mComposeMessage);
        stubModel.withArgs('compose-state').returns(this.mComposeState);
    });

    describe('#onSelectorClick', function() {
        it('Закрыли плашку-настройка модели поменялась', function() {
            this.view.onSelectorClick();
            expect(this.mComposeState.get('.recipients-diff-pane-close')).to.be.eql(true);
        });
    });
    describe('#getRecipientsEmails', function() {
        it('Приходит пустой массив емейлов', function() {
            this.mComposeMessage.set('.recipients-diff', {
                added: [],
                removed: []
            });
            this.view.getRecipientsEmails();
            expect(this.mComposeMessage.get('.recipients-diff')).to.be.eql({added: [], removed: []});
        });
        it('Приходит непустой массив емейлов', function() {
            this.mComposeMessage.set('.recipients-diff', {
                added: ['test1@ya.ru', 'test2@y.ru', 'testName'],
                removed: ['test4@ya.com', 'test@']
            });
            this.view.getRecipientsEmails();
            expect(this.mComposeMessage.get('.recipients-diff')).to.be.be.eql({added: ['test1@', 'test2@', 'testName'], removed: ['test4@', 'test@']});
        });
    });
});
