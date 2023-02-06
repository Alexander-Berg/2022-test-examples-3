describe('Daria.mMessageDraft', function() {
    beforeEach(function() {
        this.model = ns.Model.get('message-draft', { 'ids': 'test' }).setData({});
    });

    describe('#getDraftMid', function() {
        it('Должен вернуть undefined, если нет mid черновика', function() {
            this.model.set('.mid', undefined);
            expect(this.model.getDraftMid()).to.be.equal(undefined);
        });

        it('Не валидная модель письма не влияет на результат', function() {
            this.model.set('.mid', 'message-draft-1');
            this.mMessage = ns.Model.get('message', { ids: 'message-draft-1' });
            this.mMessage.invalidate();
            expect(this.model.getDraftMid()).to.be.equal('message-draft-1');
            expect(this.model.get('.mid')).to.be.equal('message-draft-1');
        });

        it('Должен вернуть undefined, если mid черновика есть, но письмо не является черновиком', function() {
            this.model.set('.mid', 'message-draft-2');
            this.mMessage = ns.Model.get('message', { ids: 'message-draft-2' }).setData({});
            this.sinon.stub(this.mMessage, 'isDraftLike').returns(false);
            expect(this.model.getDraftMid()).to.be.equal(undefined);
            expect(this.model.get('.mid')).to.be.equal(undefined);
        });

        it('Должен вернуть mid черновика если есть модель письма и письмо является черновиком', function() {
            this.model.set('.mid', 'message-draft-3');
            this.mMessage = ns.Model.get('message', { ids: 'message-draft-3' }).setData({});
            this.sinon.stub(this.mMessage, 'isDraftLike').returns(true);
            expect(this.model.getDraftMid()).to.be.equal('message-draft-3');
        });
    });
});

