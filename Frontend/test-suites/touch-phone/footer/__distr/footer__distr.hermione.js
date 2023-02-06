'use strict';

hermione.only.in('chrome-phone');
specs({
    feature: 'Подвал',
    experiment: 'Дистрибуция в подвале'
}, () => {
    it('Дистрибуция браузера', function() {
        const PO = require('../../../../page-objects/touch-phone').PO;

        return this.browser
            .yaOpenSerp({
                text: 'окна',
                exp_flags: 'distr_granny_bro=1'
            }, PO.footer2())
            .yaCheckBaobabServerCounter({
                path: '/$page/$footer/promofooter[@product="browser" and @platform="android"]'
            })
            .scroll(PO.footer2.distr())
            .assertView('footer-distr', PO.footer2.distr())
            .yaCheckLink(PO.footer2.distr.button()).then(url => this.browser
                .yaCheckURL(url,
                    'https://redirect.appmetrica.yandex.com/serve/745720944961538460?c=Footer_serp&adgroup=universal&source_id=serp_granny&creative=universal_text_1',
                    'Невалидная ссылка на блоке дистрибуции'
                )
            );
    });
});
