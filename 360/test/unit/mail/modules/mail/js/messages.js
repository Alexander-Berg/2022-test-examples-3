describe('Daria.messages', function() {
    beforeEach(function() {
        this.mSettings = ns.Model.get('settings');
    });

    describe('.clearSearchCache', function() {

        beforeEach(function() {
            setModelsByMock('message');

            this.inbox = ns.Model.get('messages', {
                current_folder: '1'
            }).setData({
                details: {},
                message: []
            });

            this.search = ns.Model.get('messages', {
                search: 'yes'
            }).setData({
                details: {},
                message: []
            });

            this.sinon.spy(ns.Model.prototype, 'invalidate');
        });

        it('не должен инвалидировать список писем в папке', function() {
            Daria.messages.clearSearchCache();
            expect(ns.Model.prototype.invalidate.calledOn(this.inbox)).to.be.equal(false);
        });

        it('должен инвалидировать списки поиска', function() {
            Daria.messages.clearSearchCache();
            expect(ns.Model.prototype.invalidate).to.be.calledOn(this.search);
        });

    });

    describe('.everHadMessages', function() {
        beforeEach(function() {
            var params = {};
            this.sinon.stub(ns.page.current, 'params').value(params);

            this.mMessages = ns.Model.get('messages', params);
        });

        it('настройка has_inbox_message установлена', function() {
            this.mMessages.setData(mock['messages'][0].data);
            this.mSettings.setData({
                'has_inbox_message': 'on'
            });

            expect(Daria.messages.everHadMessages()).to.be.ok;
        });

        it('настройка has_inbox_message не установлена и нет писем', function() {
            this.mMessages.setData({details: {}, message: []});
            this.mSettings.setData({});

            expect(Daria.messages.everHadMessages()).to.not.be.ok;
        });

        describe('настройка has_inbox_message не установлена.', function() {

            beforeEach(function() {
                this.sinon.spy(this.mSettings, 'setSettingOn');
                this.mSettings.setData({});
            });

            it('в ящике только приветсвенные письма', function() {
                this.mMessages.setData(mock['messages'][9].data);
                expect(Daria.messages.everHadMessages()).to.not.be.ok;
                expect(this.mSettings.setSettingOn.called).to.not.be.ok;
            });
            it('в ящике есть письма', function() {
                this.mMessages.setData(mock['messages'][8].data);
                expect(Daria.messages.everHadMessages()).to.be.ok;
                expect(this.mSettings.setSettingOn.called).to.be.ok;
            });
        });
    });

    describe('.whereToGoAfterMove', function() {
        describe('Настройка `После перемещения письма` выставлена в `к следующему письму` →', function() {
            beforeEach(function() {
                this.sinon.stub(this.mSettings, 'getSetting')
                    .withArgs('page_after_move').returns('next_message')
                    .withArgs('page_after_delete').returns('next_message');

                this.sinon.stub(Daria, 'is3pane').returns(false);
                this.sinon.stub(Daria, 'is2pane').returns(true);
            });
            describe('Режим 2pane обычный', function() {
                beforeEach(function() {
                    this.message = { mid: '1', tid: 't1', fid: '1' };
                    this.mMessage = ns.Model.get('message', { ids: this.message.mid }).setData(this.message);

                    this.mMessagesChecked = ns.Model.get('messages-checked');
                    this.mMessagesChecked.check(this.mMessage, true);
                });
                it('Не переходим к следующему письму, если оно есть', function() {
                    this.sinon.stub(Daria.messages, 'calculateUrlForNextMessageSetting')
                        .withArgs(this.mMessage)
                        .returns({
                            where: 'next message url',
                            whereFolderId: undefined,
                            expandMessage: undefined
                        });

                    var result = Daria.messages.whereToGoAfterMove('archive', this.mMessagesChecked);
                    expect(result).to.be.eql({ where: '', requestModels: [], requestParams: {} });
                });

                it('Не переходим в папку, если нет следующего', function() {
                    this.sinon.stub(Daria.messages, 'calculateUrlForNextMessageSetting')
                        .withArgs(this.mMessage)
                        .returns({
                            where: '',
                            whereFolderId: 'inbox',
                            expandMessage: undefined
                        });

                    this.sinon.stub(ns.router, 'generateUrl')
                        .withArgs('messages', { current_folder: 'inbox' })
                        .returns('inbox');


                    var result = Daria.messages.whereToGoAfterMove('archive', this.mMessagesChecked);
                    expect(result).to.be.eql({
                        where: '',
                        requestModels: [],
                        requestParams: { }
                    });
                });
            });

            describe('2pane с показом в списке сообщений', function() {
                beforeEach(function() {
                    this.sinon.stub(this.mSettings, 'isSet').withArgs('open-message-list').returns(true);
                    this.sinon.stub(Daria, 'isMessagePage').returns(false);
                });

                describe('Режим без группировки по тредам', function() {
                    beforeEach(function() {
                        this.message = { mid: '1', tid: 't1', fid: '1' };
                        this.mMessage = ns.Model.get('message', { ids: this.message.mid }).setData(this.message);

                        this.mMessagesChecked = ns.Model.get('messages-checked');
                        this.mMessagesChecked.check(this.mMessage, true);
                    });

                    it('При удалениии сообщения -> Переходим к следующему сообщению', function() {
                        this.sinon.stub(this.mSettings, 'isThreaded').returns(false);

                        this.sinon.stub(Daria.messages, 'calculateUrlForNextMessageSetting')
                            .withArgs(this.mMessage)
                            .returns({
                                where: 'next message url',
                                whereFolderId: undefined,
                                expandMessage: undefined
                            });

                        const result = Daria.messages.whereToGoAfterMove('delete', this.mMessagesChecked);
                        expect(result).to.be.eql({ where: 'next message url', requestParams: {}, requestModels: [] });
                    });
                });

                describe('Режим группировки по тредам', function() {
                    beforeEach(function() {
                        this.sinon.stub(this.mSettings, 'isThreaded').returns(true);

                        this.threadMessage = { mid: 't12', tid: 't12', fid: '1' };
                        this.mThreadMessage = ns.Model.get('message', { ids: this.threadMessage.mid }).setData(this.threadMessage);

                        this.mThreadMessagesChecked = ns.Model.get('messages-checked');
                        this.mThreadMessagesChecked.check(this.mThreadMessage, true);
                    });

                    it('При перемещении/удалении треда или его последнего сообщения переходим к следующему треду', function() {

                        this.sinon.stub(Daria.messages, 'calculateUrlForNextMessageSetting')
                            .withArgs(this.mThreadMessage)
                            .returns({
                                where: 'next thread url',
                                whereFolderId: undefined,
                                expandMessage: undefined
                            });

                        const result = Daria.messages.whereToGoAfterMove('delete', this.mThreadMessagesChecked);
                        expect(result).to.be.eql({ where: 'next thread url', requestParams: {}, requestModels: [] });
                    });
                    it('При перемещении/удалении не последнего сообщения из переходим к предыдущему сообщению из того же треда', function() {

                        this.sinon.stub(Daria.messages, 'calculateUrlForNextMessageSetting')
                            .withArgs(this.mThreadMessage)
                            .returns({
                                where: 'current thread url',
                                whereFolderId: undefined,
                                expandMessage: '11'
                            });

                        const result = Daria.messages.whereToGoAfterMove('delete', this.mThreadMessagesChecked);
                        expect(result).to.be.eql({ where: 'current thread url', requestParams: {}, requestModels: [], expandMessage: '11' });
                    });
                });
            });
        });
    });

    describe('#checkUsePinsByParams', function() {
        beforeEach(function() {
            this.method = Daria.messages.checkUsePinsByParams;
        });

        it('Должна вернуть false, если список по метке', function() {
            expect(this.method({ 'current_label': '1' })).to.not.ok;
        });

        it('Должна вернуть false, если список по непрочитанным или аттачам', function() {
            expect(this.method({ 'extra_cond': '1' })).to.not.ok;
        });

        it('Должна вернуть false, если список по треду', function() {
            expect(this.method({ 'thread_id': '1' })).to.not.ok;
        });

        it('Должен вернуть true, если в параметрах указаны thread_id и current_folder', function() {
            expect(this.method({ 'thread_id': '1', 'current_folder': '1' })).to.be.equal(true);
        });

        it('Должна вернуть false, если список по сборщику', function() {
            expect(this.method({ 'scope': 'rpopinfo' })).to.not.ok;
        });

        it('Должен вернуть true, если параметров нет или список не по метке, не по треду, не по сборщикам или по аттачам и непрочитанным', function() {
            expect(this.method({ 'current_folder': '1' })).to.be.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "удаленные"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': 'fid_bad_folders', 'symbol': 'trash', subfolder: [] } ] });
            this.sinon.stub(this.mFolders, 'isValid').returns(true);
            expect(this.method({ 'current_folder': 'fid_bad_folders' })).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "черновики"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': 'fid_bad_folders', 'symbol': 'draft', subfolder: [] } ] });
            this.sinon.stub(this.mFolders, 'isValid').returns(true);
            expect(this.method({ 'current_folder': 'fid_bad_folders' })).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "спам"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': 'fid_bad_folders', 'symbol': 'spam', subfolder: [] } ] });
            this.sinon.stub(this.mFolders, 'isValid').returns(true);
            expect(this.method({ 'current_folder': 'fid_bad_folders' })).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "шаблоны"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': 'fid_bad_folders', 'symbol': 'template', subfolder: [] } ] });
            this.sinon.stub(this.mFolders, 'isValid').returns(true);
            expect(this.method({ 'current_folder': 'fid_bad_folders' })).to.not.ok;
        });
    });

    describe('#canShowPinControlsByParams', function() {
        beforeEach(function() {
            this.method = Daria.messages.canShowPinControlsByParams;
            this.pageParams = _.clone(ns.page.current.params);
        });

        afterEach(function() {
            ns.page.current.params = _.clone(this.pageParams);
            delete this.pageParams;
        });

        it('Должен взять параметры страницы, если не передали параметры', function() {
            ns.page.current.params = {
                current_folder: '12345'
            };
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': '12345', 'symbol': 'trash', subfolder: [] } ] });

            expect(this.method()).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "Удаленные"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': '1', 'symbol': 'trash', subfolder: [] } ] });
            expect(this.method({ current_folder: '1' })).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "Черновики"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': '2', 'symbol': 'draft', subfolder: [] } ] });
            expect(this.method({ current_folder: '2' })).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "Спам"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': '3', 'symbol': 'spam', subfolder: [] } ] });
            expect(this.method({ current_folder: '3' })).to.not.ok;
        });

        it('Должен вернуть false, если в параметрах передана папка "Шаблоны"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': '4', 'symbol': 'template', subfolder: [] } ] });
            expect(this.method({ current_folder: '4' })).to.not.ok;
        });

        it('Должен вернуть true, если в параметрах передана папка "Входящие"', function() {
            this.mFolders = ns.Model.get('folders').setData({ 'folder': [ { 'fid': '5', 'symbol': 'inbox', subfolder: [] } ] });
            expect(this.method({ current_folder: '5' })).to.be.ok;
        });
    });

    describe('.findNearestMessage', function() {
        beforeEach(function() {
            this.viewParams = {
                current_folder: '1'
            };
            this.sinon.stub(ns.page.current, 'params').value(this.viewParams);
            this.mFolders = ns.Model.get('folders');
        });

        describe('обычный режим', function() {
            beforeEach(function() {
                this.sinon.stub(this.mSettings, 'isThreaded').returns(false);
                this.message1 = ns.Model.get('message', { ids: '1' }).setData({ fid: '1', tid: 't1', mid: '1' });
                this.message2 = ns.Model.get('message', { ids: '2' }).setData({ fid: '1', tid: 't2', mid: '2' });

                this.sinon.stub(this.message1, 'getFolderId').returns('1');
                this.sinon.stub(this.message2, 'getFolderId').returns('1');

                this.mMessages = ns.Model.get('messages', this.viewParams).setData({
                    message: [ { mid: '2', tid: 't2', count: 1 }, { mid: '1', tid: 't1', count: 1 } ]
                });
            });
            it('должен находить следующее сообщение', function() {
                const result = Daria.messages.findNearestMessage(this.message2);
                expect(result.data.mid).to.be.eql(this.message1.data.mid);
            });
            it('должен находить предыдущее сообщение', function() {
                const result = Daria.messages.findNearestMessage(this.message1, 'prev');
                expect(result.data.mid).to.be.eql(this.message2.data.mid);
            });
            it('не должен находить следующее сообщение, если переданное - последнее', function() {
                const result = Daria.messages.findNearestMessage(this.message1);
                expect(result).to.equal(false);
            });
        });

        describe('режим треда', function() {
            beforeEach(function() {
                this.sinon.stub(this.mSettings, 'isThreaded').returns(true);
                this.viewParams = {
                    current_folder: '1',
                    threaded: 'yes'
                };

                this.sinon.stub(ns.page.current, 'params').value(this.viewParams);

                this.message1 = ns.Model.get('message', { ids: '1' }).setData({ fid: '1', tid: 't1', mid: '1' });
                this.message2 = ns.Model.get('message', { ids: '2' }).setData({ fid: '1', tid: 't2', mid: '2' });
                this.message3 = ns.Model.get('message', { ids: '3' }).setData({ fid: '1', tid: 't3', mid: '3' });
                this.message4 = ns.Model.get('message', { ids: '4' }).setData({ fid: '1', tid: 't3', mid: '4' });

                this.mMessages = ns.Model.get('messages', { thread_id: 't3' }).setData({
                    message: [
                        { mid: '3', tid: 't3' },
                        { mid: '4', tid: 't3' }
                    ]
                });

                this.sinon.stub(this.message1, 'getFolderId').returns('1');
                this.sinon.stub(this.message2, 'getFolderId').returns('1');
                this.sinon.stub(this.message3, 'getFolderId').returns('1');
                this.sinon.stub(this.message4, 'getFolderId').returns('1');

                this.mMessages = ns.Model.get('messages', this.viewParams).setData({
                    message: [
                        { mid: '3', tid: 't3', count: 2 },
                        { mid: '2', tid: 't2', count: 1 },
                        { mid: '1', tid: 't1', count: 1 }
                    ]
                });
            });

            it('должен находить следующий тред, если переданное сообщение не тред', function() {
                const result = Daria.messages.findNearestMessage(this.message2);
                expect(result.data.tid).to.be.eql(this.message1.data.tid);
            });

            it('не должен находить следующее сообщение, если переданное сообщение последнее', function() {
                const result = Daria.messages.findNearestMessage(this.message1);
                expect(result).to.equal(false);
            });

            it('должен находить предыдущий тред, если переданное сообщение не тред', function() {
                const result = Daria.messages.findNearestMessage(this.message1, 'prev');
                expect(result.data.tid).to.be.eql(this.message2.data.tid);
            });

            it('должен находить следующее сообщение из того же треда, если переданное сообщение тред', function() {
                const result = Daria.messages.findNearestMessage(this.message3);
                expect(result.data.mid).to.be.eql(this.message4.data.mid);
            });

            it('должен находить предыдущее сообщение из того же треда, если переданное сообщение тред', function() {
                const result = Daria.messages.findNearestMessage(this.message4, 'prev');
                expect(result.data.mid).to.be.eql(this.message3.data.mid);
            });
        });
    });

    describe('.getDestFolderForCurrentMessage', function() {
        beforeEach(function() {
            this.twoPaneStub = this.sinon.stub(Daria, 'is2pane').returns(false);
            this.isMessagePageStub = this.sinon.stub(Daria, 'isMessagePage').returns(false);
            this.message = { mid: '1', tid: 't1', fid: '1' };
            this.message.getFolderId = () => this.message.fid;
            this.mFolders = ns.Model.get('folders');
            this.sinon.stub(this.mFolders, 'getFidBySymbol').returnsArg(0);
        });

        it('should return passed fid for move action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'move', { fid: 'dest' });
            expect(result.folderId).to.equal('dest');
        });

        it('should return passed fid for infolder action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'infolder', { fid: 'dest' });
            expect(result.folderId).to.equal('dest');
        });

        it('should return archive folder for archive action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'archive');
            expect(result.folderId).to.equal('archive');
        });

        it('should return trash folder for delete action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'delete');
            expect(result.folderId).to.equal('trash');
        });

        it('should return remove folder for delete action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'remove');
            expect(result.folderId).to.equal('trash');
        });

        it('should return spam folder for tospam action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'tospam');
            expect(result.folderId).to.equal('spam');
        });

        it('should return inbox folder for notspam action', function() {
            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'notspam');
            expect(result.folderId).to.equal('inbox');
        });

        it('should return message folder for unmark action for 2pane and message page', function() {
            this.twoPaneStub.returns(true);
            this.isMessagePageStub.returns(true);

            const result = Daria.messages.getDestFolderForCurrentMessage(this.message, 'unmark');
            expect(result.folderId).to.equal(this.message.fid);
        });
    });

    describe('.getPageAfterSettingForAction', function() {
        beforeEach(function() {
            this.mFolders = ns.Model.get('folders');

            this.getSettingsStub = this.sinon.stub(this.mSettings, 'getSetting');

            this.threePaneStub = this.sinon.stub(Daria, 'is3pane').returns(false);
            this.twoPaneStub = this.sinon.stub(Daria, 'is2pane').returns(true);
            this.isMessagePageStub = this.sinon.stub(Daria, 'isMessagePage').returns(false);
        });

        describe('setting after move -> current_list', function() {
            beforeEach(function() {
                this.getSettingsStub
                    .withArgs('page_after_move')
                    .returns('current_list');
            });

            it('shouldn\'t return page', function() {
                const result = Daria.messages.getPageAfterSettingForAction('move', { fid: 'custom' });
                expect(result).to.be.eql({ pageAfterSetting: '', settingsAction: 'move' });
            });
        });

        describe('setting after delete -> current_list', function() {
            beforeEach(function() {
                this.getSettingsStub
                    .withArgs('page_after_delete')
                    .returns('current_list');
            });

            it('should return current_list & delete for remove action', function() {
                const result = Daria.messages.getPageAfterSettingForAction('remove');
                expect(result).to.be.eql({ pageAfterSetting: 'current_list', settingsAction: 'delete' });
            });

            it('should return current_list & delete for delete action', function() {
                const result = Daria.messages.getPageAfterSettingForAction('delete');
                expect(result).to.be.eql({ pageAfterSetting: 'current_list', settingsAction: 'delete' });
            });
        });

        describe('setting after delete & move -> next_message', function() {
            beforeEach(function() {
                ns.page.current.page = null;
                this.getSettingsStub
                    .withArgs('page_after_delete').returns('next_message')
                    .withArgs('page_after_move').returns('next_message');
            });

            it('should return current_list for 3pane', function() {
                this.threePaneStub.returns(true);

                const result = Daria.messages.getPageAfterSettingForAction('remove');
                expect(result).to.be.eql({ pageAfterSetting: 'current_list', settingsAction: 'delete' });
            });

            it('should return source_folder for compose page', function() {
                ns.page.current.page = 'compose';

                const result = Daria.messages.getPageAfterSettingForAction('remove');
                expect(result).to.be.eql({ pageAfterSetting: 'source_folder', settingsAction: 'delete' });
            });

            it('should return dest_folder for is2pane on message page with action unmark', function() {
                this.isMessagePageStub.returns(true);

                const result = Daria.messages.getPageAfterSettingForAction('unmark');
                expect(result).to.be.eql({ pageAfterSetting: 'dest_folder', settingsAction: 'move' });
            });

            it('should return delete action for is2pane on message page with move to trash', function() {
                this.isMessagePageStub.returns(true);
                this.sinon.stub(this.mFolders, 'getFolderById')
                    .withArgs('trash')
                    .returns({ symbol: 'trash' });

                const result = Daria.messages.getPageAfterSettingForAction('move', { fid: 'trash' });
                expect(result).to.be.eql({ pageAfterSetting: 'next_message', settingsAction: 'delete' });
            });
        });
    });

    describe('.calculateUrlForNextMessageSetting', function() {
        describe('Настройка `После перемещения письма` выставлена в `к следующему письму` →', function() {
            beforeEach(function() {
                this.sinon.stub(this.mSettings, 'getSetting')
                    .withArgs('page_after_move').returns('next_message')
                    .withArgs('page_after_delete').returns('next_message');

                this.sinon.stub(Daria, 'is3pane').returns(false);
                this.sinon.stub(Daria, 'is2pane').returns(true);

                this.message = { mid: '1', tid: 't1', fid: '1' };
                this.next = { mid: '2', tid: 't2', fid: '1' };

                this.mMessage = ns.Model.get('message', { ids: this.message.mid }).setData(this.message);
                this.mNextMessage = ns.Model.get('message', { ids: this.next.mid }).setData(this.next);
            });
            describe('Режим 2pane обычный', function() {
                it('Переходим к следующему письму, если оно есть', function() {
                    this.sinon.stub(Daria.messages, 'findNearestMessage').returns(this.mNextMessage);
                    this.sinon.stub(Daria.Page.generateUrl, 'contentMessage')
                        .withArgs(this.next)
                        .returns('next message url');

                    const result = Daria.messages.calculateUrlForNextMessageSetting(this.mMessage);
                    expect(result).to.be.eql({
                        where: 'next message url',
                        whereFolderId: undefined,
                        expandMessage: undefined,
                        whereTabId: undefined
                    });
                });

                it('Переходим в папку, если нет следующего письма', function() {
                    this.sinon.stub(Daria.messages, 'findNearestMessage').returns(false);

                    const result = Daria.messages.calculateUrlForNextMessageSetting(this.mMessage);
                    expect(result).to.be.eql({
                        where: '',
                        whereFolderId: this.message.fid,
                        expandMessage: undefined,
                        whereTabId: null
                    });
                });

                it('Переходим к последнему сообщению из следующего треда, если следующее сообщение тред', function() {
                    const nextMessage = ns.Model.get('message', { ids: 't1' })
                        .setData({
                            tid: 't1',
                            mid: 't1',
                            last_mid: '3'
                        });

                    this.sinon.stub(Daria.messages, 'findNearestMessage').returns(nextMessage);
                    this.sinon.stub(Daria.Page.generateUrl, 'contentMessage')
                        .withArgs({ tid: 't1', mid: '3' })
                        .returns('message #3 url');

                    const result = Daria.messages.calculateUrlForNextMessageSetting(this.mMessage);
                    expect(result).to.be.eql({
                        where: 'message #3 url',
                        whereFolderId: undefined,
                        expandMessage: undefined,
                        whereTabId: undefined
                    });
                });
            });

            describe('2pane с показом в списке сообщений', function() {
                beforeEach(function() {
                    this.sinon.stub(this.mSettings, 'isSet').withArgs('open-message-list').returns(true);
                    this.sinon.stub(Daria, 'isMessagePage').returns(false);
                });

                describe('Режим без группировки по тредам', function() {
                    it('Переходим к следующему сообщению, если оно есть', function() {
                        this.sinon.stub(this.mSettings, 'isThreaded').returns(false);
                        this.sinon.stub(Daria.messages, 'findNearestMessage').returns(this.mNextMessage);

                        this.sinon.stub(Daria.Page.generateUrl, 'contentMessage')
                            .withArgs(this.next)
                            .returns('next message url');

                        const result = Daria.messages.calculateUrlForNextMessageSetting(this.mMessage);
                        expect(result).to.be.eql({
                            where: 'next message url',
                            whereFolderId: undefined,
                            expandMessage: undefined,
                            whereTabId: undefined
                        });
                    });
                });

                describe('Режим группировки по тредам', function() {
                    beforeEach(function() {
                        this.sinon.stub(this.mSettings, 'isThreaded').returns(true);

                        this.threadMessage = ns.Model.get('message', { ids: 't1' }).setData({ tid: 't1', mid: 't1', fid: '1' });
                        this.threadNextMessage = ns.Model.get('message', { ids: 't2' }).setData({ tid: 't2', mid: 't2', fid: '1' });
                        this.anotherMessageFromSameThread = ns.Model.get('message', { ids: '3' }).setData({ tid: 't1', mid: '3', fid: '1' });
                    });
                    it('Переходим к следующему треду, если он есть', function() {

                        this.sinon.stub(Daria.messages, 'findNearestMessage').returns(this.threadNextMessage);

                        this.sinon.stub(Daria.Page.generateUrl, 'contentMessage')
                            .withArgs(this.threadNextMessage.getData())
                            .returns('next thread url');

                        const result = Daria.messages.calculateUrlForNextMessageSetting(this.threadMessage);
                        expect(result).to.be.eql({
                            where: 'next thread url',
                            whereFolderId: undefined,
                            expandMessage: undefined,
                            whereTabId: undefined
                        });
                    });
                    it('Переходим к тому же треду, если следующее сообщение из того же треда, что и текущее', function() {

                        this.sinon.stub(Daria.messages, 'findNearestMessage').returns(this.anotherMessageFromSameThread);

                        this.sinon.stub(Daria.Page.generateUrl, 'contentMessage')
                            .withArgs({ tid: 't1', mid: 't1' })
                            .returns('current thread url');

                        const result = Daria.messages.calculateUrlForNextMessageSetting(this.threadMessage);
                        expect(result).to.be.eql({
                            where: 'current thread url',
                            whereFolderId: undefined,
                            expandMessage: this.anotherMessageFromSameThread.getData().mid,
                            whereTabId: undefined
                        });
                    });
                });
            });
        });
    });

    describe('._isOpenInMessageList', function() {
        beforeEach(function() {
            this.openMessageInList = this.sinon.stub(this.mSettings, 'isSet').withArgs('open-message-list');
        });

        it('Возвращает false, если выключена настройка open-message-list', function() {
            this.openMessageInList.returns(false);

            expect(Daria.messages._isOpenInMessageList()).to.be.equal(false);
        });

        it('Возвращает true, если включена настройка open-message-list', function() {
            this.openMessageInList.returns(true);

            expect(Daria.messages._isOpenInMessageList()).to.be.equal(true);
        });
    });

    describe('._shouldStayInCurrentThread', function() {
        beforeEach(function() {
            this.openMessageInList = this.sinon.stub(Daria.messages, '_isOpenInMessageList');
            this.openMessageInList.returns(true);

            this.isThreaded = this.sinon.stub(this.mSettings, 'isThreaded');
            this.isThreaded.returns(true);

            this.currentMessage = ns.Model.get('message', { ids: '1' }).setData({ fid: '1', tid: 't1', mid: '1' });
        });

        it('Если выключена настройка openMessageInList, возвращает false', function() {
            this.openMessageInList.returns(false);

            expect(Daria.messages._shouldStayInCurrentThread(this.currentMessage)).to.be.equal(false);
        });

        it('Если режим не тредный, то возвращает false', function() {
            this.isThreaded.returns(false);

            expect(Daria.messages._shouldStayInCurrentThread(this.currentMessage)).to.be.equal(false);
        });

        it('Если переданное сообщение целый тред, то возвращает false', function() {
            expect(Daria.messages._shouldStayInCurrentThread(this.currentMessage)).to.be.equal(false);
        });

        it('Если переданное сообщение не единственное в своем треде, то возвращает true', function() {
            const messages = [
                ns.Model.get('message', { ids: '100' }).setData({ fid: '1', tid: 't100', mid: '100' }),
                ns.Model.get('message', { ids: '101' }).setData({ fid: '1', tid: 't100', mid: '101' })
            ];

            ns.Model.get('messages', { thread_id: 't100' }).setData({
                message: messages.map((msg) => ({ mid: msg.get('.mid'), tid: msg.get('.tid') }))
            });

            expect(Daria.messages._shouldStayInCurrentThread(messages[0])).to.be.equal(true);
        });
    });

    describe('._calculateUrlForInMessageListSetting2pane', function() {
        beforeEach(function() {
            this.shouldStayInCurrentThread = this.sinon.stub(Daria.messages, '_shouldStayInCurrentThread');
            this.shouldStayInCurrentThread.returns(false);

            this.sinon.stub(ns.page, 'currentUrl').value('current-url');
        });

        it('если переданное сообщение не единственное в треде, то возвращает текущий урл', function() {
            this.shouldStayInCurrentThread.returns(true);

            expect(Daria.messages._calculateUrlForInMessageListSetting2pane(null))
                .to.be.eql({
                    where: 'current-url',
                    requestModels: [],
                    requestParams: {}
                });
        });

        it('если переданное сообщение не входит в тред из нескольких сообщений, то генерируем новый адрес без его учета', function() {
            const currentMessage = ns.Model.get('message', { ids: '1' }).setData({ fid: '1', tid: 't1', mid: '1' });

            this.sinon.stub(ns.page.current, 'params').value({
                ids: currentMessage.get('.mid'),
                thread_id: currentMessage.get('.tid'),
                current_folder: '1'
            });

            this.sinon.stub(ns.router, 'generateUrl')
                .withArgs('messages', { current_folder: '1' })
                .returns('inbox');

            expect(Daria.messages._calculateUrlForInMessageListSetting2pane(currentMessage))
                .to.be.eql({
                    where: 'inbox',
                    requestModels: [],
                    requestParams: {}
                });
        });

        it('если текущий адрес не включает папку, то переходим в папку удаляемого сообщения', function() {
            const currentMessage = ns.Model.get('message', { ids: '1' }).setData({ fid: '1', tid: 't1', mid: '1' });

            this.sinon.stub(ns.page.current, 'params').value({});

            this.sinon.stub(ns.router, 'generateUrl')
                .withArgs('messages', { current_folder: '1' })
                .returns('sent');

            expect(Daria.messages._calculateUrlForInMessageListSetting2pane(currentMessage))
                .to.be.eql({
                    where: 'sent',
                    requestModels: [],
                    requestParams: {}
                });
        });
    });

    describe('.calculateUrlForCurrentListSetting', function() {
        beforeEach(function() {
            this._calculateUrlForListSetting3Pane = this.sinon.stub(Daria.messages, '_calculateUrlForListSetting3Pane');
            this._calculateUrlForListSetting2Pane = this.sinon.stub(Daria.messages, '_calculateUrlForListSetting2Pane');
            this._calculateUrlForInMessageListSetting2pane = this.sinon.stub(Daria.messages, '_calculateUrlForInMessageListSetting2pane');

            this.is3pane = this.sinon.stub(Daria, 'is3pane');
            this.is3pane.returns(false);

            this.openMessageInList = this.sinon.stub(Daria.messages, '_isOpenInMessageList');
            this.openMessageInList.returns(false);
        });

        it('Для режима 3pane вызываем _calculateUrlForListSetting3Pane', function() {
            this.is3pane.returns(true);

            Daria.messages.calculateUrlForCurrentListSetting();

            expect(this._calculateUrlForListSetting3Pane).to.have.callCount(1);
        });

        it('Для режима 2pane вызываем _calculateUrlForListSetting2Pane', function() {
            Daria.messages.calculateUrlForCurrentListSetting();

            expect(this._calculateUrlForListSetting2Pane).to.have.callCount(1);
        });

        it('Для режима 2pane с опцией показа в списке сообщений вызываем _calculateUrlForInMessageListSetting2pane', function() {
            this.openMessageInList.returns(true);

            Daria.messages.calculateUrlForCurrentListSetting();

            expect(this._calculateUrlForInMessageListSetting2pane).to.have.callCount(1);
        });
    });
});
