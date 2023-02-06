import {makeSuite, makeCase} from 'ginny';
import {tail} from 'ambar';

/**
 * @param {PageObject.ProductTabs} productTabs
 */
export default makeSuite('Вкладка', {
    environment: 'kadavr',
    story: {
        'По клику': {
            'переходит на ожидаемую страницу': makeCase({
                params: {
                    expectedPage: 'Ожидаемая страница',
                    selector: 'Селектор вкладки для клика',
                },
                async test() {
                    const {expectedPage, selector} = this.params;

                    await this.browser.yaWaitForChangeUrl(() => this.browser.click(selector));

                    const {pathname} = await this.browser.yaParseUrl();
                    const pageId = tail(pathname.split('/'));

                    await this.expect(pageId, `Не произошло перехода на страницу ${expectedPage}`)
                        .to.be.equal(expectedPage);
                },
            }),
        },
    },
});
