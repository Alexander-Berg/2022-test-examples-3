describe('b-infoblock-news', function() {
    var block,
        ctx = {
            block: 'b-infoblock-news'
        },
        sandbox,
        modelsList,
        modelData = [
            {
                  "ext_news_id" : "d-2017-04-04",
                  "new" : false,
                  "read" : false,
                  "visited" : false,
                  "seen" : true,
                  "data" : {
                     "date" : "4 апреля 2017 г",
                     "content" : [
                        {
                           "text" : ""
                        },
                        {
                           "text" : "В Директе появятся корректировки по региону показов объявлений ",
                           "link_url" : "https://yandex.ru/support/direct-news/n-2017-04-04.html"
                        },
                        {
                           "text" : ""
                        }
                     ]
                  },
                  "region" : "ru",
                  "lang" : "ru"
               },
               {
                  "data" : {
                     "content" : [
                        {
                           "text" : ""
                        },
                        {
                           "text" : "22 марта в Директе изменится минимальный платеж ",
                           "link_url" : "http://yandex.ru/support/direct-news/n-2017-03-15.xml"
                        },
                        {
                           "text" : ""
                        }
                     ],
                     "date" : "15 марта 2017 г"
                  },
                  "seen" : true,
                  "region" : "ru",
                  "lang" : "ru",
                  "visited" : false,
                  "read" : true,
                  "ext_news_id" : "d-2017-03-15",
                  "new" : false
               },
               {
                  "seen" : true,
                  "data" : {
                     "content" : [
                        {
                           "text" : ""
                        },
                        {
                           "text" : "Look-alike расширяет охват и становится гибче: ищем «похожих» в разных городах и на разных устройствах",
                           "link_url" : "http://yandex.ru/support/direct-news/n-2017-02-28.xml"
                        },
                        {
                           "text" : ""
                        }
                     ],
                     "date" : "28 февраля 2017 г"
                  },
                  "region" : "ru",
                  "lang" : "ru",
                  "new" : false,
                  "ext_news_id" : "d-2017-02-28",
                  "read" : true,
                  "visited" : false
               },
               {
                  "seen" : true,
                  "ext_news_id" : "d-2017-02-14",
                  "new" : false,
                  "read" : true,
                  "visited" : false
               }

        ];

    beforeEach(function() {
        block = u.createBlock(ctx);
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('Отрисовка исходных новостей (при их наличии)', function() {
        beforeEach(function() {
            modelsList = BEM.MODEL.create('m-infoblock-state', {
                news: modelData,
                teasers: [],
                last_opened: 1,
                show_news: true,
                show_block: true,
                show_teasers: true,
                expose: false,
                error: null
            }).get('news');

            block.repaint(modelsList);
        });

        it('В блоке есть 3 новости', function() {
            expect(block.findElem('snippet').length).to.equal(3);
        });

        it('В блоке есть кнопка подгрузки новостей', function() {
            expect(block.findBlockInside('more', 'link')).not.to.be.null;
        });

        describe('Тексты и даты ссылок соответствуют присланным', function() {
            [
                "В Директе появятся корректировки по региону показов объявлений",
                "22 марта в Директе изменится минимальный платеж",
                "Look-alike расширяет охват и становится гибче: ищем «похожих» в разных городах и на разных устройствах"
            ].forEach(function(text) {
                it('Есть ровно 1 ссылка с текстом ' + text, function() {
                    var snippets = block.findElem('snippet'),
                        hasText = 0;

                    snippets.each(function(index, snippet) {
                        var snippetText = $.trim($(snippet).find('.link__inner').text());
                        text == snippetText ? hasText++ : hasText;
                    });

                    expect(hasText).to.equal(1)
                })
            });

            [
                "15 марта 2017",
                "04 апреля 2017",
                "28 февраля 2017"
            ].forEach(function(date) {
                it('Есть ровно 1 сниппет с датой ' + date, function() {
                    var snippets = block.findElem('date'),
                        hasText = 0;

                    snippets.each(function(index, dt) {
                        var dateText = $(dt).text();
                        date == dateText ? hasText++ : hasText;
                    });

                    expect(hasText).to.equal(1)
                })
            })
        });
    });

    describe('Отрисовка исходных пустых новостей', function() {
        beforeEach(function() {
            modelsList = BEM.MODEL.create('m-infoblock-state', {
                news: [],
                teasers: [],
                last_opened: 1,
                show_news: true,
                show_block: true,
                show_teasers: true,
                expose: false,
                error: null
            }).get('news');

            block.repaint(modelsList);
        });

        it('В блоке нет новостей', function() {
            expect(block.findElem('snippet').length).to.equal(0);
        });

        it('В блоке есть кнопки подгрузки новостей', function() {
            expect(block.findBlockInside('more', 'link')).not.to.be.null;
        });

        it('Кнопка подгрузки новостей задизейблена', function() {
            expect(block.findBlockInside('more', 'link')).to.haveMod('disabled', 'yes')
        });
    });

    describe('Создание нового сниппета', function() {

        beforeEach(function() {
            modelsList = BEM.MODEL.create('m-infoblock-state', {
                news: modelData,
                teasers: [],
                last_opened: 1,
                show_news: true,
                show_block: true,
                show_teasers: true,
                expose: false,
                error: null
            }).get('news');

            block.repaint(modelsList);

            sandbox.stub(block, '_fetchData').callsFake(function() {
                var deferred = $.Deferred();

                return deferred.resolve({
                   "data" : {
                      "date" : "14 февраля 2017 г",
                      "content" : [
                         {
                            "text" : ""
                         },
                         {
                            "text" : "Директ запускает минус-фразы",
                            "link_url" : "http://yandex.ru/support/direct-news/n-2017-02-14.xml"
                         },
                         {
                            "text" : ""
                         }
                      ]
                   },
                   "ext_news_id" : "d-2017-02-14",
                   "region" : "ru",
                   "lang" : "ru"
                });
            });
        });

        it('Пришедшая с сервера ссылка появляется в инфоблоке', function() {
            block.findBlockInside('more', 'link').trigger('click');

            sandbox.clock.tick(1);

            var snippets = block.findElem('snippet'),
                hasSnippet = 0;

            snippets.each(function(index, snippet) {
                var snippetText = $.trim($(snippet).find('.link__inner').text());

                snippetText ==  "Директ запускает минус-фразы" && hasSnippet ++;
            });

            expect(hasSnippet).to.equal(1);
        });

        it('Пришедшая с сервера дата появляется в инфоблоке', function() {
            block.findBlockInside('more', 'link').trigger('click');

            sandbox.clock.tick(1);

            var dates = block.findElem('date'),
                hasDate = 0;

            dates.each(function(index, dt) {
                var dateText = $(dt).text();

                dateText ==  "14 февраля 2017" && hasDate ++;
            });

            expect(hasDate).to.equal(1);
        });


    });

    describe('Клики по ссылкам', function() {
        beforeEach(function() {
            modelsList = BEM.MODEL.create(
                'm-infoblock-state', {
                    news: modelData,
                    teasers: [],
                    last_opened: 1,
                    show_news: true,
                    show_block: true,
                    show_teasers: true,
                    expose: false,
                    error: null
                }).get('news');

            block.repaint(modelsList);
        });

        it('При клике по ссылке соответствуюшая модель получает read: true', function() {
            var unreadModel = modelsList.getByIndex(0),
                firstSnippet = block.findBlocksInside('link', 'link')[0];

            expect(unreadModel.get('read')).to.be.false;
            firstSnippet.trigger('click');

            expect(unreadModel.get('read')).to.be.true;
        });

        it('При клике по ссылке ссылка открывается', function() {
            var unreadModel = modelsList.getByIndex(0),
                firstSnippet = block.findBlocksInside('link', 'link')[0];

            sandbox.spy(window, 'open');

            firstSnippet.trigger('click');

            expect(window.open.calledWith('https://yandex.ru/support/direct-news/n-2017-04-04.html')).to.be.true;
        })
    })
});
