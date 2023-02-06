import 'hermione';
import {expect} from 'chai';

import {login, PageObject} from '../../helpers';
import {CHECK_INTERVAL, TIMEOUT_MS} from '../../constants';
import Button from '../../page-objects/button';
import AttributeValue from '../../page-objects/attributeValue';

/**
 * Для теста нужен аккаунт покупателя со склеенными uid-ами.
 * Если тест падает, возможно, у покупателя по этому url
 * поле glue опустело, в этом случае нужно заменить url.
 */
const PAGE_URL = '/entity/customer@24406984';

const waitForHeightChange = (
    browser: WebdriverIO.Browser,
    element: WebdriverIO.Element,
    heightInterval: [number, number]
): Promise<true | void> => {
    return browser.waitUntil(
        async () => {
            const glueHeight = Number(await element.getProperty('offsetHeight'));

            const [startHeightInterval, stopHeightInterval] = heightInterval;

            return glueHeight >= startHeightInterval && glueHeight <= stopHeightInterval;
        },
        {
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
        }
    );
};

/**
 * Проверяем, что:
 * Под UID-ом будет блок со склеенными uid-ами который по умолчанию свернут,
 * по клику блок успешно разворачивается и в нём отображаются uid-ы,
 * затем по клику блок сворачивается
 */
describe(`ocrm-1588: Склеенные UID на карточке клиента`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`Блок со склеенными uid-ами разворачивается и сворачивается`, async function() {
        const uidAttribute = new AttributeValue(this.browser, 'body', '[data-ow-test-attribute-container="uid"]');
        const collapsibleGlue = new PageObject(this.browser, 'body', '[data-ow-test-attribute-container="glue"]');
        const collapsibleGlueButton = new Button(this.browser, 'body', '[data-ow-test-collapsible-content-button]');

        await uidAttribute.isDisplayed();
        const glueIsVisible = await collapsibleGlue.isDisplayed();

        expect(glueIsVisible).to.equal(true, 'Список склеенных uid-ов не отображается');

        const collapsibleContent = await collapsibleGlue.elem('span div');

        await collapsibleGlueButton.clickButton();
        const glueExpanded = await waitForHeightChange(this.browser, collapsibleContent, [18, 300]);

        expect(glueExpanded).to.equal(true, 'Список склеенных uid-ов не раскрылся');

        await collapsibleGlueButton.clickButton();
        const glueCollapsed = await waitForHeightChange(this.browser, collapsibleContent, [0, 0]);

        expect(glueCollapsed).to.equal(true, 'Список склеенных uid-ов не свернулся');
    });
});
