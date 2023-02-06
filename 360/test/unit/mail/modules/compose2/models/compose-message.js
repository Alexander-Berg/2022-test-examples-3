describe('Daria.mComposeMessage', function() {
    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        ns.Model.get('signs').setData(mock.signs[0].data);

        /** @type Daria.mComposeMessage */
        this.model = ns.Model.get('compose-message', {});
        this.sinon.stub(this.model, '_getMessageType');
        this.sinon.stub(this.model, 'mComposeState').value({ get: this.sinon.stub().withArgs('.recipients-diff-pane-close').returns(false) });
        this.sinon.stub(Jane.ErrorLog, 'send');

        this.mMessage = ns.Model.get('message', { ids: '5' });
        this.sinon.stubMethods(this.mMessage, [
            'getFolderId',
            'getFromName',
            'getFromEmail',
            'getSubject',
            'getToEmail',
            'isDraftLike'
        ]);

        this.mMessageBody = ns.Model.get('message-body', { ids: '5' });
        this.sinon.stubMethods(this.mMessageBody, [
            'getSubtype',
            'getDraftBody',
            'getAddressField',
            'getInReplyTo',
            'getReferences',
            'getRecipients',
            'getMessageId',
            'getInfo',
            'getReplyBody',
            'getForwardBody'
        ]);

        this.mSettings = ns.Model.get('settings');
        this.sinon.stubMethods(this.mSettings, [
            'getSetting'
        ]);

        this.mComposePredefinedData = ns.Model.get('compose-predefined-data');

        this.model.mComposePredefinedData = this.mComposePredefinedData;
        this.model.mMessage = this.mMessage;
        this.model.mMessageBody = this.mMessageBody;
        this.model.mSettings = this.mSettings;

        this.model._initFromModels([
            this.mComposePredefinedData,
            this.mMessage,
            this.mMessageBody
        ]);

        this.sinon.stubMethods(this.model, [
            'setData',
            '_getFromEmail',
            '_prepareDataForMailSend'
        ]);

        this.sinon.stub($, 'ajax').returns($.Deferred().resolve());

        this.models = [
            this.mSettings,
            this.mComposePredefinedData,
            this.mMessage,
            this.mMessageBody
        ];

        this.checkKeyValue = function(key, value) {
            expect(this.model.setData).to.be.calledWith(sinon.match.has(key, value));
        };

        this.sinon.stub(this.model, 'needCollapsedQuotedMessage').returns(true);

        const forcedRequestMock = Promise.resolve([]);
        forcedRequestMock.always = forcedRequestMock.then;
        this.sinon.stub(ns, 'forcedRequest').returns(forcedRequestMock);

        this.sinon.stub(Daria, 'now').returns(123654789000);
    });

    describe('#_onDestroyed', function() {
        beforeEach(function() {
            this.model._isDirty = true;
            this.model._cancelled = true;
            this.model._cancelSendCallback = _.noop;
            this.model._isIgnoreUndo = true;

            this.model._onDestroyed();
        });

        it('Должен сбросить признак "данные изменены"', function() {
            expect(this.model._isDirty).to.be.equal(false);
        });

        it('Должен сбросить признак "была отмена отправки"', function() {
            expect(this.model._cancelled).to.be.equal(false);
        });

        it('Должен обнулить "колбек быстрой отмены"', function() {
            expect(this.model._cancelSendCallback).to.be.equal(null);
        });

        it('Должен сбросить признак "игнорировать отложенную отправку"', function() {
            expect(this.model._isIgnoreUndo).to.be.equal(false);
        });
    });

    describe('#set', function() {
        var dirtyFields = [ 'to', 'cc', 'bcc', 'subj', 'send', 'from_mailbox', 'from_name',
            'ttype', 'att_ids', 'parts', 'disk_att', 'save_symbol', 'lids' ];

        var cleanFields = [ 'ign_overwrite', 'overwrite' ];

        var undoLogFields = {
            'to': 'редактирование письма/получатель (To:)',
            'subj': 'редактирование письма/тема',
            'send': 'редактирование письма/текст',
            'parts': 'редактирование письма/аттачи',
            'disk_att': 'редактирование письма/аттачи',
            'parts_json': 'редактирование письма/аттачи'
        };

        var undoDoNotLogFields = [ 'cc', 'bcc', 'from_mailbox', 'from_name', 'ttype', 'att_ids', 'save_symbol',
            'lids', 'ign_overwrite', 'overwrite' ];

        beforeEach(function() {
            this.model.markSaved();

            this.undoLogger = {
                logActionOnceAfterCancel: this.sinon.stub()
            };

            this.sinon.stub(Daria.SendMail, 'getUndoSendLogger').returns(this.undoLogger);
        });

        describe('Изменение поля приводит к необходимости сохранения модели →', function() {
            _.each(dirtyFields, function(field) {
                it(field, function() {
                    this.model.set('.' + field, '42');

                    expect(this.model.isDirty()).to.be.ok;
                });
            });

            _.each(cleanFields, function(field) {
                it(field, function() {
                    this.model.set('.' + field, '42');

                    expect(this.model.isDirty()).to.not.be.ok;
                });
            });
        });

        describe('Должны логироваться изменения некоторых полей после отмены отправки  →', function() {
            Object.keys(undoLogFields).forEach(function(field) {
                it(`${field} логируется как "${undoLogFields[field]}"`, function() {
                    this.model.set('.' + field, '42');

                    expect(this.undoLogger.logActionOnceAfterCancel)
                        .to.have.been.calledWithExactly(undoLogFields[field]);
                });
            });

            undoDoNotLogFields.forEach(function(field) {
                it(`${field} не должен логироваться`, function() {
                    this.model.set('.' + field, '42');

                    expect(this.undoLogger.logActionOnceAfterCancel).to.have.callCount(0);
                });
            });
        });

        it('Вызывает ns.Model.prototype.set', function() {
            this.sinon.stub(ns.Model.prototype, 'set');

            this.model.set('.smth', '42');

            expect(ns.Model.prototype.set).to.have.callCount(1);
        });
    });

    describe('#setData', function() {
        beforeEach(function() {
            this.model.setData.restore();
        });

        it('Любой вызов приводит к необходимости сохранения модели', function() {
            this.sinon.stub(ns.Model.prototype, 'setData');

            this.model.setData({ smth: '42' });

            expect(this.model.isDirty()).to.be.ok;
        });

        it('Вызывает ns.Model.prototype.setData', function() {
            this.sinon.stub(ns.Model.prototype, 'setData');

            this.model.setData({ smth: '42' });

            expect(ns.Model.prototype.setData).to.have.callCount(1);
        });
    });

    describe('#_constructModelRequest', function() {
        it('Возвращает только запрос настроек и предопределенных данных, если нет this.param.ids', function() {
            this.sinon.stub(this.model, 'params').value({ oper: 'forward' });

            expect(this.model._constructModelRequest()).to.be.eql([
                { id: 'settings' },
                { id: 'compose-predefined-data' }
            ]);
        });

        it('Возвращает только запрос настроек и предопределенных данных, если есть this.param.tids', function() {
            this.sinon.stub(this.model, 'params').value({ oper: 'forward', tids: '42' });

            expect(this.model._constructModelRequest()).to.be.eql([
                { id: 'settings' },
                { id: 'compose-predefined-data' }
            ]);
        });

        it('Возвращает запрос письма, тела письма, настроек и предопределенных данных, если есть this.param.ids и нет this.params.tids', function() {
            this.sinon.stub(this.model, 'params').value({ oper: 'forward', ids: '42' });
            this.messageBodyParams = { draft: true };
            this.sinon.stub(ns.Model.info('message-body'), 'calculateParamsForMessageBody')
                .returns(this.messageBodyParams);

            expect(this.model._constructModelRequest()).to.be.eql([
                { id: 'settings' },
                { id: 'compose-predefined-data' },
                { id: 'message', params: { ids: '42' } },
                { id: 'message-body', params: this.messageBodyParams }
            ]);
        });
    });

    describe('#request', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [
                '_initFromModels'
            ]);
        });

        describe('Запрос подмоделей нужен (например, ids есть в параметрах)', function() {
            it('Должен запросить модели по сформированному запросу', function() {
                var request = [ { id: 'settings' } ];
                this.sinon.stub(this.model, '_constructModelRequest').returns(request);
                this.sinon.stub(ns, 'request').callsFake(() => vow.reject());

                this.model.request().then(() => {
                    expect(ns.forcedRequest).to.be.calledWith(request);
                });
            });

            it('Должен запустить инициализацию модели из запрошенных моделей', function() {
                return this.model.request().then(() => {
                    expect(this.model._initFromModels).to.have.callCount(1);
                });
            });
        });
    });

    describe('#_initFromModels', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [
                'markSaved',
                'isDraft',
                'isReplyAny',
                'isTemplate',
                'isNewMessage'
            ]);
        });

        describe('Сохранение ссылок на модели →', function() {
            beforeEach(function() {
                this.model.mSettings = this.model.mComposePredefinedData =
                    this.model.mMessage = this.model.mMessageBody = null;

                this.model._initFromModels(this.models);
            });

            it('Должен сохранить ссылку на модель settings', function() {
                expect(this.model.mSettings).to.be.equal(this.models[0]);
            });

            it('Должен сохранить ссылку на модель compose-predefined-data', function() {
                expect(this.model.mComposePredefinedData).to.be.equal(this.models[1]);
            });

            it('Должен сохранить ссылку на модель message, если в параметрах есть ids', function() {
                expect(this.model.mMessage).to.be.equal(this.models[2]);
            });

            it('Должен сохранить ссылку на модель message, если в параметрах есть ids', function() {
                expect(this.model.mMessageBody).to.be.equal(this.models[3]);
            });
        });

        it('Должен пометить данные сохраненными', function() {
            this.model._initFromModels(this.models);

            expect(this.model.markSaved).to.have.callCount(1);
        });
    });

    describe('#_addTextFromMessageBody', function() {
        beforeEach(function() {
            this.getTtypeStub = this.sinon.stub(this.model, 'get').withArgs('.ttype');
            this.model.get.withArgs('.send').returns('compose text');
            this.setSendStub = this.sinon.stub(this.model, 'set').withArgs('.send');
            this.mMessageBody = ns.Model.get('message-body', { ids: '5' });
            this.sinon.stub(this.mMessageBody, 'getComposeHTML').returns('some text from template');
        });

        it('Добавляет текст в HTML-режиме', function() {
            this.getTtypeStub.returns('html');
            this.model.addTextFromMessageBody(this.mMessageBody);
            expect(this.setSendStub).to.be.calledWith('.send', 'some text from template<br>compose text');
        });

        it('Добавляет текст в режиме без форматирования', function() {
            this.getTtypeStub.returns('plain');
            this.model.addTextFromMessageBody(this.mMessageBody);
            expect(this.setSendStub).to.be.calledWith('.send', 'some text from template\ncompose text');
        });
    });

    describe('#_setRecepientsFromMessageBody', function() {
        beforeEach(function() {
            this.mMessageBody = ns.Model.get('message-body', { ids: '5' });
            this.mMessageBody.getAddressField.withArgs('to').returns('aaa@to.mail');
            this.mMessageBody.getAddressField.withArgs('cc').returns('bbb@cc.mail');
            this.mMessageBody.getAddressField.withArgs('bcc').returns('bcc@bcc.mail');
            this.sinon.stub(this.model, 'set').withArgs(sinon.match(function(arg) {
                return [ '.to', '.cc', '.bcc' ].indexOf(arg) > -1;
            }));
            this.sinon.stub(this.model, 'get').withArgs('.cc').withArgs('.bcc');
            this.sinon.stub(this.model, 'mComposeState').value({
                set: this.sinon.stub().withArgs('.isCcVisible').withArgs('.isBccVisible')
            });
        });

        it('Должен заполнить поля адресатов', function() {
            this.model.setRecepientsFromMessageBody(this.mMessageBody);
            expect(this.model.set).to.be.calledWith('.to', 'aaa@to.mail');
            expect(this.model.set).to.be.calledWith('.cc', 'bbb@cc.mail');
            expect(this.model.set).to.be.calledWith('.bcc', 'bcc@bcc.mail');
        });

        it('Должен раскрыть на форме поля Копия и Скрытая копия после их заполнения', function() {
            this.model.get.withArgs('.cc').returns('aaa@to.mail');
            this.model.get.withArgs('.bcc').returns('bbb@to.mail');
            this.model.setRecepientsFromMessageBody(this.mMessageBody);
            expect(this.model.mComposeState.set).to.be.calledWith('.isCcVisible', true);
            expect(this.model.mComposeState.set).to.be.calledWith('.isBccVisible', true);
        });

        it('Не должен раскрывать на форме поля Копия и Скрытая копия, если они пустые', function() {
            this.model.get.withArgs('.cc').returns('');
            this.model.get.withArgs('.bcc').returns('');
            this.model.setRecepientsFromMessageBody(this.mMessageBody);
            expect(this.model.mComposeState.set).to.be.calledWith('.isCcVisible', false);
            expect(this.model.mComposeState.set).to.be.calledWith('.isBccVisible', false);
        });
    });

    describe('#_getArrayOfEmails', function() {
        it('Стандартный объект получателей правильно преобразовывается#1', function() {
            var recipients = [ { email: 'test1@yandex.ru', name: 'Test' }, { email: 'test@yandex.com', name: 'test2' } ];

            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([ 'test1@yandex.ru', 'test@yandex.com' ]);
        });
        it('Стандартный объект получателей правильно преобразовывается#2', function() {
            var recipients = [ { email: 'test1@yandex.ru', name: 'Test' }, { email: 'test3@ya.ru' } ];
            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([ 'test1@yandex.ru', 'test3@ya.ru' ]);
        });
        it('В массиве объектов нет email', function() {
            var recipients = [ { name: 'test' } ];
            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([ undefined ]);
        });
        it('Пустой массив должен нормально обработаться', function() {
            var recipients = [];
            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([]);
        });
        it('Передан массив пустых объектов', function() {
            var recipients = [ {}, {} ];
            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([ undefined, undefined ]);
        });
        it('Один элемент массива пустой, другой - заполнен не полностью - нет email', function() {
            var recipients = [ {}, { name: 'test' } ];
            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([ undefined, undefined ]);
        });
        it('Один элемент массива пустой, другой не полностью, но есть email', function() {
            var recipients = [ {}, { email: 'tst@ya.ru' } ];
            expect(this.model._getArrayOfEmails(recipients)).to.be.eql([ undefined, 'tst@ya.ru' ]);
        });
    });

    describe('#_onRecipientsChanged', function() {
        it('Плашка не должна показываться в шаблонах - никакие настройки модели не менялись', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            this.sinon.stub(this.model, 'isTemplate').returns(true);
            this.sinon.stub(this.model, 'isNewMessage').returns(false);
            this.sinon.stub(this.model, 'isForward').returns(false);
            this.sinon.spy(this.model, 'set');

            var that = this;
            return this.model._onRecipientsChanged().then(function() {
                expect(that.model.set).to.have.callCount(0);
            });
        });
        it('Плашка не должна показываться в новых письмах - никакие настройки модели не менялись', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            this.sinon.stub(this.model, 'isNewMessage').returns(true);
            this.sinon.stub(this.model, 'isTemplate').returns(false);
            this.sinon.stub(this.model, 'isForward').returns(false);
            this.sinon.spy(this.model, 'set');

            var that = this;
            return this.model._onRecipientsChanged().then(function() {
                expect(that.model.set).to.have.callCount(0);
            });
        });
        it('Плашка не должна показываться в письмах-переписках - никакие настройки модели не менялись', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            this.sinon.stub(this.model, 'isForward').returns(true);
            this.sinon.stub(this.model, 'isNewMessage').returns(false);
            this.sinon.stub(this.model, 'isTemplate').returns(false);
            this.sinon.spy(this.model, 'set');

            var that = this;
            return this.model._onRecipientsChanged().then(function() {
                expect(that.model.set).to.have.callCount(0);
            });
        });
        it('Плашка не должна показываться в обычной почте - настройки модели не меняются', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(false);
            this.sinon.stub(this.model, 'isForward').returns(false);
            this.sinon.stub(this.model, 'isNewMessage').returns(false);
            this.sinon.stub(this.model, 'isTemplate').returns(false);
            this.sinon.spy(this.model, 'set');
            var that = this;
            return this.model._onRecipientsChanged().then(function() {
                expect(that.model.set).to.have.callCount(0);
            });
        });

        describe('Плашка показывается в корпоративной почте ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, 'isTemplate').returns(false);
                this.sinon.stub(this.model, 'isNewMessage').returns(false);
                this.sinon.stub(this.model, 'isForward').returns(false);
                this.sinon.stub(Daria, 'IS_CORP').value(true);

                this.mGetMaillistsStatic = ns.Model.get('get-maillists-static');
                this.mGetMaillistsStatic.setData({});

                this.expectRecipientsDiff = function(obj) {
                    var that = this;
                    return this.model._onRecipientsChanged().then(function() {
                        expect(that.model.set).to.be.calledWith('.recipients-diff', obj);
                    });
                };

                this.setMaillist = function(arrayOfEmailsResolve) {
                    this.sinon.stub(this.mGetMaillistsStatic, 'getMaillists')
                        .returns(vow.resolve(arrayOfEmailsResolve));
                };
            });

            it('Изначально был один получатель и добавили нового', function() {
                this.model.set('.initial_to', 'test1 <test@yandex.ru>');
                this.model.set('.initial_cc', '');

                this.model.set('.to', 'test1 <test@yandex.ru>, user@yandex.ru');
                this.model.set('.cc', '');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({ added: [ 'user@yandex.ru' ], removed: [] });
            });

            it('Изначально было много неповторяющихся получателей и добавили еще одного', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, test@ya.ru');
                this.model.set('.initial_cc', 'test2 <test2@yandex-team.ru>');

                this.model.set('.to', 'test1 <test1@ya.ru>, test@ya.ru, Test4 <test4@yandex.ru>');
                this.model.set('.cc', 'test2 <test2@yandex-team.ru>');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({ added: [ 'test4@yandex.ru' ], removed: [] });
            });

            it('Было больше одного получателя и добавили еще одного повторяющегося - в плашке описан только один', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, test@ya.ru');
                this.model.set('.initial_cc', 'test@ya.ru');

                this.model.set('.to', 'test1 <test1@ya.ru>, Test5 <test5@yandex.ru>');
                this.model.set('.cc', 'test@ya.ru');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({ added: [ 'test5@yandex.ru' ], removed: [] });
            });

            it('При удалении повторяющегося адресата в плашке он пишется один раз', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@yandex.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, Test5 <test5@yandex.ru>');

                this.model.set('.to', 'test1 <test1@ya.ru>');
                this.model.set('.cc', 'test@ya.ru');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [],
                    removed: [ 'test5@yandex.ru' ]
                });
            });

            it('Изначально много пользователей, удаляем всех кроме одного - пишется -все', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@yandex.ru>');
                this.model.set('.initial_cc', 'test@ya.ru');

                this.model.set('.to', 'Test5 <test5@yandex.ru>');
                this.model.set('.cc', '');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [],
                    removed: [ 'все' ]
                });
            });

            it('Изначально много пользователей, удаляем всех - пишется -все', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@yandex.ru>');
                this.model.set('.initial_cc', 'test@ya.ru');

                this.model.set('.to', '');
                this.model.set('.cc', '');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [],
                    removed: [ 'все' ]
                });
            });

            it('Изначально много пользователей, удаляем всех кроме рассылки - не пишется -все', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail-verstka@yandex-team.ru');
                this.model.set('.to', '');
                this.model.set('.cc', 'mail-verstka@yandex-team.ru');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [],
                    removed: [ 'test1@ya.ru', 'test5@ya.ru', 'test@ya.ru' ]
                });
            });

            it('Изначально было много получателей (в том числе рассылка).Удалили всех кроме рассылки и добавили нового', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail-verstka@yandex-team.ru');
                this.model.set('.to', 'user@yandex-team.ru');
                this.model.set('.cc', 'mail-verstka@yandex-team.ru');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'user@yandex-team.ru' ],
                    removed: [ 'test1@ya.ru', 'test5@ya.ru', 'test@ya.ru' ]
                });
            });

            it('Изначально было много получателей (без рассылки).Удалили всех и добавили нового (не рассылку)', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'user@yandex-team.ru');
                this.model.set('.cc', '');

                this.setMaillist([]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'user@yandex-team.ru' ],
                    removed: [ 'все' ]
                });
            });

            it('Изначально было много получателей. Удалили всех и добавили нового (рассылку)', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'mail-verstka@yandex-team.ru');
                this.model.set('.cc', '');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'mail-verstka@yandex-team.ru' ],
                    removed: [ 'все' ]
                });
            });

            it('Изначально было много получателей. Удалили всех кроме одного (остался в кому) и добавили нового (рассылку)(в кому)', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'test@ya.ru, mail-verstka@yandex-team.ru');
                this.model.set('.cc', '');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'mail-verstka@yandex-team.ru' ],
                    removed: [ 'test1@ya.ru', 'test5@ya.ru', 'mail1@yandex-team.ru' ]
                });
            });

            it('Изначально было много получателей. Удалили всех кроме одного (один остался в копии) и добавили нового (рассылку) (в копию)', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail1@yandex-team.ru');
                this.model.set('.to', '');
                this.model.set('.cc', 'test@ya.ru, mail-verstka@yandex-team.ru');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'mail-verstka@yandex-team.ru' ],
                    removed: [ 'test1@ya.ru', 'test5@ya.ru', 'mail1@yandex-team.ru' ]
                });
            });

            it('Изначально было много получателей. Удалили всех кроме одного (остался в кому) и добавили нового (рассылку)(в кому)', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'test@ya.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'test@ya.ru');
                this.model.set('.cc', 'mail-verstka@yandex-team.ru');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'mail-verstka@yandex-team.ru' ],
                    removed: [ 'test1@ya.ru', 'test5@ya.ru', 'mail1@yandex-team.ru' ]
                });
            });

            it('Изначально было много получателей. Удалили всех кроме одного (рассылки) и добавили нового (другую рассылку)', function() {
                this.model.set('.initial_to', 'test1 <test1@ya.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'mail-test@yandex-team.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'mail-verstka@yandex-team.ru');
                this.model.set('.cc', 'mail-test@yandex-team.ru');

                this.setMaillist([ 'mail-verstka@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'mail-verstka@yandex-team.ru' ],
                    removed: [ 'test1@ya.ru', 'test5@ya.ru', 'mail1@yandex-team.ru' ]
                });
            });

            it('Включены популярные контакты - добавили повторяющегося - никто не добавился и не удалился', function() {
                this.sinon.stub(this.mSettings, 'isSet').returns(true);
                var popularContacts = ns.Model.get('compose-popular-contacts');
                popularContacts.setData([
                    {
                        email: 'newcontact@yandex-team.ru',
                        name: 'New Contact',
                        ref: '07ad6c351fa7287546f38ffd23e572cf',
                        used: true
                    }
                ]);

                this.model.set('.initial_to', 'New Contact <newcontact@yandex-team.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'mail-test@yandex-team.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'New Contact <newcontact@yandex-team.ru>, Test5 <test5@ya.ru>');
                this.model.set('.cc', 'mail-test@yandex-team.ru, mail1@yandex-team.ru');

                var contact = popularContacts.getContact({ email: 'newcontact@yandex-team.ru', name: 'New Contact' });
                this.model.appendContact('to', contact);

                this.setMaillist([ 'mail-test@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [],
                    removed: []
                });
            });

            it('Включены популярные контакты - добавили нового - никто не добавился и не удалился', function() {
                this.sinon.stub(this.mSettings, 'isSet').returns(true);
                var popularContacts = ns.Model.get('compose-popular-contacts');
                popularContacts.setData([
                    {
                        email: 'test_0@yandex-team.ru',
                        name: 'Test0',
                        ref: '15521e55d2c350aa233e4741d4fbf29a',
                        used: false
                    }
                ]);

                this.model.set('.initial_to', 'New Contact <newcontact@yandex-team.ru>, Test5 <test5@ya.ru>');
                this.model.set('.initial_cc', 'mail-test@yandex-team.ru, mail1@yandex-team.ru');
                this.model.set('.to', 'New Contact <newcontact@yandex-team.ru>, Test5 <test5@ya.ru>, test_0@yandex-team.ru');
                this.model.set('.cc', 'mail-test@yandex-team.ru, mail1@yandex-team.ru');

                var contact = popularContacts.getContact({ email: 'test_0@yandex-team.ru', name: 'Test0' });
                this.model.appendContact('to', contact);

                this.setMaillist([ 'mail-test@yandex-team.ru' ]);

                this.sinon.spy(this.model, 'set');

                return this.expectRecipientsDiff({
                    added: [ 'test_0@yandex-team.ru' ],
                    removed: []
                });
            });
        });
    });

    describe('#_getRecipientsDiffStr', function() {
        it('Должен вернуть правильную строку изменений в списках получателей не было', function() {
            this.model.set('.recipients-diff', {
                added: [],
                removed: []
            });
            expect(this.model._getRecipientsDiffStr()).to.be.eql('');
        });
        it('Должен вернуть правильный формат, если были добавлены получатели', function() {
            this.sinon.stub();
            this.model.set('.recipients-diff', {
                added: [ 'test@yandex.ru' ],
                removed: []
            });
            expect(this.model._getRecipientsDiffStr()).to.be.eql('+ test@yandex.ru');
        });
        it('Должен вернуть правильный формат, если были удалены получатели', function() {
            this.model.set('.recipients-diff', {
                added: [ 'test@yandex.ru' ],
                removed: [ 'test1@ya.ru' ]
            });
            expect(this.model._getRecipientsDiffStr()).to.be.eql('+ test@yandex.ru\n- test1@ya.ru');
        });
    });

    describe('#changeRecipientsToOpposite', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'set');
            this.sinon.stub(this.model, '_calculateOppositeRecipients');

            this.model._calculateOppositeRecipients.returns('42');

            this.model._initFromModels(this.models);
            this.mMessageBody.getRecipients.returns({ to: '', cc: '' });
        });

        it('Задает обновленное поле `to`', function() {
            this.model.changeRecipientsToOpposite();

            expect(this.model.set).to.be.calledWith('.to', '42');
        });

        it('Задает обновленное поле `cc`', function() {
            this.model.changeRecipientsToOpposite();

            expect(this.model.set).to.be.calledWith('.cc', '42');
        });
    });

    describe('#_calculateOppositeRecipients', function() {
        function processResult(result) {
            return _(result.split(','))
                .map(function(email) {
                    return $.trim(email);
                })
                .sort()
                .value();
        }

        it('reply → reply-all', function() {
            var current = 'dydka2@yandex.ru, ala@yandex.ru';
            var was = 'dydka2@yandex.ru';
            var will = 'dydka2@yandex.ru, bay@yandex.ru';

            var result = this.model._calculateOppositeRecipients(current, was, will);
            // разрезаем по запятой, убираем пробелы и сортируем
            result = processResult(result);

            expect(result).to.be.eql([ '<ala@yandex.ru>', '<bay@yandex.ru>', '<dydka2@yandex.ru>' ]);
        });

        it('reply-all → reply', function() {
            var current = 'dydka2@yandex.ru, ala@yandex.ru, bay@yandex.ru';
            var was = 'dydka2@yandex.ru, ala@yandex.ru';
            var will = 'dydka2@yandex.ru';

            var result = this.model._calculateOppositeRecipients(current, was, will);
            // разрезаем по запятой, убираем пробелы и сортируем
            result = processResult(result);

            expect(result).to.be.eql([ '<bay@yandex.ru>', '<dydka2@yandex.ru>' ]);
        });
    });

    describe('#_prepareDataForSet', function() {
        beforeEach(function() {
            this.mComposePredefinedData.setData({ to: 'la-la-la' });

            this.sinon.stub(this.mComposePredefinedData, 'destroy');
        });

        it('Подмешивает данные из модели mComposePredefinedData', function() {
            expect(this.model._prepareDataForSet().to).to.be.equal('la-la-la');
        });

        it('Переданные данные затирают данные из модели mComposePredefinedData', function() {
            expect(this.model._prepareDataForSet({ to: 'bla-bla' }).to).to.be.equal('bla-bla');
        });

        it('Уничтожает модель mComposePredefinedData после подмешивания ее данных', function() {
            this.model._prepareDataForSet();

            expect(this.mComposePredefinedData.destroy).to.have.callCount(1);
        });
    });

    describe('#checkConfirmations', function() {
        beforeEach(function() {
            this.confirmation = this.sinon.stub().returns('need_confirmation');
            this.secondConfirmation = this.sinon.stub();
            this.model.confirmations = [ this.confirmation ];
        });

        it('Должен вызвать проверку', function() {
            this.model.checkConfirmations();

            expect(this.confirmation).to.have.callCount(1);
        });

        it('Не должен вызывать вторую проверку если первая вернула строку', function() {
            this.model.confirmations.push(this.secondConfirmation);
            this.model.checkConfirmations();

            expect(this.secondConfirmation).to.have.callCount(0);
        });

        it('Должен вызвать вторую проверку, если первая вернула false', function() {
            this.model.confirmations.push(this.secondConfirmation);
            this.confirmation.returns(false);
            this.model.checkConfirmations();

            expect(this.secondConfirmation).to.have.callCount(1);
        });

        it('Должен вернуть название необходимого подтверждения', function() {
            expect(this.model.checkConfirmations()).to.be.equal('need_confirmation');
        });
    });

    describe('#storeErrors', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [
                'setIfChanged',
                'triggerFieldErrors'
            ]);

            this.model.storeErrors({
                testProp: [ 'wrong_data' ],
                anotherTestProp: [ 'anther_wrong_data', 'wrong_data' ]
            });
        });

        it('Должен стриггерить кумулятивное событие обо всех ошибках', function() {
            expect(this.model.triggerFieldErrors).to.be.calledWith({
                testProp: [ 'wrong_data' ],
                anotherTestProp: [ 'anther_wrong_data', 'wrong_data' ]
            });
        });

        it('Должен затригерить ивенты для каждого поля с ошибками', function() {
            expect(this.model.setIfChanged.callCount).to.be.equal(2);
        });

        it('Должен передать код ошибки', function() {
            expect(this.model.setIfChanged.firstCall.args[1][0]).to.be.equal('wrong_data');
        });

        it('Должен передать название поля', function() {
            expect(this.model.setIfChanged.firstCall.args[0]).to.be.equal('.errors.testProp');
        });
    });

    describe('#_getAttributeFromData', function() {
        it('Должен вернуть массив атрибутов аттачей', function() {
            var data = [ { att_id: 'aaa' }, { att_id: 'bbb' } ];

            expect(this.model._getAttributeFromData(data, 'att_id')).to.be.eql([ 'aaa', 'bbb' ]);
        });
    });

    describe('#addAttachment', function() {
        beforeEach(function() {
            this.attachId = 3;
            this.attachData = {
                att_id: 'ccc',
                hid: '1.6.7'
            };

            this.attachment = { info: { id: this.attachId } };

            this.sinon.stub(this.model, '_saveAttachmentData');
        });

        it('Должен вызывать перерасчет данных аттача для отправки', function() {
            this.model.addAttachment(this.attachment);

            expect(this.model._saveAttachmentData).to.have.callCount(1);
        });
    });

    describe('#removeAttachment', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, '_attachments').value({
                1: {
                    att_id: 'bbb',
                    hid: 2
                }
            });
            this.sinon.stub(this.model, '_saveAttachmentData');
        });

        it('Удаляет аттач с переданным идентификатором из внутреннего массива', function() {
            this.model.removeAttachment(1);

            expect(this.model._attachments[1]).to.be.equal(undefined);
        });

        it('Должен вызывать перерасчет данных аттача для отправки', function() {
            this.model.removeAttachment(1);

            expect(this.model._saveAttachmentData).to.have.callCount(1);
        });
    });

    describe('#_saveAttachmentData', function() {
        beforeEach(function() {
            this.attachData = {};
            this.sinon.stub(_, 'map').returns(this.attachData);

            this.arr = [ 1, 2, 3 ];
            this.sinon.stub(this.model, '_getAttributeFromData')
                .withArgs(this.attachData, 'att_id').returns(this.arr)
                .withArgs(this.attachData, 'hid').returns(this.arr)
                .withArgs(this.attachData, 'disk').returns(this.arr);

            this.sinon.stub(this.model, 'set');
        });

        it('Должен сохранять срез всех `att_id` в поле модели `att_ids`', function() {
            this.model._saveAttachmentData();

            expect(this.model.set).to.be.calledWithExactly('.att_ids', this.arr);
        });

        it('Должен сохранять срез всех `hid` в поле модели `parts`', function() {
            this.model._saveAttachmentData();

            expect(this.model.set).to.be.calledWithExactly('.parts', this.arr);
        });

        it('Должен сохранять склеенный срез всех `disk` в поле модели `disk_att`', function() {
            this.model._saveAttachmentData();

            expect(this.model.set).to.be.calledWithExactly('.disk_att', '[1,2,3]');
        });
    });

    describe('#_getCommonSendParams', function() {
        it('Возвращает правильные данные', function() {
            this.sinon.stub(ns.request, 'addRequestParams').callsFake(function(params) {
                params.test = '42';
            });

            expect(this.model._getCommonSendParams({})).to.be.eql({
                test: '42'
            });
        });
    });

    describe('#_prepareDataForMailSend', function() {
        beforeEach(function() {
            this.model._prepareDataForMailSend.restore();
            this.sinon.stub(this.model, 'removeLid');
        });

        describe('Для посылки письма →', function() {
            it('Должен выставить ign_overwrite = "yes", если письмо является шаблоном', function() {
                this.sinon.stub(this.model, 'isTemplate').returns(true);
                var data = {
                    test: '42'
                };
                var preparedData = this.model._prepareDataForMailSend(data, false);
                expect(preparedData.ign_overwrite).to.be.eql('yes');
            });

            it('Ничего не делает с объектом, кроме current_time - если обычная почта', function() {
                var data = {
                    test: '42'
                };

                expect(Daria.IS_CORP).to.be.equal(false);
                expect(this.model._prepareDataForMailSend(data, false)).to.be.eql({
                    current_time: 123654789,
                    test: '42'
                });
            });

            describe('Параметры для обычной отложенной отправки', function() {
                beforeEach(function() {
                    this.data = {
                        test: '42',
                        send_time: 111
                    };

                    this.sinon.stub(this.model, 'isDelayed').returns(true);

                    this.result = this.model._prepareDataForMailSend(this.data, false);
                });

                it('Должен отправить данные as is + current_time', function() {
                    expect(this.result).to.be.eql({
                        current_time: 123654789,
                        test: '42',
                        send_time: 111
                    });
                });
            });

            describe('Параметры для отправки с возможностью отмены', function() {
                beforeEach(function() {
                    this.sinon.stub(this.model, 'withUndo').returns(true);
                    this.sinon.stub(this.model, 'isDelayed').returns(false);
                    this.sinon.stub(Daria.SendMail, 'getUndoSendTime').returns(5000);

                    this.data = {
                        test: '42'
                    };
                });

                it('Должен добавить параметры, нужные для отмены', function() {
                    expect(this.model._prepareDataForMailSend(this.data, false)).to.be.eql({
                        current_time: 123654789,
                        test: '42',
                        send_time: 5000,
                        relative: true
                    });
                });

                it('Не должен добавить параметры для отмены (обычная отложенная отправка)', function() {
                    this.data.send_time = 111;
                    this.model.isDelayed.returns(true);

                    expect(this.model._prepareDataForMailSend(this.data, false)).to.be.eql({
                        current_time: 123654789,
                        test: '42',
                        send_time: 111
                    });
                });

                it('Не должен добавить параметры для отмены (отмена отправки недоступна)', function() {
                    this.model.withUndo.returns(false);

                    expect(this.model._prepareDataForMailSend(this.data, false)).to.be.eql({
                        current_time: 123654789,
                        test: '42'
                    });
                });
            });

            describe('Корпоративная почта и плашка +kukutz', () => {
                beforeEach(function() {
                    this.sinon.stub(Daria, 'IS_CORP').value(true);
                    this.sinon.stub(this.model, '_getRecipientsDiffStr').returns('');
                });

                describe('Пустой diff получателей', function() {
                    beforeEach(function() {
                        this.model._getRecipientsDiffStr.returns('');
                    });

                    it('ничего не делаем с объектом данных, кроме добавления current_time', function() {
                        var data = {
                            test: '42',
                            send: '&nbsp;'
                        };

                        expect(this.model._prepareDataForMailSend(data, false)).to.be.eql({
                            current_time: 123654789,
                            test: '42',
                            send: '&nbsp;'
                        });
                    });
                });

                describe('Непустой diff получателей', function() {
                    beforeEach(function() {
                        this.model._getRecipientsDiffStr.returns('+kukutz');
                        this.sinon.stub(this.model, 'isHtmlType');
                        this.sinon.stub(Daria.Html2Text, 'text2html').callsFake((text) => `<div>${text}</div>`);

                        this.data = {
                            test: '42',
                            send: '&nbsp;'
                        };
                    });

                    it('дописывает diff получателей (письмо в HTML формате)', function() {
                        this.model.isHtmlType.returns(true);

                        expect(this.model._prepareDataForMailSend(this.data, false)).to.be.eql({
                            current_time: 123654789,
                            test: '42',
                            send: '<div>+kukutz</div><div>&nbsp;</div>&nbsp;'
                        });
                    });

                    it('дописывает diff получателей (письмо в текстовом формате)', function() {
                        this.model.isHtmlType.returns(false);

                        expect(this.model._prepareDataForMailSend(this.data, false)).to.be.eql({
                            current_time: 123654789,
                            test: '42',
                            send: '+kukutz\n\n&nbsp;'
                        });
                    });
                });
            });
        });

        describe('Для сохранения письма →', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, 'isSystemLabel');
            });

            describe('Отфильтровывает системные метки ->', function() {
                beforeEach(function() {
                    var data = {
                        lids: [ '1', '2' ]
                    };
                    this.model.isSystemLabel
                        .withArgs('1').returns(false)
                        .withArgs('2').returns(true);

                    this.data = this.model._prepareDataForMailSend(data, true);
                });

                it('должен удалить системные метки', function() {
                    expect(this.data.lids).to.be.eql([ '1' ]);
                });

                it('должен удалить системные метки, без удаления их из данных модели', function() {
                    expect(this.model.removeLid).to.have.callCount(0);
                });
            });

            it('должен удалить данные о неответе', function() {
                var data = {
                    lids: [],
                    remind_period: '86400'
                };

                data = this.model._prepareDataForMailSend(data, true);

                expect(data).to.have.property('remind_period', null);
            });

            it('должен удалить данные об отложенной отправке', function() {
                var data = {
                    lids: [],
                    send_time: 86400000
                };

                data = this.model._prepareDataForMailSend(data, true);

                expect(data).to.have.property('send_time', null);
            });
        });
    });

    describe('#_updateDataAfterSave', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [
                'set',
                'isTemplate'
            ]);

            this.runMethod = function() {
                this.model._updateDataAfterSave({
                    storedmid: '1',
                    store_fid: '2'
                });
            };
        });

        it('Задает ign_overwrite === "no"', function() {
            this.runMethod();

            expect(this.model.set).to.be.calledWithExactly('.ign_overwrite', 'no');
        });

        it('Задает overwrite === responseObj.storedmid', function() {
            this.runMethod();

            expect(this.model.set).to.be.calledWithExactly('.overwrite', '1');
        });

        it('Задает current_folder === responseObj.store_fid', function() {
            this.runMethod();

            expect(this.model.set).to.be.calledWithExactly('.current_folder', '2');
        });
    });

    describe('#_processSaveResponse', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [
                '_updateDataAfterSave',
                'markSaved',
                'trigger',
                'getDraftMid'
            ]);
            this.sinon.stub(this.model._syncService, 'updateDraftAfterAction');
            this.sinon.stub(this.model, 'isDraft').returns(true);
        });

        it('Возвращает реджекченный промис, если не передан `storedmid`', function() {
            expect(this.model._processSaveResponse({}).isRejected()).to.be.ok;
            expect(this.model._updateDataAfterSave).to.have.callCount(0);
            expect(this.model.markSaved).to.have.callCount(0);
        });

        it('Возвращает реджекченный промис, если модель была уничтожена', function() {
            this.sinon.stub(this.model, 'inNotInitedState').returns(true);

            expect(this.model._processSaveResponse({}).isRejected()).to.be.ok;
        });

        it('Запускает обновление данных модели', function() {
            var responseObj = { storedmid: '1' };
            this.model._processSaveResponse(responseObj);

            expect(this.model._updateDataAfterSave).to.be.calledWith(responseObj);
        });

        it('Помечает модель сохраненной', function() {
            var responseObj = { storedmid: '1' };
            this.model._processSaveResponse(responseObj);

            expect(this.model.markSaved).to.have.callCount(1);
        });

        it('Триггерит событие сохранение черновика с правильными данными', function() {
            var responseObj = { storedmid: '1', attachment: [] };
            this.model._processSaveResponse(responseObj);

            expect(this.model.trigger).to.be.calledWith('ns-model:draft-saved');
            expect(this.model.trigger.getCall(0).args[1]).to.be.eql({
                mid: '1',
                attachments: []
            });
        });

        it('Должен вызвать метод обновления кэшей, связанных с черновиком', function() {
            var responseObj = { storedmid: '123', attachment: [] };
            this.model._processSaveResponse(responseObj);
            expect(this.model._syncService.updateDraftAfterAction.withArgs(true)).to.have.callCount(1);
        });
    });

    describe('#_processSendResponse', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'set');
            this.sinon.stub(this.model._syncService, 'updateDraftAfterAction');
            this.sinon.stub(this.model._syncService, 'updateMessageFlagsAfterSend');
        });

        it('Должен установить значение limited', function() {
            this.sinon.stub(this.model, '_parseLimited').returns({ recipients: 'a,b,c', limit: '1kb' });

            const responseObj = { limited: { recipient: {} } };
            this.model._processSendResponse(responseObj);

            expect(this.model.set).to.be.calledWith('.limited', { recipients: 'a,b,c', limit: '1kb' });
        });

        it('Должен установить значение sentMessageId', function() {
            const responseObj = { message_id: '123' };
            this.model._processSendResponse(responseObj);
            expect(this.model.set).to.be.calledWith('.sentMessageId', '123');
        });

        it('Должен установить значение sentMid', function() {
            const responseObj = { storedmid: '123' };
            this.model._processSendResponse(responseObj);
            expect(this.model.set).to.be.calledWith('.sentMid', '123');
        });

        it('Должен вызвать метод обновления кэшей, связанных с черновиком', function() {
            this.sinon.stub(this.model, 'getDraftMid').returns('321');
            const responseObj = { storedmid: '123', attachment: [] };
            this.model._processSendResponse(responseObj);
            expect(this.model._syncService.updateDraftAfterAction)
                .to.be.calledWithExactly(false, '321');
        });

        it('Должен вызвать метод установки флагов для писем-пересылок и(или) писем-ответов', function() {
            const responseObj = { storedmid: '123', attachment: [] };
            this.model._processSendResponse(responseObj);
            expect(this.model._syncService.updateMessageFlagsAfterSend).to.have.callCount(1);
        });
    });

    describe('#createTemplateFolder', function() {
        beforeEach(function() {
            this.mFolders = ns.Model.get('folders');
            this.sinon.stubMethods(this.mFolders, [ 'getFidBySymbol', 'createTemplateFolder' ]);
            this.sinon.stub(this.model, 'set');
        });

        describe('Папки "Шаблоны" нет →', function() {
            beforeEach(function() {
                this.mFolders.getFidBySymbol.returns(undefined);
                this.mFolders.createTemplateFolder.callsFake(() => vow.Promise.reject());
            });

            it('Запускает создание папки', function() {
                this.model.createTemplateFolder();

                expect(this.mFolders.createTemplateFolder).to.have.callCount(1);
            });

            it('Сохраняет ее fid в .template_fid после создания', function() {
                this.mFolders.createTemplateFolder.returns(vow.Promise.resolve({ fid: '42' }));

                return this.model.createTemplateFolder().then(function() {
                    expect(this.model.set).to.be.calledWith('.templates_fid', '42');
                }, this);
            });
        });

        describe('Папка "Шаблоны" есть →', function() {
            beforeEach(function() {
                this.mFolders.getFidBySymbol.returns('42');
                this.mFolders.createTemplateFolder.callsFake(() => vow.Promise.reject());
            });

            it('Возвращает резолвленный промис', function() {
                expect(this.model.createTemplateFolder().isResolved()).to.be.ok;
            });
        });
    });

    describe('#save', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, '_save').returns(vow.Promise.resolve());
        });

        it('Должен вызвать #_save с переданными параметрами', function() {
            this.model.save({ test: true });
            expect(this.model._save).have.callCount(1);
            expect(this.model._save).to.have.been.calledWith({ test: true });
        });

        it('Должен сохранить промис сохранения черновика', function() {
            expect(this.model._saveDraftPromise).to.equal(null);

            this.model.save();

            expect(this.model._saveDraftPromise).to.be.instanceOf(vow.Promise);
        });

        it('Должен удалять сохранённый промис и возвращать резолв при резолве #_save', function() {
            expect(this.model._saveDraftPromise).to.equal(null);

            return this.model.save().then(function() {
                expect(this.model._saveDraftPromise).to.equal(null);
            }, this);
        });

        it('Должен удалять сохранённый промис и возвращать реджект при реджекте #_save', function() {
            this.model._save.returns(vow.Promise.reject());

            expect(this.model._saveDraftPromise).to.equal(null);

            return this.model.save().then(
                function() {
                    throw new Error('Should be rejected');
                },
                function() {
                    expect(this.model._saveDraftPromise).to.equal(null);
                },
                this
            );
        });
    });

    describe('#_save', function() {
        beforeEach(function() {
            this.model.markSaved();
            this.sinon.stub(this.model, 'isDirty').returns(true);
            this.sinon.stub(this.model, 'createTemplateFolder').returns(vow.Promise.resolve());
            this.sinon.stub(this.model, 'makeRequest').returns(vow.Promise.resolve());
            this.sinon.stub(this.model, '_processSaveResponse').returns(vow.Promise.resolve());
            this.sinon.stub(this.model, 'getSendMessage').returns('');
        });

        describe('Проверка необходимости сохранения →', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, 'isTemplate').returns(false);

                this.dataObj = {};
                this.model._prepareDataForMailSend.returns(this.dataObj);
            });

            it('Должен безусловно сохранить, если передан options.force === true', function() {
                this.model.isDirty.returns(false);

                return this.model._save({ force: true }).then(function() {
                    expect(this.model.makeRequest).to.have.callCount(1);
                }, this);
            });

            it('Должен сохранить, если модель грязная', function() {
                this.model.isDirty.returns(true);

                return this.model._save().then(function() {
                    expect(this.model.makeRequest).to.have.callCount(1);
                }, this);
            });

            it('Не должен сохранять, если не передан options.force === true и модель чистая', function() {
                this.model.isDirty.returns(false);

                var promise = this.model._save();

                expect(promise.isResolved()).to.be.ok;
                return promise.then(function() {
                    expect(this.model.makeRequest).to.have.callCount(0);
                }, this);
            });
        });

        describe('Формирование данных →', function() {
            beforeEach(function() {
                this.model.isDirty.returns(true);
                this.model._prepareDataForMailSend.restore();
            });

            it('Параметр nosend === "yes" должен быть передан в запрос', function() {
                return this.model._save().then(function() {
                    expect(this.model.makeRequest.getCall(0).args[0].nosend).to.be.equal('yes');
                }, this);
            });

            it('Служебные данные должны быть переданы в запрос', function() {
                this.sinon.stub(this.model, '_getCommonSendParams').returns({ test: '42' });

                return this.model._save().then(function() {
                    expect(this.model.makeRequest.getCall(0).args[0].test).to.be.equal('42');
                }, this);
            });

            it('GET-строка должна быть передана в запрос', function() {
                return this.model._save().then(function() {
                    expect(this.model.makeRequest.getCall(0).args[1]).to.be.equal('_save=true');
                }, this);
            });
        });

        describe('Сохранение необходимо →', function() {
            beforeEach(function() {
                this.model.isDirty.returns(true);
                this.dataObj = {};
                this.model._prepareDataForMailSend.returns(this.dataObj);
            });

            describe('Нужно сохранить шаблон →', function() {
                beforeEach(function() {
                    this.sinon.stub(this.model, 'isTemplate').returns(true);
                });

                it('Должен создать папку "Шаблоны", если папки нет', function() {
                    this.model.createTemplateFolder.callsFake(() => vow.Promise.reject());

                    this.model._save();

                    expect(this.model.createTemplateFolder).to.have.callCount(1);
                });

                it('Должен вызвать посылку запроса после создания папки', function() {
                    this.model.createTemplateFolder.returns(vow.Promise.resolve());

                    return this.model._save().then(function() {
                        expect(this.model.makeRequest).to.have.callCount(1);
                    }, this);
                });
            });

            describe('Нужно сохранить черновик', function() {
                beforeEach(function() {
                    this.sinon.stub(this.model, 'isTemplate').returns(false);
                });

                it('Должен вызвать посылку запроса после создания папки', function() {
                    return this.model._save().then(function() {
                        expect(this.model.createTemplateFolder).to.have.callCount(0);
                        expect(this.model.makeRequest).to.have.callCount(1);
                    }, this);
                });

                it('Должен отправить запрос с правильно сформированными параметрами', function() {
                    return this.model._save().then(function() {
                        expect(this.model.makeRequest).to.be.calledWith(this.dataObj, '_save=true');
                    }, this);
                });
            });
        });
    });

    describe('#waitForDraftSave', function() {
        it('Если нет промиса сохранения черновика, то должен вернуть резолвленный промис', function() {
            this.sinon.stub(this.model, '_saveDraftPromise').value(null);
            return this.model.waitForDraftSave();
        });

        it('Если промис сохранения черновика резолвится, то должен вернуть резолвленный промис', function() {
            this.sinon.stub(this.model, '_saveDraftPromise').value(vow.Promise.resolve());
            return this.model.waitForDraftSave();
        });

        it('Если промис сохранения черновика реджектится, то должен вернуть резолвленный промис', function() {
            this.sinon.stub(this.model, '_saveDraftPromise').value(vow.Promise.reject());
            return this.model.waitForDraftSave();
        });
    });

    describe('#_waitForAttachmentsUploaded', function() {
        beforeEach(function() {
            this.fakeComposeAttachments = _.extend({
                isValid: this.sinon.stub(),
                isUploading: this.sinon.stub()
            }, ns.Events);

            this.sinon.stub(ns.Model, 'get')
                .withArgs('compose-attachments', this.model.params).returns(this.fakeComposeAttachments);

            this.sinon.stub(this.model, '_checkData').callsFake(() => vow.reject());
            this.sinon.stub(this.model, 'storeErrors');
            this.sinon.stub(Daria.Statusline, 'hide');
        });

        it('Если отправка была отменена - должен зареджектиться с ошибкой "send-cancelled"', function() {
            this.model._cancelled = true;
            return this.model._waitForAttachmentsUploaded()
                .fail(function(error) {
                    expect(error).to.be.equal('send-cancelled');
                });
        });

        it('Должен сохранить ссылку на функцию для быстрой отмены отправки', function() {
            this.model._waitForAttachmentsUploaded();
            expect(typeof this.model._cancelSendCallback).to.be.equal('function');
        });

        it('Должен зареджектить промис при вызове функции отмены отправки', function() {
            this.fakeComposeAttachments.isValid.returns(true);
            this.fakeComposeAttachments.isUploading.returns(true);

            const promise = this.model._waitForAttachmentsUploaded();

            expect(promise.isResolved()).to.be.equal(false);

            this.model._cancelSendCallback();

            expect(promise.isRejected()).to.be.equal(true);
        });

        it('Если модель "compose-attachments" не существует или не валидна и не идёт загрузка, должен зарезолвиться', function() {
            this.fakeComposeAttachments.isValid.returns(false);
            this.fakeComposeAttachments.isUploading.returns(false);

            const promise = this.model._waitForAttachmentsUploaded();
            expect(promise.isResolved()).to.be.equal(true);
            return promise;
        });

        it('Если модель "compose-attachments" не существует или не валидна и идёт загрузка, должен зарезолвиться', function() {
            this.fakeComposeAttachments.isValid.returns(false);
            this.fakeComposeAttachments.isUploading.returns(true);

            const promise = this.model._waitForAttachmentsUploaded();
            expect(promise.isResolved()).to.be.equal(true);
            return promise;
        });

        it('Если модель "compose-attachments" существует и валидна и не идёт загрузка, должен зарезолвиться', function() {
            this.fakeComposeAttachments.isValid.returns(true);
            this.fakeComposeAttachments.isUploading.returns(false);

            const promise = this.model._waitForAttachmentsUploaded();
            expect(promise.isResolved()).to.be.equal(true);
            return promise;
        });

        it('Если модель "compose-attachments" существует и валидна, идёт загрузка и сработало событие ' +
            '"attachments.all-uploaded", должен зарезолвиться', function() {
            this.fakeComposeAttachments.isValid.returns(true);
            this.fakeComposeAttachments.isUploading.returns(true);

            const promise = this.model._waitForAttachmentsUploaded();

            ns.events.trigger('attachments.all-uploaded');

            expect(promise.isResolved()).to.be.equal(true);

            return promise;
        });

        it('Если модель "compose-attachments" существует и валидна, идёт загрузка и ещё не сработало событие ' +
            '"attachments.all-uploaded", должен не зарезолвиться', function() {
            this.fakeComposeAttachments.isValid.returns(true);
            this.fakeComposeAttachments.isUploading.returns(true);

            const promise = this.model._waitForAttachmentsUploaded();
            expect(promise.isResolved()).to.be.equal(false);

            ns.events.trigger('attachments.all-uploaded');

            return promise;
        });

        it('Если модель "compose-attachments" существует и валидна, идёт загрузка и сработало событие ' +
            '"ns-model:attachments.failed", должен зареджектиться', function() {
            this.fakeComposeAttachments.isValid.returns(true);
            this.fakeComposeAttachments.isUploading.returns(true);

            const promise = this.model._waitForAttachmentsUploaded();

            ns.Model.get('compose-attachments', this.model.params)
                .trigger('ns-model:attachments.failed');

            return promise
                .then(vow.reject)
                .fail(function() {
                    expect(promise.isRejected()).to.be.equal(true);
                    return vow.resolve();
                });
        });
    });

    describe('#_preSend', function() {
        beforeEach(function() {
            this.checkDataPromise = vow.reject();

            this.sinon.stub(this.model, 'cleanErrors');
            this.sinon.stub(this.model, '_checkData').returns(this.checkDataPromise);
            this.sinon.stub(this.model, 'checkConfirmations');

            this.sinon.stub(Daria.SendMail, 'showSendingMessage');

            this.undoLogger = {
                logShow: this.sinon.stub(),
                logCancel: this.sinon.stub(),
                logClose: this.sinon.stub()
            };

            this.sinon.stub(Daria.SendMail, 'getUndoSendLogger').returns(this.undoLogger);
        });

        it('Должен сбрасывать флаг о том, что отправка была отменена', function() {
            this.model._cancelled = true;

            this.model._preSend({});

            expect(this.model._cancelled).to.be.equal(false);
        });

        it('Должен сохранить ссылку на функцию для быстрой отмены отправки', function() {
            this.model._preSend({});
            expect(typeof this.model._cancelSendCallback).to.be.equal('function');
        });

        it('Должен зареджектить промис при вызове функции отмены отправки', function() {
            const promise = this.model._preSend({});

            expect(promise.isResolved()).to.be.equal(false);

            this.model._cancelSendCallback();

            expect(promise.isRejected()).to.be.equal(true);
        });

        it('Должен очистить ошибки перед проверкой полей формы', function() {
            this.model._preSend({});

            expect(this.model.cleanErrors).to.be.calledBefore(this.model._checkData);
        });

        it('Должен вызвать проверку данных на валидность', function() {
            return this.model._preSend({})
                .fail(function() {
                    expect(this.model._checkData).to.have.callCount(1);
                }.bind(this));
        });

        describe('Если данные не валидны →', function() {
            it('Должен оповестить об ошибках валидации', function() {
                this.sinon.stub(this.model, 'storeErrors');
                return this.model._preSend({})
                    .fail(function() {
                        expect(this.model.storeErrors).to.have.callCount(1);
                    }.bind(this));
            });
        });

        describe('Если данные валидны →', function() {
            beforeEach(function() {
                this.model._checkData.returns(vow.resolve());
            });

            it('Должен вернуть отказ "send-cancelled" в случае отмены отправки', function() {
                this.model._checkData.restore();

                this.sinon.stub(this.model, '_checkData').callsFake(() => {
                    this.model._cancelled = true;
                    return vow.resolve();
                });

                return this.model._preSend({})
                    .fail(function(error) {
                        expect(error).to.be.equal('send-cancelled');
                    });
            });

            it('Должен вызвать проверку подтверждений', function() {
                return this.model._preSend({})
                    .then(function() {
                        expect(this.model.checkConfirmations).to.have.callCount(1);
                    }.bind(this));
            });

            it('Не должен вызвать проверку подтверждений, если передан флаг force', function() {
                return this.model._preSend({ force: true })
                    .then(function() {
                        expect(this.model.checkConfirmations).to.have.callCount(0);
                    }.bind(this));
            });

            it('Должен показать нотифайку "Отправляется..." без возможности отмены', function() {
                this.sinon.stub(this.model, 'withUndo').returns(false);

                return this.model._preSend({})
                    .then(() => {
                        expect(Daria.SendMail.showSendingMessage)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly({});
                    });
            });

            describe('Нотифайка "Отправляется..." с возможностью отмены', function() {
                beforeEach(function() {
                    this.sinon.stub(this.model, 'withUndo').returns(true);
                });

                it('Должен показать нотифайку', function() {
                    return this.model._preSend({})
                        .then(() => {
                            expect(Daria.SendMail.showSendingMessage)
                                .to.have.callCount(1)
                                .and
                                .to.be.calledWith(this.sinon.match({
                                    onClose: this.sinon.match.func,
                                    onCancel: this.sinon.match.func
                                }));
                        });
                });

                it('Должен залогировать показ нотифайки', function() {
                    return this.model._preSend({})
                        .then(() => {
                            expect(this.undoLogger.logShow).to.have.callCount(1);
                        });
                });

                describe('Обработчик закрытия нотифайки', function() {
                    beforeEach(function() {
                        this.attachmentsResolve = null;
                        this.attachmentsReject = null;
                        this.attachmentsPromise = new vow.Promise((resolve, reject) => {
                            this.attachmentsResolve = resolve;
                            this.attachmentsReject = reject;
                        });

                        this.sinon.stub(this.model, '_waitForAttachmentsUploaded').callsFake(() => {
                            const onClose = Daria.SendMail.showSendingMessage.getCall(0).args[0].onClose;

                            onClose();

                            this.attachmentsResolve();
                            return this.attachmentsPromise;
                        });
                    });

                    it('Должен залогировать закрытие нотифайки', function() {
                        return this.model._preSend({})
                            .then(() => {
                                expect(this.undoLogger.logClose).to.have.callCount(1);
                            });
                    });
                });

                describe('Обработчик отмены отправки', function() {
                    beforeEach(function() {
                        this.attachmentsResolve = null;
                        this.attachmentsReject = null;
                        this.attachmentsPromise = new vow.Promise((resolve, reject) => {
                            this.attachmentsResolve = resolve;
                            this.attachmentsReject = reject;
                        });

                        this.sinon.stub(this.model, '_waitForAttachmentsUploaded').callsFake(() => {
                            const onCancel = Daria.SendMail.showSendingMessage.getCall(0).args[0].onCancel;

                            onCancel();

                            this.attachmentsResolve();
                            return this.attachmentsPromise;
                        });
                    });

                    it('Должен залогировать закрытие нотифайки', function() {
                        return this.model._preSend({})
                            .fail((error) => {
                                expect(this.undoLogger.logCancel).to.have.callCount(1);
                            });
                    });

                    it('Должен проставить признак "отправка отменена"', function() {
                        expect(this.model._cancelled).to.be.equal(false);

                        return this.model._preSend({})
                            .fail(() => {
                                expect(this.model._cancelled).to.be.equal(true);
                            });
                    });

                    it('Должен зареджектить промис с ошибкой "send-cancelled"', function() {
                        return this.model._preSend({})
                            .fail((error) => {
                                expect(error).to.be.equal('send-cancelled');
                            });
                    });

                    it('Должен очистить функцию быстрой отмены', function() {
                        return this.model._preSend({})
                            .fail(() => {
                                expect(this.model._cancelSendCallback).to.be.equal(null);
                            });
                    });
                });
            });

            it('Должен вернуть промис ожидания загрузки аттачей', function() {
                var attachmentsUploadResult = { unploaded: true };
                this.sinon.stub(this.model, '_waitForAttachmentsUploaded').returns(vow.resolve(attachmentsUploadResult));
                return this.model._preSend({}).then(function(data) {
                    expect(data).to.be.equals(attachmentsUploadResult);
                });
            });
        });
    });

    describe('#_send', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'getData').returns({
                testProp: 'testData'
            });
            this.sinon.stub(this.model, 'makeRequest');
            this.model.makeRequest.callsFake(() => vow.reject());
        });

        it('Должен вернуть отказ "send-cancelled" в случае отмены', function() {
            this.model._cancelled = true;
            return this.model._send()
                .fail((error) => {
                    expect(error).to.be.equal('send-cancelled');
                });
        });

        it('Должен сохранить mid черновика', function() {
            this.sinon.stub(this.model, 'getDraftMid').returns('333');
            this.model._send();

            expect(this.model._draftMid).to.be.equal('333');
        });

        it('Должен поместить mid черновика в список мидов в модели message-failed', function() {
            this.sinon.stub(this.model, 'getDraftMid').returns('333');
            this.sinon.stub(ns.Model.get('message-failed'), 'setMid');
            this.model._send();

            expect(ns.Model.get('message-failed').setMid)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('333');
        });

        it('Должен сохранить ссылку на функцию для быстрой отмены отправки', function() {
            this.model._send();
            expect(typeof this.model._cancelSendCallback).to.be.equal('function');
        });

        it('Должен зареджектить промис при вызове функции отмены отправки', function() {
            const promise = this.model._send();

            expect(promise.isResolved()).to.be.equal(false);

            this.model._cancelSendCallback();

            expect(promise.isRejected()).to.be.equal(true);
        });

        it('Должен отправить запрос с правильно сформированными параметрами', function() {
            this.dataObj = {};
            this.model._prepareDataForMailSend.returns(this.dataObj);

            return this.model._send()
                .fail(() => {
                    expect(this.model.makeRequest).to.be.calledWithExactly({}, '_send=true', 'send');
                });
        });
    });

    describe('#send', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'getData').returns({
                testProp: 'testData'
            });
            this.sinon.stub(this.model, '_checkData').callsFake(() => vow.reject());
            this.sinon.stubMethods(this.model, [ 'checkConfirmations', 'makeRequest', 'trigger' ]);

            this.model.makeRequest.callsFake(() => vow.reject());

            this.undoLogger = {
                logActionOnceAfterCancel: this.sinon.stub(),
                resetStartTime: this.sinon.stub()
            };

            this.sinon.stub(Daria.SendMail, 'getUndoSendLogger').returns(this.undoLogger);
            this.sinon.stub(Daria.SendMail, 'showSendingMessage');
        });

        it('Должен вызвать метод логирования клика в Отправить', function() {
            this.model.send();

            expect(this.undoLogger.logActionOnceAfterCancel)
                .to.have.callCount(1)
                .and
                .to.have.been.calledWithExactly('Клик в Отправить');
        });

        it('Должен сбросить признак "произошла отмена отправки"', function() {
            this.model.send();

            expect(this.undoLogger.cancelled).to.be.eql(false);
        });

        it('Должен сохранить время начала отсчёта для логирования', function() {
            this.model.send();

            expect(this.undoLogger.resetStartTime).to.have.callCount(1);
        });

        it('Должен вызвать проверку данных на валидность', function() {
            this.model.send();

            expect(this.model._checkData).to.have.callCount(1);
        });

        describe('Если данные не валидны →', function() {
            it('Должен оповестить об ошибках валидации', function() {
                this.sinon.stub(this.model, 'storeErrors');
                return this.model.send()
                    .fail(function() {
                        expect(this.model.storeErrors).to.have.callCount(1);
                    }.bind(this));
            });
        });

        it('Должен вызвать проверку подтверждений', function() {
            this.model._checkData.returns(vow.resolve());
            this.model.checkConfirmations.returns(true);

            return this.model.send()
                .fail(function(errStatus) {
                    // Запрос не выполняется, поэтому проверки в reject ветке.
                    expect(this.model.checkConfirmations).to.have.callCount(1);
                    expect(errStatus).to.equal('need-confirm');
                }.bind(this));
        });

        it('Не должен вызвать проверку подтверждений если передан флаг force', function() {
            this.model._checkData.returns(vow.resolve());
            this.model.send({ force: true });

            expect(this.model.checkConfirmations).to.have.callCount(0);
        });

        it('Должен отправить запрос с правильно сформированными параметрами', function() {
            this.dataObj = {};
            this.model._checkData.returns(vow.resolve());
            this.model.checkConfirmations.returns(false);
            this.model._prepareDataForMailSend.returns(this.dataObj);

            return this.model.send().fail(function() {
                expect(this.model.makeRequest).to.be.calledWith({}, '_send=true');
            }.bind(this));
        });

        it('Должен показать нотифайку "Отправляется..."', function() {
            this.model._checkData.returns(vow.resolve());

            return this.model.send().fail(function() {
                expect(Daria.SendMail.showSendingMessage).to.have.callCount(1);
            });
        });

        it('Должен стриггерить на себе событие для сохранения темы письма для автокомплита', function() {
            this.model._checkData.returns(vow.resolve());

            return this.model.send().fail(() => {
                expect(this.model.trigger)
                    .to.be.calledWithExactly('ns-model:store-subject-for-autocomplete');
            });
        });

        describe('Успешная отправка', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, '_preSend').callsFake(() => {
                    this.model._cancelSendCallback = this.sinon.stub();
                    return vow.resolve();
                });

                this.sinon.stub(this.model, '_send').callsFake(() => {
                    this.model._draftMid = '333';
                    this.model._cancelled = true;
                });

                this.sinon.stub(ns.Model.get('message-failed'), 'removeMid');

                return this.model.send();
            });

            it('Должен удалить mid черновика из модели message-failed в случае отмены отправки', function() {
                expect(ns.Model.get('message-failed').removeMid)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly('333');
                expect(this.model._draftMid).to.be.equal(null);
            });

            it('Должен очистить функцию быстрой отмены отправки в случае успешной отправки', function() {
                expect(this.model._cancelSendCallback).to.be.equal(null);
            });
        });

        describe('Должен очистить функцию быстрой отмены отправки в случае неуспешной отправки', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, '_preSend').callsFake(() => {
                    this.model._cancelSendCallback = this.sinon.stub();
                    return vow.resolve();
                });

                this.sinon.stub(this.model, '_send').callsFake(() => {
                    this.model._draftMid = '333';
                    this.model._cancelled = true;
                });

                this.sinon.stub(ns.Model.get('message-failed'), 'removeMid');

                return this.model.send()
                    .fail(() => {
                        return vow.resolve();
                    });
            });

            it('Должен удалить mid черновика из модели message-failed в случае отмены отправки', function() {
                expect(ns.Model.get('message-failed').removeMid)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly('333');
                expect(this.model._draftMid).to.be.equal(null);
            });

            it('Должен очистить функцию быстрой отмены отправки в случае успешной отправки', function() {
                expect(this.model._cancelSendCallback).to.be.equal(null);
            });
        });
    });

    describe('#onXHRError', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'onSendError');
            this.reject = this.sinon.stub();
            this.xhr = {
                responseText: 'test response'
            };
        });

        it('Должен вызвать логирование ошибки', function() {
            this.model.onXHRError(this.xhr, 'test-error', this.reject);

            expect(Jane.ErrorLog.send).to.have.callCount(1);
        });

        it('Должен передать в лог status ошибки', function() {
            this.model.onXHRError(this.xhr, 'test-error', this.reject);

            expect(Jane.ErrorLog.send.getCall(0).args[0].status).to.be.equal('http_status_0');
        });

        it('Должен передать в лог вторым аргументом xhr.responseText', function() {
            this.model.onXHRError(this.xhr, 'test-error', this.reject);

            expect(Jane.ErrorLog.send.getCall(0).args[1]).to.be.equal('test response');
        });

        it('Должен вызвать onSendError', function() {
            this.model.onXHRError(this.xhr, 'test-error', this.reject);

            expect(this.model.onSendError).to.have.callCount(1);
        });

        describe('Если statusText = parseerror ->', function() {
            beforeEach(function() {
                this.model.onXHRError(this.xhr, 'parsererror', this.reject);
            });

            it('Должен залогировать bad_json', function() {
                expect(Jane.ErrorLog.send.getCall(0).args[0].status).to.be.equal('bad_json');
            });

            it('Должен передать код ошибки no_data', function() {
                expect(this.model.onSendError).to.be.calledWith('no_data');
            });
        });

        describe('Если statusText = noauth ->', function() {
            beforeEach(function() {
                this.model.onXHRError(this.xhr, 'no_auth', this.reject);
            });

            it('Должен залогировать NoAuth', function() {
                expect(Jane.ErrorLog.send.getCall(0).args[0].status).to.be.equal('NoAuth');
            });

            it('Не должен вызывать onSendError', function() {
                expect(this.model.onSendError).to.have.callCount(0);
            });
        });

        describe('Если xhr.status = 413 ->', function() {
            beforeEach(function() {
                this.xhr.status = 413;
                this.model.onXHRError(this.xhr, 'test_text', this.reject);
            });

            it('Должен залогировать attachment_too_big', function() {
                expect(Jane.ErrorLog.send.getCall(0).args[0].status).to.be.equal('attachment_too_big');
            });

            it('Должен вызывать onSendError c attachment_too_big', function() {
                expect(this.model.onSendError).to.be.calledWith('attachment_too_big');
            });
        });

        describe('Если xhr.status = 0 ->', function() {
            beforeEach(function() {
                this.xhr.status = 0;
                this.model.onXHRError(this.xhr, 'test_text', this.reject);
            });

            it('Должен залогировать attachment_too_big', function() {
                expect(Jane.ErrorLog.send.getCall(0).args[0].status).to.be.equal('http_status_0');
            });

            it('Должен вызывать onSendError c attachment_too_big', function() {
                expect(this.model.onSendError).to.be.calledWith('http_status_0');
            });
        });
    });

    describe('#onXHRSuccess', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'onSendError');
            this.resolve = this.sinon.stub();
            this.reject = this.sinon.stub();
        });

        it('Должен вызвать resolve если status = ok', function() {
            var data = { status: 'ok' };
            this.model.onXHRSuccess(data, this.resolve, this.reject);

            expect(this.resolve).to.be.calledWith(data);
        });

        it('Должен вызвать onSendError если status = not_ok', function() {
            this.model.onXHRSuccess({ status: 'not_ok' }, this.resolve, this.reject);

            expect(this.model.onSendError).to.be.calledWith('not_ok', this.reject);
        });
    });

    describe('#onSendError', function() {
        beforeEach(function() {
            this.reject = this.sinon.stub();
        });

        it('Должен вызвать логирование ошибки', function() {
            this.model.onSendError('test_error', this.reject);

            expect(Jane.ErrorLog.send).to.have.callCount(1);
        });

        it('Должен вызвать reject передав ему status', function() {
            this.model.onSendError('test_error', this.reject);

            expect(this.reject).to.be.calledWith('test_error');
        });

        it('Должен вызвать reject с internal_error если статус не передан', function() {
            this.model.onSendError(null, this.reject);

            expect(this.reject).to.be.calledWith('internal_error');
        });
    });

    describe('#isSystemLabel', function() {
        beforeEach(function() {
            this.mLabels = ns.Model.get('labels');
            this.waitingForReplyLabel = { lid: '2' };
            this.delayedMessageLabel = { lid: '3' };
            this.sinon.stub(this.mLabels, 'getWaitingForReplyLabel').returns(this.waitingForReplyLabel);
            this.sinon.stub(this.mLabels, 'getDelayedMessageLabel').returns(this.delayedMessageLabel);

            this.getLabelByLid = this.sinon.stub(this.mLabels, 'getLabelById');
        });

        var testsObj = {
            'метка о неответе на письмо': 'getWaitingForReplyLabel',
            'метка об отложенной отправке': 'getDelayedMessageLabel'
        };

        _.each(testsObj, function(mLabelsMethod, suiteName) {
            describe(suiteName + ' →', function() {
                beforeEach(function() {
                    this.label = { lid: '1' };
                    this.mLabels[mLabelsMethod].returns(this.label);
                });

                it('должен вернуть true, если передается эта метка', function() {
                    this.getLabelByLid.returns(this.label);
                    expect(this.model.isSystemLabel('1')).to.be.equal(true);
                });

                it('должен вернуть false, если это не системная метка', function() {
                    this.getLabelByLid.returns({ lid: '4' });
                    expect(this.model.isSystemLabel('4')).to.be.equal(false);
                });
            });
        });
    });

    describe('#addLid', function() {
        beforeEach(function() {
            this.model.set('.lids', null);
        });

        it('должен добавить метку в свойство lids', function() {
            this.model.addLid('123');
            this.model.addLid('456');
            this.model.addLid('789');

            expect(this.model.get('.lids')).to.be.eql([ '123', '456', '789' ]);
        });

        it('не должен добавлять метку, если она уже есть в lids', function() {
            this.model.addLid('123');
            this.model.addLid('123');

            expect(this.model.get('.lids')).to.be.eql([ '123' ]);
        });

        it('должен добавить метку без вызова события, если она относится к системным', function() {
            this.sinon.stub(this.model, 'isSystemLabel').returns(true);
            this.sinon.spy(this.model, 'set');
            this.model.addLid('123');

            expect(this.model.set).to.be.calledWithExactly('.lids', [ '123' ], { jpath: '.lids', silent: true });
        });

        it('должен добавлять массив меток', function() {
            this.model.addLid([ '123', '456' ]);
            this.model.addLid([ '123', '789' ]);

            expect(this.model.get('.lids')).to.be.eql([ '123', '456', '789' ]);
        });
    });

    describe('#removeLid', function() {
        beforeEach(function() {
            this.model.set('.lids', [ '1', '2', '3' ]);
        });

        it('Должен удалить lid-строку из массива lids', function() {
            this.model.removeLid('2');

            expect(this.model.get('.lids')).to.be.eql([ '1', '3' ]);
        });

        it('Должен удалить lid-массив из массива lids', function() {
            this.model.removeLid([ '2', '3' ]);

            expect(this.model.get('.lids')).to.be.eql([ '1' ]);
        });

        it('Должен удалить .lids, если в массиве не осталось меток', function() {
            this.model.removeLid([ '1', '2', '3' ]);

            // проверяем, что поле .lids было удалено из модели
            var isLidsFieldContains = _(this.model.getData()).keys().contains('lids');
            expect(isLidsFieldContains).to.not.be.ok;
        });
    });

    describe('#getFromEmails', function() {
        beforeEach(function() {
            this.model.set('.from_mailbox', 'some@example.ru');
            this.sinon.stub(ns.Model.get('account-information'), 'getFromEmails').returns([
                'test@example.com',
                'some@example.ru',
                'email@example.net'
            ]);
        });

        it('должен подготовить доступные email отправителя для nb-select', function() {
            expect(this.model.getFromEmails()).to.be.eql([
                {
                    text: 'test@example.com',
                    value: 'test@example.com',
                    selected: false
                },
                {
                    text: 'some@example.ru',
                    value: 'some@example.ru',
                    selected: true
                },
                {
                    text: 'email@example.net',
                    value: 'email@example.net',
                    selected: false
                }
            ]);
        });
    });

    describe('#getLanguage', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Translate, 'defineLanguage');
        });

        describe('для ответа на письмо ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, 'params').value({
                    oper: 'reply',
                    ids: '123'
                });
                this._previousMessageBody = this.model.mMessageBody;
                this.model.composeParamsService.mMessageBody = this.model.mMessageBody = {
                    getComposeHTML: this.sinon.stub().returns('test mail')
                };
                this.sinon.stub(this.model, 'isReply').returns(true);
                this.sinon.stub(Daria.Translate, 'getLangByMid');
            });

            afterEach(function() {
                this.model.mMessageBody = this._previousMessageBody;
            });

            it('должен определить язык письма по mid письма, на которое отвечают', function() {
                Daria.Translate.getLangByMid.returns('ru');
                var language = this.model.getLanguage();

                expect(Daria.Translate.getLangByMid).to.be.calledWithExactly(this.model.params.ids);
                expect(language).to.be.equal('ru');
            });

            it('должен определить язык письма по тексту письма, на которое отвечают, если по mid язык не был определен', function() {
                Daria.Translate.getLangByMid.returns(undefined);
                Daria.Translate.defineLanguage.returns('en');
                var language = this.model.getLanguage();

                expect(Daria.Translate.defineLanguage).to.be.calledWithExactly('test mail');
                expect(language).to.be.equal('en');
            });
        });

        describe('для всех остальных писем ->', function() {
            beforeEach(function() {
                this.sinon.stub(this.model, 'get').withArgs('.send').returns('test mail');
                this.sinon.stub(Daria.Config, 'locale').value('be');
                this.sinon.stub(_, 'contains').returns(true);
            });

            it('должен определить язык по тексту письма', function() {
                Daria.Translate.defineLanguage.returns('en');
                var language = this.model.getLanguage();

                expect(Daria.Translate.defineLanguage).to.be.calledWithExactly('test mail');
                expect(language).to.be.equal('en');
            });

            it('должен выбрать язык согласно локали, если он разрешен в сете языков и тело письма пустое', function() {
                this.model.get.withArgs('.send').returns('');
                var language = this.model.getLanguage();

                expect(_.contains).to.be.calledWithExactly(Daria.Translate.langs.all.s, 'be');
                expect(language).to.be.equal('be');
            });

            it('должен выбрать ru, если язык локали не присутствует в сете и тело письма пустое', function() {
                this.model.get.withArgs('.send').returns('');
                _.contains.returns(false);
                var language = this.model.getLanguage();

                expect(language).to.be.equal('ru');
            });
        });
    });

    describe('#getContacts', function() {
        beforeEach(function() {
            this.splitPhonesResult = [ 'split', 'phones' ];
            this.splitContactsResult = [ 'split', 'contacts' ];

            this.sinon.stub(this.model, 'get').returns('contacts');
            this.sinon.stub(Jane.FormValidation, 'splitPhones').returns(this.splitPhonesResult);
            this.sinon.stub(Jane.FormValidation, 'splitContacts').returns(this.splitContactsResult);
        });

        it(
            'Если параметр функции принимает значение "phone", то должен выполнить разделение строки на телефоны',
            function() {
                expect(this.model.getContacts('phone')).to.eql(this.splitPhonesResult);

                expect(Jane.FormValidation.splitPhones).have.callCount(1);
                expect(Jane.FormValidation.splitPhones).to.have.been.calledWith('contacts');

                expect(Jane.FormValidation.splitContacts).have.callCount(0);
            }
        );

        it(
            'Если параметр функции не принимает значение "phone", то должен выполнить разделение строки на контакты',
            function() {
                expect(this.model.getContacts('to')).to.eql(this.splitContactsResult);

                expect(Jane.FormValidation.splitContacts).have.callCount(1);
                expect(Jane.FormValidation.splitContacts).to.have.been.calledWith('contacts');

                expect(Jane.FormValidation.splitPhones).have.callCount(0);
            }
        );
    });

    describe('#appendContact', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [
                'getContacts',
                'setContacts'
            ]);
        });

        it('Добавляет контакт в переданное поле', function() {
            var fieldName = 'to';
            this.model.getContacts.withArgs(fieldName).returns([ 1, 2 ]);

            this.model.appendContact(fieldName, 3);

            expect(this.model.setContacts).to.be.calledWith(fieldName, [ 1, 2, 3 ]);
        });
    });

    describe('#setIfChanged', function() {
        beforeEach(function() {
            this.setIfChanged = this.sinon.stub(ns.Model.prototype, 'setIfChanged');
            this.sinon.stub(this.model, 'get');
        });

        describe('Для полей to, cc и bcc удаляет последнюю незначащую запятую и все пустые символы перед концом строки →', function() {
            beforeEach(function() {
                this.path = '.to';
            });

            it('Должен запустить базовый #setIfChanged, если после вырезания значения не совпадают', function() {
                this.model.get.returns('"Ivan Dydka22" <dydka22@yandex.ru>, ');

                this.model.setIfChanged(this.path, '"Ivan Dydka22" <dydka22@yandex.ru>, aaa', { data: {} });

                expect(this.setIfChanged).to.be
                    .calledWithExactly(this.path, '"Ivan Dydka22" <dydka22@yandex.ru>, aaa', { data: {} });
            });

            it('Не должен запустить базовый #setIfChanged, если после вырезания значения совпадают', function() {
                this.model.get.returns('"Ivan Dydka22" <dydka22@yandex.ru>, ');

                this.model.setIfChanged(this.path, '"Ivan Dydka22" <dydka22@yandex.ru>');

                expect(this.setIfChanged).to.have.callCount(0);
            });
        });

        it('Для других полей #setIfChanged должен вызываться безусловно', function() {
            var path = '.subj';

            this.model.get.returns('Тема');
            this.model.setIfChanged(path, 'Тема');

            expect(this.setIfChanged).to.be.calledWith(path, 'Тема');
        });
    });

    describe('#isReplyAny', function() {
        it('Вызывает composeParamsService', function() {
            this.sinon.stub(this.model.composeParamsService, 'isReplyAny');

            this.model.isReplyAny();

            expect(this.model.composeParamsService.isReplyAny).to.be.called;
        });
    });

    describe('#isNewMessage', function() {
        it('Письмо является новым, если у письма нет ids', function() {
            this.sinon.stub(this.model, 'params').value({ ids: null });
            expect(this.model.isNewMessage()).to.be.ok;
        });
        it('Письмо не является новым, если у письма есть ids', function() {
            this.sinon.stub(this.model, 'params').value({ ids: '33' });
            expect(this.model.isNewMessage()).not.to.be.ok;
        });
    });

    describe('#isForward', function() {
        it('Вызывает composeParamsService', function() {
            this.sinon.stub(this.model.composeParamsService, 'isForward');

            this.model.isForward();

            expect(this.model.composeParamsService.isForward).to.be.called;
        });
    });

    describe('#isReplyAll', function() {
        it('Вызывает composeParamsService', function() {
            this.sinon.stub(this.model.composeParamsService, 'isReplyAll');

            this.model.isReplyAll();

            expect(this.model.composeParamsService.isReplyAll).to.be.called;
        });
    });

    describe('#isDraft', function() {
        it('Вызывает composeParamsService', function() {
            this.sinon.stub(this.model.composeParamsService, 'isDraft');

            this.model.isDraft();

            expect(this.model.composeParamsService.isDraft).to.be.called;
        });
    });

    describe('#isTemplate', function() {
        it('Вызывает composeParamsService', function() {
            this.sinon.stub(this.model.composeParamsService, 'isTemplate');

            this.model.isTemplate();

            expect(this.model.composeParamsService.isTemplate).to.be.called;
        });
    });

    describe('#isNewTemplate', function() {
        it('Письмо является новым шаблоном, save_symbol === "template", но ещё не создан объект письма', function() {
            var mMessage = this.model.mMessage;
            this.model.mMessage = null;
            this.model.set('.save_symbol', 'template');

            expect(this.model.isNewTemplate()).to.be.ok;

            this.model.mMessage = mMessage;
        });

        it('Письмо не является новым шаблоном, если save_symbol !== "template"', function() {
            var mMessage = this.model.mMessage;
            this.model.mMessage = null;
            this.model.set('.save_symbol', 'draft');

            expect(this.model.isNewTemplate()).to.not.be.ok;

            this.model.mMessage = mMessage;
        });

        it('Письмо не является новым шаблоном, если объект письма уже создан', function() {
            expect(this.model.isNewTemplate()).to.not.be.ok;
        });
    });

    describe('#withUndo', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, 'getUndoSendTime').returns(5000);
            this.model._isIgnoreUndo = false;
        });

        it('должен вернуть true', function() {
            expect(this.model.withUndo()).to.be.equal(true);
        });

        it('должен вернуть false (неверное значение настройки "время отмены отправки")', function() {
            Daria.SendMail.getUndoSendTime.returns(0);
            expect(this.model.withUndo()).to.be.equal(false);
        });

        it('должен вернуть false (отправка с возможностью отмены завершилась неудачей)', function() {
            this.model._isIgnoreUndo = true;
            expect(this.model.withUndo()).to.be.equal(false);
        });
    });

    describe('#isMessageCollapsed', function() {
        it('При ответе на письмо, текст цитирования и подпись не подставляются в текст ответа', function() {
            this.sinon.stub(this.model, 'params').value({ ids: '5', oper: 'reply' });
            this.mMessageBody.getInfo.withArgs('delivered-to').returns([ '42' ]);
            this.mMessageBody.getRecipients.returns({ to: 'test', cc: 'test' });

            return this.model.request().then(() => {
                this.model.set('.collapsedMessage', true);
                expect(this.model.isMessageCollapsed()).to.be.ok;
            });
        });
    });

    describe('#getSendMessage', function() {
        it('Должен вернуть тело ответа с подклееным текстом письма, на которое отвечаем', function() {
            this.sinon.stub(this.model, 'params').value({ ids: '5', oper: 'reply' });
            this.sinon.stub(this.model, 'isReplyAny').returns(true);
            this.sinon.stub(this.model, 'isMessageCollapsed').returns(true);
            this.mMessageBody.getInfo.withArgs('delivered-to').returns([ '42' ]);
            this.mMessageBody.getRecipients.returns({ to: 'test', cc: 'test' });
            this.mMessageBody.getReplyBody.returns('test');

            return this.model.request().then(() => {
                this.model.set('.send', 'qwe');
                var message = this.model.getSendMessage();
                expect(message).to.be.equal('qwetest');
            });
        });
    });

    describe('#getDraftMid', function() {
        it('Должен вернуть значение overwrite если письмо сохранено в черновики', function() {
            this.sinon.stub(this.model, 'isDraft').returns(true);
            this.model.set('.overwrite', '123');
            expect(this.model.getDraftMid()).to.be.equal('123');
        });
    });

    describe('#getTemplateMid', function() {
        it('Должен вернуть значение overwrite, если письмо сохранено в шаблоны', function() {
            this.sinon.stub(this.model, 'isTemplate').returns(true);
            this.model.set('.overwrite', '123');
            expect(this.model.getTemplateMid()).to.be.equal('123');
        });
    });

    describe('#hasAttach', function() {
        it('Должен сказать, что аттачей нет, если нет прикрепленных аттачей', function() {
            expect(this.model.hasAttach()).to.not.be.ok;
        });

        it('Должен сказать, что аттачи есть, если есть прикрепленные аттачи', function() {
            this.model._attachments = {
                0: {}
            };

            expect(this.model.hasAttach()).to.be.ok;
        });
    });

    describe('#isDelayed', function() {
        it('должен вернуть true, если письмо с отложенной отправкой', function() {
            this.model.set('.send_time', 1433404800000);

            expect(this.model.isDelayed()).to.be.equal(true);
        });

        it('должен вернуть false, если письмо без отложенной отправки', function() {
            expect(this.model.isDelayed()).to.be.equal(false);
        });
    });

    describe('#getPassportSendDate', function() {
        it('должен вернуть время из send_time, преобразованное в часовой пояс из паспорта', function() {
            this.sinon.stub(this.model, 'get').withArgs('.send_time').returns('time');
            this.sinon.stub(Jane.Date, 'parseCalendarDate').withArgs('time').returns('parsed time');
            this.sinon.stub(Daria, 'getPassportDate').withArgs('parsed time').returns('passport time');

            expect(this.model.getPassportSendDate()).to.be.equal('passport time');
        });

        it('должен вернуть undefined если send_time не задан', function() {
            this.sinon.stub(this.model, 'get').withArgs('.send_time').returns(undefined);
            this.sinon.stub(Jane.Date, 'parseCalendarDate');
            this.sinon.stub(Daria, 'getPassportDate');

            expect(this.model.getPassportSendDate()).to.be.equal(undefined);
        });
    });

    describe('#needCollapsedQuotedMessage', function() {
        beforeEach(function() {
            this.model.needCollapsedQuotedMessage.restore();
        });

        it('должен вернуть false, если открыта страница композа', function() {
            this.sinon.stub(ns.page.current, 'page').value('compose2');
            expect(this.model.needCollapsedQuotedMessage()).to.be.equal(false);
        });

        it('должен вернуть true, если открыта не страница композа', function() {
            this.sinon.stub(ns.page.current, 'page').value('test');
            expect(this.model.needCollapsedQuotedMessage()).to.be.equal(true);
        });
    });

    describe('#getAllRecepientEmails', function() {
        beforeEach(function() {
            this._originalExtractEmailsFromContacts = this.model._extractEmailsFromContacts;
            this.model._extractEmailsFromContacts = function(contacts) {
                return contacts;
            };

            this.sinon.stub(this.model, 'getContacts');
        });

        afterEach(function() {
            this.model._extractEmailsFromContacts = this._originalExtractEmailsFromContacts;
        });

        it('должен вернуть объект с пустыми массивами, если получатели не заполнены', function() {
            this.model.getContacts.returns([]);
            expect(this.model.getAllRecepientEmails()).to.be.eql({
                to: [],
                cc: [],
                bcc: []
            });
        });

        it('должен вернуть объект с массивами получателей, если получатели заполнены', function() {
            this.model.getContacts.withArgs('to').returns([ 'aa@bb.ru', 'cc@dd.ru' ]);
            this.model.getContacts.withArgs('cc').returns([ 'ee@ff.ru', 'hh@gg.ru' ]);
            this.model.getContacts.withArgs('bcc').returns([ 'zz@xx.ru', 'ww@vv.ru' ]);

            expect(this.model.getAllRecepientEmails()).to.be.eql({
                to: [ 'aa@bb.ru', 'cc@dd.ru' ],
                cc: [ 'ee@ff.ru', 'hh@gg.ru' ],
                bcc: [ 'zz@xx.ru', 'ww@vv.ru' ]
            });
        });
    });

    describe('#makeRequest', function() {
        it('должен вернуть отказ "send-cancelled" в случае отмены', function() {
            this.model._cancelled = true;

            return this.model.makeRequest({}, '', 'send')
                .fail(function(error) {
                    expect(error).to.be.equal('send-cancelled');
                });
        });

        it('должен проставить в данных templates_fid', function() {
            this.sinon.stub(this.model, 'isTemplate').returns(true);
            this.sinon.stub(this.model, 'get').withArgs('.templates_fid').returns('33');

            const data = {};
            this.model.makeRequest(data, '', 'send');

            expect(data.templates_fid).to.be.equal('33');
        });

        describe('выполнение запроса', function() {
            beforeEach(function() {
                this.data = {
                    overwrite: '332'
                };
            });

            it('должен сохранить функцию для быстрой отмены отправки', function() {
                this.model.makeRequest(this.data, '', 'send');

                expect(typeof this.model._cancelSendCallback).to.be.equal('function');
            });

            describe('Вызов функции быстрой отмены отправки', function() {
                beforeEach(function() {
                    this.sinon.stub(Daria.SendMail, 'showMessageSentCancelling');

                    this.promise = this.model.makeRequest(this.data, '', 'send');
                    this.model._cancelSendCallback();
                });

                it('не должен отвергнуть промис запроса', function() {
                    expect(this.promise.isRejected()).to.be.equal(false);
                });

                it('должен залогировать отмену отправки в момент, когда запрос уже начал выполняться', function() {
                    expect(Jane.ErrorLog.send)
                        .to.have.callCount(1)
                        .and
                        .to.be.calledWithExactly({
                            errorType: 'cancel_send_while_sending',
                            overwriteMid: '332'
                        });
                });

                it('должен показать нотифайку об том, что выполняется отмена отправки', function() {
                    expect(Daria.SendMail.showMessageSentCancelling).to.have.callCount(1);
                });
            });

            it('должен выполнить ajax запрос с нужными параметрами', function() {
                this.model.makeRequest(this.data, 'fake_query', 'send');

                expect($.ajax)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWith(
                        this.sinon.match({
                            url: Daria.api['do-send'] + '?fake_query',
                            method: 'POST',
                            dataType: 'json',
                            cache: false,
                            data: this.data,
                            success: this.sinon.match.func,
                            error: this.sinon.match.func,
                            complete: this.sinon.match.func
                        })
                    );
            });

            describe('запрос выполнен успешно', function() {
                beforeEach(function() {
                    this.sinon.stub(this.model, 'onXHRSuccess');
                    this.sinon.stub(this.model, 'onXHRError');

                    this.promise = this.model.makeRequest(this.data, 'fake_query', 'send');

                    this.success = $.ajax.getCall(0).args[0].success;
                    this.data = {};
                    this.statusText = '?';
                    this.xhr = {};
                });

                describe('Сменилась / протухла авторизация', function() {
                    beforeEach(function() {
                        this.sinon.stub(Daria, 'parseLoginInfo').withArgs(this.data).returns(false);
                    });

                    it('должен вызвать обработчик со статусом "нет авторизации"', function() {
                        this.success(this.data, this.statusText, this.xhr);

                        expect(this.model.onXHRError)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWith(this.xhr, 'no_auth');
                    });
                });

                describe('Дефолтный обработчик', function() {
                    beforeEach(function() {
                        this.sinon.stub(Daria, 'parseLoginInfo').withArgs(this.data).returns(true);
                        this.sinon.stub(this.model, '_cancelIfNeeded');

                        this.success(this.data, this.statusText, this.xhr);
                    });

                    it('должен вызвать обработчик успешного запроса', function() {
                        expect(this.model.onXHRSuccess)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly(this.data, this.sinon.match.func, this.sinon.match.func, 'send');
                    });

                    it('должен попробовать запустить отмену отправки', function() {
                        expect(this.model._cancelIfNeeded)
                            .to.have.callCount(1)
                            .and
                            .to.be.calledWithExactly(this.data, false);
                    });
                });
            });
        });
    });

    describe('#_cancelIfNeeded', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.SendMail, 'cancelSendMessage');
        });

        it('не должен запускать отмену отправки (в данных нет mid-а отправленного сообщения)', function() {
            const data = {};
            this.model._cancelled = true;

            this.model._cancelIfNeeded(data);

            expect(Daria.SendMail.cancelSendMessage).to.have.callCount(0);
        });

        it('не должен запускать отмену отправки (пользователь не нажимал на отмену)', function() {
            const data = { storedmid: '333' };
            this.model._cancelled = false;

            this.model._cancelIfNeeded(data);

            expect(Daria.SendMail.cancelSendMessage).to.have.callCount(0);
        });

        it('должен запустить отмену отправки', function() {
            const data = { storedmid: '333' };
            this.model._cancelled = true;

            this.model._cancelIfNeeded(data, true);

            expect(Daria.SendMail.cancelSendMessage)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('333', 'sending_message', true);
        });
    });
});
