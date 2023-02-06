const assert = require('assert');

const moduleName = '../../src/bundle/spa-metric';

function reloadModule() {
    delete require.cache[require.resolve(moduleName)];
    require(moduleName);
}

const OPTIONS1 = {
    startDataLoadingMetric: true,
    finishDataLoadingMetric: true,
    startDataRenderingMetric: true,
    finishDataRenderingMetric: true,
    animationSpeedMetric: true,
};
const OPTIONS2 = {
    finishDataLoadingMetric: true,
    startDataRenderingMetric: true,
    finishDataRenderingMetric: true,
    animationSpeedMetric: true,
};
const OPTIONS3 = {
    startDataLoadingMetric: true,
    animationSpeedMetric: true,
};

describe('RUM spa metrics module', () => {
    var callTimes = {};

    beforeEach(() => {
        global.window = global;
        window.Ya = {
            Rum: {
                enabled: true,
                makeSubPage: subPageName => {
                    return {
                        '689.2322': 0,
                        '2924': subPageName,
                        '2925': 0,
                        isCanceled: () => false,
                        cancel: () => {
                        },
                    };
                },
                getTime: () => 10,

                sendTimeMark: () => callTimes.sendTimeMark += 1,
                sendDelta: () => callTimes.sendDelta += 1,
                finalizeLayoutShiftScore: () => callTimes.finalizeLayoutShiftScore += 1,
                finalizeLargestContentfulPaint: () => callTimes.finalizeLargestContentfulPaint += 1,
                sendTrafficData: () => callTimes.sendTrafficData += 1,

                spa: undefined,
                sendAnimationSpeed: undefined,
            }
        };

        window.console.error = () => callTimes['console.error'] += 1;
        window.requestAnimationFrame = cb => { cb() };

        callTimes = {
            sendTimeMark: 0,
            sendDelta: 0,
            finalizeLayoutShiftScore: 0,
            finalizeLargestContentfulPaint: 0,
            sendTrafficData: 0,
            'console.error': 0,
        };

        reloadModule();
    });

    it('Создает необходимые функции', () => {
        assert.strictEqual(Boolean(window.Ya.Rum.spa), true);
        assert.strictEqual(Boolean(window.Ya.Rum.sendAnimationSpeed), true);
    });

    it('Позволяет создать подстраницу с дефолтами', () => {
        const subPage = window.Ya.Rum.spa.makeSpaSubPage('main-feed');
        assert.strictEqual(subPage['2924'], 'page.main-feed');
    });

    it('Позволяет пользователю самому конфигурировать подстраницу', () => {
        const subPage = window.Ya.Rum.spa
            .makeSpaSubPage('main-feed', OPTIONS1, true, true);
        assert.strictEqual(callTimes.finalizeLargestContentfulPaint, 1);
        assert.strictEqual(subPage['2924'], 'block.main-feed');
    });

    it('Отправляет все метрики согласно конфигу', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS1);
        // delta
        window.Ya.Rum.spa.startDataLoading('main-feed');
        // delta
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta x4
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);

        assert.strictEqual(callTimes.sendDelta, 6);
        assert.strictEqual(callTimes['console.error'], 0);
    });

    it('Отправляет метрику начала загрузки данных, но игнорирует остальные в связи с пропуском вызова обязательной функции', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS1);
        // delta
        window.Ya.Rum.spa.startDataLoading('main-feed');
        // не вызовет ничего в связи с пропуском функции finishDataLoading
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);

        assert.strictEqual(callTimes.sendDelta, 1);
        assert.strictEqual(callTimes['console.error'], 1);
    });

    it('Отправляет все метрики согласно конфигу, и реагирует на невызов необязательной функции', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS1);
        // delta
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta x4
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);

        assert.strictEqual(callTimes.sendDelta, 5);
        assert.strictEqual(callTimes['console.error'], 1);
    });

    it('Отправляет все метрики согласно конфигу, игнорирует невызов необязательной функции', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS2);
        // delta
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta x4
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);

        assert.strictEqual(callTimes.sendDelta, 5);
        assert.strictEqual(callTimes['console.error'], 0);
    });

    it('Отправляет все метрики согласно конфигу, ждет самостоятельного вызова функции finishDataRendering', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS2);
        // delta
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta
        window.Ya.Rum.spa.startDataRendering('main-feed', '', false);
        // delta x3
        window.Ya.Rum.spa.finishDataRendering('main-feed', '', 0);

        assert.strictEqual(callTimes.sendDelta, 5);
        assert.strictEqual(callTimes['console.error'], 0);
    });

    it('Отправляет нужные метрики согласно конфигу', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS3);
        // delta
        window.Ya.Rum.spa.startDataLoading('main-feed');
        // ничего
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta x2
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);

        assert.strictEqual(callTimes.sendDelta, 3);
        assert.strictEqual(callTimes['console.error'], 0);
    });

    it('Не дает отправить одни и те же метрики дважды', () => {
        window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS3);
        // delta
        window.Ya.Rum.spa.startDataLoading('main-feed');
        // ничего
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta x2
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);
        // ошибка
        window.Ya.Rum.spa.startDataLoading('main-feed');
        // ошибка
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // ошибка
        window.Ya.Rum.spa.startDataRendering('main-feed', '', false);
        // ошибка
        window.Ya.Rum.spa.finishDataRendering('main-feed', '', 0);

        assert.strictEqual(callTimes.sendDelta, 3);
        assert.strictEqual(callTimes['console.error'], 4);
    });

    it('Отдает верную ссылку на ранее созданную подстраницу после завершения отправки метрик', () => {
        const initialsubpage = window.Ya.Rum.spa.makeSpaSubPage('main-feed', OPTIONS3);
        // delta
        window.Ya.Rum.spa.startDataLoading('main-feed');
        // ничего
        window.Ya.Rum.spa.finishDataLoading('main-feed');
        // delta x2
        window.Ya.Rum.spa.startDataRendering('main-feed', '', true, 0);

        const subpage = window.Ya.Rum.spa.getLastSpaSubPage('main-feed');

        assert.strictEqual(initialsubpage, subpage);
        assert.strictEqual(callTimes['console.error'], 0);
    });

    it('Отдает верную ссылку на ранее созданную подстраницу', () => {
        const subpage1 = window.Ya.Rum.spa.makeSpaSubPage('main-feed');
        const subpage2 = window.Ya.Rum.spa.makeSpaSubPage('main-feed');
        const subpage3 = window.Ya.Rum.spa.makeSpaSubPage('similar-feed');

        const subpage = window.Ya.Rum.spa.getLastSpaSubPage('main-feed');

        assert.notStrictEqual(subpage, subpage1);
        assert.strictEqual(subpage, subpage2);
        assert.notStrictEqual(subpage, subpage3);
        assert.strictEqual(callTimes['console.error'], 0);
    });

    it('Отдает null, если подстраница не создавалась', () => {
        const subpage = window.Ya.Rum.spa.getLastSpaSubPage('d2d-feed');
        const subpage2 = window.Ya.Rum.spa.getLastSpaSubPage();

        assert.strictEqual(subpage, null);
        assert.strictEqual(subpage2, null);
        assert.strictEqual(callTimes['console.error'], 2);
    });

    it('Вызывает customLogger', () => {
        window.Ya.Rum.spa.getLastSpaSubPage('logger');

        let logged = 0;

        window.Ya.Rum.spa.setLogger({ error: () => logged++ });
        window.Ya.Rum.spa.getLastSpaSubPage('logger');

        assert.strictEqual(callTimes['console.error'], 1);
        assert.strictEqual(logged, 1);
    });
});
