describe('Daria.MOPS', function() {
    beforeEach(function() {
        this.mFolders = ns.Model.get('folders');
        setModelByMock(this.mFolders);

        var messageData = mock.message[0].data;

        var mMessage = ns.Model.get('message', { ids: messageData.mid }).setData(messageData);
        /** @type Daria.mMessagesChecked */
        this.mMessagesChecked = ns.Model.get('messages-checked', { current_folder: '1' });
        this.mMessagesChecked.check(mMessage, true);
        this.sinon.stub(Daria.MOPS, 'showStatusline');
        this.sinon.stub(Daria.MOPS, 'showNotification');
        this.sinon.stub(Daria.Statusline, 'show');
        Daria.MOPS.HooksManager.instance().hooks = { before: {} };
    });

    describe('Archive', function() {
        xit('Должен вызывать запрос на перемещение выбранного письма', function() {
            this.sinon.stub(this.mFolders, 'createArchiveFolder').returns(Vow.fulfill());
            this.sinon.stub(Daria.MOPS, 'onMove');

            var forcedRequestStub = this.sinon.stub(ns, 'forcedRequest').returns(Vow.fulfill());

            return Daria.MOPS.archive(this.mMessagesChecked).then(function() {
                expect(forcedRequestStub).to.have.callCount(1);
                var call = forcedRequestStub.getCall(0);

                // проверяем параметры запроса
                expect(Boolean(_.find(call.args[0], { id: 'do-messages' }))).to.be.equal(true);
                expect(call.args[0][0].params).to.be.eql({
                    ids: [ '5' ],
                    tids: [],
                    movefile: '3',
                    action: 'move'
                });
            });
        });
    });

    describe('.fixMoveAction', function() {
        it('должен исправить "move" на "delete", если переносим в "Удаленные"', function() {
            expect(Daria.MOPS.fixMoveAction('move', '7')).to.be.equal('delete');
        });

        it('должен исправить "move" на "spam", если переносим в "Спам"', function() {
            expect(Daria.MOPS.fixMoveAction('move', '2')).to.be.equal('tospam');
        });
    });

    describe('Infolder', function() {
        xit('Вызывает правильное событие по завершению операции', function() {
            this.sinon.stub(ns, 'forcedRequest').returns(Vow.fulfill());

            var expectedArg = {
                action: 'move',
                originalAction: 'infolder',
                ids: { ids: [ '5' ], tids: [] },
                count: 1
            };

            var handlerStub = this.sinon.stub();
            ns.events.once('daria:MOPS:move', handlerStub);

            return Daria.MOPS.infolder(this.mMessagesChecked, { fid: '9' }).then(function() {
                expect(handlerStub).to.have.callCount(1);

                var args = handlerStub.getCall(0).args;
                expect(args[0]).to.be.eql('daria:MOPS:move');
                // проверяем, что все поля из expectedArg присутствуют во втором параметре и их значения равны
                expect(_.all(args[1], function(value, key) {
                    // если поле не присутствует в expectedArg, то пропускаем (возвращаем истину), иначе – проверяем
                    return !(key in expectedArg) || _.isEqual(value, expectedArg[key]);
                })).to.be.equal(true);
            });
        });
    });

    describe('.invalidateModelsOnMove', function() {
        beforeEach(function() {
            this.sinon.spy(ns.Model, 'destroyAll');
        });

        it('должен удалить кеши "message-nearest"', function() {
            Daria.MOPS.invalidateModelsOnMove('move', {}, {});
            expect(ns.Model.destroyAll).to.be.calledWith([ 'message-nearest' ]);
        });

        [ 'delete', 'tospam', 'notspam' ].forEach(function(action) {
            it('должен удалить кеши "message-thread-nearest" для "' + action + '"', function() {
                Daria.MOPS.invalidateModelsOnMove(action, {}, {});
                expect(ns.Model.destroyAll).to.be.calledWith([ 'message-thread-nearest' ]);
            });
        });

        it('должен удалить кеши "messages-pager" для исходной папки', function() {
            var mMessagesSource = ns.Model.get('messages-pager', { current_folder: '1' });
            this.sinon.spy(mMessagesSource, 'destroy');

            Daria.MOPS.invalidateModelsOnMove('move', {}, { current_folder: '1' });
            expect(mMessagesSource.destroy).to.have.callCount(1);
        });

        it('должен удалить кеши "messages-pager" для папки назначения', function() {
            var mMessagesDestination = ns.Model.get('messages-pager', { current_folder: '2' });
            this.sinon.spy(mMessagesDestination, 'destroy');

            Daria.MOPS.invalidateModelsOnMove('move', { current_folder: '2' }, { current_folder: '1' });
            expect(mMessagesDestination.destroy).to.have.callCount(1);
        });
    });

    describe('.label', function() {
        beforeEach(function() {
            /** @type Daria.mMessagesChecked */
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mMessagesChecked.resetChecked();

            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                lid: [],
                mid: '1',
                new: 1,
                tid: 't1'
            });

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 2,
                lid: [],
                mid: 't1',
                new: 1,
                tid: 't1'
            });
            this.mDoLabel = ns.Model.get('do-label', { lid: '1234' }).setData({ taskType: 'sync' });

            this.sinon.stub(ns, 'forcedRequest').returns(Vow.resolve([ this.mDoLabel ]));

            this.sinon.stub(ns.page, 'go');
        });

        it('не должен запрашивать mLabels, если обновили все модели', function() {
            this.mMessagesChecked.check(this.mMessage, true);

            return Daria.MOPS.label(this.mMessagesChecked, { lid: '1234' }).then(function() {
                expect(ns.forcedRequest).to.have.calledWith([
                    {
                        id: 'do-label',
                        params: {
                            ids: [ '1' ],
                            lid: '1234',
                            tids: []
                        }
                    }
                ]);
            });
        });

        it('должен запрашивать mLabels, если не удалось обновить модели', function() {
            this.mMessagesChecked.check(this.mThread, true);

            return Daria.MOPS.label(this.mMessagesChecked, { lid: '1234' }).then(function() {
                expect(ns.forcedRequest).to.have.calledWith([
                    {
                        id: 'do-label',
                        params: {
                            ids: [],
                            lid: '1234',
                            tids: [ 't1' ]
                        }
                    },
                    {
                        id: 'labels'
                    }
                ]);
            });
        });
    });

    describe('.unlabel', function() {
        beforeEach(function() {
            /** @type Daria.mMessagesChecked */
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mMessagesChecked.resetChecked();

            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                lid: [ '1234', '2345' ],
                mid: '1',
                new: 1,
                tid: 't1'
            });
            this.mDoUnlabel = ns.Model.get('do-unlabel', { lid: '1234' }).setData({ taskType: 'sync' });

            this.requestPromise = Vow.resolve([ this.mDoUnlabel ]);
            this.sinon.stub(ns, 'forcedRequest').returns(this.requestPromise);

            this.sinon.stub(ns.page, 'go');
            this.sinon.stub(Daria, 'actionLog');
        });

        describe('обновили все модели (внучную сняли метку у отдельных mMessage моделей)', function() {
            beforeEach(function() {
                this.mMessagesChecked.check(this.mMessage, true);
            });

            it('должен удалить lid у mMessage синхронно', function() {
                Daria.MOPS.unlabel(this.mMessagesChecked, { lid: '1234' });
                expect(this.mMessage.get('.lid')).to.be.eql([ '2345' ]);
            });

            it('не должен запрашивать mLabels', function() {
                return Daria.MOPS.unlabel(this.mMessagesChecked, { lid: '1234' }).then(function() {
                    expect(ns.forcedRequest).to.have.calledWith([
                        {
                            id: 'do-unlabel',
                            params: {
                                ids: [ '1' ],
                                lid: '1234',
                                tids: []
                            }
                        }
                    ]);
                });
            });

            it('должен вернуть promise от ns.forcedRequest', function() {
                expect(Daria.MOPS.unlabel(this.mMessagesChecked, { lid: '1234' })).to.be.equal(this.requestPromise);
            });

            it('должен вызвать перерисовку после выполнения ns.forcedRequest', function() {
                var callCount = ns.page.go.callCount;
                return this.requestPromise.then(function() {
                    expect(ns.page.go.callCount - callCount).to.be.equal(1);
                });
            });
        });
    });

    describe('.unmark', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());
            this.sinon.stub(Daria, 'areFoldersTabsEnabled');
        });

        describe('На корпе ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.sinon.stub(ns.page.current, 'page').value('messages');
                this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                    mid: '1',
                    new: 0,
                    count: 1,
                    tid: 't1'
                });
            });

            it('Всегда выполняем перезапрос списка папок', function() {
                this.mMessagesChecked.check(this.mMessage, true);
                Daria.areFoldersTabsEnabled.returns(false);

                return Daria.MOPS.unmark(this.mMessagesChecked).then(function() {
                    expect(ns.forcedRequest).to.have.calledWith([
                        { id: 'do-messages', params: { action: 'unmark', ids: [ '1' ], tids: [] } },
                        { id: 'folders' }
                    ]);
                });
            });
            it('Перезапрашивает список папок + табов если табы есть', function() {
                this.mMessagesChecked.check(this.mMessage, true);
                Daria.areFoldersTabsEnabled.returns(true);

                return Daria.MOPS.unmark(this.mMessagesChecked).then(function() {
                    expect(ns.forcedRequest).to.have.calledWith([
                        { id: 'do-messages', params: { action: 'unmark', ids: [ '1' ], tids: [] } },
                        { id: 'folders' },
                        { id: 'tabs' }
                    ]);
                });
            });
        });
    });

    describe('.mark', function() {
        beforeEach(function() {
            /** @type Daria.mMessagesChecked */
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());
            this.sinon.stub(Daria, 'areFoldersTabsEnabled');
        });

        describe('Работа с несколькими письмами ->', function() {
            beforeEach(function() {
                this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                    mid: '1',
                    new: 1,
                    tid: 't1'
                });

                this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                    count: 2,
                    mid: 't1',
                    new: 1,
                    tid: 't1'
                });
            });

            it('не должен запрашивать mFolders, если обновили все модели', function() {
                this.mMessagesChecked.check(this.mMessage, true);

                return Daria.MOPS.mark(this.mMessagesChecked).then(function() {
                    expect(ns.forcedRequest).to.have.calledWith([
                        {
                            id: 'do-messages',
                            params: { action: 'mark', ids: [ '1' ], tids: [] }
                        }
                    ]);
                });
            });
        });

        describe('На корпе ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.sinon.stub(ns.page.current, 'page').value('messages');
                this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                    mid: '1',
                    new: 1,
                    tid: 't1'
                });
            });

            it('Всегда выполняем перезапрос списка папок', function() {
                this.mMessagesChecked.check(this.mMessage, true);
                Daria.areFoldersTabsEnabled.returns(false);

                return Daria.MOPS.mark(this.mMessagesChecked).then(function() {
                    expect(ns.forcedRequest).to.have.calledWith([
                        { id: 'do-messages', params: { action: 'mark', ids: [ '1' ], tids: [] } },
                        { id: 'folders' }
                    ]);
                });
            });

            it('Перезапрашивает список папок + табов если табы есть', function() {
                this.mMessagesChecked.check(this.mMessage, true);
                Daria.areFoldersTabsEnabled.returns(true);

                return Daria.MOPS.mark(this.mMessagesChecked).then(function() {
                    expect(ns.forcedRequest).to.have.calledWith([
                        { id: 'do-messages', params: { action: 'mark', ids: [ '1' ], tids: [] } },
                        { id: 'folders' },
                        { id: 'tabs' }
                    ]);
                });
            });
        });
    });

    describe('#_undoFailedAction ->', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                mid: '1',
                new: 0,
                tid: 't1'
            });

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 2,
                mid: 't1',
                new: 0,
                tid: 't1'
            });

            this.opinfo = new Daria.MOPS.Opinfo();
            this.opinfo.saveMid('1');
            this.opinfo.saveUnreadCounter('t1', 2);
        });

        it('Должен пометить письмо как непрочитанное', function() {
            Daria.MOPS._undoFailedAction('mark', this.opinfo);
            expect(this.mMessage.isNew()).to.be.equal(true);
        });

        it('Должен пометить письмо как прочитанное', function() {
            Daria.MOPS._undoFailedAction('unmark', this.opinfo);
            expect(this.mMessage.isNew()).to.be.equal(false);
        });

        it('Должен восстановить счетчик непрочитанных писем треда', function() {
            Daria.MOPS._undoFailedAction('unmark', this.opinfo);
            expect(this.mThread.get('.new')).to.be.equal(2);
        });
    });

    describe('#_hasModelRequestFailed', function() {
        beforeEach(function() {
            this.models = { invalid: [ { id: 'do-messages' } ] };
        });

        it('должен вернуть true', function() {
            expect(Daria.MOPS._hasModelRequestFailed(this.models, 'do-messages')).to.be.equal(true);
        });

        it('должен вернуть false', function() {
            expect(Daria.MOPS._hasModelRequestFailed(this.models, 'messages')).to.be.equal(false);
        });
    });

    describe('.markByFid', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { current_folder: '1', ids: '10' }).setData({
                count: '1',
                fid: 1,
                mid: '10',
                tid: 't10',
                new: 1
            });

            this.mThreadMessage = ns.Model.get('message', { current_folder: '1', ids: 't11' }).setData({
                count: '1',
                fid: 1,
                mid: '11',
                tid: 't11',
                new: 1
            });

            this.mFolders = ns.Model.get('folders').setData({
                folder: [
                    { fid: '1', new: 2, subfolder: [] },
                    { fid: '2', new: 2, subfolder: [] }
                ]
            });

            this.sinon.spy(this.mFolders, 'adjustUnreadCounters');
            this.sinon.spy(this.mMessage, 'mark');
            this.sinon.spy(this.mMessage, 'unmark');
            this.sinon.spy(ns.Model, 'traverse');
            this.sinon.stub(this.mMessage, 'isThread').returns(false);
            this.sinon.stub(this.mMessage, 'getFolderId').returns('1');

            this.sinon.spy(this.mThreadMessage, 'mark');
            this.sinon.spy(this.mThreadMessage, 'unmark');
            this.sinon.stub(this.mThreadMessage, 'isThread').returns(true);
            this.sinon.stub(this.mThreadMessage, 'getFolderId').returns('1');
        });

        it('должен вызвать mMessage#mark при пометке прочитанным', function() {
            Daria.MOPS.markByFid('mark', '1');

            expect(this.mMessage.mark).to.have.callCount(1);
        });

        it('должен вызвать mMessage#mark у тредного письма при пометке прочитанной папки', function() {
            Daria.MOPS.markByFid('mark', '1');

            expect(this.mThreadMessage.mark).to.have.callCount(1);
        });

        it('должен вызвать mMessage#unmark при пометке непрочитанным', function() {
            Daria.MOPS.markByFid('unmark', '1');

            expect(this.mMessage.unmark).to.have.callCount(1);
        });

        it('должен вызвать mFolders#adjustUnreadCounters', function() {
            Daria.MOPS.markByFid('mark', '1');

            expect(this.mFolders.adjustUnreadCounters).to.have.callCount(1);
        });

        it('должен вызвать ns.Model.traverse', function() {
            Daria.MOPS.markByFid('mark', '1');

            expect(ns.Model.traverse).to.have.callCount(1);
        });
    });

    describe('.doActionInModelsByIds', function() {
        beforeEach(function() {
            this.mFolders = ns.Model.get('folders');
            setModelByMock(this.mFolders);

            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                fid: 1,
                mid: '1',
                new: 1,
                tid: 't1'
            });

            ns.Model.get('message', { ids: '2' }).setData({
                count: 1,
                fid: 1,
                mid: '2',
                new: 1,
                tid: 't1'
            });

            ns.Model.get('message', { ids: 't2' }).setData({
                count: 2,
                fid: 1,
                mid: 't2',
                new: 1,
                tid: 't2'
            });

            this.sinon.spy(this.mFolders, 'adjustUnreadCounters');
            this.sinon.spy(this.mMessage, 'mark');
            this.sinon.spy(this.mMessage, 'unmark');
        });

        it('должен вызвать mMessage#mark при пометке прочитанным', function() {
            Daria.MOPS.doActionInModelsByIds({ ids: [ '1' ], tids: [] }, 'mark');

            expect(this.mMessage.mark).to.have.callCount(1);
        });

        it('должен вызвать mMessage#unmark при пометке непрочитанным', function() {
            Daria.MOPS.doActionInModelsByIds({ ids: [ '1' ], tids: [] }, 'unmark');

            expect(this.mMessage.unmark).to.have.callCount(1);
        });

        it('должен вызвать mFolders#adjustUnreadCounters, если обновили модели', function() {
            Daria.MOPS.doActionInModelsByIds({ ids: [ '1' ], tids: [] }, 'mark', { fid: '1' });

            expect(this.mFolders.adjustUnreadCounters).to.have.callCount(1);
        });

        it('должен склеивать смещение mFolders, если обновили модели', function() {
            Daria.MOPS.doActionInModelsByIds({ ids: [ '1', '2' ], tids: [] }, 'mark', { fid: '1' });

            expect(this.mFolders.adjustUnreadCounters).to.be.calledWith({
                1: -2
            });
        });

        it('не должен вызвать mFolders#adjustUnreadCounters, если не обновили модели', function() {
            Daria.MOPS.doActionInModelsByIds({ ids: [ '1' ], tids: [ 't2' ] }, 'mark');

            expect(this.mFolders.adjustUnreadCounters).to.have.callCount(0);
        });

        it('должен вернуть Daria.MOPS.Opinfo без смещения, если не обновили модели', function() {
            var opinfo = Daria.MOPS.doActionInModelsByIds({ ids: [ '1' ], tids: [ 't2' ] }, 'mark');
            expect(opinfo.hasAdjust()).to.be.equal(false);
        });

        it('должен вернуть Daria.MOPS.Opinfo с смещением, если обновили модели', function() {
            var opinfo = Daria.MOPS.doActionInModelsByIds({ ids: [ '1' ], tids: [] }, 'mark');
            expect(opinfo.hasAdjust()).to.be.equal(true);
        });
    });

    describe('.pin', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.MOPS, '_pinHelper');
        });

        it('Должен вызвать _pinHelper в режиме pin', function() {
            Daria.MOPS.pin({});
            expect(Daria.MOPS._pinHelper.calledWith({}, true)).to.be.equal(true);
        });
    });

    describe('.unpin', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.MOPS, '_pinHelper');
        });

        it('Должен вызвать _pinHelper в режиме unpin', function() {
            Daria.MOPS.unpin({});
            expect(Daria.MOPS._pinHelper.calledWith({}, false)).to.be.equal(true);
        });
    });

    describe('._getReplacementTreeAfterPin', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                foo: 'bar'
            });
            this.mMessagesChecked.resetChecked();

            this.mMessage_1 = ns.Model.get('message', { ids: '1' }).setData({ mid: '1', fid: '1', tid: '1' });
            this.mMessage_2 = ns.Model.get('message', { ids: '2' }).setData({ mid: '2', fid: '1', tid: '2' });
            this.mMessage_3 = ns.Model.get('message', { ids: '3' }).setData({ mid: '3', fid: '2', tid: 't3' });

            this.mThreadMessage_1 = ns.Model.get('message', { ids: 't1' }).setData({
                mid: 't1',
                tid: 't1',
                fid: '1',
                count: 2
            });
            this.mThreadMessage_2 = ns.Model.get('message', { ids: 't2' }).setData({
                mid: 't2',
                tid: 't2',
                fid: '1',
                count: 3
            });
            this.mThreadMessage_3 = ns.Model.get('message', { ids: 't3' }).setData({
                mid: 't3',
                tid: 't3',
                fid: '1',
                count: 4
            });
        });

        describe('Тредный режим.', function() {
            beforeEach(function() {
                this.sinon.stub(ns.Model.get('settings'), 'isThreaded').returns(true);
            });

            describe('Запинивание.', function() {
                describe('Письмо внутри треда.', function() {
                    it('Должен вернуть тред, вместо письма.', function() {
                        this.mMessagesChecked.check(this.mMessage_3, true);
                        var result = Daria.MOPS._getReplacementTreeAfterPin(this.mMessagesChecked, true);
                        expect(Object.keys(result)).to.be.eql([ 't3' ]);
                    });
                });

                describe('Тред.', function() {
                    it('Должен вернуть тред.', function() {
                        this.mMessagesChecked.check(this.mThreadMessage_1, true);
                        var result = Daria.MOPS._getReplacementTreeAfterPin(this.mMessagesChecked, true);
                        expect(Object.keys(result)).to.be.eql([ 't1' ]);
                    });
                });
            });

            describe('Распинивание.', function() {
                describe('Письмо внутри треда.', function() {
                    it('Если в треде остались запиненные письма, то не должен вернуть такой тред.', function() {
                        this.sinon.stub(ns.Model.get('messages', { thread_id: 't3' }), 'getPinned')
                            .returns(new Array(2));
                        this.mMessagesChecked.check(this.mMessage_3, true);
                        var result = Daria.MOPS._getReplacementTreeAfterPin(this.mMessagesChecked, false);
                        expect(Object.keys(result)).to.be.eql([]);
                    });

                    it('Если в треде не осталось запиненных писем, то должен вернуть тред.', function() {
                        this.sinon.stub(ns.Model.get('messages', { thread_id: 't3' }), 'getPinned').returns([]);
                        this.mMessagesChecked.check(this.mMessage_3, true);
                        var result = Daria.MOPS._getReplacementTreeAfterPin(this.mMessagesChecked, false);
                        expect(Object.keys(result)).to.be.eql([ 't3' ]);
                    });
                });
            });
        });

        describe('Не тредный режим.', function() {
            beforeEach(function() {
                this.sinon.stub(ns.Model.get('settings'), 'isThreaded').returns(false);
            });

            describe('Запинивание.', function() {
                it('Должен вернуть письмо.', function() {
                    this.mMessagesChecked.check(this.mMessage_1, true);
                    var result = Daria.MOPS._getReplacementTreeAfterPin(this.mMessagesChecked, true);
                    expect(Object.keys(result)).to.be.eql([ '1' ]);
                });
            });

            describe('Распинивание.', function() {
                it('Должен вернуть письмо.', function() {
                    this.mMessagesChecked.check(this.mMessage_1, true);
                    var result = Daria.MOPS._getReplacementTreeAfterPin(this.mMessagesChecked, false);
                    expect(Object.keys(result)).to.be.eql([ '1' ]);
                });
            });
        });
    });

    describe('moveHelper', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mMessagesChecked.resetChecked();
            this.mDoMessages = ns.Model.get('do-messages', { ids: '1' }).setData({ taskType: 'sync' });
            this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve([ this.mDoMessages ]));
            this.sinon.stub(Daria.messages, 'whereToGoAfterMove');
        });

        describe('Работа с коллекцией выделенных писем', function() {
            beforeEach(function() {
                this.message = [];
                this.message.push(ns.Model.get('message', { ids: '1' }).setData({
                    mid: '1',
                    tid: 't1'
                }));

                this.mMessages = ns.Model.get('messages', { thread_id: 't1' });
            });

            it('Должен удалить модель письма из коллекции в 3pane', function() {
                this.sinon.stub(Daria, 'is3pane').returns(true);
                this.sinon.stub(ns.page.current, 'params').value({
                    thread_id: 't1'
                });
                this.sinon.stub(this.mMessagesChecked, 'getLidsSet').returns(new Set());

                this.mMessagesChecked.check(this.message[0], true);
                this.mMessages.insert(this.message[0]);

                expect(this.mMessages.getCount()).to.be.equal(1);

                return Daria.MOPS.remove(this.mMessagesChecked).then(function() {
                    expect(this.mMessages.getCount()).to.be.equal(0);
                }, this);
            });

            it('Если передан параметр message-id, то остается выделенным только это письмо', function() {
                this.message.push(ns.Model.get('message', { ids: '2' }).setData({
                    mid: '2',
                    tid: 't1'
                }));
                this.sinon.stub(this.mMessagesChecked, 'getLidsSet').returns(new Set());

                this.message.forEach(function(message) {
                    this.mMessagesChecked.check(message, true);
                    this.mMessages.insert(message);
                }, this);

                return Daria.MOPS.remove(this.mMessagesChecked, { 'message-id': '2' }).then(function() {
                    expect(ns.forcedRequest).to.have.callCount(1);
                    var call = ns.forcedRequest.getCall(0);

                    expect(Boolean(_.find(call.args[0], { id: 'do-messages' }))).to.be.equal(true);
                    expect(call.args[0][0].params).to.be.eql({
                        movefile: '7',
                        action: 'delete',
                        ids: [ '2' ],
                        tids: [],
                        with_sent: 0
                    });
                });
            });
        });

        describe('проверка параметров запроса do-messages', function() {
            beforeEach(function() {
                this.mMessagesChecked = ns.Model.get('messages-checked');
                this.mMessagesChecked.resetChecked();

                this.message = [];
                this.message.push(ns.Model.get('message', { ids: '1' }).setData({
                    mid: '1',
                    tid: 't1'
                }));

                this.mMessages = ns.Model.get('messages', { thread_id: 't1' });
            });

            it('запроса моделей не происходит в случае, когда валидация параметров не прошла', function() {
                this.sinon.spy(Daria.MOPS, 'invalidateModelsOnMove');
                this.sinon.stub(Daria.MOPS, '_checkDoMessagesParams').returns(false);

                this.mMessagesChecked.check(this.message[0], true);
                this.mMessages.insert(this.message[0]);

                return Daria.MOPS.remove(this.mMessagesChecked).then(
                    function() {
                        throw new Error('действие не должно быть выполнено');
                    },
                    function() {
                        expect(Daria.MOPS.invalidateModelsOnMove).to.have.callCount(0);
                    }
                );
            });
        });
    });

    describe('#invalidateMessagesCheckedModels', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked', {
                current_folder: '1'
            });

            this.resetCheckedStub = this.sinon.stub(this.mMessagesChecked, 'resetChecked');
            this.sinon.stub(this.mMessagesChecked, 'getIds').returns({
                ids: [ '1', '2' ],
                tids: []
            });
        });

        it('Должен вызвать resetChecked, если есть совпадения с переданными письмами', function() {
            Daria.MOPS.invalidateMessagesCheckedModels({ ids: [ '1' ], tids: [] }, 'delete');

            expect(this.resetCheckedStub).to.have.callCount(1);
        });

        it('Не должен вызвать resetChecked, если нет совпадений с переданными письмами', function() {
            Daria.MOPS.invalidateMessagesCheckedModels({ ids: [ '3' ], tids: [] }, 'delete');

            expect(this.resetCheckedStub).to.have.callCount(0);
        });
    });

    describe('.doActionInMessages', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFolders, 'adjustUnreadCounters');
            this.sinon.stub(Daria.MOPS.Opinfo.prototype, 'hasAdjust').returns(true);
        });

        it('Должен вызывать mFolders#adjustUnreadCounters, только если в параметрах передан fid', function() {
            Daria.MOPS.doActionInMessages([ '2190000000624510036' ], 'unmark', { fid: '2' });

            expect(this.mFolders.adjustUnreadCounters).to.have.callCount(1);
        });

        it('Не должен вызывать mFolders#adjustUnreadCounters, если в параметрах не передан fid', function() {
            Daria.MOPS.doActionInMessages([ '2190000000624510036' ], 'label', { lid: '2' });

            expect(this.mFolders.adjustUnreadCounters).to.have.callCount(0);
        });
    });

    describe('#shouldShowConfirmation', function() {
        beforeEach(function() {
            this.mFodler = ns.Model.get('folder', { fid: 1 });
            this.mFolderGetSymbol = this.sinon.stub(this.mFodler, 'get').withArgs('.symbol');
            this.sinon.stub(ns.Model, 'get').withArgs('folder').returns(this.mFodler);
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: 1
            });
        });

        it('возвращает true если пытаемся запинить больше 16 писем', function() {
            expect(Daria.MOPS.shouldShowConfirmation('pin', Daria.Constants.MAX_MESSAGES_TO_PIN)).to.be.equal(true);
        });

        it('возвращает false если пытаемся запинить меньше 15 писем', function() {
            expect(Daria.MOPS.shouldShowConfirmation('pin', Daria.Constants.MAX_MESSAGES_TO_PIN - 1))
                .to.be.equal(false);
        });

        it('возвращает true если пытаемся удалить больше 19 писем', function() {
            expect(Daria.MOPS.shouldShowConfirmation('delete', Daria.Constants.MAX_MESSAGES_TO_REMOVE_OR_SPAM))
                .to.be.equal(true);
        });

        it('возвращает false если пытаемся удалить меньше 20 писем', function() {
            expect(Daria.MOPS.shouldShowConfirmation('delete', Daria.Constants.MAX_MESSAGES_TO_REMOVE_OR_SPAM - 1))
                .to.be.equal(false);
        });

        it('возвращает true если пытаемся отправить в спам больше 19 писем', function() {
            expect(Daria.MOPS.shouldShowConfirmation('tospam', Daria.Constants.MAX_MESSAGES_TO_REMOVE_OR_SPAM))
                .to.be.equal(true);
        });

        it('возвращает false если пытаемся отправить в спам меньше 20 писем', function() {
            expect(Daria.MOPS.shouldShowConfirmation('tospam', Daria.Constants.MAX_MESSAGES_TO_REMOVE_OR_SPAM - 1))
                .to.be.equal(false);
        });

        [ 'notspam', 'unsubscribe', 'mark', 'unmark', 'archive', 'unpin', 'label' ].forEach(function(action) {
            it(
                'возвращает false если MOPS отличается от удаления, переноса в спам или запинивания (' + action + ')',
                function() {
                    expect(Daria.MOPS.shouldShowConfirmation(action, 111)).to.be.equal(false);
                }
            );
        });

        it('возвращает false если пытаемся удали письма в папке "Удаленные"', function() {
            this.mFolderGetSymbol.returns('trash');

            expect(Daria.MOPS.shouldShowConfirmation('delete', 111)).to.be.equal(false);
        });

        it('возвращает false если пытаемся удали письма в папке "Спам"', function() {
            this.mFolderGetSymbol.returns('spam');

            expect(Daria.MOPS.shouldShowConfirmation('delete', 111)).to.be.equal(false);
        });
    });

    describe('#_logMops', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                mid: '1',
                tid: 't1'
            });

            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mMessagesChecked.resetChecked();

            this.mMessagesChecked.check(this.mMessage, true);

            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(ns.page, 'current').value({
                page: 'messages',
                params: {
                    thread_id: 't1'
                }
            });
            this.mDoMessages = ns.Model.get('do-messages', { ids: 't1' }).setData({ taskType: 'async' });

            this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve([ this.mDoMessages ]));
            this.sinon.stub(Daria.messages, 'whereToGoAfterMove');
            this.sinon.stub(Date, 'now').returns(123);
        });

        it('Внутри вызывается Daria.monitoring.journal', function() {
            this.sinon.stub(Daria.monitoring, 'journal');

            var options = {
                current_folder: '3',
                fid: '3',
                originalAction: 'archive',
                noResetChecked: true
            };

            Daria.MOPS._logMops(
                'move',
                'move',
                'start',
                { ids: [ '1' ], tids: [] },
                options
            );

            expect(Daria.monitoring.journal).to.be.calledWith(
                'MOPS',
                {
                    helper: 'move',
                    action: 'move',
                    actionStage: 'start',
                    originalAction: 'archive',
                    fid: '3',
                    ids: '1',
                    tids: '',
                    time: 123,
                    noResetChecked: true,
                    kind: 'folder',
                    target_id: '3',
                    target_type: 'archive',
                    page: 'thread',
                    sources: [ { kind: 'folder', source_id: 'null', source_type: null } ]
                }
            );
        });

        describe('move', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.MOPS, '_logMops');
                return Daria.MOPS.move(this.mMessagesChecked, { fid: '4' });
            });

            it('логируется самое начало выполнения операции', function() {
                expect(Daria.MOPS._logMops.getCall(0).args).to.be.eql([
                    'move',
                    'move',
                    'start',
                    { ids: [ '1' ], tids: [] },
                    { current_folder: '4', fid: '4' }
                ]);
            });

            it('логируется начало выполнения запроса за моделями', function() {
                expect(Daria.MOPS._logMops.getCall(1).args).to.be.eql([
                    'move',
                    'move',
                    'startRequest',
                    { ids: [ '1' ], tids: [] },
                    { current_folder: '4', fid: '4' }
                ]);
            });
        });

        describe('archive', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.MOPS, '_logMops');
                return Daria.MOPS.archive(this.mMessagesChecked);
            });

            it('логируется самое начало выполнения операции архивации', function() {
                expect(Daria.MOPS._logMops.getCall(0).args).to.be.eql([
                    'archive',
                    'archive',
                    'start',
                    { ids: [ '1' ], tids: [] },
                    undefined
                ]);
            });

            it('логируется начало выполнения операции move', function() {
                expect(Daria.MOPS._logMops.getCall(1).args).to.be.eql([
                    'move',
                    'move',
                    'start',
                    { ids: [ '1' ], tids: [] },
                    {
                        current_folder: '3',
                        fid: '3',
                        originalAction: 'archive',
                        noResetChecked: true
                    }
                ]);
            });

            it('логируется начало выполнения запроса за моделями', function() {
                expect(Daria.MOPS._logMops.getCall(2).args).to.be.eql([
                    'move',
                    'move',
                    'startRequest',
                    { ids: [ '1' ], tids: [] },
                    {
                        current_folder: '3',
                        fid: '3',
                        originalAction: 'archive',
                        noResetChecked: true
                    }
                ]);
            });
        });
    });

    describe('#registerOperationFail', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.MOPS._failNotificationManager, 'registerOperationFail');
            this.registerOperationFail = Daria.MOPS.registerOperationFail;
        });

        it('Если ошибка в проверяемой модели, то нужно регистрировать ошибку операции',
            function() {
                this.registerOperationFail(
                    'move', { count: 1, ids: [ 1 ] }, { invalid: [ { id: 'do-messages' } ] }, 'do-messages'
                );

                expect(Daria.MOPS._failNotificationManager.registerOperationFail).has.callCount(1);
                expect(Daria.MOPS._failNotificationManager.registerOperationFail)
                    .to.have.been.calledWith('move', { count: 1, ids: [ 1 ] });
            }
        );

        it('Если ошибка не в проверяемой модели, то не нужно регистрировать ошибку операции', function() {
            this.registerOperationFail(
                'move', { count: 1, ids: [ 1 ] }, { invalid: [ { id: 'folders' } ] }, 'do-messages'
            );

            expect(Daria.MOPS._failNotificationManager.registerOperationFail).has.callCount(0);
        });
    });

    describe('#_checkDoMessagesParams', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.ErrorLog, 'send');
        });

        sit({ action: 'mark' }, false);

        sit({ action: 'mark', ids: null }, false);
        sit({ action: 'mark', ids: undefined }, false);
        sit({ action: 'mark', ids: [] }, false);
        sit({ action: 'mark', ids: [ '123' ] }, true);

        sit({ action: 'mark', tids: null }, false);
        sit({ action: 'mark', tids: undefined }, false);
        sit({ action: 'mark', tids: [] }, false);
        sit({ action: 'mark', tids: [ '123' ] }, true);

        sit({ action: 'mark', fid: null }, false);
        sit({ action: 'mark', fid: undefined }, false);
        sit({ action: 'mark', fid: '' }, false);
        sit({ action: 'mark', fid: '123' }, true);

        sit({ action: 'mark', ids: [ '123' ], fid: null }, true);

        function sit(params, result) {
            it('params=' + JSON.stringify(params) + ' => ' + result, function() {
                expect(Daria.MOPS._checkDoMessagesParams(params)).to.be.equal(result);
            });
        }

        it('должен отправить мониторинг о том, что запрос невозможен', function() {
            var requestParams = { action: 'mark', ids: [], extra: 'ha' };

            Daria.MOPS._checkDoMessagesParams(requestParams);

            expect(Jane.ErrorLog.send).to.have.callCount(1);
            expect(Jane.ErrorLog.send).to.be.calledWithExactly({
                errorType: 'do_messages_not_enough_params_for_request',
                requestParams: JSON.stringify(requestParams)
            });
        });
    });

    describe('markHelper', function() {
        beforeEach(function() {
            this.mMessagesChecked = ns.Model.get('messages-checked');
            this.mMessagesChecked.resetChecked();

            this.message = [];
            this.message.push(ns.Model.get('message', { ids: '1' }).setData({
                mid: '1',
                tid: 't1',
                new: 1
            }));

            this.mMessages = ns.Model.get('messages', { thread_id: 't1' });

            this.scenarioManager = this.sinon.stubScenarioManager(Daria.MOPS);
            this.sinon.stub(Daria.MOPS, '_logFailedMark');
            this.sinon.stub(Daria.MOPS, '_logSuccessfulMark');
        });

        describe('недостаточно параметров для запроса мопсов', function() {
            beforeEach(function() {
                this.mMessagesChecked.check(this.message[0], true);
                this.mMessages.insert(this.message[0]);

                this.sinon.stub(Daria.MOPS, '_undoFailedAction');
                this.sinon.stub(Daria.MOPS, '_checkDoMessagesParams').returns(false);

                this.clock = this.sinon.useFakeTimers();

                this.sinon.stub(ns, 'forcedRequest');
            });

            afterEach(function() {
                this.clock.restore();
            });

            it('действие должно быть отменено', function() {
                return Daria.MOPS.mark(this.mMessagesChecked).then(
                    function() {
                        throw new Error('действие не должно быть выполнено');
                    },
                    () => {
                        expect(ns.forcedRequest).to.have.callCount(0);
                    }
                );
            });

            it('должна быть выполнена отмена изменений в интерфейсе с задержкой', function() {
                expect(Daria.MOPS._undoFailedAction).to.have.callCount(0);

                return Daria.MOPS.mark(this.mMessagesChecked).then(
                    function() {
                        throw new Error('действие не должно быть выполнено');
                    },
                    function() {
                        expect(Daria.MOPS._undoFailedAction).to.have.callCount(0);
                        this.clock.tick(1);
                        expect(Daria.MOPS._undoFailedAction).to.have.callCount(1);
                    },
                    this
                );
            });
        });

        describe('отмена изменений после неуспешного запроса', function() {
            beforeEach(function() {
                this.mMessagesChecked.check(this.message[0], true);
                this.mMessages.insert(this.message[0]);

                this.sinon.stub(Daria.MOPS, '_undoFailedAction');
                this.sinon.stub(Daria.MOPS, 'registerOperationFail');
                this.sinon.stub(Daria.MOPS, '_checkDoMessagesParams').returns(true);

                this.doMessages = {
                    id: 'do-messages',
                    params: {
                        action: 'mark',
                        ids: [ '1' ],
                        tids: []
                    }
                };
            });

            sit('должна быть зарегистрирована неудачная операция', true, function() {
                expect(Daria.MOPS.registerOperationFail).to.have.callCount(1);
            });

            sit('действие должно быть отменено', true, function() {
                expect(Daria.MOPS._undoFailedAction).to.have.callCount(1);
            });

            sit('действие не должно быть отменено', false, function() {
                expect(Daria.MOPS._undoFailedAction).to.have.callCount(0);
            });

            function sit(title, hasModelRequestFailed, checks) {
                it(title, function() {
                    var requestPromise = Vow.reject();
                    this.sinon.stub(ns, 'forcedRequest').returns(requestPromise);
                    this.sinon.stub(Daria.MOPS, '_hasModelRequestFailed').returns(hasModelRequestFailed);

                    Daria.MOPS.mark(this.mMessagesChecked);

                    return requestPromise.then(
                        function() {
                            throw new Error('действие не должно быть выполнено');
                        },
                        checks,
                        this
                    );
                });
            }
        });
    });
});
