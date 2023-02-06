'use strict';

const PO = require('./Posts.page-object').touchPhone;

hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Колдунщик 1орг',
    type: 'Публикации',
}, function() {
    const userTime = 20200421;

    describe('Таб в сайдблоке', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'ателье Лига джентльменов Алтайский край',
                user_time: userTime,
                data_filter: 'companies',
            }, PO.oneOrg());

            await this.browser.yaOpenOverlayAjax(
                () => this.browser.click(PO.oneOrg.tabsMenu.posts()),
                PO.oneOrgOverlay.posts(),
                'Сайдблок с карточкой организации не появился',
            );
        });

        hermione.also.in('safari13');
        it('Основные проверки', async function() {
            await this.browser.yaWaitForVisible(PO.oneOrgOverlay.posts.firstItem.reactions(), 'Лайки не загрузились');
            await this.browser.yaScrollOverlay(PO.oneOrgOverlay.posts.firstItem());
            await this.browser.assertView('plain', PO.oneOrgOverlay.posts.firstItem());

            await this.browser.yaCheckBaobabCounter(PO.oneOrgOverlay.posts.firstItem.textMore(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/more[@behaviour@type="dynamic"]',
            });

            // Разворачиваем все посты, пока не найдём внутри ссылку
            await this.browser.yaWaitUntil('Сломана ссылка в посте', async () => {
                await this.browser.click(PO.oneOrgOverlay.posts.item.textMore());

                return await this.browser.isVisible(PO.oneOrgOverlay.posts.item.textLink());
            });

            await this.browser.yaCheckLink2({
                selector: PO.oneOrgOverlay.posts.item.textLink(),
                baobab: {
                    path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/link',
                },
                message: 'Сломана ссылка в посте',
            });

            await this.browser.yaShouldNotExist(PO.oneOrgOverlay.posts.sixthItem(), 'Должно быть только 5 постов');

            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.oneOrgOverlay.posts.moreButton());
                return this.browser.yaWaitForVisible(PO.oneOrgOverlay.posts.sixthItem(), 'Шестой пост не загрузился');
            }, {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/more-button',
                behaviour: {
                    type: 'dynamic',
                },
            });
        });

        hermione.also.in('safari13');
        it('Просмотрщик', async function() {
            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.oneOrgOverlay.posts.firstItem.gallery.firstChild());
                return this.browser.yaWaitForVisible(PO.photoViewer(), 'Просмотрщик картинок в попапе не открылся');
            }, {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/thumb',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.assertView('plain', [PO.photoViewer()]);

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.next(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/viewer/preview/arrow-right',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.prev(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/viewer/preview/arrow-left',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.gallery.thirdItem(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/viewer/gallery/item',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.photoViewer.close());
                return this.browser.yaWaitForHidden(PO.photoViewer(), 'Просмотрщик картинок не закрылся');
            }, {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/viewer/close[@tags@close=1]',

                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'main',
                shows: 1,
                extClicks: 0,
                dynClicks: 6,
                miscClicks: 100,
            });
        });

        hermione.also.in('safari13');
        it('Лайки', async function() {
            await this.browser.yaWaitForVisible(PO.oneOrgOverlay.posts.firstItem.reactions(), 'Лайки не загрузились');
            await this.browser.yaScrollOverlay(PO.oneOrgOverlay.posts.firstItem());
            await this.browser.click(PO.oneOrgOverlay.posts.firstItem.reactions.like());

            await this.browser.yaShouldBeSame(
                PO.oneOrgOverlay.posts.firstItem.reactions.like(),
                PO.oneOrgOverlay.posts.firstItem.reactions.itemIsMy(),
                'Лайк не проставился',
            );

            await this.browser.assertView('reactions-liked', PO.oneOrgOverlay.posts.firstItem.reactions());
            await this.browser.click(PO.oneOrgOverlay.posts.firstItem.reactions.dislike());

            await this.browser.yaShouldBeSame(
                PO.oneOrgOverlay.posts.firstItem.reactions.dislike(),
                PO.oneOrgOverlay.posts.firstItem.reactions.itemIsMy(),
                'Дизлайк не проставился',
            );

            await this.browser.yaCheckBaobabCounter(() => {}, [
                {
                    path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/reactions/like',
                    behaviour: {
                        type: 'dynamic',
                    },
                },
                {
                    path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/posts/posts/reactions/dislike',
                    behaviour: {
                        type: 'dynamic',
                    },
                },
            ]);

            // TODO: Починится после https://st.yandex-team.ru/SERP-85299
            // .yaCheckBaobabCounter(() => {}, [
            //     {
            //         path: '/$page/$main/$result/overlay-wrapper/content/tabs/controls/tabs/posts/reactions/like',
            //         behaviour: { type: 'dynamic' },
            //     },
            //     {
            //         eslint-disable-next-line max-len
            //         path: '/$page/$main/$result/overlay-wrapper/content/tabs/controls/tabs/posts/reactions/dislike',
            //         behaviour: { type: 'dynamic' },
            //     },
            // ])
            await this.browser.click(PO.oneOrgOverlay.posts.firstItem.reactions.dislike());

            await this.browser.yaShouldNotExist(
                PO.oneOrgOverlay.posts.firstItem.reactions.itemIsMy(),
                'Оценка пользователя не сбросилась',
            );

            await this.browser.click(PO.oneOrgOverlay.posts.moreButton());
            await this.browser.yaWaitForVisible(PO.oneOrgOverlay.posts.sixthItem(), 'Шестой пост не загрузился');

            await this.browser.yaWaitForVisible(
                PO.oneOrgOverlay.posts.sixthItem.reactions(),
                'Лайки в шестом посте в оверлее не загрузились',
            );

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'main',
                shows: 1,
                extClicks: 0,
                dynClicks: 5,
                miscClicks: 100,
                requests: 1,
            });
        });

        hermione.only.notIn('searchapp-phone', 'https://st.yandex-team.ru/SERP-91664#5d84c65f701665001cdc200b');
        hermione.also.in('safari13');
        it('Комментарии', async function() {
            await this.browser.yaWaitForVisible(
                PO.oneOrgOverlay.posts.firstItem.reactions.comment(),
                'Нет иконки комментариев',
            );

            await this.browser.click(PO.oneOrgOverlay.posts.firstItem.reactions.comment());
            await this.browser.assertView('comments-opened', PO.oneOrgOverlay.posts.firstItem.reactions.comment());
            await this.browser.yaWaitForVisible(PO.oneOrgOverlay.posts.firstItem.comments(), 'Комментарии не открылись');
            await this.browser.click(PO.oneOrgOverlay.posts.firstItem.reactions.comment());
            await this.browser.yaWaitForHidden(PO.oneOrgOverlay.posts.firstItem.comments(), 'Комментарии не скрылись');

            await this.browser.yaCheckResultMetrics({
                name: 'orgs',
                place: 'main',
                shows: 1,
                extClicks: 0,
                dynClicks: 3,
                miscClicks: 100,
                requests: 1,
            });
        });
    });
});
