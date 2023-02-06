describe('Daria.Suggest.Contacts', function() {
    beforeEach(function() {
        this.sinon.stub($, 'ajax').returns($.Deferred().resolve());
        this.eventObj = {
            preventDefault: this.sinon.stub()
        };
        this.input = $('<input />');
        this.contactsSuggestPrototype = Daria.Suggest.Contacts.prototype;
        this.suggestPrototype = Daria.Suggest.prototype;
        this.contactsSuggest = new Daria.Suggest.Contacts(this.input[0]);
    });

    afterEach(function() {
        this.contactsSuggest.destroy();
    });

    var itShouldBeChaining = function(methodName) {
        var data = [].slice.call(arguments, 1);
        it('Должен поддерживать chaining', function() {
            var methodResult = this.contactsSuggest[methodName].apply(this.contactsSuggest, data);

            expect(methodResult).to.be.equal(this.contactsSuggest);
        });
    };

    var itShouldRunSuperClassMethod = function(methodName, args) {
        it('Должен вызвать метод родительского класса', function() {
            if (!args) {
                args = [];
            }

            this.sinon.stub(this.suggestPrototype, methodName);
            this.contactsSuggest[methodName].apply(this.contactsSuggest, args);

            expect(this.suggestPrototype[methodName]).to.have.callCount(1);
        });
    };

    it('Должен иницировать опцию renderItem для саджеста и связать её с методом #_renderItem', function() {
        this.sinon.stub(this.contactsSuggestPrototype, '_renderItem');
        var contactsSuggest = new Daria.Suggest.Contacts(this.input[0]);
        contactsSuggest.options.renderItem();

        expect(this.contactsSuggestPrototype._renderItem).to.have.callCount(1);
    });

    it('Должен добавить метод _getContactsToExclude, если в опциях передать функцию exclude', function() {
        var excludeFunction = function() {};
        var contactsSuggest = new Daria.Suggest.Contacts(this.input[0], {exclude: excludeFunction});
        expect(contactsSuggest._getContactsToExclude).to.be.eql(excludeFunction);
    });

    it('Не добавлять метод _getContactsToExclude, если в опциях exclude не является функцией', function() {
        var contactsSuggest = new Daria.Suggest.Contacts(this.input[0], {exclude: {}});
        expect(contactsSuggest._getContactsToExclude).to.be.eql(undefined);
    });

    it('Не добавлять метод _getContactsToExclude, если в опциях отсутствует exclude', function() {
        var contactsSuggest = new Daria.Suggest.Contacts(this.input[0], {});
        expect(contactsSuggest._getContactsToExclude).to.be.eql(undefined);
    });

    it('Должен вызвать родительский класс при создании экземпляра', function() {
        this.sinon.stub(Daria, 'Suggest');
        var phoneSuggest = new Daria.Suggest.Contacts(this.input[0]);

        expect(Daria.Suggest).to.have.callCount(1);
    });

    describe('#init', function() {
        it('Должен отключить показ "Копия по SMS" для Quick Reply поля', function() {
            this.$parent = $('<div class="js-compose-mail-input js-compose-mail-message-input"></div>');
            this.$parent.append(this.input);
            var suggest = new Daria.Suggest.Contacts(this.input[0], {
                noSmsButton: false
            });

            expect(suggest.options.noSmsButton).to.be.equal(true);
        });


        itShouldRunSuperClassMethod('init');

        itShouldBeChaining('init');
    });

    describe('#bindEvents', function() {
        beforeEach(function() {
            this.sinon.stub(this.contactsSuggest.$inputField, 'on').returns(this.contactsSuggest.$inputField);
        });

        var itBindEventWithObjectMethod = function(event, methodName, callNumber) {
            it('Должен связать метод #' + methodName + ' с событием ' + event + ' поля ввода', function() {
                this.sinon.stub(this.contactsSuggest, methodName);

                this.contactsSuggest.bindEvents();
                this.contactsSuggest.$inputField.on.getCall(callNumber).args[1]();

                expect(this.contactsSuggest.$inputField.on.calledWith(event) && this.contactsSuggest[methodName].called).to.be.ok;
            });
        };

        itBindEventWithObjectMethod('keydown.daria-suggest keypress.daria-suggest', '_keydownEvent', 0);
        itBindEventWithObjectMethod('click.daria-suggest', '_clickEvent', 1);

        itShouldRunSuperClassMethod('bindEvents');

        itShouldBeChaining('bindEvents');
    });

    describe('#_keydownEvent', function() {
        beforeEach(function() {
            this.KEY = Jane.Common.keyCode;

            this.keyEvent = $.Event('keydown', {});
            this.enterKeyEvent = $.Event('keydown', {
                which: this.KEY.ENTER
            });
            this.contactsSuggest.currentContent = [1, 2];
            this.contactsSuggest.currentFocused = true;
            this.contactsSuggest.setOptions({
                multiple: true
            });

            this.sinon.stub(this.contactsSuggest, 'focusedSelect');
        });

        it('Должен исключить рекурсию, если произошло нажатие клавиши Enter', function() {
            this.contactsSuggest._keydownEvent(this.enterKeyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(0);
        });

        it('Должен вызвать событие нажатия клавиши Enter, если нажата клавиша ","', function() {
            this.keyEvent.which = this.KEY.COMMA_CHAR;

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(1);
        });

        it('Должен вызвать событие нажатия клавиши Enter, если нажата клавиша ";"',  function() {
            this.keyEvent.which = this.KEY.SEMI_CHAR;

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(1);
        });

        it('Должен вызвать событие нажатия клавиши Enter, если нажата клавиша " " и один результат в саджесте', function() {
            this.keyEvent.which = this.KEY.SPACE_CHAR;
            this.contactsSuggest.currentContent = [1];

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(1);
        });

        it('Должен вызвать событие нажатия клавиши Enter, если нажата клавиша " " и нет результатов в саджесте', function() {
            this.keyEvent.which = this.KEY.SPACE_CHAR;
            this.contactsSuggest.currentContent = [];

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(1);
        });

        it('Должен не вызвать событие нажатия клавиши Enter, если нажата клавиша " " и больше одного результата в саджесте', function() {
            this.keyEvent.which = this.KEY.SPACE_CHAR;

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(0);
        });

        it('Должен не вызвать событие нажатия клавиши Enter, если нажата любая клавиша, кроме ",", ";", " "', function() {
            this.keyEvent.which = 10;

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.contactsSuggest.focusedSelect).to.have.callCount(0);
        });

        it('Должен не дать вывестись символу, если нажата любая клавиша ",", ";", " " + 1 результат', function() {
            this.keyEvent.which = this.KEY.COMMA_CHAR;
            this.sinon.spy(this.keyEvent, 'preventDefault');

            this.contactsSuggest._keydownEvent(this.keyEvent);

            expect(this.keyEvent.preventDefault).to.have.callCount(1);
        });
    });

    describe('#_clickEvent', function() {
        beforeEach(function() {
            this.sinon.stub(this.contactsSuggest.nbSuggest, 'getValue').returns('test');
            this.sinon.stub(this.contactsSuggest.nbSuggest, 'search');
        });

        it('Должен вызвать search с текущим термом при первом клике', function() {
            this.contactsSuggest._clickEvent();

            expect(this.contactsSuggest.nbSuggest.search.calledWith('test')).to.be.ok;
        });
    });

    describe('#_renderItem', function() {
        beforeEach(function() {
            this.renderData = {
                item: {
                    type: 'contact',
                    name: 'test',
                    email: 'test@example.com',
                    phone: '123'
                }
            };
            var hAccountInformation = ns.Model.get('account-information');
            this.sinon.stub(hAccountInformation, 'getAllUserEmails');
            this.hAccountInformation = hAccountInformation;
            this.sinon.stub(yr, 'run').returns('test');
        });

        ['name', 'email', 'phone'].forEach(function(field) {
            it('Должен создать поле highlighted-' + field, function() {
                this.contactsSuggest.currentTerm = 'ex';
                this.contactsSuggest._renderItem(this.renderData);

                expect(this.renderData.item['highlighted-' + field]).to.be.ok;
            });
        });

        it('Должен вызвать подсветку терма в данных саджеста', function() {
            this.sinon.spy(this.contactsSuggest, '_highlight');
            this.contactsSuggest._renderItem(this.renderData);

            expect(this.contactsSuggest._highlight).to.have.callCount(1);
        });

        it('Должен пройтись по полям name, mail, phone элемента саджеста и подсветить терм в первом поле, в котором он будет сожержаться', function() {
            this.contactsSuggest.currentTerm = 'ex';
            this.contactsSuggest._renderItem(this.renderData);

            expect(this.renderData.item['highlighted-name'].indexOf('strong') === -1 &&
                this.renderData.item['highlighted-email'].indexOf('strong') !== -1 &&
                this.renderData.item['highlighted-phone'].indexOf('strong') === -1).to.be.ok;
        });

        it('Должен вызвать YATE рендер с передачей в него данных об элементе саджеста', function() {
            this.contactsSuggest._renderItem(this.renderData);

            expect(yr.run.getCall(0).args[1]).to.eql(this.renderData);
        });
    });

    describe('#_isInnerEvent', function() {
        beforeEach(function() {
            this.event = $.Event('some-suggest-event', {
                originalEvent: null
            });
        });

        it('Должен вернуть true, если событие саджеста не вызвано событием в браузере', function() {
           expect(this.contactsSuggest._isInnerEvent(this.event)).to.be.equal(true);
        });

        it('Должен вернуть false, если событие саджеста вызвано событием в браузере', function() {
            this.event.originalEvent = {};
            expect(this.contactsSuggest._isInnerEvent(this.event)).to.be.equal(false);
        });
    });

    describe('#_isKeydown', function() {
        beforeEach(function() {
            this.event = $.Event('some-suggest-event', {
                originalEvent: {
                    type: 'keydown'
                }
            });
        });

        it('Должен вернуть true, если событие саджеста вызвано событием keydown в браузере', function() {
            expect(this.contactsSuggest._isKeydown(this.event)).to.be.equal(true);
        });

        it('Должен вернуть false, если событие саджеста не вызвано событием keydown в браузере', function() {
            this.event.originalEvent.type = 'input';
            expect(this.contactsSuggest._isKeydown(this.event)).to.be.equal(false);
        });
    });

    describe('#_isDownOrUpKey', function() {
        beforeEach(function() {
            this.KEY = Jane.Common.keyCode;
            this.event = $.Event('some-suggest-event', {
                originalEvent: {
                    type: 'keydown'
                }
            });
        });

        it('Должен вернуть true, если событие саджеста вызвано нажатием клавиши UP', function() {
            this.event.originalEvent.which = this.KEY.UP;
            expect(this.contactsSuggest._isDownOrUpKey(this.event)).to.be.equal(true);
        });

        it('Должен вернуть true, если событие саджеста вызвано нажатием клавиши DOWN', function() {
            this.event.originalEvent.which = this.KEY.DOWN;
            expect(this.contactsSuggest._isDownOrUpKey(this.event)).to.be.equal(true);
        });

        it('Должен вернуть false, если событие саджеста не вызвано нажатием клавиш DOWN или UP', function() {
            this.event.originalEvent.type = 'input';
            expect(this.contactsSuggest._isDownOrUpKey(this.event)).to.be.equal(false);
        });
    });

    describe('#_search', function() {
        it('Должен запомнить событие, которое вызвало поиск в саджесте', function() {
            var event = {};
            this.contactsSuggest._search(event);
            expect(this.contactsSuggest.searchEvent).to.be.equal(event);
        });

        itShouldRunSuperClassMethod('_search', [$.Event()]);
    });

    describe('#_source', function() {
        beforeEach(function() {
            this.response = _.noop;
            this.sinon.stub(this.contactsSuggest, '_isTemplate').returns(false);
            this.sinon.stub(this.contactsSuggest, '_isInnerEvent');
            this.sinon.stub(this.contactsSuggest, '_isDownOrUpKey');
        });

        it('Должен задать в опции хэндлер abook-suggest с параметром popular, если терм пустой, страница не темплейтная и событие саджеста', function() {
            this.contactsSuggest._isInnerEvent.returns(true);
            this.contactsSuggest._isDownOrUpKey.returns(false);
            this.contactsSuggest._source({term: ''}, this.response);

            expect(this.contactsSuggest.options.handlers['abook-suggest'].popular).to.be.ok;
        });

        it('Должен задать в опции хэндлер abook-suggest с параметром popular, если терм пустой, страница не темплейтная и нажата стрелка вниз/вверх', function() {
            this.contactsSuggest._isInnerEvent.returns(false);
            this.contactsSuggest._isDownOrUpKey.returns(true);
            this.contactsSuggest._source({term: ''}, this.response);

            expect(this.contactsSuggest.options.handlers['abook-suggest'].popular).to.be.ok;
        });

        it('Должен убрать все хэндлеры, если терм пустой и есть оригинальное событие не нажатия стрелки вниз/вверх', function() {
            this.contactsSuggest._isInnerEvent.returns(false);
            this.contactsSuggest._isDownOrUpKey.returns(false);
            this.contactsSuggest._source({term: ''}, this.response);

            expect(this.contactsSuggest.options.handlers).to.be.eql({});
        });

        it('Должен задать в опции хэндлер abook-suggest, если страница темплейтная', function() {
            this.contactsSuggest._isTemplate.returns(true);
            this.contactsSuggest._source({term: ''}, this.response);

            expect(this.contactsSuggest.options.handlers['abook-suggest']).to.be.ok;
        });

        it('Должен задать в опции хэндлер abook-suggest, если есть терм', function() {
            this.contactsSuggest._source({term: 'test'}, this.response);

            expect(this.contactsSuggest.options.handlers['abook-suggest']).to.be.ok;
        });

        it('Должен задать в опции хэндлер abook-suggest с параметрами to, cc и bcc, если есть функция _getContactsToExclude', function() {
            this.contactsSuggest._getContactsToExclude = this.sinon.stub().returns({to: ['aa@bb'], cc: ['cc@dd'], bcc: ['dd@ff']});
            this.contactsSuggest._source({term: 'test'}, this.response);

            expect(this.contactsSuggest.options.handlers['abook-suggest'].to).to.be.eql(['aa@bb']);
            expect(this.contactsSuggest.options.handlers['abook-suggest'].cc).to.be.eql(['cc@dd']);
            expect(this.contactsSuggest.options.handlers['abook-suggest'].bcc).to.be.eql(['dd@ff']);
        });

        it('Не должен задавать в опции хэндлера abook-suggest параметры to, cc и bcc, если нет функции _getContactsToExclude', function() {
            this.contactsSuggest._getContactsToExclude = undefined;
            this.contactsSuggest._source({term: 'test'}, this.response);

            expect(this.contactsSuggest.options.handlers['abook-suggest'].to).to.be.eql(undefined);
            expect(this.contactsSuggest.options.handlers['abook-suggest'].cc).to.be.eql(undefined);
            expect(this.contactsSuggest.options.handlers['abook-suggest'].bcc).to.be.eql(undefined);
        });

        itShouldRunSuperClassMethod('_source', [{term: 'test'}]);
    });

    describe('#_select', function() {
        itShouldRunSuperClassMethod('_select', [this.eventObj, {
            item: {}
        }]);
    });

    describe('#_focus', function() {
        var ui = {
            item: {}
        };

        it('Должен сохранить текущий элемент в фокусе', function() {
            this.contactsSuggest._focus(this.eventObj, ui);

            expect(this.contactsSuggest.currentFocused).to.be.equal(ui.item);
        });
        itShouldRunSuperClassMethod('_focus', [{}, ui]);
    });

    describe('#_response', function() {
        var ui = {
            content: [{
                type: 'label'
            }]
        };

        itShouldRunSuperClassMethod('_response', [{}, ui]);

        it('Должен сбросить массив данных саджеста, если он состоит только из label элемента', function() {
            this.contactsSuggest._response(this.eventObj, ui);

            expect(ui.content.length).to.be.equal(0);
        });
    });

    describe('#_close', function() {
        it('Должен сбросить счетчик кликов в поле ввода', function() {
            this.contactsSuggest.countOfClick = 5;
            this.contactsSuggest._close();

            expect(this.contactsSuggest.countOfClick).to.be.equal(0);
        });

        itShouldRunSuperClassMethod('_close');
    });

    describe('#_highlight', function() {
        it('Должен подсветить первый встретевшийся терм в тексте', function() {
            var text = 'some text';
            var resultText = 'some <strong class="js-suggest-highlight">te</strong>xt';
            expect(this.contactsSuggest._highlight(text, 'te')).to.eql(resultText);
        });

        it('Должен заэкранировать теги', function() {
            var text = '<script>alert("test")</script>';
            var resultText = '&lt;script&gt;alert(&quot;<strong class="js-suggest-highlight">te</strong>st&quot;)&lt;/script&gt;';
            expect(this.contactsSuggest._highlight(text, 'te')).to.eql(resultText);
        });
    });

    describe('#parse', function() {
        beforeEach(function() {
            this.sinon.stub(this.contactsSuggest, 'parseAbookSuggest');
        });

        it('Должен вызвать парсинг abook-suggest хэндлера, если вызван его parse', function() {
            this.contactsSuggest.parse('abook-suggest', {});

            expect(this.contactsSuggest.parseAbookSuggest.calledWith({})).to.be.ok;
        });

        it('Должен возвращать пустой массив, когда пришли данные от abook-suggest', function() {
            expect(this.contactsSuggest.parse('some-handler', {})).to.eql([]);
        });
    });

    describe('#parseAbookSuggest', function() {
        beforeEach(function() {
            this.contacts = [
                {
                    name: '',
                    email: 'test@yandex.ru',
                    popdom: true
                },
                {
                    name: 'test1 name1',
                    email: 'test1@example.com',
                    phones: ['123']
                },
                {
                    name: 'test2 name2',
                    email: 'test2@example.com'
                }
            ];
            this.groups = [{
                title: 'test',
                contacts: this.contacts

            }];
            this.data = {
                contacts: this.contacts,
                groups: this.groups
            };
            this.result = [
                $.extend({}, this.contacts[0], {
                    value: '<test@yandex.ru>',
                    phone: '',
                    name: '',
                    type: 'popdom'
                }),
                $.extend({}, this.contacts[1], {
                    phone: this.contacts[1].phones[0],
                    value: '"test1 name1" <test1@example.com>',
                    type: 'contact'
                }),
                $.extend({}, this.contacts[2], {
                    phone: '',
                    value: '"test2 name2" <test2@example.com>',
                    type: 'contact'
                }),
                $.extend({}, this.groups[0], {
                    name: this.groups[0].title,
                    type: 'group',
                    email: 'test@yandex.ru, test1@example.com, test2@example.com',
                    value: '<test@yandex.ru>, "test1 name1" <test1@example.com>, "test2 name2" <test2@example.com>'
                })
            ];
            this.sinon.spy(this.contactsSuggest, 'parseAbookSuggest');
        });

        it('Должен не вызвать ошибки при отсутствии данных', function() {
            var that = this;
            expect(function() {
                that.contactsSuggest.parseAbookSuggest();
            }).to.not.throw();
        });

        it('Должен не вызвать ошибки при отсутствии контактов и групп', function() {
            var that = this;
            expect(function() {
                that.contactsSuggest.parseAbookSuggest({});
            }).to.not.throw();
        });

        it('Должен распарсить контакты и группы', function() {
            var result = this.contactsSuggest.parseAbookSuggest(this.data);

            expect(result).to.eql(this.result);
        });

        it('Должен добавить label, если в данных только подсказки по популярным доменам', function() {
            var data = {
                contacts: [this.contacts[0]]
            };
            var result = this.contactsSuggest.parseAbookSuggest(data);
            expect(result).to.eql([{
                type: 'label',
                name: i18n('%Suggest_notify') + ':'
            }, this.result[0]]);
        });
    });

    describe('#parseValue', function() {
        beforeEach(function() {
            this.sinon.stub(this.contactsSuggest, 'formatContact');
        });

        it('Должен вызвать #formatContact, если передан контакт', function() {
            this.contactsSuggest.parseValue({
                type: 'contact'
            });

            expect(this.contactsSuggest.formatContact).to.have.callCount(1);
        });

        it('Должен вызвать #formatContact, если передана доменная подсказка', function() {
            this.contactsSuggest.parseValue({
                type: 'popdom'
            });

            expect(this.contactsSuggest.formatContact).to.have.callCount(1);
        });

        it('Должен вызвать #formatContact, если передана группа', function() {
            this.contactsSuggest.parseValue({
                type: 'group',
                contacts: [{}, {}, {}]
            });

            expect(this.contactsSuggest.formatContact).to.have.callCount(3);
        });

        it('Должен вызвать #formatContact для каждого контакта группы и объединить результаты через ","', function() {
            this.contactsSuggest.formatContact.returns('test');
            var result = this.contactsSuggest.parseValue({
                type: 'group',
                contacts: [{}, {}, {}]
            });

            expect(result).to.be.equal('test, test, test');
        });
    });

    describe('#formatContact', function() {
        it('Должен вызвать Jane.FormValidation.obj2contact и передать в него объект контакта', function() {
            var contact = {
                name: 'test',
                email: 'test@example.com'
            };
            this.sinon.stub(Jane.FormValidation, 'obj2contact');
            this.contactsSuggest.formatContact(contact);

            expect(Jane.FormValidation.obj2contact.calledWithExactly(contact)).to.be.equal(true);
        });
    });

});

