const {
    getMetrikaVisorReportUrl,
    getMetrikaAccessRequestIdmUrl,
    getMetrikaVisorReportArguments,
    getMetrikaInPageReportUrl,
    getWorkflowUrl,
    getTicketUrl,
    getTolokaPoolUrl } = require('../../../../src/shared/utils/urls');

describe('shared/utils/urls', () => {
    describe('getMetrikaVisorReportUrl', () => {
        it('должен возвращать корректный урл на запись вебвизора', () => {
            const expected = 'https://metrika.yandex.ru/stat/visor?filter=(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527workerTaskKey%2527+and+ym%3As%3AparamsLevel2%3D%3D%25272iqwkdm9ek1567149885021%2527))+and+(ym%3Apv%3AURL%3D*%2527https%3A%2F%2Fsamadhi-layouts.s3.yandex.net%2Fsbs-NiuJIZ%2Findex.html%2527)&id=50967290&period=year';
            assert.equal(getMetrikaVisorReportUrl(50967290, 'ru', { workerTaskKey: '2iqwkdm9ek1567149885021' }, 'https://samadhi-layouts.s3.yandex.net/sbs-NiuJIZ/index.html'), expected);
        });

        it('должен возвращать корректный урл на запись вебвизора, если не передан url страницы', () => {
            const expected = 'https://metrika.yandex.ru/stat/visor?filter=(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527workerTaskKey%2527+and+ym%3As%3AparamsLevel2%3D%3D%25272iqwkdm9ek1567149885021%2527))&id=50967290&period=year';
            assert.equal(getMetrikaVisorReportUrl(50967290, 'ru', { workerTaskKey: '2iqwkdm9ek1567149885021' }), expected);
        });

        it('должен возвращать корректный урл на запись вебвизора, если передано больше одного параметра визита', () => {
            const expected = 'https://metrika.yandex.by/stat/visor?filter=(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527workerTaskKey%2527+and+ym%3As%3AparamsLevel2%3D%3D%2527mo6pmly1atr1567673822172%2527))+and+(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527poolId%2527+and+ym%3As%3AparamsLevel2%3D%3D%2527b066c96bf9e9f43ecce3%2527))&id=50967290&period=year';
            assert.equal(getMetrikaVisorReportUrl(50967290, 'by', { workerTaskKey: 'mo6pmly1atr1567673822172', poolId: 'b066c96bf9e9f43ecce3' }), expected);
        });
    });

    it('getMetrikaAccessRequestIdmUrl: должен возвращать корректный урл на запрос доступа к счетчику', () => {
        const expected = 'https://idm.yandex-team.ru/user/brazhenko/roles#rf=1,rf-role=R5Q4LupP#user:brazhenko@metrika/internal_counter_grant(fields:(counter_id:50967290;grant_type:view);params:()),rf-expanded=R5Q4LupP';
        assert.equal(getMetrikaAccessRequestIdmUrl(50967290, 'brazhenko'), expected);
    });

    it('getMetrikaVisorReportArguments: должен возвращать корректные урлы на запись вебвизора и запрос доступа к счетчику', () => {
        assert.deepEqual(getMetrikaVisorReportArguments({
            counterId: 50967290,
            visitParams: {
                workerTaskKey: 'mo6pmly1atr1567673822172',
                poolId: 'b066c96bf9e9f43ecce3',
            },
        }, 'brazhenko'), {
            reportUrl: 'https://metrika.yandex.by/stat/visor?filter=(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527workerTaskKey%2527+and+ym%3As%3AparamsLevel2%3D%3D%2527mo6pmly1atr1567673822172%2527))+and+(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527poolId%2527+and+ym%3As%3AparamsLevel2%3D%3D%2527b066c96bf9e9f43ecce3%2527))&id=50967290&period=year',
            requestCounterAccessUrl: 'https://idm.yandex-team.ru/user/brazhenko/roles#rf=1,rf-role=R5Q4LupP#user:brazhenko@metrika/internal_counter_grant(fields:(counter_id:50967290;grant_type:view);params:()),rf-expanded=R5Q4LupP',
        });
    });

    describe('getMetrikaInPageReportUrl', () => {
        it('должен возвращать корректный урл на карту кликов', () => {
            const expected = 'http://webvisor.com/inpage/click_map?id=50967290&period=year&tld=by&url=https%3A%2F%2Fsbs.s3.yandex.net%2F3fb746f591dbf456d3fadf5e2cf910700d9d37e6d584bcdd220a100c45feb208%2F7fc77213.html';
            assert.equal(getMetrikaInPageReportUrl('click', 50967290, 'https://sbs.s3.yandex.net/3fb746f591dbf456d3fadf5e2cf910700d9d37e6d584bcdd220a100c45feb208/7fc77213.html'), expected);
        });

        it('должен возвращать корректный урл на карту кликов, если переданы параметры визита', () => {
            const expected = 'http://webvisor.com/inpage/click_map?filter=(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527workerTaskKey%2527+and+ym%3As%3AparamsLevel2%3D%3D%252779sp58b0ct91567673955015%2527))&id=50967290&period=year&tld=by&url=https%3A%2F%2Fsbs.s3.yandex.net%2F3fb746f591dbf456d3fadf5e2cf910700d9d37e6d584bcdd220a100c45feb208%2F7fc77213.html';
            assert.equal(getMetrikaInPageReportUrl('click', 50967290, 'https://sbs.s3.yandex.net/3fb746f591dbf456d3fadf5e2cf910700d9d37e6d584bcdd220a100c45feb208/7fc77213.html', { workerTaskKey: '79sp58b0ct91567673955015' }), expected);
        });
    });

    describe('getWorkflowUrl', () => {
        it('должен возвращать правильную ссылку на граф в Нирване', () => {
            const ID = 'bccf77dc-3568-11e7-89a6-0025909427cc';
            const URL = 'https://nirvana.yandex-team.ru/flow/bccf77dc-3568-11e7-89a6-0025909427cc/graph';

            assert.equal(getWorkflowUrl(ID), URL);
        });
    });

    describe('getTicketUrl', () => {
        it('должен возвращать правильную ссылку на тикет в Трекере', () => {
            const HOST = 'https://st.test.yandex-team.ru/';
            const ID = 1231;
            const URL = 'https://st.test.yandex-team.ru/SIDEBYSIDE-1231';

            assert.equal(getTicketUrl(HOST, ID), URL);
        });
    });

    describe('getTolokaPoolUrl', () => {
        it('должен возвращать правильную ссылку на production-пул в Толоке', () => {
            const fixture = 'https://toloka.yandex.ru/requester/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: false,
                yang: false,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на sandbox-пул в Толоке (.ru)', () => {
            const fixture = 'https://sandbox.toloka.yandex.ru/requester/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: true,
                yang: false,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на sandbox-пул в Толоке (.com)', () => {
            const fixture = 'https://sandbox.toloka.yandex.com/requester/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: true,
                yang: false,
                com: true,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на production-пул в Янге', () => {
            const fixture = 'https://yang.yandex-team.ru/requester/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: false,
                yang: true,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на sandbox-пул в Янге', () => {
            const fixture = 'https://sandbox.yang.yandex-team.ru/requester/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: true,
                yang: true,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на прохождение задания в пуле Толоки', () => {
            const fixture = 'https://toloka.yandex.ru/task/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: false,
                yang: false,
                task: true,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на прохождение задания в пуле Янга', () => {
            const fixture = 'https://sandbox.yang.yandex-team.ru/task/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: true,
                yang: true,
                task: true,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на предпросмотр sandbox-пул в Толоке', () => {
            const fixture = 'https://sandbox.toloka.yandex.ru/requester/assignment-preview/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: true,
                yang: false,
                preview: true,
            }), fixture);
        });

        it('должен возвращать правильную ссылку на production-пул в Толоке', () => {
            const fixture = 'https://toloka.yandex.ru/requester/pool/123';
            assert.equal(getTolokaPoolUrl({
                poolId: '123',
                sandbox: false,
                yang: false,
                preview: false,
            }), fixture);
        });
    });
});
