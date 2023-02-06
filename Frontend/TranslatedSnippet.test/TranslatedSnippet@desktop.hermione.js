'use strict';

const PO = require('./TranslatedSnippet.page-object');

specs({ feature: 'Переводный сниппет' }, function() {
    it('Внешний вид и основные проверки', async function() {
        const ORIGINAL_URL = 'https://en.wikipedia.org/wiki/%C3%81ngel_Gaspar';
        const TRANSLATED_URL =
            'https://translate.yandex.ru/translate?lang=en-ru&url=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2F%25C3%2581ngel_Gaspar&view=c';

        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2527448692',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.assertView('plain', PO.translatedSnippet());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result/translated_snippet',
        });

        await this.browser.yaCheckLink2({
            selector: PO.translatedSnippet.translatedTitle(),
            url: {
                href: TRANSLATED_URL,
            },
            baobab: {
                path: '/$page/$main/$result/translated_snippet/title',
                attrs: {
                    isTranslated: true,
                },
            },
            target: '_blank',
            message: 'Заголовок перевода',
        });

        await this.browser.yaCheckLink2({
            selector: PO.translatedSnippet.title.link(),
            url: {
                href: ORIGINAL_URL,
            },
            baobab: {
                path: '/$page/$main/$result/translated_snippet/title',
                attrs: {
                    isTranslated: false,
                },
            },
            target: '_blank',
            message: 'Заголовок сниппета',
        });

        await this.browser.yaCheckLink2({
            selector: PO.translatedSnippet.path.link(),
            url: {
                href: ORIGINAL_URL,
            },
            baobab: {
                path: '/$page/$main/$result/translated_snippet/path/urlnav',
            },
            clickCoords: [10, 2],
            target: '_blank',
            message: 'Гринурл сниппета',
        });
    });

    it('Проверка метки официальности', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2527448692',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.click(PO.verifiedBlue());
        await this.browser.yaWaitForTooltip('Официальный сайт', 5000, 'Тултип ачивки официальности не открылся');

        await this.browser.yaAssertViewExtended(
            'plain',
            PO.translatedSnippet.path(),
            {
                horisontalOffset: 85,
                verticalOffset: 50,
            },
        );

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result/translated_snippet/official',
        });
    });

    it('Клик по кебабу', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2527448692',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.click(PO.translatedSnippet.extralinks());
        await this.browser.assertView('plain', PO.extralinksPopup());
    });

    it('Без метки официальности', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3175275683',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.yaShouldNotExist(PO.translatedSnippet.verifiedBlue());
        await this.browser.assertView('plain', PO.translatedSnippet());
    });

    it('Избыточный текст', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2443408956',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.assertView('plain', PO.translatedSnippet());
    });
});
