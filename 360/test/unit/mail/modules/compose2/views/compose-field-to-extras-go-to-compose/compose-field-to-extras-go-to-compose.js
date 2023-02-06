describe('Daria.vComposeFieldToExtrasGoToCompose', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-to-extras-go-to-compose');
        this.mComposeMessage = this.view.getModel('compose-message');
        this.sinon.stub(Daria, 'isReactiveCompose').returns(false);
    });

    describe('#onClick', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'expandMessage');
            this.sinon.stub(ns.page, 'go').returns({
                then: _.noop
            });
            this.sinon.stub(ns.router, 'generateUrl');

            this.view.onClick();
        });

        it('должен выполнить переход в композ', function() {
            expect(ns.router.generateUrl).to.be.calledWithExactly('compose2', {});
        });
    });
});

