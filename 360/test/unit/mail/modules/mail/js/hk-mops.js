describe('Daria.hkMOPS', function() {

    describe('#doAction', function() {

        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');

            this.sinon.stub(this.mMessagesChecked, 'resetChecked');
            this.sinon.stub(Daria.MOPS, 'updateHelper');
        });

        it('Должен вызвать MOPS с переданными параметрами', function() {
            Daria.MOPS.updateHelper.returns(vow.resolve());

            Daria.hkMOPS.doAction('label', this.mMessagesChecked);

            expect(Daria.MOPS.updateHelper).to.be.calledWith('label', this.mMessagesChecked);
        });
    });

    describe('#doUserDefinedAction', function() {

        beforeEach(function() {
            // Стаб нужен для mFocus
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([[], [], []]);

            this.mFocus = ns.Model.get('focus');
            this.sinon.stub(this.mFocus, 'isCurrentFocusOnMessage');
            this.sinon.stub(this.mFocus, 'getFocus');

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.sinon.stub(this.mMessagesChecked, 'getCount');

            this.sinon.stub(Daria.hkMOPS, 'doAction');
            this.sinon.stub(Daria.MOPS, 'doActionWithMessages');

            this._nsPageCurrent = ns.page.current;
        });

        afterEach(function() {
            ns.page.current = this._nsPageCurrent;
        });

        it('Должен вызвать MOPS с заданными параметрами для выделенных писем, если они есть', function() {
            this.mMessagesChecked.getCount.returns(1);
            var params = {'lid': '1234567'};
            Daria.hkMOPS.doUserDefinedAction('label', params);

            expect(Daria.hkMOPS.doAction).to.be.calledWith('label', this.mMessagesChecked, params);
        });

        it('Должен вызвать MOPS с заданными параметрами для письма в фокусе, если оно есть', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
               params: {
                   ids: '123'
               }
            });

            var params = {'lid': '1234567'};
            Daria.hkMOPS.doUserDefinedAction('label', params);

            expect(Daria.MOPS.doActionWithMessages).to.be.calledWith([ '123' ], 'label', params);
        });

        it('Должен вызвать MOPS с заданными параметрами только для выделенных писем, если есть выделенные и в фокусе', function() {
            this.mMessagesChecked.getCount.returns(1);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
                params: {
                    ids: '123'
                }
            });

            var params = {'lid': '1234567'};
            Daria.hkMOPS.doUserDefinedAction('label', params);

            expect(Daria.hkMOPS.doAction).to.be.calledWith('label', this.mMessagesChecked, params);
            expect(Daria.MOPS.doActionWithMessages).to.have.callCount(0);
        });

        it('Должен вызвать MOPS с заданными параметрами для текущего сообщения, если открыта страница сообщения', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(false);
            ns.page.current.page = 'message';
            ns.page.current.params = {ids: '123'};

            var params = {'lid': '1234567'};
            Daria.hkMOPS.doUserDefinedAction('label', params);

            expect(Daria.hkMOPS.doAction).to.have.callCount(0);
            expect(Daria.MOPS.doActionWithMessages).to.be.calledWith([ '123' ], 'label', params);
        });

        it('Не должен вызывать MOPS, если нет выделенных писем и письма с фокусом и текущая страница - не страница сообщения', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(false);
            ns.page.current.page = 'inbox';

            var params = {'lid': '1234567'};
            Daria.hkMOPS.doUserDefinedAction('label', params);

            expect(Daria.MOPS.doActionWithMessages).to.have.callCount(0);
            expect(Daria.hkMOPS.doAction).to.have.callCount(0);
        });
    });

    describe('#runReplyAction', function() {

        beforeEach(function() {
            // Стаб нужен для mFocus
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([[], [], []]);

            this.mFocus = ns.Model.get('focus');
            this.sinon.stub(this.mFocus, 'isCurrentFocusOnMessage');
            this.sinon.stub(this.mFocus, 'getFocus');

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.sinon.stub(this.mMessagesChecked, 'getCount');

            this.mMessagesCheckedFake = ns.Model.get('messages-checked', {mid: 'mops-fake-checked'});
            this.sinon.stub(this.mMessagesCheckedFake, 'destroy');

            this.sinon.stub(Daria.MOPS, 'getFakeMessagesChecked').returns(this.mMessagesCheckedFake);

            this.sinon.stub(ns.action, 'run');

            this._nsPageCurrent = ns.page.current;
        });

        afterEach(function() {
            ns.page.current = this._nsPageCurrent;
        });

        it('Должен вызвать экшн с заданными параметрами для выделенных писем, если они есть', function() {
            this.mMessagesChecked.getCount.returns(1);
            var params = {
                email: 'noreply@test.test'
            };
            Daria.hkMOPS.runReplyAction('sendon', params);
            var actionParams = {
                toolbar: true,
                mMessagesChecked: this.mMessagesChecked,
                email: 'noreply@test.test'
            };
            expect(ns.action.run).to.be.calledWith('sendon', actionParams);
        });

        it('Должен вызвать экшн с заданными параметрами и toolbar=true для письма в фокусе, если оно есть', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
               params: {
                   ids: '123'
               }
            });

            var params = {
                email: 'noreply@test.test'
            };
            Daria.hkMOPS.runReplyAction('sendon', params);

            var actionParams = {
                toolbar: true,
                'message-id': '123',
                email: 'noreply@test.test'
            };
            expect(ns.action.run).to.be.calledWith('sendon', actionParams);
        });

        it('Должен использовать messages-checked-fake, если под фокусом тред', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
               params: {
                   ids: 't123'
               }
            });

            var params = {
                email: 'noreply@test.test'
            };
            Daria.hkMOPS.runReplyAction('sendon', params);

            var actionParams = {
                toolbar: true,
                email: 'noreply@test.test',
                mMessagesChecked: this.mMessagesCheckedFake
            };
            expect(ns.action.run).to.be.calledWith('sendon', actionParams);
            expect(this.mMessagesCheckedFake.destroy).to.have.callCount(1);
        });

        it('Должен вызвать экшн с заданными параметрами только для выделенных писем, если есть выделенные и в фокусе', function() {
            this.mMessagesChecked.getCount.returns(1);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
                params: {
                    ids: '123'
                }
            });

            var params = {
                email: 'noreply@test.test'
            };
            Daria.hkMOPS.runReplyAction('sendon', params);

            var actionParams = {
                toolbar: true,
                mMessagesChecked: this.mMessagesChecked,
                email: 'noreply@test.test'
            };
            expect(ns.action.run).to.be.calledWith('sendon', actionParams);
        });

        it('Должен вызвать экшн с заданными параметрами и toolbar=true для текущего сообщения, если открыта страница сообщения', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(false);
            ns.page.current.page = 'message';
            ns.page.current.params = {ids: '123'};

            var params = {
                email: 'noreply@test.test'
            };
            Daria.hkMOPS.runReplyAction('sendon', params);

            var actionParams = {
                toolbar: true,
                'message-id': '123',
                email: 'noreply@test.test'
            };
            expect(ns.action.run).to.be.calledWith('sendon', actionParams);
        });

        it('Не должен вызывать экшн, если нет выделенных писем и письма с фокусом и текущая страница - не страница сообщения', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(false);
            ns.page.current.page = 'inbox';

            var params = {
                email: 'noreply@test.test'
            };
            Daria.hkMOPS.runReplyAction('sendon', params);

            expect(ns.action.run).to.have.callCount(0);
        });
    });

    describe('#toggleImportance', function() {

        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');

            this.sinon.stub(this.mMessagesChecked, 'getCount');
            this.sinon.stub(Daria.MOPS, 'updateHelper');
            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(ns.router, 'generateUrl');

        });

        it('Должен вызвать MOPS, который поставит/снимет метку `важное`, если есть выделенные письма', function() {
            this.mMessagesChecked.getCount.returns(1);

            Daria.hkMOPS.toggleImportance();

            expect(Daria.MOPS.updateHelper).to.have.callCount(1);
        });

        it('Должен перейти на страницу с важными письмами, если нет выделенных писем', function() {
            this.mMessagesChecked.getCount.returns(0);

            Daria.hkMOPS.toggleImportance();

            expect(ns.page.go).to.have.callCount(1);
        });

    });

    describe('#mark', function() {

        beforeEach(function() {
            // Стаб нужен для mFocus
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([[], [], []]);

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mFocus = ns.Model.get('focus');

            this.sinon.stub(this.mMessagesChecked, 'getCount');
            this.sinon.stub(this.mFocus, 'isCurrentFocusOnMessage');
            this.sinon.stub(this.mFocus, 'getFocus');
            this.sinon.stub(Daria.hkMOPS, 'doAction');
            this.sinon.stub(Daria.MOPS, 'doActionWithMessages');
        });

        it('Должен вызвать MOPS для выделенных писем, если они есть', function() {
            this.mMessagesChecked.getCount.returns(1);

            Daria.hkMOPS.mark();

            expect(Daria.hkMOPS.doAction).to.be.calledWith('mark', this.mMessagesChecked);
        });

        it('Должен вызвать MOPS для письма в фокусе, если оно есть', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
               params: {
                   ids: '123'
               }
            });

            Daria.hkMOPS.mark();

            expect(Daria.MOPS.doActionWithMessages).to.be.calledWith([ '123' ], 'mark');
        });

        it('Должен вызвать MOPS только для выделенных писем, если есть выделенные и в фокусе', function() {
            this.mMessagesChecked.getCount.returns(1);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
                params: {
                    ids: '123'
                }
            });

            Daria.hkMOPS.mark();

            expect(Daria.hkMOPS.doAction).to.be.calledWith('mark', this.mMessagesChecked);
            expect(Daria.MOPS.doActionWithMessages).to.have.callCount(0);
        });

        it('Не должен вызывать MOPS, если нет выделенных писем и письма с фокусом', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(false);

            Daria.hkMOPS.mark();

            expect(Daria.MOPS.doActionWithMessages).to.have.callCount(0);
            expect(Daria.hkMOPS.doAction).to.have.callCount(0);
        });

    });

    describe('#unmark', function() {

        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');

            this.sinon.stub(this.mMessagesChecked, 'getCount');
            this.sinon.stub(Daria.hkMOPS, 'doAction');
            this.sinon.stub(ns.page, 'go');
            this.recalculateMessagesStub = this.sinon.stub(ns.events, 'trigger').withArgs('daria:vMessages:recalculate');

            this.pageParams = _.clone(ns.page.current.params);
        });

        afterEach(function() {
            ns.page.current.params = _.clone(this.pageParams);
            delete this.pageParams;
        });

        it('Должен вызвать MOPS, чтобы сделать письма непрочитанными, если есть выделенные письма', function() {
            this.mMessagesChecked.getCount.returns(1);

            Daria.hkMOPS.unmark();

            expect(Daria.hkMOPS.doAction).to.be.calledWith('unmark', this.mMessagesChecked);
        });

        it('Должен перейти на страницу с непрочитанными письмами, если нет выделенных писем', function() {
            this.mMessagesChecked.getCount.returns(0);

            Daria.hkMOPS.unmark();

            expect(ns.page.go).to.be.calledWith('#unread');
        });

        it('Должен обновить список писем, если нет выделенных писем и текущая страница - список только непрочитанных писем', function() {
            this.mMessagesChecked.getCount.returns(0);
            ns.page.current.params['extra_cond'] = 'only_new';

            Daria.hkMOPS.unmark();

            expect(this.recalculateMessagesStub).to.have.callCount(1);
        });

    });

    describe('#remove', function() {

        beforeEach(function() {
            // Стаб нужен для mFocus
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([[], [], []]);

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mFocus = ns.Model.get('focus');

            this.sinon.stub(this.mMessagesChecked, 'getCount');
            this.sinon.stub(this.mFocus, 'isCurrentFocusOnMessage');
            this.sinon.stub(this.mFocus, 'getFocus');
            this.sinon.stub(Daria.hkMOPS, 'doAction');
            this.sinon.stub(Daria.MOPS, 'doActionWithMessages');
        });

        it('Должен вызвать MOPS для выделенных писем, если они есть', function() {
            this.mMessagesChecked.getCount.returns(1);

            Daria.hkMOPS.remove();

            expect(Daria.hkMOPS.doAction).to.be.calledWith('remove', this.mMessagesChecked);
        });

        it('Должен вызвать MOPS для письма в фокусе, если оно есть', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
                params: {
                    ids: '123'
                }
            });

            Daria.hkMOPS.remove();

            expect(Daria.MOPS.doActionWithMessages).to.be.calledWith([ '123' ], 'remove');
        });

        it('Должен вызвать MOPS только для выделенных писем, если есть выделенные и в фокусе', function() {
            this.mMessagesChecked.getCount.returns(1);
            this.mFocus.isCurrentFocusOnMessage.returns(true);
            this.mFocus.getFocus.returns({
                params: {
                    ids: '123'
                }
            });

            Daria.hkMOPS.remove();

            expect(Daria.hkMOPS.doAction).to.be.calledWith('remove', this.mMessagesChecked);
            expect(Daria.MOPS.doActionWithMessages).to.have.callCount(0);
        });

        it('Не должен вызывать MOPS, если нет выделенных писем и письма с фокусом', function() {
            this.mMessagesChecked.getCount.returns(0);
            this.mFocus.isCurrentFocusOnMessage.returns(false);

            Daria.hkMOPS.remove();

            expect(Daria.MOPS.doActionWithMessages).to.have.callCount(0);
            expect(Daria.hkMOPS.doAction).to.have.callCount(0);
        });

    });

});
