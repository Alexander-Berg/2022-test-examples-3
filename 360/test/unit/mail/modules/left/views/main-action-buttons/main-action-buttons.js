describe('Daria.vMainActionButtons', function() {
    beforeEach(function() {
        this.vMainActionButtons = ns.View.create('main-action-buttons');
        this.vMainActionButtons.$node = $('<div class="js-compose-button-container"></div>');
        this.scroller = ns.Model.get('scroller-left');
        this.sinon.stub(this.vMainActionButtons, '_getScrollerModel').returns(this.scroller);
        this.sinon.stub(this.scroller, 'setScrollTop');
    });

    describe('onFolderChanged', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane');
            this.sinon.stub(Daria, 'is2pane');
            this.sinon.stub(Daria, 'hasAsideScrollContainer');

            this.$scrollerNode = $('<div class=\'js-scroller-left\'></div>');
            this.sinon.stub(this.vMainActionButtons, '_getScrollerNode').returns(this.$scrollerNode);
            this.sinon.stub(this.vMainActionButtons, '_hasScrollbar');
            this.sinon.stub(this.vMainActionButtons, '_isNeedToDetectScrollbar');
        });

        it('Если выполнены условия для вызова метода и есть скролл, ставим класс на ноду', function() {
            this.vMainActionButtons._isNeedToDetectScrollbar.returns(true);
            this.vMainActionButtons._hasScrollbar.returns(true);

            expect(this.vMainActionButtons.$node.hasClass('with-padding')).to.eql(false);
        });

        it('Если выполнены условия для вызова метода, не ставим класс на ноду', function() {
            this.vMainActionButtons._isNeedToDetectScrollbar.returns(false);

            expect(this.vMainActionButtons.$node.hasClass('with-padding')).to.eql(false);
        });
    });

    it('_adjustElementsForStickyComposeButtons', function() {
        this.sinon.stub(this.vMainActionButtons, 'toggleFixedComposeButtons');
        this.sinon.stub(this.vMainActionButtons, 'setOptimalWidthForComposeButtons');
        this.sinon.stub(this.vMainActionButtons, 'toggleVisibilityStickyComposeButtons');
        this.sinon.stub(this.vMainActionButtons, 'toggleFixedLeftColumnToggler');

        this.vMainActionButtons._adjustElementsForStickyComposeButtons();

        expect(this.vMainActionButtons.toggleFixedComposeButtons).to.have.callCount(1);
        expect(this.vMainActionButtons.setOptimalWidthForComposeButtons).to.have.callCount(1);
        expect(this.vMainActionButtons.toggleVisibilityStickyComposeButtons).to.have.callCount(1);
        expect(this.vMainActionButtons.toggleFixedLeftColumnToggler).to.have.callCount(1);
    });

    describe('toggleVisibilityStickyComposeButtons', function() {
        beforeEach(function() {
            this.testNode = $('<div></div>');
            this.sinon.stub(this.vMainActionButtons, '_getComposeButtonsContainer')
                .returns(this.testNode);
            this.sinon.stub(this.vMainActionButtons, '_addUnfoldedSearchFlag');
        });
        it('Если не компактный режим + не 2pane, ничего не делаем', function() {
            this.sinon.stub(Daria, 'is2pane').returns(false);
            this.sinon.stub(Daria.CompactMode, 'isCompactHeader').returns(false);

            this.vMainActionButtons.toggleVisibilityStickyComposeButtons();

            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.have.callCount(0);
        });

        it('Если выполнены условия для скрытия кнопок композа в залипшем состоянии (тулбар не скрыт), ' +
            'скрываем ноды если раскрыт поиск', function() {
            this.sinon.stub(Daria, 'is2pane').returns(true);
            this.sinon.stub(Daria.CompactMode, 'isCompactHeader').returns(true);
            this.sinon.stub(this.vMainActionButtons, '_isMainToolbarHidden').returns(false);

            this.vMainActionButtons.toggleVisibilityStickyComposeButtons({}, true);

            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.have.callCount(1);
            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.calledWith(true);
        });

        it('Если выполнены условия для скрытия кнопок композа в залипшем состоянии, ' +
            'не скрываем ноды если не раскрыт поиск', function() {
            this.sinon.stub(Daria, 'is2pane').returns(true);
            this.sinon.stub(Daria.CompactMode, 'isCompactHeader').returns(true);

            this.vMainActionButtons.toggleVisibilityStickyComposeButtons({}, false);

            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.have.callCount(1);
            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.calledWith(false);
        });

        it('Если выполнены условия для скрытия кнопок композа в залипшем состоянии (тулбар скрыт), ' +
            'скрываем ноды если раскрыт поиск', function() {
            this.sinon.stub(Daria, 'is2pane').returns(true);
            this.sinon.stub(Daria.CompactMode, 'isCompactHeader').returns(true);
            this.sinon.stub(this.vMainActionButtons, '_isMainToolbarHidden').returns(true);

            this.vMainActionButtons.toggleVisibilityStickyComposeButtons({}, true);

            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.have.callCount(1);
            expect(this.vMainActionButtons._addUnfoldedSearchFlag).to.calledWith(true);
        });
    });

    describe('toggleFixedLeftColumnToggler', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is2pane');
            this.sinon.stub(this.vMainActionButtons, '_isIE');
            this.sinon.stub(this.vMainActionButtons, '_toggleFixedLayoutAsideNodeIE');
            this.sinon.stub(this.vMainActionButtons, '_isMainToolbarFixed');
        });

        it('Не вызываем логику для IE/Edge если нет условий', function() {
            this.vMainActionButtons._isIE.returns(false);
            Daria.is2pane.returns(false);
            this.vMainActionButtons.toggleFixedLeftColumnToggler();

            expect(this.vMainActionButtons._toggleFixedLayoutAsideNodeIE).to.have.callCount(0);
        });

        it('Не вызываем логику для IE/Edge если нет условий 2', function() {
            this.vMainActionButtons._isIE.returns(true);
            Daria.is2pane.returns(false);
            this.vMainActionButtons.toggleFixedLeftColumnToggler();

            expect(this.vMainActionButtons._toggleFixedLayoutAsideNodeIE).to.have.callCount(0);
        });

        it('Не вызываем логику для IE/Edge если нет условий 3', function() {
            this.vMainActionButtons._isIE.returns(false);
            Daria.is2pane.returns(false);
            this.vMainActionButtons.toggleFixedLeftColumnToggler();

            expect(this.vMainActionButtons._toggleFixedLayoutAsideNodeIE).to.have.callCount(0);
        });

        it('Вызываем логику для IE/Edge если есть условия 1', function() {
            this.vMainActionButtons._isIE.returns(true);
            Daria.is2pane.returns(true);
            this.vMainActionButtons.toggleFixedLeftColumnToggler();

            expect(this.vMainActionButtons._toggleFixedLayoutAsideNodeIE).to.have.callCount(1);
        });
    });
});
