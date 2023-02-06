/* global buildDomBlock:true */
describe('unread-letters-updater', function() {
    var block,
        bemjson,
        channelTrigger,
        channelOn;

    function paramsForEach(data, jsParams) {
        return function() {
            channelTrigger = sinon.stub();
            channelOn = sinon.stub();
            bemjson = { block: 'unread-letters-updater', js: jsParams };

            sinon.stub(BEM, 'create').withArgs('i-request_type_ajax').returns({
                get: sinon.stub().callsArgWith(1, { counters: data })
            });

            sinon.stub(BEM, 'channel')
                .withArgs('unread-letters-count').returns({
                    trigger: channelTrigger
                })
                .withArgs('serp-request').returns({
                    on: channelOn
                });

            sinon.stub(BEM.blocks['i-global'], 'param')
                .withArgs('export-host').returns('//export.yandex.ru')
                .withArgs('tld').returns('ru');

            block = buildDomBlock('unread-letters-updater', bemjson);
        };
    }

    afterEach(function() {
        BEM.create.restore();
        BEM.channel.restore();
        BEM.blocks['i-global'].param.restore();

        BEM.DOM.destruct(block.domElem);
    });

    describe('on getting info from server', function() {
        beforeEach(paramsForEach({ unread: 42 }, { hasMailBox: true, enabled: true }));

        it('should use valid url', function() {
            assert.calledWith(
                BEM.create,
                'i-request_type_ajax',
                sinon.match({ url: 'https://mail.yandex.ru/api/v2/serp/counters?silent' })
            );
        });
    });

    describe('on geting non empty mailbox', function() {
        beforeEach(paramsForEach({ unread: 42 }, { hasMailBox: true, enabled: true }));

        it('should send channel message', function() {
            assert.calledWith(channelTrigger, 'update', 42);
        });

        it('should subscribe on serp-request', function() {
            assert.calledWith(channelOn, 'success');
        });
    });

    describe('on geting invalid server respose', function() {
        beforeEach(paramsForEach({}));

        it('shouldn\'t call BEM.channel.trigger', function() {
            assert.notCalled(channelTrigger);
        });
    });

    describe('for users without mailbox', function() {
        beforeEach(paramsForEach({}, { hasMailBox: false, enabled: true }));

        it('should not make ajax request', function() {
            assert.neverCalledWith(
                BEM.create,
                'i-request_type_ajax',
                sinon.match({ url: 'https://mail.yandex.ru/api/v2/serp/counters?silent' })
            );
        });
    });

    describe('with disabled state', function() {
        beforeEach(paramsForEach({}, { hasMailBox: true, enabled: false }));

        it('should not make ajax request', function() {
            assert.neverCalledWith(
                BEM.create,
                'i-request_type_ajax',
                sinon.match({ url: 'https://mail.yandex.ru/api/v2/serp/counters?silent' })
            );
        });
    });
});
