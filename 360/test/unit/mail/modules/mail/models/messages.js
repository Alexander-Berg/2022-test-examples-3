describe('Daria.mMessages', function() {
    beforeEach(function() {
        this.sinon.stub(Daria.Config, 'layout').value('2pane');

        this.model = ns.Model.get('messages');
        setModelByMock(this.model);

        setModelsByMock('messages');
    });

    afterEach(function() {
        this.model.destroy();
    });

    describe('#_unsetRefreshCount', function() {
        it('Должен сбросить счетчик загруженных писем', function() {
            this.model._refreshCount = 10;
            this.model._unsetRefreshCount();

            expect(this.model._refreshCount).to.be.equal(undefined);
        });
    });

    describe('#_setRefreshCount', function() {
        it('Должен установить счетчик загруженных писем равный колличеству моделей', function() {
            this.model._refreshCount = 10;
            this.model._setRefreshCount();

            expect(this.model._refreshCount).to.be.equal(this.model.models.length);
        });
    });

    describe('#_hasParamsForRequest', function() {
        it('При изменении параметров модели должны меняться проверки', function() {
            // ВАЖНО!!! Если меняются параметры модели, то нужно и добавлять проверки в _hasParamsForRequest
            // чтобы определять, есть ли нужные параметры для запроса сообщений с сервера, если проверку не добавить,
            // то корректное поведение не гарантированно

            expect(ns.Model.infoLite('messages').params).to.be.eql({
                threaded: null,
                goto: null,
                filter_ids: null,
                current_folder: null,
                current_label: null,
                thread_id: null,
                extra_cond: null,
                page_number: null,
                serviceId: null,
                sort_type: 'date',
                request: null,
                scope: null,
                unread: null,
                datePager: null,
                mrange: null,
                from: null,
                to: null,
                hdr_from: null,
                hdr_to: null,
                fid: null,
                lid: null,
                excluded: null,
                search: null,
                force: null,
                type: null,
                attaches: null,
                first: null,
                count: null,
                with_pins: 'yes',
                allow_empty: null,
                prevent_update: null,
                tabId: null,
                deleted: null
            });
        });

        [ {
            name: 'запрос поиска',
            params: {
                search: 'search'
            }
        }, {
            name: 'запрос ближайших сообщений',
            params: {
                deviation: true
            }
        }, {
            name: 'запрос по папке',
            params: {
                current_folder: '1'
            }
        }, {
            name: 'запрос по треду',
            params: {
                thread_id: 't1'
            }
        }, {
            name: 'запрос по метке',
            params: {
                current_label: '1'
            }
        }, {
            name: 'запрос только новых',
            params: {
                goto: 'all',
                extra_cond: 'only_new'
            }
        }, {
            name: 'запрос писем с аттачами',
            params: {
                goto: 'all',
                extra_cond: 'only_atta'
            }
        }, {
            name: 'запрос с фильтрами',
            params: {
                filter_ids: '123'
            }
        } ].forEach(function(opts) {
            it('Должен вернуть true, если ' + opts.name, function() {
                this.sinon.stub(this.model, 'params').value(opts.params);

                expect(this.model._hasParamsForRequest()).to.be.equal(true);
            });
        });

        it('Если нет ни одного из параметров, то должен вернуть false', function() {
            this.sinon.stub(this.model, 'params').value({});

            expect(this.model._hasParamsForRequest()).to.be.equal(false);
        });
    });

    describe('#_fakeRequest', function() {
        it('При вызове должен выставить модели пустые данные и удалить метод из model.request', function() {
            this.model.request = this.model._fakeRequest;
            this.model._fakeRequest();

            expect(this.model.request).to.be.equal(undefined);
            expect(this.model.getData()).to.be.eql({
                details: { 'has-more': false },
                message: []
            });
        });
    });

    describe('#isFakeMessages', function() {
        it('Должен вернуть false, если можем запросить messages, и есть нормальные параметры ' +
            'и true если можем запросить messages, но у них нет нормальных параметров', function() {
            var opts = [
                { params: { extra_cond: 'only_new', unread: 'unread', goto: 'all' }, expectValue: false },
                { params: { extra_cond: 'only_atta', goto: 'all' }, expectValue: false },
                { params: { tabId: 'relevant' }, expectValue: false },
                { params: { not_valid_param: 'default' }, expectValue: true }
            ];

            opts.forEach(function(opt) {
                this.sinon.stub(this.model, 'params').value(opt.params);
                expect(this.model.isFakeMessages()).to.be.eql(opt.expectValue);
            }, this);
        });
    });

    describe('#getUnreads', function() {
        it('должен вернуть массив с непрочитанными сообщениями', function() {
            expect(this.model.getUnreads()).to.be.eql([
                this.model.models[0]
            ]);
        });

        it('должен вернуть пустой массив, если все все сообщения прочитаны', function() {
            this.model.models[0].mark();
            expect(this.model.getUnreads()).to.be.eql([]);
        });
    });

    describe('#getUnreadCount', function() {
        it('Должен вернуть количество непрочитанных писем в коллекции', function() {
            expect(this.model.getUnreadCount()).to.be.equal(1);
        });
    });

    describe('Параметры модели', function() {
        it('должен удалить thread_id, если есть current_folder и thread_id', function() {
            var model = ns.Model.get('messages', {
                current_folder: '1',
                thread_id: 't1'
            });

            expect(model.params).to.be.eql({
                current_folder: '1',
                sort_type: 'date',
                with_pins: 'yes'
            });
        });

        it('должен добавить with_pins, если Daria.messages.checkUsePinsByParams вернула true', function() {
            this.sinon.stub(Daria.messages, 'checkUsePinsByParams').returns(true);

            var model = ns.Model.get('messages', {
                current_folder: '1'
            });

            expect(model.params).to.be.eql({
                current_folder: '1',
                sort_type: 'date',
                with_pins: 'yes'
            });
        });

        it('должен удалить with_pins, если Daria.messages.checkUsePinsByParams вернула false', function() {
            this.sinon.stub(Daria.messages, 'checkUsePinsByParams').returns(false);

            var model = ns.Model.get('messages', {
                current_folder: '1',
                with_pins: 'yes'
            });

            expect(model.params).to.be.eql({
                current_folder: '1',
                sort_type: 'date'
            });
        });

        // Для параметра with_pins пришлось указать дефолтное значение, чтобы была одна и та же последовательность
        // параметров в ключе.
        // @see https://github.com/yandex-ui/noscript/issues/643
        // Баг воспроизводился, когда после with_pins был добавлен параметр top_only и в тесте ниже
        // вместо scope - использовался параметр top_only.
        it('ключ модели Daria.mMessages строится одинаково для разных наборов параметров (набор параметров ' +
            'по сути один и тот же, отличие - в последовательности параметров)', function() {
            var m1 = ns.Model.get('messages', { fid: 333 });
            var m2 = ns.Model.get('messages', { fid: 333, scope: 'rpopinfo' });
            var m3 = ns.Model.get('messages', _.extend({}, m1.params, { scope: 'rpopinfo' }));

            // Проверяем, что получился один и тот же ключ несмотря на то, что параметры переданы разными способами.
            expect(m2.key).to.be.equal(m3.key);
        });
    });

    describe('Реакция на события ->', function() {
        describe('"ns-model-remove" ->', function() {
            beforeEach(function() {
                this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                    mid: '1',
                    tid: 't1'
                });

                this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                    last_mid: '1',
                    mid: 't1',
                    tid: 't1'
                });
            });

            it('не должен вызвать #updateThreadMeta, если модель треда невалидна', function() {
                var mMessages = ns.Model.get('messages', { thread_id: 't1' });
                this.sinon.stub(mMessages, 'updateThreadMeta');

                mMessages.remove(this.mMessage);

                expect(mMessages.updateThreadMeta).to.have.callCount(0);
            });

            it('не должен вызвать #updateThreadMeta, если это не модель треда', function() {
                var mMessages = ns.Model.get('messages', { current_folder: '123' }).setData({
                    message: []
                });
                this.sinon.stub(mMessages, 'updateThreadMeta');

                mMessages.remove(this.mMessage);

                expect(mMessages.updateThreadMeta).to.have.callCount(0);
            });

            it('должен вызвать #updateThreadMeta, если это модель треда, она валидна и есть мета про тред', function() {
                var mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({ message: [] });
                mMessages.insert(this.mMessage);

                this.sinon.stub(mMessages, 'updateThreadMeta');

                mMessages.remove(this.mMessage);

                expect(mMessages.updateThreadMeta)
                    .to.have.callCount(1)
                    .and.to.be.calledWith(this.mThread);
            });

            it('должен вызваться #_updateDetailsForSearch', function() {
                var mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({ message: [] });
                mMessages.insert(this.mMessage);

                this.sinon.spy(mMessages, '_updateDetailsForSearch');

                mMessages.remove(this.mMessage);

                expect(mMessages._updateDetailsForSearch)
                    .to.have.callCount(1);
            });
        });

        describe('"ns-model-insert" ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '333', search: 'search', request: 'hi' }).setData({
                    details: {
                        topResultsMids: [ '1', '2' ]
                    },
                    message: [
                        { mid: '1' },
                        { mid: '2' },
                        { mid: '3' },
                        { mid: '4' }
                    ]
                });

                this.mMessageNew = ns.Model.get('message', { ids: '5' }).setData({
                    mid: '5'
                });
            });

            // TODO пока отключил этот тест: при создании списка писем сообщения
            // должны быть отсортированы вручную.
            // TODO проверить после https://github.com/yandex-ui/noscript/issues/645
            // it('после создания - сообщения уже отсортированы по принципу Top Results вначале', function() {
            //     expect(this.mMessages.models.map(function(mMessage) { return mMessage.get('.mid'); }))
            //         .to.be.eql([ '1', '2', '3' ]);
            // });

            describe('после вставки письма продолжают сохранять порядок "вначале Top Results" ->', function() {
                sit('вставка на позицию 0 - до Top Results', 0, [ '1', '2', '5', '3', '4' ]);
                sit('вставка на позицию 1 - между Top Results', 1, [ '1', '2', '5', '3', '4' ]);
                sit('вставка на позицию 2 - после Top Results', 2, [ '1', '2', '5', '3', '4' ]);
                sit('вставка на позицию 3 - между остальными результатами', 3, [ '1', '2', '3', '5', '4' ]);

                function sit(testTitle, insertIndex, expectedMids) {
                    it(testTitle, function() {
                        this.mMessages.insert(this.mMessageNew, insertIndex);
                        expect(this.mMessages.models.map(function(mMessage) {
                            return mMessage.get('.mid');
                        }))
                            .to.be.eql(expectedMids);
                    });
                }
            });
        });
    });

    describe('getEmails', function() {
        it('should return wellformed object', function() {
            expect(ns.Model.get('messages', {}).getEmails()).to.eql({
                '2190000000624510036': {
                    'social': false,
                    'type': [],
                    'from': { ref: 'ref', email: 'foginat6@yandex.ru' },
                    'to': { ref: 'ref', email: 'foginat4@ya.ru' },
                    'reply-to': { ref: 'ref', email: 'foginat6@yandex.ru' }
                },
                '2190000000624510037': {
                    'social': false,
                    'type': [],
                    'from': { ref: 'ref', email: 'foginat5@yandex.ru' },
                    'to': { ref: 'ref', email: 'foginat4@ya.ru' },
                    'reply-to': { ref: 'ref', email: 'foginat5@yandex.ru' }
                }
            });
        });
    });

    describe('#hasMessageWithLabel', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages').setData({
                message: [
                    { mid: '1', lid: [ '1' ] },
                    { mid: '2', lid: [ '2' ] },
                    { mid: '3', lid: [ '3' ] }
                ]
            });
        });

        it('должен вернуть null, если нет писем', function() {
            this.mMessages.clear();
            expect(this.mMessages.hasMessageWithLabel('1', 3)).to.be.equal(null);
        });

        it('должен вернуть false, если есть не все письма', function() {
            expect(this.mMessages.hasMessageWithLabel('1', 5)).to.be.equal(null);
        });

        it('должен вернуть false, если есть все письма и на них нет этой метки', function() {
            expect(this.mMessages.hasMessageWithLabel('4', 3)).to.be.equal(false);
        });

        it('должен вернуть true, если есть все письма и в них есть хотя бы одно письмо с меткой', function() {
            expect(this.mMessages.hasMessageWithLabel('1', 3)).to.be.equal(true);
        });
    });

    describe('#insertMessage', function() {
        function messagesToIds(mMessages) {
            return mMessages.models.map(function(mMessage) {
                return mMessage.params.ids;
            });
        }

        beforeEach(function() {
            this.newMessage = ns.Model.get('message', { ids: 'newMsgKey' });
        });

        it('должен вставить новое письмо в список', function() {
            var params = {
                current_folder: 'insertMessage1'
            };
            /** @type Daria.mMessages */
            var mMessages = ns.Model.get('messages', params);
            var result = mMessages.insertMessage(this.newMessage);
            var ids = messagesToIds(mMessages);

            expect(result).to.be.equal(true);
            expect(ids).to.eql([ 'newMsgKey', 'insertMessage1-1', 'insertMessage1-2', 'insertMessage1-3' ]);
        });

        it('должен вставить новое письмо в список и удалить переданное', function() {
            var params = {
                current_folder: 'insertMessage1'
            };

            /** @type Daria.mMessages */
            var mMessages = ns.Model.get('messages', params);
            var result = mMessages.insertMessage(this.newMessage, ns.Model.get('message', { ids: 'insertMessage1-2' }));
            var ids = messagesToIds(mMessages);

            expect(result).to.be.equal(true);
            expect(ids).to.eql([ 'newMsgKey', 'insertMessage1-1', 'insertMessage1-3' ]);
        });

        it('должен вставить новое письмо в список после запиненных', function() {
            var params = {
                current_folder: 'insertMessage1'
            };

            /** @type Daria.mMessages */
            var mMessages = ns.Model.get('messages', params);

            mMessages.setData({
                message: [
                    {
                        mid: '1111'
                    },
                    {
                        mid: '2222'
                    }
                ]
            });

            this.sinon.stub(ns.Model.get('message', { ids: '1111' }), 'isPinned').returns(true);
            this.sinon.stub(ns.Model.get('message', { ids: '2222' }), 'isPinned').returns(true);

            mMessages.insertMessage(this.newMessage);

            expect(mMessages.models.indexOf(this.newMessage)).to.be.equal(2);
        });

        it('должен вставить новый тред в список после запиненных', function() {
            var params = {
                current_folder: 'insertMessage1'
            };

            /** @type Daria.mMessages */
            var mMessages = ns.Model.get('messages', params);

            mMessages.setData({
                message: [
                    {
                        mid: '1111'
                    },
                    {
                        mid: '2222'
                    }
                ]
            });

            this.sinon.stub(ns.Model.get('message', { ids: '1111' }), 'isPinned').returns(true);
            this.sinon.stub(ns.Model.get('message', { ids: '2222' }), 'isPinned').returns(true);
            this.sinon.stub(this.newMessage, 'isThread').returns(true);
            this.sinon.stub(this.newMessage, 'isPinned').returns(false);

            mMessages.insertMessage(this.newMessage);

            expect(mMessages.models.indexOf(this.newMessage)).to.be.equal(2);
        });

        it('должен вставить новый запиненный тред в список перед запиненныыми', function() {
            var params = {
                current_folder: 'insertMessage1'
            };

            /** @type Daria.mMessages */
            var mMessages = ns.Model.get('messages', params);

            mMessages.setData({
                message: [
                    {
                        mid: '1111'
                    },
                    {
                        mid: '2222'
                    }
                ]
            });

            this.sinon.stub(ns.Model.get('message', { ids: '1111' }), 'isPinned').returns(true);
            this.sinon.stub(ns.Model.get('message', { ids: '2222' }), 'isPinned').returns(true);
            this.sinon.stub(this.newMessage, 'isThread').returns(true);
            this.sinon.stub(this.newMessage, 'isPinned').returns(true);

            mMessages.insertMessage(this.newMessage);

            expect(mMessages.models.indexOf(this.newMessage)).to.be.equal(0);
        });
    });

    describe('#insertMessageBySort', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '100' }).setData({
                count: 1,
                fid: '1',
                field: [
                    { type: 'from', name: '00' }
                ],
                mid: '100',
                date: {
                    timestamp: new Date(2018, 9, 14, 12, 54, 16).getTime() // Sun Oct 14 2018 12:54:16
                },
                length: 110
            });
        });

        it('не должен вставлять письмо, если список невалиден', function() {
            var mMessages = ns.Model.get('messages', { current_folder: '1' }).setData({
                message: [
                    { mid: '11', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 54, 16).getTime() } },
                    { mid: '12', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 44, 16).getTime() } }
                ]
            });
            mMessages.invalidate();
            mMessages.insertMessageBySort(this.mMessage);

            expect(mMessages.models).to.have.length(2);
        });

        it('должен нормально вставить письмо в пустой список', function() {
            var mMessages = ns.Model.get('messages', { current_folder: '1' }).setData({
                message: []
            });
            mMessages.insertMessageBySort(this.mMessage);

            expect(mMessages.models).to.have.length(1);
        });

        describe('Вставка элемента коллекции в новый список', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1' }).setData({
                    message: [
                        { mid: '12', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 54, 6).getTime() } }, // Sun Oct 14 2018 12:54:06
                        { mid: '11', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 56).getTime() } }, // Sun Oct 14 2018 12:53:56
                        { mid: '14', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 46).getTime() } }, // Sun Oct 14 2018 12:53:46
                        { mid: '13', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 36).getTime() } } // Sun Oct 14 2018 12:53:36
                    ]
                });
            });

            it('если вставка происходит в текущий чанк коллекции (в середину), вставляем', function() {
                this.sinon.stub(this.mMessages, 'canLoadMore').returns(true);
                this.sinon.spy(this.mMessages, 'insert');

                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 54).getTime()); // Sun Oct 14 2018 12:53:55
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.insert).to.be.calledWith(this.mMessage, 2);
                expect(this.mMessages.models, 'length').to.have.length(5);
            });

            it('если вставка происходит в текущий чанк коллекции (в начало), вставляем', function() {
                this.sinon.stub(this.mMessages, 'canLoadMore').returns(true);
                this.sinon.spy(this.mMessages, 'insert');

                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 55, 3).getTime()); // Sun Oct 14 2018 12:53:55
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.insert).to.be.calledWith(this.mMessage, 0);
                expect(this.mMessages.models, 'length').to.have.length(5);
            });
            it('если вставка происходит в текущий чанк коллекции (в конец), то не вставляем (это сделает бек)',
                function() {
                    this.sinon.stub(this.mMessages, 'canLoadMore').returns(true);
                    this.sinon.spy(this.mMessages, 'insert');

                    this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 26).getTime()); // Sun Oct 14 2018 12:53:55
                    this.mMessages.insertMessageBySort(this.mMessage);

                    expect(this.mMessages.insert).to.have.callCount(0);
                    expect(this.mMessages.models, 'length').to.have.length(4);
                });
            it('если вставка происходит в конец коллекции, но догружать нечего, то вставляем', function() {
                this.sinon.stub(this.mMessages, 'canLoadMore').returns(false);
                this.sinon.spy(this.mMessages, 'insert');

                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 26).getTime()); // Sun Oct 14 2018 12:53:55
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.insert).to.be.calledWith(this.mMessage, 4);
                expect(this.mMessages.models, 'length').to.have.length(5);
            });
        });

        describe('Сортировка по дате DESC (date) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1' }).setData({
                    message: [
                        { mid: '12', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 54, 6).getTime() } }, // Sun Oct 14 2018 12:54:06
                        { mid: '11', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 56).getTime() } }, // Sun Oct 14 2018 12:53:56
                        { mid: '14', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 46).getTime() } }, // Sun Oct 14 2018 12:53:46
                        { mid: '13', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 36).getTime() } } // Sun Oct 14 2018 12:53:36
                    ]
                });
                this.sinon.stub(this.mMessages, 'canLoadMore').returns(false);
            });

            it('вставка в начало', function() {
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 55).getTime()); // Sun Oct 14 2018 12:53:55
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 26).getTime());
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });
        });

        describe('Сортировка по дате ASC (date1) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'date1' }).setData({
                    message: [
                        { mid: '13', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 36).getTime() } }, // Sun Oct 14 2018 12:53:36
                        { mid: '14', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 46).getTime() } }, // Sun Oct 14 2018 12:53:46
                        { mid: '11', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 53, 56).getTime() } }, // Sun Oct 14 2018 12:53:56
                        { mid: '12', fid: '1', count: 1, date: { timestamp: new Date(2018, 9, 14, 12, 54, 6).getTime() } } // Sun Oct 14 2018 12:54:06
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 26).getTime());
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 55).getTime());
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.set('.date.timestamp', new Date(2018, 9, 14, 12, 53, 26).getTime());
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });
        });

        describe('Сортировка по отправителю ASC (from) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'from' }).setData({
                    message: [
                        { mid: '11', fid: '1', count: 1, field: [
                            { type: 'from', name: 'aa' }
                        ] },
                        { mid: '12', fid: '1', count: 1, field: [
                            { type: 'from', name: 'bb' }
                        ] },
                        { mid: '13', fid: '1', count: 1, field: [
                            { type: 'from', name: 'cc' }
                        ] },
                        { mid: '14', fid: '1', count: 1, field: [
                            { type: 'from', name: 'dd' }
                        ] }
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.getData().field[0].name = 'a1';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.getData().field[0].name = 'bc';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.getData().field[0].name = 'ee';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });

            it('сортировка не должна зависеть от регистра', function() {
                this.mMessage.getData().field[0].name = 'BC';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });
        });

        describe('Сортировка по отправителю DESC (from1) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'from1' }).setData({
                    message: [
                        { mid: '11', fid: '1', count: 1, field: [
                            { type: 'from', name: 'dd' }
                        ] },
                        { mid: '12', fid: '1', count: 1, field: [
                            { type: 'from', name: 'cc' }
                        ] },
                        { mid: '13', fid: '1', count: 1, field: [
                            { type: 'from', name: 'bb' }
                        ] },
                        { mid: '14', fid: '1', count: 1, field: [
                            { type: 'from', name: 'aa' }
                        ] }
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.getData().field[0].name = 'ee';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.getData().field[0].name = 'bc';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.getData().field[0].name = 'a1';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });

            it('сортировка не должна зависеть от регистра', function() {
                this.mMessage.getData().field[0].name = 'BC';
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });
        });

        describe('Сортировка по размеру DESC (size) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'size' }).setData({
                    message: [
                        { mid: '11', fid: '1', count: 1, date: { ts: 50 }, length: 100 },
                        { mid: '12', fid: '1', count: 1, date: { ts: 40 }, length: 90 },
                        { mid: '13', fid: '1', count: 1, date: { ts: 30 }, length: 80 },
                        { mid: '14', fid: '1', count: 1, date: { ts: 20 }, length: 70 }
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.set('.length', 101);
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.set('.length', 71);
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[3], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.set('.length', 70);
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });
        });

        describe('Сортировка по размеру ASC (size1) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'size1' }).setData({
                    message: [
                        { mid: '11', fid: '1', count: 1, length: 70 },
                        { mid: '12', fid: '1', count: 1, length: 80 },
                        { mid: '13', fid: '1', count: 1, length: 90 },
                        { mid: '14', fid: '1', count: 1, length: 100 }
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.set('.length', 69);
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.set('.length', 91);
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[3], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.set('.length', 100);
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });
        });

        describe('Сортировка по теме ASC (subject) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'subject' }).setData({
                    message: [
                        { mid: '11', fid: '1', count: 1, subject: 'aa' },
                        { mid: '12', fid: '1', count: 1, subject: 'bb' },
                        { mid: '13', fid: '1', count: 1, subject: 'cc' },
                        { mid: '14', fid: '1', count: 1, subject: 'dd' }
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.set('.subject', 'a1');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.set('.subject', 'bc');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.set('.subject', 'ee');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });

            it('сортировка не должна зависеть от регистра', function() {
                this.mMessage.set('.subject', 'BC');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });
        });

        describe('Сортировка по теме DESC (subject1) ->', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'subject1' }).setData({
                    message: [
                        { mid: '11', fid: '1', count: 1, subject: 'dd' },
                        { mid: '12', fid: '1', count: 1, subject: 'cc' },
                        { mid: '13', fid: '1', count: 1, subject: 'bb' },
                        { mid: '14', fid: '1', count: 1, subject: 'aa' }
                    ]
                });
            });

            it('вставка в начало', function() {
                this.mMessage.set('.subject', 'ee');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[0], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в середину', function() {
                this.mMessage.set('.subject', 'bc');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });

            it('вставка в конец', function() {
                this.mMessage.set('.subject', 'a1');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
            });

            it('сортировка не должна зависеть от регистра', function() {
                this.mMessage.set('.subject', 'BC');
                this.mMessages.insertMessageBySort(this.mMessage);

                expect(this.mMessages.models, 'length').to.have.length(5);
                expect(this.mMessages.models[2], 'model').to.be.equal(this.mMessage);
            });
        });
    });

    describe('#isEmptyList', function() {
        beforeEach(function() {
            this.handler = ns.Model.get('messages');
        });

        it('Должен возвращать false при присутствии писем на странице', function() {
            this.handler.setData(mock.messages[0].data);
            expect(this.handler.isEmptyList()).to.be.equal(false);
        });

        it('Должен возвращать true при отсутствии писем на странице', function() {
            this.handler.setData({
                message: []
            });
            expect(this.handler.isEmptyList()).to.be.equal(true);
        });
    });

    describe('#label / unlabel', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages').setData({
                message: [
                    { mid: '1', tid: 't1', lid: [ '1' ] },
                    { mid: '2', tid: 't2', lid: [ '1' ] },
                    { mid: '3', tid: 't3', lid: [ '2' ] },
                    { mid: '4', tid: 't4', lid: [ '2' ] }
                ]
            });
        });

        describe('#label', function() {
            it('должен вернуть полную информацию об операции', function() {
                var opinfo = this.mMessages.label({ lid: '1' });

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds(),
                    count: opinfo.getModelsCount()
                }).to.be.eql({
                    adjust: {
                        1: 2
                    },
                    affected: {
                        ids: [ '3', '4' ],
                        tids: []
                    },
                    count: 4
                });
            });

            it('должен пометить письма меткой', function() {
                this.mMessages.label({ lid: '1' });
                expect(ns.Model.get('message', { ids: '3' }).hasLabel('1')).to.be.equal(true);
                expect(ns.Model.get('message', { ids: '4' }).hasLabel('1')).to.be.equal(true);
            });

            it('должен вернуть информацию об операции без смещения, если в модели нет данных', function() {
                var opinfo = ns.Model.get('messages', { current_folder: 'no' }).label({ lid: '1' });

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: []
                    }
                });
            });
        });

        describe('#unlabel', function() {
            it('должен вернуть полную информацию об операции', function() {
                var opinfo = this.mMessages.unlabel({ lid: '1' });

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds(),
                    count: opinfo.getModelsCount()
                }).to.be.eql({
                    adjust: {
                        1: -2
                    },
                    affected: {
                        ids: [ '1', '2' ],
                        tids: []
                    },
                    count: 4
                });
            });

            it('должен пометить письма меткой', function() {
                this.mMessages.unlabel({ lid: '1' });
                expect(ns.Model.get('message', { ids: '1' }).hasLabel('1')).to.be.equal(false);
                expect(ns.Model.get('message', { ids: '2' }).hasLabel('1')).to.be.equal(false);
            });

            it('должен вернуть информацию об операции без смещения, если в модели нет данных', function() {
                var opinfo = ns.Model.get('messages', { current_folder: 'no' }).unlabel({ lid: '1' });

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: []
                    }
                });
            });
        });
    });

    describe('#mark / #unmark', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages').setData({
                message: [
                    // непрочитанное письмо в папке1
                    { count: 1, fid: '1', mid: '1', tid: 't1', new: 1 },
                    // непрочитанное письмо в папке1
                    { count: 1, fid: '1', mid: '11', tid: 't11', new: 1 },
                    // прочитанное письмо в папке1
                    { count: 1, fid: '1', mid: '2', tid: 't2', new: 0 },
                    // непрочитанное письмо в папке2
                    { count: 1, fid: '2', mid: '3', tid: 't3', new: 1 },
                    // прочитанное письмо в папке2
                    { count: 1, fid: '2', mid: '4', tid: 't4', new: 0 },
                    // прочитанное письмо в папке2
                    { count: 1, fid: '2', mid: '5', tid: 't5', new: 0 }
                ]
            });
        });

        describe('#mark', function() {
            it('должен вернуть полную информацию об операции', function() {
                var opinfo = this.mMessages.mark();

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: -2,
                        2: -1
                    },
                    affected: {
                        ids: [ '1', '11', '3' ],
                        tids: []
                    }
                });
            });

            it('должен пометить письма прочитанными', function() {
                this.mMessages.mark();
                expect(ns.Model.get('message', { ids: '1' }).isNew()).to.be.equal(false);
                expect(ns.Model.get('message', { ids: '11' }).isNew()).to.be.equal(false);
                expect(ns.Model.get('message', { ids: '3' }).isNew()).to.be.equal(false);
            });

            it('должен вернуть информацию об операции без смещения, если в модели нет данных', function() {
                var opinfo = ns.Model.get('messages', { current_folder: 'no' }).mark();

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: []
                    }
                });
            });
        });

        describe('#unmark', function() {
            it('должен вернуть полную информацию об операции', function() {
                var opinfo = this.mMessages.unmark();

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: {
                        1: 1,
                        2: 2
                    },
                    affected: {
                        ids: [ '2', '4', '5' ],
                        tids: []
                    }
                });
            });

            it('должен пометить письма прочитанными', function() {
                this.mMessages.unmark();
                expect(ns.Model.get('message', { ids: '2' }).isNew()).to.be.equal(true);
                expect(ns.Model.get('message', { ids: '4' }).isNew()).to.be.equal(true);
                expect(ns.Model.get('message', { ids: '5' }).isNew()).to.be.equal(true);
            });

            it('должен вернуть информацию об операции без смещения, если в модели нет данных', function() {
                var opinfo = ns.Model.get('messages', { current_folder: 'no' }).unmark();

                expect({
                    adjust: opinfo.getAdjust(),
                    affected: opinfo.getIds()
                }).to.be.eql({
                    adjust: null,
                    affected: {
                        ids: [],
                        tids: []
                    }
                });
            });
        });
    });

    describe('#getPinned', function() {
        beforeEach(function() {
            this.mMessage_1_pinned = ns.Model.get('message', { ids: '1' }).setData({ ids: '1' });
            this.mMessage_2_pinned = ns.Model.get('message', { ids: '2' }).setData({ ids: '2' });
            this.mMessage_3_unpinned = ns.Model.get('message', { ids: '3' }).setData({ ids: '3' });

            this.mMessages = ns.Model.get('messages').setData({
                message: []
            });

            this.mMessages.insert([
                this.mMessage_1_pinned,
                this.mMessage_2_pinned,
                this.mMessage_3_unpinned
            ]);

            this.sinon.stub(this.mMessage_1_pinned, 'isPinned').returns(true);
            this.sinon.stub(this.mMessage_2_pinned, 'isPinned').returns(true);
            this.sinon.stub(this.mMessage_3_unpinned, 'isPinned').returns(false);
        });

        it('Должен вернуть все запиненные письма в коллекции', function() {
            var pinnedMessages = this.mMessages.getPinned();
            expect(pinnedMessages.map(function(mMessage) {
                return mMessage.get('.ids');
            })).to.be.eql([ '1', '2' ]);
        });
    });

    describe('#recalculateIndexesAfterPin', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages', { current_folder: '1', sort_type: 'date' }).setData({
                message: []
            });

            this.mMessage_1 = ns.Model.get('message', { ids: '1' }).setData({
                ids: '1',
                fid: '1',
                date: { timestamp: new Date(2018, 9, 14, 12, 53, 46).getTime() }
            });
            this.mMessage_2 = ns.Model.get('message', { ids: '2' }).setData({
                ids: '2',
                fid: '1',
                date: { timestamp: new Date(2018, 9, 14, 12, 53, 26).getTime() }
            });
            this.mMessage_3 = ns.Model.get('message', { ids: '3' }).setData({
                ids: '3',
                fid: '2',
                date: { timestamp: new Date(2018, 9, 14, 12, 53, 36).getTime() }
            });
            this.mMessage_4 = ns.Model.get('message', { ids: '4' }).setData({
                ids: '4',
                fid: '1',
                date: { timestamp: new Date(2018, 9, 14, 12, 53, 55).getTime() }
            });
            this.mMessage_5 = ns.Model.get('message', { ids: '5' }).setData({
                ids: '5',
                fid: '1',
                date: { timestamp: new Date(2018, 9, 14, 12, 53, 56).getTime() }
            });
            this.mMessage_6 = ns.Model.get('message', { ids: '6' }).setData({
                ids: '6',
                fid: '1',
                date: { timestamp: new Date(2018, 9, 14, 12, 54, 6).getTime() }
            });

            this.mMessages.insert([
                this.mMessage_1,
                this.mMessage_2
            ]);
        });

        describe('pin', function() {
            it('Должен вставить новое запиненное письмо согласно сортировке по дате', function() {
                this.mMessages.recalculateIndexesAfterPin([ this.mMessage_3 ], true);
                expect(this.mMessages.models.indexOf(this.mMessage_3)).to.be.equal(1);
            });

            it('Если письмо уже есть в списке, то не должен его удалить', function() {
                this.mMessages.recalculateIndexesAfterPin([
                    this.mMessage_1,
                    this.mMessage_2,
                    this.mMessage_3
                ], true);
                expect(this.mMessages.models.indexOf(this.mMessage_1)).to.be.above(-1);
            });
        });

        describe('unpin', function() {
            it('Распиненное сообщение должно вставиться согласно сортировке по дате', function() {
                this.mMessages.insert([
                    this.mMessage_1,
                    this.mMessage_2,
                    this.mMessage_3,
                    this.mMessage_4,
                    this.mMessage_5,
                    this.mMessage_6
                ]);

                this.sinon.stub(this.mMessage_1, 'isPinned').returns(true);
                this.sinon.stub(this.mMessage_2, 'isPinned').returns(true);

                this.mMessages.recalculateIndexesAfterPin([
                    this.mMessage_1
                ], false);
                expect(this.mMessages.models.indexOf(this.mMessage_1)).to.be.equal(0);
            });

            it('Должен удалить из списка письмо, если оно не из этой папки', function() {
                this.mMessages.insert([
                    this.mMessage_1,
                    this.mMessage_2,
                    this.mMessage_3
                ]);
                this.mMessages.recalculateIndexesAfterPin([
                    this.mMessage_3
                ], false);
                expect(this.mMessages.models.indexOf(this.mMessage_3)).to.be.equal(-1);
            });
        });
    });

    describe('#updateThreadMeta', function() {
        beforeEach(function() {
            this.mMessage1 = ns.Model.get('message', { ids: '1' }).setData({ mid: '1', tid: 't1' });
            this.sinon.stub(this.mMessage1, 'isExternalLike').returns(true);

            this.mMessage2 = ns.Model.get('message', { ids: '2' }).setData({ mid: '2', tid: 't1' });
            this.sinon.stub(this.mMessage2, 'isExternalLike').returns(true);

            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                last_mid: '1'
            });
            this.sinon.stub(this.mThread, 'updateThreadContextInfo');

            this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                message: []
            });
            // делаем нужный нам порядок
            this.mMessages.insert([
                this.mMessage2,
                this.mMessage1
            ]);
        });

        it('должен обновить тред первым письмо из внешней папки (кейс 1)', function() {
            this.mMessages.updateThreadMeta(this.mThread);

            expect(this.mThread.updateThreadContextInfo)
                .to.have.callCount(1)
                .and.to.be.calledWith(this.mMessage2);
        });

        it('должен обновить тред первым письмо из внешней папки (кейс 2)', function() {
            this.mMessage2.isExternalLike.returns(false);
            this.mMessages.updateThreadMeta(this.mThread);

            expect(this.mThread.updateThreadContextInfo)
                .to.have.callCount(1)
                .and.to.be.calledWith(this.mMessage1);
        });

        it('должен обновить тред первым письмо, если нет писем из внешних папок', function() {
            this.mMessage1.isExternalLike.returns(false);
            this.mMessage2.isExternalLike.returns(false);

            this.mMessages.updateThreadMeta(this.mThread);

            expect(this.mThread.updateThreadContextInfo)
                .to.have.callCount(1)
                .and.to.be.calledWith(this.mMessage2);
        });
    });

    describe('#uncheckThreadMessages', function() {
        beforeEach(function createSomeMessages() {
            this.mMessagesChecked = ns.Model.get('messages-checked');

            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                mid: '1',
                new: 1,
                fid: '1',
                tid: 't1'
            });
            this.mThread = ns.Model.get('message', { ids: 't1' }).setData({
                count: 2,
                mid: 't1',
                new: 0,
                tid: 't1'
            });

            this.mMessages = ns.Model.get('messages', { thread_id: 't1' }).setData({
                message: [
                    { count: '1', fid: 1, lid: [], mid: '11', new: 0 },
                    { count: '1', fid: 1, lid: [], mid: '12', new: 0 }
                ]
            });

            this.mMessageChecked = ns.Model.get('message', { ids: '11' });
            this.mMessageUnchecked = ns.Model.get('message', { ids: '12' });

            this.mMessagesChecked.check(this.mMessage, true);
            this.mMessagesChecked.check(this.mThread, false);
            this.mMessagesChecked.check(this.mMessageChecked, true);
            this.mMessagesChecked.check(this.mMessageUnchecked, false);
        });

        it('ничего не делаем если это не тред', function() {
            this.sinon.spy(this.mMessagesChecked, 'check');

            this.mMessageChecked.uncheckThreadMessages();
            expect(this.mMessagesChecked.check).to.have.callCount(0);
        });

        it('снимает выделение со всех выделенных писем внутри треда', function() {
            expect(this.mMessagesChecked.isChecked(this.mMessageChecked)).to.be.true;

            this.mThread.uncheckThreadMessages();
            expect(this.mMessagesChecked.isChecked(this.mMessageChecked)).to.be.false;
        });

        it('меняем состояние только у выделенных писем', function() {
            this.sinon.spy(this.mMessagesChecked, 'check');

            this.mThread.uncheckThreadMessages();
            expect(this.mMessagesChecked.check).to.have.callCount(1);
        });

        it('не снимает выделение с писем вне треда', function() {
            this.mThread.uncheckThreadMessages();
            expect(this.mMessagesChecked.isChecked(this.mMessage)).to.be.true;
        });

        it('снимаем выделение с писем в указанной папке', function() {
            this.sinon.spy(this.mMessagesChecked, 'check');
            ns.page.current.params.current_folder = '2';

            this.mThread.uncheckThreadMessages(this.mMessagesChecked);
            expect(this.mMessagesChecked.check).to.have.callCount(1);
        });
    });

    describe('#getNearestMessageByMid', function() {
        beforeEach(function() {
            this.mMessage1 = ns.Model.get('message', { ids: '112233' });
            this.mMessage2 = ns.Model.get('message', { ids: '112244' });
            this.mMessage3 = ns.Model.get('message', { ids: '112255' });

            this.mMessages = ns.Model.get('messages');
            this.mMessages.setData({
                message: []
            });

            this.mMessages.insert([
                this.mMessage1,
                this.mMessage2,
                this.mMessage3
            ]);
        });

        it('должен вернуть предыдущее сообщение для вызова с position == `prev`, если оно есть', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('112244', 'prev');
            expect(mMessage).to.be.equal(this.mMessage1);
        });

        it('должен вернуть следующее сообщение для вызова с position == `next`, если оно есть', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('112244', 'next');
            expect(mMessage).to.be.equal(this.mMessage3);
        });

        it('должен вернуть false для вызова с position == `prev`, если предыдущего сообщения нет', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('112233', 'prev');
            expect(mMessage).to.be.equal(false);
        });

        it('должен вернуть false для вызова с position == `next`, если следующего сообщения нет', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('112255', 'next');
            expect(mMessage).to.be.equal(false);
        });

        it('должен вернуть false, если position не указан', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('112255');
            expect(mMessage).to.be.equal(false);
        });

        it('должен вернуть false, если position не `prev` и не `next`', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('112255', 'pre');
            expect(mMessage).to.be.equal(false);
        });

        it('должен вернуть false, если mid не указан', function() {
            var mMessage = this.mMessages.getNearestMessageByMid();
            expect(mMessage).to.be.equal(false);
        });

        it('должен вернуть false, если указан mid письма, отсутствующего в треде', function() {
            var mMessage = this.mMessages.getNearestMessageByMid('123', 'prev');
            expect(mMessage).to.be.equal(false);
        });
    });

    describe('#getMessageByMid', function() {
        beforeEach(function() {
            this.mMessage1 = ns.Model.get('message', { ids: '112233' });
            this.mMessage2 = ns.Model.get('message', { ids: '112244' });

            this.mMessages = ns.Model.get('messages');

            this.mMessages.insert([
                this.mMessage1,
                this.mMessage2
            ]);
        });

        it('Должен возвращать модель письма, если ее удалось найти по mid', function() {
            expect(this.mMessages.getMessageByMid('112233')).to.be.equal(this.mMessage1);
        });

        it('Должен возвращать null, если не удалось найти модель письма', function() {
            expect(this.mMessages.getMessageByMid('112277')).to.be.equal(null);
        });
    });

    describe('#refresh', function() {
        beforeEach(function() {
            var mSettings = ns.Model.get('settings');
            this.sinon.stub(mSettings, 'getSetting').returns(30);

            var params = { current_folder: 'refresh1' };

            this.mMessages = ns.Model.get('messages', params).setData({
                message: []
            });

            this.mMessages.insert([
                ns.Model.get('message', { ids: 'mrefresh1' }).setData({ ids: 'mrefresh1', fid: 'refresh1' }),
                ns.Model.get('message', { ids: 'mrefresh2' }).setData({ ids: 'mrefresh2', fid: 'refresh1' })
            ]);

            this.portionParams = _.assign({}, this.mMessages.params, {
                first: 0,
                count: 30
            });
        });

        it('Должен создать временнрую модель с добавленными параметрами first и count', function() {
            this.sinon.spy(ns.Model, 'get');
            this.mMessages.refresh();
            expect(ns.Model.get).to.be.calledWith(this.mMessages.id, this.portionParams);
        });

        it('Должен загрузить временную модель', function() {
            var mMessages = ns.Model.get(this.mMessages.id, this.portionParams);
            this.mMessages.refresh();
            expect(ns.request.models).to.be.calledWith([ mMessages ], { forced: true });
        });

        describe('Обновление данных поисковой выдачи ->', function() {
            beforeEach(function(runTests) {
                var mMessages = this.mMessages = ns.Model.get('messages', { fid: 333 }).setData({
                    details: {
                        'top-relevant': 2,
                        'total-found': 7,
                        'topResultsMids': [ 'm1', 'm2' ]
                    },
                    message: [
                        { mid: 'm1', date: { chunks: {} }, fid: '1' },
                        { mid: 'm2', date: { chunks: {} }, fid: '1' },
                        { mid: 'm3', date: { chunks: {} }, fid: '1' }
                    ]
                });

                ns.request.models.restore();

                this.sinon.stub(ns.request, 'models').callsFake(function(models) {
                    models[0].setData({
                        details: {
                            'top-relevant': 1,
                            'total-found': 7,
                            'topResultsMids': [ 'm2' ]
                        },
                        message: [
                            { mid: 'm2', date: { chunks: {} }, fid: '1' },
                            { mid: 'm1', date: { chunks: {} }, fid: '1' },
                            { mid: 'm3', date: { chunks: {} }, fid: '1' }
                        ]
                    });
                    return Vow.resolve(models);
                });

                mMessages.refresh().then(runTests);
            });

            it('обновляется details.top-relevant', function() {
                expect(this.mMessages.get('.details.top-relevant')).to.be.equal(1);
            });

            it('обновляется details.topResultsMids', function() {
                expect(this.mMessages.get('.details.topResultsMids')).to.be.eql([ 'm2' ]);
            });

            it('обновляется последовательность сообщений', function() {
                // Тут сейчас мы ничего специально не делаем - как вернулись данные, так мы их и сохранили.
                expect(this.mMessages.models.map(function(mMessage) {
                    return mMessage.get('.mid');
                }))
                    .to.be.eql([ 'm2', 'm1', 'm3' ]);
            });
        });
    });

    describe('#isSearch', function() {
        // mMessages.params -> isSearch result
        [
            [ { current_folder: '1' }, false ],
            [ { search: 'search' }, true ],
            [ { request: 'response' }, true ],
            [ { current_label: '2', search: 'search' }, true ],
            [ { current_folder: '2', search: 'search' }, true ],
            [ { current_label: '3' }, false ]
        ].forEach(function(test) {
            var params = test[0];
            var result = test[1];
            var paramsString = JSON.stringify(params);

            it('с параметрами ' + paramsString + ' должен вернуть ' + result, function() {
                var mMessages = ns.Model.get('messages', params);
                expect(mMessages.isSearch()).to.be.equal(result);
            });
        });
    });

    describe('#isUnreadMessagesCollection', function() {
        // mMessages.params -> isUnreadMessagesCollection result
        [
            [ { current_folder: '1' }, false ],
            [ { current_folder: '1', extra_cond: 'only_new' }, true ],
            [ { current_folder: '1', extra_cond: 'only_atta' }, false ],
            [ { current_folder: '1', unread: 'yes' }, true ],
            [ { current_folder: '1', unread: 'no' }, false ]
        ].forEach(function(test) {
            var params = test[0];
            var result = test[1];
            var paramsString = JSON.stringify(params);

            it('с параметрами ' + paramsString + ' должен вернуть ' + result, function() {
                var mMessages = ns.Model.get('messages', params);
                expect(mMessages.isUnreadMessagesCollection()).to.be.equal(result);
            });
        });
    });

    describe('#hasOnlyPinnedMessages', function() {
        beforeEach(function() {
            this.mMessages = ns.Model.get('messages', { current_folder: '1' });

            this.hasUnpinnedStub = this.sinon.stub(this.mMessages, 'hasUnpinned');
            this.canLoadMoreStub = this.sinon.stub(this.mMessages, 'canLoadMore');
        });

        it('Должен вернуть true если все текущие письма запинены, и дальше писем нет', function() {
            this.hasUnpinnedStub.returns(false);
            this.canLoadMoreStub.returns(false);

            expect(this.mMessages.hasOnlyPinnedMessages()).to.be.equal(true);
        });

        it('Должен вернуть false, если все текущие письма запинены, но дальше есть письма', function() {
            this.hasUnpinnedStub.returns(false);
            this.canLoadMoreStub.returns(true);

            expect(this.mMessages.hasOnlyPinnedMessages()).to.be.equal(false);
        });
    });

    describe('#_splitModels', function() {
        it('метод переопределён', function() {
            //eslint-disable-next-line no-prototype-builtins
            expect(ns.Model.infoLite('messages').methods.hasOwnProperty('_splitModels')).to.be.eql(true);
        });

        describe('в нём создаются модели Daria.mMessagePresentation', function() {
            beforeEach(function() {
                this.mMessages = ns.Model.get('messages', { current_folder: '3' });
            });

            it('модели создаются', function() {
                this.mMessages.setData({
                    message: [
                        { mid: '1', lid: [ '1' ] },
                        { mid: '2', lid: [ '2' ] },
                        { mid: '3', lid: [ '3' ] }
                    ]
                });

                expect(ns.Model.get('message-presentation', { ids: '1', fid: '3' }).isValid()).to.be.eql(true);
                expect(ns.Model.get('message-presentation', { ids: '2', fid: '3' }).isValid()).to.be.eql(true);
                expect(ns.Model.get('message-presentation', { ids: '3', fid: '3' }).isValid()).to.be.eql(true);
            });

            it('fid берётся из params.current_folder', function() {
                var mMessages = ns.Model.get('messages', { current_folder: '331122' });
                mMessages.setData({ message: [ { mid: '1', lid: [ '1' ] } ] });
                expect(ns.Model.get('message-presentation', { ids: '1', fid: '331122' }).isValid()).to.be.eql(true);
            });

            it('fid НЕ берётся из params.fid', function() {
                var mMessages = ns.Model.get('messages', { fid: '331122' });
                mMessages.setData({ message: [ { mid: '1', lid: [ '1' ] } ] });
                expect(ns.Model.get('message-presentation', { ids: '1', fid: '331122' }).isValid()).to.be.eql(false);
            });

            it('модели не создаются для невалидных мессаджей', function() {
                var mMessage1 = ns.Model.get('message', { ids: '1' }).setData({});
                var mMessage2 = ns.Model.get('message', { ids: '2' }).setData({});

                this.sinon.stub(mMessage2, 'isValid').returns(false);

                this.sinon.stub(this.mMessages.super_, '_splitModels').returns([
                    mMessage1,
                    mMessage2
                ]);

                this.mMessages.setData({});

                expect(ns.Model.get('message-presentation', { ids: '1', fid: '3' }).isValid()).to.be.eql(true);
                expect(ns.Model.get('message-presentation', { ids: '2', fid: '3' }).isValid()).to.be.eql(false);
            });
        });
    });

    describe('#_getCountForRequest', function() {
        beforeEach(function() {
            var mSettings = ns.Model.get('settings');
            this.sinon.stub(mSettings, 'getSetting').returns(30);

            this.mMessages = ns.Model.get('messages', { current_folder: '1' });
            this.mMessages.setData({
                message: []
            });
        });

        var tests = [
            [ 0, 30 ],
            [ 1, 30 ],
            [ 2, 30 ],
            [ 29, 30 ],
            [ 30, 30 ],

            [ 31, 60 ],
            [ 32, 60 ],
            [ 59, 60 ],
            [ 60, 60 ],

            [ 61, 90 ],
            [ 62, 90 ],

            [ 199, 200 ],
            [ 200, 200 ],
            [ 201, 200 ],
            [ 202, 200 ],
            [ 300, 200 ]
        ];

        tests.forEach(function(testCase) {
            var modelsCount = testCase[0];
            var requestCountValue = testCase[1];

            it(modelsCount + ' => ' + requestCountValue, function() {
                expect(this.mMessages._getCountForRequest(modelsCount)).to.be.eql(requestCountValue);
            });
        });
    });

    describe('Подгрузка / обновление данных', function() {
        describe('#refresh', function() {
            beforeEach(function() {
                var mSettings = ns.Model.get('settings');
                this.sinon.stub(mSettings, 'getSetting').returns(30);

                this.mMessages = ns.Model.get('messages', { current_folder: '1' });
                this.mMessagesParams = this.mMessages.params;

                this.sinon.stub(Daria.messages, 'preload');

                var messageDatas = {
                    m1: { mid: 'm1', date: { chunks: {} }, fid: '1' },
                    m2: { mid: 'm2', date: { chunks: {} }, fid: '1' },
                    m3: { mid: 'm3', date: { chunks: {} }, fid: '1' }
                };

                this.setupMessagesModel = function(mids) {
                    this.mMessages.setData({
                        message: mids.map(function(mid) {
                            return messageDatas[mid];
                        })
                    });
                };

                this.setupRefreshMessagesModel = function(mids, pCount) {
                    var params = _.extend(this.mMessages.params, {
                        first: 0,
                        count: pCount
                    });
                    this.mMessagesRefreshParams = params;

                    this.mMessagesRefresh = ns.Model.get('messages', params).setData({
                        message: mids.map(function(mid) {
                            return messageDatas[mid];
                        })
                    });
                };
            });

            var testCases = [
                { mMessagesBefore: [], mMessagesAfter: [ 'm1' ] },
                { mMessagesBefore: [ 'm1' ], mMessagesAfter: [ 'm1', 'm2' ] },
                { mMessagesBefore: [ 'm1', 'm2' ], mMessagesAfter: [ 'm1' ] },
                { mMessagesBefore: [ 'm1', 'm2' ], mMessagesAfter: [ 'm2' ] },
                { mMessagesBefore: [ 'm1', 'm2' ], mMessagesAfter: [ 'm1', 'm3' ] },
                { mMessagesBefore: [ 'm1', 'm2' ], mMessagesAfter: [ 'm2', 'm3' ] },
                { mMessagesBefore: [ 'm1', 'm2' ], mMessagesAfter: [ 'm2', 'm1' ] }
            ];

            testCases.forEach(function(testCase) {
                var testTitle = testCase.mMessagesBefore.join(',') + ' => ' + testCase.mMessagesAfter.join(',');

                it(testTitle + ' запрашивается правильная модель', function() {
                    this.setupMessagesModel(testCase.mMessagesBefore);
                    this.setupRefreshMessagesModel(testCase.mMessagesAfter, 30);

                    this.mMessages.refresh();

                    expect(ns.request.models).to.be.calledWith([ this.mMessagesRefresh ], { forced: true });
                });

                it(testTitle + ' проверка данных коллекции после подгрузки', function() {
                    this.setupMessagesModel(testCase.mMessagesBefore);
                    this.setupRefreshMessagesModel(testCase.mMessagesAfter, 30);

                    this.mMessages.refresh();

                    // У нас уже есть валидная модель this.mMessagesRefresh.
                    // Чтобы создать видимость, что она подгрузилась, используем событие ns-model-changed.
                    this.mMessagesRefresh.trigger('ns-model-changed');

                    expect(this.mMessages.models.map(function(mMessage) {
                        return mMessage.get('.mid');
                    })).to.be.eql(testCase.mMessagesAfter);
                });

                it(testTitle + ' + mMessages невалидна: проверка данных коллекции после подгрузки', function() {
                    this.setupMessagesModel(testCase.mMessagesBefore);
                    this.setupRefreshMessagesModel(testCase.mMessagesAfter, 30);

                    this.mMessages.invalidate();
                    this.mMessages.refresh();

                    // У нас уже есть валидная модель this.mMessagesRefresh.
                    // Чтобы создать видимость, что она подгрузилась, используем событие ns-model-changed.
                    this.mMessagesRefresh.trigger('ns-model-changed');

                    expect(this.mMessages.models.map(function(mMessage) {
                        return mMessage.get('.mid');
                    })).to.be.eql(testCase.mMessagesAfter);
                });

                it(testTitle + ' + mMessages уничтожается перед refresh: проверка данных коллекции после подгрузки', function() {
                    this.setupMessagesModel(testCase.mMessagesBefore);
                    this.setupRefreshMessagesModel(testCase.mMessagesAfter, 30);

                    ns.Model.destroy(this.mMessages);
                    this.mMessages.refresh();

                    // У нас уже есть валидная модель this.mMessagesRefresh.
                    // Чтобы создать видимость, что она подгрузилась, используем событие ns-model-changed.
                    this.mMessagesRefresh.trigger('ns-model-changed');

                    expect(this.mMessages.models.map(function(mMessage) {
                        return mMessage.get('.mid');
                    })).to.be.eql(testCase.mMessagesAfter);
                });
            });
        });

        describe('#loadMore', function() {
            beforeEach(function() {
                var mSettings = ns.Model.get('settings');
                this.sinon.stub(mSettings, 'getSetting').returns(30);

                this.mMessages = ns.Model.get('messages', { current_folder: '1' });
                this.mMessagesParams = this.mMessages.params;

                this.sinon.stub(Daria.messages, 'preload');

                var messageDatas = {
                    m1: { mid: 'm1', date: { chunks: {} }, fid: '1' },
                    m2: { mid: 'm2', date: { chunks: {} }, fid: '1' },
                    m3: { mid: 'm3', date: { chunks: {} }, fid: '1' }
                };

                this.setupMessagesModel = function(mids) {
                    this.mMessages.setData({
                        message: mids.map(function(mid) {
                            return messageDatas[mid];
                        })
                    });
                };

                this.setupLoadMoreModel = function(mids, pFirst, pCount) {
                    var params = _.extend(this.mMessages.params, {
                        first: pFirst,
                        count: pCount
                    });
                    this.mLoadMoreParams = params;

                    this.mLoadMoreMessages = ns.Model.get('messages', params).setData({
                        message: mids.map(function(mid) {
                            return messageDatas[mid];
                        })
                    });
                };
            });

            // Формат: mMessagesBefore, mLoadMoreParams, mLoadMoreMids, mMessagesAfter
            var testCases = {
                'mMessages.isValid()': [
                    [ [], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1' ], { first: 1, count: 30 }, [ 'm2' ], [ 'm1', 'm2' ] ],
                    [ [ 'm1' ], { first: 1, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1', 'm2' ], { first: 2, count: 30 }, [ 'm2', 'm1' ], [ 'm1', 'm2' ] ]
                ],

                '!mMessages.isValid() && !checkValidity': [
                    [ [], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1' ], { first: 1, count: 30 }, [ 'm2' ], [ 'm1', 'm2' ] ],
                    [ [ 'm1' ], { first: 1, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1', 'm2' ], { first: 2, count: 30 }, [ 'm2', 'm1' ], [ 'm1', 'm2' ] ]
                ],

                '!mMessages.isValid() && checkValidity': [
                    [ [], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1' ], { first: 0, count: 30 }, [ 'm2' ], [ 'm2' ] ],
                    [ [ 'm1' ], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1', 'm2' ], { first: 0, count: 30 }, [ 'm2', 'm1' ], [ 'm2', 'm1' ] ]
                ],

                'mMessages.destroy() && !checkValidity': [
                    [ [], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1' ], { first: 0, count: 30 }, [ 'm2' ], [ 'm2' ] ],
                    [ [ 'm1' ], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1', 'm2' ], { first: 0, count: 30 }, [ 'm2', 'm1' ], [ 'm2', 'm1' ] ]
                ],

                'mMessages.destroy() && checkValidity': [
                    [ [], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1' ], { first: 0, count: 30 }, [ 'm2' ], [ 'm2' ] ],
                    [ [ 'm1' ], { first: 0, count: 30 }, [ 'm1' ], [ 'm1' ] ],
                    [ [ 'm1', 'm2' ], { first: 0, count: 30 }, [ 'm2', 'm1' ], [ 'm2', 'm1' ] ]
                ]
            };

            var runTests = function(tests, testAction) {
                tests.forEach(function(testCase) {
                    var mMessagesBefore = testCase[0];
                    var mLoadMoreParams = testCase[1];
                    var mLoadMoreMids = testCase[2];
                    var mMessagesAfter = testCase[3];

                    var testTitle = mMessagesBefore.join(',') + ' => ' + mMessagesAfter.join(',');

                    it(testTitle + ' запрашивается модель с правильными параметрами', function() {
                        this.setupMessagesModel(mMessagesBefore);
                        this.setupLoadMoreModel(mMessagesAfter, mLoadMoreParams.first, mLoadMoreParams.count);

                        this.sinon.spy(ns, 'request');

                        testAction(this.mMessages);

                        expect(ns.request).to.be.calledWith('messages', _.extend({}, this.mMessages.params, mLoadMoreParams));
                    });

                    it(testTitle + ' проверка данных коллекции после подгрузки', function(done) {
                        this.setupMessagesModel(mMessagesBefore);
                        this.setupLoadMoreModel(mLoadMoreMids, mLoadMoreParams.first, mLoadMoreParams.count);

                        testAction(this.mMessages).then(function() {
                            expect(this.mMessages.models.map(function(mMessage) {
                                return mMessage.get('.mid');
                            })).to.be.eql(mMessagesAfter);
                            done();
                        }.bind(this));
                    });
                });
            };

            runTests(testCases['mMessages.isValid()'], function(mMessages) {
                return mMessages.loadMore();
            });

            runTests(testCases['!mMessages.isValid() && !checkValidity'], function(mMessages) {
                mMessages.invalidate();
                return mMessages.loadMore();
            });

            runTests(testCases['!mMessages.isValid() && checkValidity'], function(mMessages) {
                mMessages.invalidate();
                return mMessages.loadMore(true);
            });

            runTests(testCases['mMessages.destroy() && !checkValidity'], function(mMessages) {
                ns.Model.destroy(mMessages);
                return mMessages.loadMore();
            });

            runTests(testCases['mMessages.destroy() && checkValidity'], function(mMessages) {
                ns.Model.destroy(mMessages);
                return mMessages.loadMore(true);
            });

            describe('requestParams.first ->', function() {
                sit('Top Results пуста -> дозапрос с начала', { top_relevant: 0, total_found: 0 }, { first: 0 });
                sit('Top Results не пуста -> дозапрос начиная с последней модели', { top_relevant: 3, total_found: 3 }, { first: 3 });

                sit('не Top Results пуста -> дозапрос начиная с числа Top Results', { top_relevant: 2, total_found: 2 }, { first: 2 });
                sit('не Top Results не пуста -> дозапрос начиная с числа Top Results + число моделей', { top_relevant: 2, total_found: 5 }, { first: 5 });

                function sit(testTitle, modelInfo, resultInfo) {
                    it(testTitle, function() {
                        var all = [
                            { mid: 'm1', date: { chunks: {} }, fid: '1' },
                            { mid: 'm2', date: { chunks: {} }, fid: '1' },
                            { mid: 'm3', date: { chunks: {} }, fid: '1' },
                            { mid: 'm4', date: { chunks: {} }, fid: '1' },
                            { mid: 'm5', date: { chunks: {} }, fid: '1' }
                        ];

                        this.sinon.stub(ns, 'request').returns({ then: function() {} });

                        this.mMessages.setData({
                            details: { 'top-relevant': modelInfo.top_relevant },
                            message: all.slice(0, modelInfo.total_found)
                        });

                        this.mMessages.loadMore();

                        expect(ns.request).to.be.calledWith('messages', {
                            first: resultInfo.first,
                            count: 30,
                            current_folder: '1',
                            sort_type: 'date',
                            with_pins: 'yes'
                        });
                    });
                }
            });

            describe('подгрузка поисковой выдачи ->', function() {
                beforeEach(function() {
                    this.mMessages = ns.Model.get('messages',
                        { fid: 333, search: 'search', request: 'hi' }
                    ).setData({
                        details: {
                            'top-relevant': 2,
                            'total-found': 7,
                            'topResultsMids': [ 'm1', 'm2' ]
                        },
                        message: [
                            { mid: 'm1', date: { chunks: {} }, fid: '1' },
                            { mid: 'm2', date: { chunks: {} }, fid: '1' },
                            { mid: 'm3', date: { chunks: {} }, fid: '1' },
                            { mid: 'm4', date: { chunks: {} }, fid: '1' }
                        ]
                    });
                });

                sit(
                    'новая порция без Top Results',
                    {
                        details: {
                            'top-relevant': 2,
                            'total-found': 7
                        },
                        message: [
                            { mid: 'm5', date: { chunks: {} }, fid: '1' },
                            { mid: 'm6', date: { chunks: {} }, fid: '1' },
                            { mid: 'm7', date: { chunks: {} }, fid: '1' }
                        ]
                    },
                    2,
                    [ 'm1', 'm2' ],
                    [ 'm1', 'm2', 'm3', 'm4', 'm5', 'm6', 'm7' ]
                );

                sit(
                    'в новой порции прилетает ещё одно письмо из Top Results',
                    {
                        details: {
                            'top-relevant': 3,
                            'total-found': 7,
                            'topResultsMids': [ 'm5' ]
                        },
                        message: [
                            { mid: 'm5', date: { chunks: {} }, fid: '1' },
                            { mid: 'm6', date: { chunks: {} }, fid: '1' },
                            { mid: 'm7', date: { chunks: {} }, fid: '1' }
                        ]
                    },
                    // +1 Top Results сообщение.
                    3,
                    // Новое сообщение Top Results добавляется в конец.
                    [ 'm1', 'm2', 'm5' ],
                    // Все сообщения переставляются так, чтобы Top Results были в голове списка.
                    [ 'm1', 'm2', 'm5', 'm3', 'm4', 'm6', 'm7' ]
                );

                sit(
                    'в новой порции прилетает topResultsMids где нет части сообщений, которые были',
                    {
                        details: {
                            'top-relevant': 2,
                            'total-found': 7,
                            'topResultsMids': [ 'm2' ]
                        },
                        message: [
                            { mid: 'm5', date: { chunks: {} }, fid: '1' },
                            { mid: 'm6', date: { chunks: {} }, fid: '1' },
                            { mid: 'm7', date: { chunks: {} }, fid: '1' }
                        ]
                    },
                    // Число Top Results остаётся прежним, старые сообщения не удаляем их Top Results при подгрузке.
                    2,
                    // Список Top Results не меняется.
                    [ 'm1', 'm2' ],
                    // Последовательность "Top Results в голове списка" сохраняется.
                    [ 'm1', 'm2', 'm3', 'm4', 'm5', 'm6', 'm7' ]
                );

                function sit(testTitle, testData, expectedTopResultsCount, expectedTopMids, expectedMids) {
                    it(testTitle, function() {
                        ns.request.models.restore();

                        this.sinon.stub(ns.request, 'models').callsFake(function(models) {
                            models[0].setData(testData);
                            return Vow.resolve(models);
                        });

                        return this.mMessages.loadMore().then(function() {
                            expect(this.mMessages.get('.details.top-relevant')).to.be.equal(expectedTopResultsCount);
                            expect(this.mMessages.get('.details.topResultsMids')).to.be.eql(expectedTopMids);
                            expect(this.mMessages.models.map(function(mMessage) {
                                return mMessage.get('.mid');
                            }))
                                .to.be.eql(expectedMids);
                        }, this);
                    });
                }
            });
        });

        describe('#getRequest', function() {
            beforeEach(function() {
                var mSettings = ns.Model.get('settings');
                this.sinon.stub(mSettings, 'getSetting').returns(30);

                this.mMessages = ns.Model.get('messages', { current_folder: '1' });
            });

            it('валидная модель - запрашивается напрямую', function() {
                expect(this.mMessages.getRequest()).to.be.eql(this.mMessages);
            });

            it('пустая невалидная модель - также запрашивается напрямую', function() {
                this.mMessages.setData({ message: [] });
                this.mMessages.invalidate();
                expect(this.mMessages.getRequest()).to.be.eql(this.mMessages);
            });

            it('для непустой невалидной модели - создаётся специальная модель для запроса', function() {
                this.mMessages.setData({
                    message: [
                        { mid: 'm1', date: { chunks: {} }, fid: '1' },
                        { mid: 'm2', date: { chunks: {} }, fid: '1' }
                    ]
                });
                this.mMessages.invalidate();
                expect(this.mMessages.getRequest()).to.be.eql(
                    ns.Model.get('messages', _.extend({}, this.mMessages.params, { first: 0, count: 30 }))
                );
            });

            it('для уничтоженной модели - создаётся специальная модель для запроса', function() {
                this.mMessages.setData({
                    message: [
                        { mid: 'm1', date: { chunks: {} }, fid: '1' },
                        { mid: 'm2', date: { chunks: {} }, fid: '1' },
                        { mid: 'm3', date: { chunks: {} }, fid: '1' }
                    ]
                });
                ns.Model.destroy(this.mMessages);
                expect(this.mMessages.getRequest()).to.be.eql(
                    ns.Model.get('messages', _.extend({}, this.mMessages.params, { first: 0, count: 30 }))
                );
            });

            it('невалидная модель с заданным params.first будет запрашиваться с этим значением first', function() {
                var mMessages = ns.Model.get('messages', { current_folder: '1', first: 33 });
                mMessages.setData({
                    message: [
                        { mid: 'm1', date: { chunks: {} }, fid: '1' },
                        { mid: 'm2', date: { chunks: {} }, fid: '1' },
                        { mid: 'm3', date: { chunks: {} }, fid: '1' }
                    ]
                });
                ns.Model.destroy(mMessages);
                expect(mMessages.getRequest()).to.be.eql(
                    ns.Model.get('messages', _.extend({}, mMessages.params, { first: 33, count: 30 }))
                );
            });

            it('значение count не может превышать 200 (ограничение wmi - они возвращают максимум 200 писем)', function() {
                this.mMessages.setData({
                    message: (new Array(250)).join('.').split('.').map(function(item, itemIndex) {
                        return { mid: 'm' + itemIndex, date: { chunks: {} }, fid: '1' };
                    })
                });
                this.mMessages.invalidate();
                expect(this.mMessages.getRequest()).to.be.eql(
                    ns.Model.get('messages', _.extend({}, this.mMessages.params, { first: 0, count: 200 }))
                );
            });

            it('Если нет параметров для нормального запроса за моделями, но canRequest === true, ' +
                'то не должно ничего запрашиваться и отстутствие параметров должно залогироваться',
            function() {
                this.sinon.stub(this.mMessages, 'params').value({});
                this.sinon.stub(Jane.ErrorLog, 'send');
                this.mMessages.invalidate();
                expect(this.mMessages.getRequest()).to.be.eql(this.mMessages);
                expect(this.mMessages.request).to.be.eql(this.mMessages._fakeRequest);
                expect(Jane.ErrorLog.send).to.has.callCount(1);
            }
            );

            it('Если нет параметров для нормального запроса за моделями, но canRequest === true и allow_empty=true, ' +
                'то не должно ничего запрашиваться и отстутствие параметров не должно логироваться',
            function() {
                this.sinon.stub(this.mMessages, 'params').value({ allow_empty: true });
                this.sinon.stub(Jane.ErrorLog, 'send');
                this.mMessages.invalidate();
                expect(this.mMessages.getRequest()).to.be.eql(this.mMessages);
                expect(this.mMessages.request).to.be.eql(this.mMessages._fakeRequest);
                expect(Jane.ErrorLog.send).to.has.callCount(0);
            }
            );
        });
    });

    describe('#getRequestParams ->', function() {
        describe('count ->', function() {
            sit('для поиска - не меньше 10: 7 -> 10', true, 7, 10);
            sit('для поиска - не меньше 10: 10 -> 10', true, 10, 10);
            sit('для поиска - не меньше 10: 30 -> 30', true, 30, 30);

            sit('для НЕ поиска - без ограничений: 7 -> 7', false, 7, 7);
            sit('для НЕ поиска - без ограничений: 10 -> 10', false, 10, 10);
            sit('для НЕ поиска - без ограничений: 30 -> 30', false, 30, 30);

            function sit(testTitle, isSearch, tryCountValue, validCountValue) {
                it(testTitle, function() {
                    if (typeof tryCountValue !== 'undefined') {
                        this.sinon.stub(this.model.super_, 'getRequestParams').returns({ count: tryCountValue });
                    }

                    if (isSearch) {
                        this.model.params.search = 'search';
                        this.sinon.stub(this.model, 'isSearchNotFilter').returns(true);
                    }

                    //eslint-disable-next-line no-constant-condition
                    if (typeof validCountValue) {
                        expect(this.model.getRequestParams({}).count).to.be.eql(validCountValue);
                    } else {
                        expect(this.model.getRequestParams({})).to.not.have.property('count');
                    }
                });
            }
        });
    });

    describe('#_updateDetailsForSearch', function() {
        beforeEach(function() {
            var all = this.all = [
                { mid: 'm1', date: { chunks: {} }, fid: '1' },
                { mid: 'm2', date: { chunks: {} }, fid: '1' },
                { mid: 'm3', date: { chunks: {} }, fid: '1' },

                { mid: 'm4', date: { chunks: {} }, fid: '1' },
                { mid: 'm5', date: { chunks: {} }, fid: '1' },
                { mid: 'm6', date: { chunks: {} }, fid: '1' },
                { mid: 'm7', date: { chunks: {} }, fid: '1' }
            ];

            this.mMessages = ns.Model.get('messages', { fid: 333 }).setData({
                details: {
                    'top-relevant': 3,
                    'total-found': 7,
                    'topResultsMids': [ 'm1', 'm2', 'm3' ]
                },
                message: all
            });
        });

        describe('удаление нескольких Top Results писем ->', function() {
            beforeEach(function() {
                this.mMessages.remove([
                    this.mMessages.models[0],
                    this.mMessages.models[1]
                ]);
            });

            it('total-found уменьшается на число удалённых моделей', function() {
                expect(this.mMessages.get('.details.total-found')).to.be.equal(5);
            });

            it('top-relevant также уменьшается на число удалённых моделей', function() {
                expect(this.mMessages.get('.details.top-relevant')).to.be.equal(1);
            });

            it('mid-ы удалены из details.topResultsMids', function() {
                expect(this.mMessages.get('.details.topResultsMids')).to.be.eql([ 'm3' ]);
            });
        });

        describe('удаление нескольких писем из остальных результатов ->', function() {
            beforeEach(function() {
                this.mMessages.remove([
                    this.mMessages.models[3],
                    this.mMessages.models[4]
                ]);
            });

            it('total-found уменьшается на число удалённых моделей', function() {
                expect(this.mMessages.get('.details.total-found')).to.be.equal(5);
            });

            it('top-relevant - прежний', function() {
                expect(this.mMessages.get('.details.top-relevant')).to.be.equal(3);
            });

            it('details.topResultsMids остался прежним', function() {
                expect(this.mMessages.get('.details.topResultsMids')).to.be.eql([ 'm1', 'm2', 'm3' ]);
            });
        });
    });

    describe('#isSearchNotFilter ->', function() {
        sit(
            'список писем во входящих',
            { current_folder: '1', sort_type: 'date', threaded: 'yes', with_pins: 'yes' },
            false
        );

        sit(
            'поиск',
            { request: 'привет', search: 'search', sort_type: 'date', with_pins: 'yes' },
            true
        );

        sit(
            'фильтр "Билеты"',
            { search: 'search', sort_type: 'date', type: '5', with_pins: 'yes' },
            false
        );

        sit(
            'поиск по фильтру "Билеты"',
            { request: 'билет', search: 'search', sort_type: 'date,', type: '5', with_pins: 'yes' },
            true
        );

        sit(
            'поиск по фильтру "С вложениями"',
            { extra_cond: 'only_atta', goto: 'all', sort_type: 'date', with_pins: 'yes' },
            false
        );

        sit(
            'выдача по сборщику',
            {
                request: 'test@imap.yandex.ru', scope: 'rpopinfo', search: 'search',
                sort_type: 'date,', with_pins: 'yes'
            },
            false
        );
        sit(
            'выдача писем-рассылок',
            {
                request: 'subscription-email:support@codepen.io AND type:7'
            },
            false
        );

        function sit(testTitle, params, expectedResult) {
            it(testTitle, function() {
                var mMessages = ns.Model.get('messages', params).setData({
                    details: {},
                    message: []
                });
                expect(mMessages.isSearchNotFilter()).to.be.equal(expectedResult);
            });
        }
    });

    describe('#setError', function() {
        beforeEach(function() {
            this.scenarioManager = this.sinon.stubScenarioManager(this.model);
        });

        it('Должен дописать шаг "opening-error-messages", если есть активный сценарий "Просмотр письма"', function() {
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.withArgs('message-view-scenario').returns(true);
            this.scenarioManager.getActiveScenario.withArgs('message-view-scenario').returns(scenario);

            this.model.setError({});

            expect(scenario.logError)
                .to.have.callCount(1)
                .and.to.be.calledWith({ type: 'opening-error-messages', severity: 'blocker' });
        });

        it('Должен дописать шаг "search-error-messages", если есть активный сценарий "Поиск писем"', function() {
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.withArgs('search-scenario').returns(true);
            this.scenarioManager.getActiveScenario.withArgs('search-scenario').returns(scenario);

            this.model.setError({});

            expect(scenario.logError)
                .to.have.callCount(1)
                .and.to.be.calledWith({ type: 'search-error-messages', severity: 'blocker' });
        });
    });
});
