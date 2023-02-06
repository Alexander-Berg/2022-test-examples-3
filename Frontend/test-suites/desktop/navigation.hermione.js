'use strict';

const PO = require('../../page-objects/desktop').PO;

const navigation = [
    {
        selector: PO.navigation.imagesLink(),
        url: 'http://yandex.ru/images/search?text=sea&parent-reqid=',
        baobab: { path: '/$page/$header/$navigation/item/link[@type="images"]' },
        name: 'Картинки'
    },
    {
        selector: PO.navigation.videoLink(),
        url: 'https://yandex.ru/video/search?text=sea',
        baobab: { path: '/$page/$header/$navigation/item/link[@type="video"]' },
        name: 'Видео'
    },
    {
        selector: PO.navigation.mapsLink(),
        url: 'http://yandex.ru/maps/?source=serp_navig&text=sea',
        baobab: { path: '/$page/$header/$navigation/item/link[@type="maps"]' },
        name: 'Карты'
    },
    {
        selector: PO.navigation.marketLink(),
        url: 'http://market.yandex.ru/search.xml?clid=521&cvredirect=2&text=sea',
        baobab: { path: '/$page/$header/$navigation/item/link[@type="market"]' },
        name: ' Маркет'
    }
];

specs('Табы сервисов', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp({ text: 'sea', exp_flags: 'distr_splashscreen_disable=1' })
            .yaWaitForVisible(PO.search(), 'Стрелка не появилась');
    });

    hermione.skip.in('ie8', 'https://st.yandex-team.ru/SERP-65991');
    it('Проверка ссылок на табах постоянных сервисов', function() {
        return Promise.all(
            navigation.map(tab => this.browser
                .yaCheckLink(tab.selector, { url: tab.url })
                .then(url => {
                    let skipQuery = false;

                    // Вырезаем parent-reqid в картинках и видео, т.к. в каждом стабе он меняется
                    if (url.pathname === '/images/search' || url.pathname === '/video/search') {
                        skipQuery = true;
                    }

                    return this.browser
                        .yaCheckURL(url, tab.url,
                            `Сломана ссылка в табе '${tab.name}'`, { skipProtocol: true, skipQuery: skipQuery }
                        );
                })
                .yaCheckBaobabCounter(tab.selector, tab.baobab)
            )
        );
    });

    it('Работа кнопки Еще', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.navigation.more(), { path: '/$page/$header/$navigation/item/link[@type="more"]' });
    });
});
