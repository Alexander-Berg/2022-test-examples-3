describe('Daria.Page', function() {
    describe('#fixEmptyImage', function() {
        beforeEach(function() {
            this.$imgParent = $('<div></div>');
            this.$img = $('<img width="1"/>').appendTo(this.$imgParent);

            this.sinon.stub($.fn, 'width').returns(1);
            this.sinon.spy(ns.events, 'trigger');
        });

        it('должен задавать изображению атрибут onload равный $.noop', function() {
            Daria.Page.fixEmptyImage(this.$img[0]);

            expect(this.$img[0].onload).to.be.equal($.noop);
        });

        describe('при вызове с параметром `type = message-right`', function() {
            it('должен добавлять классы `g-hidden error` родителю', function() {
                Daria.Page.fixEmptyImage(this.$img[0], 'message-right');

                expect(this.$imgParent.hasClass('g-hidden error')).to.be.equal(true);
            });

            it('должен вызывать событие `message-right.avatar-error` c нодой изображения в качестве параметра', function() {
                Daria.Page.fixEmptyImage(this.$img[0], 'message-right');

                expect(ns.events.trigger).to.be.calledWith('message-right.avatar-error', this.$img[0]);
            });
        });

        describe('при вызове с параметром `type = message-right-single`', function() {
            it('должен задавать изображению src равный результату вызова Jane.getEntity с параметром `unknown-50.png`', function() {
                Daria.Page.fixEmptyImage(this.$img[0], 'message-right-single');

                expect(this.$img.attr('src')).to.contain('unknown-50.png');
            });
        });

        describe('при вызове с параметром `type = message-right-link`', function() {
            it('должен задавать изображению src равный результату вызова Jane.getEntity с параметром `external.png`', function() {
                Daria.Page.fixEmptyImage(this.$img[0], 'message-right-link');

                expect(this.$img.attr('src')).to.contain('external.png');
            });
        });

        describe('при вызове с параметром `type = collectors`', function() {
            it('должен задавать изображению src равный результату вызова Jane.getEntity с параметром `mailbox.png`', function() {
                Daria.Page.fixEmptyImage(this.$img[0], 'collectors');

                expect(this.$img.attr('src')).to.contain('mailbox.png');
            });
        });

        describe('при вызове без параметров', function() {
            it('должен добавлять изображению класс `g-hidden`', function() {
                Daria.Page.fixEmptyImage(this.$img[0]);

                expect(this.$img.hasClass('g-hidden')).to.be.equal(true);
            });
        });
    });

    describe('#isParamsForSingleThread', function() {
        it('должен вернуть true, если параметры для треда на отдельной странице', function() {
            expect(Daria.Page.isParamsForSingleThread({ thread_id: 't1' })).to.be.equal(true);
        });

        it('должен вернуть true, если параметры для всех писем треда на отдельной странице', function() {
            expect(Daria.Page.isParamsForSingleThread({ thread_id: 't1', full: 'true' })).to.equal(true);
        });

        it('должен вернуть false, если параметры не для треда на отдельной странице', function() {
            expect(Daria.Page.isParamsForSingleThread({ current_folder: '12', ids: '3' })).to.be.equal(false);
        });
    });
});

describe('Daria.Page.generateUrl', function() {
    describe('.closeMessage', function() {
        beforeEach(function() {
            this.sinon.stub(ns.router, 'generateUrl');
        });

        it('должен сделать правильную ссылку для треда на отдельной странице', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                ids: '1',
                thread_id: 't1'
            });

            Daria.Page.generateUrl.closeMessage();

            expect(ns.router.generateUrl)
                .to.have.callCount(1)
                .and.to.be.calledWith('messages', { thread_id: 't1' });
        });

        it('должен сделать правильную ссылку для списка писем, если было открыто письмо', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '12',
                ids: '1'
            });

            Daria.Page.generateUrl.closeMessage();

            expect(ns.router.generateUrl)
                .to.have.callCount(1)
                .and.to.be.calledWith('messages', { current_folder: '12' });
        });

        it('должен сделать правильную ссылку для списка писем, если был открыт тред', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '12',
                thread_id: 't1'
            });

            Daria.Page.generateUrl.closeMessage();

            expect(ns.router.generateUrl)
                .to.have.callCount(1)
                .and.to.be.calledWith('messages', { current_folder: '12' });
        });
    });

    describe('.contentMessage2pane', function() {
        beforeEach(function() {
            ns.router.init();
        });

        it('должен вернуть ссылку на письмо для тредизированного письма', function() {
            this.sinon.stub(Daria.Page.generateUrl, 'contentMessageCompose').returns(false);

            ns.Model.get('messages', { thread_id: 't1' }).setData({
                message: [
                    { mid: '2', tid: 't1' }
                ]
            });

            var expectUrl = ns.router.generateUrl('message', { ids: '2' });

            expect(Daria.Page.generateUrl.contentMessage2pane({ mid: 't1', count: 1 })).to.be.equal(expectUrl);
        });

        it('должен вернуть ссылку на тред, если это тред', function() {
            this.sinon.stub(Daria.Page.generateUrl, 'contentMessageCompose').returns(false);

            var expectUrl = ns.router.generateUrl('messages', { thread_id: 't1' });

            expect(Daria.Page.generateUrl.contentMessage2pane({ mid: 't1', count: 2 })).to.be.equal(expectUrl);
        });

        it('должен вернуть ссылку на письмо, если это письмо', function() {
            this.sinon.stub(Daria.Page.generateUrl, 'contentMessageCompose').returns(false);

            var expectUrl = ns.router.generateUrl('message', { ids: '1' });

            expect(Daria.Page.generateUrl.contentMessage2pane({ mid: '1', count: 1 })).to.be.equal(expectUrl);
        });

        describe('Почти фейковый тред ->', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.Page.generateUrl, 'contentMessageCompose').returns(false);
                this.sinon.stub(ns.page.current, 'params').value({ current_folder: '1' });

                this.sinon.stub(Jane.ErrorLog, 'send');

                this.url = Daria.Page.generateUrl.contentMessage2pane({ mid: 't' }, {});
            });

            it('логгирует ошибку', function() {
                expect(Jane.ErrorLog.send).to.have.callCount(1);
                expect(Jane.ErrorLog.send).to.be.calledWith({
                    message: JSON.stringify({ mid: 't' }),
                    pageParams: JSON.stringify(ns.page.current.params),
                    type: 'fake_thread_url_error'
                });
            });

            it('возвращает пустую ссыль', function() {
                expect(this.url).to.be.equal('');
            });
        });
    });

    describe('#_isMessagesPageWithFolder', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(Daria, 'is2pane').returns(false);

            var mFolders = ns.Model.get('folders');
            setModelByMock(mFolders);

            ns.router.buildFoldersUrls();
            ns.router.init();
        });

        it('При измении роутов для лейаута messages нужно добавлять новые проверки', function() {
            /**
             * ВАЖНО!!! Если меняются роуты для messages, то нужно и добавлять проверки в _isMessagesPageWithFolder
             * чтобы определять, нужно ли вычислять директорию при формировании урла
             */
            var routes = ns.router.routes.route;

            var messagesRoutes = Object.keys(routes).filter(function(urlPattern) {
                return routes[urlPattern] === 'messages';
            });

            expect(messagesRoutes).to.be.eql([
                '#{attachments==attachments}/{ids:int}',
                '#{attachments==attachments}',
                '#{unread==unread}/{ids:int}',
                '#{unread==unread}',
                '#{important==important}/{ids:int}',
                '#{important==important}',
                '#{remind==remind}/{ids:int}',
                '#{remind==remind}',
                '#{priority==priority}/{ids:int}',
                '#{priority==priority}',
                '#tabs/{tabId:folders-tabs}/message/{ids:int}',
                '#tabs/{tabId:folders-tabs}/thread/{thread_id:TThreadID}',
                '#tabs/{tabId:folders-tabs}',
                '#{search==search}/{thread_id:TThreadID}',
                '#{search==search}/{ids:int}',
                '#{search==search}',
                '#{default_folder:TFolderDefault}/message/{ids:int}',
                '#{default_folder:TFolderDefault}/thread/{thread_id:TThreadID}',
                '#folder/{current_folder:int}/message/{ids:int}',
                '#folder/{current_folder:int}/thread/{thread_id:TThreadID}',
                '#folder/{current_folder:int}/push/{tabId:folders-tabs}/message/{ids:int}',
                '#folder/{current_folder:int}/push/{tabId:folders-tabs}/thread/{thread_id:TThreadID}',
                '#folder/{current_folder:int}/push/{tabId:folders-tabs}',
                '#label/{current_label:int}/message/{ids:int}',
                '#label/{current_label:int}/thread/{thread_id:TThreadID}',
                '#folder/{current_folder:int}',
                '#label/{current_label:int}',
                '#thread/{thread_id:TThreadID}/message/{ids:int}',
                '#thread/{thread_id:TThreadID}'
            ]);
        });

        var messagesParams = [ {
            name: 'Письма с аттачами',
            url: '#attachments',
            result: false
        }, {
            name: 'Письма с аттачами с id сообщения',
            url: '#attachments/123',
            result: false
        }, {
            name: 'Не прочитанные письма',
            url: '#unread',
            result: false
        }, {
            name: 'Не прочитанные письма с id сообщения',
            url: '#unread/123',
            result: false
        }, {
            name: 'Важные письма',
            url: '#important',
            result: false
        }, {
            name: 'Важные письма с id сообщения',
            url: '#important/123',
            result: false
        }, {
            name: 'Письма ждущие ответа',
            url: '#remind',
            result: false
        }, {
            name: 'Письма ждущие ответа с id сообщения',
            url: '#remind/123',
            result: false
        }, {
            name: 'Письма в поиске',
            url: '#search',
            result: false
        }, {
            name: 'Письма в поиске c id треда',
            url: '#search/t123',
            result: false
        }, {
            name: 'Письма в поиске c id письма',
            url: '#search/123',
            result: false
        }, {
            name: 'Письма по метке',
            url: '#label/123',
            result: false
        }, {
            name: 'Письма по метке c id сообщения',
            url: '#label/123/message/123',
            result: false
        }, {
            name: 'Письма по метке c id треда',
            url: '#label/123/thread/t123',
            result: false
        }, {
            name: 'Письма в кастомной папке',
            url: '#folder/2',
            redirect: false,
            result: true
        }, {
            name: 'Письма в кастомной папке c id сообщения',
            url: '#folder/2/message/123',
            result: true
        }, {
            name: 'Письма в кастомной папке c тредом',
            url: '#folder/2/thread/t123',
            result: true
        }, {
            name: 'Письма в стандартной папке',
            url: '#inbox',
            redirect: false,
            result: true
        }, {
            name: 'Письма в стандартной папке c id сообщения',
            url: '#inbox/message/123',
            result: true
        }, {
            name: 'Письма в стандартной папке c тредом',
            url: '#inbox/thread/t123',
            result: true
        }, {
            name: 'Письма треда',
            url: '#thread/t123',
            result: true
        }, {
            name: 'Письма в поиске c id треда и id сообщения',
            url: '#thread/t123/message/123',
            result: true
        }, {
            name: 'Письма с потерявшимя current_folder, но c id сообщения',
            url: '#message/123',
            result: true
        }, {
            name: 'Письма таба Входящие',
            url: '#tabs/relevant',
            result: true
        }, {
            name: 'Ссылка на тредное письмо пуша в отдельной вкладке',
            url: '#folder/1/push/news/thread/t1',
            result: true
        }, {
            name: 'Ссылка на письмо пуша в отдельной вкладке',
            url: '#folder/1/push/news/message/1',
            result: true
        }
        ];

        messagesParams.forEach(function(options) {
            it(options.name + ' - ' + (options.result ? 'true' : 'false'), function() {
                var route = ns.router(options.url);
                var params;

                if (options.redirect) {
                    params = ns.router(route.redirect).params;
                } else {
                    params = route.params;
                }

                expect(Daria.Page.generateUrl._isMessagesPageWithFolder(params)).to.be.equal(options.result);
            });
        });
    });

    describe('.contentMessage3pane', function() {
        beforeEach(function() {
            ns.router.init();
            this.mFolders = ns.Model.get('folders');
            setModelByMock(this.mFolders);

            this.sinon.stub(Jane.ErrorLog, 'send');
        });

        it('должен сгенерировать ссылку с пейджером по датам', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '11',
                datePager: '06.2015'
            });

            var expectUrl = ns.router.generateUrl('messages', {
                current_folder: '11',
                datePager: '06.2015',
                ids: '1'
            });

            expect(Daria.Page.generateUrl.contentMessage3pane({ mid: '1', count: 1 })).to.be.equal(expectUrl);
        });

        it('должен сгенерировать ссылку для "только непрочитанные"', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                current_folder: '11',
                extra_cond: 'only_new'
            });

            var expectUrl = ns.router.generateUrl('messages', {
                current_folder: '11',
                extra_cond: 'only_new',
                ids: '1'
            });

            expect(Daria.Page.generateUrl.contentMessage3pane({ mid: '1', count: 1 })).to.be.equal(expectUrl);
        });

        it('должен сгенерировать ссылку для треда и письма', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                ids: '1',
                thread_id: 't1'
            });

            var expectUrl = ns.router.generateUrl('messages', {
                ids: '1',
                thread_id: 't1'
            });

            // Стабим метод логирования иначе он после первого вызова превращается в тыкву.
            // TODO хорошо бы переписать, потому что сейчас тест ниже зависит от этого теста.
            this.sinon.stub(Daria.Page.generateUrl, '_logFolderNotFoundOnce');

            expect(Daria.Page.generateUrl.contentMessage3pane({ mid: '1', count: 1 })).to.be.equal(expectUrl);
        });

        describe('параметр folder в урле', function() {
            // Такой монструозный тест пришлось написать из-за того, что метод логирования
            // обёрнут в _.once и вызывается один раз.
            it('модель folder не найдена', function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '9999',
                    thread_id: '1',
                    threaded: 'yes'
                });

                var url = Daria.Page.generateUrl.contentMessage3pane({ mid: 't1', count: 2 });

                // current_folder берётся из ns.page.current.params
                expect(url).to.be.equal('#folder/9999/thread/t1');

                // Логируем в monitoring.txt исключительную ситуацию.
                expect(Jane.ErrorLog.send).to.have.callCount(1);

                // Логируется много всего полезного.
                expect(Jane.ErrorLog.send).to.have.calledWith({
                    errorType: 'debug.folder-not-found',
                    current_folder: '9999',
                    mFoldersStatus: 'ok',
                    mFoldersData: JSON.stringify(this.mFolders.getData())
                });

                // Исключительная ситуация логируется только один раз.
                Daria.Page.generateUrl.contentMessage3pane({ mid: 't1', count: 2 });

                expect(Jane.ErrorLog.send).to.have.callCount(1);
            });

            it('модель folder найдена, ns.page.current.params.current_folder - символическое название папки', function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: 'archive',
                    thread_id: '1',
                    threaded: 'yes'
                });

                expect(Daria.Page.generateUrl.contentMessage3pane({ mid: 't1', count: 2 })).to.be.equal('#archive/thread/t1');
            });

            it('модель folder найдена, ns.page.current.params.current_folder - fid папки', function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    current_folder: '11',
                    thread_id: '1',
                    threaded: 'yes'
                });

                expect(Daria.Page.generateUrl.contentMessage3pane({ mid: 't1', count: 2 })).to.be.equal('#folder/11/thread/t1');
            });
        });

        describe('только параметр thread_id в урле ->', function() {
            beforeEach(function() {
                this.sinon.stub(ns.page.current, 'params').value({
                    thread_id: '1'
                });

                this.mFolders = ns.Model.get('folders');
                this.mMessageData = { mid: 't1', count: 2, fid: '2' };

                this.sinon.spy(Daria.Page.generateUrl, '_logCurrentFolderLost');
            });

            it('пытаемся вычислить папку при генерации урла', function() {
                this.sinon.spy(Daria.Page.generateUrl, '_fixFolderInParams');
                Daria.Page.generateUrl.contentMessage3pane(this.mMessageData);
                expect(Daria.Page.generateUrl._fixFolderInParams).to.have.callCount(1);
            });

            it('вычислили current_folder -> продолжаем генерировать ссылку', function() {
                var that = this;
                this.sinon.stub(Daria.Page.generateUrl, '_fixFolderInParams').callsFake(function(newPageParams, current_folder, mMessageData) {
                    newPageParams.current_folder = that.mMessageData.fid;
                });
                Daria.Page.generateUrl.contentMessage3pane(this.mMessageData);
                expect(Daria.Page.generateUrl._logCurrentFolderLost).to.have.callCount(0);
            });

            it('вычислили default_folder -> продолжаем генерировать ссылку', function() {
                var that = this;
                this.sinon.stub(Daria.Page.generateUrl, '_fixFolderInParams').callsFake(function(newPageParams, current_folder, mMessageData) {
                    newPageParams.default_folder = that.mMessageData.fid;
                });
                Daria.Page.generateUrl.contentMessage3pane(this.mMessageData);
                expect(Daria.Page.generateUrl._logCurrentFolderLost).to.have.callCount(0);
            });

            it('не вычислили папку + в ns.page.current.params нет current_folder + в данных сообщения есть fid -> логируем ошибку', function() {
                this.sinon.stub(Daria.Page.generateUrl, '_fixFolderInParams');
                Daria.Page.generateUrl.contentMessage3pane(this.mMessageData);
                expect(Daria.Page.generateUrl._logCurrentFolderLost).to.have.callCount(1);
            });

            it('не вычислили папку + в ns.page.current.params нет current_folder + в данных сообщения есть fid -> логируем ошибку -> параметры', function() {
                this.sinon.stub(Daria.Page.generateUrl, '_fixFolderInParams');
                Daria.Page.generateUrl.contentMessage3pane(this.mMessageData);

                expect(Jane.ErrorLog.send).to.have.calledWith({
                    errorType: 'current_folder_lost',
                    pageParams: JSON.stringify(ns.page.current.params),
                    mFoldersStatus: this.mFolders.status,
                    messageMid: this.mMessageData.mid,
                    messageFid: this.mMessageData.fid
                });
            });

            describe('Фейковый тред ->', function() {
                it('передаем 2 аргументом папку - она добавляется в ссыль', function() {
                    var fakeThreadData = ns.Model.get('message', { ids: 't' }).getData();
                    var url = Daria.Page.generateUrl.contentMessage3pane(fakeThreadData, {
                        current_folder: '123'
                    });
                    expect(url).to.be.equal('');
                });
            });
        });

        describe('в параметрах нет current_folder и thread_id ->', function() {
            it('Если есть message.fid, то должен подставить его, в качестве директории', function() {
                this.sinon.stub(ns.page.current, 'params').value({});
                this.mMessageData = { mid: 't1', count: 2, fid: '2' };

                var url = Daria.Page.generateUrl.contentMessage3pane(this.mMessageData);
                expect(url).to.be.equal('#spam/thread/t1');
            });
        });

        describe('._fixFolderInParams', function() {
            beforeEach(function() {
                this.sinon.stub(Daria.Page.generateUrl, '_logFolderNotFoundOnce');
                this.mFolders = ns.Model.get('folders');
                this.mMessageData = { mid: 't1', count: 2, fid: '55' };
            });

            describe('ns.page.params.current_folder непустой', function() {
                describe('-> ищем folder', function() {
                    it('-> по FID', function() {
                        this.sinon.spy(this.mFolders, 'getFolderById');
                        Daria.Page.generateUrl._fixFolderInParams({}, '1', this.mMessageData);
                        expect(this.mFolders.getFolderById).to.have.callCount(1);
                    });

                    it('-> по символическому имени', function() {
                        this.sinon.spy(this.mFolders, 'getFolderBySymbol');
                        Daria.Page.generateUrl._fixFolderInParams({}, 'spam');
                        expect(this.mFolders.getFolderBySymbol).to.have.callCount(1);
                    });
                });

                it('-> не нашли folder -> ищем по fid из данных модели', function() {
                    this.sinon.stub(this.mFolders, 'getFolderBySymbol').returns(undefined);
                    this.sinon.stub(this.mFolders, 'getFolderById')
                        .withArgs('3').returns(undefined)
                        .withArgs('55').returns({ fid: '55' });

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, '3', this.mMessageData);

                    expect(this.mFolders.getFolderById).to.have.callCount(2);
                    expect(nextParams).to.be.eql({ current_folder: '55' });
                });
            });

            describe('ns.page.params.current_folder пустой', function() {
                it('-> ищем folder по FID из данных модели', function() {
                    this.sinon.stub(this.mFolders, 'getFolderBySymbol').returns(undefined);
                    this.sinon.stub(this.mFolders, 'getFolderById').withArgs('55').returns({ fid: '55' });

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, null, this.mMessageData);

                    expect(this.mFolders.getFolderBySymbol).to.have.callCount(0);
                    expect(this.mFolders.getFolderById).to.have.callCount(1);
                    expect(this.mFolders.getFolderById.getCall(0).args[0]).to.be.eql(this.mMessageData.fid);
                });
            });

            describe('не нашли folder', function() {
                it('-> ставим ns.page.current_folder если не пустой', function() {
                    this.sinon.stub(this.mFolders, 'getFolderBySymbol').returns(undefined);
                    this.sinon.stub(this.mFolders, 'getFolderById').returns(undefined);

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, '66', this.mMessageData);

                    expect(nextParams).to.be.eql({ current_folder: '66' });
                });

                it('-> не ставим ns.page.current_folder если пустой', function() {
                    this.sinon.stub(this.mFolders, 'getFolderBySymbol').returns(undefined);
                    this.sinon.stub(this.mFolders, 'getFolderById').returns(undefined);

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, null, this.mMessageData);

                    expect(nextParams).to.be.eql({});
                });

                it('-> логируем ошибку один раз', function() {
                    this.sinon.stub(this.mFolders, 'getFolderBySymbol').returns(undefined);
                    this.sinon.stub(this.mFolders, 'getFolderById').returns(undefined);

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, null, this.mMessageData);

                    expect(Daria.Page.generateUrl._logFolderNotFoundOnce).to.have.callCount(1);
                });
            });

            describe('нашли folder', function() {
                it('-> устанавливаем default_folder = symbol, если у folder-а есть символическое имя', function() {
                    this.sinon.stub(this.mFolders, 'getFolderById').returns(undefined);
                    this.sinon.stub(this.mFolders, 'getFolderBySymbol').returns({ fid: '1', symbol: 'inbox' });

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, '1', this.mMessageData);

                    expect(nextParams).to.be.eql({ default_folder: 'inbox' });
                });

                it('-> устанавливаем current_folder = fid, если у folder-а нет символического имени', function() {
                    this.sinon.stub(this.mFolders, 'getFolderById').returns({ fid: '33' });

                    var nextParams = {};
                    Daria.Page.generateUrl._fixFolderInParams(nextParams, '33', this.mMessageData);

                    expect(nextParams).to.be.eql({ current_folder: '33' });
                });
            });
        });
    });
});
