describe('context-menu-mixin-messages', function() {

    beforeEach(function() {
        this.messages = ns.View.create('context-menu-mixin-messages');

        this.messages.__table = {
            getIDs: sinon.stub().returns({
                ids: [],
                tids: []
            })
        };
    });

    it('Миксин присутствует', function() {
        expect(this.messages).ok;
    });

    describe('__showContextMenu ->', function() {
        beforeEach(function() {
            this.sinon.spy(ns.events, 'trigger');
            this.sinon.stub(this.messages, '__cmMessagesPrepareToShowMenu').returns({show: function() {}});

            this.event = {
                currentTarget: {},
                pageX: 0,
                pageY: 0
            };
        });

        it('при показе КМ на письме должен сниматся чек с других писем', function() {
            this.messages.__showContextMenu(this.event);
            expect(ns.events.trigger).calledWith('daria:vMessages:deselect');
        });
    });

    xdescribe('__getFolders ->', function() {

        beforeEach(function() {
            var mFolders = ns.Model.get('folders');

            var folders = {
                'i': {
                    name: 'Inbox',
                    symbol: 'inbox',
                    fid: '333'
                },
                '2': {
                    name: 'name2',
                    fid: '2',
                    folder_options: {
                        position: 100
                    }
                },
                '1': {
                    name: 'name1',
                    fid: '1',
                    folder_options: {
                        position: 10
                    }
                },
                '1000': {
                    name: 'Yandex',
                    fid: '1000'
                },
                '111': {
                    name: 'OuTbOx',
                    symbol: 'outbox',
                    fid: '111'
                }
            };

            this.sinon.stub(mFolders, 'get').withArgs('.folder').returns(_.values(folders));
            this.sinon.stub(mFolders, 'getFolderById').callsFake(function(id) {
                return folders[id];
            });

            //ns.Model.get.withArgs('messages').returns({});

            this.folders = this.messages.__getFolders();
        });

        it('Игнорирует папку "Исходящие"', function() {
            expect(_.any(this.folders, { fid: "111" })).to.not.ok;
        });

        it('Игнорирует папку "рассылки"', function() {
            expect(_.any(this.folders, { fid: "1000" })).to.not.ok;
        });

        it('Учитывает пользовательский порядок', function() {
            var i1 = _(this.folders).findIndex({
                fid: '1'
            });

            var i2 = _(this.folders).findIndex({
                fid: '2'
            });

            expect(i1).below(i2);
        });

    });

    describe('__onReplyClick ->', function() {

        beforeEach(function() {
            var foldersHash = {
                '1': {
                    fid: '1',
                    symbol: 'outbox'
                },
                '2': {
                    fid: '2',
                    symbol: 'sent'
                },
                '3': {
                    fid: '3',
                    symbol: 'draft'
                },
                '4': {
                    fid: '4',
                    symbol: 'inbox'
                }
            };

            var messagesData = [
                {
                    mid: '100',
                    fid: '1'
                },
                {
                    mid: '102',
                    fid: '2'
                },
                {
                    mid: '103',
                    fid: '3'
                },
                {
                    mid: '104',
                    fid: '4'
                },
                {
                    mid: '105',
                    fid: '4'
                }
            ];

            var mFolders = ns.Model.get('folders');

            this.sinon.stub(mFolders, 'getFolderById').callsFake(function(fid) {
                return foldersHash[fid];
            });

            var model = ns.Model.get('messages');
            this.sinon.stub(model, 'get').withArgs('.message').returns(messagesData);
            this.sinon.stub(ns, 'request').returns(Vow.resolve([model]));

            this.sinon.stub(this.messages, '__goReply');
        });

        xit('Должен брать первое письмо находящееся не в папках "outbox", "sent", "draft"', function() {
            this.messages.__isThread = true;
            this.messages.__mid = 3;

            return this.messages.__onReplyClick().then(function() {
                expect(this.messages.__goReply.calledOnce).to.be.ok;
                expect(this.messages.__goReply.calledWith('104')).to.be.ok;

            }.bind(this));
        });

    });

    describe('__processLabels ->', function() {
        beforeEach(function() {
            this.mLabels = ns.Model.get('labels');

            this.sinon.stub(this.mLabels, 'getLabelById')
                .withArgs('1').returns({ color: '00F', name: 'blue' })
                .withArgs('3').returns({ color: '0F0', name: 'green' })
                .withArgs('7').returns({ color: 'F00', name: 'Important' });

            this.sinon.stub(this.mLabels, 'getImportantLID').returns('7');

            this.sinon.stub(yr, 'run').returns('fakeIcon');

            this.messages._onToolbarButton = this.sinon.stub();
        });

        afterEach(function() {
            delete this.messages._onToolbarButton;
        });

        it('Генерация обычной метки', function() {
            this.sinon.stub(Daria, 'shouldShowContextToolbar').returns(false);

            const result = this.messages.__processLabels([ '1' ], 'label', 0);
            const onClick = result[0].onClick;
            result[0].onClick = 'fakeOnClick';

            expect(yr.run).to.be.calledWithExactly('mail', { color: '00F' }, 'context-menu-label-custom-icon');

            expect(result).to.be.eql([
                {
                    id: '1',
                    text: 'blue',
                    alt: 'blue',
                    index: 1,
                    customIcon: 'fakeIcon',
                    metrika: [ 'метка', 'пользовательская метка', 'проставление' ],
                    onClick: 'fakeOnClick'
                }
            ]);

            onClick();
            expect(this.messages._onToolbarButton).to.be.calledWithExactly('label', { lid: '1' });
        });

        it('Генерация метки Важное', function() {
            this.sinon.stub(Daria, 'shouldShowContextToolbar').returns(false);

            const result = this.messages.__processLabels([ '7' ], 'unlabel', 0);
            const onClick = result[0].onClick;
            result[0].onClick = 'fakeOnClick';

            expect(yr.run).to.be.calledWithExactly('mail', { icon: 'Important' }, 'context-menu-MainToolbar-icon');

            expect(result).to.be.eql([
                {
                    id: '7',
                    text: 'Important',
                    alt: 'Important',
                    index: -1,
                    customIcon: 'fakeIcon',
                    metrika: [ 'метка', 'флажок', 'снятие' ],
                    onClick: 'fakeOnClick'
                }
            ]);

            onClick();
            expect(this.messages._onToolbarButton).to.be.calledWithExactly('unlabel', { lid: '7' });
        });

        it('Метка Важное должна быть всегда в начале списка', function() {
            this.sinon.stub(Daria, 'shouldShowContextToolbar').returns(false);

            const result = this.messages.__processLabels([ '1', '3', '7' ], 'label', 0);
            expect(result.map((label) => { return { id: label.id, index: label.index } }))
                .to.be.eql([
                    { id: '1', index: 1 },
                    { id: '3', index: 2 },
                    { id: '7', index: -1 }
                ]);
        });

        it('Метка Важное должна отсутствовать', function() {
            this.sinon.stub(Daria, 'shouldShowContextToolbar').returns(true);

            const result = this.messages.__processLabels([ '1', '3', '7' ], 'label', 0);
            expect(result.map((label) => { return { id: label.id, index: label.index } }))
                .to.be.eql([
                    { id: '1', index: 1 },
                    { id: '3', index: 2 }
                ]);
        });
    });
});
