specs({
    feature: 'sport-result-list-teaser',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=sportresultlistteaser/data.json')
            .yaWaitForVisible(PO.blocks.sportResultListTeaser(), 'Не работает')
            .assertView('plain', PO.blocks.sportResultListTeaser())
            .yaCheckLink({
                selector: PO.blocks.sportResultListTeaser.firstSportMore(),
                message: 'Неправильная ссылка на все матчи футбола',
                target: '_blank',
                url: {
                    href: 'https://yandex.ru/sport/calendar/football',
                },
            })
            .yaCheckLink({
                selector: PO.blocks.sportResultListTeaser.secondSportMore(),
                message: 'Неправильная ссылка на все матчи хоккея',
                target: '_self',
                url: {
                    href: 'https://yandex.ru/sport/calendar/hockey',
                },
            })
            .yaCheckLink({
                selector: PO.blocks.sportResultListTeaser.firstMatch(),
                message: 'Неправильная ссылка на первый матч',
                target: '_blank',
                url: {
                    href: 'https://yandex.ru/sport',
                },
            });
    });
});
