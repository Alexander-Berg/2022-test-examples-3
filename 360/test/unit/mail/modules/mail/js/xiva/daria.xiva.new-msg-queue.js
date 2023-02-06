describe('Daria.Xiva.NewMsgQueue', function() {

    describe('#clearMessagesCache', function() {

        beforeEach(function() {
           this.sinon.stub(Daria.messages, 'clearSearchCache');
           this.sinon.stub(ns.Model, 'invalidateAll');
        });

        it('должен очистить кеши для поиска', function() {
            Daria.Xiva.NewMsgQueue.clearMessagesCache();
            expect(Daria.messages.clearSearchCache).to.have.callCount(1);
        });

        it('должен очистить кеши для пред-след', function() {
            Daria.Xiva.NewMsgQueue.clearMessagesCache();
            expect(ns.Model.invalidateAll).to.be.calledWith(['message-nearest', 'message-thread-nearest']);
        });

    });

    describe('#shouldPreventAnyNotifications', function() {

        beforeEach(function() {
            this.mMessage = ns.Model.get('message', {ids: 'newmessage'});
            this.isNew = this.sinon.stub(this.mMessage, 'isNew');
            this.isNew.returns(true);

            this.existOpenQR = this.sinon.stub(ns.Model.get('quick-reply-state'), 'existOpenQR');
            this.existOpenQR.returns(false);
        });

        describe('любая нотификация о новых письмах не должна показываться', function() {
            it('если письмо на самом деле не новое', function() {
                this.isNew.returns(false);
                expect(Daria.Xiva.NewMsgQueue.shouldPreventAnyNotifications(this.mMessage)).to.be.eql(true);
            });
        });

        describe('какая-нибудь нотификация о новых письмах может показываться', function() {
            it('если пользователь в композе', function() {
                this.sinon.stub(ns.page.current, 'page').value('compose2');
                expect(Daria.Xiva.NewMsgQueue.shouldPreventAnyNotifications(this.mMessage)).to.be.eql(false);
            });

            it('если открыт хотя бы один QR', function() {
                this.existOpenQR.returns(true);
                expect(Daria.Xiva.NewMsgQueue.shouldPreventAnyNotifications(this.mMessage)).to.be.eql(false);
            });
        });
    });

    describe('#shouldPreventPopupNotifications', function() {
        beforeEach(function() {
            this.existOpenQR = this.sinon.stub(ns.Model.get('quick-reply-state'), 'existOpenQR');
            this.existOpenQR.returns(false);
        });

        describe('всплывающая нотификация о новых письмах', function() {
            it('не должна показываться, если пользователь в композе', function() {
                this.sinon.stub(ns.page.current, 'page').value('compose2');
                expect(Daria.Xiva.NewMsgQueue.shouldPreventPopupNotifications()).to.be.eql(true);
            });

            it('не должна показываться, если открыт хотя бы один QR', function() {
                this.existOpenQR.returns(true);
                expect(Daria.Xiva.NewMsgQueue.shouldPreventPopupNotifications()).to.be.eql(true);
            });

            it('должна показываться, если нет открытого QR или пользователь не на странице композа', function() {
                expect(Daria.Xiva.NewMsgQueue.shouldPreventPopupNotifications()).to.be.eql(false);
            });
        });
    });

    describe('.process', function() {

        beforeEach(function() {
            this.xivaData = {
                lcn: 1,
                message: { mid: '123' },
                raw_data: {
                    counters: [ 1, 10, 2, 5 ],
                    countersNew: [ 2, 1 ]
                }
            };
        });

        describe('Daria.Xiva.NewMsgQueue.processData rejected', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva.NewMsgQueue, 'processData').callsFake(() => vow.reject());
                this.sinon.stub(Daria.Xiva.NewMsgQueue, 'insertMessage').returns(new vow.Promise());

                return Daria.Xiva.NewMsgQueue.process(this.xivaData).then(null, function() {
                    return vow.resolve();
                });
            });

            it('не вызывает Daria.Xiva.NewMsgQueue.insertMessage', function() {
                expect(Daria.Xiva.NewMsgQueue.insertMessage).to.have.callCount(0);
            });

        });

        describe('Daria.Xiva.NewMsgQueue.processData resolved', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva.NewMsgQueue, 'processData').returns(vow.resolve());
                this.sinon.stub(Daria.Xiva.NewMsgQueue, 'insertMessage').returns(vow.resolve());

                return Daria.Xiva.NewMsgQueue.process(this.xivaData);
            });

            it('вызывает Daria.Xiva.NewMsgQueue.insertMessage', function() {
                expect(Daria.Xiva.NewMsgQueue.insertMessage).to.have.callCount(1);
            });

        });

    });

    describe('.processData', function() {

        beforeEach(function() {
            this.sinon.stub(ns, 'request').callsFake(() => vow.reject());
            this.sinon.stub(ns, 'forcedRequest').callsFake(() => vow.reject());

            this.mSettings = ns.Model.get('settings');
            this.sinon.stub(this.mSettings, 'isThreaded').returns(false);

            this.xivaData1 = {
                lcn: 1,
                message: { mid: '5' },
                raw_data: {}
            };

            this.xivaData2 = {
                lcn: 1,
                message: { mid: '5', thread_id: '1' },
                raw_data: {}
            };
        });

        it('должен отреджектить промис, если уже есть такое сообщение', function() {
            var mMessage = ns.Model.get('message', {ids: '5'});
            setModelByMock(mMessage);

            return Daria.Xiva.NewMsgQueue.processData(this.xivaData1).then(function() {
                return vow.reject('MUST_REJECT');

            }, function(reason) {
                expect(reason).to.be.equal('KNOWN_MESSAGE');
                return vow.resolve();
            });
        });

        it('должен запросить модель mMessage, если режим не тредный', function() {
            var mMessage = ns.Model.get('message', {ids: '5'});

            return Daria.Xiva.NewMsgQueue.processData(this.xivaData1).then(function() {
                return vow.reject('MUST_REJECT');

            }, function() {
                expect(ns.forcedRequest).to.be.calledWith([mMessage]);
                return vow.resolve();
            });
        });

        it('должен запросить модель mMessages, если режим тредный и нет информации о треде', function() {
            this.mSettings.isThreaded.returns(true);

            return Daria.Xiva.NewMsgQueue.processData(this.xivaData2).then(function() {
                return vow.reject('MUST_REJECT');

            }, function() {

                var mMessages = ns.Model.get('messages', {thread_id: 't1'});
                expect(ns.forcedRequest).to.be.calledWith([mMessages]);
                return vow.resolve();
            });
        });

        it('должен запросить модель mMessage, если режим тредный и есть информации о треде', function() {
            this.mSettings.isThreaded.returns(true);
            ns.Model.get('message', {ids: 't1'}).setData({mid: 't1', tid: 't1', count: 2});

            return Daria.Xiva.NewMsgQueue.processData(this.xivaData2).then(function() {
                return vow.reject('MUST_REJECT');

            }, function() {
                var mMessage = ns.Model.get('message', {ids: '5'});
                expect(ns.forcedRequest).to.be.calledWith([mMessage]);
                return vow.resolve();
            });
        });

    });

    describe('.updateLists', function() {

        beforeEach(function() {
            this.mMessage = ns.Model.get('message', {ids: '3'}).setData({
                'count': 1,
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'lid': [
                    '1'
                ],
                'mid': '3',
                'new': 0,
                'tid': 't1'
            });

            this.mMessageInsert = ns.Model.get('message', {ids: 'insert'});
            this.mMessageReplace = ns.Model.get('message', {ids: 'replace'});

            this.sinon.stub(Daria.Xiva.NewMsgQueue, 'updateThreadModels').returns({
                insert: this.mMessageInsert,
                replace: this.mMessageReplace
            });
        });

        it('должен обновить тредный список писем в папке', function() {
            var mMessages = ns.Model.get('messages', {current_folder: '7', threaded: 'yes'});
            this.sinon.stub(mMessages, 'insertMessage');

            Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

            expect(mMessages.insertMessage).to.be.calledWith(this.mMessageInsert, this.mMessageReplace);
        });

        it('должен обновить нетредный список писем в папке', function() {
            var mMessages = ns.Model.get('messages', {current_folder: '7'});
            this.sinon.stub(mMessages, 'insertMessage');

            Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

            expect(mMessages.insertMessage).to.be.calledWith(this.mMessage);
        });

        it('должен обновить все списки писем по метке', function() {
            var mMessages = ns.Model.get('messages', {current_label: '1'});
            this.sinon.stub(mMessages, 'insertMessage');

            Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

            expect(mMessages.insertMessage).to.be.calledWith(this.mMessage);
        });

        describe('Письмо прочитано ->', function() {

            beforeEach(function() {
                this.mMessage.mark();
            });

            it('не должен обновить список непрочитанных писем в папке', function() {
                var mMessages = ns.Model.get('messages', {current_folder: '7', extra_cond: 'only_new'});
                this.sinon.stub(mMessages, 'insertMessage');

                Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

                expect(mMessages.insertMessage).to.have.callCount(0);
            });

            it('не должен обновить список всех непрочитанных писем', function() {
                var mMessages = ns.Model.get('messages', {'goto': 'all', extra_cond: 'only_new', unread: 'unread'});
                this.sinon.stub(mMessages, 'insertMessage');

                Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

                expect(mMessages.insertMessage).to.have.callCount(0);
            });

        });

        describe('Письмо непрочитано ->', function() {

            beforeEach(function() {
                this.mMessage.unmark();
            });

            it('должен обновить список непрочитанных писем в папке', function() {
                var mMessages = ns.Model.get('messages', {current_folder: '7', extra_cond: 'only_new'});
                this.sinon.stub(mMessages, 'insertMessage');

                Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

                expect(mMessages.insertMessage).to.be.calledWith(this.mMessage);
            });

            it('не должен обновить список всех непрочитанных писем', function() {
                var mMessages = ns.Model.get('messages', {'goto': 'all', extra_cond: 'only_new', unread: 'unread'});
                this.sinon.stub(mMessages, 'insertMessage');

                Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

                expect(mMessages.insertMessage).to.be.calledWith(this.mMessage);
            });

        });

        it('не должен обновить список писем c аттачами, если нет аттача', function() {
            var mMessages = ns.Model.get('messages', {'goto': 'all', extra_cond: 'only_atta'});
            this.sinon.stub(mMessages, 'insertMessage');
            this.sinon.stub(this.mMessage, 'hasAttachment').returns(false);

            Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

            expect(mMessages.insertMessage).to.have.callCount(0);
        });

        it('должен обновить список писем c аттачами, если есть аттача', function() {
            var mMessages = ns.Model.get('messages', {'goto': 'all', extra_cond: 'only_atta'});
            this.sinon.stub(mMessages, 'insertMessage');
            this.sinon.stub(this.mMessage, 'hasAttachment').returns(true);

            Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

            expect(mMessages.insertMessage).to.be.calledWith(this.mMessage);
        });

        ['archive', 'draft', 'spam', 'template', 'trash'].forEach(function(symbol) {

            describe('Письмо из "' + symbol + '"', function() {

                beforeEach(function() {
                    /** @type Daria.mFolders */
                    var mFolders = ns.Model.get('folders');
                    setModelByMock(mFolders);

                    this.symbolFid = mFolders.getFidBySymbol(symbol);
                    if (!this.symbolFid) {
                        throw new Error('No mock for folder "' + symbol + '"');
                    }

                    this.mMessage.set('.fid', this.symbolFid);
                });

                it('не должен обновлять информацию о треде', function() {
                    Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);
                    expect(Daria.Xiva.NewMsgQueue.updateThreadModels).to.have.callCount(0);
                });

                it('должен обновить нетредный список писем в папке', function() {
                    var mMessages = ns.Model.get('messages', {current_folder: this.symbolFid});
                    this.sinon.stub(mMessages, 'insertMessage');

                    Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

                    expect(mMessages.insertMessage).to.be.calledWith(this.mMessage);
                });

                it('не должен обновить тредный список писем в папке', function() {
                    var mMessages = ns.Model.get('messages', {current_folder: this.symbolFid, threaded: 'yes'});
                    this.sinon.stub(mMessages, 'insertMessage');

                    Daria.Xiva.NewMsgQueue.updateLists(this.mMessage);

                    expect(mMessages.insertMessage).to.have.callCount(0);
                });

            });

        });

    });

    describe('.updateThreadModels', function() {

        describe('Была информация о треде ->', function() {

            beforeEach(function() {
                this.mMessage = ns.Model.get('message', {ids: '3'}).setData({
                    'count': 1,
                    'date': {
                        'chunks': {}
                    },
                    'fid': '7',
                    'lid': [],
                    'mid': '3',
                    'tid': 't1'
                });

                this.mThread = ns.Model.get('message', {ids: 't1'}).setData({
                    'count': 2,
                    'date': {
                        'chunks': {}
                    },
                    'fid': '7',
                    'lid': [],
                    'mid': 't1',
                    'tid': 't1'
                });

                this.mThreadMessages = ns.Model.get('messages', {thread_id: 't1'});

                this.sinon.stub(this.mThread, 'updateThreadInfo');
                this.sinon.stub(this.mThreadMessages, 'insertMessage');
            });

            it('должен обновить информацию о треде', function() {
                Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                expect(this.mThread.updateThreadInfo).to.be.calledWith(this.mMessage);
            });

            it('должен вставить письмо в список писем в треде', function() {
                Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                expect(this.mThreadMessages.insertMessage).to.be.calledWith(this.mMessage);
            });

            it('должен вернуть, что надо вставить тред и заменить его на тред', function() {
                expect(Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage)).to.be.eql({
                    insert: this.mThread,
                    replace: this.mThread
                });
            });

        });

        describe('Не было информации о треде -> ', function() {

            beforeEach(function() {
                this.sinon.stub(ns.Model.get('settings'), 'isThreaded').returns(true);

                this.mMessage = ns.Model.get('message', {ids: '3'}).setData({
                    'count': 1,
                    'date': {
                        'chunks': {}
                    },
                    'fid': '7',
                    'lid': [],
                    'mid': '3',
                    'tid': 't1'
                });

                this.mThread = ns.Model.get('message', {ids: 't1'});

                this.mThreadMessages = ns.Model.get('messages', {thread_id: 't1'});

                this.sinon.stub(this.mThreadMessages, 'insertMessage');
                this.sinon.stub(Daria.Xiva, 'createThreadFromMessage');
            });

            describe('образовался тред -> ', function() {

                beforeEach(function() {
                    this.mThreadMessages.setData({
                        details: {},
                        message: [
                            {
                                'count': 1,
                                'date': {
                                    'chunks': {}
                                },
                                'fid': '7',
                                'lid': [],
                                'mid': '3',
                                'tid': 't1'
                            },
                            {
                                'count': 1,
                                'date': {
                                    'chunks': {}
                                },
                                'fid': '7',
                                'lid': [],
                                'mid': '4',
                                'tid': 't1'
                            }
                        ]
                    });
                });

                it('должен вставить письмо в список писем в треде', function() {
                    Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                    expect(this.mThreadMessages.insertMessage)
                        .to.have.callCount(1)
                        .and.to.be.calledWith(this.mMessage);
                });

                it('должен создать модель треда из письма, которое уже было в треде (не новое)', function() {
                    Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                    expect(Daria.Xiva.createThreadFromMessage)
                        .to.have.callCount(1)
                        .and.to.be.calledWith(ns.Model.get('message', {ids: '4'}), this.mThread, 1);
                });

                it('должен вернуть, что надо вставить тред и заменить его на тред', function() {
                    expect(Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage)).to.be.eql({
                        insert: this.mThread,
                        replace: this.mThread
                    });
                });

            });

            describe('Был тред, про который мы не знали -> ', function() {

                beforeEach(function() {
                    this.mThreadMessages.setData({
                        details: {},
                        message: [
                            {
                                'count': 1,
                                'date': {
                                    'chunks': {}
                                },
                                'fid': '7',
                                'lid': [],
                                'mid': '3',
                                'tid': 't1'
                            },
                            {
                                'count': 1,
                                'date': {
                                    'chunks': {}
                                },
                                'fid': '7',
                                'lid': [],
                                'mid': '4',
                                'tid': 't1'
                            },
                            {
                                'count': 1,
                                'date': {
                                    'chunks': {}
                                },
                                'fid': '7',
                                'lid': [],
                                'mid': '5',
                                'tid': 't1'
                            }
                        ]
                    });
                });

                it('должен вставить письмо в список писем в треде', function() {
                    Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                    expect(this.mThreadMessages.insertMessage).to.be.calledWith(this.mMessage);
                });

                it('должен создать модель треда из письма, которое уже было в треде (не новое)', function() {
                    Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                    expect(Daria.Xiva.createThreadFromMessage).to.be.calledWith(ns.Model.get('message', {ids: '4'}), this.mThread, 2);
                });

                it('должен вернуть, что надо вставить тред', function() {
                    expect(Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage)).to.be.eql({
                        insert: this.mThread,
                        replace: this.mThread
                    });
                });

            });

            describe('не образовался тред -> ', function() {

                beforeEach(function() {
                    this.mThreadMessages.setData({
                        details: {},
                        message: [
                            {
                                'count': 1,
                                'date': {
                                    'chunks': {}
                                },
                                'fid': '7',
                                'lid': [],
                                'mid': '3',
                                'tid': 't1'
                            }
                        ]
                    });
                });

                it('должен вставить письмо в список писем в треде', function() {
                    Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                    expect(this.mThreadMessages.insertMessage).to.have.callCount(1);
                });

                it('должен создать модель треда из письма', function() {
                    Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage);

                    expect(Daria.Xiva.createThreadFromMessage).to.have.callCount(1);
                });

                it('должен вернуть, что надо вставить письмо и заменить его на письма', function() {
                    expect(Daria.Xiva.NewMsgQueue.updateThreadModels(this.mMessage)).to.be.eql({
                        insert: this.mThread,
                        replace: this.mThread
                    });
                });

            });

        });

    });

    describe('#toShowNotification', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.Xiva.NewMsgQueue, '_getCurrentMessages');
            this.updatedLists = [
                ns.Model.get('messages', { current_folder: '2' }),
                ns.Model.get('messages', { current_folder: '3' })
            ];

            this.mSettings = ns.Model.get('settings');
            this.stubShowNotifications = this.sinon.stub(this.mSettings, 'getSign').withArgs('notify_message');
        });

        it('Должен вернуть false если у пользователя нет настройки', function() {
            this.stubShowNotifications.returns(false);

            expect(Daria.Xiva.NewMsgQueue.toShowNotification(this.updatedLists)).to.be.equal(false);
        });

        it('Должен вернуть true, если пользователь не в списке писем', function() {
            this.stubShowNotifications.returns(true);
            this.sinon.stub(ns.page.current, 'page').value('settings');

            expect(Daria.Xiva.NewMsgQueue.toShowNotification(this.updatedLists)).to.be.equal(true);
        });

        it('Должен вернуть true, если письмо пришло не в текущий список', function() {
            this.stubShowNotifications.returns(true);
            this.sinon.stub(ns.page.current, 'page').value('messages');
            Daria.Xiva.NewMsgQueue._getCurrentMessages.returns(
                ns.Model.get('messages', { current_folder: '1' })
            );

            expect(Daria.Xiva.NewMsgQueue.toShowNotification(this.updatedLists)).to.be.equal(true);
        });

        it('Должен вернуть false, если письмо пришло в текущий список', function() {
            this.stubShowNotifications.returns(true);
            this.sinon.stub(ns.page.current, 'page').value('messages');
            Daria.Xiva.NewMsgQueue._getCurrentMessages.returns(
                ns.Model.get('messages', { current_folder: '2' })
            );

            expect(Daria.Xiva.NewMsgQueue.toShowNotification(this.updatedLists)).to.be.equal(false);
        });

    });

    describe('#showNotification', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.Xiva.NewMsgQueue, 'showNotificationCalendarEvent');
            this.sinon.stub(Daria.Xiva.NewMsgQueue, 'showNotificationMessage');

            this.mMessage = ns.Model.get('message', { ids: '1' });
            this.sinon.stub(this.mMessage, 'isCalendarService');
        });

        it.skip('Должен вызвать показ нотификации от календаря, если письмо от календаря', function() {
            this.mMessage.isCalendarService.returns(true);

            Daria.Xiva.NewMsgQueue.showNotification(this.mMessage);

            expect(Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent).to.have.callCount(1);
            expect(Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent).to.be.calledWith(this.mMessage);
            expect(Daria.Xiva.NewMsgQueue.showNotificationMessage).to.have.callCount(0);
        });

        it('Должен вызвать показ нотификации о новом сообщение, если письмо не от календаря', function() {
            this.mMessage.isCalendarService.returns(false);

            Daria.Xiva.NewMsgQueue.showNotification(this.mMessage);

            expect(Daria.Xiva.NewMsgQueue.showNotificationMessage).to.have.callCount(1);
            expect(Daria.Xiva.NewMsgQueue.showNotificationMessage).to.be.calledWith(this.mMessage);
            expect(Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent).to.have.callCount(0);
        });

    });

    describe('#showNotificationCalendarEvent', function() {

        beforeEach(function() {
            this.sinon.stub(ns, 'request');
            this.sinon.stub(Daria.Xiva.NewMsgQueue, 'showNotificationMessage');

            this.mMessage = ns.Model.get('message', { ids: '1' });
            this.sinon.stub(this.mMessage, 'getFolderId');

            this.mMessageWidgetEvents = ns.Model.get('message-widget-events', { ids: '1' });
            this.mMessageWidgetEventsMock = {
                eventInfo: {
                    name: 'Поговорить',
                    location: 'Камчатка',
                    start: 10000,
                    end: 20000
                }
            };

            this.mNotifications = ns.Model.get('notifications');
            this.sinon.stub(this.mNotifications, 'addNotification');
        });

        it('Должен показать нотификацию о новой встречи', function() {
            var that = this;
            var eventInfo = this.mMessageWidgetEventsMock.eventInfo;

            this.mMessageWidgetEvents.setData(this.mMessageWidgetEventsMock);
            ns.request.returns(vow.resolve([ this.mMessageWidgetEvents ]));

            return Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent(this.mMessage).then(function() {
                expect(that.mNotifications.addNotification).to.have.callCount(1);
                expect(that.mNotifications.addNotification).to.be.calledWith({
                    type: 'calendar',
                    name: eventInfo.name,
                    location: eventInfo.location,
                    start: eventInfo.start,
                    end: eventInfo.end
                });
            });
        });

        it('Должен показать нотификацию об измененной встречи', function() {
            var that = this;
            var eventInfo = this.mMessageWidgetEventsMock.eventInfo;

            this.mMessageWidgetEventsMock.eventInfo.calendarMailType = 'event_update';

            this.mMessageWidgetEvents.setData(this.mMessageWidgetEventsMock);
            ns.request.returns(vow.resolve([ this.mMessageWidgetEvents ]));

            return Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent(this.mMessage).then(function() {
                expect(that.mNotifications.addNotification).to.have.callCount(1);
                expect(that.mNotifications.addNotification).to.be.calledWith({
                    type: 'calendar',
                    name: eventInfo.name,
                    location: eventInfo.location,
                    start: eventInfo.start,
                    end: eventInfo.end,
                    label: 'updated'
                });
            });
        });

        it('Должен показать нотификацию об отмененной встречи', function() {
            var that = this;
            var eventInfo = this.mMessageWidgetEventsMock.eventInfo;

            this.mMessageWidgetEventsMock.eventInfo.isCancelled = true;

            this.mMessageWidgetEvents.setData(this.mMessageWidgetEventsMock);
            ns.request.returns(vow.resolve([ this.mMessageWidgetEvents ]));

            return Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent(this.mMessage).then(function() {
                expect(that.mNotifications.addNotification).to.have.callCount(1);
                expect(that.mNotifications.addNotification).to.be.calledWith({
                    type: 'calendar',
                    name: eventInfo.name,
                    location: eventInfo.location,
                    start: eventInfo.start,
                    end: eventInfo.end,
                    label: 'canceled'
                });
            });
        });

        it('Должен показать нотификацию о новом сообщение, если не удалось получить данные из календаря', function() {
            var that = this;

            ns.request.callsFake(() => vow.reject());

            return Daria.Xiva.NewMsgQueue.showNotificationCalendarEvent(this.mMessage).then(function() {
                expect(Daria.Xiva.NewMsgQueue.showNotificationMessage).to.have.callCount(1);
                expect(Daria.Xiva.NewMsgQueue.showNotificationMessage).to.be.calledWith(that.mMessage);
            });
        });

    });

    describe('#showNotificationMessage', function() {

        beforeEach(function() {
            this.sinon.stub(ns.router, 'generateUrl').returns('url');

            this.mNotifications = ns.Model.get('notifications');
            this.sinon.stub(this.mNotifications, 'addNotification');

            this.mMessage = ns.Model.get('message', { ids: '1' });
            this.sinon.stub(this.mMessage, 'getFolderId');
            this.sinon.stub(this.mMessage, 'getFromEmail').returns('test@ya.ru');
            this.sinon.stub(this.mMessage, 'getSubject').returns('Тема письма');
            this.sinon.stub(this.mMessage, 'getFirstline').returns('Первая строка письма');
            this.sinon.stub(this.mMessage, 'getFromName').returns('tester tester');
            this.sinon.stub(this.mMessage, 'getEmailRef').returns('123456');
        });

        it('Должен показать нотификацию о новом письме', function() {
            Daria.Xiva.NewMsgQueue.showNotificationMessage(this.mMessage);

            expect(this.mNotifications.addNotification).to.have.callCount(1);
            expect(this.mNotifications.addNotification).to.be.calledWith({
                type: 'message',
                subject: 'Тема письма',
                email: 'test@ya.ru',
                firstline: 'Первая строка письма',
                name: 'tester tester',
                ref: '123456',
                url: 'url'
            });
        });

    });

});
