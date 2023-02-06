'use strict';

const PO = require('./TranslatedSnippet.page-object');

specs({ feature: 'Переводный сниппет' }, function() {
    hermione.also.in('iphone-dark');
    it('Внешний вид и основные проверки', async function() {
        const ORIGINAL_URL = 'https://en.wikipedia.org/wiki/%C3%81ngel_Gaspar';
        const TRANSLATED_URL =
            'https://translate.yandex.ru/translate?lang=en-ru&url=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2F%25C3%2581ngel_Gaspar&view=c';

        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2001932280',
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
            target: '_blank',
            message: 'Гринурл сниппета',
        });
    });

    hermione.also.in('iphone-dark');
    hermione.only.notIn('chrome-phone');
    it('Проверка метки официальности', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2001932280',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        // Иначе шапка вызывает закрытие попапов
        // Раздебажить не удалось
        await this.browser.yaHideHeader();

        await this.browser.click(PO.verifiedBlue());
        await this.browser.yaWaitForTooltip('Официальный сайт', 5000, 'Тултип ачивки официальности не открылся');

        await this.browser.yaAssertViewExtended(
            'plain',
            PO.translatedSnippet.path(),
            {
                horisontalOffset: 40,
                verticalOffset: 80,
            },
        );

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result/translated_snippet/official',
        });
    });

    hermione.also.in('iphone-dark');
    it('Клик по кебабу', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2001932280',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.click(PO.translatedSnippet.extralinks());
        await this.browser.assertView('plain', PO.translatedSnippet());
    });

    hermione.also.in('iphone-dark');
    it('Без метки официальности', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3539116653',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.yaShouldNotExist(PO.verifiedBlue());
        await this.browser.assertView('plain', PO.translatedSnippet());
    });

    hermione.also.in('iphone-dark');
    it('Избыточный текст', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '1837316245',
            data_filter: 'translated_snippet',
        }, PO.translatedSnippet());

        await this.browser.assertView('plain', PO.translatedSnippet());
    });
});
