describe('Daria.Suggest', function() {
    beforeEach(function() {
        this.KEY = Jane.Common.keyCode;
        this.sinon.stub($, 'ajax').returns($.Deferred().resolve());
        this.eventObj = {
            preventDefault: this.sinon.stub()
        };
        this.input = $('<input />');
        this.suggestPrototype = Daria.Suggest.prototype;
        this.suggest = new Daria.Suggest(this.input[0], {
            cacheSize: 2
        });
        this.sinon.stub(this.suggest.nbSuggest, 'search');
    });

    afterEach(function() {
        this.suggest.destroy();
    });

    var itShouldBeChaining = function(methodName) {
        var data = [].slice.call(arguments, 1);
        it('Должен поддерживать chaining', function() {
            var methodResult = this.suggest[methodName].apply(this.suggest, data);
            expect(methodResult).to.be.equal(this.suggest);
        });
    };

    it('Должен вызвать #changeInputField с флагом запрета #destroy', function() {
        this.sinon.stub(this.suggestPrototype, 'changeInputField');
        var suggest = new Daria.Suggest(this.input[0]);

        expect(suggest.changeInputField.calledWith(this.input[0], true)).to.be.ok;

        suggest.destroy();
    });

    describe('#changeInputField', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Suggest.prototype, 'init');
        });

        it('можно передать jquery объект', function() {
            var $node = $('<div />');
            var node = $node[0];
            this.suggest.changeInputField($node);
            expect(this.suggest.inputField).to.be.equal(node);
        });

        it('можно передать ноду', function() {
            var node = $('<div />')[0];
            this.suggest.changeInputField(node);
            expect(this.suggest.inputField).to.be.equal(node);
        });
    });

    describe('extractLast', function() {
        it('Должен вернуть последнюю фразу, находящуюся за разделителем', function() {
            var result = Daria.Suggest.extractLast('Term1, Term2', /,\s+/);
            expect(result).to.be.equal('Term2');
        });
    });

    describe('mixPrivateMethodWithOption', function() {
        beforeEach(function() {
            this.obj = {
                _show: this.sinon.stub(),
                _hide: this.sinon.stub(),
                _open: this.sinon.stub()
            };
            this.options = {
                show: this.sinon.stub(),
                hide: this.sinon.stub()
            };
            // Клонирование необходимо для остлеживание вызова, т.к. подменяются методы исходных опций
            this.clonedOptions = $.extend({}, this.options);
        });

        it('Должен объединить указанные методы объекта и опций в единые методы', function() {
            var resultOptions = Daria.Suggest.mixPrivateMethodWithOption(this.obj, this.options, ['show']);
            resultOptions.show();
            expect(this.clonedOptions.show.called && this.obj._show.called).to.be.ok;
        });

        it('Должен не объединять методы, которые неуказаны в списке объединяемых', function() {
            var resultOptions = Daria.Suggest.mixPrivateMethodWithOption(this.obj, this.options, ['show']);
            resultOptions.hide();
            expect(this.obj._hide).to.have.callCount(0);
        });

        it('Должен при вызове объединения передать this вызываемой функции в опции', function() {
            var thisObj = {};
            var resultOptions = Daria.Suggest.mixPrivateMethodWithOption(this.obj, this.options, ['show']);
            resultOptions.show.call(thisObj, 'test');
            expect(this.obj._show.thisValues[0] === this.obj && this.clonedOptions.show.thisValues[0] === thisObj).to.be.ok;
        });

        it('Должен при вызове объединения передать все аргументы вызываемой функции', function() {
            var thisObj = {};
            var resultOptions = Daria.Suggest.mixPrivateMethodWithOption(this.obj, this.options, ['show']);
            resultOptions.show.call(thisObj, 'test');
            expect(this.obj._show.calledWith('test') && this.clonedOptions.show.calledWith('test')).to.be.ok;
        });

        it('Должен при отсутствии у опции указанного метода добавить метод объекта', function() {
            var resultOptions = Daria.Suggest.mixPrivateMethodWithOption(this.obj, this.options, ['open']);
            resultOptions.open();
            expect(this.obj._open).to.have.callCount(1);
        });

        it('Должен при отсутствии и у опции и объекта указанного метода не упасть с ошибкой', function() {
            var that = this;
            expect(function() {
                Daria.Suggest.mixPrivateMethodWithOption(that.obj, that.options, ['someMethod']);
            }).to.not.throw();
        });

        it('Должен исключить повторное миксование', function() {
            var resultOptions1 = Daria.Suggest.mixPrivateMethodWithOption(this.obj, this.options, ['show']);
            var resultOptions2 = Daria.Suggest.mixPrivateMethodWithOption(this.obj, resultOptions1, ['show']);
            resultOptions2.show();

            expect(this.obj._show.calledOnce && this.clonedOptions.show.calledOnce && resultOptions2.show === resultOptions1.show).to.be.ok;
        });
    });

    describe('#changeInputField', function() {
        beforeEach(function() {
            this.sinon.stub(this.suggest, 'init');
            this.sinon.stub(this.suggest, 'destroy');
        });

        it('Должен вызвать #init, если inputField DOM node', function() {
            this.suggest.changeInputField(this.input[0]);
            expect(this.suggest.init).to.have.callCount(1);
        });

        it('Должен не вызывать #init, если inputField не DOM node', function() {
            this.suggest.changeInputField({});
            expect(this.suggest.init).to.have.callCount(0);
        });

        it('Должен не вызывать #destroy, если указан флаг запрета destroy', function() {
            this.suggest.changeInputField(this.input[0], true);
            expect(this.suggest.destroy).to.have.callCount(0);
        });

        it('Должен изменить ссылку на поле ввода', function() {
            var inputField = this.input.clone();
            this.suggest.changeInputField(inputField[0], true);
            expect(this.suggest.inputField).to.be.equal(inputField[0]);
        });

        itShouldBeChaining('changeInputField');
    });

    describe('#init', function() {
        it('Должен подготовить поле ввода под инициализацию островного саджеста', function() {
            var suggest = new Daria.Suggest(this.input[0]);
            expect(suggest.$inputField.attr('data-nb') === 'suggest' &&
                suggest.$inputField.hasClass('_init') &&
                suggest.$inputField.attr('data-class-suggest') === suggest.options.suggestClass
            ).to.be.ok;
        });

        it('Должен инициализировать островной саджест', function() {
            this.sinon.spy(nb, 'block');
            var suggest = new Daria.Suggest(this.input[0]);

            expect(nb.block).to.have.callCount(1);

            suggest.destroy();
        });

        it('Должен вызвать установку опций', function() {
            this.sinon.stub(this.suggestPrototype, 'setOptions').returns(this.suggest);
            var suggest = new Daria.Suggest(this.input[0]);

            expect(this.suggestPrototype.setOptions).to.have.callCount(1);

            suggest.destroy();
        });

        itShouldBeChaining('init');
    });

    describe('#setOptions', function() {
        it('Должен объединить методы экземпляра и методы опций', function() {
            this.sinon.stub(Daria.Suggest, 'mixPrivateMethodWithOption');
            this.suggest.setOptions();

            expect(Daria.Suggest.mixPrivateMethodWithOption).to.have.callCount(1);
        });

        it('Должен передать настройки в островной саджест', function() {
            var options = {
                someOptions: 1
            };
            this.suggest.setOptions(options);
            expect(this.suggest.nbSuggest.getOption('someOptions')).to.be.equal(1);
        });

        it('Не должен вызывать эксепшена, если островной саджест не создан', function() {
            var suggest = new Daria.Suggest({});

            expect(function() {
                suggest.setOptions();
            }).to.not.throw();

            suggest.destroy();
        });

        it('Должен при поддержке множественного ввода перевести разделитель в RegExp', function() {
            this.suggest.setOptions({
                multiple: true,
                multipleSeparator: ','
            });
            expect(this.suggest.options.multipleSeparatorRegExp instanceof RegExp).to.be.ok;
        });

        itShouldBeChaining('setOptions');
    });

    describe('#show', function() {
        it('Должен вызвать search для островного саджеста', function() {
            this.suggest.show('test');
            expect(this.suggest.nbSuggest.search.calledWithExactly('test')).to.be.equal(true);
        });

        it('Должен вызвать взять значение поля ввода, если не передано значение для show саджеста', function() {
            this.suggest.nbSuggest.setValue('test');
            this.suggest.show();
            expect(this.suggest.nbSuggest.search.calledWithExactly('test')).to.be.equal(true);
        });

        it('Не должен вызывать ошибки, если островной саджест не был создан', function() {
            var that = this;
            delete  this.suggest.nbSuggest;
            expect(function() {
                that.suggest.show();
            }).to.not.throw();
        });

        itShouldBeChaining('show');
    });

    describe('#hide', function() {
        beforeEach(function() {
            this.sinon.stub(this.suggest.nbSuggest, 'close');
            this.sinon.stub(this.suggest, 'abort');
        });

        it('Должен установить в false флаг #dontClose', function() {
            this.suggest.hide();
            expect(this.suggest.dontClose).to.be.equal(false);
        });

        it('Должен сбрасывать текущие запросы', function() {
            this.suggest.hide();
            expect(this.suggest.abort).to.have.callCount(1);
        });

        it('Должен скрывать саджест', function() {
            this.suggest.hide();
            expect(this.suggest.nbSuggest.close).to.have.callCount(1);
        });

        it('Не должен вызывать ошибки, если островной саджест не был создан', function() {
            var that = this;
            delete  this.suggest.nbSuggest;
            expect(function() {
                that.suggest.hide();
            }).to.not.throw();
        });

        it('Должен вызвать функцию опции hide с передаче #inputField как this', function() {
            this.suggest.options.hide = this.sinon.stub();
            this.suggest.hide();

            expect(this.suggest.options.hide.thisValues[0]).to.be.equal(this.suggest.inputField);
        });

        it('Не должен вызвать исключение, если нет опции hide', function() {
            var that = this;

            expect(function() {
                that.suggest.hide();
            }).to.not.throw();
        });

        itShouldBeChaining('hide');
    });

    describe('#bindJaneEvent', function() {
        beforeEach(function() {
            this.sinon.spy(ns.events, 'on');
            this.suggest.someMethod = this.sinon.stub();
            this.suggest.bindJaneEvent('some-event', 'someMethod');
        });

        it('Должен записать связанный метод в #_janeEvents', function() {
            this.suggest._janeEvents['some-event']();

            expect(this.suggest.someMethod).to.have.callCount(1);
        });

        it('Должен при вызове обработчика, вызывать метод саджеста с коректным this', function() {
            this.suggest._janeEvents['some-event']();
            expect(this.suggest.someMethod.thisValues[0]).to.be.equal(this.suggest);
        });

        it('Должен подписаться на событие приложения', function() {
            expect(ns.events.on.calledWithExactly('some-event', this.suggest._janeEvents['some-event'])).to.be.equal(true);
        });

        itShouldBeChaining('bindJaneEvent', 'some-event', 'someMethod');
    });

    describe('#unbindJaneEvent', function() {
        beforeEach(function() {
            this.sinon.spy(ns.events, 'off');
            this.suggest._janeEvents = {
                'some-event': _.noop
            };
            this.suggest.unbindJaneEvent('some-event');
        });

        it('Должен отписать указанный обработчик от события приложения', function() {
            expect(ns.events.off.calledWithExactly('some-event', _.noop)).to.be.equal(true);
        });

        it('Должен удалить из #_janeEvents отписанный обработчик', function() {
            expect(this.suggest._janeEvents['some-event']).to.be.equal(undefined);
        });

        itShouldBeChaining('unbindJaneEvent');
    });

    describe('#bindEvents', function() {
        beforeEach(function() {
            this._msie = Modernizr.msie;
            Modernizr.msie = true;

            this.sinon.stub(this.suggest.$document, 'on');
            this.suggest.$inputField.on = this.sinon.stub();
            this.sinon.stub(this.suggest, '_ieDocumentMousedown');
            this.sinon.stub(this.suggest, '_ieInputBlur');
        });

        afterEach(function() {
            delete this.suggest.$inputField.on;
            Modernizr.msie = this._msie;
        });

        it('Должен подписаться на событие приложения "timymce.click"', function() {
            this.sinon.stub(this.suggest, 'bindJaneEvent');
            this.suggest.bindEvents();

            expect(this.suggest.bindJaneEvent.calledWithExactly('timymce.click', 'hidex'));
        });

        it('Должен связать метод #_ieDocumentMousedown с событием "mousedown.daria-suggest" документа для ie', function() {
            this.suggest.bindEvents();
            this.suggest.$document.on.getCall(0).args[1]();

            expect(this.suggest.$document.on.calledWith('mousedown.daria-suggest') && this.suggest['_ieDocumentMousedown'].called).to.be.equal(true);
        });

        it('Не должен связать метод #_ieDocumentMousedown с событием "mousedown.daria-suggest" документа для не ie браузеров', function() {
            Modernizr.msie = false;
            this.suggest.bindEvents();

            expect(this.suggest.$document.on.calledWith('mousedown.daria-suggest')).to.be.equal(false);
        });


        it('Должен связать метод #_ieInputBlur с событием "blur.daria-suggest" поля ввода для ie', function() {
            this.suggest.bindEvents();
            this.suggest.$inputField.on.getCall(0).args[1]();

            expect(this.suggest.$inputField.on.calledWith('blur.daria-suggest') && this.suggest['_ieInputBlur'].called).to.be.ok;
        });

        it('Не должен связать метод #_ieInputBlur с событием "blur.daria-suggest" поля ввода для не ie браузеров', function() {
            Modernizr.msie = false;
            this.suggest.bindEvents();

            expect(this.suggest.$inputField.on.calledWith('mousedown.daria-suggest')).to.be.equal(false);
        });

        itShouldBeChaining('bindEvents');
    });

    describe('#_isSuggestDropDown', function() {
        it('Должен вернуть true, если target переданного события - dropdown саджеста', function() {
            var eventTarget = this.suggest.nbSuggest.$suggest[0];

            expect(this.suggest._isSuggestDropDown({target: eventTarget})).to.be.equal(true);
        });

        it('Должен вернуть true, если target переданного события - элемент dropdown саджеста', function() {
            var eventTarget = this.suggest.nbSuggest.$suggest.append('<li></li>').children()[0];

            expect(this.suggest._isSuggestDropDown({target: eventTarget})).to.be.equal(true);
        });

        it('Должен вернуть false, если target переданного события - не связан с dropdown саджеста', function() {
            var eventTarget = $('<div />')[0];

            expect(this.suggest._isSuggestDropDown({target: eventTarget})).to.be.equal(false);
        });
    });

    describe('#_ieDocumentMousedown', function() {
        beforeEach(function() {
            this.sinon.stub(this.suggest, 'hide');
        });

        it('Должен установить флаг #dontClose в true, если произошло нажатие по dropdown саджеста', function() {
            this.sinon.stub(this.suggest, '_isSuggestDropDown').returns(true);
            this.suggest._ieDocumentMousedown({});

            expect(this.suggest.dontClose).to.be.equal(true);
        });

        it('Должен установить флаг #dontClose в false, если произошло нажатие вне dropdown саджеста', function() {
            this.sinon.stub(this.suggest, '_isSuggestDropDown').returns(false);
            this.suggest._ieDocumentMousedown({});

            expect(this.suggest.dontClose).to.be.equal(false);
        });

        it('Должен вызвать скрытие саджеста, если поле ввода покинуто и event.target не связан с dropdown саджеста', function() {
            this.sinon.stub(this.suggest, '_isSuggestDropDown').returns(false);
            this.suggest.isInputFieldBlur = true;
            this.suggest._ieDocumentMousedown({});

            expect(this.suggest.hide).to.have.callCount(1);
        });

        it('Не должен вызвать скрытие саджеста, если поле ввода не покинуто', function() {
            this.sinon.stub(this.suggest, '_isSuggestDropDown').returns(false);
            this.suggest.isInputFieldBlur = false;
            this.suggest._ieDocumentMousedown({});

            expect(this.suggest.hide).to.have.callCount(0);
        });

        it('Не должен вызвать скрытие саджеста, если произошло нажатие по dropdown саджеста, а поле ввода покинуто', function() {
            this.sinon.stub(this.suggest, '_isSuggestDropDown').returns(true);
            this.suggest.isInputFieldBlur = true;
            this.suggest._ieDocumentMousedown({});

            expect(this.suggest.hide).to.have.callCount(0);
        });
    });

    describe('#_ieInputBlur', function() {
        it('Должен установить #isInputFieldBlur флаг в true при покидании поля ввода', function() {
            this.suggest._ieInputBlur();

            expect(this.suggest.isInputFieldBlur).to.be.equal(true);
        });
    });

    describe('#getValueWithPrevious', function() {
        beforeEach(function() {
            this.suggest.setOptions({
                multiple: true,
                previousValues: ['Term1']
            });
            this.sinon.stub(this.suggest.nbSuggest, 'getValue').returns('Term2');
        });

        it('Должен вернуть текстовое значение поля с учетом массива options.previousValues при множественном вводе', function() {
            expect(this.suggest.getValueWithPrevious()).to.be.equal('Term1, Term2');
        });

        it('Должен не добавлять options.previousValues при одиночном вводе', function() {
            this.suggest.setOptions({
                multiple: false
            });

            expect(this.suggest.getValueWithPrevious()).to.be.equal('Term2');
        });

        it('Должен корректно обработать пустой массив в options.previousValues', function() {
            this.suggest.setOptions({
                previousValues: []
            });

            expect(this.suggest.getValueWithPrevious()).to.be.equal('Term2');
        });

        it('Доллжен вернуть пустую строку, если островной саджест не был создан', function() {
            var that = this;
            delete  this.suggest.nbSuggest;
            expect(this.suggest.getValueWithPrevious()).to.be.equal('');
        });
    });

    describe('#_convertHandlers', function() {
        beforeEach(function() {
            this.suggest.setOptions({
                handlers: {
                    'test-handler-1': {
                        param1: 1
                    },
                    'test-handler-2': {
                        param2: 1
                    }
                }
            });
        });
        it('Должен сконвертировать объект данных о хэндлерах в массив', function() {
            var handlers = this.suggest._convertHandlers();
            expect(handlers).to.eql([
                {
                    name: 'test-handler-1',
                    params: {
                        param1: 1
                    }
                },
                {
                    name: 'test-handler-2',
                    params: {
                        param2: 1
                    }
                }
            ]);
        });

        it('Должен сконвертировать пустой объект данных о хэндлерах в пустой массив', function() {
            this.suggest.setOptions({
                handlers: {}
            });
            var handlers = this.suggest._convertHandlers();
            expect(handlers).to.eql([]);
        });

        it('Должен подмешать общие дополнительные параметры к хэндлерам', function() {
            this.suggest.setOptions({
                extraParams: {
                    extraParam: 1
                }
            });
            var handlers = this.suggest._convertHandlers();
            expect(handlers).to.eql([
                {
                    name: 'test-handler-1',
                    params: {
                        param1: 1,
                        extraParam: 1
                    }
                },
                {
                    name: 'test-handler-2',
                    params: {
                        param2: 1,
                        extraParam: 1
                    }
                }
            ]);
        });

        it('Должен подмешать параметры запроса к хэндлерам', function() {
            this.suggest.setOptions({
                extraParams: {
                    extraParam: 1
                }
            });
            var handlers = this.suggest._convertHandlers({
                query: 'test'
            });
            expect(handlers).to.eql([
                {
                    name: 'test-handler-1',
                    params: {
                        param1: 1,
                        extraParam: 1,
                        query: 'test'
                    }
                },
                {
                    name: 'test-handler-2',
                    params: {
                        param2: 1,
                        extraParam: 1,
                        query: 'test'
                    }
                }
            ]);
        });

        it('Должен соблюсти приоритетность параметров запроса перед всеми видами параметров', function() {
            this.suggest.setOptions({
                extraParams: {
                    param: 'extra-param'
                },
                handlers: {
                    'test-handler': {
                        param: 'handler-param'
                    }
                }
            });
            var handlers = this.suggest._convertHandlers({
                param: 'query-param'
            });
            expect(handlers[0].params.param).to.be.equal('query-param');
        });

        it('Должен соблюсти приоритетность параметров хэндлера перед общими параметрами', function() {
            this.suggest.setOptions({
                extraParams: {
                    param: 'extra-param'
                },
                handlers: {
                    'test-handler': {
                        param: 'handler-param'
                    }
                }
            });
            var handlers = this.suggest._convertHandlers();
            expect(no.jpath('/.[.name == "test-handler"]', handlers)[0].params.param).to.be.equal('handler-param');
        });
    });

    describe('#_parseHandlersData', function() {
        beforeEach(function() {
            this.handlers = [{
                name: 'test-handler1',
                params: {
                    param: 1
                }
            }];
            this.response1 = {
                models: [{
                    data: {
                        id: 1,
                        value: 'test'
                    }
                }],
                versions: {}
            };
            this.response2 = {
                models: [{
                    data: {
                        id: 2,
                        value: 'test'
                    }
                }],
                versions: {}
            };

            this.sinon.stub(this.suggest, 'parse');
        });

        it('Должен вызвать метод #parse при передаче данных от одного хэндлера', function() {
            this.suggest._parseHandlersData(this.handlers, [this.response1, 'success', $.Deferred()]);

            expect(this.suggest.parse.calledWith(this.handlers[0].name, this.response1.models[0].data)).to.be.ok;
        });

        it('Должен вызвать метод #parse при передаче данных от одного хэндлера', function() {
            var that = this;
            var response = [
                [this.response1, 'success', $.Deferred()],
                [this.response2, 'success', $.Deferred()]
            ];
            this.handlers.push({
                name: 'test-handler2',
                params: {
                    param: 2
                }
            });
            this.suggest._parseHandlersData(this.handlers, response);

            $.each(response, function(index) {
                var args = that.suggest.parse.getCall(index).args;
                var handlerName = args[0];
                var handlerData = args[1];

                expect(handlerName).to.be.equal(that.handlers[index].name);
                expect(handlerData).to.be.equal(that['response' + (index + 1)].models[0].data);
            });
        });

        it('Должен вызвать метод #parse с пустыми данными, если ответ пришел без них', function() {
            this.suggest._parseHandlersData(this.handlers, []);

            expect(this.suggest.parse.calledWithExactly('test-handler1', {}));
        });
    });

    describe('#_getTerm', function() {
        it('Должен вернуть переданный терм при одиночном вводе', function() {
            var result = this.suggest._getTerm('T');
            expect(result).to.be.equal('T');
        });

        it('Должен вернуть последний терм при множественном вводе', function() {
            this.suggest.setOptions({
                multiple: true
            });
            var result = this.suggest._getTerm('Term1, Ter');
            expect(result).to.be.equal('Ter');
        });
    });

    describe('#_source', function() {
        beforeEach(function() {

            this.callSuggestSource = function(withCache, newTerm) {
                this.response = this.sinon.stub();
                this.result = [{
                    label: newTerm ? newTerm : 'test',
                    value: newTerm ? newTerm : 'test'
                }];
                this.sinon.stub(this.suggest, '_getTerm').returns(newTerm ? newTerm : 'test');
                this.sinon.stub(this.suggest, '_parseHandlersData').returns(this.result);

                if (withCache) {
                    this.suggest.cache = {
                      '[{"param.0":"some-param","q.0":"test"}]': [
                          { name: 'Test', cid: '1' },
                          { name: 'Test1', cid: '2' }
                      ]
                    };
                }
                this.suggest.options.handlers = {
                    'some-handler': {
                        param: 'some-param'
                    }
                };
                this.suggest._source({term: newTerm ? newTerm : 'test'}, this.response);
            };
        });

        it('Должен при выборе терма обратиться к #_getTerm', function() {
            this.callSuggestSource();

            expect(this.suggest._getTerm).to.have.callCount(1);
        });

        it('Должен записать терм в #currentTerm', function() {
            this.callSuggestSource();

            expect(this.suggest.currentTerm).to.be.equal('test');
        });

        it('Должен не рендерить данные если длина терма не соответcтвует заданной', function() {
            this.suggest.setOptions({
                minLength: 10
            });
            this.callSuggestSource();

            expect(this.response).to.have.callCount(0);
        });

        it('Должен произвести конвертирование данных о хэндлерах перед отправкой (вызов _convertHandlers)', function() {
            this.sinon.stub(this.suggest, '_convertHandlers').returns([]);
            this.callSuggestSource();

            expect(this.suggest._convertHandlers).to.have.callCount(1);
        });

        it('Должен произвести остановку предыдущих запросов', function() {
            this.sinon.stub(this.suggest, 'abort');
            this.callSuggestSource();

            expect(this.suggest.abort).to.have.callCount(1);
        });

        it('Должен не брать данные из кэша и не отправлять запросы, если нет хэндлеров', function() {
            this.suggest.options.handlers = {};
            this.suggest._source({term: 'test'}, this.response);
            expect(this.response.calledWith([])).to.be.ok;
        });

        it('Должен попробовать взять данные из кэша', function() {
            this.callSuggestSource(true);
            expect(this.response.calledWith(this.suggest.cache['[{"param.0":"some-param","q.0":"test"}]'])).to.be.ok;
        });

        it('Если нет данных в кеше, должен сделать запрос', function() {
            this.callSuggestSource(true, 'test1');
            expect($.ajax).to.have.callCount(1);
        });

        it('Должен при взятии данных из кэша не отправлять запросы', function() {
            this.callSuggestSource(true);
            expect($.ajax).to.have.callCount(0);
        });

        it('Должен запросить данные у хэдлера', function() {
            this.callSuggestSource();
            expect($.ajax).to.have.callCount(1);
        });

        it('Должен при получении данных от хэндлера произвести их парсинг (вызов _parseHandlersData)', function() {
            this.callSuggestSource();
            expect(this.suggest._parseHandlersData).to.have.callCount(1);
        });

        it('Должен при успешном получении данных от хэндлеров записать их в кэш', function() {
            this.sinon.stub(this.suggest, 'setCache');
            this.callSuggestSource();

            expect(this.suggest.setCache.calledWith('[{"param.0":"some-param","q.0":"test"}]', this.result)).to.be.ok;
        });

        it('Должен вызвать рендринг данных после получения ответов от хэндлеров', function() {
            this.callSuggestSource();
            expect(this.response.calledWith(this.result)).to.be.ok;
        });
    });

    describe('#_select',  function() {
        beforeEach(function() {
            this.suggest.setOptions({
                multiple: true
            });
            this.suggest.nbSuggest.setValue('Term1, T');
            this.eventObj.isDefaultPrevented = this.sinon.stub().returns(false);
            this.selectedData = {
                item: {
                    value: 'Term2'
                }
            };
        });

        it('Должен установить в false флаг #dontClose', function() {
            this.suggest._select(this.eventObj, this.selectedData);
            expect(this.suggest.dontClose).to.be.equal(false);
        });

        it('Должен добавлять при множественном вводе в поле выбранных элемент из саджеста', function() {
            this.suggest._select(this.eventObj, this.selectedData);

            expect(this.suggest.nbSuggest.getValue()).to.be.equal('Term1, Term2, ');
        });

        it('Должен не выполняться, если произошел preventDefault у события', function() {
            this.eventObj.isDefaultPrevented.returns(true);
            this.suggest._select(this.eventObj, this.selectedData);

            expect(this.suggest.nbSuggest.getValue()).to.be.equal('Term1, T');
        });
    });

    describe('#_focus',  function() {
        it('Должен не давать выводить в поле ввода значение саджеста в фокусе до его выбора', function() {
            this.suggest._focus(this.eventObj);

            expect(this.eventObj.preventDefault).to.have.callCount(1);
        });
    });

    describe('#_response', function() {
        beforeEach(function() {
            this.sinon.stub(this.suggest, 'getValueWithPrevious').returns('test1, test2');
            this.ui = {
                content: [{
                    value: 'test1'
                }, {
                    value: 'test2'
                }, {
                    value: 'test3'
                }]
            };
            this.suggest.setOptions({
                multiple: true
            });
        });

        it('Должен не исключать данные, если это первый вводимый элемент', function() {
            this.ui = {
                content: [{
                    value: 'test3'
                }]
            };
            this.suggest.getValueWithPrevious.returns('test3');
            this.suggest._response(this.eventObj, this.ui);
            expect(this.ui.content.length === 1 && this.ui.content[0].value === 'test3').to.be.ok;
        });

        it('Должен исключить данные из саджеста, если они уже выбраны', function() {
            this.suggest._response(this.eventObj, this.ui);
            expect(this.ui.content.length === 1 && this.ui.content[0].value === 'test3').to.be.ok;
        });

        it('Должен записать данные саджеста в #currentContent', function() {
            this.suggest._response(this.eventObj, this.ui);
            expect(this.suggest.currentContent).to.be.equal(this.ui.content);
        });

        it('Должен записать пустой массив в #currentContent, если данные саджеста отсутствуют', function() {
            this.suggest._response(this.eventObj, {
                content: []
            });
            expect(this.suggest.currentContent).to.eql([]);
        });
    });

    describe('#_close', function() {
        beforeEach(function() {
            this.suggest.currentContent = [13, 23, 33];
            this.suggest.currentTerm = '3';
            this.suggest._close();
        });

        it('Должен установить в false флаг #dontClose', function() {
            expect(this.suggest.dontClose).to.be.equal(false);
        });

        it('Должен установить в false флаг #isInputFieldBlur', function() {
            expect(this.suggest.isInputFieldBlur).to.be.equal(false);
        });

        it('Должен очистить данные в #currentContent', function() {
            expect(this.suggest.currentContent).to.eql([]);
        });

        it('Должен очистить данные в #currentTerm', function() {
            expect(this.suggest.currentTerm).to.eql('');
        });
    });

    describe('#abort', function() {
        beforeEach(function() {
            var deferred = $.Deferred();
            deferred.abort = _.noop;
            this.suggest.requests = [deferred];
            this.deferred = deferred;
        });

        it('Должен остановить все активные запросы', function() {
            this.suggest.abort();

            expect(this.suggest.requests).to.be.equal(undefined);
        });

        it('Должен вызвать abort метод у запросов', function() {
            this.sinon.spy(this.deferred, 'abort');
            this.suggest.abort();

            expect(this.deferred.abort).to.have.callCount(1);
        });

        it('Должен отработать без эксепшенов при отсутствии abort методов у запросов', function() {
            var that = this;
            this.suggest.requests.push($.Deferred());

            expect(function() {
                that.suggest.abort();
            }).to.not.throw();
        });

        itShouldBeChaining('abort');
    });

    describe('#destroy', function() {
        it('Должен остановить запросы к хэндлерам', function() {
            this.sinon.stub(this.suggest, 'abort');
            this.suggest.destroy();

            expect(this.suggest.abort).to.have.callCount(1);
        });

        it('Должен вызвать действия при _close', function() {
            this.sinon.stub(this.suggest, '_close');
            this.suggest.destroy();

            expect(this.suggest._close).to.have.callCount(1);
        });

        it('Должен удалить островной саджест, если он был создан', function() {
            var nbSuggest = this.suggest.nbSuggest;
            this.sinon.spy(nbSuggest, 'destroy');
            this.suggest.destroy();
            expect(nbSuggest.destroy).to.have.callCount(1);
        });

        it('Должен не вызывать удаление островного саджеста, если он не был создан', function() {
            var suggest = new Daria.Suggest({});
            expect(function() {
                suggest.destroy();
            }).to.not.throw();
        });

        it('Должен отписывать поле ввода от событий саджеста (.daria-suggest)', function() {
            this.sinon.stub(this.suggest.$inputField, 'off');
            this.suggest.destroy();

            expect(this.suggest.$inputField.off.calledWith('.daria-suggest')).to.be.ok;
        });

        it('Должен отписывать document от событий саджеста (.daria-suggest)', function() {
            this.sinon.stub(this.suggest.$document, 'off');
            this.suggest.destroy();

            expect(this.suggest.$document.off.calledWith('.daria-suggest')).to.be.ok;
        });

        it('Должен отписать все обработчики от событий приложения', function() {
            this.suggest._janeEvents = {
                'some-event1': _.noop,
                'some-event2': _.noop
            };

            this.sinon.spy(this.suggest, 'unbindJaneEvent');
            this.suggest.destroy();
            expect(this.suggest.unbindJaneEvent.calledTwice).to.be.ok;
        });

        itShouldBeChaining('destroy');
    });

    describe('#setCache', function() {
        it('Должен поместить заданные данные в кэш', function() {
            var key = 'some-key';
            var data = {
                someData: 'value'
            };
            this.suggest.setCache(key, data);
            expect(this.suggest.cache[key]).to.be.equal(data);
        });

        it('Должен при переполнении кэша его очищать и вносить переданное значение', function() {
            this.suggest
                .setCache('some-key1', {})
                .setCache('some-key2', {})
                .setCache('some-key3', {});
            expect(this.suggest.cache).to.eql({
                'some-key3': {}
            });
        });

        itShouldBeChaining('setCache', 'some-key', {});
    });

    describe('#getCache', function() {
        it('Должен возвращать данные из кэша, если они есть', function() {
            var key = 'some-key';
            var data = {
                someData: 'value'
            };
            this.suggest.setCache(key, data);
            expect(this.suggest.getCache(key)).to.be.equal(data);
        });
    });

    describe('#clearCache', function() {
        it('Должен очищать кэш', function() {
            var key = 'some-key';
            var data = {
                someData: 'value'
            };
            this.suggest
                .setCache(key, data)
                .clearCache();
            expect(this.suggest.cache).to.eql({});
        });

        itShouldBeChaining('clearCache');
    });

    describe('#focusedSelect', function() {
        beforeEach(function() {
            this.sinon.stub(this.suggest.$inputField, 'trigger');
            this.sinon.stub(this.suggest.options, 'select');
        });

        it('Должен триггерить событие на инпуте, если список не пустой', function() {
            this.suggest.currentContent = [1];
            this.suggest.focusedSelect();

            var triggerEvent = this.suggest.$inputField.trigger.getCall(0).args[0];

            expect(this.suggest.options.select).to.have.callCount(0);
            expect(triggerEvent && triggerEvent.which === this.KEY.ENTER).to.be.ok;
        });

        it('Должен напрямую вызывать options.select если список пустой', function() {
            this.suggest.currentContent = [];
            this.suggest.focusedSelect();

            var triggerEvent = this.suggest.options.select.getCall(0).args[0];

            expect(this.suggest.$inputField.trigger).to.have.callCount(0);
            expect(triggerEvent && triggerEvent.which === this.KEY.ENTER).to.be.ok;
        });

        it('Должен передавать в options.select текущую введенную строку', function() {
            var term = 'test';

            this.suggest.currentTerm = term;
            this.suggest.currentContent = [];
            this.suggest.focusedSelect();

            var eventData = this.suggest.options.select.getCall(0).args[1];

            expect(eventData).to.be.eql({
                item: {
                    email: term,
                    value: term
                }
            });
        });

    });
});
