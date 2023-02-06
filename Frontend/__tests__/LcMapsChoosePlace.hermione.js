specs({
    feature: 'LcMapsChoosePlace',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычная карта', function() {
        return this.browser
            .url('/turbo?stub=lcmapschooseplace/default.json')
            .execute(hideMapTiles)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcMapsChoosePlace());
    });

    hermione.only.in('chrome-desktop', 'Двигать карту в тестах можно только в chrome-desktop');
    hermione.only.notIn('safari13');
    it('При движении карты появляется кнопка', function() {
        return this.browser
            .url('/turbo?stub=lcmapschooseplace/default.json')
            .execute(hideMapTiles)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // двигаем карту
            .moveTo(null, 50, 50)
            .buttonDown()
            .moveTo(null, 10, 0)
            .buttonUp()
            .yaWaitForVisible(PO.lcMapsChoosePlace.submitButton(), 'Кнопка не появилась')
            .getAttribute(PO.lcMapsChoosePlace.submitButtonLink(), 'href')
            .then(href =>
                this.browser.yaCheckURL(href, {
                    // в ссылке должны быть подставлены координаты
                    queryValidator: query => Boolean(query.ll),
                    url: 'https://yandex.ru',
                })
            )
            .assertView('plain', PO.lcMapsChoosePlace())
            // координаты должны сброситься на начальные
            .click(PO.lcMapsChoosePlace.mapControlSetCenter())
            .getAttribute(PO.lcMapsChoosePlace.submitButtonLink(), 'href')
            .then(href =>
                this.browser.yaCheckURL(href, 'https://yandex.ru/?ll=55.734003999993746%2C37.58852999999996')
            );
    });
}
);

function hideMapTiles() {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = '.ymaps-2-1-76-map { opacity: 0 }';

    document.body.appendChild(style);

    return true;
}
