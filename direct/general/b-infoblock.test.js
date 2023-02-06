// b-infoblock.js использует нативные промисы
// фейковые таймеры неправильно работают с промисами
// см DIRECT-71562
describe.skip('b-infoblock', function() {
    var sandbox,
        /**
         * URL ручки для отправки и чтения состояний инфоблока
         * @type {String}
         */
        ajaxUrl = '/ajax/infoblock/process_message',
        /**
         * URL ручки для получения новостей
         * @type {String}
         */
        newsUrl = '/ajax/news_by_id',
        /**
         * Создает блок инфоблока и встраивает его в DOM
         * @param {Object} [ctx]
         * @returns {BEM.DOM}
         */
        createBlock = function(ctx) {
            return u.createBlock(u._.extend({
                block: 'b-infoblock',
                messagesURL: ajaxUrl,
                newsFetchURL: newsUrl,
            }, ctx), {
                inject: true,
                hidden: false
            });
        },
        /**
         * Возвращает фейковых данных о состоянии инфоблока
         * Для обновления фейковых данных можно воспользоваться ответом от ручки инфоблока (скопировав нужные части
         * ответа в консоли браузера, просто пройти по ручке недостаточно, так как там POST запросы ходят)
         * @param {Object} [params]
         * @param {Boolean} [params.expose=false] флаг о том, что инфоблок должен быть уже открыт автоматически
         * @param {Boolean} [params.empty=false] флаг для того, чтобы инфоблок отдал ответ без новостей и тизеров
         * @param {Boolean} [params.show_news=true] флаг для о том, что инфоблок должен показывать новости
         * @param {Boolean} [params.show_block=true] флаг для о том, что инфоблок должен показываться
         * @returns {Object}
         */
        getStateInfo = function(params) {
            var settings = params || {},
                news = [];

            news = [
                {
                    lang: 'ru',
                    closed: false,
                    ext_news_id: 'd-2014-03-24',
                    read: false,
                    region: 'ru',
                    data: {
                        date: '24 марта 2014 г',
                        content: [
                            {
                                text: ''
                            },
                            {
                                text: 'Мобильный Директ',
                                link_url: 'http://yandex.ru/support/direct-news/n-2014-03-24.xml'
                            },
                            {
                                text: ' стал доступен владельцам Android'
                            }
                        ]
                    }
                },
                {
                    lang: 'ru',
                    closed: false,
                    ext_news_id: 'd-2014-03-20',
                    read: false,
                    region: 'ru',
                    data: {
                        date: '20 марта 2014 г',
                        content: [
                            {
                                text: ''
                            },
                            {
                                text: 'Три новых режима',
                                link_url: 'http://yandex.ru/support/direct-news/n-2014-03-20.xml'
                            },
                            {
                                text: ' подбора дополнительных релевантных фраз.'
                            }
                        ]
                    }
                },
                {
                    ext_news_id: 'd-2014-03-13',
                    read: false,
                    closed: false
                },
                {
                    ext_news_id: 'd-2014-02-17',
                    read: false,
                    closed: false
                }
            ];

            return {
                news: {
                    unseen_count: news.length,
                    items: news
                },
                error: null,
                // не добавляем фейковый видеотизер в тестах
                testScope: true
            };
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            // указываем основу таймера, иначе new Date() в коде всегда 'Thu Jan 01 1970 03:00:00 GMT+0300 (MSK)'
            useFakeTimers: [+new Date()],
            useFakeServer: true
        });
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Общие моменты', function() {
        var block,
            /**
             * Регистрирует обработчики фэйкового сервера для ответа ошибками на запросы
             */
            registerServerFailure = function() {
                u.respondJSONWithRoutes(sandbox.server, [
                    ['POST', ajaxUrl, function() { throw new Error('No Answer Error'); }]
                ]);
            },
            /**
             * Регистрирует обработчики фэйкового сервера для ответа на запросы
             */
            registerServerSuccess = function(state) {
                u.respondJSONWithRoutes(sandbox.server, [
                    ['POST', ajaxUrl, function(xhr) {
                        var results = { 'require-events': state };

                        return u.getRequestBody(xhr).queue.map(function(queue) {
                            return { uniqId: queue.uniqId, result: results[queue.type] };
                        });
                    }]
                ]);
            };

        beforeEach(function() {
            // block = createBlock();

            // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
            // sandbox.clock.tick(1);
        });

        afterEach(function() {
            // block.destruct();
        });

        describe('При создании блока запрашивается список "элементов инфоблока"', function() {
            var requestBody;

            beforeEach(function() {
                requestBody = u.getRequestBody(sandbox.server.requests[0]);
            });

            it('sendEventMessage("require-events") послал запрос где в queue лежит массив', function() {

                expect(requestBody.queue).to.be.an('array');
            });

            it('sendEventMessage("require-events") послал запрос где в элементах массива лежит объект с полем type="require-events"', function() {

                expect(requestBody.queue[0].type).to.be.equal('require-events');
            });

            it('sendEventMessage("require-events") послал запрос где в элементах массива лежит объект с не пустым полем uniqId', function() {

                expect(requestBody.queue[0].uniqId).to.not.be.empty;
            });

            it('sendEventMessage("require-events") послал запрос где в элементах массива лежит объект с полем data, который является пустым объектом', function() {

                expect(requestBody.queue[0].data).to.deep.equal({ });
            });

            it('если всё ok, то click на switcher вызывает toggle()', function() {
                registerServerSuccess(getStateInfo());

                sandbox.stub(block, 'toggle').callsFake(function() { });

                sandbox.server.respond();

                block.elem('switcher').click();

                // ждём когда отработает клик (afterCurrentEvent у b-link_pseudo_yes)
                sandbox.clock.tick(1);

                expect(block.toggle.called).to.be.true;
            });

        });

        describe('Проверка модели состояния инфоблока', function() {

            ['hasNewEvents', 'itemsCount'].forEach(function(field) {

                it('серверная ручка прислала пустой ответ - поле модели `' + field + '=0`', function() {
                    registerServerSuccess(getStateInfo({ empty: true }));

                    sandbox.server.respond();

                    expect(+block._getModel().get(field)).to.be.equal(0);
                });

            });

            u._.forOwn(
                {
                    showBlock: 'show_block',
                    showNews: 'show_news',
                    expose: 'expose'
                },
                function(property, field) {

                    describe('Соответствие ответа сервера `' + property + '` и поля модели `' + field + '`', function() {

                        [false, true].forEach(function(value) {

                            it('если серверная ручка прислала ' + value + ', то поле модели ' + value, function() {
                                var params = {};

                                params[property] = value;

                                registerServerSuccess(getStateInfo(params));

                                // не надо идти дальше вызова toggle()
                                sandbox.stub(block, 'toggle').callsFake(function() { });

                                sandbox.server.respond();

                                expect(block._getModel().get(field)).to.be.equal(value);
                            });

                        });

                    });

                });

            [false, true].forEach(function(show) {

                it('если серверная ручка прислала `show_block=' + show + '`, то модификатор hidden ' + ['выставлен', 'снят'][+show], function() {
                    registerServerSuccess(getStateInfo({ show_block: show }));

                    sandbox.server.respond();

                    (show ? expect(block).not : expect(block))
                        .to.haveMod('hidden', 'yes');
                });

            });

        });

        describe('Автопоказ при инициализации блока после получения состояния от сервера', function() {
            var block,
                /**
                 * Регистрирует обработчики фэйкового сервера для ответа на запросы
                 * @param {Object} state предустановленное состояние инфоблока для тест-кейса
                 */
                registerServerResponse = function(state) {
                    u.respondJSONWithRoutes(sandbox.server, [
                        ['POST', ajaxUrl, function(xhr) {
                            var results = { 'require-events': state };

                            return u.getRequestBody(xhr).queue.map(function(queue) {
                                return { uniqId: queue.uniqId, result: results[queue.type] };
                            });
                        }]
                    ]);
                };

            [true, false].forEach(function(expose) {
                var not = ['', 'не '][+expose];

                it(not + 'требуется -  ' + not + ' вызывает toggle()', function() {
                    block = createBlock();

                    sandbox.stub(block, 'toggle').callsFake(function() { });
                    registerServerResponse(getStateInfo({ expose: expose }));

                    // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
                    sandbox.clock.tick(1);

                    sandbox.server.respond();

                    expect(block.toggle.called).to.be.equal(expose);

                    // в destruct снимается mod autoclosable, чтобы отписаться от ивентов
                    // надо сделать задержку, иначе тест упадет из-за afterCurrentEvent
                    sandbox.clock.tick(100);

                    block.destruct();
                });

            });

            it('Если автораскрытие инфоблока включено, но уведомления запрещены, автораскрытия не происходит', function() {
                block = createBlock({ autoExposeDisabled: true });
                sandbox.stub(block, 'toggle').callsFake(function() { });

                registerServerResponse(getStateInfo({ expose: true }));

                // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
                sandbox.clock.tick(1);

                sandbox.server.respond();

                expect(block.toggle.called).to.equal(false);

                // в destruct снимается mod autoclosable, чтобы отписаться от ивентов
                // надо сделать задержку, иначе тест упадет из-за afterCurrentEvent
                sandbox.clock.tick(100);

                block.destruct();
            });

        });

        describe('toggle() ', function() {
            var block;

            beforeEach(function() {
                u.respondJSONWithRoutes(sandbox.server, [
                    ['POST', ajaxUrl, function(xhr) {
                        var results = {
                            'require-events': getStateInfo(),
                            'update-last-opened': {},
                            'update-news-state': {}
                        };

                        return u.getRequestBody(xhr).queue.map(function(queue) {
                            return { uniqId: queue.uniqId, result: results[queue.type] };
                        });
                    }]
                ]);

                block = createBlock();

                // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
                sandbox.clock.tick(1);
                sandbox.server.respond();
            });

            afterEach(function() {
                // в destruct снимается mod autoclosable, чтобы отписаться от ивентов
                // надо сделать задержку, иначе тест упадет из-за afterCurrentEvent
                sandbox.clock.tick(100);

                block.destruct();
            });

            it('триггерит событие opening, если popup не открыт', function() {
                expect(block).to.triggerEvent('opening', function() {
                    block.toggle();
                });

                // ждем актуализации состояния
                sandbox.clock.tick(1);
                sandbox.server.respond();
            });

            it('не триггерит событие opening, если popup открыт', function() {
                block.toggle();

                // ждем актуализации состояния
                sandbox.clock.tick(1);
                sandbox.server.respond();

                expect(block).not.to.triggerEvent('opening', function() {
                    block.toggle();
                });
            });

            it('делает popup _visibility_visible', function() {
                block.toggle();

                // ждем актуализации состояния
                sandbox.clock.tick(1);
                sandbox.server.respond();

                expect(block._getPopup()).to.haveMod('visibility', 'visible');
            });

            //Задача на разскипать https://st.yandex-team.ru/DIRECT-62891
            it.skip('делает открытый popup НЕ _visibility_visible', function() {
                block.toggle();

                // ждем актуализации состояния
                sandbox.clock.tick(1);
                sandbox.server.respond();

                block.toggle();

                sandbox.clock.tick(1000);

                expect(block._getPopup()).not.to.haveMod('visibility', 'visible');
            });

        });

        it.only('При закрытии по крестику дергается ручка обновления статуса', function() {
            u.respondJSONWithRoutes(sandbox.server, [
                ['POST', ajaxUrl, function(xhr) {
                    var results = {
                        'require-events': getStateInfo(),
                        'update-last-opened': {},
                        'update-news-state': {}
                    };

                    return u.getRequestBody(xhr).queue.map(function(queue) {
                        return { uniqId: queue.uniqId, result: results[queue.type] };
                    });
                }]
            ]);

            block = createBlock();

            // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
            sandbox.clock.tick(1);
            sandbox.server.respond();
            sandbox.clock.tick(1);

            var spy = sandbox.spy(BEM.DOM.blocks['b-infoblock'].prototype, 'sendEventMessage');

            block._getPopup().elem('close').trigger('click');

            expect(spy.calledWith('update-last-opened')).to.be.true;
        });
    });

    describe('При работе с инфоблоком', function() {
        var block,
            state,
            spy;

        beforeEach(function() {
            state = getStateInfo();

            u.respondJSONWithRoutes(sandbox.server, [
                ['POST', ajaxUrl, function(xhr) {
                    var results = {
                        'require-events': state,
                        'update-last-opened': {},
                        'update-news-state': {}
                    };

                    return u.getRequestBody(xhr).queue.map(function(queue) {
                        return { uniqId: queue.uniqId, result: results[queue.type] };
                    });
                }],
                ['GET', new RegExp(newsUrl), function(xhr) {
                    return {
                        ext_news_id: xhr.url.match(/[\?&]ext_news_id=([^&]+)/)[1],
                        lang: 'ru',
                        region: 'en',
                        data: {
                            date: 'd-2014-03-13',
                            content: [{ text: 'fetched mock' }]
                        }
                    };
                }]
            ]);

            spy = sandbox.spy(BEM.DOM.blocks['b-infoblock'].prototype, 'sendEventMessage');

            block = createBlock();

            // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
            sandbox.clock.tick(1);

            sandbox.server.respond();

            block.toggle();

            sandbox.clock.tick(100); // ждём открытия попапа

            sandbox.server.respond(); // отвечаем на запросы, посланные при открытии
        });

        afterEach(function() {
            block.destruct();
        });


        describe('после открытия', function() {
            var
                /**
                 * Возвращает блок кнопок ротации для указаного блока инфоблока
                 * @param {BEM.DOM} block инфоблок
                 * @param {String} blockInside имя блока внутри (новости/тизеры)
                 * @returns {BEM.DOM}
                 */
                getRotator = function(block, blockInside) {
                    return block._getPopup().findBlockInside(blockInside).findBlockInside('b-rotate-buttons');
                },
                /**
                 * Возвращает блок единичной кнопки у блока ротации для указаного блока инфоблока
                 * @param {BEM.DOM} block инфоблок
                 * @param {String} blockInside имя блока внутри (новости/тизеры)
                 * @param {String} buttonElem название кнопки (back/next)
                 * @returns {BEM.DOM}
                 */
                getRotatorButton = function(block, blockInside, buttonElem) {
                    return getRotator(block, blockInside).findBlockOn(buttonElem, 'button')
                };

            describe('для новостей', function() {

                describe('Кнопка "Показать еще"', function() {
                    var news,
                        more;

                    beforeEach(function() {
                        news = block._getPopup().findBlockInside('b-infoblock-news');
                        more = news.findBlockInside('more', 'link');

                        sandbox.server.respondWith('GET', '/ajax/news_by_id?ext_news_id=d-2014-03-13', [200, {"Content-Type":"application/json"}, JSON.stringify({
                            "lang": "ru",
                            "region": "ru",
                            "data": {
                                "content": [
                                    {
                                        "text": "Точно по адресу: "
                                    }, {
                                        "text": "гиперлокальный таргетинг по сегментам Аудиторий",
                                        "link_url": "http://yandex.ru/support/direct-news/n-2016-11-24.xml"
                                    }, {
                                        "text": ""
                                    }
                                ],
                                "date": "24 ноября 2016 г"
                            },
                            "ext_news_id": "d-2016-11-24"
                        })
                        ])
                    });

                    it('дергает серверную ручку', function() {
                        more.trigger('click');

                        var request = u._.filter(sandbox.server.requests, function(request) {
                            return request.url == '/ajax/news_by_id?ext_news_id=d-2014-03-13'
                        });

                        expect(request).not.to.equal(undefined);
                    });

                    it('новость появляется в интерфейсе', function() {
                        more.trigger('click');

                        sandbox.server.respond();

                        expect(news.elem('event').length).to.equal(3);
                    })
                })

            });

        });
    });

    describe('Кнопка pin', function() {
        var pin,
            block,
            popup;

        beforeEach(function() {
            u.respondJSONWithRoutes(sandbox.server, [
                ['POST', ajaxUrl, function(xhr) {
                    var results = {
                        'require-events': getStateInfo(),
                        'update-last-opened': {},
                        'update-news-state': {}
                    };

                    return u.getRequestBody(xhr).queue.map(function(queue) {
                        return { uniqId: queue.uniqId, result: results[queue.type] };
                    });
                }]
            ]);

            block = createBlock();

            sandbox.spy(block, 'trigger');

            // ждём когда уйдёт запрос (для инфоблока задана задержка в 1ms)
            sandbox.clock.tick(1);
            sandbox.server.respond();
        });

        afterEach(function() {
            // в destruct снимается mod autoclosable, чтобы отписаться от ивентов
            // надо сделать задержку, иначе тест упадет из-за afterCurrentEvent
            sandbox.clock.tick(100);

            block.destruct();
        });

        it('По клику на кнопку снимается модификатор autoclosable', function() {
            block.toggle();

            popup = block._getPopup();
            pin = popup.findBlockInside(block.elem('pinner'), 'link');

            pin.trigger('click');

            expect(popup).not.to.haveMod('autoclosable');
        });

        it('По повторином клику на кнопку ставится модификатор autoclosable', function() {
            block.toggle();

           popup = block._getPopup();
           pin = popup.findBlockInside(block.elem('pinner'), 'link');

           pin.trigger('click');
           pin.trigger('click');

           expect(popup).to.haveMod('autoclosable', 'yes')
        });
    })

});
