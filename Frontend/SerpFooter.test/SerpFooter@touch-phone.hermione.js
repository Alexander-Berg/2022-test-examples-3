'use strict';

specs({ feature: 'Футер' }, function() {
    describe('Стандартные проверки', function() {
        hermione.only.in('iphone');
        describe('Переключатель региона', function() {
            it('Доступность', async function() {
                const { PO, browser } = this;

                await browser.yaOpenSerp({
                    text: 'test',
                    lr: 213,
                }, PO.serpFooter());

                await browser.yaScroll(PO.serpFooter());

                await browser.yaWaitForVisible(PO.serpFooter.region());

                const ariaLabel = await browser.getAttribute(PO.serpFooter.region.link(), 'aria-label');
                assert.equal(ariaLabel, 'Ваш регион Москва', 'Неверный aria-label аттрибут');
            });
        });

        describe('Поиск в других системах', function() {
            it('Переход на другие поисковые системы', async function() {
                const { PO, browser } = this;
                const text = 'test';
                await browser.yaOpenSerp({
                    text,
                    tld: 'ru',
                    l10n: 'ru',
                    data_filter: 'no_results',
                }, PO.serpFooter.searchengines());

                await browser.yaCheckLink2({
                    selector: PO.serpFooter.searchengines.firstLink(),
                    url: {
                        href: {
                            url: '//www.google.com/m/search',
                            queryValidator: query => query && query.q === text,
                        },
                        ignore: ['protocol'],
                    },
                    message: 'Сломана ссылка на поиск Google',
                });

                await browser.yaCheckLink2({
                    selector: PO.serpFooter.searchengines.secondLink(),
                    url: {
                        href: {
                            url: '//m.bing.com/search',
                            queryValidator: query => query && query.q === text,
                        },
                        ignore: ['protocol'],
                    },
                    message: 'Сломана ссылка на поиск Bing',
                });

                await browser.yaCheckLink2({
                    selector: PO.serpFooter.searchengines.thirdLink(),
                    url: {
                        href: {
                            url: '//go.mail.ru/msearch',
                            queryValidator: query => query && query.q === text,
                        },
                        ignore: ['protocol'],
                    },
                    message: 'Сломана ссылка на поиск Mail',
                });
            });
        });
    });
});
