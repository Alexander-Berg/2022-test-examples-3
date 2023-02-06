specs({
    feature: 'LcBodyImage',
}, () => {
    hermione.only.notIn('safari13');
    it('Простая обложка', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Скроллим до конца страницы и потом до элемента, иначе тесты флапают с некорректным склеиванием
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcBodyImage())
            .assertView('plain', PO.lcBodyImage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с картинкой по верхней границе', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/with-image-top.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcBodyImage())
            .assertView('plain', PO.lcBodyImage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с картинкой по середине', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/with-image-center.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcBodyImage())
            .assertView('plain', PO.lcBodyImage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с картинкой по нижней границе', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/with-image-bottom.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcBodyImage())
            .assertView('plain', PO.lcBodyImage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с видео', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/with-video.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcBodyImage())
            .assertView('plain', PO.lcBodyImage(), { ignoreElements: ['.lc-body-image__video-wrapper'] });
    });

    hermione.only.notIn('safari13');
    it('Кастомная обложка', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/custom.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcBodyImage())
            .assertView('plain', PO.lcBodyImage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с overflow без нижнего отступа', function() {
        // Повторяем логику LPC, поэтому тень кнопки должна обрезаться
        return this.browser
            .url('/turbo?stub=lcbodyimage/overflow-no-padding-bottom.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Фоткаем вместе с lc-offsets, чтобы проверить работу overflow
            .assertView('plain', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с overflow и нижним отступом', function() {
        // Тут тень от кнопки должна попадать в padding
        return this.browser
            .url('/turbo?stub=lcbodyimage/overflow-padding-bottom.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Обложка с картинкой без картинки и правым расположением', function() {
        return this.browser
            .url('/turbo?stub=lcbodyimage/reversed-layout-without-media.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('without-media', PO.lcBodyImage());
    });

    hermione.only.notIn('safari13');
    it('Обложка на весь экран', function() {
        // Тут тень от кнопки должна попадать в padding
        return this.browser
            .url('/turbo?stub=lcbodyimage/viewport.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('viewport', PO.lcPage());
    });
});
