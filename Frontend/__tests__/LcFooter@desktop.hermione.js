specs({
    feature: 'LcFooter',
}, () => {
    var setDefaultFont = function() {
        const element = document.createElement('style');
        element.setAttribute('type', 'text/css');
        element.appendChild(document.createTextNode(
            ' .lc-footer { font-family: sans-serif !important; }'
        ));
        document.head.appendChild(element);
    };

    hermione.only.in(['chrome-desktop', 'firefox']);
    it('Внешний вид ссылок', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/full.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .execute(setDefaultFont)
            .execute(function() {
                document.getElementsByClassName('lc-footer-contacts__item')[0].getElementsByClassName('link')[0].setAttribute('href', '#');
                document.getElementsByClassName('lc-footer-menu__item')[2].setAttribute('href', '#');
            })
            .moveToObject(PO.lcFooter.menuItem())
            .assertView('hover-menu', PO.lcFooter())
            .click(PO.lcFooter.menuItem())
            .assertView('click-menu', PO.lcFooter())
            .moveToObject(PO.lcFooter.contactItem.link())
            .assertView('hover-contacts', PO.lcFooter())
            .click(PO.lcFooter.contactItem.link())
            .assertView('click-contacts', PO.lcFooter());
    });

    it('Внешний вид с LcPhone', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/with-lc-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPhoneButton(), 'Кнопка lcPhone не появилась')
            .assertView('plain', PO.lcFooter())
            .click(PO.lcPhoneButton())
            .yaWaitForVisible(PO.lcPhoneModal(), 'Модал не открылся');
    });
});
