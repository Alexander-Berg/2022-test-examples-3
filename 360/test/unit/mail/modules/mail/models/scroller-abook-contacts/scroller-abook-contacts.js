describe('Daria.mScrollerAbookContacts', function() {
    beforeEach(function() {
        this.model = ns.Model.get('scroller-abook-contacts');
        this.$node = $("<div class='js-entries-container'></div>").appendTo('body');
    });

    afterEach(function() {
        this.$node.remove();
    });

    describe('#_getScroller', function() {
        it('-> 2pane', function() {
            this.sinon.stub(Daria, 'is2pane').returns(true);
            expect(this.model._getScroller()).to.be.eql($(window));
        });
        it('-> 3pane', function() {
            this.sinon.stub(Daria, 'is2pane').returns(false);
            expect(this.model._getScroller().hasClass("js-entries-container")).to.be.eql(true);
        });
    });

    describe('#getScrollOffset', function() {
        it('-> 2pane', function() {
            this.sinon.stub(Daria, 'is2pane').returns(true);
            expect(this.model.getScrollOffset()).to.be.equal(107);
        });
        it('-> 3pane', function() {
            this.sinon.stub(Daria, 'is2pane').returns(false);
            expect(this.model.getScrollOffset()).to.be.equal(60);
        });
    });
});
