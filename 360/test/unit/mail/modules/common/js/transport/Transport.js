describe('Daria.Transport', function() {
    beforeEach(function() {
        this.stubDebounce();
        this.sinon.useFakeTimers();

        this.transport = new Daria.Transport('');
        this.socket = {
            sendConfirm: this.sinon.spy(),
            updatePingEventTimeout: this.sinon.spy()
        };

        // некрасиво, но стабить весь Daria.Transport.Backend неохото
        this.transport._socket = this.socket;

        this.sinon.stub(Jane.ErrorLog, 'send');
    });

    afterEach(function() {
        this.sinon.clock.restore();
    });

    describe('#enqueue', function() {
        beforeEach(function() {
            this.sinon.spy(vow.Queue.prototype, 'stop');
            this.sinon.spy(vow.Queue.prototype, 'start');
            this.sinon.spy(vow.Queue.prototype, 'enqueue');

            this.sinon.stub(Daria.Xiva.handlers, 'insert').returns(vow.resolve());
            this.sinon.stub(Daria.Xiva.handlers, 'unsupported').returns(vow.resolve());

            this.transport.enqueue({
                lcn: 2,
                operation: 'insert'
            });
            this.transport.enqueue({
                lcn: 1,
                operation: 'unsupported'
            });
        });

        it('должен остановить очередь', function() {
            this.sinon.clock.tick(200);
            expect(vow.Queue.prototype.stop).to.have.callCount(1);
        });

        it('должен сгруппировать и добавить сообщения пачкой для правильной сортировки', function() {
            this.sinon.clock.tick(200);
            expect(vow.Queue.prototype.enqueue).to.have.callCount(2);
        });

        it('должен запустить очередь', function() {
            this.sinon.clock.tick(200);
            expect(vow.Queue.prototype.start).to.have.callCount(1);
        });

        it('должен очистить очередь на добавление', function() {
            this.sinon.clock.tick(200);
            expect(this.transport._msgQueue).to.be.eql([]);
        });

        it('должен вызывать сообщение с lcn=1 перед lcn=2', function(done) {
            this.sinon.clock.tick(1000);
            _.delay(function() {
                expect(Daria.Xiva.handlers.unsupported).to.be.calledBefore(Daria.Xiva.handlers.insert);
                done();
            }, 200);
        });
    });

    describe('#onmessage', function() {
        beforeEach(function() {
            this.sinon.stub(this.transport, 'processMessage');
        });

        describe('Невалидный JSON ->', function() {
            beforeEach(function() {
                this.res = this.transport.onmessage('{a');
            });

            it('должен вызывать #sendConfirm у сокета', function() {
                expect(this.socket.sendConfirm).to.be.calledWithExactly();
            });

            it('должен вызывать Jane.ErrorLog.send', function() {
                expect(Jane.ErrorLog.send).to.calledWith({
                    errorType: 'xiva.json_parse'
                }, '{a');
            });

            it('не должен вызывать #processMessage', function() {
                expect(this.transport.processMessage).to.have.callCount(0);
            });

            it('должен вернуть false', function() {
                expect(this.res).to.be.equal(false);
            });
        });

        describe('"operation=ping" ->', function() {
            beforeEach(function() {
                this.res = this.transport.onmessage('{"operation": "ping", "server-interval-sec": 60 }');
            });

            it('не должен вызывать #sendConfirm у сокета', function() {
                expect(this.socket.sendConfirm).to.have.callCount(0);
            });

            it('должен вызывать #updatePingEventTimeout у сокета', function() {
                expect(this.socket.updatePingEventTimeout).to.be.calledWithExactly(60);
            });

            it('не должен вызывать Jane.ErrorLog.send', function() {
                expect(Jane.ErrorLog.send).to.have.callCount(0);
            });

            it('должен вернуть false', function() {
                expect(this.res).to.be.equal(false);
            });

            it('не должен вызывать #processMessage', function() {
                expect(this.transport.processMessage).to.have.callCount(0);
            });
        });

        describe('Нормальное сообщение ->', function() {
            beforeEach(function() {
                this.xivaMsg = JSON.stringify({
                    bright: true,
                    lcn: '477799',
                    message: {
                        extra_data: 'aandrosov@yandex-team.ru',
                        fid: '2370000030000039751',
                        fid_type: '1',
                        firstline: '«Ошибки подписок на нотификации в логах»',
                        fresh_count: '7',
                        hdr_from: '"Алексей Шелковин" <info@calendar.yandex-team.ru>',
                        hdr_message_id: 'feooosjxbnkbxjkxnpcc@calcorp-back1h.cmail.yandex.net',
                        hdr_status: 'New',
                        hdr_subject: 'Ошибки подписок на нотификации в логах',
                        hdr_to: '<aandrosov@yandex-team.ru>',
                        is_mixed: '39059457',
                        lcn: '477799',
                        lid: '2080000000000067843',
                        lname: 'yacal',
                        method_id: 'insert ham',
                        mid: '2370000001663937321',
                        new_messages: '76',
                        operation: 'insert',
                        received_date: '26.05.2015 12:47:34',
                        session_key: '',
                        sz: '18641',
                        thread_id: '2370000001663937321',
                        uname: '1120000000007162'
                    },
                    operation: 'insert',
                    server_notify_id: 'YlghjTPQsqM1:YlgBh50kJ4Y1:daria:mail',
                    service: 'mail',
                    session_key: '',
                    uid: '1120000000000371',
                    version: '1'
                });

                this.res = this.transport.onmessage(this.xivaMsg);

                this.parsedMessage = JSON.parse(this.xivaMsg);
            });

            it('должен вызывать #sendConfirm у сокета', function() {
                expect(this.socket.sendConfirm).to.be.calledWithExactly(this.parsedMessage);
            });

            it('не должен вызывать Jane.ErrorLog.send', function() {
                expect(Jane.ErrorLog.send).to.have.callCount(0);
            });

            it('должен вернуть true', function() {
                expect(this.res).to.be.equal(true);
            });

            it('должен вызывать #processMessage', function() {
                expect(this.transport.processMessage).to.be.calledWithExactly(this.parsedMessage, this.xivaMsg);
            });
        });
    });

    describe('#processMessage', function() {
        beforeEach(function() {
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(this.transport, 'enqueue');
            this.sinon.stub(this.transport, 'sendCommonEvent');
        });

        describe('Сообщение от МА ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'uid').value('1');
            });

            describe('"insert" -> "', function() {
                beforeEach(function() {
                    this.transport.onmessage(JSON.stringify({
                        uid: '2',
                        message: {
                            operation: 'insert'
                        },
                        operation: 'insert'
                    }));
                });

                it('должен бросить событие "xiva.mail.multi.insert"', function() {
                    expect(ns.events.trigger).to.be.calledWith('xiva.mail.multi.insert', {
                        uid: '2',
                        message: {
                            operation: 'insert'
                        },
                        operation: 'insert'
                    });
                });

                sendCommonEventNotCalled();
                enqueueNotCalled();
            });

            describe('"transer" ->', function() {
                beforeEach(function() {
                    this.transport.onmessage(JSON.stringify({
                        bright: true,
                        lcn: '1',
                        message: { lcn: '1', session_key: '' },
                        operation: 'unsupported',
                        raw_data: '{"lcn":"1","operation":"transfer","sessionKey":"","uid":"323392647","uname":864490415}',
                        server_notify_id: 'qjJIIM0JdKo1:qjJWPkBh18c1:u2709:mail',
                        service: 'mail',
                        session_key: '',
                        tags: [],
                        uid: '323392647',
                        version: '1'
                    }));
                });

                it('не должен бросить события, потому что перенос акка из МА ни на что не влияет', function() {
                    expect(ns.events.trigger).to.have.callCount(0);
                });

                sendCommonEventNotCalled();
                enqueueNotCalled();
            });

            describe('другие -> "', function() {
                beforeEach(function() {
                    this.transport.onmessage(JSON.stringify({
                        uid: '2',
                        message: {
                            operation: 'status change'
                        },
                        operation: 'status change'
                    }));
                });

                it('должен бросить событие "xiva.mail.multi.update"', function() {
                    expect(ns.events.trigger).to.be.calledWith('xiva.mail.multi.update', {
                        uid: '2',
                        message: {
                            operation: 'status change'
                        },
                        operation: 'status change'
                    });
                });

                sendCommonEventNotCalled();
                enqueueNotCalled();
            });
        });

        describe('Сообщение от calendar', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'uid').value('1120000000000371');

                this.msg = {
                    bright: true,
                    lcn: '',
                    message: '{ "subject": "\\u0412\\u0441\\u0442\\u0440\\u0435\\u0447\\u0430", "location": "" }',
                    operation: 'meeting-reminder',
                    server_notify_id: 'mqiGoZSQmuQ1:mqi95xNAKiE1:daria:calendar',
                    service: 'calendar',
                    session_key: '',
                    uid: '1120000000000371',
                    version: '1'
                };

                this.transport.processMessage(this.msg);
            });

            it('должен вызвать #sendCommonEvent', function() {
                expect(this.transport.sendCommonEvent).to.be.calledWith(this.msg);
            });

            eventsTriggerNotCalled();
            enqueueNotCalled();
        });

        describe('Сообщение от mail ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'uid').value('1120000000000371');

                this.msg = {
                    bright: true,
                    lcn: '477914',
                    message: {
                        is_mixed_add: '2048',
                        is_mixed_del: '0',
                        lcn: '477914',
                        method_id: '',
                        mids: '["2370000001664276403"]',
                        new_messages: '24',
                        operation: 'status change',
                        session_key: 'df5514c8e7655023a633eb10faa10bed',
                        status: 'RO',
                        uname: '1120000000007162'
                    },
                    operation: 'unsupported',
                    server_notify_id: '1giL7GSQqOs1:1gi6ToNAJ8c1:daria:mail',
                    service: 'mail',
                    session_key: 'df5514c8e7655023a633eb10faa10bed',
                    uid: '1120000000000371',
                    version: '1'
                };

                this.transport.processMessage(this.msg);
            });

            it('должен добавить сообщение в очередь обработки', function() {
                expect(this.transport.enqueue).to.be.calledWith(this.msg);
            });

            eventsTriggerNotCalled();
            sendCommonEventNotCalled();
        });

        describe('Сообщение от mail, lcn больше предыдущего', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'uid').value('1120000000000371');
                this.sinon.stub(Daria, 'lcn').value('477913');

                this.msg = {
                    bright: true,
                    lcn: '477914',
                    message: {
                        is_mixed_add: '2048',
                        is_mixed_del: '0',
                        lcn: '477914',
                        method_id: '',
                        mids: '["2370000001664276403"]',
                        new_messages: '24',
                        operation: 'status change',
                        session_key: 'df5514c8e7655023a633eb10faa10bed',
                        status: 'RO',
                        uname: '1120000000007162'
                    },
                    operation: 'unsupported',
                    server_notify_id: '1giL7GSQqOs1:1gi6ToNAJ8c1:daria:mail',
                    service: 'mail',
                    session_key: 'df5514c8e7655023a633eb10faa10bed',
                    uid: '1120000000000371',
                    version: '1'
                };

                this.transport.processMessage(this.msg);
            });

            it('должен добавить сообщение в очередь обработки', function() {
                expect(this.transport.enqueue).to.be.calledWith(this.msg);
            });

            eventsTriggerNotCalled();
            sendCommonEventNotCalled();
        });

        describe('Сообщение от mail, operation="transfer" ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'uid').value('1120000000000371');
                this.sinon.stub(Daria.Xiva.handlers, 'transfer');

                this.msg = {
                    bright: true,
                    lcn: '1',
                    message: { lcn: '1', session_key: '' },
                    operation: 'unsupported',
                    raw_data: '{"lcn":"1","operation":"transfer","sessionKey":"","uid":"1120000000000371","uname":"1120000000000371"}',
                    server_notify_id: 'qjJIIM0JdKo1:qjJWPkBh18c1:u2709:mail',
                    service: 'mail',
                    session_key: '',
                    tags: [],
                    uid: '1120000000000371',
                    version: '1'
                };

                this.transport.processMessage(this.msg);
            });

            it('должен добавить сообщение в очередь обработки', function() {
                expect(this.transport.enqueue).to.be.calledWith(this.msg);
            });

            eventsTriggerNotCalled();
            sendCommonEventNotCalled();
        });

        describe('Плохие сообщения', function() {
            describe('Неизвестный service', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'uid').value('1120000000000371');

                    this.msg = {
                        bright: true,
                        lcn: '477914',
                        message: {
                            is_mixed_add: '2048',
                            is_mixed_del: '0',
                            lcn: '477914',
                            method_id: '',
                            mids: '["2370000001664276403"]',
                            new_messages: '24',
                            operation: 'status change',
                            session_key: 'df5514c8e7655023a633eb10faa10bed',
                            status: 'RO',
                            uname: '1120000000007162'
                        },
                        operation: 'unsupported',
                        server_notify_id: '1giL7GSQqOs1:1gi6ToNAJ8c1:daria:mail',
                        service: 'new-super-puper',
                        session_key: 'df5514c8e7655023a633eb10faa10bed',
                        uid: '1120000000000371',
                        version: '1'
                    };

                    this.transport.processMessage(this.msg, JSON.stringify(this.msg));
                });

                it('должен вызвать Jane.ErrorLog.send', function() {
                    expect(Jane.ErrorLog.send).to.be.calledWith({
                        errorType: 'xiva.unknown.message'
                    }, JSON.stringify(this.msg));
                });

                eventsTriggerNotCalled();
                sendCommonEventNotCalled();
                enqueueNotCalled();
            });

            describe('service=mail, нет operation', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'uid').value('1120000000000371');

                    this.msg = {
                        bright: true,
                        lcn: '477914',
                        message: {
                            is_mixed_add: '2048',
                            is_mixed_del: '0',
                            lcn: '477914',
                            method_id: '',
                            mids: '["2370000001664276403"]',
                            new_messages: '24',
                            operation: 'status change',
                            session_key: 'df5514c8e7655023a633eb10faa10bed',
                            status: 'RO',
                            uname: '1120000000007162'
                        },
                        operation: '',
                        server_notify_id: '1giL7GSQqOs1:1gi6ToNAJ8c1:daria:mail',
                        service: 'mail',
                        session_key: 'df5514c8e7655023a633eb10faa10bed',
                        uid: '1120000000000371',
                        version: '1'
                    };

                    this.transport.processMessage(this.msg, JSON.stringify(this.msg));
                });

                it('должен вызвать Jane.ErrorLog.send', function() {
                    expect(Jane.ErrorLog.send).to.be.calledWith({
                        errorType: 'xiva.unknown.message'
                    }, JSON.stringify(this.msg));
                });

                eventsTriggerNotCalled();
                sendCommonEventNotCalled();
                enqueueNotCalled();
            });

            describe('service=mail, нет lcn', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'uid').value('1120000000000371');

                    this.msg = {
                        bright: true,
                        message: {
                            is_mixed_add: '2048',
                            is_mixed_del: '0',
                            lcn: '477914',
                            method_id: '',
                            mids: '["2370000001664276403"]',
                            new_messages: '24',
                            operation: 'status change',
                            session_key: 'df5514c8e7655023a633eb10faa10bed',
                            status: 'RO',
                            uname: '1120000000007162'
                        },
                        operation: 'oper',
                        server_notify_id: '1giL7GSQqOs1:1gi6ToNAJ8c1:daria:mail',
                        service: 'mail',
                        session_key: 'df5514c8e7655023a633eb10faa10bed',
                        uid: '1120000000000371',
                        version: '1'
                    };

                    this.transport.processMessage(this.msg, JSON.stringify(this.msg));
                });

                it('должен вызвать Jane.ErrorLog.send', function() {
                    expect(Jane.ErrorLog.send).to.be.calledWith({
                        errorType: 'xiva.unknown.message'
                    }, JSON.stringify(this.msg));
                });

                eventsTriggerNotCalled();
                sendCommonEventNotCalled();
                enqueueNotCalled();
            });

            describe('service=mail, lcn меньше текущего', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'uid').value('1120000000000371');
                    this.sinon.stub(Daria, 'lcn').value('477915');

                    this.msg = {
                        bright: true,
                        lcn: '477914',
                        message: {
                            is_mixed_add: '2048',
                            is_mixed_del: '0',
                            lcn: '477914',
                            method_id: '',
                            mids: '["2370000001664276403"]',
                            new_messages: '24',
                            operation: 'status change',
                            session_key: 'df5514c8e7655023a633eb10faa10bed',
                            status: 'RO',
                            uname: '1120000000007162'
                        },
                        operation: 'unsupported',
                        server_notify_id: '1giL7GSQqOs1:1gi6ToNAJ8c1:daria:mail',
                        service: 'mail',
                        session_key: 'df5514c8e7655023a633eb10faa10bed',
                        uid: '1120000000000371',
                        version: '1'
                    };

                    this.transport.processMessage(this.msg);
                });

                eventsTriggerNotCalled();
                sendCommonEventNotCalled();
                enqueueNotCalled();
            });
        });

        function eventsTriggerNotCalled() {
            it('не должен вызвать ns.events.trigger', function() {
                expect(ns.events.trigger).to.have.callCount(0);
            });
        }

        function sendCommonEventNotCalled() {
            it('не должен вызвать #sendCommonEvent', function() {
                expect(this.transport.sendCommonEvent).to.have.callCount(0);
            });
        }

        function enqueueNotCalled() {
            it('не должен вызвать #enqueue', function() {
                expect(this.transport.enqueue).to.have.callCount(0);
            });
        }
    });

    describe('#sendCommonEvent', function() {
        beforeEach(function() {
            this.sinon.stub(ns.events, 'trigger');
        });

        it('должен отправить сообщение "xiva.<service>.<operation>"', function() {
            var msg = {
                bright: true,
                lcn: '',
                message: { subject: 'Без названия', location: '' },
                operation: 'meeting-reminder',
                server_notify_id: '6xbVug0COSw1:6xbQSJwxWSw1:u2709:calendar',
                service: 'calendar',
                session_key: '',
                uid: '1120000000019397',
                version: '1'
            };
            this.transport.sendCommonEvent(msg);

            expect(ns.events.trigger)
                .to.have.callCount(1)
                .and.to.be.calledWith('xiva.calendar.meeting-reminder', msg);
        });
    });

    describe('Обработка "xiva.mail.multi.insert"', function() {
        beforeEach(function() {
            this.xivaMsg = {
                bright: true,
                lcn: '1083',
                message: {
                    fid: '1',
                    fid_type: '1',
                    firstline: '',
                    fresh_count: '11',
                    hdr_from: '"Карцев Роман" <chestozo@yandex-team.ru>',
                    hdr_message_id: '<109881468488364@webcorp01f.yandex-team.ru>',
                    hdr_status: 'New',
                    hdr_subject: 'test 101010',
                    hdr_to: '"lambru1@yandex.ru" <lambru1@yandex.ru>',
                    lcn: '1083',
                    lid: '26,FAKE_POSTMASTER_LBL,FAKE_RECENT_LBL',
                    method_id: '',
                    mid: '159314836818232175',
                    new_messages: '12',
                    operation: 'insert',
                    received_date: '14.07.2016 12:26:04',
                    session_key: '',
                    sz: '2088',
                    thread_id: '159314836818232175',
                    uid: '340929597',
                    uname: '880037648'
                },
                operation: 'insert',
                //eslint-disable-next-line max-len
                raw_data: '{"envelopes":[{"avatarUrl":"https:\\\/\\\/avatars-fast.yandex.net\\\/get-profile-avatar\\\/people-4d56b50795f9d041e27a38a2771188a3","date":1468488364,"fid":"1","fidType":1,"firstline":"","from":[{"displayName":"Карцев Роман","domain":"yandex-team.ru","local":"chestozo"}],"labels":["26","FAKE_POSTMASTER_LBL","FAKE_RECENT_LBL"],"labelsInfo":{"26":{"symbolicName":{"title":""}},"FAKE_POSTMASTER_LBL":{"symbolicName":{"title":"postmaster_label"}},"FAKE_RECENT_LBL":{"symbolicName":{"title":"recent_label"}}},"messageId":"<109881468488364@webcorp01f.yandex-team.ru>","mid":"159314836818232175","size":2088,"subject":"test 101010","threadId":"159314836818232175","to":[{"displayName":"lambru1@yandex.ru","domain":"yandex.ru","local":"lambru1"}],"types":[4]}],"fid":"1","freshCount":11,"lcn":"1083","loc-args":["Карцев Роман","test 101010",""],"mid":"159314836818232175","mids_str":"[\\"159314836818232175\\"]","newCount":12,"operation":"insert","sessionKey":"","status":"New","threadId":"159314836818232175","uid":"340929597","uname":"880037648"}',
                server_notify_id: '7QRh4C1IUGk1:7QWf6pNaw4Y1:u2709:mail',
                service: 'mail',
                session_key: '',
                tags: [],
                uid: '340929597',
                version: '1'
            };

            this.mHeadUserState = ns.Model.get('head-user-state');
            this.sinon.stub(this.mHeadUserState, 'setNewLettersInMA');

            this.mUserDropdownData = ns.Model.get('user-dropdown-data').setData({});
            this.sinon.stub(this.mUserDropdownData, 'setNewCountById');
            this.sinon.stub(this.mUserDropdownData, 'getFreshForDefault').returns(11);

            this.gotNewMailBefore = 3;
            this.sinon.stub(Daria, 'gotNewMail').value(this.gotNewMailBefore);

            this.sinon.stub(Daria, 'updateTitle');
        });

        it('задан fresh_count - обновляется флаг "есть непрочитанные письма" у МА пользователя', function() {
            ns.events.trigger('xiva.mail.multi.insert', this.xivaMsg);
            expect(this.mHeadUserState.setNewLettersInMA).to.have.callCount(1);
        });

        it('задан fresh_count - обновляется флаг "есть непрочитанные письма" у МА пользователя', function() {
            ns.events.trigger('xiva.mail.multi.insert', this.xivaMsg);
            expect(this.mHeadUserState.setNewLettersInMA).to.calledWith(true);
        });

        it('задан fresh_count - обновляется число свежих писем', function() {
            ns.events.trigger('xiva.mail.multi.insert', this.xivaMsg);
            expect(this.mUserDropdownData.setNewCountById).to.have.callCount(1);
        });

        it('задан fresh_count - обновляется число свежих писем', function() {
            ns.events.trigger('xiva.mail.multi.insert', this.xivaMsg);
            expect(this.mUserDropdownData.setNewCountById).to.calledWith('340929597', 11);
        });

        it('задан fresh_count - обновляется число непрочитанных писем', function() {
            ns.events.trigger('xiva.mail.multi.insert', this.xivaMsg);
            expect(Daria.gotNewMail).to.be.eql(this.gotNewMailBefore + 11);
        });

        it('fresh_count - обновляется title страницы', function() {
            ns.events.trigger('xiva.mail.multi.insert', this.xivaMsg);
            expect(Daria.updateTitle).to.have.callCount(1);
        });
    });

    describe('Обработка "xiva.mail.multi.update"', function() {
        beforeEach(function() {
            this.xivaMsg = {
                bright: true,
                lcn: '1084',
                message: {
                    lcn: '1084',
                    method_id: '',
                    mids: '["159314836818232175"]',
                    new_messages: '12',
                    operation: 'status change',
                    session_key: '',
                    status: '',
                    uid: '340929597',
                    uname: '880037648'
                },
                operation: 'unsupported',
                raw_data: {
                    lcn: '1084',
                    mids_str: '["159314836818232175"]',
                    new_messages: 12,
                    operation: 'status change',
                    sessionKey: '',
                    status: '',
                    uid: '340929597',
                    uname: '880037648'
                },
                server_notify_id: '8QRhCC1IUGk1:8QWZI5If9qM1:u2709:mail',
                service: 'mail',
                session_key: '',
                tags: [],
                uid: '340929597',
                version: '1'
            };

            this.mHeadUserState = ns.Model.get('head-user-state');
            this.sinon.stub(this.mHeadUserState, 'setNewLettersInMA');

            this.mUserDropdownData = ns.Model.get('user-dropdown-data').setData({});
            this.sinon.stub(this.mUserDropdownData, 'setNewCountById');
            this.sinon.stub(this.mUserDropdownData, 'getSumOfFresh').returns(11);

            this.gotNewMailBefore = 3;
            this.sinon.stub(Daria, 'gotNewMail').value(this.gotNewMailBefore);

            this.sinon.stub(Daria, 'updateTitle');
        });

        it('если не задан fresh_count - не обновляется флаг "есть непрочитанные письма" у МА пользователя', function() {
            ns.events.trigger('xiva.mail.multi.update', this.xivaMsg);
            expect(this.mHeadUserState.setNewLettersInMA).to.have.callCount(0);
        });

        it('если не задан fresh_count - не обновляется число свежих писем', function() {
            ns.events.trigger('xiva.mail.multi.update', this.xivaMsg);
            expect(this.mUserDropdownData.setNewCountById).to.have.callCount(0);
        });

        it('если не задан fresh_count - не обновляется число непрочитанных писем', function() {
            ns.events.trigger('xiva.mail.multi.update', this.xivaMsg);
            expect(Daria.gotNewMail).to.be.eql(this.gotNewMailBefore);
        });

        it('если не задан fresh_count - не обновляется title страницы', function() {
            ns.events.trigger('xiva.mail.multi.update', this.xivaMsg);
            expect(Daria.updateTitle).to.have.callCount(0);
        });
    });
});
