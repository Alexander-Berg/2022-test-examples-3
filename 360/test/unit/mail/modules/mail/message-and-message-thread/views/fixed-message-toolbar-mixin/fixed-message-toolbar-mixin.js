describe('Daria.vFixedMessageToolbarMixin', function() {
    before(function() {
        // вьюшка-наследник для тестов
        ns.View.edefine('test-fixed-message-toolbar-mixin', {
            models: {
                'scroller-message': false
            }
        }, 'message', 'fixed-message-toolbar-mixin', 'message-dimensions-mixin', 'scenario-manager-mixin');
    });

    beforeEach(function() {
        this.view = ns.View.create('test-fixed-message-toolbar-mixin');
        this.$node = $('<div class="node"/>').appendTo('body');
        this.view.$node = this.$node;
        this.mMessage = ns.View.create('message', { ids: '12334' });

        this.scroller = this.view.getModel('scroller-message');

        this.view.$messageHead = $('<div class="js-message-head"></div>').appendTo(this.$node);
        this.view.$messageHeadHeight = this.view.$messageHead.height();
        this.view.$header = $('<div class="js-thread-toolbar"></div>').appendTo(this.$node);
        this.view.$headerHeight = this.view.$header.height();
        this.view.$toolbar = $('<div class="js-message-toolbar-content" style="height: 20px"></div>').appendTo(this.$node);
        this.view.$toolbarHeight = this.view.$toolbar.height();
    });

    describe('_fixedToolbar', function() {
        it('Если письмо не видно в области видимости, то принудительно отлепляем тулбар письма', function() {
            this.sinon.stub(this.scroller, 'getScrollTop').returns(0);
            this.sinon.stub(this.view, 'isOpen').returns(true);
            this.sinon.stub(this.scroller, 'getVDimensions').returns({
                bottom: 1174,
                top: 662
            });

            this.sinon.stub(this.view, 'isVisibleInScroller').returns(false);

            this.sinon.stub(this.view, '_toggleFixed');
            this.view._fixedToolbar();
            expect(this.view._toggleFixed).to.be.calledWith(false);
        });

        it('Если письмо видимо в области видимости, и мы уже достигли скроллом тулбара, то прилеплять его', function() {
            this.sinon.stub(this.scroller, 'getScrollTop').returns(386);
            this.sinon.stub(this.view, 'isOpen').returns(true);
            this.sinon.stub(this.scroller, 'getVDimensions').returns({
                bottom: 893,
                top: 446
            });

            this.sinon.stub(this.view, 'isVisibleInScroller').returns(true);
            this.sinon.stub(this.view, 'getDimensions').returns({
                bottom: 676,
                height: 367,
                top: 309
            });

            this.sinon.stub(this.view, '_toggleFixed');
            this.view._fixedToolbar();
            expect(this.view._toggleFixed).to.be.calledWith(true);
        });

        it('Если письмо видимо в области видимости, и мы достигли конца письма, то отлепляем тулбар', function() {
            this.sinon.stub(this.scroller, 'getScrollTop').returns(604);
            this.sinon.stub(this.view, 'isOpen').returns(true);
            this.sinon.stub(this.scroller, 'getVDimensions').returns({
                bottom: 1111,
                top: 664
            });

            this.sinon.stub(this.view, 'isVisibleInScroller').returns(true);
            this.sinon.stub(this.view, 'getDimensions').returns({
                bottom: 676,
                height: 367,
                top: 309
            });

            this.sinon.stub(this.view, '_toggleFixed');
            this.view._fixedToolbar();
            expect(this.view._toggleFixed).to.be.calledWith(false);
        });
    });

    describe('_toggleFixed', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane').returns(false);
            this.sinon.stub(this.scroller, 'getScroller');
        });

        it('Если передано true в аргументе, то прилепляем тулбар (инлайновые стили добавляются)', function() {
            this.view._toggleFixed(true);
            var style = this.view.$toolbar[0].style.cssText.replace(/\s?width:\s?\d+px;?/, '');
            expect(style).to.be.equal('height: 20px; left: 8px; top: 0px;');
        });

        it('Если передано true в аргументе, то прилепляем тулбар (добавляется класс)', function() {
            this.view._toggleFixed(true);
            expect(this.view.$toolbar.hasClass('is-fixed')).to.be.equal(true);
        });

        it('Если передано false в аргументе, то отлепляем тулбар (убираем инлайновые стили)', function() {
            this.view._toggleFixed(false);
            expect(this.view.$toolbar[0].style.cssText).to.be.equal('');
        });

        it('Если передано false в аргументе, то отлепляем тулбар (убираем класс)', function() {
            this.view._toggleFixed(false);
            expect(this.view.$toolbar.hasClass('is-fixed')).to.be.equal(false);
        });
    });
});
