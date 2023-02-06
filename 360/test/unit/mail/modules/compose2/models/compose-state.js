describe('Daria.mComposeState', function() {
    beforeEach(function() {
        ns.Model.get('account-information').setData(mock['account-information'][0].data);
        setModelByMock(ns.Model.get('signs'));

        this.mMessageBody = ns.Model.get('message-body');
        this.mComposeMessage = ns.Model.get('compose-message');
        this.mComposeMessage._initFromModels([ {
            id: 'compose-predefined-data',
            getData: this.sinon.stub(),
            destroy: this.sinon.stub()
        } ]);
        this.model = ns.Model.get('compose-state');
        this.model.setData({});
    });

    describe('#request', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'request').callsFake(() => vow.reject());
            this.sinon.stubMethods(this.model, [
                '_constructModelRequest',
                '_initFromModels'
            ]);
        });

        it('Конструирует параметры и запрашивает с ними модели', function() {
            this.model._constructModelRequest.returns({ test: '42' });

            this.model.request();

            expect(ns.request).to.be.calledWith({ test: '42' });
        });

        it('Запускает инициализацию моделей после запроса', function() {
            ns.request.returns(vow.resolve([ 'model1', 'model2' ]));

            return this.model.request()
                .then(function() {
                    expect(this.model._initFromModels).to.be.calledWith([ 'model1', 'model2' ]);
                }, this);
        });
    });

    describe('#_initFromModels', function() {
        beforeEach(function() {
            this.sinon.stubMethods(this.model, [ 'setData' ]);

            this.sinon.stub(this.mComposeMessage, 'get');

            this.runMethod = function() {
                this.model._initFromModels([ this.mComposeMessage, this.mMessageBody ]);
            };
        });

        describe('Правильно инициализирует поля', function() {
            beforeEach(function() {
                this.getCallData = function() {
                    return this.model.setData.getCall(0).args[0];
                };
            });

            it('Видимость поля cc (isCcVisible)', function() {
                this.mComposeMessage.get.withArgs('.cc').returns('42');

                this.runMethod();

                expect(this.getCallData().isCcVisible).to.be.equal(true);
            });

            it('Видимость поля cc (isBccVisible)', function() {
                this.mComposeMessage.get.withArgs('.bcc').returns('42');

                this.runMethod();

                expect(this.getCallData().isBccVisible).to.be.equal(true);
            });

            describe('Напоминание об ответе в случае возможности ответа всем (isReplyAllNoticeVisible) →', function() {
                beforeEach(function() {
                    this.sinon.stub(this.mMessageBody, 'allowedReplyAll');
                });

                it('Это ответ на письмо, и можно ответить всем в оригинальном письме', function() {
                    this.sinon.stub(this.model, 'params').value({ oper: 'reply' });
                    this.mMessageBody.allowedReplyAll.returns(true);

                    this.runMethod();

                    expect(this.getCallData().isReplyAllNoticeVisible).to.be.equal(true);
                });

                it('Это ответ всем на письмо, и можно ответить всем в оригинальном письме', function() {
                    this.sinon.stub(this.model, 'params').value({ oper: 'reply-all' });
                    this.mMessageBody.allowedReplyAll.returns(true);

                    this.runMethod();

                    expect(this.getCallData().isReplyAllNoticeVisible).to.be.equal(false);
                });

                it('Это ответ на письмо, и нельзя ответить всем в оригинальном письме', function() {
                    this.sinon.stub(this.model, 'params').value({ oper: 'reply' });
                    this.mMessageBody.allowedReplyAll.returns(false);

                    this.runMethod();

                    expect(this.getCallData().isReplyAllNoticeVisible).to.be.equal(false);
                });
            });
        });
    });

    describe('#set', function() {
        beforeEach(function() {
            this.model.set('.test', 'test');
            this.sinon.spy(ns.Model.prototype, 'set');
            this.sinon.spy(this.model, 'trigger');
        });

        it('не должен устанавливать новое значение, если оно равно текущему', function() {
            this.model.set('.test', 'test');

            expect(ns.Model.prototype.set).to.have.callCount(0);
        });

        it('не должен вызывать событие ns-model-changed, если новое значение равно текущему', function() {
            this.model.set('.test', 'test');

            expect(this.model.trigger).to.have.callCount(0);
        });

        it('должен установить новое значение, если оно отличается от текущего', function() {
            this.model.set('.test', 'new');

            expect(ns.Model.prototype.set).to.have.callCount(1);
        });

        it('должен вызывать событие ns-model-changed, если новое значение не равно текущему', function() {
            this.model.set('.test', 'new');

            expect(this.model.trigger).to.be.calledWith('ns-model-changed.test');
        });

        it('должен установить новое занчение, если оно равно текущему, когда передан параметр force', function() {
            this.model.set('.test', 'test', {
                force: true
            });

            expect(ns.Model.prototype.set).to.be.calledWithExactly('.test', 'test', { force: true, jpath: '.test' });
        });

        it('должен вызывать событие ns-model-changed, если новое значение не равно текущему', function() {
            this.model.set('.test', 'test', {
                force: true
            });

            expect(this.model.trigger).to.be.calledWith('ns-model-changed.test');
        });
    });

    describe('#setMessageHeight', function() {
        it('Должен установить значение высоты поля ввода текста письма', function() {
            this.model.setMessageHeight(300);

            expect(this.model.get('.messageHeight')).to.be.equal(300);
        });

        it('Должен установить значение высоты поля ввода текста письма в 150 пикселей, если устанавливаемая величина меньше', function() {
            this.model.setMessageHeight(100);

            expect(this.model.get('.messageHeight')).to.be.equal(190);
        });
    });

    describe('#hasSpellingCheck', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'product').value('RUS');
            this.sinon.stub(Daria.Config, 'locale').value('ru');
        });

        it('должен разрешить вывести кнопку проверки орфографии для не TUR продукта и локалей, отличных от ka, hy, ro локалей', function() {
            expect(this.model.hasSpellingCheck()).to.be.equal(true);
        });

        it('должен запретить выводить кнопку проверки орфографии для TUR продукта', function() {
            this.sinon.stub(Daria.Config, 'product').value('TUR');

            expect(this.model.hasSpellingCheck()).to.be.equal(false);
        });

        [ 'ka', 'hy', 'ro' ].forEach(function(locale) {
            it('должен запретить выводить кнопку проверки орфографии для ' + locale + ' локали', function() {
                this.sinon.stub(Daria.Config, 'locale').value(locale);

                expect(this.model.hasSpellingCheck()).to.be.equal(false);
            });
        });
    });

    describe('#hasEnSpellingCheckIcon', function() {
        it('должен разрешить выводить английскую иконку проверки орфографии, если текущая локаль не ru, uk, be', function() {
            this.sinon.stub(Daria.Config, 'locale').value('ka');

            expect(this.model.hasEnSpellingCheckIcon()).to.be.equal(true);
        });

        [ 'ru', 'uk', 'be' ].forEach(function(locale) {
            it('должен запретиь выводить английскую иконку проверки орфографии, если текущая локаль ' + locale, function() {
                this.sinon.stub(Daria.Config, 'locale').value(locale);

                expect(this.model.hasEnSpellingCheckIcon()).to.be.equal(false);
            });
        });
    });

    describe('#setLastSaveTime', function() {
        beforeEach(function() {
            this.sinon.stub(Jane.Date, 'format');
            this.sinon.stub(this.model, 'set');
        });

        it('Правильно формирует строку и задает данные', function() {
            Jane.Date.format.returns('42:test');

            this.model.setLastSaveTime();

            expect(this.model.set).calledWith('.lastSaveTime', '42:test');
        });
    });

    describe('#_refreshSubmitButtonText', function() {
        it('Должен вызвать событие изменения submitButtonText при изменении translationEnabled', function() {
            this.model._initFromModels([ this.mComposeMessage, this.mMessageBody ]);
            var spy = this.sinon.spy();
            this.model.on('ns-model-changed.submitButtonText', spy);
            this.model.set('.translationEnabled', true);

            expect(spy).to.have.callCount(1);
        });

        it('Должен вызвать событие изменения submitButtonText при изменении send_time модели compose-message', function() {
            this.model._initFromModels([ this.mComposeMessage, this.mMessageBody ]);
            this.mComposeMessage.setData({});
            var spy = this.sinon.spy();
            this.model.on('ns-model-changed.submitButtonText', spy);
            this.mComposeMessage.set('.send_time', 1467102720000);

            expect(spy).to.have.callCount(1);
        });
    });

    describe('#getSubmitButtonText', function() {
        beforeEach(function() {
            this.model.mComposeMessage = this.mComposeMessage;

            this.sinon.stub(this.model, 'get');
            this.sinon.stub(this.mComposeMessage, 'getPassportSendDate');
        });

        describe('Отложенная отправка', function() {
            beforeEach(function() {
                this.mComposeMessage.getPassportSendDate.returns('passport time');

                this.sinon.stub(Jane.Date, 'toHumanFormat').withArgs('passport time').returns('ДАТА');
                this.sinon.stub(Jane.Date, 'format').withArgs('%Date_HM__colon', 'passport time').returns('ВРЕМЯ');
            });

            it('должен вернуть корректный текст для кнопки отправки перевода письма', function() {
                this.model.get.withArgs('.translationEnabled').returns(true);
                expect(this.model.getSubmitButtonText()).to.be.equal('Отправить перевод ДАТА в ВРЕМЯ');
            });

            it('должен вернуть корректный текст для кнопки отправки письма', function() {
                this.model.get.withArgs('.translationEnabled').returns(false);
                expect(this.model.getSubmitButtonText()).to.be.equal('Отправить ДАТА в ВРЕМЯ');
            });
        });

        describe('Обычная отправка', function() {
            beforeEach(function() {
                this.mComposeMessage.getPassportSendDate.returns(undefined);
            });

            it('должен вернуть корректный текст для кнопки (переводчик активен)', function() {
                this.model.get.withArgs('.translationEnabled').returns(true);
                expect(this.model.getSubmitButtonText()).to.be.equal('Отправить перевод');
            });

            it('должен вернуть корректный текст для кнопки (переводчик не активен)', function() {
                this.model.get.withArgs('.translationEnabled').returns(false);
                expect(this.model.getSubmitButtonText()).to.be.equal('Отправить');
            });
        });
    });
});
