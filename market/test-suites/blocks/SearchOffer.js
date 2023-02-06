import {makeSuite, makeCase} from 'ginny';
import {get, negate, eq, filter, head, flow} from 'lodash/fp';

const isNotBlankHostname = flow([
    get('hostname'),
    negate(eq('blank')),
]);

/**
 * @param {PageObject.SearchOffer} snippet
 */
export default makeSuite('Листовой сниппет оффера.', {
    environment: 'kadavr',
    story: {
        'При клике': {
            'должен открываться сайт магазина в другой вкладке': makeCase({
                async test() {
                    // На случай, если сайт магазина отвалится -
                    // проверяем только факт редиректа, а не загрузку страницы
                    const redirectedSuccessfully = () =>
                        this.browser
                            .yaParseUrl()
                            .then(isNotBlankHostname);

                    const secondTabOpened = () =>
                        this.browser
                            .getTabIds()
                            .then(get('length'))
                            .then(eq(2));

                    const hostnameBeforeClick = await this.browser
                        .yaParseUrl().then(get('hostname'));
                    const firstTabId = await this.browser.getCurrentTabId();

                    await this.snippet.root.click();

                    await this.browser.waitUntil(secondTabOpened, 10000, 'Вкладка с магазином не открылась');

                    const secondTabId = await this.browser
                        .getTabIds()
                        .then(filter(negate(eq(firstTabId))))
                        .then(head);
                    await this.browser.switchTab(secondTabId);

                    await this.browser.waitUntil(redirectedSuccessfully, 20000, 'Не дождались перехода в магазин');

                    const url = await this.browser.yaParseUrl();

                    const hostnameAfterClick = url.hostname;

                    await this.browser.close();

                    return this.expect(hostnameAfterClick)
                        .to.not.be.equal(hostnameBeforeClick, 'Успешно перешли в магазин');
                },
            }),
        },
    },
});
