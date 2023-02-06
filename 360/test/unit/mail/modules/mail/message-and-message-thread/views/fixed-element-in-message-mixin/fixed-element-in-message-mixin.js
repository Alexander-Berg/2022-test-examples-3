describe('Daria.vFixedElementInMessageMixin', function() {
    before(function() {
        // вьюшка-наследник для тестов
        ns.View.edefine('test-fixed-element-in-message-mixin', {
            models: {
                'scroller-message': false
            }
        }, 'fixed-element-in-message-mixin');
    });

    beforeEach(function() {
        this.$3paneScrollArea = $('<div class="js-message-scroll-area"></div>').appendTo('body');

        this.$node = $('<div />').appendTo('body');
        this.$node.wrap('<div class="js-cbt-item"></div>');

        this.view = ns.View.create('test-fixed-element-in-message-mixin');
        this.view.$node = this.$node;

        this.sinon.stub(Daria, 'is3pane').returns(true);
        this.sinon.stub(ns.events, 'on');
        this.sinon.stub(ns.events, 'off');
    });

    afterEach(function() {
        this.$node.parent().remove();
        this.$3paneScrollArea.remove();
    });

    describe('fixedElementStart', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, '_fixedElementSetFixed3PaneHeader');
        });

        describe('Для 3pane', function() {
            beforeEach(function() {
                Daria.is3pane.returns(true);
            });

            it('Должен вызвать _fixedElementSetFixed3PaneHeader', function() {
                this.view.fixedElementStart();
                expect(this.view._fixedElementSetFixed3PaneHeader).to.have.callCount(1);
            });
        });
    });

    describe('#_fixedElementRecalcStickyHeader', function() {

        beforeEach(function() {
            this.view.$node.closest('.js-cbt-item').height(500);

            this.sinon.spy(this.view, '_fixedElementToggleFixedHeader');
        });

        it('в 3пейн не нужно отрывать залипающую шапку, когда письмо кончилось', function() {
            Daria.is3pane.returns(true);
            this.view.fixedElementStart();

            expect(this.view._fixedElementRecalcStickyHeader()).to.be.equal(undefined);
        });

        it('в 2пейн нужно отрывать залипающую шапку, когда письмо кончилось', function() {
            Daria.is3pane.returns(false);
            this.view.fixedElementStart();

            this.view._fixedElementRecalcStickyHeader();
            expect(this.view._fixedElementToggleFixedHeader).to.have.callCount(1);
        });

        it('нужно, чтобы шапка залипала, когда зашла за пределы скролла', function() {
            Daria.is3pane.returns(false);
            this.view.fixedElementStart();

            var nodeOffset = {top: 20};
            this.sinon.stub(this.view.$node, 'offset').returns(nodeOffset);

            this.sinon.stub(this.view.getModel('scroller-message'), 'getScrollTop').returns(400);
            this.view._fixedElementRecalcStickyHeader();
            expect(this.view._fixedElementToggleFixedHeader.withArgs(true, nodeOffset).called).to.be.ok;
        });

        it('нужно, чтобы шапка отлипала, когда зашла за пределы письма/треда', function() {
            Daria.is3pane.returns(false);
            this.view.fixedElementStart();

            var nodeOffset = {top: 20};
            this.sinon.stub(this.view.$node, 'offset').returns(nodeOffset);

            this.sinon.stub(this.view.getModel('scroller-message'), 'getScrollTop').returns(600);
            this.view._fixedElementRecalcStickyHeader();
            expect(this.view._fixedElementToggleFixedHeader.withArgs(true, nodeOffset).called).to.be.equal(false);
        });
    });
});

