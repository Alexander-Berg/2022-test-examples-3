const urlsUtils = require('../../../src/shared/utils/urls');

const { getMetrikaReportsParams } = require('../../../src/server/utils/metrika-reports-params');

describe('utils/metrika-reports-params', () => {
    const sandbox = sinon.createSandbox();

    const requestCounterAccessUrl = 'https://idm.yandex-team.ru/#rf-role=R5Q4LupP#user:brazhenko@metrika/internal_counter_grant;counter_id:50967290;grant_type:view;;,rf-expanded=R5Q4LupP,rf=1';
    const visorReportUrl = 'https://metrika.yandex.by/stat/visor?filter=(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527workerTaskKey%2527+and+ym%3As%3AparamsLevel2%3D%3D%2527mo6pmly1atr1567673822172%2527))+and+(EXISTS(ym%3As%3AparamsLevel1%3D%3D%2527poolId%2527+and+ym%3As%3AparamsLevel2%3D%3D%2527b066c96bf9e9f43ecce3%2527))&id=50967290&period=year';
    const mapReportUrl = 'http://webvisor.com/inpage/click_map?id=50967290&period=year&tld=by&url=https%3A%2F%2Fsbs.s3.yandex.net%2F3fb746f591dbf456d3fadf5e2cf910700d9d37e6d584bcdd220a100c45feb208%2F7fc77213.html';

    beforeEach(() => {
        sandbox.stub(urlsUtils, 'getMetrikaAccessRequestIdmUrl').returns(requestCounterAccessUrl);
        sandbox.stub(urlsUtils, 'getMetrikaVisorReportUrl').returns(visorReportUrl);
        sandbox.stub(urlsUtils, 'getMetrikaInPageReportUrl').returns(mapReportUrl);
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('getMetrikaReportsParams', () => {
        const login = 'eroshinev';
        const expHash = '1c06a6bedf21d78511f97d777c004983';
        const systems = [
            {
                name: 'старое',
                id: 'sys-0',
            },
            {
                name: 'новое',
                id: 'sys-1',
            },
        ];
        const visitedPages = [
            {
                pages: [
                    'https://sbs.s3.yandex.net/8b2728b20ceb58de43f1327efb3eab50ef3d167f93d7d4472a0dd79702717695/f4a3c252.html',
                    'https://sbs.s3.yandex.net/8b2728b20ceb58de43f1327efb3eab50ef3d167f93d7d4472a0dd79702717695/f049cfec.html',
                ],
                scenarioId: '0',
                systemId: 'sys-1',
            },
            {
                pages: [
                    'https://sbs.s3.yandex.net/8b2728b20ceb58de43f1327efb3eab50ef3d167f93d7d4472a0dd79702717695/466e8453.html',
                    'https://sbs.s3.yandex.net/8b2728b20ceb58de43f1327efb3eab50ef3d167f93d7d4472a0dd79702717695/19aa07fe.html',
                ],
                scenarioId: '0',
                systemId: 'sys-0',
            },
        ];
        const isFigma = false;

        it('должен возвращать null, если значение одного или всех параметров не определено', () => {
            assert.equal(getMetrikaReportsParams(login, null, [], [], isFigma), null);
            assert.equal(getMetrikaReportsParams(), null);
        });

        it('должен возвращать параметры для отчетов Метрики', () => {
            assert.deepEqual(getMetrikaReportsParams(login, expHash, systems, visitedPages, isFigma), {
                login,
                accessRequestIdmUrl: requestCounterAccessUrl,
                reports: [
                    {

                        system: {
                            id: 'sys-1',
                            name: 'новое',
                        },
                        visorReportUrl,
                        clickMapReportUrls: [mapReportUrl, mapReportUrl],
                        scrollMapReportUrls: [mapReportUrl, mapReportUrl],
                    },
                    {

                        system: {
                            id: 'sys-0',
                            name: 'старое',
                        },
                        visorReportUrl,
                        clickMapReportUrls: [mapReportUrl, mapReportUrl],
                        scrollMapReportUrls: [mapReportUrl, mapReportUrl],
                    },
                ],
            });
        });

        it('не должен возвращать ссылки на карты скроллов, если эксперимент проведен на основе figma-прототипов', () => {
            assert.deepEqual(getMetrikaReportsParams(login, expHash, systems, visitedPages, true), {
                login,
                accessRequestIdmUrl: requestCounterAccessUrl,
                reports: [
                    {

                        system: {
                            id: 'sys-1',
                            name: 'новое',
                        },
                        visorReportUrl,
                        clickMapReportUrls: [mapReportUrl, mapReportUrl],
                        scrollMapReportUrls: null,
                    },
                    {

                        system: {
                            id: 'sys-0',
                            name: 'старое',
                        },
                        visorReportUrl,
                        clickMapReportUrls: [mapReportUrl, mapReportUrl],
                        scrollMapReportUrls: null,
                    },
                ],
            });
        });

        it('не должен возвращать ссылки на постраничные отчеты или список посещенных страниц (visitedPages[].pages) пустой', () => {
            const vp = visitedPages.map((p) => {
                p.pages = [];
                return p;
            });

            assert.deepEqual(getMetrikaReportsParams(login, expHash, systems, vp, isFigma), {
                login,
                accessRequestIdmUrl: requestCounterAccessUrl,
                reports: [
                    {

                        system: {
                            id: 'sys-1',
                            name: 'новое',
                        },
                        visorReportUrl,
                        clickMapReportUrls: null,
                        scrollMapReportUrls: null,
                    },
                    {

                        system: {
                            id: 'sys-0',
                            name: 'старое',
                        },
                        visorReportUrl,
                        clickMapReportUrls: null,
                        scrollMapReportUrls: null,
                    },
                ],
            });
        });
    });
});
