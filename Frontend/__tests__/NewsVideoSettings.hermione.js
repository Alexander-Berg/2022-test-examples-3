specs({
    feature: 'news-video-settings',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=newsvideosettings/default.json')
            // перекрашиваем background, т.к. белая иконка кнопки сливается с фоном
            .execute(changeBackgroundColor, PO.newsVideoSettingsShow())
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.newsVideoSettings())
            .click(PO.newsVideoSettingsShow())
            .yaWaitForVisible(PO.popup(), 'Попап с настройками не появился')
            .assertView('open', [PO.newsVideoSettings(), PO.popup()]);
    });
});

function changeBackgroundColor(className) {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = `${className} { background-color: #eceef2; }`;

    document.body.appendChild(style);

    return true;
}
