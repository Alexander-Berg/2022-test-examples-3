describe('b-href-control', function() {
    var block,
        createBlock = function(params) {
            block = u.createBlock({
                block: 'b-href-control',
                protocol: params.protocol,
                limit: params.limit,
                href: params.href
            });
        },
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeServer: true, useFakeTimers: true });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('У блока есть элемент hint', function() {
        createBlock({ protocol: 'https://', href: 'ya.ru' });

        expect(block).to.haveElem('hint');
    });

    it('Счетчик оставшихся символов учитывает протокол', function() {
        var params = { protocol: 'https://', href: 'ya.ru', limit: 50 };
        createBlock(params);

        expect(+block.findBlockOn('href', 'input').elem('counter').text())
            .to.be.eq(params.limit - (params.protocol + params.href).length);
    });

    // Два поля ввода, протокол и ссылка, их можно задать в контексте блока
    // при изменении любого из них запускается валидация
    // валидация синхронная, в начале и конце валидации триггерится событие state:change с параметром isReady
    // во время валидации и при ошибке выставляются соответствующие модификаторы
    // метод val возвращает информацию о введённой ссылке (объект)
    describe('Значения полей "протокол" и "url" устанавливаются в контексте и возвращаются методом val', function(){
        it('Несуществующий протокол, должен заменяться на "http"', function() {
            createBlock({ protocol: 'abc', href: 'ya.ru' });

            expect(block.val().protocol).to.be.equal('https://');
        });

        it('Если не указан href, val возвращает undefined', function() {
            createBlock({ protocol: 'https://' });

            expect(block.val()).to.be.undefined;
        });

        it('Существующий протокол', function() {
            createBlock({ protocol: 'https://', href: 'ya.ru' });

            expect(block.val().protocol).to.be.equal('https://');
        });

        it('url', function() {
            createBlock({ href: 'blizzard.com' });

            expect(block.val().href).to.be.equal('blizzard.com');
        });

        it('метод val возвращает href_domain', function() {
            createBlock({ href: 'direct.yandex.ru/commander' });

            expect(block.val().href_domain).to.be.equal('direct.yandex.ru');
        });

        it('метод val возвращает href', function() {
            createBlock({ href: 'direct.yandex.ru/commander' });

            expect(block.val().href).to.be.equal('direct.yandex.ru/commander');
        });

        it('Метод val возвращает href без пробелов', function() {
            createBlock({ href: ' ya.ru' });

            expect(block.val().href).to.equal('ya.ru');
        });

        it('метод val возвращает url', function() {
            createBlock({ href: 'direct.yandex.ru/commander' });

            expect(block.val().url).to.be.equal('https://direct.yandex.ru/commander');
        });

        it('формат результата метода val', function() {
            createBlock({ protocol: 'https://', href: 'direct.yandex.ru/commander' });

            expect(block.val()).to.have.all.keys(['protocol', 'href_domain', 'href', 'url']);
        });

        it('метод val не должен обрезать протокол у get парметров', function() {
            createBlock({ href: 'direct.yandex.ru/commander?param=https://ya.ru' });

            expect(block.val().url).to.be.equal('https://direct.yandex.ru/commander?param=https://ya.ru');
        });

        it('метод val не должен обрезать протокол у get парметров', function() {
            createBlock({ href: 'https://direct.yandex.ru/commander?param=https://ya.ru' });

            expect(block.val().url).to.be.equal('https://direct.yandex.ru/commander?param=https://ya.ru');
        });

    });

    describe('Валидация', function() {
        beforeEach(function() {
            createBlock({ protocol: 'https://', href: 'direct.yandex.ru/commander' });
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"},
                JSON.stringify([{ "requestId": 1, code: 1 }, { "requestId": 2, code: 1 },
                    { "requestId": 3, code: 1 }, { "requestId": 4, code: 1 } ])]);
        });

        afterEach(function() {
            block.destruct();
            sandbox.restore();
        });

        it('При пустой ссылке валидация проходит', function() {
            block._href.val('');

            // ожидание debounce у deferred
            sandbox.clock.tick(2000);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block).not.to.haveMod('state');
            expect(block._hint.text()).to.equal('');
        });

        it('При неправильном формате ссылки выдается ошибка', function() {
            block._href.val('fff');

            sandbox.clock.tick(100);
            expect(block._hint.text()).to.equal('Неправильный формат ссылки');
        });

        it('При превышении длины ссылки выдается ошибка', function() {
            var length = u.consts('MAX_URL_LENGTH');
            block._href.val('www.yandex' + (new Array(length)).join('x') + '.com.tr');

            sandbox.clock.tick(100);
            expect(block._hint.text()).to.equal('Максимальная длина ссылки с учетом протокола (' + length + ') превышена');
        });

        it('При правильной ссылке ставится текст хинта', function() {
            block._href.val('www.yandex.com.tr');

            sandbox.clock.tick(100);
            expect(block._hint.text()).to.equal('Проверка ссылки');
        });

        it('При правильной ссылке валидация проходит', function() {
            block._href.val('www.yandex.com.tr');

            // ожидание debounce у deferred
            sandbox.clock.tick(2000);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block).not.to.haveMod('state');
            expect(block._hint.text()).to.equal('');
        });

        it('После валидации текст ссылки не меняется', function() {
            var newHref = 'www.yandex.com.tr';
            block.findBlockOn('href', 'input').val(newHref);

            // ожидание debounce у deferred
            sandbox.clock.tick(2000);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block.findBlockOn('href', 'input').val()).to.equal(newHref);
        });

        it('После валидации протокол не меняется', function() {
            block.findBlockOn('href', 'input').val('www.yandex.com.tr');

            // ожидание debounce у deferred
            sandbox.clock.tick(2000);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block.findBlockOn('protocol', 'select').val()).to.equal('https://');
        });
    });

    describe('Поведение блока', function() {
        beforeEach(function() {
            createBlock({ protocol: 'https://', href: 'direct.yandex.ru/commander' });

            sandbox.server.respondWith("POST", '/registered/main.pl', [200, {"Content-Type":"application/json"},
                JSON.stringify([
                    { "requestId": 1, code: 1 }, { "requestId": 2, code: 1 },
                    { "requestId": 3, code: 1 }, { "requestId": 4, code: 1 }, { "requestId": 5, code: 1 },
                    { "requestId": 6, code: 1 }, { "requestId": 7, code: 1 } ])]);
        });

        it('При начале проверки триггерится isReady: false', function() {
            sandbox.spy(block, 'trigger');

            block._href.val('www.yandex.com.tr');
            sandbox.clock.tick(200);

            expect(block.trigger.calledWith('state:changed', { isReady: false, alerts: [], validatedData: undefined })).to.equal(true);
        });

        it('При ошибке в проверке триггерится isReady: false', function() {
            sandbox.spy(block, 'trigger');

            block._href.val('fff');
            sandbox.clock.tick(100);

            expect(block.trigger.calledWith('state:changed', { isReady: false, alerts: ["Неправильный формат ссылки"], validatedData: undefined })).to.equal(true);
        });

        it('При прохождении проверки триггерится isReady: true', function() {
            // Событие триггерится дважды - в начале и конце проверки

            expect(block).to.triggerEvent(
                'state:changed',
                { isReady: false, alerts: [], validatedData: undefined },
                function() {
                    block._href.val('www.yandex.com.tr');
                    // ожидание debounce у deferred
                    sandbox.clock.tick(2000);
                }
            );

            expect(block).to.triggerEvent(
                'state:changed',
                { isReady: true, alerts: [], validatedData: {code: 1, requestId: 6 } },
                function() {
                    sandbox.server.respond();
                    sandbox.clock.tick(100);
                }
            );
        });

        it('Счетчик оставшихся символов меняется с учетом протокола', function() {
            var params = { protocol: 'https://', href: 'ya.ru', limit: 50 },
                newHref = 'direct.ya.ru';
            createBlock(params);
            block.findBlockOn('href', 'input').val(newHref);

            expect(+block.findBlockOn('href', 'input').elem('counter').text()).to.be.eq(params.limit - (params.protocol + newHref).length);
        });

        it('Счетчик оставшихся символов меняется с учетом протокола при смене href', function() {
            var params = { protocol: 'https://', href: 'ya.ru', limit: 50 },
                newHref = 'direct.ya.ru';
            createBlock(params);
            block.findBlockOn('href', 'input').val(newHref);

            expect(+block.findBlockOn('href', 'input').elem('counter').text()).to.be.eq(params.limit - (params.protocol + newHref).length);
        });

        it('Счетчик оставшихся символов меняется с учетом протокола при смене протокола', function() {
            var params = { protocol: 'https://', href: 'ya.ru', limit: 50 },
                newProtocol = 'http://';
            createBlock(params);
            block.findBlockOn('protocol', 'select').val(newProtocol);

            expect(+block.findBlockOn('href', 'input').elem('counter').text())
                .to.be.eq(params.limit - (newProtocol + params.href).length);
        });

    });
});

