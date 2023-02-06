describe('Daria.vMessageBody', function() {
    beforeEach(function() {
        this.vMessageBody = ns.View.create('message-body', { ids: '1' });
        this.vMessageBody.$node = $("<div></div>");
        this.l = $("<a name='kot' href='#kot' data-vdir-href='#link'>Name of Link</a>");
        this.l.appendTo(this.vMessageBody.$node[0]);
        this.event = {
            currentTarget: this.l[0]
        };

        this.sinon.stub(Daria, 'isMessagePage').returns(true);
        this.sinon.spy(Jane.DOM, 'placeInViewport');
    });

    describe('#changeLinkHref', function() {
        describe('Если ссылка внешняя ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'isInnerMailLinkHostname').returns(false);
            });
            it('открываем как обычную внешнюю ссылку, внутри письма подскролл не делаем', function() {
                expect(this.vMessageBody.changeLinkHref(this.event)).to.be.eql(true);
                expect(Jane.DOM.placeInViewport).to.have.callCount(0);
            });
        });
        describe('Если ссылка внутренняя ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'isInnerMailLinkHostname').returns(true);
            });
            it('подскролливаем по нужному якорю внутри письма', function() {
                expect(this.vMessageBody.changeLinkHref(this.event)).to.be.eql(false);
                expect(Jane.DOM.placeInViewport).to.have.callCount(1);
            });
        });
    });

});
