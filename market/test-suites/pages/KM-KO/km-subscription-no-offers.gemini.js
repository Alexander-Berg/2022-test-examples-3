import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import KMSubscribe from '@self/platform/spec/gemini/test-suites/blocks/KM/n-product-subscribe_action_subscribe';
import KMSubscribed from '@self/platform/spec/gemini/test-suites/blocks/KM/n-product-subscribe_action_subscribed';
import ProductSubscribeForm from '@self/platform/widgets/content/NotOnSale/components/Subscription/__pageObject';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KM-subscription-no-offers',
    url: '/product--telefon-nokia-3310/160291',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...KMSubscribe,
            before(actions) {
                actions.wait(500); // боремся с тряской
            },
        },
        {
            ...KMSubscribed,
            before(actions, find) {
                actions
                    .sendKeys(find(`${ProductSubscribeForm.name} input`), 'GeminiUser')
                    .sendKeys(find(`${ProductSubscribeForm.email} input`), 'gemini2random@yatest.ru')
                    .click(find(`${ProductSubscribeForm.actionButton} button`))
                    // TemporalLoader : TIMEOUT_FOR_SHOW
                    .wait(200)
                    // Не использовал waitForElementToHide, потому что он по какой-то причине не фиксирует исчезновение
                    // прелоадера из DOM-а и просто таймаутит. Прелоадер может крутиться довольно долго, поэтому 5 сек.
                    // eslint-disable-next-line no-new-func
                    .waitForJSCondition(new Function(`
                        return !document.querySelector('${ProductSubscribeForm.preloader}');
                    `), 5000);
            },
            after(actions, find) {
                // Отписываемся от итема. Иначе может зааффектить другие тесты.
                // При подписке итем добавляется в избранное. Следующий тест может залогиниться в рамках той же сессии,
                // после чего смёржатся избранное незалогина и залогина. Это приводило к тому, что падали скринтесты
                // пустой страницы "избранное".
                actions
                    .click(find(`${ProductSubscribeForm.actionButton} button`))
                    // TemporalLoader : TIMEOUT_FOR_SHOW
                    .wait(200)
                    // eslint-disable-next-line no-new-func
                    .waitForJSCondition(new Function(`
                        return !document.querySelector('${ProductSubscribeForm.preloader}');
                    `), 5000);
            },
        },
    ],
};
