describe('Daria.Recipients', function() {
    function getCacheId(recipient) {
        return recipient.name ? '"' + recipient.name + '" <' + recipient.email + '>' : '<' + recipient.email + '>';
    }

    beforeEach(function() {
        this.tmp = {
            className: 'mail-User-Avatar_header js-user-picture',
            email: 'test@yandex.ru',
            name: 'Test',
            size: 42,
            mid: '12212',
            tid: undefined,
            participantType: 'from',
            single: true
        };

        this.Recipients = Daria.Recipients;
    });

    afterEach(function() {
        this.Recipients._queue = [];
    });

    describe('#add', function() {
        beforeEach(function() {
            this.sinon.stub(this.Recipients, '_getMessageInfo').returns({});
            this.Recipients._recipients = [];
            this.Recipients._shouldUpdate = [];
            this.Recipients._queue = [];
            this.Recipients._cache = {};
        });

        it('должен отправить запрос в котопса', function() {
            this.sinon.stub(this.Recipients, '_request');
            this.Recipients.add(this.tmp);

            expect(this.Recipients._request).to.have.callCount(1);
        });

        describe('должен сгенерить ключ для обращения к кэшу', function() {
            it('из имени и адреса почты, если есть имя', function() {
                this.Recipients.add(_.extend({}, this.tmp, { id: '1' }));

                expect(this.Recipients._recipients['1'].cacheId).to.be.eq(getCacheId(this.tmp));
            });

            it('из адреса почты, если нет имени', function() {
                var cacheId = getCacheId(_.extend({}, this.tmp, { name: '' }));
                this.Recipients.add(_.extend({}, this.tmp, { id: '1', name: '' }));

                expect(this.Recipients._recipients['1'].cacheId).to.be.eq(cacheId);
            });
        });

        it('должен присвоить дефолтный размер аватарки, если переданный невалиден', function() {
            this.Recipients.add(_.extend({}, this.tmp, { id: '1', size: 213214 }));

            expect(this.Recipients._recipients['1'].size).to.be.eq(Daria.Constants.AVATARS.DEFAULT_SIZE);
        });

        it('должен добавить реципиента в очередь для обновления', function() {
            this.Recipients.add(_.extend({}, this.tmp, { id: '1' }));

            expect(this.Recipients._shouldUpdate.length).to.be.eq(1);
        });

        it('не должен добавить реципиента в очередь для обновления, если уже есть реципиент с таким id', function() {
            this.Recipients.add(_.extend({}, this.tmp, { id: '1' }));
            this.Recipients.add(_.extend({}, this.tmp, { id: '1' }));

            expect(this.Recipients._shouldUpdate.length).to.be.eq(1);
        });

        it('должен добавить реципиента в очередь для обновления, если уже есть реципиент с таким id и' +
            'передан параметр force', function() {
            this.Recipients.add(_.extend({}, this.tmp, { id: '1' }));
            this.Recipients.add(_.extend({}, this.tmp, { id: '1' }), true);

            expect(this.Recipients._shouldUpdate.length).to.be.eq(2);
        });

        it('не должен добавить в очередь загрузки, если она уже есть в кэше', function() {
            this.Recipients._cache[getCacheId(this.tmp)] = this.Recipients._normalizeRecipientData(this.tmp);

            expect(this.Recipients._queue.length).to.be.eq(0);
        });

        it('не должен добавить в очередь загрузки, если она уже в ней есть', function() {
            this.Recipients._queue.push(getCacheId(this.tmp));

            expect(this.Recipients._queue.length).to.be.eq(1);
        });

        it('должен добавить в очередь загрузки, если передан флаг force', function() {
            this.Recipients.add(_.extend({}, this.tmp, { id: 1 }));
            this.Recipients.add(_.extend({}, this.tmp, { id: 1 }), true);

            expect(this.Recipients._queue.length).to.be.eq(1);
        });

        it('должен вернуть вьюшку аватарки', function() {
            var id = 1;
            this.sinon.stub(this.Recipients, '_getAvatarView');
            this.Recipients.add(_.extend({}, this.tmp, { id: id }));

            expect(this.Recipients._getAvatarView).to.be.calledWith(id);
        });

        it('должен добавить свойство showUserAvatar либо оставить его без изменения', function() {
            var id = 1;
            this.Recipients.add(_.extend({}, this.tmp, { id: id }));

            expect(typeof this.Recipients.getRecipient(id).showUserAvatar).to.be.eq('boolean');
        });

        it('должен вызвать "_getMessageInfo" с параметрами', function() {
            this.Recipients.add(this.tmp);

            expect(this.Recipients._getMessageInfo).to.be.calledWith({
                mid: this.tmp.mid,
                tid: this.tmp.tid,
                participantType: this.tmp.participantType,
                single: this.tmp.single
            });
        });

        it('должен вызвать "_createHref"', function() {
            this.sinon.stub(this.Recipients, '_createHref');
            this.Recipients.add(this.tmp);
            var message = this.Recipients._getMessageInfo({
                mid: this.tmp.mid,
                tid: this.tmp.tid,
                participantType: this.tmp.participantType,
                single: this.tmp.single
            });

            expect(this.Recipients._createHref).to.be.calledWith({
                href: this.tmp.href,
                message: message,
                title: this.tmp.title,
                participantType: this.tmp.participantType,
                email: this.tmp.email
            });
        });
    });

    describe('#_extractData', function() {
        beforeEach(function() {
            this.Recipients._queue = [ 1, 2, 3 ];
            this.mRecipients = ns.Model.get('recipients');
            this.sinon.stub(ns.Model, 'get').withArgs('recipients').returns(this.mRecipients);
            this.sinon.stub(this.Recipients, '_normalizeRecipientData');
            this.sinon.stub(this.Recipients, '_updateAvatarsViews');
            this.sinon.stub(this.mRecipients, 'destroy');
            this.sinon.stub(this.mRecipients, 'getData').returns({
                '"АФИША" <afisha@newsletters.afisha.ru>': {
                    displayName: 'АФИША',
                    type: 'icon',
                    color: '#C82020',
                    mono: 'А',
                    local: 'afisha',
                    url: 'afisha.ru',
                    domain: 'newsletters.afisha.ru',
                    valid: true,
                    email: 'АФИША <afisha@newsletters.afisha.ru>'
                },
                '"Facebook" <notification+kjdvmpj1_11m@facebookmail.com>': {
                    displayName: 'Facebook',
                    type: 'icon',
                    color: '#56bf68',
                    mono: 'F',
                    local: 'notification+kjdvmpj1_11m',
                    url: 'facebook.com',
                    domain: 'facebookmail.com',
                    valid: true,
                    email: 'Facebook <notification+kjdvmpj1_11m@facebookmail.com>'
                }
            });
        });

        it('должен вызвать перерисовку аватарок', function() {
            this.Recipients._extractData([ this.mRecipients ]);

            expect(this.Recipients._updateAvatarsViews).to.have.callCount(1);
        });

        it('должен вызвать нормализацию данных для каждого реципиента', function() {
            this.Recipients._extractData([ this.mRecipients ]);

            expect(this.Recipients._normalizeRecipientData).to.have.callCount(2);
        });

        it('должен уничтожить модель', function() {
            this.Recipients._extractData([ this.mRecipients ]);

            expect(this.mRecipients.destroy).to.have.callCount(1);
        });
    });

    describe('#_normalizeRecipientData', function() {
        beforeEach(function() {
            this.sinon.stub(this.Recipients, '_getSocialavatarsUrl');
        });

        it('должен добавить в объект фолбэк на фантомаса', function() {
            this.Recipients._getSocialavatarsUrl.restore();
            var recipient = this.Recipients._normalizeRecipientData(this.tmp);

            expect(typeof recipient.fallbackAvatar).to.be.eq('string');
        });

        it('должен вызвать _getSocialavatarsUrl для генерации фолбэка', function() {
            this.Recipients._normalizeRecipientData(this.tmp);

            expect(this.Recipients._getSocialavatarsUrl).to.have.calledWith('person');
        });

        it('должен вызвать _getSocialavatarsUrl для генерации фолбэка с пареметром "group", если передан флаг',
            function() {
                this.Recipients._normalizeRecipientData(this.tmp, true);

                expect(this.Recipients._getSocialavatarsUrl).to.have.calledWith('group');
            });

        it('должен вызвать _getSocialavatarsUrl для ссыдки на иконку, если тип icon', function() {
            var icon = 'github.com';
            this.Recipients._normalizeRecipientData(_.extend({}, this.tmp, { type: 'icon', url: icon }));

            expect(this.Recipients._getSocialavatarsUrl).to.have.calledWith(icon);
        });

        it('должен добавить свойство shortMono', function() {
            var recipient = this.Recipients._normalizeRecipientData(this.tmp);

            expect(typeof recipient.shortMono).to.be.eq('boolean');
        });
    });

    describe('#_getAvatarView', function() {
        beforeEach(function() {
            this.sinon.stub(this.Recipients, 'getRecipient');
            this.sinon.stub(ns, 'renderString');
        });

        it('должен вызвать getRecipient с переданным id', function() {
            var id = 1;
            this.Recipients._getAvatarView(id);

            expect(this.Recipients.getRecipient).to.have.calledWith(id);
        });

        it('должен вызвать renderString', function() {
            var recipient = _.extend({}, this.Recipients._getAvatarView(1), { isIE: false });

            expect(ns.renderString).to.have.calledWith(recipient, 'js-avatars', 'mail');
        });
    });

    describe('#_updateAvatarsViews', function() {
        beforeEach(function() {
            this.Recipients._shouldUpdate = [ 1, 2, 3 ];
        });

        it('должен отчищать очередь обновлений', function() {
            this.Recipients._updateAvatarsViews();

            expect(this.Recipients._shouldUpdate.length).to.be.eq(0);
        });
    });

    describe('#update', function() {
        beforeEach(function() {
            this.args = [ 1, 2, 3 ];
            this.sinon.stub(this.Recipients, 'getRecipient');
            this.sinon.stub(this.Recipients, 'add');
        });

        it('должен добавить аватарку с флагом force', function() {
            var that = this;
            this.Recipients.update(this.args);

            this.args.forEach(function(recipientId) {
                expect(that.Recipients.add).to.have.calledWith(that.Recipients.getRecipient(recipientId), true);
            });
        });

        it('должен вызвать getRecipient для каждого id', function() {
            this.Recipients.update(this.args);

            expect(this.Recipients.getRecipient).to.have.callCount(this.args.length);
        });
    });
});
