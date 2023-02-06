import {makeSuite, mergeSuites, makeCase} from '@yandex-market/ginny';
// state
import YandexGoProductPageState from '@self/root/src/spec/hermione/helpers/yandexGo/pages/YandexGoProductPageState';
// page objects
import StickyBoxPO from '@self/root/src/components/StickyBox/__pageObject';
import MandrelDevToolsPO from '@self/platform/spec/page-objects/mandrel/DevTools';
// helpers
import {getSonyHeadPhonesRoute} from '@self/root/src/spec/hermione/helpers/yandexGo/pages/YandexGoProductPageState/routes';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница товара Yandex Go.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64980',
    id: 'm-touch-3862',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaDisableCSSAnimation();
                await this.browser.yaTestLogin();
                const state = new YandexGoProductPageState();
                await state.createSonyHeadphonesState();
                await state.setState(this.browser);

                const {pageId, params} = getSonyHeadPhonesRoute();

                await this.browser.yaOpenPage(pageId, params);
            },
            async afterEach() {
                await this.browser.yaLogout();
            },
        },
        makeSuite('По умолчанию', {
            story: {
                'отображается верно': makeCase({
                    async test() {
                        await this.browser.yaRemoveElement(MandrelDevToolsPO.root);

                        // скрываем StickyBox.fixedBlock, чтобы YandexGoHeader не прыгал на скриншоте
                        // https://jing.yandex-team.ru/files/asvasilenko/chrome_current_0.png
                        await this.browser.yaAddCssStyles(`${StickyBoxPO.fixedBlock}`, {
                            'display': 'none!important',
                            'box-shadow': 'unset!important',
                        });

                        await this.browser.assertView('plain', 'body');
                    },
                }),
            },
        })
    ),
});
