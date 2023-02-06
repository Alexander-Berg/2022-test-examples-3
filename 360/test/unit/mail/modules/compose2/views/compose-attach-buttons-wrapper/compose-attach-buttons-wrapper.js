describe('Daria.vComposeAttachButtonsWrapper', function() {
    beforeEach(function() {
        this.viewParams = {
            ids: undefined,
            tids: undefined,
            oper: undefined
        };
        this.view = ns.View.create('compose-attach-buttons-wrapper', this.viewParams);
        this.sinon.stub(ns.page.current, 'page').value('compose2');
    });

    describe('#patchLayout', function() {
        it('должен выбрать лайаут compose-attach-buttons-box', function() {
            var layout = this.view.patchLayout({});
            expect(layout).to.be.eql('layout-compose-attach-buttons-wrapper');
        });
    });
});

