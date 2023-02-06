describe('Daria.vComposeMessageToolbarButtonCommon', function() {
    beforeEach(function() {
        this.vComposeMessageToolbarButtonCommon = ns.View.create('compose-message-toolbar-button-common');
        this.vComposeMessageToolbarButtonCommon.$node = $('<div />');
        this.vComposeMessageToolbarButtonCommon.node = this.vComposeMessageToolbarButtonCommon.$node[0];

        this.event = $.Event('click');
        this.sinon.stub(this.event, 'preventDefault');
        this.sinon.stub(ns.page.current, 'page').value('compose2');
    });

    describe('#_onNsViewShow', function() {

        it('должен получить смещение и размеры блока вида', function() {
            this.vComposeMessageToolbarButtonCommon._onNsViewShow();
            expect(this.vComposeMessageToolbarButtonCommon.rect).to.be.instanceof(DOMRect);
        });
    });

    describe('#checkShortlink', function() {

        it('должен проверить наличие класса is-shortlink на ноде вида 1', function() {
            var rez = this.vComposeMessageToolbarButtonCommon.checkShortlink();
            expect(rez).to.not.ok;
        });

        it('должен проверить наличие класса is-shortlink на ноде вида 2', function() {
            this.vComposeMessageToolbarButtonCommon.$node.addClass('is-shortlink');
            var rez = this.vComposeMessageToolbarButtonCommon.checkShortlink();
            expect(rez).to.be.ok;
        });
    });

    describe('#setShortlink', function() {

        it('должен установить признак короткой кнопки', function() {
            this.vComposeMessageToolbarButtonCommon.setShortlink();
            expect(this.vComposeMessageToolbarButtonCommon.isShortlink()).to.be.ok;
            expect(this.vComposeMessageToolbarButtonCommon.checkShortlink()).to.be.ok;
        });
    });

    describe('#removeShortlink', function() {

        it('должен снять признак короткой кнопки', function() {
            this.vComposeMessageToolbarButtonCommon.removeShortlink();
            expect(this.vComposeMessageToolbarButtonCommon.isShortlink()).to.not.ok;
            expect(this.vComposeMessageToolbarButtonCommon.checkShortlink()).to.not.ok;
        });
    });
});

