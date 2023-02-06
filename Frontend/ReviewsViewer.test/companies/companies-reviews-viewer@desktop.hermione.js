'use strict';

const PO = require('./companies.page-object/index@desktop');

specs({
    feature: 'Отзывы на Серпе',
    type: 'Просмотрщик отзывов организации',
}, function() {
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());

        await browser.click(PO.oneOrg.tabsMenu.reviews());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        await browser.yaWaitForVisible(PO.reviewViewerModal.reviewItem(), 'Не загрузились отзывы');
        await this.browser.yaStubImage(PO.reviewViewerModal.reviewItem.authorImage(), 42, 42);
        await this.browser.yaStubImage(PO.reviewViewerModal.reviewItem.photos.photosItem(), 130, 130);

        await browser.assertView('plain', PO.reviewViewerModal.Content(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
        });
    });

    describe('Точки входа', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'кафе пушкин',
            }, PO.oneOrg());
        });

        it('Таб "Отзывы"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.reviews());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Заголовок "Отзывы ∙ {N}"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.reviewsPreview.title.link());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Текст отзыва', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.reviewsPreview.list.firstPreviewItem.cut.more());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Ссылка "Читать все отзывы"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.reviewsPreview.more());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Аспект отзывов', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.reviewsPreview.aspectsList.firstCard());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });
    });

    describe('Проверка счётчиков', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'кафе пушкин',
            }, PO.oneOrg());

            await this.browser.click(PO.oneOrg.tabsMenu.reviews());
            await this.browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        afterEach(async function() {
            await this.browser.yaCheckMetrics({
                'web.part_all_click_without_baobab': 0,
            });
        });

        it('RUM счетчики', async function() {
            await this.browser.yaWaitForVisible(PO.reviewViewerModal.reviewItem(), 'Не загрузились отзывы');

            await this.browser.yaCheckCounter2(() => {}, {
                path: '/tech/perf/delta',
                vars: { '1701': 'companies_reviews_react_load_view_default' },
            }, 'Не сработал Ya.Rum delta счетчик загрузки отзывов');
        });

        it('Клик в "Ещё" и выбор первого пункта', async function() {
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.moreTab(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/tabs-menu/more',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в таб "Ещё"',
            );
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.moreTab.item(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/tabs-menu',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик выбора таба в меню "Ещё"',
            );
        });

        it('Просмотр фото аспекта', async function() {
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.secondTab(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/tabs-menu',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в таб',
            );
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.page.photo(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/scroller/photo',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в фото',
            );
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.page.photoViewer.close(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/viewer/close',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик закрытия вьюера фото',
            );
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.yaScrollContainer(PO.reviewViewerModal.page.photos.scrollerWrap(), 200),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/scroller',
                    event: 'scroll',
                },
                'Сломан счётчик скролла фотографий',
            );
        });

        it('Сортировка отзывов', async function() {
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.page.sorting(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/reviewsSelect/select/button',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в селектор сортировки отзывов',
            );
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.page.sorting.secondItem(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/reviewsSelect/select/menu-item',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик выбора сортировки отзывов',
            );
        });

        it('Читать целиком/скрыть отзыв', async function() {
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.reviewItem.cut.more(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/cut/more',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в "Читать целиком"',
            );
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.reviewItem.cut.hide(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/cut/hide',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в "Скрыть"',
            );
        });

        it('Ссылка на автора отзыва', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.reviewViewerModal.reviewItem.profileLink(),
                baobab: {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/review-author/author',
                },
                target: '_blank',
            });
        });

        it('Кнопка написания отзыва', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.reviewViewerModal.page.loginButton(),
                baobab: {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/login-button',
                },
                target: '',
            });
        });

        it('Закрытие попапа', async function() {
            await this.browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.close(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/close',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик закрытия попапа',
            );
        });

        it('Лайки в отзывах', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.reviewItem.reactions.like(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/reactions/like',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика лайка',
            );

            await browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.reviewItem.reactions.dislike(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/reactions/dislike',
                    behaviour: { type: 'dynamic' },
                },
            );
        });

        it('Фото в отзывах', async function() {
            const { browser } = this;

            await browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.reviewItem.photos.firstPhotosItem(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/scroller/photo-viewer',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик клика в фото',
            );
            await browser.yaWaitForVisible(PO.reviewViewerModal.page.photoViewer());
            await browser.yaCheckBaobabCounter(
                PO.reviewViewerModal.page.photoViewer.close(),
                {
                    path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/review-item/viewer/close',
                    behaviour: { type: 'dynamic' },
                },
                'Сломан счётчик закрытия вьюера фото',
            );
        });
    });

    it('Дозагрузка отзывов', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());

        await browser.click(PO.oneOrg.tabsMenu.reviews());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        await browser.yaWaitForVisible(PO.reviewViewerModal.reviewItem(), 'Не загрузились отзывы');

        const items = await browser.yaVisibleCount(PO.reviewViewerModal.reviewItem());

        await this.browser.yaScrollContainer(PO.reviewViewerModal.page(), 0, 50000);

        // Ждём загрузку новых элементов
        await browser.yaWaitUntil('Новые элементы не загрузились', async function() {
            const newItems = await browser.yaVisibleCount(PO.reviewViewerModal.reviewItem());

            return newItems > items;
        });
    });

    it('Нет отзывов', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'гастроном андреевский шейнкмана 55 екатеринбург',
        }, PO.oneOrg());

        await browser.click(PO.oneOrg.tabsMenu.reviews());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        await browser.yaWaitForVisible(PO.reviewViewerModal.page.loginButton());

        await browser.assertView('plain', PO.reviewViewerModal.Content(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
        });
    });

    it('Доступность отзывов', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());

        await browser.click(PO.oneOrg.tabsMenu.reviews());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        await browser.yaWaitForVisible(PO.reviewViewerModal.reviewItem(), 'Не загрузились отзывы');

        const ariaLabel2 = await this.browser.getAttribute(PO.reviewViewerModal.reviewItem.rating(), 'aria-label');
        await assert.exists(ariaLabel2, 'Нет атрибута aria-label у оценки отзыва');
        const ariaLabel3 = await this.browser.getAttribute(PO.reviewViewerModal.reviewItem.reactions(), 'aria-label');
        await assert.equal(ariaLabel3, 'Реакция пользователей', 'Неверный атрибут aria-label у реакций к отзыву');
        const ariaLabel4 = await this.browser.getAttribute(PO.reviewViewerModal.reviewItem.reactions(), 'role');
        await assert.equal(ariaLabel4, 'radiogroup', 'Неверный атрибут role у реакций к отзыву');
        const ariaLabel5 = await this.browser.getAttribute(PO.reviewViewerModal.reviewItem.reactions.item(), 'role');
        await assert.equal(ariaLabel5, 'radio', 'Неверный атрибутrole у реакции к отзыву');
        const tag3 = await this.browser.getTagName(PO.reviewViewerModal.reviewItem.reactions.item());
        await assert.equal(tag3, 'button', 'Ошибка в теге у реакции к отзыву');
        const ariaLabel6 = await this.browser.getAttribute(PO.reviewViewerModal.reviewItem.reactions.item(), 'aria-label');
        await assert.exists(ariaLabel6, 'Нет атрибута aria-label у реакции к отзыву');
        const ariaLabel7 = await this.browser.getAttribute(PO.reviewViewerModal.reviewItem.reactions.item(), 'aria-checked');
        await assert.exists(ariaLabel7, 'Нет атрибута aria-checked у реакции к отзыву');
    });
});
