/* global window */
'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs('API мобильного приложения', function() {
    it('Изменение запроса на странице', function() {
        return this.browser
            .yaOpenSerp({
                text: 'клбаса',
                exp_flags: 'fake_yandex_apps_api=1'
            })
            .execute(function() {
                return window.YandexApplicationsAPIBackend.setQueryText.calledWith;
            }).then(result => assert.deepEqual(result.value, ['колбаса']));
    });

    it('Открытие вертикальных сервисов в отдельных окнах/табах', function() {
        let href;

        return this.browser
            .yaOpenSerp({
                text: 'котики',
                exp_flags: 'fake_yandex_apps_api=1'
            })
            .yaCheckLink(PO.imagesConstructor.firstImage()).then(url => href = url.href)
            .click(PO.imagesConstructor.firstImage())
            .execute(function() {
                return window.YandexApplicationsAPIBackend.openVertService.calledWith;
            }).then(result => assert.deepEqual(result.value, ['images', href, false]));
    });

    it('Открытие связанных запросов', function() {
        let href;
        let text;

        return this.browser
            .yaOpenSerp({
                text: 'анджелина джоли',
                exp_flags: 'fake_yandex_apps_api=1'
            })
            .yaCheckLink(PO.entityCard.firstFactLink(), { target: '' }).then(url => {
                href = url.search;
                text = url.query.text;
            })
            .click(PO.entityCard.firstFactLink())
            .execute(function() {
                return window.YandexApplicationsAPIBackend.openRelatedQuery.calledWith;
            }).then(result => assert.deepEqual(JSON.parse(result.value[0]), { query: text, url: href }));
    });

    it('Открытие связанных запросов после исправления опечатки', function() {
        let href;
        let text;

        return this.browser
            .yaOpenSerp({
                text: 'лев толсто',
                exp_flags: 'fake_yandex_apps_api=1'
            })
            .click(PO.misspell.buttonLink())
            .yaWaitForVisible(PO.entityCard())
            .yaCheckLink(PO.entityCard.firstFactLink(), { target: '' }).then(url => {
                href = url.search;
                text = url.query.text;
            })
            .click(PO.entityCard.firstFactLink())
            .execute(function() {
                return window.YandexApplicationsAPIBackend.openRelatedQuery.calledWith;
            }).then(result => assert.deepEqual(JSON.parse(result.value[0]), { query: text, url: href }));
    });

    it('Счётчик первого показа страницы', function() {
        return this.browser
            .yaOpenSerp({
                text: 'test',
                exp_flags: 'fake_yandex_apps_api=1;page_visibility_counters=1'
            })
            .yaCheckCounter2(() => {
                this.browser.execute(function() {
                    window.YandexApplicationsAPIBackend.pageVisibilityChange();
                });
            }, {
                path: '/tech/request_entry_completed'
            });
    });
});
