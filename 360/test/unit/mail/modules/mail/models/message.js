describe('Daria.mMessage', function() {
    beforeEach(function() {
        this.sinon.stub(window, 'i18n')
            .withArgs('%Без_темы').returns('NO_SUBJECT_LOC');

        this.hMessage = ns.Model.get('message', { ids: '1' });
        this.messageMock = {
            mid: '1'
        };
    });

    afterEach(function() {
        ns.Model.traverse('message', function(model) {
            model.destroy();
        });
    });

    describe('#hasDataChanged', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'forcedRequest');
            this.sinon.stub(this.hMessage, 'set');
            this.sinon.stub(this.hMessage, 'isThread').returns(false);
        });

        it('должен вернуть true, если у модели нет данных', function() {
            expect(this.hMessage.hasDataChanged({})).to.be.equal(true);
        });

        it('должен вернуть true, если модель невалидна', function() {
            this.hMessage.setData({});
            this.hMessage.invalidate();

            expect(this.hMessage.hasDataChanged({})).to.be.equal(true);
        });

        it('должен вернуть true, если модель-фейковый тред', function() {
            var mThread = ns.Model.get('message', { ids: 't1' });

            expect(mThread.hasDataChanged({})).to.be.equal(true);
        });

        describe('Ничего не поменялось', function() {
            beforeEach(function() {
                this.hMessage.setData({
                    count: 1,
                    new: 0
                });

                this.newData = {
                    count: 1,
                    new: 0
                };
            });

            it('должен вернуть false', function() {
                expect(this.hMessage.hasDataChanged(this.newData)).to.be.equal(false);
            });

            it('не должен ничего записать', function() {
                this.hMessage.hasDataChanged(this.newData);

                expect(this.hMessage.set).to.have.callCount(0);
            });
        });

        describe('Проверка одного изменения ->', function() {
            var TEST = [
                {
                    field: 'count',
                    oldData: { count: 1 },
                    newData: { count: 2 }
                },
                {
                    field: 'fid',
                    oldData: { fid: '1' },
                    newData: { fid: '2' }
                },
                {
                    field: 'firstline',
                    oldData: { firstline: '1' },
                    newData: { firstline: '2' }
                },
                {
                    field: 'new',
                    oldData: { new: 0 },
                    newData: { new: 1 }
                },
                {
                    field: 'flags',
                    oldData: { flags: { attachment: true } },
                    newData: { flags: { attachment: true, hotel: true } }
                },
                {
                    field: 'lid',
                    oldData: { lid: [ '1', '2' ] },
                    newData: { lid: [ '2', '3' ] }
                },
                {
                    field: 'field',
                    oldData: {
                        field: [
                            {
                                name: 'mailer-daemon@yandex.ru',
                                email: 'mailer-daemon@yandex.ru',
                                type: 'from',
                                ref: '4007062ea2c9b8bd4d126093a1d87a88',
                                is_service: true,
                                is_free: true
                            },
                            {
                                name: 'fistula01@yandex.by',
                                email: 'fistula01@yandex.by',
                                type: 'to',
                                ref: '680a5bcce71429cb140b7243e9f18e09',
                                is_service: false,
                                is_free: true
                            },
                            {
                                name: 'mailer-daemon@yandex.ru',
                                email: 'mailer-daemon@yandex.ru',
                                type: 'reply-to',
                                ref: '4007062ea2c9b8bd4d126093a1d87a88',
                                is_service: true,
                                is_free: true
                            }
                        ]
                    },
                    newData: {
                        field: [
                            {
                                name: 'mailer-daemon@yandex.ru',
                                email: 'mailer-daemon@yandex.ru',
                                type: 'from',
                                ref: '4007062ea2c9b8bd4d126093a1d87a88',
                                is_service: true,
                                is_free: true
                            },
                            {
                                name: 'fistula01@yandex.by',
                                email: 'fistula01@yandex.by',
                                type: 'to',
                                ref: '680a5bcce71429cb140b7243e9f18e09',
                                is_service: false,
                                is_free: true
                            }
                        ]
                    }
                }
            ];

            TEST.forEach(changeCheck);

            function changeCheck(testData) {
                var field = testData.field;
                var oldData = testData.oldData;
                var newData = testData.newData;

                describe('Реакция на изменение "' + field + '" ->', function() {
                    beforeEach(function() {
                        this.hMessage.setData(oldData);
                    });

                    it('должен вернуть false', function() {
                        expect(this.hMessage.hasDataChanged(newData)).to.be.equal(false);
                    });

                    it('должен записать изменнные данные', function() {
                        this.hMessage.hasDataChanged(newData);

                        expect(this.hMessage.set)
                            .to.have.callCount(1)
                            .to.be.calledWith('.' + field, newData[field]);
                    });
                });
            }
        });

        describe('Проверка нескольких изменений ->', function() {
            beforeEach(function() {
                this.hMessage.setData({
                    count: 1,
                    new: 0
                });

                this.newData = {
                    count: 2,
                    new: 1
                };
            });

            it('должен вернуть false', function() {
                expect(this.hMessage.hasDataChanged(this.newData)).to.be.equal(false);
            });

            it('должен записать изменнные данные (.count)', function() {
                this.hMessage.hasDataChanged(this.newData);

                expect(this.hMessage.set)
                    .to.have.callCount(2)
                    .to.be.calledWith('.count', 2);
            });

            it('должен записать изменнные данные (.new)', function() {
                this.hMessage.hasDataChanged(this.newData);

                expect(this.hMessage.set)
                    .to.have.callCount(2)
                    .to.be.calledWith('.new', 1);
            });
        });

        describe('Перезапрос списка писем треда ->', function() {
            beforeEach(function() {
                this.hMessage.isThread.returns(true);

                this.hMessage.setData({
                    count: 1,
                    new: 0,
                    tid: 't123'
                });

                this.newData = {
                    count: 2,
                    new: 0,
                    tid: 't123'
                };
            });

            it('должен вернуть false', function() {
                expect(this.hMessage.hasDataChanged(this.newData)).to.be.equal(false);
            });

            it('должен записать изменнные данные', function() {
                this.hMessage.hasDataChanged(this.newData);

                expect(this.hMessage.set)
                    .to.have.callCount(1)
                    .to.be.calledWith('.count', 2);
            });

            it('должен запросить список писем треда', function() {
                this.hMessage.hasDataChanged(this.newData);

                expect(ns.forcedRequest)
                    .to.have.callCount(1)
                    .and.to.be.calledWith([ {
                        id: 'messages',
                        params: { thread_id: 't123' }
                    } ]);
            });
        });

        it('Должен обновить last_mid у треда, если он поменялся', function() {
            this.hMessage.setData({
                last_mid: '1'
            });

            this.hMessage.hasDataChanged({
                last_mid: '2'
            });

            expect(this.hMessage.set)
                .to.have.callCount(1)
                .to.be.calledWith('.last_mid', '2');
        });
    });

    describe('#preprocessData', function() {
        it('должен добавить тему, если ее нет', function() {
            var res = this.hMessage.preprocessData({ subject: '' });

            expect(res).to.be.eql({
                subject: 'NO_SUBJECT_LOC'
            });
        });

        it('должен локализовать тему, если она "No subject"', function() {
            var res = this.hMessage.preprocessData({ subject: '' });

            expect(res).to.be.eql({
                subject: 'NO_SUBJECT_LOC'
            });
        });

        it('должен привести данные о виджетах к нужному формату', function() {
            const widgets = [
                {
                    info: {
                        type: 'testType1',
                        showType: 'test1'
                    }
                },
                {
                    info: {
                        type: 'testType2',
                        showType: 'test2'
                    }
                },
                {
                    info: {
                        type: 'testType3',
                        showType: 'test3'
                    }
                },
                {
                    info: {
                        type: 'testType4',
                        showType: 'test2'
                    }
                },
                {
                    info: {
                        type: 'testType5',
                        showType: 'test3'
                    }
                }
            ];

            expect(this.hMessage.preprocessData({ widgets }).widgets).to.eql({
                test1: [ widgets[0] ],
                test2: [ widgets[1], widgets[3] ],
                test3: [ widgets[2], widgets[4] ]
            });
        });

        it('должен удалить информацию о виджетах, если это не массив', function() {
            expect(this.hMessage.preprocessData({ widgets: {} })).to.eql({
                subject: 'NO_SUBJECT_LOC'
            });
        });
    });

    describe('#getMessageThreadMid', function() {
        beforeEach(function() {
            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 1,
                mid: 't1',
                tid: 't1'
            });
        });

        it('должен вернуть mid первого письма в треде, если оно есть', function() {
            ns.Model.get('messages', { thread_id: 't1' }).setData({
                message: [
                    { count: 1, mid: '2', tid: 't1' }
                ]
            });

            expect(this.mThread.getMessageThreadMid()).to.be.equal('2');
        });

        it('должен вернуть сделать mid из tid, если нет писем в треде', function() {
            expect(this.mThread.getMessageThreadMid()).to.be.equal('1');
        });

        it('должен вернуть свой ids, если это не тредизированное письмо', function() {
            this.sinon.stub(this.mThread, 'isMessageThread').returns(false);
            expect(this.mThread.getMessageThreadMid()).to.be.equal('t1');
        });
    });

    describe('#isExternalLike', function() {
        beforeEach(function() {
            this.mFolders = ns.Model.get('folders');
            setModelByMock(this.mFolders);
            //this.inFolderBySymbol = this.sinon.stub(this.hMessage, 'inFolderBySymbol');
        });

        [ 'draft', 'outbox', 'sent', 'template' ].forEach(function(symbol) {
            it('должен возвращать false, если письмо лежит в "' + symbol + '"', function() {
                var fid = this.mFolders.getFidBySymbol(symbol);
                if (!fid) {
                    // eslint-disable-next-line no-throw-literal
                    throw 'Unknown folder "' + symbol + '"';
                }
                this.sinon.stub(this.hMessage, 'getFolderId').returns(fid);

                expect(this.hMessage.isExternalLike()).to.be.equal(false);
            });
        });

        it('должен возвращать true, если письмо лежит в "inbox"', function() {
            var fid = this.mFolders.getFidBySymbol('inbox');
            this.sinon.stub(this.hMessage, 'getFolderId').returns(fid);

            expect(this.hMessage.isExternalLike()).to.be.equal(true);
        });
    });

    describe('#isMessageThread', function() {
        beforeEach(function() {
            this.sinon.stub(this.hMessage, 'isThreadModel').returns(true);
            this.sinon.stub(this.hMessage, 'getThreadCount').returns(2);
        });

        it('должен сказать true, если это тред с одним письмом', function() {
            this.hMessage.isThreadModel.returns(true);
            this.hMessage.getThreadCount.returns(1);

            expect(this.hMessage.isMessageThread()).to.be.equal(true);
        });

        it('должен сказать false, если это тред с несколькими письмами письмом', function() {
            this.hMessage.isThreadModel.returns(true);
            this.hMessage.getThreadCount.returns(2);

            expect(this.hMessage.isMessageThread()).to.be.equal(false);
        });

        it('должен сказать false, если это не тред', function() {
            this.hMessage.isThreadModel.returns(false);
            this.hMessage.getThreadCount.returns(1);

            expect(this.hMessage.isMessageThread()).to.be.equal(false);
        });
    });

    describe('#isThread', function() {
        it('Должен сказать что письмо является тредом', function() {
            var mMessage = ns.Model.get('message', { ids: 't1' });
            mMessage.setData({
                count: 3
            });

            expect(mMessage.isThread()).to.be.equal(true);
        });

        it('Должен сказать что письмо не является тредом', function() {
            var mMessage = ns.Model.get('message', { ids: '1' });
            mMessage.setData({
                count: 1
            });

            expect(mMessage.isThread()).to.be.equal(false);
        });
    });

    describe('#isCouponService', function() {
        beforeEach(function() {
            this.hMessage = ns.Model.get('message', { ids: '1' });
            this.messageMock = {
                mid: '1'
            };
        });

        it('Письмо должно являться купоном, если .type содержат [13, 14]', function() {
            this.messageMock.type = [ 14, 12, 13 ];
            this.hMessage.setData(this.messageMock);

            expect(this.hMessage.isCouponService()).to.be.ok;
        });

        it('Письмо должно являться купоном, если .type совпадает с [13, 14]', function() {
            this.messageMock.type = [ 13, 14 ];
            this.hMessage.setData(this.messageMock);

            expect(this.hMessage.isCouponService()).to.be.ok;
        });

        it('Письмо не должно являться купоном, если .type не содержит с [13, 14]', function() {
            this.messageMock.type = [ 12, 14 ];
            this.hMessage.setData(this.messageMock);

            expect(this.hMessage.isCouponService()).to.not.be.ok;
        });
    });

    describe('#isCalendarService', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' });
            this.messageMock = {
                mid: '1'
            };
        });

        it('Письмо от календаря, если .type содержит 42', function() {
            this.messageMock.type = [ 17, 42 ];
            this.mMessage.setData(this.messageMock);

            expect(this.mMessage.isCalendarService()).to.be.equal(true);
        });

        it('Письмо не от календаря, если .type не содержит 42', function() {
            this.messageMock.type = [ 17 ];
            this.mMessage.setData(this.messageMock);

            expect(this.mMessage.isCalendarService()).to.be.equal(false);
        });
    });

    describe('#getMessageFieldName', function() {
        beforeEach(function() {
            this.mFolders = ns.Model.get('folders');
            this.mMessage = ns.Model.get('message', { ids: '1' });
            this.getField = this.mMessage.getMessageFieldName;
        });

        it('Возвращает поле «кому», если текущая папка «Отправленные»', function() {
            this.sinon.stub(this.mFolders, 'isFolder').returns(true);

            var result = this.getField();

            expect(result).to.be.equal('to');
        });

        it('Возвращает поле «от кого», если текущая папка не «Отправленные»', function() {
            this.sinon.stub(this.mFolders, 'isFolder').returns(false);

            var result = this.getField();

            expect(result).to.be.equal('from');
        });

        it('Возвращает поле "кому", если мы в расширенном поиске ищем по папке "Отправленные"', function() {
            this.sinon.stub(ns.page, 'current').value({
                page: 'messages',
                params: {
                    fid: '2420000220059399891',
                    search: 'search'
                }
            });

            this.sinon.stub(this.mFolders, 'isFolder').returns(true);
            var result = this.getField();
            expect(result).to.be.equal('to');
        });
    });

    describe('#hasRealData', function() {
        it('должен вернуть false для треда без данных', function() {
            var mThread = ns.Model.get('message', { ids: 't7' });
            expect(mThread.hasRealData()).to.be.equal(false);
        });

        it('должен вернуть true для треда с данными', function() {
            var mThread = ns.Model.get('message', { ids: 't7' });
            setModelByMock(mThread);
            expect(mThread.hasRealData()).to.be.equal(true);
        });
    });

    describe('#hasType', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '143' }).setData({ type: [ 10, 15, 20 ] });
        });
        describe('Передан один тип →', function() {
            it('Вернет false, если тип письма не определен', function() {
                // скидываем данные
                this.mMessage.setData({});

                expect(this.mMessage.hasType(14)).to.not.be.ok;
            });

            it('Вернет false, если переданный тип не представлен в типах письма', function() {
                expect(this.mMessage.hasType(14)).to.not.be.ok;
            });

            it('Вернет true, если переданный тип представлен в типах письма', function() {
                expect(this.mMessage.hasType(15)).to.be.ok;
            });
        });

        describe('Передано несколько типов →', function() {
            describe('Второй параметр не передан →', function() {
                it('Вернет true, если весь массив переданных типов представлен в типах письма', function() {
                    expect(this.mMessage.hasType([ 15, 20 ])).to.be.ok;
                });
                it('Вернет false, если хотя бы один элемент из массива переданных типов не представлен в типах письма', function() {
                    expect(this.mMessage.hasType([ 15, 21 ])).to.not.be.ok;
                });
            });

            describe('Второй параметр true →', function() {
                it('Вернет true, если хотя бы один элемент из массива переданных типов представлен в типах письма', function() {
                    expect(this.mMessage.hasType([ 15, 21 ], true)).to.be.ok;
                });
                it('Вернет false, если хотя ни один элемент из массива переданных типов не представлен в типах письма', function() {
                    expect(this.mMessage.hasType([ 16, 21 ], true)).to.not.be.ok;
                });
            });
        });
    });

    describe('#label', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                lid: [ '2' ],
                mid: '1',
                new: 1,
                fid: '2',
                tid: 't1'
            });

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                lid: [ '2' ],
                count: 3,
                mid: 't1',
                new: 2,
                tid: 't1'
            });
        });

        afterEach(function() {
            delete this.mMessage;
            delete this.mThread;
        });

        it('должен пометить письмо меткой', function() {
            this.mMessage.label({ lid: '1' });
            expect(this.mMessage.hasLabel('1')).to.be.equal(true);
        });

        it('не должен ничего делать, если на письме уже есть метка', function() {
            this.sinon.spy(this.mMessage, 'set');
            this.mMessage.label({ lid: '2' });
            expect(this.mMessage.set).to.have.callCount(0);
        });

        describe('Помечаем письмо ->', function() {
            it('должен скорректировать тред, если есть информация о нем', function() {
                this.mMessage.label({ lid: '1' });

                expect(this.mThread.hasLabel('1')).to.be.equal(true);
            });

            it('должен вернуть информацию об операции', function() {
                var opinfo = this.mMessage.label({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: 1
                    },
                    affected: {
                        ids: [ '1' ],
                        tids: []
                    }
                });
            });
        });

        describe('Помечаем тред ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                    message: [
                        { count: '1', fid: 1, lid: [], mid: '10', tid: 't10', new: 1 },
                        { count: '1', fid: 1, lid: [ '1' ], mid: '11', tid: 't11', new: 0 },
                        { count: '1', fid: 2, lid: [], mid: '12', tid: 't12', new: 1 }
                    ]
                });
            });

            it('должен скорректировать список писем в треде', function() {
                this.sinon.spy(this.mMessages, 'label');
                this.mThread.label({ lid: '1' });
                expect(this.mMessages.label).to.be.calledWith({ lid: '1' });
            });

            it('должен вернуть список mid и смещение, если есть информация о списке писем в треде', function() {
                var opinfo = this.mThread.label({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: 2
                    },
                    affected: {
                        ids: [ '10', '12' ],
                        tids: []
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если нет информации о списке писем в треде', function() {
                this.mMessages.clear();
                var opinfo = this.mThread.label({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если количество писем не совпало', function() {
                this.mMessages.remove(0);
                var opinfo = this.mThread.label({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });
        });

        it('должен обновить lid_cnt у треда при клике по треду', function() {
            this.sinon.spy(this.mThread, 'set');

            this.mThread.label({ lid: '1' });

            expect(this.mThread.set)
                .to.be.calledWith('.lid_cnt', { 1: 3 });
        });

        it('должен обновить lid_cnt у треда при клике по письму в треде', function() {
            this.sinon.spy(this.mThread, 'set');

            this.mMessage.label({ lid: '1' });

            expect(this.mThread.set)
                .to.be.calledWith('.lid_cnt', { 1: 1 });
        });
    });

    describe('#mark', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                mid: '1',
                new: 1,
                fid: '2',
                tid: 't1'
            });

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 3,
                mid: 't1',
                new: 2,
                tid: 't1'
            });
        });

        afterEach(function() {
            delete this.mMessage;
        });

        it('должен пометить письмо прочитанным', function() {
            this.mMessage.mark();
            expect(this.mMessage.get('.new')).to.be.equal(0);
        });

        it('не должен ничего делать, если письмо уже прочитано', function() {
            this.mMessage.mark();

            this.sinon.spy(this.mMessage, 'set');
            this.mMessage.mark();
            expect(this.mMessage.set).to.have.callCount(0);
        });

        describe('Помечаем письмо ->', function() {
            it('должен скорректировать тред, если есть информация о нем', function() {
                this.mMessage.mark();

                expect(this.mThread.get('.new')).to.be.equal(1);
            });

            it('должен вернуть информацию об операции', function() {
                var opinfo = this.mMessage.mark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        2: -1
                    },
                    affected: {
                        ids: [ '1' ],
                        tids: []
                    }
                });
            });
        });

        describe('Помечаем тред ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                    message: [
                        { count: '1', fid: 1, mid: '10', tid: 't10', new: 1 },
                        { count: '1', fid: 1, mid: '11', tid: 't11', new: 0 },
                        { count: '1', fid: 2, mid: '12', tid: 't12', new: 1 }
                    ]
                });
            });

            it('должен скорректировать список писем в треде', function() {
                this.sinon.spy(this.mMessages, 'mark');
                this.mThread.mark();
                expect(this.mMessages.mark).to.have.callCount(1);
            });

            it('должен вернуть список mid и смещение, если есть информация о списке писем в треде', function() {
                var opinfo = this.mThread.mark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: -1,
                        2: -1
                    },
                    affected: {
                        ids: [ '10', '12' ],
                        tids: []
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если нет информации о списке писем в треде', function() {
                this.mMessages.destroy();
                var opinfo = this.mThread.mark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если количество помеченным писем не совпало', function() {
                this.mMessages.remove(0);
                var opinfo = this.mThread.mark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });
        });
    });

    describe('#markAnswered', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({ mid: '1' });
            this.mThread = ns.Model.get('message', { ids: 't2' }).setData({ mid: 't2', last_mid: '1' });

            this.sinon.stub(this.mMessage, 'getThreadMessage').returns(this.mThread);
            this.sinon.stub(this.mMessage, 'isThread').returns(false);
            this.sinon.stub(this.mThread, 'isValid').returns(true);
            this.sinon.stub(this.mThread, 'getLastMid').returns('1');
        });

        it('должен пометить письмо отвеченным', function() {
            this.mMessage.markAnswered();

            expect(this.mMessage.get('.flags.replied')).to.equal(true);
        });

        it('должен пометить тред письма отвеченным, если выполняются все условия', function() {
            this.mMessage.markAnswered();

            expect(this.mThread.get('.flags.replied')).to.equal(true);
        });

        describe('не должен помечать тред письма отвеченным', function() {
            it('если текущее письмо тред', function() {
                this.mMessage.isThread.returns(true);
                this.mMessage.markAnswered();

                expect(this.mThread.get('.flags.replied')).to.equal(undefined);
            });

            it('если у письма нет треда', function() {
                this.mMessage.getThreadMessage.returns(null);
                this.mMessage.markAnswered();

                expect(this.mThread.get('.flags.replied')).to.equal(undefined);
            });

            it('если тред не валиден', function() {
                this.mThread.isValid.returns(false);
                this.mMessage.markAnswered();

                expect(this.mThread.get('.flags.replied')).to.equal(undefined);
            });

            it('если текущее письмо не последнее письмо в треде', function() {
                this.mThread.getLastMid.returns(3);
                this.mMessage.markAnswered();

                expect(this.mThread.get('.flags.replied')).to.equal(undefined);
            });
        });
    });

    describe('#markForwarded', function() {
        it('должен поставить флаг ".flags.forwarded", если модель валидна', function() {
            var mMessage = ns.Model.get('message', { ids: '1' }).setData({});
            mMessage.markForwarded();
            expect(mMessage.get('.flags.forwarded')).to.be.equal(true);
        });

        it('не должен поставить флаг ".flags.forwarded", если модель невалидна', function() {
            var mMessage = ns.Model.get('message', { ids: '1' });
            mMessage.markForwarded();
            expect(mMessage.get('.flags.forwarded')).to.be.equal(undefined);
        });
    });

    describe('#move', function() {
        describe('Перенос письма ->', function() {
            beforeEach(function() {
                ns.Model.get('message', { ids: 't11' }).setData({
                    mid: 't11'
                });

                this.mMessages1 = ns.Model.get('messages', { current_folder: '1' }).setData({
                    message: [
                        { mid: '11', fid: '1', tid: 't11', count: 1, date: { ts: 50 } },
                        { mid: '12', fid: '1', tid: 't12', count: 1, date: { ts: 40 } },
                        { mid: '13', fid: '1', tid: 't13', count: 1, date: { ts: 30 } },
                        { mid: '14', fid: '1', tid: 't14', count: 1, date: { ts: 20 } }
                    ]
                });
                this.mMessages2 = ns.Model.get('messages', { current_folder: '2' }).setData({
                    message: [
                        { mid: '21', fid: '2', tid: 't21', count: 1, date: { ts: 51 } },
                        { mid: '22', fid: '2', tid: 't22', count: 1, date: { ts: 41 } },
                        { mid: '23', fid: '2', tid: 't23', count: 1, date: { ts: 31 } },
                        { mid: '24', fid: '2', tid: 't24', count: 1, date: { ts: 21 } }
                    ]
                });

                /** @type Daria.mMessage */
                this.mMessage = ns.Model.get('message', { ids: '11' });
                this.mMessage.move({ fid: '2' });
            });

            it('должен обновить данные в модели письма', function() {
                expect(this.mMessage.getFolderId()).to.be.equal('2');
            });

            it('должен удалить письмо из списка', function() {
                expect(this.mMessages1.models.indexOf(this.mMessage)).to.be.equal(-1);
            });

            it('должен добавить письмо в новый списка', function() {
                expect(this.mMessages2.models.indexOf(this.mMessage)).to.be.above(0);
            });
        });

        describe('Перенос треда ->', function() {
            beforeEach(function() {
                var mFolders = ns.Model.get('folders');
                setModelByMock(mFolders);

                this.mMessages1 = ns.Model.get('messages', { current_folder: '1' }).setData({
                    message: [
                        { mid: 't11', fid: '1', tid: 't11', count: 2, date: { ts: 50 } },
                        { mid: '12', fid: '1', count: 1, date: { ts: 40 } },
                        { mid: '13', fid: '1', count: 1, date: { ts: 30 } },
                        { mid: '14', fid: '1', count: 1, date: { ts: 20 } }
                    ]
                });
                this.mMessagesThread = ns.Model.get('messages', { thread_id: 't11' }).setData({
                    message: [
                        { mid: '111', fid: '1', count: 2, date: { ts: 50 } },
                        { mid: '112', fid: '8', count: 1, date: { ts: 40 } }
                    ]
                });
                this.mMessages2 = ns.Model.get('messages', { current_folder: '2' }).setData({
                    message: [
                        { mid: '21', fid: '2', count: 1, date: { ts: 51 } },
                        { mid: '22', fid: '2', count: 1, date: { ts: 41 } },
                        { mid: '23', fid: '2', count: 1, date: { ts: 31 } },
                        { mid: '24', fid: '2', count: 1, date: { ts: 21 } }
                    ]
                });

                /** @type Daria.mMessage */
                this.mMessage = ns.Model.get('message', { ids: 't11' });
                this.mMessage.move({ fid: '2' });
            });

            it('должен обновить данные в модели письма', function() {
                expect(this.mMessage.getFolderId()).to.be.equal('2');
            });

            it('должен удалить письмо из списка', function() {
                expect(this.mMessages1.models.indexOf(this.mMessage)).to.be.equal(-1);
            });

            it('должен добавить письмо в новый списка', function() {
                expect(this.mMessages2.models.indexOf(this.mMessage)).to.be.above(0);
            });

            it('должен перенести письма в треде', function() {
                expect(this.mMessagesThread.models[0].getFolderId()).to.be.equal('2');
            });

            it('не должен переносить письма из "Отправленных"', function() {
                expect(this.mMessagesThread.models[1].getFolderId()).to.be.equal('8');
            });
        });

        describe('Удаление письма из треда', function() {
            beforeEach(function() {
                setModelByMock(ns.Model.get('folders'));

                this.mMessages = ns.Model.get('messages', { current_folder: '1' }).setData({
                    message: [
                        { mid: '11', fid: '1', tid: 't11', count: 1, date: { ts: 50 } },
                        { mid: '12', fid: '1', tid: 't12', count: 1, date: { ts: 40 } },
                        { mid: 't13', fid: '1', tid: 't13', count: 3, date: { ts: 30 } },
                        { mid: '14', fid: '1', tid: 't14', count: 1, date: { ts: 20 } }
                    ]
                });
                this.mMessagesThread = ns.Model.get('messages', { thread_id: 't13' }).setData({
                    message: [
                        { mid: '21', fid: '1', tid: 't13', count: 1, date: { ts: 51 } },
                        { mid: '22', fid: '1', tid: 't13', count: 1, date: { ts: 41 } },
                        { mid: '23', fid: '1', tid: 't13', count: 1, date: { ts: 31 } }
                    ]
                });

                /** @type Daria.mMessage */
                this.mMessage = ns.Model.get('message', { ids: '22' });
                this.mMessage.move({ fid: '7' });
            });

            it('должен удалить письмо из коллекции треда', function() {
                expect(this.mMessagesThread.models).to.have.length(2);
            });

            it('должен уменьшить количество писем в мета-инфе про тред', function() {
                /** @type Daria.mMessage */
                var mThread = ns.Model.get('message', { ids: 't13' });
                expect(mThread.getThreadCount()).to.be.equal(2);
            });
        });

        describe('Перенос письма из треда с удалением треда из списка писем ->', function() {
            beforeEach(function() {
                setModelByMock(ns.Model.get('folders'));

                this.mMessages1 = ns.Model.get('messages', { current_folder: '1', threaded: 'yes' }).setData({
                    message: [
                        { mid: '11', fid: '1', tid: 't11', count: 1, date: { ts: 50 } },
                        { mid: '12', fid: '1', tid: 't12', count: 1, date: { ts: 40 } },
                        { mid: 't13', fid: '1', tid: 't13', count: 3, date: { ts: 30 } },
                        { mid: '14', fid: '1', tid: 't14', count: 1, date: { ts: 20 } }
                    ]
                });

                this.mMessages9 = ns.Model.get('messages', { current_folder: '9', threaded: 'yes' }).setData({
                    message: [
                        { mid: 't13', fid: '1', tid: 't13', count: 3, date: { ts: 30 } }
                    ]
                });

                this.mMessagesThread = ns.Model.get('messages', { thread_id: 't13' }).setData({
                    details: {
                        'has-more': false
                    },
                    message: [
                        { mid: '21', fid: '1', tid: 't13', count: 1, date: { ts: 51 } },
                        { mid: '22', fid: '9', tid: 't13', count: 1, date: { ts: 41 } }
                    ]
                });

                this.mMessageThread = ns.Model.get('message', { ids: 't13' });

                /** @type Daria.mMessage */
                this.mMessage = ns.Model.get('message', { ids: '21' });
                this.mMessage.move({ fid: '9' });
            });

            it('должен обновить данные в модели письма', function() {
                expect(this.mMessage.getFolderId()).to.be.equal('9');
            });

            it('должен удалить тред из списка', function() {
                expect(this.mMessages1.models.indexOf(this.mMessageThread)).to.be.equal(-1);
            });

            it('должен добавить тред в новый список', function() {
                expect(this.mMessages9.models.indexOf(this.mMessageThread)).to.be.equal(0);
            });

            it('не должен добавить одиночное письмо в новый список', function() {
                expect(this.mMessages9.models.indexOf(this.mMessage)).to.be.equal(-1);
            });
        });

        describe('Удаление последнего письма из запиненного треда ->', function() {
            beforeEach(function() {
                setModelByMock(ns.Model.get('folders'));
                setModelByMock(ns.Model.get('labels'));

                // входящие
                this.mMessages1 = ns.Model.get('messages', { current_folder: '1', threaded: 'yes', with_pins: 'yes' }).setData({
                    message: [
                        { lid: [ '2420000001823639879' ], mid: 't13', fid: '1', tid: 't13', count: 1, date: { ts: 30 } },
                        { mid: '11', fid: '1', tid: 't11', count: 1, date: { ts: 50 } },
                        { mid: '12', fid: '1', tid: 't12', count: 1, date: { ts: 40 } },
                        { mid: '14', fid: '1', tid: 't14', count: 1, date: { ts: 20 } }
                    ]
                });

                // пользовательская папка
                this.mMessages9 = ns.Model.get('messages', { current_folder: '9', threaded: 'yes', with_pins: 'yes' }).setData({
                    message: [
                        { lid: [ '2420000001823639879' ], mid: 't13', fid: '1', tid: 't13', count: 1, date: { ts: 30 } },
                        { mid: '31', fid: '1', tid: 't31', count: 1, date: { ts: 50 } }
                    ]
                });

                this.mMessagesThread = ns.Model.get('messages', { thread_id: 't13' }).setData({
                    details: {
                        'has-more': false
                    },
                    message: [
                        { lid: [ '2420000001823639879' ], mid: '21', fid: '1', tid: 't13', count: 1, date: { ts: 51 } }
                    ]
                });

                this.mMessageThread = ns.Model.get('message', { ids: 't13' });

                /** @type Daria.mMessage */
                this.mMessage = ns.Model.get('message', { ids: '21' });
                this.mMessage.move({ fid: '7' });
            });

            it('должен удалить тред из списка1', function() {
                expect(this.mMessages1.models.indexOf(this.mMessageThread)).to.be.equal(-1);
            });

            it('должен удалить тред из списка2', function() {
                expect(this.mMessages9.models.indexOf(this.mMessageThread)).to.be.equal(-1);
            });
        });
    });

    describe('#moveToDrafts', function() {
        it('должен переложить письмо в папку "Черновики"', function() {
            const fakePromise = {};

            this.sinon.stub(Daria.Xiva, 'moveMessageToFolder').returns(fakePromise);
            this.sinon.stub(ns.Model.get('folders'), 'getFidBySymbol').withArgs('draft').returns('6');

            const mMessage = ns.Model.get('message', { ids: '1' }).setData({});
            const result = mMessage.moveToDrafts();

            expect(result).to.be.equal(fakePromise);
            expect(Daria.Xiva.moveMessageToFolder)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('1', '6');
        });
    });

    describe('#doMove', function() {
        // TODO дописать полноценные тесты.

        // @see DARIA-59117: Сворачиваем список непрочитанных писем после удаления письма
        // @see DARIA-58067: Убирать удалённые из непрочитанных при удалении.
        describe('удаление нового (непрочитанного) письма', function() {
            beforeEach(function() {
                this.setupTestModels = function(options) {
                    options = options || {};

                    this.mid = '160440736725076023';
                    this.tid = 't' + this.mid;
                    this.trashFid = '3';

                    // Готовим список папок.
                    ns.Model.get('folders', {}).setData({
                        folder: [ {
                            symbol: 'inbox',
                            name: 'Входящие',
                            fid: '1',
                            subfolder: []
                        }, {
                            symbol: 'trash',
                            name: 'Удалённые',
                            fid: '3',
                            subfolder: []
                        }, {
                            symbol: 'spam',
                            name: 'Спам',
                            fid: '2',
                            subfolder: []
                        } ]
                    });

                    // Создаём тред во Входящих с одним непрочитанным письмом.
                    this.mMessagesThread = ns.Model.get('messages', {
                        sort_type: 'date',
                        thread_id: this.tid
                    });

                    if (options.isIntoThread) {
                        this.mMessagesThread.setData({
                            details: {
                                'has-more': false
                            },
                            message: [ {
                                mid: this.mid,
                                tid: this.tid,
                                fid: '1',
                                new: 1
                            } ]
                        });

                        // Достаём из треда это письмо - его мы и будем удалять.
                        this.mMessageNew = this.mMessagesThread.models[0];

                        // Создаём руками тредное письмо для этого письма.
                        ns.Model.get('message', { ids: this.mMessageNew.getThreadId() }).setData(this.mMessageNew.getData());
                    } else {
                        // Создаём модель руками.
                        this.mMessageNew = ns.Model.get('message', {
                            ids: this.mid
                        }).setData({
                            mid: this.mid,
                            tid: this.tid,
                            fid: '1',
                            new: 1
                        });
                    }

                    // Проверяем валидность тестового письма.
                    ns.assert(this.mMessageNew.isNew(), '#doMove test', 'should be new');
                    ns.assert((this.mMessageNew.isIntoThread() === options.isIntoThread), '#doMove test', 'check test this.mMessageNew.isIntoThread()');
                    ns.assert(!this.mMessageNew.isThread(), '#doMove test', 'our message is not a thread');
                    ns.assert(!this.mMessageNew.inFolderBySymbol([ 'trash' ]), '#doMove test', 'test message is not in Trash');

                    // Создаём различные списки писем, где это письмо может оказаться и откуда оно должно удаляться.
                    this.mMessagesInbox = ns.Model.get('messages', {
                        threaded: 'yes',
                        current_folder: '1',
                        sort_type: 'date',
                        with_pins: 'yes'
                    }).setData({ message: [] });

                    this.mMessagesInboxOnlyNew = ns.Model.get('messages', {
                        current_folder: '1',
                        sort_type: 'date',
                        extra_cond: 'only_new'
                    }).setData({ message: [] });

                    this.mMessagesUnread = ns.Model.get('messages', {
                        sort_type: 'date',
                        extra_cond: 'only_new',
                        goto: 'all',
                        unread: 'unread'
                    }).setData({ message: [] });

                    this.mMessagesInbox.insert(this.mMessageNew);
                    this.mMessagesInboxOnlyNew.insert(this.mMessageNew);
                    this.mMessagesUnread.insert(this.mMessageNew);
                };
            });

            describe('письмо в треде', function() {
                beforeEach(function() {
                    this.setupTestModels({
                        isIntoThread: true
                    });
                });

                it('удаляется из Входящих', function() {
                    this.mMessageNew.doMove({ current_folder: this.trashFid });
                    expect(this.mMessagesInbox.models.length).to.be.eql(0);
                });

                it('удаляется из Только новые во Входящих', function() {
                    this.mMessageNew.doMove({ current_folder: this.trashFid });
                    expect(this.mMessagesInboxOnlyNew.models.length).to.be.eql(0);
                });

                it('удаляется из Непрочитанных', function() {
                    this.mMessageNew.doMove({ current_folder: this.trashFid });
                    expect(this.mMessagesUnread.models.length).to.be.eql(0);
                });
            });

            describe('письмо не в треде', function() {
                beforeEach(function() {
                    this.setupTestModels({
                        isIntoThread: false
                    });
                });

                it('удаляется из Входящих', function() {
                    this.mMessageNew.doMove({ current_folder: this.trashFid });
                    expect(this.mMessagesInbox.models.length).to.be.eql(0);
                });

                it('удаляется из Только новые во Входящих', function() {
                    this.mMessageNew.doMove({ current_folder: this.trashFid });
                    expect(this.mMessagesInboxOnlyNew.models.length).to.be.eql(0);
                });

                it('удаляется из Непрочитанных', function() {
                    this.mMessageNew.doMove({ current_folder: this.trashFid });
                    expect(this.mMessagesUnread.models.length).to.be.eql(0);
                });
            });
        });

        // @see DARIA-61418
        describe('удаление из треда письма, которое есть в нескольких списках писем', function() {
            it('количество писем в треде уменьшается на 1', function() {
                setupDataBeforeRemove.call(this);

                expect(this.mThreadMessage.get('.count')).to.be.equal(3);

                this.mMessage.doMove({ current_folder: this.trashFid });

                expect(this.mThreadMessage.get('.count')).to.be.equal(2);
            });

            function setupDataBeforeRemove() {
                var m1Data = {
                    count: 1,
                    fid: '1',
                    mid: '164944336352444704',
                    tid: 't164944336352444700'
                };
                var m2Data = {
                    count: 1,
                    fid: '1',
                    mid: '164944336352444703',
                    tid: 't164944336352444700'
                };
                var m3Data = {
                    count: 1,
                    fid: '1',
                    mid: '164944336352444700',
                    tid: 't164944336352444700'
                };

                // Готовим список папок.
                ns.Model.get('folders', {}).setData({
                    folder: [ {
                        symbol: 'inbox',
                        name: 'Входящие',
                        fid: '1',
                        subfolder: []
                    }, {
                        symbol: 'trash',
                        name: 'Удалённые',
                        fid: '3',
                        subfolder: []
                    }, {
                        symbol: 'spam',
                        name: 'Спам',
                        fid: '2',
                        subfolder: []
                    } ]
                });

                this.trashFid = '3';

                ns.Model.get('messages', {
                    thread_id: 't164944336352444700',
                    sort_type: 'date'
                }).setData({
                    details: {
                        'has-more': false
                    },
                    message: [
                        m1Data,
                        m2Data,
                        m3Data
                    ]
                });

                ns.Model.get('messages', {
                    thread_id: 't164944336352444700',
                    sort_type: 'date',
                    prevent_update: true
                }).setData({
                    details: {
                        'has-more': false
                    },
                    message: [
                        m1Data,
                        m2Data,
                        m3Data
                    ]
                });

                this.mMessage = ns.Model.get('message', { ids: '164944336352444704' });
                this.mThreadMessage = ns.Model.get('message', { ids: 't164944336352444700' }).setData({
                    count: 3,
                    fid: '1',
                    mid: 't164944336352444700',
                    tid: 't164944336352444700'
                });
            }
        });
    });

    describe('#remove', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.get('message', { ids: '1' }), 'destroy');
            this.sinon.stub(ns.Model.get('message-body', { ids: '1' }), 'destroy');
            this.sinon.stub(ns.Model.get('messages'), 'remove');

            ns.Model.get('messages').setData({ message: [] });
        });

        it('должен удалить письмо из всех списков', function() {
            ns.Model.get('message', { ids: '1' }).remove();

            expect(ns.Model.get('messages').remove).to.have.callCount(1);
        });

        it('должен уничтожить тело письма', function() {
            ns.Model.get('message', { ids: '1' }).remove();

            expect(ns.Model.get('message-body', { ids: '1' }).destroy).to.have.callCount(1);
        });

        it('должен уничтожить саму модель', function() {
            ns.Model.get('message', { ids: '1' }).remove();

            expect(ns.Model.get('message', { ids: '1' }).destroy).to.have.callCount(1);
        });
    });

    describe('#unlabel', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                lid: [ '1', '2' ],
                new: 0,
                fid: '2',
                mid: '2',
                tid: 't1'
            });

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 3,
                fid: '2',
                lid: [ '1', '2' ],
                mid: 't1',
                new: 2,
                tid: 't1'
            });
        });

        afterEach(function() {
            delete this.mMessage;
        });

        it('должен убрать метку с письма', function() {
            this.mMessage.unlabel({ lid: '1' });
            expect(this.mMessage.hasLabel('1')).to.be.equal(false);
        });

        it('не должен ничего делать, если на письме нет метки', function() {
            this.sinon.spy(this.mMessage, 'set');
            this.mMessage.unlabel({ lid: '3' });

            expect(this.mMessage.set).to.have.callCount(0);
        });

        describe('Помечаем письмо ->', function() {
            it('должен скорректировать тред, если есть информация о нем', function() {
                this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                    message: [
                        { count: '1', fid: 1, lid: [], mid: '10', tid: 't10', new: 1 },
                        { count: '1', fid: 1, lid: [], mid: '11', tid: 't11', new: 0 },
                        { count: '1', fid: 2, lid: [], mid: '12', tid: 't12', new: 1 }
                    ]
                });
                this.mMessage.unlabel({ lid: '1' });

                expect(this.mThread.hasLabel('1')).to.be.equal(false);
            });

            it('должен вернуть информацию об операции', function() {
                var opinfo = this.mMessage.unlabel({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: -1
                    },
                    affected: {
                        ids: [ '2' ],
                        tids: []
                    }
                });
            });
        });

        describe('Помечаем тред ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                    message: [
                        { count: '1', fid: 1, lid: [ '1' ], mid: '10', tid: 't10', new: 1 },
                        { count: '1', fid: 1, lid: [], mid: '11', tid: 't11', new: 0 },
                        { count: '1', fid: 2, lid: [ '1' ], mid: '12', tid: 't12', new: 1 }
                    ]
                });
            });

            it('должен скорректировать список писем в треде', function() {
                this.sinon.spy(this.mMessages, 'unlabel');
                this.mThread.unlabel({ lid: '1' });
                expect(this.mMessages.unlabel).to.be.calledWith({ lid: '1' });
            });

            it('должен вернуть список mid и смещение, если есть информация о списке писем в треде', function() {
                var opinfo = this.mThread.unlabel({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: -2
                    },
                    affected: {
                        ids: [ '10', '12' ],
                        tids: []
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если нет информации о списке писем в треде', function() {
                this.mMessages.clear();
                var opinfo = this.mThread.unlabel({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если количество помеченным писем не совпало', function() {
                this.mMessages.remove(1);
                var opinfo = this.mThread.unlabel({ lid: '1' });
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });
        });

        it('должен обновить lid_cnt у треда при клике по треду', function() {
            this.mThread.set('.lid_cnt', { 1: 1 });
            this.sinon.spy(this.mThread, 'set');

            this.mThread.unlabel({ lid: '1' });

            expect(this.mThread.set)
                .to.be.calledWith('.lid_cnt', { 1: 0 });
        });

        it('должен обновить lid_cnt у треда при клике по письму в треде', function() {
            this.mThread.set('.lid_cnt', { 1: 1 });
            this.sinon.spy(this.mThread, 'set');

            this.mMessage.unlabel({ lid: '1' });

            expect(this.mThread.set)
                .to.be.calledWith('.lid_cnt', { 1: 0 });
        });
    });

    describe('#unmark', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                count: 1,
                new: 0,
                fid: '2',
                mid: '2',
                tid: 't1'
            });

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 3,
                fid: '2',
                mid: 't1',
                new: 2,
                tid: 't1'
            });
        });

        afterEach(function() {
            delete this.mMessage;
        });

        it('должен пометить письмо непрочитанным', function() {
            this.mMessage.unmark();
            expect(this.mMessage.get('.new')).to.be.equal(1);
        });

        it('не должен ничего делать, если письмо уже непрочитано', function() {
            this.mMessage.unmark();

            this.sinon.spy(this.mMessage, 'set');
            this.mMessage.unmark();
            expect(this.mMessage.set).to.have.callCount(0);
        });

        describe('Помечаем письмо ->', function() {
            it('должен скорректировать тред, если есть информация о нем', function() {
                this.mMessage.unmark();

                expect(this.mThread.get('.new')).to.be.equal(3);
            });

            it('должен вернуть информацию об операции', function() {
                var opinfo = this.mMessage.unmark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        2: 1
                    },
                    affected: {
                        ids: [ '2' ],
                        tids: []
                    }
                });
            });
        });

        describe('Помечаем тред ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                    message: [
                        { count: '1', fid: 1, mid: '10', tid: 't10', new: 1 },
                        { count: '1', fid: 1, mid: '11', tid: 't11', new: 0 },
                        { count: '1', fid: 2, mid: '12', tid: 't12', new: 1 }
                    ]
                });
            });

            it('должен скорректировать список писем в треде', function() {
                this.sinon.spy(this.mMessages, 'unmark');
                this.mThread.unmark();
                expect(this.mMessages.unmark).to.have.callCount(1);
            });

            it('должен вернуть список mid и смещение, если есть информация о списке писем в треде', function() {
                var opinfo = this.mThread.unmark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: 1
                    },
                    affected: {
                        ids: [ '11' ],
                        tids: []
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если нет информации о списке писем в треде', function() {
                this.mMessages.destroy();
                var opinfo = this.mThread.unmark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });

            it('должен вернуть tid и сброшенное смещение, если количество помеченным писем не совпало', function() {
                this.mMessages.remove(1);
                var opinfo = this.mThread.unmark();
                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: [ 't1' ]
                    }
                });
            });
        });
    });

    describe('#updateThreadInfo', function() {
        it('не должен ничего делать, если у треда нет данных', function() {
            var mMessage = ns.Model.get('message', { ids: '3' }).setData({
                count: 1,
                lid: [ '2' ],
                mid: '3',
                new: 0,
                tid: 't1'
            });

            var mThread = ns.Model.get('message', { ids: 't1' });
            this.sinon.stub(mThread, 'set');

            mThread.updateThreadInfo(mMessage);
            expect(mThread.set).to.have.callCount(0);
        });

        it('должен добавить в тред метки из письма', function() {
            /** @type Daria.mMessage */
            var mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 2,
                lid: [ '1' ],
                mid: 't1',
                new: 0,
                tid: 't1'
            });

            /** @type Daria.mMessage */
            var mMessage = ns.Model.get('message', { ids: '3' }).setData({
                count: 1,
                lid: [ '2' ],
                mid: '3',
                new: 0,
                tid: 't1'
            });

            mThread.updateThreadInfo(mMessage);

            expect(mThread.get('.lid')).to.be.eql([ '1', '2' ]);
        });

        describe('Логика обновления контекстной информации ->', function() {
            beforeEach(function() {
                this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                    lid: []
                });
                this.sinon.stub(this.mThread, 'updateThreadContextInfo');

                this.mMessage = ns.Model.get('message', { ids: '5' });
                setModelByMock(this.mMessage);

                this.mFolders = ns.Model.get('folders');
                setModelByMock(this.mFolders);
            });

            [ 'draft', 'outbox', 'sent', 'template' ].forEach(function(symbol) {
                it('не должен обновлять, если новое письмо в папке "' + symbol + '"', function() {
                    var fid = this.mFolders.getFidBySymbol(symbol);
                    this.mMessage.set('.fid', fid);

                    this.mThread.updateThreadInfo(this.mMessage);

                    expect(this.mThread.updateThreadContextInfo).to.have.callCount(0);
                });
            });

            it('должен обновлять, если новое письмо в нормальной папке', function() {
                var fid = this.mFolders.getFidBySymbol('inbox');
                this.mMessage.set('.fid', fid);

                this.mThread.updateThreadInfo(this.mMessage);

                expect(this.mThread.updateThreadContextInfo).to.have.callCount(1);
            });
        });

        describe('поведение пометки треда отвеченным', function() {
            beforeEach(function() {
                this.mTestThread = ns.Model.get('message', { ids: 't12345' }).setData({ lid: [ '1', '2', '3' ] });
                this.mTestMessage = ns.Model.get('message', { ids: '678910' }).setData({ lid: [ '4', '5', '6' ] });

                this.sinon.stub(this.mTestThread, 'isAnswered').returns(true);
                this.sinon.stub(this.mTestMessage, 'isReply').returns(false);
                this.sinon.stub(this.mTestMessage, 'isOutcomeMessage').returns(true);
                this.sinon.spy(this.mTestThread, 'unsetAnswered');
            });

            describe('должен пометить тред не отвеченным', function() {
                it('если пришло письмо от текущего пользователя, не являющееся ответом', function() {
                    this.mTestThread.updateThreadInfo(this.mTestMessage);
                    expect(this.mTestThread.get('.flags.replied')).to.equal(false);
                });

                it('если пришло письмо - ответ не от текущего юзера', function() {
                    this.mTestMessage.isReply.returns(true);
                    this.mTestMessage.isOutcomeMessage.returns(false);

                    this.mTestThread.updateThreadInfo(this.mTestMessage);
                    expect(this.mTestThread.get('.flags.replied')).to.equal(false);
                });
            });

            describe('не должен помечать тред не отвеченным', function() {
                it('если текущий тред не помечен отвеченным', function() {
                    this.mTestThread.isAnswered.returns(false);
                    this.mTestMessage.isReply.returns(true);

                    this.mTestThread.updateThreadInfo(this.mTestMessage);
                    expect(this.mTestThread.unsetAnswered).have.callCount(0);
                });

                it('если письмо - ответ и пришло от текущего юзера', function() {
                    this.mTestMessage.isReply.returns(true);

                    this.mTestThread.updateThreadInfo(this.mTestMessage);
                    expect(this.mTestThread.unsetAnswered).have.callCount(0);
                });
            });
        });

        describe('приходит новое письмо - обновляется правильная модель Daria.mMessagePresentation', function() {
            beforeEach(function() {
                this.mThread = ns.Model.get('message', { ids: 't159033361841522075' })
                    .setData({
                        mid: 't159033361841522075',
                        fid: '1',
                        firstline: '3 the new one',
                        flags: {
                            replied: true,
                            human: true,
                            attachment: false
                        },
                        lid: [ '50', '2', 'FAKE_RECENT_LBL', 'FAKE_SEEN_LBL' ],
                        field: [ {
                            name: 'Roman Kartsev',
                            email: 'chestozo@gmail.com',
                            type: 'from'
                        }, {
                            name: 'Карцев Роман',
                            email: 'chestozo@yandex.ru',
                            type: 'to'
                        } ]
                    });

                this.newInboxMessage = ns.Model.get('message', { ids: '159033361841522137' })
                    .setData({
                        mid: '159033361841522137',
                        fid: '1',
                        firstline: '3 the new one',
                        flags: {
                            human: true
                        },
                        field: [ {
                            name: 'Roman Kartsev',
                            email: 'chestozo@gmail.com',
                            type: 'from'
                        }, {
                            name: 'Карцев Роман',
                            email: 'chestozo@yandex.ru',
                            type: 'to'
                        } ]
                    });

                this.newSentMessage = ns.Model.get('message', { ids: '159033361841522138' })
                    .setData({
                        mid: '159033361841522138',
                        fid: '4',
                        firstline: '4 say hello...',
                        flags: {
                            human: true
                        },
                        field: [ {
                            name: 'Карцев Роман',
                            email: 'chestozo@yandex.ru',
                            type: 'from'
                        }, {
                            name: 'Roman Kartsev',
                            email: 'chestozo@gmail.com',
                            type: 'to'
                        } ]
                    });

                this.mMP4Inbox = ns.Model.get('message-presentation', _.extend({}, this.mThread.params, { fid: '1' }));
                this.mMP4Sent = ns.Model.get('message-presentation', _.extend({}, this.mThread.params, { fid: '4' }));

                this.sinon.spy(this.mMP4Inbox, 'fill');
                this.sinon.spy(this.mMP4Sent, 'fill');
            });

            it('приходит новое письмо в тред -> обновляется Daria.mMessagePresentation для Входящих', function() {
                this.mThread.updateThreadInfo(this.newInboxMessage);
                expect(this.mMP4Inbox.fill).to.have.callCount(1);
            });

            it('приходит новое письмо в тред -> НЕ обновляется Daria.mMessagePresentation для Отправленных', function() {
                this.mThread.updateThreadInfo(this.newInboxMessage);
                expect(this.mMP4Sent.fill).to.have.callCount(0);
            });

            it('приходит новое отправленное письмо -> обновляется Daria.mMessagePresentation для Отправленных', function() {
                this.mThread.updateThreadInfo(this.newSentMessage);
                expect(this.mMP4Sent.fill).to.have.callCount(1);
            });

            it('приходит новое отправленное письмо -> НЕ обновляется Daria.mMessagePresentation для Входящих', function() {
                this.mThread.updateThreadInfo(this.newSentMessage);
                expect(this.mMP4Inbox.fill).to.have.callCount(0);
            });
        });
    });

    describe('#updateThreadContextInfo', function() {
        beforeEach(function() {
            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                flags: {
                    attachment: true
                }
            });

            this.mMessage = ns.Model.get('message', { ids: '5' }).setData({
                date: {
                    iso: '123'
                },
                fid: 'new fid',
                field: [
                    { name: 'name', email: 'email' }
                ],
                firstline: 'new firstline',
                mid: '4',
                subject: 'new subject'
            });
        });

        [ 'data', 'fid', 'field', 'firstline', 'subject' ].forEach(function(field) {
            it('должен обновить поле "' + field + '"', function() {
                this.mThread.updateThreadContextInfo(this.mMessage);

                expect(this.mThread.get('.' + field)).to.be.equal(this.mMessage.get('.' + field));
            });
        });

        it('должен обновить last_mid', function() {
            this.mThread.updateThreadContextInfo(this.mMessage);

            expect(this.mThread.get('.last_mid')).to.be.equal('4');
        });

        it('должен обновить .flags.attachment', function() {
            // у этого письма нет аттачей,
            // поэтому у треда должен пропасть флаг
            this.mThread.updateThreadContextInfo(this.mMessage);

            expect(this.mThread.hasAttachment()).to.be.equal(false);
        });
    });

    describe('#isPinned', function() {
        beforeEach(function() {
            this.mMessage_1 = ns.Model.get('message', { ids: '1' }).setData({
                lid: [ '2', '3', '112233' ],
                mid: '1',
                new: 1,
                fid: '2',
                tid: 't1'
            });

            this.mMessage_2 = ns.Model.get('message', { ids: '2' }).setData({
                lid: [ '2', '3' ],
                mid: '1',
                new: 1,
                fid: '2',
                tid: 't1'
            });

            this.sinon.stub(ns.Model, 'get').withArgs('labels').returns({
                getPinnedLabel: function() {
                    return {
                        lid: '112233'
                    };
                }
            });
        });

        it('Если на письме есть метка "запиненные", то должен вернуть true', function() {
            expect(this.mMessage_1.isPinned()).to.be.equal(true);
        });

        it('Если на письме нет метки "запиненные", то должен вернуть false', function() {
            expect(this.mMessage_2.isPinned()).to.be.equal(false);
        });
    });

    describe('#isValidForPin', function() {
        beforeEach(function() {
            this.folders = ns.Model.get('folders').setData({
                folder: [
                    { fid: '112233', symbol: 'spam', subfolder: [] },
                    { fid: '445566', symbol: 'trash', subfolder: [] },
                    { fid: '778899', symbol: 'inbox', subfolder: [] }
                ]
            });
            this.sinon.stub(this.hMessage, 'getFolderId');
        });

        it('Должен вернуть false для папки spam', function() {
            this.hMessage.getFolderId.returns('112233');
            expect(this.hMessage.isValidForPin()).to.be.equal(false);
        });

        it('Должен вернуть false для папки trash', function() {
            this.hMessage.getFolderId.returns('445566');
            expect(this.hMessage.isValidForPin()).to.be.equal(false);
        });

        it('Должен вернуть true для папки inbox', function() {
            this.hMessage.getFolderId.returns('778899');
            expect(this.hMessage.isValidForPin()).to.be.equal(true);
        });
    });

    describe('#isDraftLike', function() {
        beforeEach(function() {
            this.isThread = this.sinon.stub(this.hMessage, 'isThread');
            this.inFolderBySymbol = this.sinon.stub(this.hMessage, 'inFolderBySymbol');
        });

        it('должен возвращать true, если письмо лежит в черновиках', function() {
            this.isThread.returns(false);
            this.inFolderBySymbol.returns(true);
            expect(this.hMessage.isDraftLike()).to.be.equal(true);
        });

        it('должен возвращать false, если письмо не лежит в черновиках', function() {
            this.isThread.returns(false);
            this.inFolderBySymbol.returns(false);
            expect(this.hMessage.isDraftLike()).to.be.equal(false);
        });

        it('должен возвращать false, если письмо лежит в черновиках, но является тредом', function() {
            this.isThread.returns(true);
            this.inFolderBySymbol.returns(true);
            expect(this.hMessage.isDraftLike()).to.be.equal(false);
        });
    });

    describe('#hasLabelInDiff', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '1' });
        });

        it('Должен вернуть false, если лейбел "3" есть в текущем .lid и в предыдущем', function() {
            this.mMessage.setData({
                lid: [ 1, 2, 3 ]
            });

            expect(this.mMessage.hasLabelInDiff(3, [ 1, 2, 3, 4 ])).to.not.be.ok;
        });

        it('Должен вернуть false, если лейбел "3" отсутствует в текущем .lid и в предыдущем', function() {
            this.mMessage.setData({
                lid: [ 1, 2 ]
            });

            expect(this.mMessage.hasLabelInDiff(3, [ 1, 2, 4 ])).to.not.be.ok;
        });

        it('Должен вернуть true, если лейбел "3" есть в текущем .lid', function() {
            this.mMessage.setData({
                lid: [ 1, 2, 3 ]
            });

            expect(this.mMessage.hasLabelInDiff(3, [ 1, 2 ])).to.be.ok;
        });

        it('Должен вернуть true, если лейбел "3" есть в предыдущем .lid', function() {
            this.mMessage.setData({
                lid: [ 1, 2 ]
            });

            expect(this.mMessage.hasLabelInDiff(3, [ 1, 2, 3 ])).to.be.ok;
        });
    });

    describe('#isTemplate', function() {
        beforeEach(function() {
            this.isThread = this.sinon.stub(this.hMessage, 'isThread');
            this.inFolderBySymbol = this.sinon.stub(this.hMessage, 'inFolderBySymbol');
        });

        it('Должен возвращать true, если письмо лежит в шаблонах', function() {
            this.isThread.returns(false);
            this.inFolderBySymbol.returns(true);

            expect(this.hMessage.isTemplate()).to.be.ok;
        });

        it('Должен возвращать false, если письмо не лежит в шаблонах', function() {
            this.isThread.returns(false);
            this.inFolderBySymbol.returns(false);

            expect(this.hMessage.isTemplate()).to.not.be.ok;
        });

        it('Должен возвращать false, если письмо лежит в шаблонах, но является тредом', function() {
            this.isThread.returns(true);
            this.inFolderBySymbol.returns(true);

            expect(this.hMessage.isTemplate()).to.not.be.ok;
        });
    });

    describe('#isRecipient ->', function() {
        it('Должен возвращать true, если я есть в получателях', function() {
            var accountInformation = ns.Model.get('account-information');
            this.sinon.stub(accountInformation, 'getAllUserEmails').returns([
                'ekhurtina@yandex-team.ru',
                'ekhurtina@yandex-team.com',
                'ekhurtina@yandex-team.com.tr',
                'ekhurtina@yandex-team.com.ua'
            ]);

            var getFieldsByType = this.sinon.stub(this.hMessage, 'getFieldsByType');
            getFieldsByType.withArgs('to').returns([
                { email: 'ekhurtina@yandex-team.ru' },
                { email: 'mail-test@yandex-team.ru' }
            ]);
            getFieldsByType.withArgs('cc').returns([
                { email: 'test@yandex.ru' }
            ]);
            getFieldsByType.withArgs('bcc').returns([]);
            expect(this.hMessage.isRecipient()).to.be.ok;
        });
        it('Должен возвращать false, если меня нет в получателях', function() {
            var accountInformation = ns.Model.get('account-information');
            this.sinon.stub(accountInformation, 'getAllUserEmails').returns([
                'ekhurtina@yandex-team.ru',
                'ekhurtina@yandex-team.com',
                'ekhurtina@yandex-team.com.tr',
                'ekhurtina@yandex-team.com.ua'
            ]);

            var getFieldsByType = this.sinon.stub(this.hMessage, 'getFieldsByType');
            getFieldsByType.withArgs('to').returns([
                { email: 'test1@ya.ru' },
                { email: 'mail-test@yandex-team.ru' }
            ]);
            getFieldsByType.withArgs('cc').returns([
                { email: 'test@yandex.ru' }
            ]);
            getFieldsByType.withArgs('bcc').returns([]);
            expect(this.hMessage.isRecipient()).to.not.be.ok;
        });
    });

    describe('#toDelete', function() {
        it('Должен вернуть true, если письмо находится в удаленных', function() {
            this.sinon.stub(this.hMessage, 'inFolderBySymbol').withArgs('trash').returns(true);
            expect(this.hMessage.toDelete()).to.be.ok;
        });

        it('Должен вернуть true, если письмо не в папке удаленных, но перемещение возможно', function() {
            this.sinon.stub(this.hMessage, 'inFolderBySymbol').withArgs('trash').returns(false);
            this.sinon.stub(this.hMessage, 'toMove').returns(true);
            expect(this.hMessage.toDelete()).to.be.ok;
        });
    });

    describe('#isDelayed', function() {
        beforeEach(function() {
            this.now = Date.now();
            this.stubNow = this.sinon.stub(Daria, 'now').returns(this.now);
            this.stubInFolderBySymbol = this.sinon.stub(this.hMessage, 'inFolderBySymbol').withArgs('outbox').returns(true);
            this.stubGetDelayedMessageLabel = this.sinon.stub(ns.Model.get('labels'), 'getDelayedMessageLabel').returns({ lid: 'test' });
            this.stubHasLabel = this.sinon.stub(this.hMessage, 'hasLabel').withArgs('test').returns(true);
            this.stubGetDateTs = this.sinon.stub(this.hMessage, 'get').withArgs('.date.ts').returns(this.now + 1);
        });

        it('Письмо с отложенной отправкой должно находиться в папке "Исходящие", содержать метку отложенной отправкии иметь время создания больше текущего', function() {
            expect(this.hMessage.isDelayed()).to.be.equal(true);
        });

        it('Если письмо не в папке "Исходящие", то оно не будет отправлено отложенно', function() {
            this.stubInFolderBySymbol.returns(false);
            expect(this.hMessage.isDelayed()).to.be.equal(false);
        });

        it('Если метка отложенной отправки не определена, письмо не имеет отложенной отправки', function() {
            this.stubGetDelayedMessageLabel.returns(null);
            expect(this.hMessage.isDelayed()).to.be.equal(false);
        });

        it('Если метка отложенной отправки не найдена в письме, письмо не имеет отложенной отправки', function() {
            this.stubHasLabel.returns(false);
            expect(this.hMessage.isDelayed()).to.be.equal(false);
        });

        it('Если время создания письма меньше текущего, то письмо либо не имеет отложенной отправки, либо уже отправлено', function() {
            this.stubGetDateTs.returns(this.now - 1);
            expect(this.hMessage.isDelayed()).to.be.equal(false);
        });
    });

    describe('#getDelayedTime', function() {
        it('Должен вернуть undefined, если письмо без отложенной отправки', function() {
            this.sinon.stub(this.hMessage, 'isDelayed').returns(false);
            expect(this.hMessage.getDelayedTime()).to.be.equal(undefined);
        });

        it('Должен вернуть число из поля .date.ts, если письмо с отоженной отправкой', function() {
            this.sinon.stub(this.hMessage, 'isDelayed').returns(true);
            this.sinon.stub(this.hMessage, 'get')
                .withArgs('.date.ts').returns(1)
                .withArgs('.date.timestamp').returns(2);
            expect(this.hMessage.getDelayedTime()).to.equal(1);
        });

        it('Должен вернуть число из поля .date.timestamp, если письмо с отоженной отправкой', function() {
            this.sinon.stub(this.hMessage, 'isDelayed').returns(true);
            this.sinon.stub(this.hMessage, 'get').withArgs('.date.timestamp').returns(2);
            expect(this.hMessage.getDelayedTime()).to.equal(2);
        });
    });
});
