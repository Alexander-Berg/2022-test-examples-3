specs({
    feature: 'DynamicComments',
}, () => {
    hermione.skip.in('firefox', 'Firefox криво работает');
    hermione.only.notIn('safari13');
    it('Базовая функциональность и ответ на первый комментарий', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/login.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForVisible(PO.turboDynamicComments.loader(), 'Лоадер не показался')
            //.assertView('loading', PO.turboDynamicComments()) — это очень не стабильно, просто проверяем, что он был и скрылся
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .assertView('plain', PO.turboDynamicComments())
            .yaWatchInnerHeight(function() {
                return this.click(PO.turboDynamicComments.firstCommentAnswer.toggle())
                    .yaWaitForVisible(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 'Не открылась форма для ответа на первый комментарий')
                    .click(PO.footer());
            }, 3000) // на меньших таймаутах начинает плавать в serchapp/chrome-phone
            .assertView('firstCommentReplyForm', PO.turboDynamicComments())
            .yaWatchInnerHeight(function() {
                return this
                    .click(PO.turboDynamicComments.firstReplyAnswer.toggle())
                    .yaShouldBeVisible(PO.turboDynamicComments.firstReplyAnswer.commentForm(), 'Не открылась форма для ответа на первый ответный комментарий')
                    .click(PO.footer());
            })
            .assertView('firstReplyCommentReplyForm', PO.turboDynamicComments())
            .yaWatchInnerHeight(function() {
                return this
                    .click(PO.turboDynamicComments.firstReplyAnswer.commentForm.hide())
                    .yaShouldNotBeVisible(PO.turboDynamicComments.firstReplyAnswer.commentForm(), 'Не скрылась форма для ответа на первый ответный комментарий')
                    .setValue(PO.turboDynamicComments.firstCommentAnswer.commentForm.textarea(), '      \n\n          ')
                    .click(PO.turboDynamicComments.firstCommentAnswer.commentForm.send())
                    .yaShouldNotBeVisible(PO.turboDynamicComments.firstCommentAnswer.commentFormDisabled())
                    .setValue(PO.turboDynamicComments.firstCommentAnswer.commentForm.textarea(), '       \n  Вторая строка\n   ')
                    .click(PO.turboDynamicComments.firstCommentAnswer.commentForm.send());
            })
            .assertView('disabledReplyForm', PO.turboDynamicComments.firstCommentAnswer.commentFormDisabled())
            .yaWaitForHidden(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 5000, 'Форма не скрылась')
            .assertView('replyToComment', PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Ответ на isreply-комментарий', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/login.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .yaWatchInnerHeight(function() {
                return this
                    .click(PO.turboDynamicComments.firstReplyAnswer.toggle())
                    .yaShouldBeVisible(PO.turboDynamicComments.firstReplyAnswer.commentForm(), 'Не открылась форма для ответа на первый ответный комментарий')
                    .setValue(PO.turboDynamicComments.firstReplyAnswer.commentForm.textarea(), 'Первая строка \n\nи последняя')
                    .click(PO.turboDynamicComments.firstReplyAnswer.commentForm.send());
            })
            .yaWaitForHidden(PO.turboDynamicComments.firstReplyAnswer.commentForm(), 5000, 'Форма не скрылась')
            .assertView('replyToReplyComment', PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Новый комментарий', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/login.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .yaWatchInnerHeight(function() {
                return this
                    .click(PO.turboDynamicComments.newCommentButton())
                    .setValue(PO.turboDynamicComments.newCommentForm.commentForm.textarea(), 'Первая строка \n\nи последняя')
                    .click(PO.turboDynamicComments.newCommentForm.commentForm.send())
                    .yaWaitForHidden(PO.turboDynamicComments.newCommentForm.commentForm(), 5000, 'Форма не скрылась');
            })
            .assertView('newComment', PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Работает пагинация реплаев', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/login.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .click(PO.turboDynamicComments.showMoreReplies())
            .assertView('repliesShowMore', PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Загружается кнопка, если нет комментариев', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/default-no-data.json')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .assertView('comments', PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Работает пагинация верхнеуровневых комментариев', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/login.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .click(PO.turboDynamicComments.showMoreComments())
            .yaWaitForVisible(PO.turboDynamicComments.showMoreCommentsLoading(), 'Не началась загрузка следующей страницы')
            .yaWaitForHidden(PO.turboDynamicComments.showMoreCommentsLoading(), 3000, 'Загрузка не закончилась')
            .assertView('commentsShowMore', PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Возможность ответить на верхнеуровневый комментарий после загрузки следующей страницы комментов', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/login.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .click(PO.turboDynamicComments.showMoreComments())
            .yaWaitForVisible(PO.turboDynamicComments.showMoreCommentsLoading(), 'Не началась загрузка следующей страницы')
            .yaWaitForHidden(PO.turboDynamicComments.showMoreCommentsLoading(), 4000, 'Загрузка не закончилась')
            .yaIndexify(PO.turboComments.rootComment())
            .yaWatchInnerHeight(function() {
                return this
                    .click(PO.turboDynamicComments.secondRootAnswer.toggle())
                    .yaShouldBeVisible(PO.turboDynamicComments.secondRootAnswer.commentForm(), 'Не открылась форма для ответа на первый комментарий')
                    .setValue(PO.turboDynamicComments.secondRootAnswer.commentForm.textarea(), '       \n  Вторая строка\n   ')
                    .click(PO.turboDynamicComments.secondRootAnswer.commentForm.send());
            })
            .yaWaitForHidden(PO.turboDynamicComments.secondRootAnswer.commentForm(), 5000, 'Форма не скрылась')
            .assertView('replyOnSecondRootComment', PO.turboDynamicComments());
    });

    hermione.skip.in('firefox', 'Firefox криво работает');
    hermione.only.notIn('safari13');
    it('Не загрузились комментарии', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/error-on-load.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .yaWaitForHidden(PO.turboDynamicComments());
    });

    hermione.only.notIn('safari13');
    it('Ошибка при отправке комментария', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/error-on-send.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .yaIndexify(PO.turboComments.rootComment())
            .yaWatchInnerHeight(function() {
                return this
                    .click(PO.turboDynamicComments.firstCommentAnswer.toggle())
                    .yaShouldBeVisible(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 'Не открылась форма для ответа на первый комментарий')
                    .setValue(PO.turboDynamicComments.firstCommentAnswer.commentForm.textarea(), '       \n  Вторая строка\n   ')
                    .click(PO.turboDynamicComments.firstCommentAnswer.commentForm.send());
            })
            .yaWaitForHidden(PO.turboDynamicComments.firstCommentAnswer.commentFormDisabled(), 4000, 'Форма не стала активной')
            .yaShouldBeVisible(PO.turboDynamicComments.firstCommentAnswer.commentForm())
            .yaAssertViewportView('alert');
    });

    hermione.skip.in('firefox', 'Нельзя выйти из закрытого iframe: https://bugzilla.mozilla.org/show_bug.cgi?id=1399032');
    hermione.only.notIn('safari13');
    it('Поведение при логине/разлогине', function() {
        const login = 'Test user';
        const loginInputSelector = 'input[name="login"]';
        const passwordInputSelector = 'input[name="password"]';
        const submitButtonSelector = '.button_submit';

        return this.browser
            .url('/turbo?stub=dynamiccomments/default.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .click(PO.turboDynamicComments.firstCommentAnswer.toggle())
            .yaWaitForVisible(PO.turboAuthIframe(), 'Должен отобразиться iframe с формой авторизации')
            .click(PO.turboModal.close())
            .yaWaitForHidden(PO.turboModal(), 'Попап авторизации не закрылся')
            .yaShouldNotBeVisible(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 'Открылась форма для ответа на первый комментарий')
            .click(PO.turboAuth.button())
            .yaWaitForVisible(PO.turboAuthIframe(), 'Должен отобразиться iframe с формой авторизации')
            .element(PO.turboAuthIframe())
            .then(el => this.browser
                .frame(el.value)
                .setValue(loginInputSelector, login)
                .setValue(passwordInputSelector, '123')
                .click(submitButtonSelector)
                .frameParent()
                .yaWaitForHidden(PO.turboAuthIframe(), 'iframe с авторизацией должен закрыться')
            )
            .click(PO.turboDynamicComments.firstCommentAnswer.toggle())
            .yaShouldBeVisible(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 'Не открылась форма для ответа на первый комментарий, если пользователь уже залогинен')
            .click(PO.turboAuth.button())
            .yaWaitForHidden(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 'Не скрылась форма для ответа на первый комментарий после разлогина')
            .click(PO.turboDynamicComments.firstCommentAnswer.toggle())
            .yaWaitForVisible(PO.turboAuthIframe(), 'Должен отобразиться iframe с формой авторизации')
            .element(PO.turboAuthIframe())
            .then(el => this.browser
                .frame(el.value)
                .setValue(loginInputSelector, login)
                .setValue(passwordInputSelector, '123')
                .click(submitButtonSelector)
                .frameParent()
                .yaWaitForHidden(PO.turboAuthIframe(), 'iframe с авторизацией должен закрыться')
            )
            .yaShouldBeVisible(PO.turboDynamicComments.firstCommentAnswer.commentForm(), 'Не открылась форма для ответа на первый комментарий после залогина');
    });

    hermione.only.notIn('safari13');
    it('Корректно работает пагинация с лимитом > 1', function() {
        return this.browser
            .url('/turbo?stub=dynamiccomments/large.json')
            .yaWaitForVisible(PO.turboDynamicComments(), 'Блок динамических комментариев не появился на странице')
            .yaWaitForHidden(PO.turboDynamicComments.loader(), 'Лоадер не скрылся')
            .assertView('withLargeOffset', PO.turboDynamicComments())
            .click(PO.turboDynamicComments.showMoreReplies())
            .assertView('withLargeOffsetFirstOpened', PO.turboDynamicComments())
            .click(PO.turboDynamicComments.showMoreComments())
            .yaWaitForVisible(PO.turboDynamicComments.showMoreCommentsLoading(), 'Не началась загрузка следующей страницы')
            .yaWaitForHidden(PO.turboDynamicComments.showMoreCommentsLoading(), 5000, 'Загрузка не закончилась')
            .assertView('withLargeOffsetMoreOpened', PO.turboDynamicComments());
    });
});
