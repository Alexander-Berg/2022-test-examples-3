describe('Daria.Xiva', function() {

    describe('.createThreadFromMessage', function() {

        it('должен собрать метки из всех писем треда', function() {
            ns.Model.get('messages', {thread_id: 't1'}).setData({
                message: [
                    {'count': 1, lid: ['1'], 'mid': '1', 'new': 0, 'tid': 't1'},
                    {'count': 1, lid: ['1', '2'], 'mid': '2', 'new': 0, 'tid': 't1'}
                ]
            });

            var mThread = ns.Model.get('message', {ids: 't1'});

            Daria.Xiva.createThreadFromMessage(
                ns.Model.get('message', {ids: '1'}),
                mThread,
                2
            );

            expect(mThread.get('.lid')).to.be.eql(['1', '2']);
        });

    });

    describe('.messageLabel', function() {

        describe('Поменялись метки у писем ->', function() {

            beforeEach(function() {
                ns.Model.get('labels').setData({
                    label: [
                        {lid: '2119130000000001414', name: '1'}
                    ]
                });
                this.sinon.stub(Daria.MOPS, 'doActionInMessages').returns(new Daria.MOPS.Opinfo());
                this.sinon.stub(ns, 'forcedRequest');

                this.msg = {
                    "lcn": "133941",
                    "m_lids": "[\"2119130000000001414\"]",
                    "method_id": "",
                    "mids": "[\"2170000000024303381\"]",
                    "operation": "mark mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "tids": "[\"2170000000024079116\"]",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageLabel(this.msg);
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageLabel(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['2170000000024303381'], 'label', {lid: '2119130000000001414'});
                });
            });

        });

        describe('Поменялись метки у треда ->', function() {

            beforeEach(function() {
                ns.Model.get('labels').setData({
                    label: [
                        {lid: '2290000002366407654', name: '1'}
                    ]
                });
                this.sinon.stub(Daria.MOPS, 'doActionInMessages').returns(new Daria.MOPS.Opinfo());
                this.sinon.stub(ns, 'forcedRequest');

                this.msg = {
                    "lcn": "211055",
                    "method_id": "",
                    "mids": "[\"2290000005459236657\",\"2290000005874581527\",\"2290000005879641230\",\"2290000005459101406\",\"2290000006207858684\",\"2290000005879550701\",\"2290000005874494800\",\"2290000005874581122\",\"2290000005323216162\",\"2290000005146126106\",\"2290000005458523143\",\"2290000005458522231\"]",
                    "operation": "mark mails",
                    "session_key": "09f67041d5713c43cfcfe58fd9635300",
                    "t_lids": "[\"2290000002366407654\"]",
                    "tids": "[\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\",\"2290000005146126106\"]",
                    "uname": "499870203"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageLabel(this.msg);
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageLabel(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['t2290000005146126106'], 'label', {lid: '2290000002366407654', onlyAdd: true});
                });

            });

        });

        describe('Незнакомая метка ->', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.MOPS, 'doActionInMessages').returns(new Daria.MOPS.Opinfo());
                this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());

                this.msg = {
                    "lcn": "133941",
                    "m_lids": "[\"2119130000000001414\"]",
                    "method_id": "",
                    "mids": "[\"2170000000024303381\"]",
                    "operation": "mark mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "tids": "[\"2170000000024079116\"]",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageLabel(this.msg);
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageLabel(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['2170000000024303381'], 'label', {lid: '2119130000000001414'});
                });
            });

            it('должен запросить список меток перед вызовом Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageLabel(this.msg).then(function() {
                    expect(ns.forcedRequest).to.be.calledBefore(Daria.MOPS.doActionInMessages);
                });
            });

        });

        describe('сообщение от эквалайзера', function() {

            describe('добавление метки ->', function() {

                beforeEach(function() {
                    this.msg = {
                        "all_labels": "[[\"label_to_add\",\"2160000210000578054\",\"FAKE_HAS_USER_LABELS_LBL\"]]",
                        "lcn": "156",
                        "method_id": "",
                        "mids": "[\"2160000000004725254\"]",
                        "operation": "mark mails",
                        "sessionKey": "u2709-1439218298931-67103715",
                        "session_key": "u2709-1439218298931-67103715",
                        "uid": "4000723812",
                        "uname": "3001569001"
                    };

                    this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());

                    // в этом письмо добавляем метку "label_to_add"
                    this.mMessage = ns.Model.get('message', {ids: '2160000000004725254'}).setData({
                        lid: ["2160000210000578054", "FAKE_HAS_USER_LABELS_LBL"]
                    });
                    this.sinon.stub(this.mMessage, 'label').returns(new Daria.MOPS.Opinfo());
                    this.sinon.stub(this.mMessage, 'unlabel').returns(new Daria.MOPS.Opinfo());
                });

                it('должен добавить метку в первое письмо', function() {
                    return Daria.Xiva.messageLabel(this.msg).then(function() {
                        expect(this.mMessage.label)
                            .to.have.callCount(1)
                            .and.to.be.calledWith({lid: 'label_to_add'});
                    }, this);
                });

                it('не должен удалить метку', function() {
                    return Daria.Xiva.messageLabel(this.msg).then(function() {
                        expect(this.mMessage.unlabel).to.have.callCount(0);
                    }, this);
                });

            });

            describe('удаление метки ->', function() {

                beforeEach(function() {
                    this.msg = {
                        "all_labels": "[[\"2160000000001457848\",\"2160000210000578054\",\"FAKE_HAS_USER_LABELS_LBL\"]]",
                        "lcn": "156",
                        "method_id": "",
                        "mids": "[\"2160000000004725257\"]",
                        "operation": "mark mails",
                        "sessionKey": "u2709-1439218298931-67103715",
                        "session_key": "u2709-1439218298931-67103715",
                        "uid": "4000723812",
                        "uname": "3001569001"
                    };

                    this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());

                    // из этого письма удаляем метку "label_to_remove"
                    this.mMessage = ns.Model.get('message', {ids: '2160000000004725257'}).setData({
                        lid: ["label_to_remove", "2160000000001457848","2160000210000578054","FAKE_HAS_USER_LABELS_LBL"]
                    });
                    this.sinon.stub(this.mMessage, 'label').returns(new Daria.MOPS.Opinfo());
                    this.sinon.stub(this.mMessage, 'unlabel').returns(new Daria.MOPS.Opinfo());
                });

                it('не должен добавить метку', function() {
                    return Daria.Xiva.messageLabel(this.msg).then(function() {
                        expect(this.mMessage.label).to.have.callCount(0);
                    }, this);
                });

                it('должен удалить метку "label_to_remove"', function() {
                    return Daria.Xiva.messageLabel(this.msg).then(function() {
                        expect(this.mMessage.unlabel)
                            .to.have.callCount(1)
                            .and.to.be.calledWith({lid: 'label_to_remove'});
                    }, this);
                });

            });

            describe('письмо, про которое мы ничего не знаем ->', function() {

                beforeEach(function() {
                    this.msg = {
                        "all_labels": "[[\"2160000000001457848\",\"2160000210000578054\",\"FAKE_HAS_USER_LABELS_LBL\"]]",
                        "lcn": "156",
                        "method_id": "",
                        "mids": "[\"2160000000004725257\"]",
                        "operation": "mark mails",
                        "sessionKey": "u2709-1439218298931-67103715",
                        "session_key": "u2709-1439218298931-67103715",
                        "uid": "4000723812",
                        "uname": "3001569001"
                    };

                    this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());

                    this.mMessage = ns.Model.get('message', {ids: '2160000000004725257'});
                    this.sinon.stub(this.mMessage, 'label').returns(new Daria.MOPS.Opinfo());
                    this.sinon.stub(this.mMessage, 'unlabel').returns(new Daria.MOPS.Opinfo());
                });

                it('не должен добавить метку', function() {
                    return Daria.Xiva.messageLabel(this.msg).then(function() {
                        expect(this.mMessage.label).to.have.callCount(0);
                    }, this);
                });

                it('не должен удалить метку', function() {
                    return Daria.Xiva.messageLabel(this.msg).then(function() {
                        expect(this.mMessage.unlabel).to.have.callCount(0);
                    }, this);
                });

            });

        });

    });

    describe('.moveMessageToFolder', function() {
        it('должен вызвать Daria.Xiva.messageMove с правильными параметрами', function() {
            const fakePromise = {};

            this.sinon.stub(Daria.Xiva, 'messageMove').returns(fakePromise);

            const result = Daria.Xiva.moveMessageToFolder('1', '2');

            expect(result).to.be.equal(fakePromise);
            expect(Daria.Xiva.messageMove)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly({ mids: '["1"]', fid: '2' });
        });
    });

    describe('.messageMove', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.MOPS, 'doActionInMessages');
            this.sinon.stub(ns, 'forcedRequest').returns(vow.resolve());
        });

        describe('неизвестное письмо ->', function() {
            beforeEach(function() {
                this.msg = {
                    "fid": "2170000240000059512",
                    "lcn": "133936",
                    "method_id": "",
                    "mids": "[\"2170000000024307942\"]",
                    "operation": "move mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "uname": "59670986"
                };

                ns.forcedRequest.returns(vow.resolve());
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageMove(this.msg);
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(
                        [ '2170000000024307942' ], 'doMove', { current_folder: '2170000240000059512' }
                    );
                });
            });

            it('должен запросить folders, labels и messages', function() {
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(ns.forcedRequest).to.be.calledWith([
                        ns.Model.get('messages', {filter_ids: ['2170000000024307942']}),
                        ns.Model.get('folders'),
                        ns.Model.get('labels')
                    ]);
                });
            });

            it('должен разбить запрос за messages на два', function() {
                var mids = [];
                for (var i = 0; i <= Daria.Xiva.MAX_MESSAGES_IN_MODEL; i++) {
                    mids.push('100500' + i);
                }
                this.msg.mids = JSON.stringify(mids);
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(ns.forcedRequest).to.be.calledWith([
                        ns.Model.get('messages', {filter_ids: mids.slice(0, 1)}),
                        ns.Model.get('messages', {filter_ids: mids.slice(1)}),
                        ns.Model.get('folders'),
                        ns.Model.get('labels')
                    ]);
                });
            });

        });

        describe('известное письмо ->', function() {
            beforeEach(function() {
                ns.Model.get('message', {ids: '2170000000024307942'}).setData({});

                this.msg = {
                    "fid": "2170000240000059512",
                    "lcn": "133936",
                    "method_id": "",
                    "mids": "[\"2170000000024307942\"]",
                    "operation": "move mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "uname": "59670986"
                };

                ns.forcedRequest.returns(vow.resolve());
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageMove(this.msg);
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(
                        [ '2170000000024307942' ], 'doMove', { current_folder: '2170000240000059512' }
                    );
                });
            });

            it('должен запросить folders, labels', function() {
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(ns.forcedRequest).to.be.calledWith([
                        ns.Model.get('folders'),
                        ns.Model.get('labels')
                    ]);
                });
            });

        });

        describe('fid = -1', function() {

            beforeEach(function() {
                this.sinon.stub(ns.Model.get('message', {ids: '1'}), 'remove');
                this.msg = {
                    "fid": "-1",
                    "lcn": "133936",
                    "method_id": "",
                    "mids": "[\"1\"]",
                    "operation": "move mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "uname": "59670986"
                };
            });

            it('должен вызвать #remove для каждого письма', function() {
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(ns.Model.get('message', {ids: '1'}).remove).to.have.callCount(1);
                });
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageMove(this.msg);
            });

            it('должен перезапросить folders, labels', function() {
                return Daria.Xiva.messageMove(this.msg).then(function() {
                    expect(ns.forcedRequest).to.be.calledWith(['folders', 'labels']);
                });
            });

        });

    });

    describe('.messageStatusChange', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.MOPS, 'doActionInMessages');
            this.sinon.stub(ns, 'request').returns(vow.resolve([]));
        });

        it('должен вернуть реджекченный промис, если нет "status"', function() {
            return Daria.Xiva.messageStatusChange({}).then(function() {
                return vow.reject('MUST REJECT');
            }, function() {
                return vow.resolve();
            });
        });

        it('должен вернуть реджекченный промис, если "status" неизвестен', function() {
            Daria.Xiva.messageStatusChange({
                status: 'unknown'
            }).then(function() {
                return vow.reject('MUST REJECT');
            }, function() {
                return vow.resolve();
            });
        });

        describe('status="New" -> ', function() {

            beforeEach(function() {
                ns.Model.get('message', {ids: '2170000000023849417'}).setData({
                    'new': 0
                });
                this.msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "130744",
                    "method_id": "",
                    "mids": "[\"2170000000023849417\"]",
                    "new_messages": "47",
                    "operation": "status change",
                    "session_key": "5df01ee8fe50420898f556b0f628a5e1",
                    "status": "New",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageStatusChange(this.msg);
            });

            it('не должен запрашивать модели', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(ns.request).to.be.calledWith([]);
                });
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['2170000000023849417'], 'unmark');
                });
            });

        });

        describe('status="PL" -> ', function() {

            beforeEach(function() {
                this.msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "130744",
                    "method_id": "",
                    "mids": "[\"2170000000023849417\"]",
                    "new_messages": "47",
                    "operation": "status change",
                    "session_key": "5df01ee8fe50420898f556b0f628a5e1",
                    "status": "PL",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageStatusChange(this.msg);
            });

            it('не должен запрашивать модели', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(ns.request).to.have.callCount(0);
                });
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['2170000000023849417'], 'markForwarded');
                });
            });

        });

        describe('status="OQ" -> ', function() {

            beforeEach(function() {
                this.msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "130744",
                    "method_id": "",
                    "mids": "[\"2170000000023849417\"]",
                    "new_messages": "47",
                    "operation": "status change",
                    "session_key": "5df01ee8fe50420898f556b0f628a5e1",
                    "status": "OQ",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageStatusChange(this.msg);
            });

            it('не должен запрашивать модели', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(ns.request).to.have.callCount(0);
                });
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['2170000000023849417'], 'markAnswered');
                });
            });

        });

        describe('status="RO" -> ', function() {

            beforeEach(function() {
                ns.Model.get('message', {ids: '2170000000023849417'}).setData({
                    'new': 1
                });
                this.msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "130744",
                    "method_id": "",
                    "mids": "[\"2170000000023849417\"]",
                    "new_messages": "47",
                    "operation": "status change",
                    "session_key": "5df01ee8fe50420898f556b0f628a5e1",
                    "status": "RO",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageStatusChange(this.msg);
            });

            it('должен вызвать Daria.MOPS.doActionInMessages', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(Daria.MOPS.doActionInMessages).to.be.calledWith(['2170000000023849417'], 'mark');
                });
            });

        });

        describe('Неизвестное письмо ->', function() {

            beforeEach(function() {
                ns.request.returns(vow.resolve().then(function() {
                    var model = ns.Model.get('message', {ids: '2170000000023849417'}).setData({
                        'fid': '123',
                        'new': 1
                    });
                    return [model];
                }));
                this.sinon.stub(ns.Model.get('folders'), 'adjustUnreadCounters');

                this.msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "130744",
                    "method_id": "",
                    "mids": "[\"2170000000023849417\"]",
                    "new_messages": "47",
                    "operation": "status change",
                    "session_key": "5df01ee8fe50420898f556b0f628a5e1",
                    "status": "New",
                    "uname": "59670986"
                };
            });

            it('должен вернуть резолвленный промис', function() {
                return Daria.Xiva.messageStatusChange(this.msg);
            });

            it('должен запросить неизвестное сообщение', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    expect(ns.request).to.be.calledWith([
                        ns.Model.get('messages', {filter_ids: ['2170000000023849417']})
                    ]);
                });
            });

            it('должен сместить счетчики папок', function() {
                return Daria.Xiva.messageStatusChange(this.msg).then(function() {
                    var method = ns.Model.get('folders').adjustUnreadCounters;

                    expect(method).to.be.calledWith({
                        '123': 1
                    });
                });
            });

        });

        describe('удаление флага Recent ->', function() {
            // удаление флага Recent
            // вот такое сочетание:
            // status=""
            // is_mixed_add="0"
            // is_mixed_del="32"

            it('должен проигнорировать это сообщение', function() {
                var msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "3125",
                    "method_id": "",
                    "mids": "[\"2490000004837378308\",\"2490000004837378296\",\"2490000004809018973\",\"2490000004785097993\",\"2490000004785098053\",\"2490000004820933014\",\"2490000004820951663\",\"2490000004820863124\",\"2490000004820866174\",\"2490000004820864310\",\"2490000004816891078\",\"2490000004817104938\"]",
                    "new_messages": "",
                    "operation": "status change",
                    "session_key": "",
                    "status": "",
                    "uname": "845896349"
                };
                return Daria.Xiva.messageStatusChange(msg);
            });

            it('не должен проигнорировать, если есть еще и другие изменения', function() {
                var msg = {
                    "is_mixed_add": "16",
                    "is_mixed_del": "32",
                    "lcn": "3125",
                    "method_id": "",
                    "mids": "[\"2490000004837378308\",\"2490000004837378296\",\"2490000004809018973\",\"2490000004785097993\",\"2490000004785098053\",\"2490000004820933014\",\"2490000004820951663\",\"2490000004820863124\",\"2490000004820866174\",\"2490000004820864310\",\"2490000004816891078\",\"2490000004817104938\"]",
                    "new_messages": "",
                    "operation": "status change",
                    "session_key": "",
                    "status": "",
                    "uname": "845896349"
                };
                return Daria.Xiva.messageStatusChange(msg).then(function() {
                    return vow.reject('MUST REJECT');
                }, function() {
                    return vow.resolve();
                });
            });

        });
    });

    describe('.processUnsupported', function() {

        describe('operation="delayed_message" ->', function() {

            it('должен проигнорировать', function() {
                var msg = {
                    "lcn": "9301",
                    "method_id": "",
                    "operation": "delayed_message",
                    "session_key": "feced109151c54b7ca4eac99bed7dff2",
                    "uname": "634276775"
                };

                return Daria.Xiva.processUnsupported(msg);
            });

        });

        describe('operation="mark mails" ->', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva, 'messageLabel');
                this.msg = {
                    "lcn": "133941",
                    "m_lids": "[\"2119130000000001414\"]",
                    "method_id": "",
                    "mids": "[\"2170000000024303381\"]",
                    "operation": "mark mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "tids": "[\"2170000000024079116\"]",
                    "uname": "59670986"
                };
            });

            it('должен вызвать .messageLabel', function() {
                Daria.Xiva.processUnsupported(this.msg);

                expect(Daria.Xiva.messageLabel).to.be.calledWith(this.msg);
            });

            it('должен вернуть результат .messageLabel', function() {
                var rnd = Math.random();
                Daria.Xiva.messageLabel.returns(rnd);

                var res = Daria.Xiva.processUnsupported(this.msg);

                expect(res).to.be.equal(rnd);
            });

        });

        describe('operation="move mails"', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva, 'messageMove');
                this.msg = {
                    "fid": "2170000240000059512",
                    "lcn": "133936",
                    "method_id": "",
                    "mids": "[\"2170000000024307942\"]",
                    "operation": "move mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "uname": "59670986"
                };
            });

            it('должен вызвать .messageMove', function() {
                Daria.Xiva.processUnsupported(this.msg);

                expect(Daria.Xiva.messageMove).to.be.calledWith(this.msg);
            });

            it('должен вернуть результат .messageMove', function() {
                var rnd = Math.random();
                Daria.Xiva.messageMove.returns(rnd);

                var res = Daria.Xiva.processUnsupported(this.msg);

                expect(res).to.be.equal(rnd);
            });

        });

        describe('operation="reset fresh" ->', function() {

            it('должен проигнорировать', function() {
                var msg = {
                    "lcn": "9301",
                    "method_id": "",
                    "operation": "reset fresh",
                    "session_key": "feced109151c54b7ca4eac99bed7dff2",
                    "uname": "634276775"
                };

                return Daria.Xiva.processUnsupported(msg);
            });

        });

        describe('operation="status change" ->', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva, 'messageStatusChange');
                this.msg = {
                    "is_mixed_add": "0",
                    "is_mixed_del": "32",
                    "lcn": "130744",
                    "method_id": "",
                    "mids": "[\"2170000000023849417\"]",
                    "new_messages": "47",
                    "operation": "status change",
                    "session_key": "5df01ee8fe50420898f556b0f628a5e1",
                    "status": "New",
                    "uname": "59670986"
                };
            });

            it('должен вызвать .messageStatusChange', function() {
                Daria.Xiva.processUnsupported(this.msg);

                expect(Daria.Xiva.messageStatusChange).to.be.calledWith(this.msg);
            });

            it('должен вернуть результат .messageStatusChange', function() {
                var rnd = Math.random();
                Daria.Xiva.messageStatusChange.returns(rnd);

                var res = Daria.Xiva.processUnsupported(this.msg);

                expect(res).to.be.equal(rnd);
            });

        });

        describe('operation="unmark mails" ->', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva, 'messageLabel');
                this.msg = {
                    "lcn": "133941",
                    "m_lids": "[\"2119130000000001414\"]",
                    "method_id": "",
                    "mids": "[\"2170000000024303381\"]",
                    "operation": "unmark mails",
                    "session_key": "c44a610f40171c348ed6a5cb7e59cbef",
                    "tids": "[\"2170000000024079116\"]",
                    "uname": "59670986"
                };
            });

            it('должен вызвать .messageLabel', function() {
                Daria.Xiva.processUnsupported(this.msg);

                expect(Daria.Xiva.messageLabel).to.be.calledWith(this.msg);
            });

            it('должен вернуть результат .messageLabel', function() {
                var rnd = Math.random();
                Daria.Xiva.messageLabel.returns(rnd);

                var res = Daria.Xiva.processUnsupported(this.msg);

                expect(res).to.be.equal(rnd);
            });

        });

        describe('operation="transfer" ->', function() {

            beforeEach(function() {
                this.sinon.stub(Daria.Xiva.handlers, 'transfer').returns(vow.resolve());
                this.msg = {
                    lcn: '1',
                    operation: 'transfer',
                    sessionKey: '',
                    uid: '1120000000000371',
                    uname: '1120000000000371'
                };
            });

            it('должен вызвать Daria.Xiva.handlers.transfer', function() {
                return Daria.Xiva.processUnsupported(this.msg).then(function() {
                    expect(Daria.Xiva.handlers.transfer).to.have.callCount(1);
                });
            });

        });

    });

    describe('#_getFoldersCounters', function() {
        it('Должен вернуть пустые объекты, если ничего не передали', function() {
            expect(Daria.Xiva._getFoldersCounters()).to.eql({ countersHash: {}, countersNewHash: {} });
        });

        it('Должен вернуть пустые объекты, если передали пустой объект', function() {
            expect(Daria.Xiva._getFoldersCounters({})).to.eql({ countersHash: {}, countersNewHash: {} });
        });

        it('Должен вернуть пустые объекты, если счётчики - пустые массивы', function() {
            expect(Daria.Xiva._getFoldersCounters({ counters: [], countersNew: [] }))
                .to.eql({ countersHash: {}, countersNewHash: {} });
        });

        it('Должен правильно расчитать значения счётчиков по папкам', function() {
            var counters = [ '1', '10', '2', '30', '100', '0' ];
            var countersNew = [ '5', '10', '1', '30', '102130102301230', '0' ];

            expect(Daria.Xiva._getFoldersCounters({ counters: counters, countersNew: countersNew }))
                .to.eql({
                    countersHash: {
                        '1': '10',
                        '2': '30',
                        '100': '0'
                    },
                    countersNewHash: {
                        '5': '10',
                        '1': '30',
                        '102130102301230': '0'
                    }
                });
        });

        it('Должен правильно распарсить нечётную комбинацию значений в массиве счётчиков', function() {
            var counters = [ '1', '10', '2', '30', '100' ];
            var countersNew = [ '5', '10', '1', '30', '102130102301230' ];

            expect(Daria.Xiva._getFoldersCounters({ counters: counters, countersNew: countersNew }))
                .to.eql({
                countersHash: {
                    '1': '10',
                    '2': '30',
                    '100': undefined
                },
                countersNewHash: {
                    '5': '10',
                    '1': '30',
                    '102130102301230': undefined
                }
            });
        });
    });

    describe('#updateFolderCounters', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Xiva, '_getFoldersCounters').returns({
                countersHash: {},
                countersNewHash: {}
            });

            this.sinon.stub(ns.page.current.params, 'current_folder').value('1');

            this.mMessage = {
                getFolderId: this.sinon.stub().returns('123')
            };

            this.mFolder = {
                updateCounts: this.sinon.stub().returns({ new: 0, count: 0 }),
                setRecent: this.sinon.stub()
            };

            this.sinon.stub(ns.Model, 'get').returns(this.mFolder);

            const counters = [ '1', '2', '3', '4' ];
            const countersNew = [ '5', '6', '7', '8' ];
            this.rawData = { counters: counters, countersNew: countersNew }
        });

        it('Должен вызвать Daria.Xiva._getFoldersCounters с правильными параметрами', function() {
            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(Daria.Xiva._getFoldersCounters).to.have.been.calledWith(this.rawData);
        });

        it('Должен вызвать правильную модель', function() {
            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(ns.Model.get).to.have.been.calledWith('folder', { fid: '123' });
        });

        it('Должен вызвать mFolder.updateCounts с правильными счётчиками', function() {
            const countersHash = {
                '123': 10
            };

            const countersNewHash = {
                '123': 1
            };

            Daria.Xiva._getFoldersCounters.returns({ countersHash: countersHash, countersNewHash: countersNewHash });

            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(this.mFolder.updateCounts).to.have.been.calledWith({
                count: countersHash['123'],
                new: countersNewHash['123']
            });
        });

        it('Если нет значений счётчиков, то должен подставить 0 при вызове mFolder.updateCounts', function() {
            Daria.Xiva._getFoldersCounters.returns({ countersHash: {}, countersNewHash: {} });

            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(this.mFolder.updateCounts).to.have.been.calledWith({ count: 0, new: 0 });
        });

        it('Если в обновляемой папке есть новые письма и это не текущая папка, то должен пометить папку', function() {
            this.mFolder.updateCounts.returns({ new: 1, count: 2 });

            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(this.mFolder.setRecent).have.callCount(1);
        });

        it('Если в обновляемой папке есть новые письма и это текущая папка, то должен не помечать папку', function() {
            this.mMessage.getFolderId.returns('1');
            this.mFolder.updateCounts.returns({ new: 1, count: 2 });

            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(this.mFolder.setRecent).have.callCount(0);
        });

        it('Если в обновляемой папке нет новых писем и это не текущая папка, то должен не помечать папку', function() {
            this.mMessage.getFolderId.returns('1');
            this.mFolder.updateCounts.returns({ new: 0, count: 2 });

            Daria.Xiva.updateFolderCounters(this.mMessage, this.rawData);

            expect(this.mFolder.setRecent).have.callCount(0);
        });
    });


    describe('#updateTabsCounters', function() {
        beforeEach(function() {
            this.targetTabId = 'social';
            this.mMessage = {
                getTabId: this.sinon.stub().returns(this.targetTabId)
            };

            const data = {
                tabs: [
                    {
                        id: "relevant",
                        counters: { new: 8, all: 1010, recent: false }
                    },
                    {
                        id: "news",
                        counters: { new: 0, all: 1363 }
                    },
                    {
                        id: "social",
                        counters: { new: 14, all: 325, recent: false }
                    }
                ]
            };

            this.mTab =  {
                data: data.tabs.filter((tabItem)  => tabItem.id === this.targetTabId)[0],
                setRecent: this.sinon.stub()
            };

            this.mTabs = {
                data,
                adjustUnreadCounters: this.sinon.stub(),
                getTab: this.sinon.stub().returns(this.mTab)
            };

            this.sinon.stub(ns.Model, 'get')
                .withArgs('tabs').returns(this.mTabs)
                .withArgs('tab').returns(this.mTab)
        });

        it('Обновляет счетчики в соответствии с пришедшими данными', function() {
            Daria.Xiva.updateTabsCounters(this.mMessage);
            expect(this.mTabs.adjustUnreadCounters).calledWith({ social: 1 });
        });

        it('Если открытый текущий таб не совпадает с табом, ' +
            'в который пришло сообщение, то делаем обновление значение recent', function() {
            this.sinon.stub(ns.page.current, 'params').value({ tabId: 'relevant' });
            Daria.Xiva.updateTabsCounters(this.mMessage);
            expect(this.mTab.setRecent).to.have.callCount(1);
        });

        it('Если открытый текущий таб совпадает с табом, ' +
            'в который пришло сообщение, то не делаем обновление значение recent', function() {
            this.sinon.stub(ns.page.current, 'params').value({ tabId: 'social' });
            Daria.Xiva.updateTabsCounters(this.mMessage);
            expect(this.mTab.setRecent).to.have.callCount(0);
        });
    });
});
