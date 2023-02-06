describe('Daria.vApp', function() {
    beforeEach(function() {
        this.view = ns.View.create('app');
        this.view._loader = {
            start: function() {},
            stop: function() {}
        };
    });

    describe('#closeMessageOnExternalClick', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(Daria.Page.generateUrl, 'closeMessage');
            this.isParamsForMessageStub = this.sinon.stub(Daria.Page, 'isParamsForMessage');
            this.sinon.stub(Jane, 'c');
            this.sinon.stub(Daria, 'is2pane').returns(true);

            var target = {};
            this.event = {
                target: target,
                currentTarget: target
            };
        });

        it('Если сообщение открыто на странице messages, то при клике по пустой области в 2pane должен закрыть сообщение ', function() {
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.isParamsForMessageStub.returns(true);
            this.view.closeMessageOnExternalClick(this.event);

            expect(ns.page.go).to.have.callCount(1);
        });

        it('Если сообщение открыто на странице messages, то при клике не по пустой области в 2pane должен закрыть сообщение ', function() {
            this.event.currentTarget = 'some block';
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.isParamsForMessageStub.returns(true);
            this.view.closeMessageOnExternalClick(this.event);

            expect(ns.page.go).to.have.callCount(0);
        });

        it('Если текущая страница - messages, но открытого сообщения нет, то не должен закрывать сообщение ', function() {
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.isParamsForMessageStub.returns(false);
            this.view.closeMessageOnExternalClick(this.event);

            expect(ns.page.go).to.have.callCount(0);
        });

        it('Если сообщение открыто не на странице messages, то не должен закрывать сообщение ', function() {
            this.sinon.stub(ns.page.current, 'page').value('message');
            this.isParamsForMessageStub.returns(true);
            this.view.closeMessageOnExternalClick(this.event);

            expect(ns.page.go).to.have.callCount(0);
        });

        it('Если сообщение открыто на странице messages, то в 3pane не должен закрывать сообщение ', function() {
            Daria.is2pane.returns(false);
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.isParamsForMessageStub.returns(true);
            this.view.closeMessageOnExternalClick(this.event);

            expect(ns.page.go).to.have.callCount(0);
        });

        it('Если сообщение открыто не на странице messages, то в 3pane не должен закрывать сообщение ', function() {
            Daria.is2pane.returns(false);
            this.sinon.stub(ns.page.current, 'page').value('message');
            this.isParamsForMessageStub.returns(true);
            this.view.closeMessageOnExternalClick(this.event);

            expect(ns.page.go).to.have.callCount(0);
        });
    });

    describe('#_onChangeLeftColumWidth', function() {
        beforeEach(function() {
            this.view.$node = $('<div/>');

            this.mSettings = ns.Model.get('settings');
            this.sinon.stub(this.mSettings, 'setSettings');

            this.sinon.stub(Jane, 'c');
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(this.view, 'resizeHeaderLeftColumn');
            this.appStateSetStub = this.sinon.stub(this.view.getModel('app-state'), 'setIfChanged');

            this.setupLeftColumnNodeWidth = function(width) {
                var $leftColumn = $('<div/>');
                this.sinon.stub(this.view.$node, 'find').returns($leftColumn);
                this.sinon.stub($leftColumn, 'width').returns(width);
            };
        });

        afterEach(function() {
            delete this.view.$node;
        });

        it('<60 => compact', function() {
            this.setupLeftColumnNodeWidth(59);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'compact');
        });

        it('leftColumnWidth=60 => compact', function() {
            this.setupLeftColumnNodeWidth(60);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'compact');
        });

        it('leftColumnWidth=79 => compact', function() {
            this.setupLeftColumnNodeWidth(99);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'compact');
        });

        it('leftColumnWidth=80 => minimum', function() {
            this.setupLeftColumnNodeWidth(100);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'minimum');
        });

        it('leftColumnWidth=179 => minimum', function() {
            this.setupLeftColumnNodeWidth(179);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'minimum');
        });

        it('leftColumnWidth=180 => middle', function() {
            this.setupLeftColumnNodeWidth(180);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'middle');
        });

        it('leftColumnWidth=249 => middle', function() {
            this.setupLeftColumnNodeWidth(249);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'middle');
        });

        it('leftColumnWidth=250 => maximum', function() {
            this.setupLeftColumnNodeWidth(250);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'maximum');
        });

        it('leftColumnWidth=400 => maximum', function() {
            this.setupLeftColumnNodeWidth(400);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'maximum');
        });

        it('leftColumnWidth=401 => maximum', function() {
            this.setupLeftColumnNodeWidth(401);
            this.view._onChangeLeftColumWidth();

            expect(this.appStateSetStub).to.be.calledWith('.asideSizeKind', 'maximum');
        });
        it('При изменении размера левой колонки не выставляется настройка ' +
            'показа папок только непрочитанных писем', function() {
            this.setupLeftColumnNodeWidth(401);
            this.view._onChangeLeftColumWidth();
            expect(this.mSettings.setSettings).to.have.callCount(0);
        });

    });

    describe('#onPageAfterLoad', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, '_onChangeLeftColumWidth');
            this.sinon.stub(Daria.resize, 'reinit3paneVerticalResizer');
            this.sinon.stub(Daria.resize, 'reinit3paneHorizontalResizer');
            this.sinon.stub(ns.page.current, 'page').value('messages');
        });

        it('Должен вызвать "_onChangeLeftColumWidth"', function() {
            this.view.onPageAfterLoad();

            expect(this.view._onChangeLeftColumWidth).to.have.callCount(1);
        });

        it('Должен вызвать "Daria.resize.reinit3paneVerticalResizer" если перешли на страницу "messages" и' +
                ' включен вертикальный 3pane', function() {
            this.sinon.stub(Daria, 'is3pv').returns(true);
            this.view.onPageAfterLoad();

            expect(Daria.resize.reinit3paneVerticalResizer).to.have.callCount(1);
        });

        it('Не должен вызвать "Daria.resize.reinit3paneVerticalResizer" если перешли на страницу "messages" и' +
                ' выключен вертикальный 3pane', function() {
            this.sinon.stub(Daria, 'is3pv').returns(false);
            this.view.onPageAfterLoad();

            expect(Daria.resize.reinit3paneVerticalResizer).to.have.callCount(0);
        });

        it('Должен вызвать "Daria.resize.reinit3paneHorizontalResizer" если перешли на страницу "messages" и' +
                ' включен горизонтальный 3pane', function() {
            this.sinon.stub(Daria, 'is3ph').returns(true);
            this.view.onPageAfterLoad();

            expect(Daria.resize.reinit3paneHorizontalResizer).to.have.callCount(1);
        });

        it('Не должен вызвать "Daria.resize.reinit3paneHorizontalResizer" если перешли на страницу "messages" и' +
                ' выключен горизонтальный 3pane', function() {
            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.view.onPageAfterLoad();

            expect(Daria.resize.reinit3paneHorizontalResizer).to.have.callCount(0);
        });
    });
});
