describe('Daria.vFixedQuickReplyMixin', function() {

    describe('#toggleStickyMode', function() {

        it('во время вычислений залипания нужно округлять дробную часть у отступов', function() {
            var view = ns.View.create('fixed-quick-reply-mixin');
            view.$node = $('<div/>');
            view.$qr = $('<div/>');

            this.sinon.stub(view.$qr,   'offset').returns({ top: 510.94998 });
            this.sinon.stub(view.$node, 'offset').returns({ top: 490.94999 });

            this.sinon.stub(view, 'isQRInvisibleInScrollArea').returns(false);
            this.sinon.stub(view, 'stickyModeOn');
            this.sinon.stub(view, 'stickyModeOff');


            if (!view.hasOwnProperty('QR_OFFSET')) {
                view.QR_OFFSET = undefined;
            }
            this.sinon.stub(view, 'QR_OFFSET').value(20);

            var mMessage = ns.Model.get('message', { ids: 'test-01' });
            this.sinon.stub(mMessage, 'isHuman').returns(true);

            this.sinon.stub(view, 'getModel').withArgs('message').returns(mMessage);

            view.toggleStickyMode();

            expect(view.stickyModeOff).to.have.callCount(1);

        });

    });

});
