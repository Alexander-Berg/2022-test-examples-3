'use strict';
const yasmkit = require('../');
yasmkit.server.run();
yasmkit.metrics.addHistogram('sample_hgram');
yasmkit.metrics.addMaxLine('sample_max');
yasmkit.metrics.addMinLine('sample_min');
yasmkit.metrics.addSummLine('sample_summ_counter');
yasmkit.metrics.addSummLine('sample_summ');
// yasmkit.config('hosts', 'QLOUD');
// yasmkit.config('tag', 'itype=qloud;ctype=unknown;prj=ekb-interface-infra.unistat-test.test');

const panel = yasmkit.server.addPanel({
    id: 'test',
    title: 'Панелько',
    charts: [{
        title: 'Суммочка',
        signals: [{
            title: 'Сумма',
            metric: 'sample_summ',
        }],
    }],
});

const chart = panel.addChart({
    title: 'Графичек',
});

chart.addSignal({
    metric: 'sample_summ_counter',
    title: 'Сумма',
    func: ['max', '0'],
});

panel.addChart({
    title: 'Кастомный графичек',
    signals: [{
        host: 'QLOUD',
        tag: 'itype=qloudrouter;ctype=external;prj=browser-web.browser.prod',
        name: 'push-requests_summ',
    }],
});

setInterval(() => {
    const number = Math.random() * 300;
    // eslint-disable-next-line no-console
    console.log(number);
    yasmkit.addEvent('sample_hgram', number);
    yasmkit.addEvent('sample_min', number);
    yasmkit.addEvent('sample_max', number);
    yasmkit.addEvent('sample_summ', number);
    yasmkit.addEvent('sample_summ_counter');
}, 1000);
