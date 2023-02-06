import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок CartTotalInformation
 * @param {PageObject.CartTotalInformation} cartTotalInformation
 */
export default makeSuite('Доступна для незрячих', {
    story: {
        'Озвучивает изменившиеся данные': makeCase({
            async test() {
                const EXPECTED_ROLE = 'alert';
                const EXPECTED_ARIA_LIVE = 'polite';
                const EXPECTED_ARIA_ATOMIC = 'true';

                const root = await this.cartTotalInformation.root;
                const rootRole = this.browser.getAttribute(root.selector, 'role');
                const rootAriaLive = this.browser.getAttribute(root.selector, 'aria-live');
                const rootAriaAtomic = this.browser.getAttribute(root.selector, 'aria-atomic');

                await rootRole.should.eventually.be.equal(
                    EXPECTED_ROLE,
                    `ARIA роль должна быть ${EXPECTED_ROLE}`
                );
                await rootAriaLive.should.eventually.be.equal(
                    EXPECTED_ARIA_LIVE,
                    `Приоритет озвучки должен быть ${EXPECTED_ARIA_LIVE}`
                );
                await rootAriaAtomic.should.eventually.be.equal(
                    EXPECTED_ARIA_ATOMIC,
                    `Объём озвучки должен быть ${EXPECTED_ARIA_ATOMIC}`
                );
            },
        }),

        'Находится по заголовку': makeCase({
            async test() {
                const EXPECTED_TITLE_TEXT = 'Итоговая сумма';
                const EXPECTED_TITLE_TAG = 'h2';

                const title = await this.cartTotalInformation.title;
                const titleText = this.cartTotalInformation.getTitleText();
                const titleTag = this.browser.getTagName(title.selector);

                await titleText.should.eventually.to.be.equal(
                    EXPECTED_TITLE_TEXT,
                    `Содержание заголовка должно быть "${EXPECTED_TITLE_TEXT}".`
                );
                await titleTag.should.eventually.to.be.equal(
                    EXPECTED_TITLE_TAG,
                    `Тег заголовка должен быть "${EXPECTED_TITLE_TAG}".`
                );
            },
        }),
    },
});
