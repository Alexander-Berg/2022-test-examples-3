describe('Daria.mFolder', function() {

    beforeEach(function() {
        /** @type Daria.mFolder */
        this.mFolder = ns.Model.get('folder', { fid: '1' });
        this.mFolders = ns.Model.get('folders');
    });

    describe('#adjustUnreads', function() {

        it('должен изменить счетчик непрочитанных', function() {
            this.mFolder.setData({'new': 1, 'subfolder': []});
            this.mFolder.adjustUnreads(2);

            expect(this.mFolder.getUnreadsCount()).to.be.equal(3);
        });

        it('должен записать 0, если счетчик пытается уйти в отрицательное значение', function() {
            this.mFolder.setData({'new': 1, 'subfolder': []});
            this.mFolder.adjustUnreads(-2);

            expect(this.mFolder.getUnreadsCount()).to.be.equal(0);
        });

    });

    describe('#getUnreadsCount', function() {

        it('должен вернуть количенство непрочитанных писем в папке', function() {
            this.mFolder.setData({'new': 10});

            expect(this.mFolder.getUnreadsCount()).to.be.equal(10);
        });

    });

    describe('#hasUnreads', function() {

        it('должен вернуть true, если в папке есть непрочитанные письма', function() {
            this.mFolder.setData({'new': 1});
            expect(this.mFolder.hasUnreads()).to.be.equal(true);
        });

        it('должен вернуть false, если в папке нет непрочитанных писем', function() {
            this.mFolder.setData({'new': 0});
            expect(this.mFolder.hasUnreads()).to.be.equal(false);
        });

    });

    describe('#hasNestingToggler', function() {

        beforeEach(function() {
            this.mSettings = ns.Model.get('settings');

            this.mFolder1 = ns.Model.get('folder', {fid: '1'}).setData({
                'has-unread': true,
                'new': 0,
                'subfolder': ['2']
            });

            this.mFolder2 = ns.Model.get('folder', {fid: '2'}).setData({
                'has-unread': true,
                'new': 1,
                'parent_id': '1',
                'subfolder': []
            });

            this.mFolder3 = ns.Model.get('folder', {fid: '3'}).setData({
                'has-unread': true,
                'new': 1,
                'subfolder': ['4']
            });

            this.mFolder4 = ns.Model.get('folder', {fid: '4'}).setData({
                'parent_id': '3',
                'subfolder': []
            });

        });

        it('должен вернуть false, если у папки нет вложенных', function() {
            this.mFolder.setData({'new': 1});
            expect(this.mFolder.hasNestingToggler()).to.be.equal(false);
        });

        it('должен вернуть true, если в папке есть вложенные', function() {
            expect(this.mFolder3.hasNestingToggler()).to.be.equal(true);
        });

        it('должен вернуть true, если в папке есть вложенные непрочитанные', function() {
            this.mSettings.setData({
                hide_empty_folders: true
            });
            expect(this.mFolder1.hasNestingToggler()).to.be.equal(true);
        });

        it('должен вернуть false, если в папке нет вложенных непрочитанных', function() {
            this.mSettings.setData({
                hide_empty_folders: true
            });
            expect(this.mFolder3.hasNestingToggler()).to.be.equal(false);
        });

    });

    describe('#updateCounts', function() {

        beforeEach(function() {
            this.sinon.spy(this.mFolder, 'set');
            this.mFolder.setData({
                'count': 10,
                'new': 2
            });
        });

        describe('count', function() {
            describe('count не указан', function() {
                beforeEach(function() {
                    this.result = this.mFolder.updateCounts({ new: 2 });
                    this.sinon.stub(this.mFolders, 'adjustAllUnreads');
                });

                it('не меняем поле .count', function() {
                    expect(this.mFolder.set).to.have.callCount(0);
                });

                it('не возвращаем diff.count', function() {
                    expect(this.result).to.not.have.keys('count');
                });
            });

            describe('count не изменился', function() {
                beforeEach(function() {
                    this.result = this.mFolder.updateCounts({ count: 10, new: 2 });
                    this.sinon.stub(this.mFolders, 'adjustAllUnreads');
                });

                it('не меняем поле .count', function() {
                    expect(this.mFolder.set).to.have.callCount(0);
                });

                it('возвращается diff.count = 0', function() {
                    expect(this.result.count).to.be.equal(0);
                });
            });

            describe('count изменился', function() {
                beforeEach(function() {
                    this.result = this.mFolder.updateCounts({ count: 13, new: 2 });
                    this.sinon.stub(this.mFolders, 'adjustAllUnreads');
                });

                it('меняем поле .count', function() {
                    expect(this.mFolder.set).to.have.callCount(1);
                    expect(this.mFolder.set).to.be.calledWith('.count', 13);
                });

                it('возвращается diff.count', function() {
                    expect(this.result.count).to.be.equal(3);
                });
            });
        });

        describe('new', function() {
            describe('new не указан', function() {
                beforeEach(function() {
                    this.result = this.mFolder.updateCounts({ count: 10 });
                    this.sinon.stub(this.mFolders, 'adjustAllUnreads');
                });

                it('не меняем поле .new', function() {
                    expect(this.mFolder.set).to.have.callCount(0);
                });

                it('не возвращаем diff.new', function() {
                    expect(this.result).to.not.have.keys('new');
                });
            });

            describe('new не изменился', function() {
                beforeEach(function() {
                    this.result = this.mFolder.updateCounts({ count: 10, new: 2 });
                    this.sinon.stub(this.mFolders, 'adjustAllUnreads');
                });

                it('не меняем поле .new', function() {
                    expect(this.mFolder.set).to.have.callCount(0);
                });

                it('возвращается diff.new = 0', function() {
                    expect(this.result.new).to.be.equal(0);
                });
            });

            describe('new изменился', function() {
                beforeEach(function() {
                    this.result = this.mFolder.updateCounts({ count: 10, new: 7 });
                    this.sinon.stub(this.mFolders, 'adjustAllUnreads');
                });

                it('меняем поле .new', function() {
                    expect(this.mFolder.set).to.have.callCount(1);
                    expect(this.mFolder.set).to.be.calledWith('.new', 7);
                });

                it('возвращается diff.new', function() {
                    expect(this.result.new).to.be.equal(5);
                });

                it('должен обновить общий счётчик непрочитанных, если это не спам или не удалённые', function() {
                    const mFolder2 = ns.Model.get('folder', { fid: '2' });
                    mFolder2.setData({
                        'count': 10,
                        'new': 2
                    });

                    this.sinon.stub(this.mFolders, 'spamOrTrash').returns(false);
                    mFolder2.updateCounts({ count: 10, new: 7 });

                    expect(this.mFolders.adjustAllUnreads)
                        .to.have.callCount(1)
                        .to.be.calledWith(5);
                });

                it('не должен обновлять общий счётчик непрочитанны, если это спам или удалённые', function() {
                    const mFolder2 = ns.Model.get('folder', { fid: '102' });
                    mFolder2.setData({
                        'count': 10,
                        'new': 2
                    });

                    this.sinon.stub(this.mFolders, 'spamOrTrash').returns(true);
                    mFolder2.updateCounts({ count: 10, new: 7 });

                    expect(this.mFolders.adjustAllUnreads)
                        .to.have.callCount(0);
                });
            });
        });
    });

    describe('#setHasUnread', function() {

        beforeEach(function() {

            this.mFolder1 = ns.Model.get('folder', {fid: '1'}).setData({
                'has-unread': true,
                'new': 0,
                'subfolder': ['2']
            });

            this.mFolder2 = ns.Model.get('folder', {fid: '2'}).setData({
                'has-unread': true,
                'new': 1,
                'parent_id': '1',
                'subfolder': []
            });

        });

        it('не должен сбросить флаг, если есть непрочитанные сообщения в дочерних папках', function() {
            this.mFolder1.setHasUnread(false);

            expect(this.mFolder1.get('.has-unread')).to.be.equal(true);
        });

    });

    describe('#setRecent', function() {

        beforeEach(function() {
            this.mFolder.setData({});
        });

        it('должен выставить флаг, если он false', function() {
            this.mFolder.set('.recent', false);
            this.sinon.spy(this.mFolder, 'set');
            this.mFolder.setRecent();

            expect(this.mFolder.set)
                .to.have.callCount(1)
                .and.to.be.calledWith('.recent', true);
        });

        it('не должен выставить флаг, если он true', function() {
            this.mFolder.set('.recent', true);
            this.sinon.spy(this.mFolder, 'set');
            this.mFolder.setRecent();

            expect(this.mFolder.set).to.have.callCount(0);
        });

    });

    describe('#preprocessData', function() {
        beforeEach(function() {
            this.sinon.stub(this.mFolder, '_messagesInvalidate');
        });

        describe('Инвалидация модели messages', function() {
            it('Если корп, обновление данных и изменилось количество новых писем', function() {
                this.mFolder.setData({ 'new': 1 });
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.mFolder.preprocessData({ 'new': 0 });

                expect(this.mFolder._messagesInvalidate).to.have.callCount(1);
            });

            it('Если корп, обновление данных и изменилось общее количество писем', function() {
                this.mFolder.setData({ 'count': 1 });
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.mFolder.preprocessData({ 'count': 2 });

                expect(this.mFolder._messagesInvalidate).to.have.callCount(1);
            });

            it('Если не корп, ничего не делаем', function() {
                this.mFolder.setData({ 'count': 1 });
                this.sinon.stub(Daria, 'IS_CORP').value(false);
                this.mFolder.preprocessData({ 'count': 2 });

                expect(this.mFolder._messagesInvalidate).to.have.callCount(0);
            });

            it('Если начальная инициализация модели, ничего не делаем', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.mFolder.preprocessData({ 'count': 2 });

                expect(this.mFolder._messagesInvalidate).to.have.callCount(0);
            });

            it('Если не изменилось общее количество или количество новых, ничего не делаем', function() {
                this.mFolder.setData({ 'count': 1, 'new': 1 });
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.mFolder.preprocessData({ 'count': 1, 'new': 1 });

                expect(this.mFolder._messagesInvalidate).to.have.callCount(0);
            });
        });
    });

    describe('#_messagesInvalidate', function() {
        beforeEach(function() {
            this.sinon.spy(ns.Model, 'invalidate');
        });

        it('Должен вызвать инвализацию моделей messages-item-state для папки', function() {
            var mMessagesItemState = ns.Model.get('messages-item-state', { 'current_folder': '1' });
            this.sinon.stub(mMessagesItemState, 'invalidate');
            this.sinon.stub(ns.page.current, 'params').value({
                'current_folder': '2'
            });

            this.mFolder._messagesInvalidate();
            var call = ns.Model.invalidate.getCall(1);
            expect(call.args[0] === 'messages-item-state').to.be.ok;
            expect(mMessagesItemState.invalidate).to.have.callCount(1);
        });

        it('Должен вызвать инвалидацию моделей messages для папки и для всех messages тредов внутри папки', function() {
            var mMessages = ns.Model.get('messages', { 'current_folder': '1' });
            this.sinon.stub(mMessages, 'invalidate');

            var mMessagesThread = ns.Model.get('messages', { 'thread_id': 't1' });
            this.sinon.stub(mMessagesThread, 'invalidate');

            mMessages.insert(ns.Model.get('message', { ids: '1' }).setData({ 'mid': '1', 'tid': 't1' }));

            this.mFolder._messagesInvalidate();

            var call = ns.Model.invalidate.getCall(0);
            expect(call.args[0] === 'messages').to.be.ok;
            expect(mMessages.invalidate).to.have.callCount(1);
            expect(mMessagesThread.invalidate).to.have.callCount(1);
        });

        it('не должен выполнять никакую инвалидацию для текущей папки', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                'current_folder': '1'
            });

            this.mFolder._messagesInvalidate();
            expect(ns.Model.invalidate).to.have.callCount(0);
        });
    });
});
