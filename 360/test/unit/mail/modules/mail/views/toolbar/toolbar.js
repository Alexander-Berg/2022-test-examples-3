describe('Daria.vToolbar', function() {
    beforeEach(function() {
        this.view = ns.View.create('toolbar');

        this.sinon.stubGetModel(this.view, [ 'toolbar', 'scroller-messages', 'settings' ]);

        this.sinon.stub(this.mToolbar, 'toggleFixed');
        this.sinon.stub(this.view, '_toggleFixed');
    });

    describe('#onOpenMessage ->', function() {
        it('При открытии письма убираем залипание у тулбара списка писем', function() {
            this.view.onOpenMessage();
            expect(this.view._toggleFixed).to.be.calledWith(false);
        });
    });

    describe('#_isMessageOpen ->', function() {
        it('Если письмо открыто, триггерится событие daria:vToolbar:message-opened', function() {
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(ns.page.current, 'params').value({
                thread_id: '123456789'
            });

            this.view._isMessageOpen();
            expect(ns.events.trigger).to.be.calledWith('daria:vToolbar:message-opened');
        });
        it('Если письмо открыто, если в параметрах есть thread_id или ids', function() {
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(ns.page.current, 'params').value({
                thread_id: '123456789'
            });

            expect(this.view._isMessageOpen()).to.be.equal(true);
        });
        it('Если нет thread_id или ids, то письмо не открыто', function() {
            this.sinon.stub(ns.events, 'trigger');

            this.sinon.stub(ns.page.current, 'params').value({});

            expect(this.view._isMessageOpen()).to.be.equal(false);
        });
    });
    describe('#onChangeCheckedCount', function() {
        beforeEach(function() {
            this.view.$node = $('<div class="toolbar"></div>');
            this.sinon.stub(Daria, 'React').value({
                showLastSearchQuery: this.sinon.stub(),
                toggleSuggestFolded: this.sinon.stub()
            });
        });

        describe('Если режим обычный', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.CompactMode, 'isCompactHeader').returns(false);
            });
            it('Если не выделено ни одного письма - тулбар неактивен', function() {
                this.view.onChangeCheckedCount({}, 0);

                expect(this.view.$node.hasClass('is-active')).be.equal(false);
                expect(Daria.React.showLastSearchQuery).to.have.callCount(0);
                expect(Daria.React.toggleSuggestFolded).to.have.callCount(0);
            });

            it('Если выделено несколько писем - тулбар остается активным', function() {
                this.view.onChangeCheckedCount({}, 11);

                expect(this.view.$node.hasClass('is-active')).be.equal(true);
                expect(Daria.React.showLastSearchQuery).to.have.callCount(0);
                expect(Daria.React.toggleSuggestFolded).to.have.callCount(0);
            });
        });

        describe('Если режим компактный', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.CompactMode, 'isCompactHeader').returns(true);
                this.sinon.stub(Daria, 'React').value({
                    getSearchBoxIsFolded: this.sinon.stub(),
                    showLastSearchQuery: this.sinon.stub(),
                    toggleSuggestFolded: this.sinon.stub()
                });
            });

            describe('Если не выбрано ни одного письма ->', function() {
                it('Ранее не показывали саджест, то и не показываем, тулбар не активен', function() {
                    Daria.React.getSearchBoxIsFolded.returns(true);

                    this.view.onChangeCheckedCount({}, 0);

                    expect(Daria.React.showLastSearchQuery).to.have.callCount(0);
                    expect(Daria.React.toggleSuggestFolded).to.have.callCount(0);
                    expect(this.view.wasSuggestFoldBeforeToolbarOpened).to.equal(undefined);
                    expect(this.view.$node.hasClass('is-active')).to.equal(false);
                });
                it('Ранее показывали саджест и мы сейчас в поисковой странице, ' +
                    'так его и показывать с запросом который был до чека писем', function() {
                    Daria.React.getSearchBoxIsFolded.returns(false);
                    this.sinon.stub(Daria, 'isSearchPage').returns(true);
                    this.view.wasSuggestFoldBeforeToolbarOpened = true;

                    this.view.onChangeCheckedCount({}, 0);

                    expect(Daria.React.showLastSearchQuery).to.have.callCount(1);
                    expect(Daria.React.toggleSuggestFolded).to.have.callCount(1);
                    expect(Daria.React.toggleSuggestFolded).to.calledWith(false);
                    expect(this.view.wasSuggestFoldBeforeToolbarOpened).to.equal(false);
                    expect(this.view.$node.hasClass('is-active')).to.equal(false);
                });
                it('Если анчекнули письма, до чека показывали саджест но вернулись в инбокс,' +
                    ' то не показывать саджест', function() {
                    Daria.React.getSearchBoxIsFolded.returns(false);
                    this.sinon.stub(Daria, 'isSearchPage').returns(false);
                    this.view.wasSuggestFoldBeforeToolbarOpened = true;

                    this.view.onChangeCheckedCount({}, 0);

                    expect(Daria.React.toggleSuggestFolded).to.have.callCount(0);
                    expect(Daria.React.showLastSearchQuery).to.have.callCount(0);
                    expect(this.view.wasSuggestFoldBeforeToolbarOpened).to.equal(true);
                    expect(this.view.$node.hasClass('is-active')).to.equal(false);
                });
            });
            describe('Если выбрано несколько писем ->', function() {
                it('Ранее показывали саджест, скрыть его и показать тулбар', function() {
                    Daria.React.getSearchBoxIsFolded.returns(false);

                    this.view.onChangeCheckedCount({}, 11);

                    expect(Daria.React.toggleSuggestFolded).to.have.callCount(1);
                    expect(Daria.React.toggleSuggestFolded).to.calledWith(true);
                    expect(this.view.wasSuggestFoldBeforeToolbarOpened).to.equal(true);
                    expect(this.view.$node.hasClass('is-active')).to.equal(true);
                });
                it('Ранее не показывали саджест, то не показывать его и показать активный тулбар', function() {
                    Daria.React.getSearchBoxIsFolded.returns(true);

                    this.view.onChangeCheckedCount({}, 11);

                    expect(Daria.React.toggleSuggestFolded).to.have.callCount(0);
                    expect(this.view.wasSuggestFoldBeforeToolbarOpened).to.equal(undefined);
                    expect(this.view.$node.hasClass('is-active')).to.equal(true);
                });
            });
        });
    });
});
