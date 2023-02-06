describe('Daria.Focus', function() {
    beforeEach(function() {
        this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([ [], [], [] ]);
        this.mFocus = ns.Model.get('focus');
        this.mScrollerLeft = ns.Model.get('scroller-left');
        this.mScrollerMessages = ns.Model.get('scroller-messages');
        this.mScrollerMessage = ns.Model.get('scroller-message');

        this.sinon.stub(Daria, 'hasAsideScrollContainer');
        this.sinon.stub(Daria, 'is2pane');
        this.sinon.stub(Daria, 'is3pane');

        this.$viewNode = $('<div class="mail-Element" style="height: 20px"></div>');
    });

    describe('подскроллы', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Focus, 'scrollToTopMessages');
        });

        describe('scrollIntoView', function() {
            it('В режиме превью темы ничего не подскролливаем', function() {
                this.mSkinSaverState = ns.Model.get('skin-saver-state');
                this.sinon.stub(this.mSkinSaverState, 'isPreviewMode').returns(true);
                this.sinon.stub(this.mScrollerLeft, 'setScrollTop');
                this.sinon.stub(Daria.Focus, 'getScroller').returns(this.mScrollerLeft);

                Daria.Focus.scrollIntoView(this.$viewNode, 0, {});

                expect(this.mScrollerLeft.setScrollTop).to.have.callCount(0);
            });

            describe('Скролл вверх-вниз', function() {
                beforeEach(function() {
                    this.sinon.stub(this.mScrollerLeft, 'setScrollTop');
                    this.sinon.stub(this.mScrollerLeft, 'getScrollOffset');
                    this.sinon.stub(this.mScrollerLeft, 'getScrollTop');
                    this.sinon.stub(this.mScrollerLeft, 'getNodeTop');
                    this.sinon.stub(Daria.Focus, 'getScroller').returns(this.mScrollerLeft);
                });

                it('Подскролливаем вниз, nodeBottom позади scrollBottom', function() {
                    this.mScrollerLeft.getScrollOffset.returns(18);
                    this.mScrollerLeft.getScrollTop.returns(60);
                    this.mScrollerLeft.getNodeTop.returns(100);

                    Daria.Focus.scrollIntoView(this.$viewNode, 0, {});
                    expect(this.mScrollerLeft.setScrollTop).calledWith(80);
                });

                it('Подскролливаем вверх, nodeTop позади scrollNodeTop', function() {
                    this.mScrollerLeft.getScrollOffset.returns(30);
                    this.mScrollerLeft.getScrollTop.returns(2);
                    this.mScrollerLeft.getNodeTop.returns(33);

                    Daria.Focus.scrollIntoView(this.$viewNode, 0, {});
                    expect(this.mScrollerLeft.setScrollTop).calledWith(1);
                });

                it('Не подскролливаем, если диффа нет ни при скролле вверх ни вниз', function() {
                    this.mScrollerLeft.getScrollOffset.returns(18);
                    this.mScrollerLeft.getScrollTop.returns(70);
                    this.mScrollerLeft.getNodeTop.returns(100);
                    this.sinon.stub(this.mScrollerLeft, 'getHeight').returns(140);

                    Daria.Focus.scrollIntoView(this.$viewNode, 0, {});
                    expect(this.mScrollerLeft.setScrollTop).to.have.callCount(0);
                });
            });
        });
    });

    describe('moveItem', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Focus, 'lazyScroll');
            this.sinon.stub(this.mFocus, 'changeItem');
            this.sinon.stub(this.mFocus, 'setColumn');

            this._page = ns.page.current.page;

            ns.page.current.page = 'messages';
        });

        afterEach(function() {
            ns.page.current.page = this._page;
        });

        it('Должен вызвать смену элемента, если не нужен скроллинг страницы', function() {
            Daria.Focus.moveItem({ toForward: true });

            expect(Daria.Focus.lazyScroll).to.have.callCount(0);
            expect(this.mFocus.changeItem).to.have.callCount(1);
        });

        it('Должен вызвать скроллинг страницы вместо смены элемента, если мы на странице письма и фокус не на левой колонке', function() {
            ns.page.current.page = 'message';
            this.mFocus.set('.columnIndex', this.mFocus.indexColumns.MESSAGES);

            Daria.Focus.moveItem({ toForward: true });

            expect(Daria.Focus.lazyScroll).to.have.callCount(1);
            expect(this.mFocus.changeItem).to.have.callCount(0);
        });

        it('Не должен вызывать скроллинг страницы вместо смены элемента, если фокус на левой колонке', function() {
            ns.page.current.page = 'message';
            this.mFocus.set('.columnIndex', this.mFocus.indexColumns.LEFT);

            Daria.Focus.moveItem({ toForward: true });

            expect(Daria.Focus.lazyScroll).to.have.callCount(0);
        });

        it('Не должен вызывать скроллинг страницы вместо смены элемента, если мы не на странице письма', function() {
            Daria.Focus.moveItem({ toForward: true });

            expect(Daria.Focus.lazyScroll).to.have.callCount(0);
        });

        it('Должен вызывать установку колонки, если вызывается принудительная навигация по письмам', function() {
            Daria.Focus.moveItem({ toForward: true, focusMessages: true });

            expect(this.mFocus.changeItem).to.have.callCount(1);
            expect(this.mFocus.setColumn).to.have.callCount(1);
        });
    });
});
