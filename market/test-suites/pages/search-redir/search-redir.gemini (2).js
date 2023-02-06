// page objects
import SearchResultAlert from '@self/root/src/widgets/parts/SearchResultAlert/__pageObject';

// helpers
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'RedirectBlock',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            suiteName: 'KM-from-search',
            url: {
                pathname: '/product--smartfon-yandex-telefon/177547282',
                query: {
                    was_redir: 1,
                    suggest_text: 'яндекс.телефон',
                },
            },
            selector: SearchResultAlert.root,
            capture(actions) {
                actions.waitForElementToShow(SearchResultAlert.root, 3000);
            },
        },
        {
            suiteName: 'Brands-from-search',
            url: {
                pathname: '/brands--apple/153043',
                query: {
                    was_redir: 1,
                    suggest_text: 'apple',
                },
            },
            selector: SearchResultAlert.root,
            capture(actions) {
                actions.waitForElementToShow(SearchResultAlert.root, 3000);
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
            selector: SearchResultAlert.root,
            capture(actions) {
                actions.waitForElementToShow(SearchResultAlert.root, 3000);
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
            selector: SearchResultAlert.root,
            capture(actions) {
                actions.waitForElementToShow(SearchResultAlert.root, 3000);
            },
        },
    ],
};
