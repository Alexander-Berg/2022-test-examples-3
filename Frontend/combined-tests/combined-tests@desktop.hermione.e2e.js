'use strict';
//В yframe нет возможности собирать запросы по двум счетчикам. Поэтому
//проверяем на сколько будут стабильны тесты с запросами собранными по стату
//заказ на тесты с пересечением: SERPTEST-1657
const { checkYandexNewsUrl } = require('../common/news/news.hermione-helper');

specs({
    feature: 'Колдунщики на выдаче',
    type: 'Совместный показ'
}, function() {
    it('Видео в центральной и картинки в правой колонках', async function() {
        const PO = this.PO;
        const fallback = [
            '/search/?text=природа фото и видео',
            '/search/?text=пейзажи фото и видео',
            '/search/?text=горы фото и видео',
            '/search/?text=портретное фото и видео',
            '/search/?text=котики фото и видео'
        ];

        await this.browser.yaOpenUrlByFeatureCounter('/$page/$main/$result[@wizard_name="images"]/images/title', [
            PO.contentRight.images(),
            PO.Video()
        ], fallback);

        await this.browser.yaShouldBeVisible(PO.contentRight.images());
        await this.browser.yaShouldBeVisible(PO.Video.Title(), 'Нет тайтла');
        await this.browser.yaShouldSomeBeVisible(PO.Video.VideoSnippet.Thumb(), 'Нет видео-тумбы');

        await this.browser.yaShouldBeVisible(
            PO.contentRight.images.header.title(),
            'Заголовок колдунщика отсутствует'
        );

        await this.browser.yaShouldBeVisible(
            PO.contentRight.images.gallery.rows.firstRow(),
            'Первый ряд галереи колдунщика отсутствует'
        );
    });

    it('Видео и картинки в центральной колонке', async function() {
        const PO = this.PO;
        const fallback = [
            '/search/?text=мадонна фото и видео',
            '/search/?text=путин фото и видео',
            '/search/?text=медведев фото и видео',
            '/search/?text=bmw фото и видео',
            '/search/?text=котики фото и видео'
        ];

        await this.browser.yaOpenUrlByFeatureCounter('/$page/$main/$result[@wizard_name="images"]/images/title', [
            PO.imagesLeft.images(),
            PO.Video()
        ], fallback);

        await this.browser.yaShouldBeVisible(PO.imagesLeft.images());
        await this.browser.yaShouldBeVisible(PO.Video());
        await this.browser.yaShouldBeVisible(PO.Video.Title(), 'Нет тайтла');
        await this.browser.yaShouldSomeBeVisible(PO.Video.VideoSnippet.Thumb(), 'Нет видео-тумбы');
        await this.browser.yaShouldBeVisible(PO.imagesLeft.images.title(), 'Нет заголовка колдунщика картинок');
        await this.browser.yaShouldBeVisible(PO.imagesLeft.images.gallery(), 'Нет галереи в колдунщике картинок');
    });

    it('Видео в центральной и ОО в правой колонках', async function() {
        const PO = this.PO;
        const fallback = [
            '/search/?text=мадонна видео',
            '/search/?text=путин видео',
            '/search/?text=медведев видео',
            '/search/?text=видео россия',
            '/search/?text=lindemann till видео'
        ];

        await this.browser.yaOpenUrlByFeatureCounter('/$page/$parallel/$result[@wizard_name="entity_search"]/object-badge', [
            PO.entityCard(),
            PO.entityCard.title(),
            PO.entityCard.subtitle(),
            PO.entityCard.description(),
            PO.Video()
        ], fallback);

        await this.browser.yaHaveVisibleText(PO.entityCard.title(), 'Нет заголовка');
        await this.browser.yaShouldBeVisible(PO.entityCard.feedbackFooter(), 'Нет футера');

        await this.browser.yaHaveVisibleText(
            PO.entityCard.feedbackFooter.firstLink(),
            'В футере должна быть хотя бы одна ссылка'
        );

        await this.browser.yaHaveVisibleText(PO.entityCard.subtitle(), 'Нет подзаголовка');
        await this.browser.yaHaveVisibleText(PO.entityCard.description(), 'Нет описания');
        await this.browser.yaShouldBeVisible(PO.Video.Title(), 'Нет тайтла');
        await this.browser.yaShouldSomeBeVisible(PO.Video.VideoSnippet.Thumb(), 'Нет видео-тумбы');
    });

    it('Видео и карусель ОО в центральной колонке', async function() {
        const PO = this.PO;
        const fallback = [
            '/search/?text=фильмы 2018',
            '/search/?text=лучшие фильмы',
            '/search/?text=фильмы',
            '/search/?text=сериалы 2020',
            '/search/?text=лучшие фильмы 2019'
        ];

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page[@ui="desktop"]/$top/$result[@wizard_name="entity_search"]/carousel/filters/select|/$page[@ui="desktop"]/$main/$result[@wizard_name="entity_search"]/carousel/showcase/item',
            [
                PO.EntityCarousel.Filters(),
                PO.EntityCarousel.Showcase(),
                PO.EntityCarousel.Showcase.Item(),
                PO.Video()
            ],
            fallback
        );

        await this.browser.yaShouldBeVisible(PO.EntityCarousel.Showcase(), 'Карусель фильмов не появилась');
        await this.browser.yaWaitForVisible(PO.EntityCarousel.Showcase.Item(), 'В карусели нет элементов');
        await this.browser.yaShouldBeVisible(PO.Video.Title(), 'Нет тайтла');
        await this.browser.yaShouldSomeBeVisible(PO.Video.VideoSnippet.Thumb(), 'Нет видео-тумбы');
    });

    it('Новости и объектный ответ', async function() {
        const PO = this.PO;
        const fallback = [
            '/search/?text=борис джонсон сми',
            '/search/?text=дональд трамп',
            '/search/?text=дмитрий медведев'
        ];

        await this.browser.yaOpenUrlByFeatureCounter('/$page/$main/$result[@wizard_name="news"]/item/doc', [
            PO.newsListRight(),
            PO.newsListRight.title(),
            PO.newsListRight.item.miniSnippet.title(),
            PO.entityCard(),
            PO.entityCard.title(),
            PO.entityCard.subtitle(),
            PO.entityCard.description()
        ], fallback);

        const url = await this.browser.yaCheckLink2({
            selector: PO.newsListRight.title(),
            target: '_blank',
            message: 'Сломана ссылка в заголовке'
        });

        assert.isTrue(checkYandexNewsUrl(url.href), 'Некорректный адрес ссылки в заголовке');

        await this.browser.yaCheckLink2({
            selector: PO.newsListRight.item.miniSnippet.title(),
            target: '_blank',
            message: 'Сломана ссылка первой новости'
        });

        await this.browser.yaHaveVisibleText(PO.entityCard.title(), 'Нет заголовка');
        await this.browser.yaShouldBeVisible(PO.entityCard.feedbackFooter(), 'Нет футера');

        await this.browser.yaHaveVisibleText(
            PO.entityCard.feedbackFooter.firstLink(),
            'В футере должна быть хотя бы одна ссылка'
        );

        await this.browser.yaHaveVisibleText(PO.entityCard.subtitle(), 'Нет подзаголовка');
        await this.browser.yaHaveVisibleText(PO.entityCard.description(), 'Нет описания');
    });

    it('Факты и картинки', async function() {
        const PO = this.PO;
        const fallback = [
            '/search/?text=вес слона',
            '/search/?text=самый твердый металл',
            '/search/?text=высота монблана',
            '/search/?text=глубина байкала',
            '/search/?text=марианская впадина'
        ];

        await this.browser.yaOpenUrlByFeatureCounter('/$page/$main/$result[@wizard_name="suggest_fact"]', [
            PO.suggestFact.fact(),
            PO.imagesLeft.images(),
            PO.imagesLeft.images.title(),
            PO.imagesLeft.images.gallery()
        ], fallback);

        await this.browser.yaShouldBeVisible(PO.suggestFact.fact(), 'Нет фактового ответа');
        await this.browser.yaShouldBeVisible(PO.imagesLeft.images.title(), 'Нет тайтла в к-ке картинок');
        await this.browser.yaShouldBeVisible(PO.imagesLeft.images.gallery(), 'Нет галереи в к-ке картинок');
    });

    it('Переформулировки и ОО', async function() {
        const PO = this.PO;

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="entity_search"]/object-badge',
            [PO.entityCard(), PO.relatedAbove()],
            [
                '/search/?text=мадонна',
                '/search/?text=немейский лев',
                '/search/?text=кремль',
                '/search/?text=великобритания состав',
                '/search/?text=венера',
                '/search/?text=доктор манхэттен'
            ],
            async () => {
                await this.browser.yaScroll(401);
                await this.browser.yaScroll(0);
            }
        );

        await this.browser.yaHaveVisibleText(PO.entityCard.title(), 'Нет заголовка');
        await this.browser.yaShouldBeVisible(PO.entityCard.feedbackFooter(), 'Нет футера');

        await this.browser.yaHaveVisibleText(
            PO.entityCard.feedbackFooter.firstLink(),
            'В футере должна быть хотя бы одна ссылка'
        );

        await this.browser.yaHaveVisibleText(PO.entityCard.description(), 'Нет описания');
    });

    it('Переформулировки и видео', async function() {
        const PO = this.PO;

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="video"]/video/title',
            PO.Video(),
            '/search/?text=котики видео'
        );
        await this.browser.yaScroll(401);
        await this.browser.yaScroll(0);
        await this.browser.yaWaitForVisible(PO.relatedAbove(), 'Переформулировки не появились');
        await this.browser.yaShouldBeVisible(PO.Video.Title(), 'Нет тайтла');
        await this.browser.yaShouldSomeBeVisible(PO.Video.VideoSnippet.Thumb(), 'Нет видео-тумбы');
    });
});
