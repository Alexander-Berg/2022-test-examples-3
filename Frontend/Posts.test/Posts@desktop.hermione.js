'use strict';

const PO = require('./Posts.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Публикации',
}, function() {
    const userTime = 20200421;

    describe('Таб на морде', function() {
        it('Основные проверки', async function() {
            await this.browser.yaOpenSerp({
                text: 'лига джентльменов алтайский край ателье',
                user_time: userTime,
                data_filter: 'companies',
            }, PO.oneOrg());

            // Устанавливаем ширину вручную, чтобы таб не прятался под кнопку Ещё
            await this.browser.setViewportSize({ width: 1600, height: 1024 });

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-posts"', {
                field: 'url',
            }, () => this.browser.click(PO.oneOrg.tabsMenu.posts()));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await this.browser.yaWaitForVisible(PO.oneOrg.posts(), 'Таб с публикациями не загрузился', 20000);

            await this.browser.yaWaitForVisible(
                PO.oneOrg.posts.firstItem.gallery(),
                'Галерея в посте не показалась',
                20000,
            );

            await this.browser.yaWaitForVisible(PO.oneOrg.posts.firstItem.reactions(), 'Лайки не загрузились');
            await this.browser.moveToObject('body', 0, 0);
            await this.browser.assertView('plain', [PO.oneOrg.posts()]);

            await this.browser.yaCheckBaobabCounter(PO.oneOrg.posts.secondItem.textMore(), {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/more[@behaviour@type="dynamic"]',
            });

            const requestUrlMore = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-posts"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.oneOrg.posts.moreButton());
                return this.browser.yaWaitForVisible(PO.popup.oneOrg.posts(), 'Публикации не загрузились в попапе');
            }, {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/more-button',
                behaviour: {
                    type: 'dynamic',
                },
            }));

            assert.isNotNull(requestUrlMore, 'В запросе попапа отсутствует request_intent');
        });

        it('Просмотрщик', async function() {
            await this.browser.yaOpenSerp({
                text: 'монтаж-кам проспект ленинского комсомола',
                yandex_login: 'stas.mihailov666',
                user_time: userTime,
                data_filter: 'companies',
            }, PO.oneOrg());

            // Устанавливаем ширину вручную, чтобы таб не прятался под кнопку Ещё
            await this.browser.setViewportSize({ width: 1600, height: 1024 });

            await this.browser.click(PO.oneOrg.tabsMenu.posts());
            await this.browser.yaWaitForVisible(PO.oneOrg.posts(), 'Таб с публикациями не загрузился', 20000);

            await this.browser.yaWaitForVisible(
                PO.oneOrg.posts.firstItem.gallery(),
                'Галерея в посте не показалась',
                20000,
            );

            await this.browser.yaWaitForVisible(PO.oneOrg.posts.firstItem.reactions(), 'Лайки не загрузились');

            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.oneOrg.posts.firstItem.gallery.firstChild());
                return this.browser.yaWaitForVisible(PO.photoViewer(), 'Просмотрщик картинок не открылся');
            }, {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/thumb',
            });

            // что бы появились контролы листания
            await this.browser.moveToObject(PO.photoViewer());

            await this.browser.assertView('plain', [PO.photoViewer()]);

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.next(), {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/viewer/preview/arrow-right',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.prev(), {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/viewer/preview/arrow-left',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.gallery.thirdItem(), {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/viewer/gallery/item',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.photoViewer.close());
                return this.browser.yaWaitForHidden(PO.photoViewer(), 'Просмотрщик картинок не закрылся');
            }, {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/viewer/close[@tags@close=1]',

                behaviour: {
                    type: 'dynamic',
                },
            });
        });

        it('Лайки', async function() {
            await this.browser.yaOpenSerp({
                text: 'монтаж-кам проспект ленинского комсомола',
                user_time: userTime,
                data_filter: 'companies',
            }, PO.oneOrg());

            // Устанавливаем ширину вручную, чтобы таб не прятался под кнопку Ещё
            await this.browser.setViewportSize({ width: 1600, height: 1024 });

            await this.browser.click(PO.oneOrg.tabsMenu.posts());
            await this.browser.yaWaitForVisible(PO.oneOrg.posts(), 'Таб с публикациями не загрузился', 20000);

            await this.browser.yaWaitForVisible(
                PO.oneOrg.posts.firstItem.photos(),
                'Фотография в посте не показалась',
                20000,
            );

            await this.browser.yaWaitForVisible(
                PO.oneOrg.posts.firstItem.reactions(),
                'Лайки у первого поста не загрузились',
            );

            await this.browser.moveToObject(PO.oneOrg.posts.firstItem.reactions.like());
            await this.browser.assertView('reactions-hover', PO.oneOrg.posts.firstItem.reactions());
            await this.browser.click(PO.oneOrg.posts.firstItem.reactions.like());
            await this.browser.moveToObject(PO.oneOrg.posts.firstItem());

            await this.browser.yaShouldBeSame(
                PO.oneOrg.posts.firstItem.reactions.like(),
                PO.oneOrg.posts.firstItem.reactions.itemIsMy(),
                'Лайк не проставился',
            );

            await this.browser.assertView('reactions-liked', PO.oneOrg.posts.firstItem.reactions());
            await this.browser.click(PO.oneOrg.posts.firstItem.reactions.dislike());

            await this.browser.yaShouldBeSame(
                PO.oneOrg.posts.firstItem.reactions.dislike(),
                PO.oneOrg.posts.firstItem.reactions.itemIsMy(),
                'Дизлайк не проставился',
            );

            await this.browser.yaCheckBaobabCounter(() => {}, [
                {
                    path: '/$page/$parallel/$result/composite/tabs/posts/posts/reactions/like',
                    behaviour: { type: 'dynamic' },
                },
                {
                    path: '/$page/$parallel/$result/composite/tabs/posts/posts/reactions/dislike',
                    behaviour: { type: 'dynamic' },
                },
            ]);

            await this.browser.click(PO.oneOrg.posts.firstItem.reactions.dislike());

            await this.browser.yaShouldNotExist(
                PO.oneOrg.posts.firstItem.reactions.itemIsMy(),
                'Оценка пользователя не сбросилась',
            );
        });

        it('Комментарии', async function() {
            await this.browser.yaOpenSerp({
                text: 'монтаж-кам проспект ленинского комсомола',
                yandex_login: 'stas.mihailov666',
                user_time: userTime,
                data_filter: 'companies',
            }, PO.oneOrg());

            // Устанавливаем ширину вручную, чтобы таб не прятался под кнопку Ещё
            await this.browser.setViewportSize({ width: 1600, height: 1024 });

            await this.browser.click(PO.oneOrg.tabsMenu.posts());
            await this.browser.yaWaitForVisible(PO.oneOrg.posts(), 'Таб с публикациями не загрузился', 20000);

            await this.browser.yaWaitForVisible(
                PO.oneOrg.posts.firstItem.photos(),
                'Фотография в посте не показалась',
                20000,
            );

            await this.browser.yaWaitForVisible(PO.oneOrg.posts.firstItem.reactions.comment(), 'Нет иконки комментариев');
            await this.browser.click(PO.oneOrg.posts.firstItem.reactions.comment());
            await this.browser.assertView('comments-opened', PO.oneOrg.posts.firstItem.reactions.comment());
            await this.browser.yaWaitForVisible(PO.oneOrg.posts.firstItem.comments(), 'Комментарии не открылись');
            await this.browser.click(PO.oneOrg.posts.firstItem.reactions.comment());
            await this.browser.yaWaitForHidden(PO.oneOrg.posts.firstItem.comments(), 'Комментарии не скрылись');

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$parallel/$result/composite/tabs/posts/posts/reactions/comment',
                behaviour: { type: 'dynamic' },
            });
        });
    });

    describe('Таб в попапе', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'Лига джентльменов Алтайский край ателье',
                user_time: userTime,
                data_filter: 'companies',
            }, PO.oneOrg());

            await this.browser.click(PO.oneOrg.tabsMenu.posts());
            await this.browser.yaWaitForVisible(PO.oneOrg.posts(), 'Таб с публикациями не загрузился', 20000);
            // await this.browser.click(PO.oneOrg.reviewsPreview.title.link());
            await this.browser.click(PO.oneOrg.posts.moreButton());
            await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'Попап не загрузился');
            // await this.browser.click(PO.popup.oneOrg.tabsMenu.posts());
            await this.browser.yaWaitForVisible(PO.popup.oneOrg.posts(), 'Публикации не загрузились в попапе');

            await this.browser.yaWaitForVisible(
                PO.popup.oneOrg.posts.firstItem.gallery(),
                'Галерея в посте не показалась',
                20000,
            );

            await this.browser.yaWaitForVisible(PO.popup.oneOrg.posts.firstItem.reactions(), 'Лайки не загрузились');
        });

        it('Основные проверки', async function() {
            await this.browser.yaCheckBaobabCounter(PO.popup.oneOrg.posts.secondItem.textMore(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/more[@behaviour@type="dynamic"]',
            });

            await this.browser.yaShouldNotExist(PO.popup.oneOrg.posts.sixthItem(), 'Должно быть только 5 постов');

            const requestUrlMore = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-posts"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.popup.oneOrg.posts.moreButton());
                return this.browser.yaWaitForVisible(PO.popup.oneOrg.posts.sixthItem(), 'Шестой пост не загрузился');
            }, {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/more-button',
                behaviour: {
                    type: 'dynamic',
                },
            }));

            assert.isNotNull(requestUrlMore, 'В запросе попапа отсутствует request_intent');
        });

        it('Просмотрщик', async function() {
            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.popup.oneOrg.posts.firstItem.gallery.firstChild());
                return this.browser.yaWaitForVisible(PO.photoViewer(), 'Просмотрщик картинок в попапе не открылся');
            }, {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/thumb',
                behaviour: {
                    type: 'dynamic',
                },
            });

            // чтобы появились контролы листания
            await this.browser.moveToObject(PO.photoViewer());

            await this.browser.yaWaitForVisible(PO.photoViewer.next(), 'Контролы листания не появились');

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.next(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/viewer/preview/arrow-right',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.prev(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/viewer/preview/arrow-left',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(PO.photoViewer.gallery.thirdItem(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/viewer/gallery/item',
                behaviour: {
                    type: 'dynamic',
                },
            });

            await this.browser.yaCheckBaobabCounter(async () => {
                await this.browser.click(PO.photoViewer.close());
                return this.browser.yaWaitForHidden(PO.photoViewer(), 'Просмотрщик картинок не закрылся');
            }, {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/posts/ajax-loader/posts/viewer/close[@tags@close=1]',

                behaviour: {
                    type: 'dynamic',
                },
            });
        });

        it('Лайки', async function() {
            await this.browser.yaWaitForVisible(
                PO.popup.oneOrg.posts.firstItem.reactions(),
                'Лайки не загрузились в попапе',
            );

            await this.browser.click(PO.popup.oneOrg.posts.moreButton());
            await this.browser.yaWaitForVisible(PO.popup.oneOrg.posts.sixthItem(), 'Шестой пост не загрузился');
            await this.browser.yaScroll(PO.popup.oneOrg.posts.sixthItem());

            await this.browser.yaWaitForVisible(
                PO.popup.oneOrg.posts.sixthItem.reactions(),
                'Лайки в шестом посте в попапе не загрузились',
            );
        });

        it('Комментарии', async function() {
            await this.browser.yaWaitForVisible(
                PO.popup.oneOrg.posts.firstItem.reactions.comment(),
                'Нет иконки комментариев',
            );

            await this.browser.click(PO.popup.oneOrg.posts.firstItem.reactions.comment());
            await this.browser.yaWaitForVisible(PO.popup.oneOrg.posts.firstItem.comments(), 'Комментарии не открылись');
            await this.browser.click(PO.popup.oneOrg.posts.firstItem.reactions.comment());
            await this.browser.yaWaitForHidden(PO.popup.oneOrg.posts.firstItem.comments(), 'Комментарии не скрылись');

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '/' + [
                    '$page',
                    '$parallel',
                    '$result',
                    'modal-popup',
                    'modal-content-loader',
                    'content',
                    'company',
                    'tabs',
                    'posts',
                    'ajax-loader',
                    'posts',
                    'reactions',
                    'comment',
                ].join('/'),
                behaviour: { type: 'dynamic' },
            });
        });
    });
});
