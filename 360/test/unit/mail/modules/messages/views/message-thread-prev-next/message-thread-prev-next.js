describe('Daria.vMessageThreadPrevNext', function() {
    beforeEach(function() {
        this.view = ns.View.create('message-thread-prev-next', { ids: '112233' });

        this.view.$prevArrow = {
            toggleClass: this.sinon.stub()
        };
        this.view.$nextArrow = {
            toggleClass: this.sinon.stub()
        };

        this.mMessage1 = ns.Model.get('message', { ids: '112233' });
        this.mMessage2 = ns.Model.get('message', { ids: '112244' });
        this.mMessage3 = ns.Model.get('message', { ids: '112255' });
        this.mMessage4 = ns.Model.get('message', { ids: '112266' });
        this.mMessage5 = ns.Model.get('message', { ids: '112277' });
        this.mMessage6 = ns.Model.get('message', { ids: '112288' });

        this.sinon.stub(this.mMessage1, 'isPinned').returns(true);
        this.sinon.stub(this.mMessage2, 'isPinned').returns(true);
        this.sinon.stub(this.mMessage3, 'isPinned').returns(true);
        this.sinon.stub(this.mMessage4, 'isPinned').returns(false);
        this.sinon.stub(this.mMessage5, 'isPinned').returns(false);
        this.sinon.stub(this.mMessage6, 'isPinned').returns(false);

        this.mMessages = ns.Model.get('messages');
        this.mMessages.setData({
            message: []
        });

        this.mMessages.insert([
            this.mMessage1,
            this.mMessage2,
            this.mMessage3,
            this.mMessage4,
            this.mMessage5,
            this.mMessage6
        ]);
        this.canLoadMoreStub = this.sinon.stub(this.mMessages, 'canLoadMore');

        this.sinon.stub(this.view, 'getMessagesModel').returns(this.mMessages);
        this.getModelMessageStub = this.sinon.stub(this.view, 'getModel').withArgs('message');
    });

    describe('#updateArrowBlocks', function() {
        beforeEach(function() {
            this.canOpenThreadStub = this.sinon.stub(this.view, 'canOpenThread');
        });

        it('должен включить стрелку "Предыдущий", если доступен предыдущий тред', function() {
            this.canOpenThreadStub.withArgs('prev').returns(true);

            this.view.updateArrowBlocks();
            expect(this.view.$prevArrow.toggleClass).to.be.calledWith('is-disabled', false);
        });

        it('должен включить стрелку "Следующий", если доступен следующий тред', function() {
            this.canOpenThreadStub.withArgs('next').returns(true);

            this.view.updateArrowBlocks();
            expect(this.view.$nextArrow.toggleClass).to.be.calledWith('is-disabled', false);
        });

        it('должен выключить стрелку "Предыдущий", если недоступен предыдущий тред', function() {
            this.canOpenThreadStub.withArgs('prev').returns(false);

            this.view.updateArrowBlocks();
            expect(this.view.$prevArrow.toggleClass).to.be.calledWith('is-disabled', true);
        });

        it('должен выключить стрелку "Следующий", если недоступен следующий тред', function() {
            this.canOpenThreadStub.withArgs('next').returns(false);

            this.view.updateArrowBlocks();
            expect(this.view.$nextArrow.toggleClass).to.be.calledWith('is-disabled', true);
        });
    });

    describe('#canOpenThread', function() {
        describe('для position !== `next`', function() {
            it('должен вернуть true, если есть тред для позиции и isPinned у него совпадает с текущим', function() {
                this.view.params.ids = this.mMessage2.params.ids;
                this.getModelMessageStub.returns(this.mMessage2);

                var canOpenThread = this.view.canOpenThread('prev');
                expect(canOpenThread).to.be.equal(true);
            });

            it('должен вернуть false, если есть тред для позиции, но isPinned у него не совпадает с текущим', function() {
                this.view.params.ids = this.mMessage4.params.ids;
                this.getModelMessageStub.returns(this.mMessage4);

                var canOpenThread = this.view.canOpenThread('prev');
                expect(canOpenThread).to.be.equal(false);
            });

            it('должен вернуть false, если нет треда для позиции', function() {
                this.view.params.ids = this.mMessage1.params.ids;
                this.getModelMessageStub.returns(this.mMessage1);

                var canOpenThread = this.view.canOpenThread('prev');
                expect(canOpenThread).to.be.equal(false);
            });

            it('должен вернуть false, если позиция не задана', function() {
                this.view.params.ids = this.mMessage1.params.ids;
                this.getModelMessageStub.returns(this.mMessage1);

                var canOpenThread = this.view.canOpenThread();
                expect(canOpenThread).to.be.equal(false);
            });
        });

        describe('для position === `next`', function() {
            it('должен вернуть true, если есть следующий тред и isPinned у него совпадает с текущим', function() {
                this.view.params.ids = this.mMessage1.params.ids;
                this.getModelMessageStub.returns(this.mMessage1);

                var canOpenThread = this.view.canOpenThread('next');
                expect(canOpenThread).to.be.equal(true);
            });

            it('должен вернуть false, если есть следующий тред, но isPinned у него не совпадает с текущим', function() {
                this.view.params.ids = this.mMessage3.params.ids;
                this.getModelMessageStub.returns(this.mMessage3);

                var canOpenThread = this.view.canOpenThread('next');
                expect(canOpenThread).to.be.equal(false);
            });

            it('должен вернуть false, если есть следующий тред, но isPinned у него не совпадает с текущим, даже если можем загрузить ещё письма', function() {
                this.view.params.ids = this.mMessage3.params.ids;
                this.getModelMessageStub.returns(this.mMessage3);
                this.canLoadMoreStub.returns(true);

                var canOpenThread = this.view.canOpenThread('next');
                expect(canOpenThread).to.be.equal(false);
            });

            it('должен вернуть true, если нет следующего треда, но можно загрузить ещё', function() {
                this.view.params.ids = this.mMessage6.params.ids;
                this.getModelMessageStub.returns(this.mMessage6);
                this.canLoadMoreStub.returns(true);

                var canOpenThread = this.view.canOpenThread('next');
                expect(canOpenThread).to.be.equal(true);
            });

            it('должен вернуть false, если нет следующего треда и нельзя загрузить ещё', function() {
                this.view.params.ids = this.mMessage6.params.ids;
                this.getModelMessageStub.returns(this.mMessage6);
                this.canLoadMoreStub.returns(false);

                var canOpenThread = this.view.canOpenThread('next');
                expect(canOpenThread).to.be.equal(false);
            });
        });
    });

    describe('#openThreadForPosition', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Page.generateUrl, 'contentMessage3pane').returns('messageUrl');
            this.sinon.stub(ns.page, 'go').withArgs('messageUrl');

            this.sinon.stub(this.mMessages, 'loadMore')
            .returns({
                then: function(func, ctx) {
                    func.apply(ctx);
                }
            });
        });

        it('должен перейти на страницу предыдущего треда, если есть предыдущий тред', function() {
            this.view.params.ids = this.mMessage3.params.ids;
            this.getModelMessageStub.returns(this.mMessage3);

            this.view.openThreadForPosition('prev');
            expect(ns.page.go).to.have.callCount(1);
        });

        it('не должен выполнять переход, если нет предыдущего треда', function() {
            this.view.params.ids = this.mMessage1.params.ids;
            this.getModelMessageStub.returns(this.mMessage1);

            this.view.openThreadForPosition('prev');
            expect(ns.page.go).to.have.callCount(0);
        });

        it('должен перейти на страницу следующего треда, если есть следующий тред', function() {
            this.view.params.ids = this.mMessage1.params.ids;
            this.getModelMessageStub.returns(this.mMessage1);

            this.view.openThreadForPosition('next');
            expect(ns.page.go).to.have.callCount(1);
        });

        it('не должен выполнять переход, если нет следующего треда', function() {
            this.view.params.ids = this.mMessage6.params.ids;
            this.getModelMessageStub.returns(this.mMessage6);

            this.view.openThreadForPosition('next');
            expect(ns.page.go).to.have.callCount(0);
        });

        it('не должен пытаться загрузить ещё сообщения, если нет следующего треда и нельзя загрузить ещё сообщения', function() {
            this.view.params.ids = this.mMessage6.params.ids;
            this.getModelMessageStub.returns(this.mMessage6);
            this.canLoadMoreStub.returns(false);

            this.view.openThreadForPosition('next');
            expect(ns.page.go).to.have.callCount(0);
        });

        it('должен финишировать активный сценарий "Просмотр письма"', function() {
            const scenarioManager = this.sinon.stubScenarioManager(this.view);
            this.view.params.ids = this.mMessage1.params.ids;
            this.getModelMessageStub.returns(this.mMessage1);

            this.view.openThreadForPosition('next');

            expect(scenarioManager.finishScenarioIfActive)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'close-and-open-another-message');
        });

        it('должен запустить сценарий "Просмотр письма" с триггером "thread-next-click"', function() {
            const scenarioManager = this.sinon.stubScenarioManager(this.view);
            this.view.params.ids = this.mMessage1.params.ids;
            this.getModelMessageStub.returns(this.mMessage1);

            this.view.openThreadForPosition('next');

            expect(scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'thread-next-click');
        });

        it('должен запустить сценарий "Просмотр письма" с триггером "thread-prev-click"', function() {
            const scenarioManager = this.sinon.stubScenarioManager(this.view);
            this.view.params.ids = this.mMessage3.params.ids;
            this.getModelMessageStub.returns(this.mMessage3);

            this.view.openThreadForPosition('prev');

            expect(scenarioManager.startScenario)
                .to.have.callCount(1)
                .and.to.be.calledWith('message-view-scenario', 'thread-prev-click');
        });

        describe('нет следующего треда, но можно загрузить ещё сообщения: ', function() {
            beforeEach(function() {
                this.view.params.ids = this.mMessage6.params.ids;
                this.getModelMessageStub.returns(this.mMessage6);
                this.canLoadMoreStub.returns(true);

                this.sinon.stub(this.view, 'canOpenThread').withArgs('next');

                this.sinon.stub(this.mMessages, 'getNearestMessageByMid');
                this.mMessages.getNearestMessageByMid.onFirstCall().returns(false);
                // имитирует загруженное после вызова loadMore сообщение
                this.mMessage7 = ns.Model.get('message', { ids: '112299' });
                this.mMessages.getNearestMessageByMid.onSecondCall().returns(this.mMessage7);
            });

            it('должен загрузить ещё сообщения', function() {
                this.view.openThreadForPosition('next');

                expect(this.mMessages.loadMore).to.have.callCount(1);
            });

            it('должен вызвать метод открытия следующего треда после подгрузки сообщений, если тред можно открыть', function() {
                this.view.canOpenThread.returns(true);
                this.view.openThreadForPosition('next');

                expect(ns.page.go).to.be.calledAfter(this.mMessages.loadMore);
                expect(ns.page.go).to.have.callCount(1);
            });

            it('должен выключить стрелку "Следующий тред" после подгрузки сообщений, если тред нельзя открыть', function() {
                this.view.canOpenThread.returns(false);
                this.view.openThreadForPosition('next');

                expect(this.view.$nextArrow.toggleClass).to.be.calledAfter(this.mMessages.loadMore);
                expect(this.view.$nextArrow.toggleClass).to.be.calledWith('is-disabled', true);
            });
        });
    });
});
