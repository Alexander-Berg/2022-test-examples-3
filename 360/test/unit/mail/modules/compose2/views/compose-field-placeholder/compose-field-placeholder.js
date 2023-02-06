describe('Daria.vComposeFieldPlaceholder', function() {
    beforeEach(function() {
        this.mComposeMessage = ns.Model.get('compose-message').setData({});
        this.mQuickReplyState = ns.Model.get('quick-reply-state').setData({});
        this.view = ns.View.create('compose-field-placeholder');

        var stub = this.sinon.stub(this.view, 'getModel');
        stub.withArgs('compose-message').returns(this.mComposeMessage);
        stub.withArgs('quick-reply-state').returns(this.mQuickReplyState);
    });

    describe('#onClick', function() {
        it('должен установить признак закрытия вида', function() {
            var stub = this.sinon.stub(this.view.getModel('quick-reply-state'), 'setIfChanged');
            this.view.onClick();
            expect(stub).to.be.calledWithExactly('.showFieldPlaceholder', false);
        });
    });

    describe('#getRecipients', function() {
        it('должен вернуть массив со списком получателей из поля to', function() {
            this.mComposeMessage.set('.to', '"test1" <test1@yandex.ru>, test2@yandex.ru');
            var val = this.view.getRecipients();
            expect(val).to.be.deep.equal([
                {
                    name: 'test1',
                    email: 'test1@yandex.ru'
                },
                {
                    email: 'test2@yandex.ru'
                }
            ]);
        });
    });

    describe('#getCopyCount', function() {
        it('должен вернуть количество получателей из поля cc', function() {
            this.mComposeMessage.set('.cc', '"test1" <test1@yandex.ru>, test2@yandex.ru');
            var val = this.view.getCopyCount();
            expect(val).to.be.equal(2);
        });
    });
});

