describe('Request', function() {

    beforeEach(function() {
        this.options = {
            type: 'post',
            url: 'https://some-domain.com/save',
            params: {
                getParam1: '123',
                getParam2: '456'
            },
            body: {
                test1: 'test1',
                test2: 'test2'
            }
        };
        this.events = {
            onStart: this.sinon.stub(),
            onSuccess: this.sinon.stub(),
            onError: this.sinon.stub()
        };
    });

    describe('Настройка запроса ->', function() {
        it('должен установить опции по умолчанию, если они не были заданы', function() {
            this.request = new WidgetSaveApi.Request();

            expect(this.request.options).to.be.deep.equal({
                type: 'GET'
            });
        });

        it('должен уставноить обработчики событий по умолчанию, если они не были заданы', function() {
            this.request = new WidgetSaveApi.Request();

            expect(this.request.events).to.be.deep.equal({});
        });

        [
            {
                url: 'https://some-domain.com/',
                expectation: 'https://some-domain.com/?getParam1=123&getParam2=456'
            },
            {
                url: 'https://some-domain.com/?test=5',
                expectation: 'https://some-domain.com/?test=5&getParam1=123&getParam2=456'
            },
            {
                url: 'https://some-domain.com/save',
                expectation: 'https://some-domain.com/save?getParam1=123&getParam2=456'
            },
            {
                url: 'https://some-domain.com/save?test=5',
                expectation: 'https://some-domain.com/save?test=5&getParam1=123&getParam2=456'
            }
        ].forEach(function(data) {
            it('должен корректно дополнить URL ' + data.url + ' GET параметрами', function() {
                this.options.url = data.url;
                this.request = new WidgetSaveApi.Request(this.options);

                expect(this.request.options.url).to.be.equal(data.expectation);
            });
        });

        it('должен кодировать GET параметры', function() {
            this.options.params = {
                test: '???'
            };

            this.request = new WidgetSaveApi.Request(this.options);

            expect(this.request.options.url).to.be.equal('https://some-domain.com/save?test=%3F%3F%3F');
        });

        it('должен сообщить об ошибке, если не был указан URL запроса', function(done) {
            var options = delete this.options.url;
            this.request = new WidgetSaveApi.Request(options, {
                onError: function(error) {
                    expect(error).to.be.deep.equal({invalidOption: 'url'});
                    done();
                }
            });
        });

        it('должен по умолчанию установить тип запроса - GET', function() {
            delete this.options.type;
            this.request = new WidgetSaveApi.Request(this.options);

            expect(this.request.options.type).to.be.equal('GET');
        });

        it('должен установить переданный тип запроса', function() {
            this.request = new WidgetSaveApi.Request(this.options);

            expect(this.request.options.type).to.be.equal('POST');
        });

        it('должен сформировать urlencode тело запроса, если это POST запрос', function() {
            var parsedBody = 'test1=test1&test2=test2';
            this.request = new WidgetSaveApi.Request(this.options);

            expect(this.request.options.parsedBody).to.be.equal(parsedBody);
        });

        it('должен стереть тело запроса, если это не POST запрос', function() {
            this.options.type = 'get';
            this.request = new WidgetSaveApi.Request(this.options);

            expect(this.request.options.parsedBody).to.be.equal(undefined);
        });

    });

    describe('Отправка запроса ->', function() {

        beforeEach(function() {
            this.request = new WidgetSaveApi.Request(this.options, this.events);
        });

        it('не должен отправлять запрос, если на этапе настройки произошла ошибка', function() {
            this.request.error = {};
            this.request.send();

            expect(this.sinon.server.requests).to.have.length(0);
        });

        it('должен отправить запрос заданного типа', function() {
            this.request.send();

            expect(this.sinon.server.requests[0].method).to.be.equal('POST');
        });

        it('должен оправить запрос на указанный URL с прибавкой GET параметров', function() {
            this.request.send();

            expect(this.sinon.server.requests[0].url).to.be.equal('https://some-domain.com/save?getParam1=123&getParam2=456');
        });

        it('должен отправить POST запрос с `Content-Type: application/x-www-form-urlencoded;charset=utf-8`', function() {
            this.request.send();

            expect(this.sinon.server.requests[0].requestHeaders['Content-Type']).to.be.equal('application/x-www-form-urlencoded;charset=utf-8');
        });

        it('должен отправить POST запрос с укзанными данными в теле запроса', function() {
            this.request.send();

            expect(this.sinon.server.requests[0].requestBody).to.be.equal('test1=test1&test2=test2');
        });

    });

    describe('Обработка успешного ответа на запрос ->', function() {

        beforeEach(function() {
            this.response = [
                200,
                {'Content-type': 'application/json;charset=utf-8'},
                '{"data":{"hello": "world"}}'
            ];
            this.request = new WidgetSaveApi.Request(this.options, this.events);
            this.sinon.server.respondWith('POST', this.request.options.url, this.response);
        });

        it('должен вызвать onSuccess событие, если оно есть', function() {
            this.request.send();
            this.sinon.server.respond();

            expect(this.events.onSuccess).to.be.called;
        });

        it('должен передать данные в onSuccess событие описанного формата', function() {
            this.request.send();
            this.sinon.server.respond();

            expect(this.events.onSuccess.getCall(0).args[0]).to.be.deep.equal({
                status: 'ok',
                code: 200,
                data: {
                    hello: 'world'
                }
            });
        });

        it('должен передать данные в onError событие, если пришло свойство .error в ответе', function() {
            this.response[2] = '{"error": {"status": "412", "code": "39"}}';
            this.request.send();
            this.sinon.server.respond();
            expect(this.events.onError.getCall(0).args[0]).to.be.deep.equal({
                status: '412',
                code: '39'
            });
            expect(this.events.onSuccess).not.to.be.called;
        });

        it('не должен вызвать ошибку, если обработчика onSuccess нет', function() {
            this.request.events = {};

            expect(function() {
                this.request.send();
                this.sinon.server.respond();
            }.bind(this)).to.be.not.throw(Error);
        });

    });

    describe('Обработка ошибок ->', function() {

        beforeEach(function() {
            this.request = new WidgetSaveApi.Request(this.options, this.events);
        });

        // Ошибки HTTP ответа
        [
            {
                response: [301, {'Content-type': 'application/json;charset=utf-8'}, '{"hello": 301}'],
                expectation: {
                    httpCode: 301
                }
            },
            {
                response: [400, {'Content-type': 'application/json;charset=utf-8'}, '{"hello": 400}'],
                expectation: {
                    hello: 400
                }
            }
            // Ошибки, для которых идентичное поведение
        ].concat([401, 403, 404, 500, 501, 502, 503, 504].map(function(code) {
            return {
                response: [code, {'Content-type': 'application/json;charset=utf-8'}, '{"hello": ' + code + '}'],
                expectation: {
                    code: 0
                }
            };
        })).forEach(function(data) {

            it('должен вызвать onError обработчик', function() {
                this.sinon.server.respondWith('POST', this.request.options.url, data.response);
                this.request.send();
                this.sinon.server.respond();

                expect(this.events.onError).to.be.called;
            });

            it('должен передать данные в onError событие, вызванное HTTP кодом `' + data.response[0] + '`', function() {
                this.sinon.server.respondWith('POST', this.request.options.url, data.response);
                this.request.send();
                this.sinon.server.respond();

                expect(this.events.onError.getCall(0).args[0]).to.be.deep.equal(data.expectation);
            });

            it('не должен вызвать ошибку, если обработчика onSuccess нет', function() {
                this.sinon.server.respondWith('POST', this.request.options.url, data.response);
                this.request.events = {};

                expect(function() {
                    this.request.send();
                    this.sinon.server.respond();
                }.bind(this)).to.be.not.throw(Error);
            });
        });

        it('должен вызвать событие начала отправки запроса', function() {
            var okResponse = [
                200,
                {'Content-type': 'application/json;charset=utf-8'},
                '{"hello": "world"}'
            ];
            this.sinon.server.respondWith('POST', this.request.options.url, okResponse);
            this.request.send();

            expect(this.events.onStart).to.be.calledWithExactly({
                status: 'started'
            });
        });

        xit('должен вызывать ошибку, если превышен timeout ожидания ответа', function() {
            // FIXME - sinon не умеет эмулировать родной timeout у XHR. Нужно решить этот момент
        });

    });

    describe('Прерывание запроса ->', function() {

        beforeEach(function() {
            var okResponse = [
                200,
                {'Content-type': 'application/json;charset=utf-8'},
                '{"hello": "world"}'
            ];

            this.request = new WidgetSaveApi.Request(this.options, this.events);
            this.sinon.server.respondWith('POST', this.request.options.url, okResponse);
            this.request.send();
            this.sinon.stub(this.request.xhr, 'abort');
        });

        it('должен вызвать `abort` запроса', function() {
            this.request.abort();
            expect(this.request.xhr.abort).to.be.called;
        });

        it('не должен вызывать обработчик ошибки', function() {
            this.request.abort();
            expect(this.events.onError).not.to.be.called;
        });

        it('не должен вызывать `abort`, если запрос был получен', function() {
            this.sinon.server.respond();
            this.request.abort();

            expect(this.request.xhr.abort).not.to.be.called;
        });

    });

});
