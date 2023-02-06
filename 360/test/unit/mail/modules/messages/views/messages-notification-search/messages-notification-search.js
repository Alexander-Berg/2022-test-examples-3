describe('vMessagesNotificationSearch', function() {
    beforeEach(function() {
        this.requestString = 'request';
        this.vMessagesNotificationSearch = ns.View.create('messages-notification-search', {request: this.requestString});
        this.sinon.stub(Daria, 'React').value({
            startSearch: this.sinon.stub(),
            setSearchTrigger: this.sinon.stub(),
            SEARCH_TRIGGER: {}
        });
    });

    describe('#shouldShowFeedback', function() {
        describe('this._shouldShowFeedback === null', function() {
            beforeEach(function() {
                this.getSetting = this.sinon.stub();
                this.sinon.stub(ns.Model, 'get').withArgs('settings').returns({
                    getSetting: this.getSetting
                });

                this.sinon.stub(this.vMessagesNotificationSearch, 'canShow').returns(true);
                this.sinon.stub(this.vMessagesNotificationSearch, 'getViewShowCount').returns(10);

                this.sinon.stub(this.vMessagesNotificationSearch, '_shouldShowFeedback').value(null);

                this.sinon.stub(Daria, 'now').returns(Jane.Date.DAY);
                this.sinon.stub(Daria.Config, 'locale').value('ru');
                this.sinon.stub(Daria, 'hasExperiment').returns(true);
            });

            it('должен вернуть true, если все переменные удовлетворяют условию', function() {
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(true);
            });

            it('должен вернуть false, если  #canShow вернул false', function() {
                this.vMessagesNotificationSearch.canShow.returns(false);
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
            });

            it('должен вернуть false, если нет эксперимента', function() {
                Daria.hasExperiment.returns(false);
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
            });

            it('должен вернуть false, если локаль не ru', function() {
                this.sinon.stub(Daria.Config, 'locale').value('en');
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
            });

            it('должен вернуть false, если кол-во закрытий >= 3', function() {
                this.getSetting.withArgs('feedback_search_close_count').returns(3);
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
            });

            it('должен вернуть false, если прошло меньше 1 дня после последнего показа', function() {
                this.getSetting.withArgs('feedback_search_show_date').returns(1000);
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
            });

            it('должен вернуть false, если это не очередное 10 использование поиска', function() {
                this.vMessagesNotificationSearch.getViewShowCount.returns(1);
                expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
            });

            it('при повторном вызове метода должен вернуть значение из this._shouldShowFeedback', function() {
                const result1 = this.vMessagesNotificationSearch.shouldShowFeedback();
                const result2 = this.vMessagesNotificationSearch.shouldShowFeedback();

                expect(result1).to.equal(true);
                expect(result2).to.equal(true);
                expect(this.vMessagesNotificationSearch.canShow).have.callCount(1);
                expect(this.vMessagesNotificationSearch.getViewShowCount).have.callCount(1);
                expect(Daria.hasExperiment).have.callCount(1);
            });
        });

        it('если this._shouldShowFeedback !== null, то должен вернуть значение this._shouldShowFeedback', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_shouldShowFeedback').value(false);

            expect(this.vMessagesNotificationSearch.shouldShowFeedback()).to.equal(false);
        });
    });

    describe('#markFeedbackShow', function() {
        beforeEach(function() {
            this.setSettings = this.sinon.stub();

            this.sinon.stub(this.vMessagesNotificationSearch, 'sendMetric');
            this.sinon.stub(ns.Model, 'get').returns({
                setSettings: this.setSettings
            });

            this.sinon.stub(Daria, 'now').returns(123);
        });

        it('если this._hasFeedbackShown !== true, то должен послать метрику и отметить в настройке', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_hasFeedbackShown').value(false);

            this.vMessagesNotificationSearch.markFeedbackShow();

            expect(this.vMessagesNotificationSearch.sendMetric).have.callCount(1);
            expect(this.vMessagesNotificationSearch.sendMetric).to.have.been.calledWith('Show');
            expect(this.setSettings).have.callCount(1);
            expect(this.setSettings).to.have.been.calledWith({ feedback_search_show_date: 123 });
        });

        it('если this._hasFeedbackShown === true, то не должен посылать метрику и менять настройки', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_hasFeedbackShown').value(true);

            this.vMessagesNotificationSearch.markFeedbackShow();

            expect(this.vMessagesNotificationSearch.sendMetric).have.callCount(0);
            expect(this.setSettings).have.callCount(0);
        });
    });

    describe('#onSearchPageClose', function() {
        beforeEach(function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_resetFeedbackFlags');
        });

        describe('не должен вызывать #_resetFeedbackFlags', function() {
            it('если предыдущая и следующая страницы - одна и та же страница', function() {
                this.vMessagesNotificationSearch.onSearchPageClose(
                    'event',
                    [
                        { page: 'messages', params: { search: true, request: 'test' } },
                        { page: 'messages', params: { search: true, request: 'test' } }
                    ]
                );

                expect(this.vMessagesNotificationSearch._resetFeedbackFlags).have.callCount(0);
            });
        });

        describe('должен вызвать #_resetFeedbackFlags', function() {
            it('если предыдущая страница - поиск, а следующая - нет', function() {
                this.vMessagesNotificationSearch.onSearchPageClose(
                    'event',
                    [
                        { page: 'messages', params: { search: true, request: 'test' } },
                        { page: 'messages', params: { request: 'test' } }
                    ]
                );

                expect(this.vMessagesNotificationSearch._resetFeedbackFlags).have.callCount(1);
            });

            it('если request у страниц не совпадает', function() {
                this.vMessagesNotificationSearch.onSearchPageClose(
                    'event',
                    [
                        { page: 'messages', params: { search: true, request: 'test1' } },
                        { page: 'messages', params: { search: true, request: 'test2' } }
                    ]
                );

                expect(this.vMessagesNotificationSearch._resetFeedbackFlags).have.callCount(1);
            });

            it('если layout страниц не совпадает', function() {
                this.vMessagesNotificationSearch.onSearchPageClose(
                    'event',
                    [
                        { page: 'messages', params: { search: true, request: 'test' } },
                        { page: 'not-messages', params: { search: true, request: 'test' } }
                    ]
                );

                expect(this.vMessagesNotificationSearch._resetFeedbackFlags).have.callCount(1);
            });
        });
    });

    describe('#getViewShowCount', function() {
        beforeEach(function() {
            this.getSetting = this.sinon.stub().withArgs('feedback_search_count').returns(8);

            this.sinon.stub(ns.Model, 'get').returns({
                getSetting: this.getSetting
            });
        });

        it('должен вернуть правильное значение, если this._hasViewShown !== true', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_hasViewShown').value(false);

            expect(this.vMessagesNotificationSearch.getViewShowCount()).to.equal(9);

            expect(this.getSetting).have.callCount(1);
            expect(this.getSetting).to.have.been.calledWith('feedback_search_count');
        });

        it('должен вернуть правильное значение, если this._hasViewShown === true', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_hasViewShown').value(true);

            expect(this.vMessagesNotificationSearch.getViewShowCount()).to.equal(8);

            expect(this.getSetting).have.callCount(1);
            expect(this.getSetting).to.have.been.calledWith('feedback_search_count');
        });
    });

    describe('#setViewShowCount', function() {
        beforeEach(function() {
            this.setSettings = this.sinon.stub();

            this.sinon.stub(ns.Model, 'get').returns({
                setSettings: this.setSettings
            });

            this.sinon.stub(this.vMessagesNotificationSearch, 'getViewShowCount').returns(9);
        });

        it('если this._hasViewShown !== true, должен проставить настройку и _hasViewShown', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_hasViewShown').value(false);

            this.vMessagesNotificationSearch.setViewShowCount();

            expect(this.setSettings).have.callCount(1);
            expect(this.setSettings).to.have.been.calledWith({ feedback_search_count: 9 });
        });

        it('если this._hasViewShown !== true, не должен ничего делать', function() {
            this.sinon.stub(this.vMessagesNotificationSearch, '_hasViewShown').value(true);

            this.vMessagesNotificationSearch.setViewShowCount();

            expect(this.setSettings).have.callCount(0);
        });
    });

    describe('#onResetFolderFilterClick', function() {
        it('должен вызвать Daria.React.startSearch с нужными параметрами', function() {
            this.vMessagesNotificationSearch.onResetFolderFilterClick();
            expect(Daria.React.startSearch).to.be.calledWith(
                this.requestString,
                {},
                true
            );
        });
    });

    describe('#onSearchRequestClick', function() {
        beforeEach(function() {
            this.sinon.stub(ns.events, 'trigger');
            var event = {
                currentTarget: $('<div>').data('request', this.requestString)
            };
            this.vMessagesNotificationSearch.onSearchRequestClick(event);
        });

        it('должен вызвать Daria.React.startSearch с нужными параметрами', function() {
            expect(Daria.React.startSearch).to.be.calledWith(
                this.requestString,
                {
                    extraSearchParams: { force: true }
                },
                true
            );
        });

        it('должен вызвать событие data:search:autocomplete-search-started', function() {
            expect(ns.events.trigger).to.be.calledWith(
                'daria:search:autocomplete-search-started',
                { request: this.requestString, force: true }
            );
        });
    });

    describe('#_onFeedbackClose', function() {
        beforeEach(function() {
            this.getSetting = this.sinon.stub();
            this.setSettings = this.sinon.stub();

            this.sinon.stub(ns.Model, 'get').returns({
                getSetting: this.getSetting,
                setSettings: this.setSettings
            });

            this.nodeRemove = this.sinon.stub();
            this.nodeFind = this.sinon.stub().returns({ remove: this.nodeRemove });

            this.sinon.stub(this.vMessagesNotificationSearch, 'sendMetric');
            this.sinon.stub(this.vMessagesNotificationSearch, '$node').value({
                find: this.nodeFind
            });
            this.sinon.stub(this.vMessagesNotificationSearch, '_shouldShowFeedback').value(true);
        });

        it('должен проставить настройку, метрику и удалить элемент, а так же выставить флаг открытия в false',
            function() {
                this.vMessagesNotificationSearch.onFeedbackClose();

                expect(this.getSetting).have.callCount(1);
                expect(this.getSetting).to.have.been.calledWith('feedback_search_close_count');
                expect(this.setSettings).have.callCount(1);
                expect(this.setSettings).to.have.been.calledWith({ feedback_search_close_count: 1 });
                expect(this.vMessagesNotificationSearch.sendMetric).have.callCount(1);
                expect(this.vMessagesNotificationSearch.sendMetric).to.have.been.calledWith('Click', 'Close');
                expect(this.nodeFind).have.callCount(1);
                expect(this.nodeFind).to.have.been.calledWith('.js-search-feedback');
                expect(this.nodeRemove).have.callCount(1);
                expect(this.vMessagesNotificationSearch._shouldShowFeedback).to.equal(false);
            }
        );
    });

    describe('#_resetFeedbackFlags', function() {
        it('должен сбросить все флаги', function() {
            this.vMessagesNotificationSearch._shouldShowFeedback = true;
            this.vMessagesNotificationSearch._hasFeedbackShown = true;
            this.vMessagesNotificationSearch._hasViewShown = true;

            this.vMessagesNotificationSearch._resetFeedbackFlags();

            expect(this.vMessagesNotificationSearch._shouldShowFeedback).to.equal(null);
            expect(this.vMessagesNotificationSearch._hasFeedbackShown).to.equal(false);
            expect(this.vMessagesNotificationSearch._hasViewShown).to.equal(false);
        });
    });
});
