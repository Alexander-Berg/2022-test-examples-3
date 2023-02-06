describe('Daria.mMessagesChecked.', function() {
    describe('Генерация ключа.', function() {
        it('должен взять mid, если он есть в параметрах', function() {
            var params = {
                foo: 'bar',
                mid: 1
            };
            var mMessagesChecked = ns.Model.get('messages-checked', params);
            expect(mMessagesChecked.key).to.be.equal('model=messages-checked&mid=1');
        });

        it('должен удалять внутренние ("_*") параметры', function() {
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.sinon.stub(ns.page.current, 'params').value({
                _foo: 'bar',
                current_folder: '1'
            });
            var mMessagesChecked = ns.Model.get('messages-checked', {});
            expect(mMessagesChecked.key)
                .to.be.equal('model=messages-checked&current_folder=1&sort_type=date&with_pins=yes');
        });

        it('должен удалить ids из параметров', function() {
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.sinon.stub(ns.page.current, 'params').value({
                ids: '2',
                current_folder: '2'
            });
            var mMessagesChecked = ns.Model.get('messages-checked', {});
            expect(mMessagesChecked.key)
                .to.be.equal('model=messages-checked&current_folder=2&sort_type=date&with_pins=yes');
        });

        it('должен удалить thread_id из параметров, если есть current_folder', function() {
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.sinon.stub(ns.page.current, 'params').value({
                thread_id: '2',
                current_folder: '2'
            });
            var mMessagesChecked = ns.Model.get('messages-checked', {});
            expect(mMessagesChecked.key)
                .to.be.equal('model=messages-checked&current_folder=2&sort_type=date&with_pins=yes');
        });

        it('не должен удалить thread_id из параметров, если нет current_folder', function() {
            this.sinon.stub(ns.page.current, 'page').value('messages');
            this.sinon.stub(ns.page.current, 'params').value({
                thread_id: '2'
            });
            var mMessagesChecked = ns.Model.get('messages-checked', {});
            expect(mMessagesChecked.key)
                .to.be.equal('model=messages-checked&thread_id=2&sort_type=date');
        });
    });

    describe('#runOps', function() {
        it('должен запустить MOPS со своим инстансом', function() {
            this.sinon.stub(Daria.MOPS, 'mark');

            var params = {};
            var mMessagesChecked = ns.Model.get('messages-checked');
            mMessagesChecked.runOps('mark', params);

            expect(Daria.MOPS.mark).to.be.calledWithExactly(mMessagesChecked, params);
        });
    });

    describe('.getPinnedCount', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                foo: 'bar'
            });

            this.count = 10;
            this.expectedCount = this.count / 2;
            this.mMessage = [];

            _.range(this.count).forEach(function(i) {
                this.mMessage[i] = ns.Model.get('message', { ids: String(i) }).setData({ mid: String(i) });
                this.mMessagesChecked.check(this.mMessage[i], true);
                this.sinon.stub(this.mMessage[i], 'isPinned').returns((i % 2 === 0));
            }, this);
        });

        it('Должен вернуть количество запиненных писем', function() {
            expect(this.mMessagesChecked.getPinnedCount()).to.be.equal(this.expectedCount);
        });
    });

    describe('._checkAllFromCollection', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                foo: 'bar'
            });
            this.mMessagesChecked._resetChecked();

            this.mMessagesChecked._mMessages = ns.Model.get('messages', { foo: 'bar' });

            this.mMessageChecked_1 = ns.Model.get('message', { ids: '1' }).setData({ mid: '1' });
            this.mMessageChecked_2 = ns.Model.get('message', { ids: '2' }).setData({ mid: '2' });
            this.mMessageChecked_3 = ns.Model.get('message', { ids: '3' }).setData({ mid: '3' });
            this.mMessageChecked_4 = ns.Model.get('message', { ids: '4' }).setData({ mid: '4' });
            this.mMessageChecked_5 = ns.Model.get('message', { ids: 't5' }).setData({ mid: 't5' });
            this.mMessageChecked_6 = ns.Model.get('message', { ids: '6' }).setData({ mid: '6', tid: 't5' });

            this.mMessagesChecked._mMessages.setData({ message: [] });
            this.mMessagesChecked._mMessages.insert([
                this.mMessageChecked_1,
                this.mMessageChecked_2,
                this.mMessageChecked_3,
                this.mMessageChecked_4
            ]);

            this.isPinnedStubMessage_1 = this.sinon.stub(this.mMessageChecked_1, 'isPinned');
            this.isPinnedStubMessage_2 = this.sinon.stub(this.mMessageChecked_2, 'isPinned');
            this.isPinnedStubMessage_3 = this.sinon.stub(this.mMessageChecked_3, 'isPinned');
            this.isPinnedStubMessage_4 = this.sinon.stub(this.mMessageChecked_4, 'isPinned');
            this.isPinnedStubMessage_5 = this.sinon.stub(this.mMessageChecked_5, 'isPinned');
            this.isPinnedStubMessage_6 = this.sinon.stub(this.mMessageChecked_6, 'isPinned');

            this.mMessageInLastCheckedMessage = ns.Model.get('message', { ids: '7' }).setData({ mid: '7' });

            this.isPinnedStub = this.sinon.stub(this.mMessageInLastCheckedMessage, 'isPinned');
            this.isThreadStub = this.sinon.stub(this.mMessageInLastCheckedMessage, 'isIntoThread');

            // Письмо в фокусе
            var mMessageInFocus = ns.Model.get('message', { ids: '111333' }).setData({ mid: '111333' });
            this.sinon.stub(mMessageInFocus, 'isPinned');
            this.sinon.stub(mMessageInFocus, 'isIntoThread');

            // Данные для mFocus
            this.sinon.stub(Daria.nsTreeWalker, 'getTreeForFocus').returns([ [], [], [] ]);

            this.mFocus = ns.Model.get('focus').setData({
                currentFocus: {
                    getModel: function() {
                        return mMessageInFocus;
                    }
                }
            });

            this.sinon.stub(this.mFocus, 'isCurrentFocusOnMessage').returns(false);
            this.sinon.spy(this.mFocus, 'getFocus');
        });

        describe('Главный тулбар, кнопка "Выделить все"', function() {
            beforeEach(function() {
                this.isThreadStub.returns(false);
                this.isPinnedStub.returns(true);

                this.isPinnedStubMessage_1.returns(true);
                this.isPinnedStubMessage_2.returns(true);
                this.isPinnedStubMessage_3.returns(false);
                this.isPinnedStubMessage_4.returns(false);

                this.mMessagesChecked._selectedFromToolbar = true;
            });

            it('Должен выбрать только незапиненные письма', function() {
                this.mMessagesChecked._checkAllFromCollection();
                expect(this.mMessagesChecked.getIds().ids).to.be.eql([ '3', '4' ]);
            });
        });

        describe('Тред ->', function() {
            describe('Обычное письмо в запиненном треде ->', function() {
                beforeEach(function() {
                    this.isThreadStub.returns(true);
                    this.isPinnedStub.returns(false);

                    this.sinon.stub(this.mMessageInLastCheckedMessage, 'getThreadId').returns('t5');

                    this.isPinnedStubMessage_1.returns(true);
                    this.isPinnedStubMessage_2.returns(true);
                    this.isPinnedStubMessage_3.returns(false);
                    this.isPinnedStubMessage_4.returns(false);
                    this.isPinnedStubMessage_5.returns(true);
                    this.isPinnedStubMessage_6.returns(false);

                    this.mMessagesChecked._mMessages.insert([
                        this.mMessageChecked_5,
                        this.mMessageChecked_6
                    ]);
                });

                it('Должен выделить все незапиненные письма.', function() {
                    this.mMessagesChecked._checkAllFromCollection();
                    expect(this.mMessagesChecked.getIds().ids).to.be.eql([ '3', '4', '6' ]);
                });
            });
        });

        it('Должен выделить все запиненные письма, если нет незапиненных', function() {
            this.mMessagesChecked._mMessages.setData({ message: [] });
            this.mMessagesChecked._mMessages.insert([
                this.mMessageChecked_1,
                this.mMessageChecked_2
            ]);

            this.isPinnedStubMessage_1.returns(true);
            this.isPinnedStubMessage_2.returns(true);

            this.mMessagesChecked._checkAllFromCollection();
            expect(this.mMessagesChecked.getIds().ids).to.be.eql([]);
        });
    });

    describe('.areAllCheckedPinned', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                foo: 'bar'
            });
            this.mMessagesChecked._mMessages = ns.Model.get('messages', { foo: 'bar' });
            this.mMessagesChecked._resetChecked();

            this.mMessage_1 = ns.Model.get('message', { ids: '1' }).setData({ mid: '1' });
            this.mMessage_2 = ns.Model.get('message', { ids: '2' }).setData({ mid: '2' });
            this.mMessage_3 = ns.Model.get('message', { ids: '3' }).setData({ mid: '3' });

            this.stubIsPinnedMessage_1 = this.sinon.stub(this.mMessage_1, 'isPinned');
            this.stubIsPinnedMessage_2 = this.sinon.stub(this.mMessage_2, 'isPinned');
            this.stubIsPinnedMessage_3 = this.sinon.stub(this.mMessage_3, 'isPinned');

            this.mMessagesChecked._mMessages.setData({ message: [] });
            this.mMessagesChecked._mMessages.insert([
                this.mMessage_1,
                this.mMessage_2,
                this.mMessage_3
            ]);
        });

        it('Должен вернуть false, если выделены не все запиненные', function() {
            this.mMessagesChecked.check(this.mMessage_1, true);

            this.stubIsPinnedMessage_1.returns(true);
            this.stubIsPinnedMessage_2.returns(true);
            this.stubIsPinnedMessage_3.returns(true);

            expect(this.mMessagesChecked.areAllCheckedPinned()).to.be.equal(false);
        });
    });

    describe('.areAllCheckedNotPinned', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                foo: 'bar'
            });
            this.mMessagesChecked._mMessages = ns.Model.get('messages', { foo: 'bar' });
            this.mMessagesChecked._resetChecked();

            this.mMessage_1 = ns.Model.get('message', { ids: '1' }).setData({ mid: '1' });
            this.mMessage_2 = ns.Model.get('message', { ids: '2' }).setData({ mid: '2' });
            this.mMessage_3 = ns.Model.get('message', { ids: '3' }).setData({ mid: '3' });

            this.stubIsPinnedMessage_1 = this.sinon.stub(this.mMessage_1, 'isPinned');
            this.stubIsPinnedMessage_2 = this.sinon.stub(this.mMessage_2, 'isPinned');
            this.stubIsPinnedMessage_3 = this.sinon.stub(this.mMessage_3, 'isPinned');

            this.mMessagesChecked._mMessages.setData({ message: [] });
            this.mMessagesChecked._mMessages.insert([
                this.mMessage_1,
                this.mMessage_2,
                this.mMessage_3
            ]);
        });

        it('Должен вернуть true, если выделены все незапиненные', function() {
            this.mMessagesChecked.check(this.mMessage_3, true);

            this.stubIsPinnedMessage_1.returns(true);
            this.stubIsPinnedMessage_2.returns(true);
            this.stubIsPinnedMessage_3.returns(false);

            expect(this.mMessagesChecked.areAllCheckedNotPinned()).to.be.equal(true);
        });

        it('Должен вернуть false, если выделены не все письма', function() {
            this.mMessagesChecked.check(this.mMessage_1, true);

            this.stubIsPinnedMessage_1.returns(false);
            this.stubIsPinnedMessage_2.returns(false);
            this.stubIsPinnedMessage_3.returns(false);

            expect(this.mMessagesChecked.areAllCheckedNotPinned()).to.be.equal(false);
        });
    });

    describe('.hasPinnedCheckedMessage', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                foo: 'bar'
            });
            this.mMessagesChecked._mMessages = ns.Model.get('messages', { foo: 'bar' });
            this.mMessagesChecked._resetChecked();

            this.mMessage_1 = ns.Model.get('message', { ids: '1' }).setData({ mid: '1' });
            this.mMessage_2 = ns.Model.get('message', { ids: '2' }).setData({ mid: '2' });
            this.mMessage_3 = ns.Model.get('message', { ids: '3' }).setData({ mid: '3' });

            this.stubIsPinnedMessage_1 = this.sinon.stub(this.mMessage_1, 'isPinned');
            this.stubIsPinnedMessage_2 = this.sinon.stub(this.mMessage_2, 'isPinned');
            this.stubIsPinnedMessage_3 = this.sinon.stub(this.mMessage_3, 'isPinned');

            this.mMessagesChecked._mMessages.setData({ message: [] });
            this.mMessagesChecked._mMessages.insert([
                this.mMessage_1,
                this.mMessage_2,
                this.mMessage_3
            ]);
        });

        it('Должен вернуть true, если среди выделенных есть запиненное', function() {
            this.mMessagesChecked.check(this.mMessage_1, true);
            this.mMessagesChecked.check(this.mMessage_3, true);

            this.stubIsPinnedMessage_1.returns(true);
            this.stubIsPinnedMessage_2.returns(true);
            this.stubIsPinnedMessage_3.returns(false);

            expect(this.mMessagesChecked.hasPinnedCheckedMessage()).to.be.equal(true);
        });

        it('Должен вернуть false, если среди выделенных нет запиненных', function() {
            this.mMessagesChecked.check(this.mMessage_1, true);

            this.stubIsPinnedMessage_1.returns(false);
            this.stubIsPinnedMessage_2.returns(false);
            this.stubIsPinnedMessage_3.returns(false);

            expect(this.mMessagesChecked.hasPinnedCheckedMessage()).to.be.equal(false);
        });
    });

    describe('#shouldSelectAll', function() {
        beforeEach(function() {
            this.model = ns.Model.get('messages-checked', {});

            this.sinon.stubMethods(this.model, [
                'isListSelected',
                'areAllCheckedNotPinned',
                'areAllCheckedPinned',
                'isFolderSelected'
            ]);
        });

        it('Должен вернуть false, если выбраны все письма', function() {
            this.model.isListSelected.returns(true);

            expect(this.model.shouldSelectAll()).to.be.equal(false);
        });

        it('Должен вернуть false, если выбраны все письма в папке', function() {
            this.model.isListSelected.returns(false);
            this.model.isFolderSelected.returns(true);

            expect(this.model.shouldSelectAll()).to.be.equal(false);
        });

        it('Должен вернуть true, если не выбраны письма и нет выделения всей папки', function() {
            this.model.isListSelected.returns(false);
            this.model.isFolderSelected.returns(false);

            expect(this.model.shouldSelectAll()).to.be.equal(true);
        });
    });

    describe('#toggleSelectAll', function() {
        beforeEach(function() {
            this.model = ns.Model.get('messages-checked', {});

            this.sinon.stubMethods(this.model, [
                'selectList',
                'shouldSelectAll',
                'resetChecked'
            ]);
        });

        it('должен выделить все письма, если можно', function() {
            this.model.shouldSelectAll.returns(true);
            this.model.toggleSelectAll();

            expect(this.model.selectList).to.have.callCount(1);
        });

        it('должен снять выделение со всех писем, если выделены все', function() {
            this.model.shouldSelectAll.returns(false);
            this.model.toggleSelectAll();

            expect(this.model.resetChecked).to.have.callCount(1);
        });
    });

    describe('#getLastChecked', function() {
        beforeEach(function() {
            this.mMessage1 = ns.Model.get('message', { ids: '112233' });
            this.mMessage2 = ns.Model.get('message', { ids: '112244' });
            this.mMessage3 = ns.Model.get('message', { ids: '112255' });

            this.mMessagesChecked = ns.Model.get('messages-checked');

            this.mMessages = ns.Model.get('messages');

            this.mMessages.insert([
                this.mMessage1,
                this.mMessage2,
                this.mMessage3
            ]);

            this.mMessagesChecked._mMessages = this.mMessages;
        });

        describe('страница с обычным списком писем ->', function() {
            beforeEach(function() {
                this.mMessagesChecked._lastCheckedMessage = this.mMessage3;
            });

            describe('2pane -> ', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'is2pane').returns(true);
                });

                it('Должен вернуть последнее чекнутое письмо, если оно есть', function() {
                    expect(this.mMessagesChecked.getLastChecked()).to.be.equal(this.mMessage3);
                });

                it('Должен вернуть первое письмо, если нет последнего чекнутого', function() {
                    this.mMessagesChecked._lastCheckedMessage = null;

                    expect(this.mMessagesChecked.getLastChecked()).to.be.equal(this.mMessage1);
                });
            });

            describe('3pane -> ', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'is2pane').returns(false);
                    this.sinon.stub(this.mMessagesChecked._mMessages, 'getMessageByMid');

                    this._nsPageCurrent = ns.page.current;
                });

                afterEach(function() {
                    ns.page.current = this._nsPageCurrent;
                });

                it('Должен вернуть последнее чекнутое письмо, если оно есть', function() {
                    expect(this.mMessagesChecked.getLastChecked()).to.be.equal(this.mMessage3);
                });

                it('Должен запросить текущее письмо, используя параметр страницы `thread_id`', function() {
                    this.mMessagesChecked._lastCheckedMessage = null;
                    ns.page.current.params = { thread_id: '112244' };

                    this.mMessagesChecked.getLastChecked();

                    expect(this.mMessagesChecked._mMessages.getMessageByMid).to.be.calledWith('112244');
                });

                it(
                    'Должен запросить текущее письмо, используя параметр страницы `ids`, если нет `thread_id`',
                    function() {
                        this.mMessagesChecked._lastCheckedMessage = null;
                        ns.page.current.params = { ids: '111' };

                        this.mMessagesChecked.getLastChecked();

                        expect(this.mMessagesChecked._mMessages.getMessageByMid).to.be.calledWith('111');
                    }
                );

                it('Должен вернуть текущее письмо, если нет последнего чекнутого', function() {
                    this.mMessagesChecked._lastCheckedMessage = null;
                    this.mMessagesChecked._mMessages.getMessageByMid.returns(this.mMessage2);
                    ns.page.current.params = { thread_id: '112244' };

                    expect(this.mMessagesChecked.getLastChecked()).to.be.equal(this.mMessage2);
                });

                it('Должен вернуть первое письмо, если нет чекнутого и текущего', function() {
                    this.mMessagesChecked._lastCheckedMessage = null;
                    this.mMessagesChecked._mMessages.getMessageByMid.returns(null);
                    ns.page.current.params = { thread_id: '112244' };

                    expect(this.mMessagesChecked.getLastChecked()).to.be.equal(this.mMessage1);
                });
            });
        });
    });

    describe('#canShowInfoline', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', { current_folder: '1' });
            this.getCheckedMessagesStub = this.sinon.stub(this.mMessagesChecked, 'getCheckedMessages');
        });

        it('Должен вернуть true, если выбрано больше минимально количества писем для показа Infoline', function() {
            this.getCheckedMessagesStub.returns([ '1', '2', '3' ]);
            expect(this.mMessagesChecked.canShowInfoline()).to.be.equal(true);
        });

        it('Должен вернуть false, если выбрано меньше минимально количества писем для показа Infoline', function() {
            this.getCheckedMessagesStub.returns([ '1' ]);
            expect(this.mMessagesChecked.canShowInfoline()).to.be.equal(false);
        });
    });

    describe('#isEmptyMessagesList', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages', { foo: 'bar' });

            this.mMessagesChecked = ns.Model.get('messages-checked', { foo: 'bar' });
            this.mMessagesChecked._resetChecked();
            this.mMessagesChecked._mMessages = this.mMessages;
        });

        describe('обычная страница со списком писем ->', function() {
            sit(true, true);
            sit(false, false);

            function sit(setupValue, expectedValue) {
                it('mMessages: ' + setupValue +
                    ' => ' + expectedValue, function() {
                    this.sinon.stub(this.mMessages, 'isEmptyList').returns(setupValue);
                    expect(this.mMessagesChecked.isEmptyMessagesList()).to.be.equal(expectedValue);
                });
            }
        });
    });

    describe('#isPinnedMessagesList', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages', { foo: 'bar' });

            this.mMessagesChecked = ns.Model.get('messages-checked', { foo: 'bar' });
            this.mMessagesChecked._resetChecked();
            this.mMessagesChecked._mMessages = this.mMessages;
        });

        describe('обычная страница со списком писем ->', function() {
            sit(true, true);
            sit(false, false);

            function sit(setupValue, expectedValue) {
                it('mMessages: ' + setupValue + ' => ' + expectedValue, function() {
                    this.sinon.stub(this.mMessages, 'hasOnlyPinnedMessages').returns(setupValue);
                    expect(this.mMessagesChecked.isPinnedMessagesList()).to.be.equal(expectedValue);
                });
            }
        });
    });

    describe('#getCheckedMessagesHash', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mMessage = ns.Model.get('message', { ids: '2' }).setData({ mid: '2' });
            this.mThreadMessage = ns.Model.get('message', { ids: 't3' }).setData({ mid: 't3', tid: 't3' });
            this.mCheckedMessage = ns.Model.get('message', { ids: '4' }).setData({ mid: '4' });
            this.pageParamsShouldProducePassedValue = pageParamsShouldProducePassedValue;
        });

        describe('3pane на корпе ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'is3pane').returns(true);
            });

            it('должен вернуть открытое письмо на странице просмотра письма ->', function() {
                this.pageParamsShouldProducePassedValue({ ids: '2' }, { 2: this.mMessage });
            });

            it('должен вернуть чекнутое письмо из списка писем на странице просмотра письма ->', function() {
                this.sinon.stub(this.mMessagesChecked, '_checked').value({ 1: this.mCheckedMessage });
                this.pageParamsShouldProducePassedValue({ ids: '2' }, { 1: this.mCheckedMessage });
            });

            it('должен вернуть открытый тред на странице просмотра треда ->', function() {
                this.pageParamsShouldProducePassedValue({ thread_id: 't3' }, { t3: this.mThreadMessage });
            });

            it('должен вернуть чекнутое письмо из списка писем на странице просмотра треда ->', function() {
                this.sinon.stub(this.mMessagesChecked, '_checked').value({ 1: this.mCheckedMessage });
                this.pageParamsShouldProducePassedValue({ thread_id: 't3' }, { 1: this.mCheckedMessage });
            });

            it('не должен возвращать письмо/тред на других страницах ->', function() {
                this.pageParamsShouldProducePassedValue({}, {});
            });
        });

        describe('не 3pane ->', shouldNotReturnOpenedMessageIf({ is3pane: false }));

        function shouldNotReturnOpenedMessageIf(params) {
            return function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'is3pane').returns(params.is3pane);
                });

                it('не должен вернуть письмо на странице просмотра письма ->', function() {
                    this.pageParamsShouldProducePassedValue({ ids: '2' }, {});
                });

                it('должен вернуть чекнутое письмо из списка писем на странице просмотра письма ->', function() {
                    this.sinon.stub(this.mMessagesChecked, '_checked').value({ 1: this.mCheckedMessage });
                    this.pageParamsShouldProducePassedValue({ ids: '2' }, { 1: this.mCheckedMessage });
                });

                it('не должен вернуть тред на странице просмотра треда ->', function() {
                    this.pageParamsShouldProducePassedValue({ thread_id: 't3' }, {});
                });

                it('должен вернуть чекнутое письмо из списка писем на странице просмотра треда ->', function() {
                    this.sinon.stub(this.mMessagesChecked, '_checked').value({ 1: this.mCheckedMessage });
                    this.pageParamsShouldProducePassedValue({ thread_id: 't3' }, { 1: this.mCheckedMessage });
                });

                it('не должен возвращать письмо/тред на других страницах ->', function() {
                    this.pageParamsShouldProducePassedValue({}, {});
                });
            };
        }

        function pageParamsShouldProducePassedValue(pageParams, expectedValue) {
            this.sinon.stub(ns.page.current, 'params').value(pageParams);
            expect(this.mMessagesChecked.getCheckedMessagesHash()).to.be.deep.equal(expectedValue);
        }
    });

    describe('#areAllCheckedInFolderOrTab', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page.current, 'params').value({ current_folder: '1' });
            this.mFolders = ns.Model.get('folders');
            this.mMessagesChecked = ns.Model.get('messages-checked', {});

            this.sinon.stub(ns.Model, 'get').withArgs('folders').returns(this.mFolders);
            this.sinon.stub(this.mFolders, 'getCount').returns(100);

            this.sinon.stubMethods(this.mMessagesChecked, [
                'isFolderSelected',
                'isTabSelected',
                'getCount'
            ]);
        });

        it('должен вернуть true если выделена вся папка', function() {
            this.mMessagesChecked.isFolderSelected.returns(true);
            this.mMessagesChecked.getCount.returns(0);

            expect(this.mMessagesChecked.areAllCheckedInFolderOrTab()).to.be.equal(true);
        });

        it('должен вернуть true если выделен весь таб', function() {
            this.mMessagesChecked.isTabSelected.returns(true);
            this.mMessagesChecked.getCount.returns(0);

            expect(this.mMessagesChecked.areAllCheckedInFolderOrTab()).to.be.equal(true);
        });

        it('должен вернуть true если выделины все письма', function() {
            this.mMessagesChecked.isFolderSelected.returns(false);
            this.mMessagesChecked.getCount.returns(100);

            expect(this.mMessagesChecked.areAllCheckedInFolderOrTab()).to.be.equal(true);
        });
    });

    describe('#toggleCheck', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '5' });
            this.mMessagesChecked = ns.Model.get('messages-checked', {});
            this.sinon.stub(this.mMessagesChecked, 'setLastChecked');
            this.sinon.stub(this.mMessagesChecked, 'check');
        });

        it('должен вызвать "setLastChecked"', function() {
            this.mMessagesChecked.toggleCheck(this.mMessage);

            expect(this.mMessagesChecked.setLastChecked).to.have.calledWith(this.mMessage);
        });

        it('должен вызвать "check"', function() {
            this.mMessagesChecked.toggleCheck(this.mMessage);

            expect(this.mMessagesChecked.check)
                .to.have.calledWith(this.mMessage, !this.mMessagesChecked.isChecked(this.mMessage));
        });
    });
});
