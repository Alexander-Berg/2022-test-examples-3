specs({
    feature: 'Календарь на десктопе',
}, () => {
    it('Базовый вид блока', function() {
        const selector = '.content';
        const ignore = { ignoreElements: ['.news-match-line-competition__logo-container'] };

        return this.browser
            .url('/turbo?stub=newsmatchcalendar/default.json')
            .execute(resetBodyMinWidth)
            .execute(hideElements, '.news-match-result__logo')
            .execute(hideElements, '.news-match-line-competition__logo')
            .execute(hideElements, '.news-match-result-race__main-wrapper')
            .windowHandleSize({ width: 1366, height: 1500 })
            .execute(controlWidth, 1366)
            .assertView('plain-1366', selector, ignore)
            .windowHandleSize({ width: 1000, height: 1500 })
            .execute(controlWidth, 1000)
            .moveToObject('.news-navigation-menu__item-wrap-outer')
            .assertView('plain-1000', selector, ignore);
    });
});

function hideElements(className) {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = `${className} { visibility: hidden !important; }`;

    document.body.appendChild(style);

    return true;
}

function resetBodyMinWidth() {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = '.page { min-width: auto; }';

    document.body.appendChild(style);

    return true;
}

function controlWidth(width) {
    const style = document.createElement('style');
    const mapPageToNewsMatchCenterWidth = {
        '1366': '944px',
        '1000': '616px',
    };

    style.setAttribute('type', 'text/css');
    style.innerHTML = `.news-match-calendar { width: ${mapPageToNewsMatchCenterWidth[width]} }`;

    document.body.appendChild(style);

    return true;
}
