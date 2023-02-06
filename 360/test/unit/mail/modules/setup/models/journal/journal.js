describe('Daria.mJournal', function() {
    beforeEach(function() {
        this.mJournal = ns.Model.get('journal');
        this.mLabels = ns.Model.get('labels');
        this.mFolders = ns.Model.get('folders');
    });

    describe('#preprocessData', function() {
        it('Если операция не требует преобразований, то ничего нового и не добавляется', function() {
            this.data = {
                journal: {
                    day: [ {
                        view_date: 1500001200000,
                        date: 1499990400000,
                        entry: [ {
                            view_date: 1500061295286,
                            country: 'Россия',
                            operation: 'reset_fresh',
                            ip: '2a02:6b8:0:40c:3940:2413:dc23:d833',
                            date: 1500050495286,
                            affected: '1'
                        } ]
                    } ],
                    remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                }
            };
            expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                journal: {
                    day: [
                        {
                            view_date: 1500001200000,
                            date: 1499990400000,
                            entry: [
                                {
                                    view_date: 1500061295286,
                                    country: 'Россия',
                                    operation: 'reset_fresh',
                                    ip: '2a02:6b8:0:40c:3940:2413:dc23:d833',
                                    date: 1500050495286,
                                    affected: '1'
                                }
                            ]
                        }
                    ],
                    remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                }
            });
        });

        describe('abuse ->', function() {
            it('Если есть среди операций тип отличный от nonspam/spam, то выпиливаем эту операцию', function() {
                this.data = {
                    journal: {
                        day: [ {
                            view_date: 1500001200000,
                            date: 1499990400000,
                            entry: [ {
                                operation: 'abuse',
                                abuseType: 'nonspam'
                            }, {
                                operation: 'abuse',
                                abuseType: 'spam'
                            }, {
                                operation: 'abuse',
                                abuseType: 'some_new_type'
                            } ]
                        } ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                };
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'abuse',
                                        abuseType: 'nonspam',
                                        spam_type: 'nonspam'
                                    },
                                    {
                                        operation: 'abuse',
                                        abuseType: 'spam',
                                        spam_type: 'spam'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                });
            });
        });
        describe('move ->', function() {
            it('Если обычная операция move папки не Удаленные, то поля folder_symbol не будет', function() {
                this.data = {
                    journal: {
                        day: [ {
                            view_date: 1500001200000,
                            date: 1499990400000,
                            entry: [ {
                                operation: 'move',
                                destFid: '93'
                            } ]
                        } ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                };

                this.sinon.stub(this.mFolders, 'getFolderById').returns({
                    name: 'my_folder',
                    fid: '93'
                });
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'move',
                                        destFid: '93'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                });
            });
            it('Если операция move папки Удаленные, то запишется folder_symbol=trash', function() {
                this.data = {
                    journal: {
                        day: [ {
                            view_date: 1500001200000,
                            date: 1499990400000,
                            entry: [ {
                                operation: 'move',
                                destFid: '3'
                            } ]
                        } ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                };
                this.sinon.stub(this.mFolders, 'getFolderById').returns({
                    name: 'Удаленные',
                    symbol: 'trash',
                    fid: '3'
                });
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'move',
                                        destFid: '3',
                                        folder_symbol: 'trash'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                });
            });
        });
        describe('mark ->', function() {
            it('Преобразует msgStatus в condition', function() {
                this.data = {
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'mark',
                                        msgStatus: 'forwarded'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                };
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'mark',
                                        msgStatus: 'forwarded',
                                        condition: 'forwarded'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                }
                );
            });
        });
        describe('receive ->', function() {
            it('Не показываем операции которые не являются типами 0, 1 и 5', function() {
                this.data = {
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'receive',
                                        ftype: '1',
                                        affected: '2'
                                    },
                                    {
                                        operation: 'receive',
                                        ftype: '5',
                                        affected: '3'
                                    },
                                    {
                                        operation: 'receive',
                                        ftype: '0',
                                        affected: '1'
                                    },
                                    {
                                        operation: 'receive',
                                        ftype: '3',
                                        affected: '233'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                };
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'receive',
                                        ftype: '1',
                                        affected: '2'
                                    },
                                    {
                                        operation: 'receive',
                                        ftype: '5',
                                        affected: '3'
                                    },
                                    {
                                        operation: 'receive',
                                        ftype: '0',
                                        affected: '1'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                });
            });
            it('Для операции черновика не логично выводить "от кого", даже если он пришел', function() {
                this.data = {
                    journal: {
                        day: [
                            {
                                entry: [
                                    {
                                        operation: 'receive',
                                        ftype: '5',
                                        emailFrom: 'ekhurtina@yandex-team.ru'
                                    }
                                ]
                            }
                        ]
                    }
                };
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                entry: [
                                    {
                                        operation: 'receive',
                                        ftype: '5',
                                        emailFrom: ''
                                    }
                                ]
                            }
                        ]
                    }
                });
            });
        });
        describe('label, unlabel ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.mJournal, 'lidToText').returns('Текст метки');
                this.sinon.stub(this.mLabels, 'getLabelById').returns({
                    user: true,
                    lid: '13'
                });
            });
            it('Для метки выдается lidText', function() {
                this.data = {
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'label',
                                        lids: '13'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                };
                expect(this.mJournal.preprocessData(this.data)).to.be.eql({
                    journal: {
                        day: [
                            {
                                view_date: 1500001200000,
                                date: 1499990400000,
                                entry: [
                                    {
                                        operation: 'label',
                                        lids: '13',
                                        lidText: 'Текст метки'
                                    }
                                ]
                            }
                        ],
                        remote_ip: '2a02:6b8:0:40c:b911:1cc5:991d:5931'
                    }
                });
            });
        });
    });
    describe('#groupDraftOperations', function() {
        beforeEach(function() {
            this.dayEntry = [
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '123456789'
                },
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '123456789'
                },
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '123456789'
                },
                {
                    operation: 'receive',
                    ftype: '0',
                    affected: '1'
                },
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '1234567891213'
                },
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '1234567891213'
                }
            ];
            this.sinon.spy(this.mJournal, 'deleteEntry');
        });
        it('Операций несколько, с одинаковым mid - оставляем одну и сохраняем count', function() {
            this.mJournal.groupDraftOperations(this.dayEntry);
            expect(this.dayEntry).to.be.eql([
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '123456789',
                    count: 3
                },
                {
                    operation: 'receive',
                    ftype: '0',
                    affected: '1'
                },
                {
                    operation: 'receive',
                    ftype: '5',
                    mid: '1234567891213',
                    count: 2
                }
            ]);
            expect(this.mJournal.deleteEntry).to.have.callCount(3);
        });
    });
    describe('#removeFrequentResetFreshOperations', function() {
        beforeEach(function() {
            this.dayEntry = [
                {
                    operation: 'reset_fresh',
                    date: 1500050562829
                },
                {
                    operation: 'reset_fresh',
                    date: 1500050553313
                },
                {
                    operation: 'reset_fresh',
                    date: 1500050547042
                },
                {
                    operation: 'reset_fresh',
                    date: 1500050544224
                }
            ];
            this.sinon.spy(this.mJournal, 'deleteEntry');
        });
        it('Если заходы в почту чаще 1 минуты, оставляем один из них', function() {
            this.mJournal.removeFrequentResetFreshOperations(this.dayEntry);
            expect(this.dayEntry).to.be.eql([ {
                operation: 'reset_fresh',
                date: 1500050562829
            } ]);
            expect(this.mJournal.deleteEntry).to.have.callCount(3);
        });
    });

    describe('#lidToText', function() {
        it('Если передаем системную метку важные, запиненные, мьют, то отдается осмысленный текст', function() {
            expect(this.mJournal.lidToText({ symbolicName: 'mute_label' })).to.be.eql(i18n('%Label_mute'));
            expect(this.mJournal.lidToText({ symbolicName: 'pinned_label' })).to.be.eql(i18n('%Label_pin'));
            expect(this.mJournal.lidToText({ symbolicName: 'important_label', name: 'Важные' }))
                .to.be.eql(i18n('%Labels_Important'));
        });
        it('Если передаем пользовательскую метку, отдается текст имени этой метки', function() {
            expect(this.mJournal.lidToText({ name: 'Моя метка', user: true })).to.be.eql('Моя метка');
        });
        it('Если метка системная, и в нашем словаре ее нет, и имени нет, то отдается пустота', function() {
            expect(this.mJournal.lidToText({ symbolicName: 'dffsdfs' })).to.be.eql('');
        });
    });

    describe('#deleteEntry', function() {
        beforeEach(function() {
            this.mas = [ 'apple', 'kiwi', 'strawberry', 'pineapple', 'pear' ];
        });
        it('Удаляет то что нужно и модифицирует массив, индекс уменьшает и отдает его', function() {
            var index = this.mJournal.deleteEntry(this.mas, 2);
            expect(index).to.be.eql(1);
            expect(this.mas).to.be.eql([ 'apple', 'kiwi', 'pineapple', 'pear' ]);
            expect(this.mas.length).to.be.eql(4);
        });
    });
});
