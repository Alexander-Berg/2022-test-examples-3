describe('Daria.vPreventScrollPropagationMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('prevent-scroll-propagation-mixin');
    });

    describe('#onWheel', function() {
        beforeEach(function() {
            this.event = {
                preventDefault: this.sinon.stub(),
                stopImmediatePropagation: this.sinon.stub(),
                originalEvent: {
                    deltaY: 0
                }
            };

        });
        it('-> если контента нет, то скролла не будет', function() {
            this.sinon.stub(this.view, 'getScrollContent').returns('');
            this.view.onWheel(this.event);
            expect(this.event.preventDefault).to.have.callCount(1);
        });
        it('-> если не пытаемся скроллить в пределах контента, то скролл не разрешен #1', function() {
            this.element = {
                offsetHeight: 200,
                scrollHeight: 200,
                scrollTop: 0
            };

            this.sinon.stub(this.view, 'getScrollContent').returns(this.element);
            this.sinon.stub(this.event, 'originalEvent').value({deltaY: -1});

            this.view.onWheel(this.event);

            expect(this.event.preventDefault).to.have.callCount(1);
        });
        it('-> если не пытаемся скроллить в пределах контента, то скролл не разрешен #2', function() {
            this.element = {
                offsetHeight: 200,
                scrollHeight: 100,
                scrollTop: 0
            };

            this.sinon.stub(this.view, 'getScrollContent').returns(this.element);
            this.sinon.stub(this.event, 'originalEvent').value({deltaY: 11});

            this.view.onWheel(this.event);

            expect(this.event.preventDefault).to.have.callCount(1);
        });

        it('-> если пытаемся скроллить в пределах контента, то скролл разрешен', function() {
            this.element = {
                offsetHeight: 20,
                scrollHeight: 100,
                scrollTop: 0
            };

            this.sinon.stub(this.view, 'getScrollContent').returns(this.element);
            this.sinon.stub(this.event, 'originalEvent').value({deltaY: 11});

            this.view.onWheel(this.event);

            expect(this.event.preventDefault).to.have.callCount(0);
        });
    });
});
