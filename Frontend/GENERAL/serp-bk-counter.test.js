describe('serp-bk-counter', function() {
    var sandbox,
        block;

    beforeEach(function() {
        sandbox = sinon.createSandbox({
            useFakeTimers: true,
            useFakeServer: true
        });

        BEM.blocks['i-global'].requestPageVisible = function(callback) {
            callback();
        };
    });

    afterEach(function() {
        sandbox.restore();
        delete BEM.blocks['i-global'].requestPageVisible;

        if (block) {
            block.destruct();
            block = null;
        }
    });

    ['global'].forEach(function(type) {
        it('should make ajax request with user credentials for _type_' + type, function() {
            block = createInstance(type);

            // Нужно дождаться завершения асинхронных процессов при инициализации блока
            sandbox.clock.tick(100);

            sandbox.server.respond(
                'GET',
                getCounterUrl(type),
                [200, { 'Content-Type': 'text/html', 'Content-Length': 0 }, '']
            );

            var request = sandbox.server.requests[0];

            // Нужно убедиться, что объект запроса получил статус 200 OK при ответе от сервера
            assert.equal(request.status, 200);
            assert.isTrue(request.withCredentials);
        });

        it('should fallback with request via img.src on ajax request failed for _type_' + type, function() {
            block = createInstance(type);

            sandbox.spy(block, 'executeByImage');

            // Нужно дождаться завершения асинхронных процессов при инициализации блока
            sandbox.clock.tick(100);

            sandbox.server.respond([404, {}, '']);

            assert.calledWith(block.executeByImage, getCounterUrl(type));
        });
    });

    function getCounterUrl(type) {
        return ({
            global: '/count/global/url'
        })[type];
    }

    function createInstance(type) {
        return buildDomBlock('serp-bk-counter', {
            block: 'serp-bk-counter',
            mods: { type: type },
            js: {
                counterUrl: getCounterUrl(type)
            },
            content: '-'
        });
    }
});
