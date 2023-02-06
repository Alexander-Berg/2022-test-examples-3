'use strict';

import {makeSuite, importSuite, mergeSuites} from 'ginny';

/**
 * Тест на блок подписки на услугу по логину или почте.
 * @param {PageObject.SubscribersGroup} subscribersGroup
 */
export default makeSuite('Подписка на услугу.', {
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления блока подписки на услугу.', () =>
                    this.subscribersGroup.waitForExist(),
                );
            },
        },
        importSuite('SubscribersGroup/__subscribeByEmail', {
            pageObjects: {
                subscribe() {
                    return this.createPageObject(
                        'Subscribe',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.subscribersGroup,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.subscribersGroup.subscribeByEmail,
                    );
                },
                multiTextInput() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('MultiTextInput', this.subscribe);
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
                tags() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Tags', this.subscribe);
                },
                tag() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Tag', this.tags, this.tags.getItemByIndex(0));
                },
                expander() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.tags);
                },
            },
        }),
        importSuite('SubscribersGroup/__subscribeByLogin', {
            pageObjects: {
                subscribe() {
                    return this.createPageObject(
                        'Subscribe',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.subscribersGroup,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.subscribersGroup.subscribeByLogin,
                    );
                },
                suggest() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Suggest', this.subscribe);
                },
                tags() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Tags', this.subscribe);
                },
                tag() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Tag', this.tags, this.tags.getItemByIndex(0));
                },
                expander() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.tags);
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
    ),
});
