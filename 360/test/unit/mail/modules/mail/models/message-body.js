/*global mock*/
describe('Daria.mMessageBody', function() {
    var loaderImgSrc = '/client/build/_/2966ad46f182d8d22e58f5f1c200484b-b-mail-icon_ajax-loader.gif';

    function htmlToNode(html) {
        return $(html)[0];
    }

    function compareNodes(resultNode, expectNode) {
        Array.from(resultNode.querySelectorAll('.b-mail-icon_ajax-loader')).forEach(function(node) {
            node.setAttribute('src', loaderImgSrc);
        });

        var checkResult = (resultNode).isEqualNode(expectNode);
        if (!checkResult) {
            console.group(this.test.title);
            console.log('resultNode', resultNode.outerHTML);
            console.log('expectNode', expectNode.outerHTML);
            console.groupEnd(this.test.title);
        }
        expect(checkResult).to.be.ok;
    }

    beforeEach(function() {
        ns.router.init();

        this.quoteControlHtml = '' +
            '<div class="b-quote__ctrls js-quote-ignore">' +
            '<span class="b-quote__button js-quote-expand" data-ids="{ids}">' +
            '<span class="b-quote__button-i">' + i18n('%Message_Quote_Toggle2') + '</span>' +
            '</span>' +
            '<span class="b-quote__button js-quote-expand-all" data-ids="{ids}">' +
            '<span class="b-quote__button-i">' + i18n('%Message_Quote_expand_all') + '</span>' +
            '</span>' +
            '<span class="b-quote__button b-quote__button_maximize_begin js-quote-maximize">' +
            '<span class="b-quote__button-i">' + i18n('%Message_Quote_Показать_начало_цитаты') + '</span>' +
            '</span>' +
            '<span class="b-quote__button b-quote__button_maximize_part js-quote-maximize">' +
            '<span class="b-quote__button-i">' + i18n('%Message_Quote_Показать_часть_цитаты') + '</span>' +
            '</span>' +
            '<span class="b-quote__button b-quote__button_maximize_end js-quote-maximize">' +
            '<span class="b-quote__button-i">' + i18n('%Message_Quote_Показать_конец_цитаты') + '</span>' +
            '</span>' +
            '<span class="b-quote__loading">' +
            '<img class="b-mail-icon b-mail-icon_ajax-loader" src="' + loaderImgSrc + '">' +
            '</span>' +
            '</div>';

        this.hAccountInformation = ns.Model.get('account-information');
        setModelByMock(this.hAccountInformation);

        this.hSettings = ns.Model.get('settings');
        setModelByMock(this.hSettings);

        this.hFolders = ns.Model.get('folders');
        setModelByMock(this.hFolders);

        this.hLabels = ns.Model.get('labels');
        setModelByMock(this.hLabels);

        setModelsByMock('message');
        setModelsByMock('message-body');

        this.handler = this.model = ns.Model.get('message-body');
        this.sinon.spy(this.handler, '_processBody');
        this.sinon.spy(Jane, 'tt');
        this.getAttachmentsStub = this.sinon.stub(this.handler, 'getAttachments').returns([]);

        this.messageBodyStaticMethods = ns.Model.infoLite('message-body');
    });

    afterEach(function() {
        ns.Model.traverse('account-information', ns.Model.destroy);
        ns.Model.traverse('folders', ns.Model.destroy);
        ns.Model.traverse('labels', ns.Model.destroy);
        ns.Model.traverse('message', ns.Model.destroy);
        ns.Model.traverse('message-body', ns.Model.destroy);
        ns.Model.traverse('settings', ns.Model.destroy);
    });

    describe('static #calculateParamsForMessageBody', function() {
        it('Если в параметрах нет ids, то должен вернуть null', function() {
            expect(this.messageBodyStaticMethods.calculateParamsForMessageBody({})).to.be.equal(null);
        });

        it('Если в параметрах ids - массив из > 1 элемента и операция пересылки, то должен вернуть null', function() {
            expect(this.messageBodyStaticMethods.calculateParamsForMessageBody({
                ids: [ '1', '2', '3' ], oper: 'forward'
            })).to.be.equal(null);
        });

        it('Если в параметрах есть ids и это строка или 1 элемент массива, то должен вернуть параметры', function() {
            expect(this.messageBodyStaticMethods.calculateParamsForMessageBody({ ids: 1, oper: 'forward' }))
                .to.be.eql({ ids: 1, oper: 'forward', draft: false });
        });

        it('Если есть ids и нет операции, то должен вернуть параметры и метку, что это черновик', function() {
            expect(this.messageBodyStaticMethods.calculateParamsForMessageBody({ ids: 1 }))
                .to.be.eql({ ids: 1, draft: true });
        });
    });

    describe('getSubtype', function() {
        it('Возвращает тип первого тела письма', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: '15' });
            expect(mMessageBody.getSubtype()).to.be.equal('html');
        });
    });

    describe('getDraftHTML', function() {
        it('Возвращает content первого тела письма', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: '16' });
            expect(mMessageBody.getDraftBody()).to.be.equal('plain body1');
        });

        it('Возвращает content указанного типа (html)', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: '16' });
            expect(mMessageBody.getDraftBody('html')).to.be.equal('html body2');
        });

        it('Возвращает content указанного типа (plain)', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: '17' });
            expect(mMessageBody.getDraftBody('plain')).to.be.equal('plain body2');
        });

        it('Возвращает пустую строку, если нет тела', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: '1.2' });
            expect(mMessageBody.getDraftBody()).to.be.equal('');
        });
    });

    describe('._getParamsForModels', function() {
        beforeEach(function() {
            this.method = ns.Model.info('message-body').calculateParamsForMessageBody;
        });

        it('Возвращает null, если в параметрах нет ids', function() {
            expect(this.method({})).to.be.equal(null);
        });

        it('Возвращает параметры для запроса черновика, если есть ids, но нет oper', function() {
            expect(this.method({ ids: '111' })).to.be.eql({ ids: '111', draft: true });
        });
    });

    describe('getHTML', function() {
        describe('созданная и обработанная нода кэшируется', function() {
            beforeEach(function() {
                this.mMessageBody = ns.Model.get('message-body', { ids: '5' });
                this.sinon.spy(this.mMessageBody, '_processBody');
            });

            it('изначально тело письма не проходит процессинга', function() {
                expect(this.mMessageBody._processBody).to.have.callCount(0);
            });

            it('во время первого вызова getHTML() создаётся нода тела письма', function() {
                this.mMessageBody.getHTML();
                expect(this.mMessageBody._processBody).to.have.callCount(1);
            });

            describe('повторный вызов возвращает клон закэшированной ноды ->', function() {
                it('ноды эквивалентны', function() {
                    var node1 = this.mMessageBody.getHTML();
                    var node2 = this.mMessageBody.getHTML();
                    compareNodes(node1, node2);
                });

                it('это разные ноды', function() {
                    var node1 = this.mMessageBody.getHTML();
                    var node2 = this.mMessageBody.getHTML();
                    expect(node1 === node2).to.be.equal(false);
                });

                it('процессинг выполняется только один раз', function() {
                    this.mMessageBody.getHTML();
                    this.mMessageBody.getHTML();
                    expect(this.mMessageBody._processBody).to.have.callCount(1);
                });
            });

            it('закэшированная нода сбрасывается после вызова setData()', function() {
                this.mMessageBody.getHTML();
                this.mMessageBody.getHTML();
                expect(this.mMessageBody._processBody).to.have.callCount(1);

                var newData = _.extend({}, this.mMessageBody.getData());
                newData.rnd = Date.now();
                this.mMessageBody.setData(newData);

                // Данные уже должны были сброситься, но нода пересчитается после запроса getHTML().
                expect(this.mMessageBody._processBody).to.have.callCount(1);

                this.mMessageBody.getHTML();
                expect(this.mMessageBody._processBody).to.have.callCount(2);
            });

            describe('ноды message-body кэшируются в зависимости от params ->', function() {
                it('для разных параметров кэшируеются разные ноды', function() {
                    this.mMessageBody.getHTML({ a: 1 });
                    this.mMessageBody.getHTML({ a: 2 });
                    expect(this.mMessageBody._processBody).to.have.callCount(2);
                });

                it('после setData() сбрасываются все закэшированные ноды', function() {
                    this.mMessageBody.getHTML({ a: 1 });
                    this.mMessageBody.getHTML({ a: 2 });

                    var newData = _.extend({}, this.mMessageBody.getData());
                    newData.rnd = Date.now();
                    this.mMessageBody.setData(newData);

                    this.mMessageBody.getHTML({ a: 1 });
                    this.mMessageBody.getHTML({ a: 2 });

                    expect(this.mMessageBody._processBody).to.have.callCount(4);
                });
            });

            describe('закэшированные ноды удаляются по таймауту (защита от утечек памяти) ->', function() {
                beforeEach(function() {
                    this.clock = this.sinon.useFakeTimers();
                    this.cacheLifetime = 5 * 60000;
                    this.sinon.spy(this.mMessageBody, '_destroyNodesCache');
                    this.sinon.spy(this.mMessageBody, '_cancelDestroyNodesCacheTimer');
                    this.sinon.spy(this.mMessageBody, '_resetDestroyNodesCacheTimer');
                });

                afterEach(function() {
                    this.clock.restore();
                });

                describe('после генерации DOM ноды в getHTML() запускается таймер на сброс кэша нод ->', function() {
                    it('вначале кэша нет', function() {
                        expect(this.mMessageBody._bodyNodesCache).to.be.eql({});
                    });

                    it('после запроса getHTML() - в кэше появляется нода', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        expect(this.mMessageBody._bodyNodesCache).to.have.property('a=1&ids=5');
                    });

                    it('по прошествии времени < cacheLifetime нода всё ещё в кэше', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime - 1);

                        expect(this.mMessageBody._bodyNodesCache).to.have.property('a=1&ids=5');
                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(0);
                    });

                    it('после прошествия времени >= cacheLifetime - кэш нод должен быть сброшен', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime + 1);

                        expect(this.mMessageBody._bodyNodesCache).to.be.eql({});
                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(1);
                    });
                });

                describe('setData() сбрасывает кэш и отменяет таймер на сброс кэша нод ->', function() {
                    it('вначале кэша нет', function() {
                        expect(this.mMessageBody._bodyNodesCache).to.be.eql({});
                    });

                    it('после запроса getHTML() - в кэше появляется нода', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        expect(this.mMessageBody._bodyNodesCache).to.have.property('a=1&ids=5');
                    });

                    it('по прошествии времени < cacheLifetime нода всё ещё в кэше', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime - 1);

                        expect(this.mMessageBody._bodyNodesCache).to.have.property('a=1&ids=5');
                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(0);
                    });

                    it('вызов setData() приводит к немедленно сбросу кэша', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime - 1);

                        var newData = _.extend({}, this.mMessageBody.getData());
                        newData.rnd = Date.now();
                        this.mMessageBody.setData(newData);

                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(1);
                    });

                    it('вызов setData() приводит к отмене отложенного сброса кэша ' +
                        '(потому что мы его уже сбросили)', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime - 1);

                        var newData = _.extend({}, this.mMessageBody.getData());
                        newData.rnd = Date.now();
                        this.mMessageBody.setData(newData);

                        // Отменяем отложенный сброс кэша нод.
                        // 2 потому что первый вызов на getHTML(), а второй на setData().
                        expect(this.mMessageBody._cancelDestroyNodesCacheTimer).to.have.callCount(2);

                        // Про прошествии таймаута - отложенный сброс кэша нод не выполняется.
                        this.clock.tick(this.cacheLifetime + 1);
                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(1);
                    });
                });

                describe('обращение к getHTML() заново устанавливает таймер на сброс кэша нод', function() {
                    it('после вызова метода getHTML() - отложенное удаление нод устанавливается по новой', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime - 1);

                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(2);

                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(0);
                        expect(this.mMessageBody._resetDestroyNodesCacheTimer).to.have.callCount(2);
                    });

                    it('после вызова метода getHTML() - удаление должно произойти через cacheLifetime ms', function() {
                        this.mMessageBody.getHTML({ a: 1 });
                        this.clock.tick(this.cacheLifetime - 1);
                        this.mMessageBody.getHTML({ a: 1 });

                        this.clock.tick(this.cacheLifetime);

                        expect(this.mMessageBody._destroyNodesCache).to.have.callCount(1);
                    });
                });
            });
        });

        describe('common', function() {
            it('если письма нет, то возвращает специальный html', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '124333' });
                this.sinon.spy(mMessageBody, '_processBody');
                mMessageBody.getHTML();

                expect(mMessageBody._processBody.called).to.be.equal(false);
                expect(Jane.tt.calledWith('message:message-body-error')).to.be.equal(true);
            });

            it('для письма сборщиков возвращаем специальный html', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '1' });
                this.sinon.spy(mMessageBody, '_processBody');
                mMessageBody.getHTML({
                    ids: 1
                });

                expect(mMessageBody._processBody.called).to.be.equal(false);
                expect(Jane.tt.calledWith('message:message-body-collector-msg')).to.be.equal(true);
            });

            it('правильно показываем письмо без начальных тегов', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '1.1' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    'some text here<div>some div here</div>' +
                    '</div>'
                );

                compareNodes.call(this, body, expectNode);
            });

            it('правильно обрабатываем письмо без тела', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '1.2' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '</div>'
                );

                compareNodes.call(this, body, expectNode);
            });
        });

        describe('transformers', function() {
            it('в <a href="mailto:"/> вставляется на action', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '4' });

                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><a href="mailto:doochik@ya.ru" class="ns-action" data-click-action="common.go" data-params="new_window&amp;url=%23compose%2Fmailto%3Ddoochik%2540ya.ru">link</a></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('если письмо в спаме, то делаем ссылки неактивными', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '5' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><a h_href="http://download.com/virus.exe" class="ya-hidden-link">get fresh porno</a></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            describe('forceShowImagesAndHrefs', function() {
                it('не должен отключать картинки если не активирован режим принудительного показа картинок', function() {
                    var mMessageBody = ns.Model.get('message-body', { ids: '20' });
                    var body = mMessageBody.getHTML({
                        forceShowImages: true
                    });

                    var expectNode = htmlToNode([
                        '<div class="js-message-body-content mail-Message-Body-Content">',
                        '<img src="/client/build/images/foo.png">',
                        '</div>'
                    ].join(''));

                    compareNodes.call(this, body, expectNode);
                });

                it('не должен отключать ссылки если не активирован режим принудительного показа ссылок', function() {
                    var mMessageBody = ns.Model.get('message-body', { ids: '21' });
                    var body = mMessageBody.getHTML({
                        forceShowHrefs: true
                    });

                    var expectNode = htmlToNode([
                        '<div class="js-message-body-content mail-Message-Body-Content">',
                        '<a href="http://ya.ru" class="daria-goto-anchor" target="_blank" rel="noopener noreferrer">',
                        '</a>',
                        '</div>'
                    ].join(''));

                    compareNodes.call(this, body, expectNode);
                });
            });

            it('если письмо в спаме, то делаем ссылки не активными, если это не письмо от саппортов', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '6' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><a href="http://download.com/virus.exe" class="daria-goto-anchor" target="_blank" rel="noopener noreferrer">get fresh porno</a></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('если письмо в спаме, то скрываем картинки', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '61' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<img h_src="/client/build/fake.gif" h_width="1" h_height="2" src="/client/build/_/325472601571f31e1bf00674c368d335-b-ico.gif" class="ya-hidden-image"/>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('правильно обрабатываем <img/> с переносами строк', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '62' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<img src="/client/build/fake.gif" width="1" height="2"/>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('правильно обрабатываем <img/> без src', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '63' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<img width="1" height="2"/>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('Если в src картинки есть перенос, то ничего не должно ломаться', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '18' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode(
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<img alt="" border="0" src="/client/build/wow/static/i/yand-add-b.png\n   ">' +
                    '</div>');

                compareNodes.call(this, body, expectNode);
            });

            it('к нормальным письмам в ссылки добавлям класс и target', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '7' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><a href="http://yandex.ru" class="daria-goto-anchor" target="_blank" rel="noopener noreferrer">Yandex</a></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('к object добавляем <param name="wmode" value="opaque"/>', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '8' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><object><param name="wmode" value="opaque"/></object></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('к embed добавляем атрибут wmode="opaque"', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '9' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><embed wmode="opaque"/></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('к form добавляем класс и target="_blank", вырезаем formaction', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '10' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<div><form action="" class=" js-message-form" target="_blank" rel="noopener noreferrer"/></div>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            it('раскрываем wmi-video-link', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '11' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                    '<a href="videolink" target="_blank" rel="noopener noreferrer" class="ns-action" data-click-action="message.video-player-open" data-params="a=1&amp;b=2">' +
                    '<img src="/client/build/_/7a4dd71dafbb1404bfd494dae3fe02fd-b-mail-icon_video-link.gif" class="b-mail-icon b-mail-icon_video-link" alt="" title="' + i18n('%Message_Video_Link') + '">' +
                    '</a>' +
                    '<a href="videolink" target="_blank" rel="noopener noreferrer">videolink</a>' +
                    '</div>'
                );
                compareNodes.call(this, body, expectNode);
            });

            /*
            it('Должен обернуть слова типа "аттач" и т.п.', function() {
                this.getAttachmentsStub.returns([1, 2]);

                var body = this.handler.getHTML({
                    ids: 22
                });

                var expectNode = htmlToNode('' +
                    '<div class="b-message-body b-message-body_plain-text">' +
                    '<div class="b-message-body__content" data-lang="0">' +
                    '<p>' +
                    '<span class="b-pseudo-link b-pseudo-link_attach js-attachments-scroll">attach</span> file ' +
                    '"<span class="b-pseudo-link b-pseudo-link_attach js-attachments-scroll">приложил</span>",' +
                    '</p>' +
                    '</div>' +
                    '</div>'
                );

                compareNodes.call(this, body, expectNode);
            });

            it('Не должен оборачивать слова типа "аттач", если совпадение не 100%', function() {
                this.getAttachmentsStub.returns([1, 2]);

                var body = this.handler.getHTML({
                    ids: 23
                });

                var expectNode = htmlToNode('' +
                    '<div class="b-message-body b-message-body_plain-text">' +
                    '<div class="b-message-body__content" data-lang="0">' +
                    '<p>Зааттачил файлик</p>' +
                    '</div>' +
                    '</div>'
                );

                compareNodes.call(this, body, expectNode);
            });
            */
        });

        describe('цитаты', function() {
            beforeEach(function() {
                this.compareNodesWithSvg = function(resultNode, expectNode) {
                    var checkResult = resultNode.outerHTML === expectNode.outerHTML;
                    if (!checkResult) {
                        console.group(this.test.title);
                        console.log('resultNode', resultNode);
                        console.log('expectNode', expectNode);
                        console.log('\n' + resultNode.innerHTML);
                        console.log('\n' + expectNode.innerHTML);
                        console.groupEnd(this.test.title);
                    }
                    expect(checkResult).to.be.ok;
                };
            });

            it('цитата', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '12' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                            '<blockquote type="cite" data-level="1" ' +
                                    'class="b-quote b-quote_lt b-quote_odd b-quote_lf b-quote_ll b-quote__cb_sep b-quote__ct_sep b-quote_expanded" ' +
                                    'data-processed="bc sa aps lf ll qw qws">' +
                                '<div class="b-quote_content js-quote_content is-hidden">' +
                                    '<div class="b-quote__firstline js-quote-ignore js-quote-expand">' +
                                        '<div class="b-quote__firstline_content">quote</div>' +
                                    '</div>' +
                                    this.quoteControlHtml.replace(/{ids}/g, '12') +
                                    '<div class="b-quote__i js-quote-content">quote</div>' +
                                '</div>' +
                                '<div class="mail-Quote-Toggler js-mail-Quote-Toggler">' +
                                    '<svg xmlns="http://www.w3.org/2000/svg" class="svgicon svgicon-mail--Message-Quotation">' +
                                        '<use xlink:href="#svgicon-mail--Message-Quotation"></use>' +
                                        '<rect height="100%" width="100%" style="fill: transparent;"></rect>' +
                                    '</svg>' +
                                    i18n('%Message-Quote-Toggle') +
                                '</div>' +
                            '</blockquote>' +
                    '</div>'
                );

                this.compareNodesWithSvg(body, expectNode);
            });

            it('из тега blockquote врезаются стили', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '12.1' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                            '<blockquote type="cite" data-level="1" ' +
                                    'class="b-quote b-quote_lt b-quote_odd b-quote_lf b-quote_ll b-quote__cb_sep b-quote__ct_sep b-quote_expanded" ' +
                                    'data-processed="bc sa aps lf ll qw qws">' +
                                '<div class="b-quote_content js-quote_content is-hidden">' +
                                    '<div class="b-quote__firstline js-quote-ignore js-quote-expand">' +
                                        '<div class="b-quote__firstline_content">quote</div>' +
                                    '</div>' +
                                    this.quoteControlHtml.replace(/{ids}/g, '12.1') +
                                    '<div class="b-quote__i js-quote-content">quote</div>' +
                                '</div>' +
                                '<div class="mail-Quote-Toggler js-mail-Quote-Toggler">' +
                                    '<svg xmlns="http://www.w3.org/2000/svg" class="svgicon svgicon-mail--Message-Quotation">' +
                                        '<use xlink:href="#svgicon-mail--Message-Quotation"></use>' +
                                        '<rect height="100%" width="100%" style="fill: transparent;"></rect>' +
                                    '</svg>' +
                                    i18n('%Message-Quote-Toggle') +
                                '</div>' +
                            '</blockquote>' +
                    '</div>'
                );

                this.compareNodesWithSvg(body, expectNode);
            });

            it('вложенные цитаты не обрабатываются', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '13' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                            '<blockquote type="cite" data-level="1" ' +
                                    'class="b-quote b-quote_lt b-quote_odd b-quote_lf b-quote_ll b-quote__ct_sep" data-processed="bc sa aps lf ll qw qws">' +
                                '<div class="b-quote_content js-quote_content is-hidden">' +
                                    '<div class="b-quote__firstline js-quote-ignore js-quote-expand">' +
                                        '<div class="b-quote__firstline_content">quote1</div>' +
                                    '</div>' +
                                    this.quoteControlHtml.replace(/{ids}/g, '13') +
                                    '<div class="b-quote__i js-quote-content">' +
                                    'quote1 ' +
                                    '<blockquote type="cite" data-level="2" class="b-quote b-quote_even b-quote_lf b-quote_ll" data-processed="lf ll">quote2</blockquote>' +
                                    '</div>' +
                                '</div>' +
                                '<div class="mail-Quote-Toggler js-mail-Quote-Toggler">' +
                                    '<svg xmlns="http://www.w3.org/2000/svg" class="svgicon svgicon-mail--Message-Quotation">' +
                                        '<use xlink:href="#svgicon-mail--Message-Quotation"></use>' +
                                        '<rect height="100%" width="100%" style="fill: transparent;"></rect>' +
                                    '</svg>' +
                                    i18n('%Message-Quote-Toggle') +
                                '</div>' +
                            '</blockquote>' +
                    '</div>'
                );

                this.compareNodesWithSvg(body, expectNode);
            });

            it('вложенные цитаты обрабатываются, если есть автораскрытие', function() {
                var mMessageBody = ns.Model.get('message-body', { ids: '13.1' });
                var body = mMessageBody.getHTML();
                var expectNode = htmlToNode('' +
                    '<div class="js-message-body-content mail-Message-Body-Content">' +
                            '<blockquote type="cite" data-level="1" ' +
                                    'class="b-quote b-quote_lt b-quote_odd b-quote_lf b-quote_ll b-quote__ct_sep" data-processed="bc sa aps lf ll qw qws">' +
                                '<div class="b-quote_content js-quote_content is-hidden">' +
                                    '<div class="b-quote__firstline js-quote-ignore js-quote-expand">' +
                                        '<div class="b-quote__firstline_content">quote1</div>' +
                                    '</div>' +
                                    this.quoteControlHtml.replace(/{ids}/g, '13.1') +
                                    '<div class="b-quote__i js-quote-content">' +
                                    'quote1 ' +
                                    '<blockquote type="cite" data-level="2" class="b-quote b-quote_even b-quote_lf b-quote_ll" data-processed="lf ll">quote2</blockquote>' +
                                    '</div>' +
                                '</div>' +
                                '<div class="mail-Quote-Toggler js-mail-Quote-Toggler">' +
                                    '<svg xmlns="http://www.w3.org/2000/svg" class="svgicon svgicon-mail--Message-Quotation">' +
                                        '<use xlink:href="#svgicon-mail--Message-Quotation"></use>' +
                                        '<rect height="100%" width="100%" style="fill: transparent;"></rect>' +
                                    '</svg>' +
                                    i18n('%Message-Quote-Toggle') +
                                '</div>' +
                            '</blockquote>' +
                    '</div>'
                );

                this.compareNodesWithSvg(body, expectNode);
            });
        });
    });

    describe('getRecipients', function() {
        it('should return "reply-to" in <to>', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r1' });
            expect(mMessageBody.getRecipients()).to.eql({
                to: '<test1@ya.ru>',
                cc: ''
            });
        });

        it('should return Array in <to>', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r1' });
            expect(mMessageBody.getRecipients(false, true)).to.eql({
                to: [
                    {
                        type: 'reply-to',
                        email: 'test1@ya.ru'
                    }
                ],
                cc: []
            });
        });

        it('should return "from" in <to> when "reply-to" doesn\'t exist', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r2' });
            expect(mMessageBody.getRecipients()).to.eql({
                to: '<test2@ya.ru>',
                cc: ''
            });
        });

        it('should return "from" in <to> when "reply-to" filtered', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r3' });
            expect(mMessageBody.getRecipients()).to.eql({
                to: '<test3@ya.ru>',
                cc: ''
            });
        });

        it('should return "to" in <to> when "reply-to" and "from" filtered (message from me)', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r4' });
            expect(mMessageBody.getRecipients()).to.eql({
                to: '<test4@ya.ru>',
                cc: ''
            });
        });

        it('should return "to" + "cc" in <cc> without my email when replyAll passed', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r5' });
            expect(mMessageBody.getRecipients(true)).to.eql({
                to: '<test5@ya.ru>, <test6@ya.ru>, <test7@ya.ru>',
                cc: '<test8@ya.ru>, <test9@ya.ru>'
            });
        });

        it('should return "to" + "cc" in <cc> without my email when replyAll passed and message from me', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r6' });
            expect(mMessageBody.getRecipients(true)).to.eql({
                to: '<test6@ya.ru>, <test7@ya.ru>',
                cc: '<test8@ya.ru>, <test9@ya.ru>'
            });
        });

        it('should return "from" in <to> when "reply-to" and "from" and "to" filtered (message from me to me)', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r7' });
            expect(mMessageBody.getRecipients()).to.eql({
                to: '<my2@ya.ru>',
                cc: ''
            });
        });

        it('should filter out emails with dots and dashes', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r8' });
            expect(mMessageBody.getRecipients(true)).to.eql({
                to: '<some@ya.ru>',
                cc: ''
            });
        });

        it('should work correct with upper-lower case', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r9' });
            expect(mMessageBody.getRecipients(true)).to.eql({
                to: '<some@ya.ru>',
                cc: ''
            });
        });
    });

    describe('#allowedReplyAll', function() {
        it('должен вернуть false, если один адресат', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r9' });
            expect(mMessageBody.allowedReplyAll()).to.eql(false);
        });

        it('должен вернуть true, если несколько адресатов', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'r6' });
            expect(mMessageBody.allowedReplyAll()).to.eql(true);
        });

        it('должен вернуть false, если адресат - цифровой алиас', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'withCA' });
            expect(mMessageBody.allowedReplyAll()).to.eql(false);
        });

        it('должен вернуть true, если есть несколько адресатов и поле TO - цифровой алиас', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'withCAandCC' });
            expect(mMessageBody.allowedReplyAll()).to.eql(true);
        });
    });

    describe('#getCalendarICS', function() {
        it('должен вернуть undefined, если нет ics', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'eticket1' });
            setModelByMock(mMessageBody);

            expect(mMessageBody.getCalendarICS()).to.be.equal(undefined);
        });

        it('должен вернуть hid, если есть ics', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'calendar1' });
            setModelByMock(mMessageBody);

            expect(mMessageBody.getCalendarICS()).to.be.equal('1.1.3');
        });

        it('должен вернуть один hid, если есть два ics', function() {
            var mMessageBody = ns.Model.get('message-body', { ids: 'calendar2' });
            setModelByMock(mMessageBody);

            expect(mMessageBody.getCalendarICS()).to.be.equal('1.1.3');
        });
    });

    describe('parseDate', function() {
        beforeEach(function() {
            this.hMessageBody = ns.Model.get('message-body');
        });

        it('Должен привести дату из формата (d+)-(d+)-(d+) (d+):(d+):(d+) - к timestamp', function() {
            expect(typeof this.hMessageBody.parseDate('2013-09-06 00:00:00')).to.not.be.equal(null);
        });
    });

    describe('getEmailInfoParams', function() {
        beforeEach(function() {
            this.mMessageBody = ns.Model.get('message-body');
            this.selectFromStub = this.sinon.stub(this.mMessageBody, 'select').withArgs('.info.field[.type=="from"]');
            this.checkEmailStub = this.sinon.stub(Jane.FormValidation, 'checkEmail');
        });

        it('должен вернуть null, если поля from нет', function() {
            this.selectFromStub.returns([]);
            var emailInfoParams = this.mMessageBody.getEmailInfoParams();
            expect(emailInfoParams).to.be.equal(null);
        });

        it('должен вернуть null, если у поля from не задан email', function() {
            this.selectFromStub.returns([]);
            var emailInfoParams = this.mMessageBody.getEmailInfoParams();
            expect(emailInfoParams).to.be.equal(null);
        });

        it('должен вернуть null, если email невалидный', function() {
            this.selectFromStub.returns([ { email: 'mail@mail' } ]);
            this.checkEmailStub.returns(false);
            var emailInfoParams = this.mMessageBody.getEmailInfoParams();
            expect(emailInfoParams).to.be.equal(null);
        });

        it('должен вернуть объект с ref и email отправителя, если email валидный', function() {
            var fieldFrom = { ref: 'reference', email: 'mail@ya.ru' };
            this.selectFromStub.returns([ fieldFrom ]);
            this.checkEmailStub.returns(true);
            var emailInfoParams = this.mMessageBody.getEmailInfoParams();
            expect(emailInfoParams).to.be.eql(fieldFrom);
        });
    });

    describe('getLocations', function() {
        beforeEach(function() {
            this.addressFact1 = {
                weight: 42.66666667,
                geo_addr: 'ул Льва Толстого 17',
                geocoder_request: 'улица льва толстого 17'
            };

            this.addressFact2 = {
                weight: 32,
                geo_addr: 'Льва Толстого, 18',
                geocoder_request: 'льва толстого 18'
            };

            this.handlerData = {
                facts: {
                    addr: [
                        this.addressFact1,
                        this.addressFact2
                    ]
                }
            };

            this.handler = ns.Model.get('message-body', { ids: 'locations-test' });
            this.handler.setData(this.handlerData);
        });

        it('должен возвращать обычные полные адреса', function() {
            expect(this.handler.getLocations()).to.eql([ this.addressFact1 ]);
        });

        it('должен возвращать все подходящие адреса', function() {
            this.handlerData.facts.addr = [
                _.clone(this.addressFact1, true),
                _.clone(this.addressFact1, true),
                _.clone(this.addressFac2, true)
            ];

            expect(this.handler.getLocations()).to.eql([
                this.addressFact1,
                this.addressFact1
            ]);
        });

        it('не должен возвращать адрес, если у него вес < 40', function() {
            this.addressFact1.weight = 39;
            expect(this.handler.getLocations()).to.be.eql([]);
        });

        it('должен возвращать адрес, если у него вес >= 40', function() {
            this.addressFact1.weight = 40;
            expect(this.handler.getLocations()).to.be.eql([ this.addressFact1 ]);
        });

        it('не должен возвращать адрес, если у него не задан полный адрес', function() {
            this.addressFact1.geo_addr = null;
            expect(this.handler.getLocations()).to.be.eql([]);
        });
    });

    describe('#isRecipient ->', function() {
        it('Должен возвращать true, если я есть в получателях', function() {
            this.sinon.stub(this.hAccountInformation, 'getAllUserEmails').returns([
                'ekhurtina@yandex-team.ru',
                'ekhurtina@yandex-team.com',
                'ekhurtina@yandex-team.com.tr',
                'ekhurtina@yandex-team.com.ua'
            ]);

            var getFieldsByType = this.sinon.stub(this.handler, 'getFieldsByType');
            getFieldsByType.withArgs('to').returns([
                { email: 'ekhurtina@yandex-team.ru' },
                { email: 'mail-test@yandex-team.ru' }
            ]);
            getFieldsByType.withArgs('cc').returns([
                { email: 'test@yandex.ru' }
            ]);
            getFieldsByType.withArgs('bcc').returns([]);
            expect(this.handler.isRecipient()).to.be.ok;
        });
        it('Должен возвращать false, если меня нет в получателях', function() {
            var accountInformation = ns.Model.get('account-information');
            this.sinon.stub(accountInformation, 'getAllUserEmails').returns([
                'ekhurtina@yandex-team.ru',
                'ekhurtina@yandex-team.com',
                'ekhurtina@yandex-team.com.tr',
                'ekhurtina@yandex-team.com.ua'
            ]);

            var getFieldsByType = this.sinon.stub(this.handler, 'getFieldsByType');
            getFieldsByType.withArgs('to').returns([
                { email: 'test1@ya.ru' },
                { email: 'mail-test@yandex-team.ru' }
            ]);
            getFieldsByType.withArgs('cc').returns([
                { email: 'test@yandex.ru' }
            ]);
            getFieldsByType.withArgs('bcc').returns([]);
            expect(this.handler.isRecipient()).to.not.be.ok;
        });
    });

    describe('#canRequest ->', function() {
        it('модель НЕ перезапрашивается бесконечно, если для неё постоянно приходит ошибка (DARIA-59660)', function() {
            var model = ns.Model.get('message-body', { ids: '322' });
            this.sinon.spy(model, 'canRequest');

            ns.request.models.restore();

            this.sinon.stub(ns, 'http')
                .withArgs(Daria.api.models + '?_m=message-body')
                .returns(Vow.resolve({
                    models: [ {
                        error: 'test error'
                    } ]
                }));

            return ns.request.models([ model ]).fail(function() {
                expect(model.canRequest).to.have.callCount(model.RETRY_LIMIT + 1);
            }, this);
        });
    });

    describe('#filterRecipients', function() {
        beforeEach(function() {
            this.mMessageBody = ns.Model.get('message-body', { ids: 'r1' });
            this.sinon.stub(this.mMessageBody, 'getFieldsByType').withArgs('to')
                .returns([
                    {
                        type: 'to',
                        email: 'che.test.man@narod.ru',
                        name: 'che.test.man@narod.ru'
                    },
                    {
                        type: 'to',
                        email: 'che.test.man@яндекс.рф',
                        name: 'che.test.man@яндекс.рф'
                    },
                    {
                        type: 'to',
                        email: 'chestozo@gmail.com',
                        name: 'Роман Карцев'
                    }
                ]);

            this.sinon.stub(Daria.Recipients, 'getRecipientFromCache')
                .withArgs('che.test.man@narod.ru', 'che.test.man@narod.ru').returns({ isSelf: true })
                .withArgs('che.test.man@яндекс.рф', 'che.test.man@яндекс.рф').returns({ isSelf: true })
                .withArgs('Роман Карцев', 'chestozo@gmail.com').returns({ isSelf: false });
        });

        it('исключает имейлы текущего пользователя', function() {
            expect(this.mMessageBody.filterRecipients('to')).to.be.eql([
                {
                    type: 'to',
                    email: 'chestozo@gmail.com',
                    name: 'Роман Карцев'
                }
            ]);
        });
    });

    describe('#getForwardBody', function() {
        beforeEach(function() {
            this.body = '<div>FAKE MESSAGE BODY</div>';
            this.bodyText = 'FAKE MESSAGE BODY';
            this.sinon.stub(this.model, 'params').value({ ids: '123' });
            this.sinon.stub(this.model, 'getComposeHTML').returns(this.body);

            this.mMessage = ns.Model.get('message', { ids: '123' });
            this.sinon.stub(this.mMessage, 'getDateTime').returns('FAKE_MESSAGE_DATE');
            this.sinon.stub(this.mMessage, 'getAddressField').withArgs('from').returns('\"Full Name\" <login@host.ru>');

            this.sinon.stub(Daria.Translate, 'getLangByMid').withArgs('123').returns('ru');
            this.sinon.stub(Daria.Translate, 'defineLanguage').returns('en');
            this.sinon.stub(Daria.Html2Text, 'html2text')
                .withArgs(`<div class="normalize">${this.body}</div>`)
                .returns(this.bodyText);
            this.sinon.stub(Daria.signs, 'appendToBody').returns('RESULT');

            this.sinon.spy(_, 'escape');

            this.forwardStart = i18n('%Compose_Forward_Start');
            this.forwardEnd = i18n('%Compose_Forward_End');
        });

        describe('HTML формат', function() {
            beforeEach(function() {
                this.result = this.model.getForwardBody('html', 'from@email.com');
                this.bodyRaw = Daria.signs.appendToBody.getCall(0).args[0];
            });

            it('Должен правильно сформировать тело письма', function() {
                expect(this.bodyRaw).to.be.equal(
                    '<div><br/></div>' +
                    '<div><br/></div>' +
                    `<div>-------- ${this.forwardStart} --------</div>` +
                    '<div>FAKE_MESSAGE_DATE, &quot;Full Name&quot; &lt;login@host.ru&gt;:</div>' +
                    '<div><br/></div>' +
                    '<div class="normalize"><div>FAKE MESSAGE BODY</div></div>' +
                    '<div><br/></div>' +
                    `<div>-------- ${this.forwardEnd} --------</div>`
                );
            });

            it('Должен добавить подпись', function() {
                expect(Daria.signs.appendToBody)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly(this.bodyRaw, 'html', true, 'from@email.com', 'ru');
            });
        });

        describe('Текстовый формат', function() {
            beforeEach(function() {
                Daria.Translate.getLangByMid.withArgs('123').returns();

                this.result = this.model.getForwardBody('plain', 'from@email.com');
                this.bodyRaw = Daria.signs.appendToBody.getCall(0).args[0];
            });

            it('Должен правильно сформировать тело письма', function() {
                expect(this.bodyRaw).to.be.equal(
                    '\n\n' +
                    `-------- ${this.forwardStart} --------` +
                    '\n' +
                    'FAKE_MESSAGE_DATE, "Full Name" <login@host.ru>:' +
                    '\n\n' +
                    this.bodyText +
                    '\n' +
                    `-------- ${this.forwardEnd} --------`
                );
            });

            it('Должен добавить подпись', function() {
                expect(Daria.signs.appendToBody)
                    .to.have.callCount(1)
                    .and
                    .to.be.calledWithExactly(this.bodyRaw, 'plain', true, 'from@email.com', 'en');
            });
        });
    });

    describe('#setError', function() {
        beforeEach(function() {
            this.scenarioManager = this.sinon.stubScenarioManager(this.model);
        });

        it('Должен дописать шаг "opening-error-message-body", если есть активный сценарий "Просмотр письма"', function() {
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.withArgs('message-view-scenario').returns(true);
            this.scenarioManager.getActiveScenario.withArgs('message-view-scenario').returns(scenario);

            this.model.setError({});

            expect(scenario.logError)
                .to.have.callCount(1)
                .and.to.be.calledWith({ type: 'opening-error-message-body', severity: 'blocker' });
        });

        it('Должен дописать шаг "search-error-message-body", если есть активный сценарий "Поиск писем"', function() {
            const scenario = this.scenarioManager.stubScenario;
            this.scenarioManager.hasActiveScenario.withArgs('search-scenario').returns(true);
            this.scenarioManager.getActiveScenario.withArgs('search-scenario').returns(scenario);

            this.model.setError({});

            expect(scenario.logError)
                .to.have.callCount(1)
                .and.to.be.calledWith({ type: 'search-error-message-body', severity: 'critical' });
        });
    });
});
