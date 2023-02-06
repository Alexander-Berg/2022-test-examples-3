describe('Daria.mMessagesFilters', function() {
    beforeEach(function() {
        this.mMessagesFilters = ns.Model.get('messages-filters');
    });

    describe('#_onInit ->', function() {
        it('при инициализации вызывает ns-page-after-load', function() {
            this.sinon.stub(this.mMessagesFilters, '_onLoad');
            this.mMessagesFilters._onInit();
            expect(this.mMessagesFilters._onLoad).to.have.callCount(1);
        });

        it('при инициализации до вызова _onLoad данные пустые', function() {
            this.sinon.stub(this.mMessagesFilters, '_onLoad').callsFake(function() {});
            this.mMessagesFilters._onInit();
            expect(this.mMessagesFilters.getData()).to.be.eql({});
        });
    });

    describe('#_onLoad ->', function() {
        it('находимся на странице важных писем - проставляем filter:important', function() {
            this.mMessagesFilters._onLoad({important: 'important'});
            expect(this.mMessagesFilters.get('.filter')).to.be.eql('important');
        });
        it('находимся на странице входящих писем - проставляем filter:inbox', function() {
            this.mMessagesFilters._onLoad({page: 'messages'});
            expect(this.mMessagesFilters.get('.filter')).to.be.eql('inbox');
        });
        it('находимся на странице непрочитанных писем - проставляем filter:unread', function() {
            this.mMessagesFilters._onLoad({unread: 'unread'});
            expect(this.mMessagesFilters.get('.filter')).to.be.eql('unread');
        });
        it('находимся на странице писем с вложениями - проставляем filter:attachments', function() {
            this.mMessagesFilters._onLoad({attachments: 'attachments'});
            expect(this.mMessagesFilters.get('.filter')).to.be.eql('attachments');
        });
        it('находимся на странице писем которые ждут ответа - проставляем filter:wait', function() {
            this.mLabels = ns.Model.get('labels');

            this.sinon.stub(this.mLabels, 'getWaitingForReplyLabel').returns({
                name: 'remindme_threadabout:mark',
                count: 2,
                default: true,
                lid: '2050000002369532265'
            });
            this.mMessagesFilters._onLoad({current_label: '2050000002369532265'});
            expect(this.mMessagesFilters.get('.filter')).to.be.eql('wait');
        });
        it('при переходе на новую ссылку сеттятся новые данные', function() {
            this.sinon.stub(this.mMessagesFilters, 'get').withArgs('.filter').returns('wait');

            this.sinon.spy(this.mMessagesFilters, 'set');

            this.mMessagesFilters._onLoad({attachments: 'attachments'});
            expect(this.mMessagesFilters.set).to.have.calledWith('.filter', 'attachments');
        });
    });

    describe('#_onPageBeforeLoad', function() {
        it('обновляет состояние модели перед переходом на новую страницу', function() {
            this.sinon.stub(this.mMessagesFilters, '_onLoad');
            ns.events.trigger('ns-page-before-load',
                [
                    { page: 'messages', params: { current_folder: '1', threaded: 'yes' } },
                    { page: 'messages', params: { extra_cond: 'only_new', goto: 'all', unread: 'unread' } }
                ],
                '#important'
            );

            expect(this.mMessagesFilters._onLoad).to.have.callCount(1);
            expect(this.mMessagesFilters._onLoad.getCall(0).args[0])
                .to.be.eql({ extra_cond: 'only_new', goto: 'all', unread: 'unread' });
        });
    });

});
