// suites
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

// page objects
import SearchResultAlert from '@self/platform/widgets/content/SearchResultAlert/__pageObject';
import SearchResultAlertSrc from '@self/root/src/widgets/parts/SearchResultAlert/__pageObject';
import ProductAlert from '@self/platform/widgets/content/ProductAlert/__pageObject';
import {setCookies} from '@yandex-market/gemini-extended-actions';

// Подойдёт любой из блоков, склеиваем в один селектор
const alertSelector = [
    SearchResultAlertSrc.root,
    SearchResultAlert.root,
].join(', ');


export default {
    suiteName: 'RedirectBlock',
    before(actions) {
        // не ставим дефолтные куки, потому что это вызывает перезагрузку и пропажу блока с алертом (juice)
        MainSuite.before(actions);
    },
    childSuites: [
        {
            suiteName: 'KM-from-search',
            url: {
                pathname: 'product--besprovodnye-naushniki-apple-airpods/14206836',
                query: {
                    was_redir: 1,
                    suggest_text: 'Apple AirPods',
                },
            },
            selector: ProductAlert.root,
            before(actions) {
                setCookies.setCookies.call(actions, [{
                    name: 'sFiLCoDh',
                    value: '1',
                }]);
            },
            capture(actions) {
                actions.waitForElementToShow(ProductAlert.root, 5000);
            },
        },
        {
            suiteName: 'Brands-from-search !!ALERT IF FAILED!!',
            url: {
                pathname: '/brands--apple/153043',
                query: {
                    was_redir: 1,
                    suggest_text: 'apple',
                },
            },
            selector: alertSelector,
            capture(actions) {
                actions.waitForElementToShow(alertSelector, 3000);
            },
        },
        {
            suiteName: 'Licensor-from-search',
            url: {
                pathname: '/licensor--marvel/15186891',
                query: {
                    was_redir: 1,
                    suggest_text: 'marvel',
                },
            },
            selector: alertSelector,
            capture(actions) {
                actions.wait(1000);
                actions.waitForElementToShow(alertSelector, 3000);
            },
        },
        {
            suiteName: 'Franchise-from-search',
            url: {
                pathname: '/franchise--barboskiny/14022570',
                query: {
                    was_redir: 1,
                    suggest_text: 'Барбоскины',
                },
            },
            selector: alertSelector,
            before(actions) {
                setCookies.setCookies.call(actions, [{
                    name: 'sFiLCoDh',
                    value: '1',
                }]);
            },
            capture(actions) {
                actions.wait(1000);
                actions.waitForElementToShow(alertSelector, 3000);
            },
        },
    ],
};
