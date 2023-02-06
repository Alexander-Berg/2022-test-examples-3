import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import SearchResultSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/offers-snippet.gemini';
import SearchHeader from '@self/platform/widgets/content/SearchHeader/redesign/__pageObject';

import {
    hideModalFloat,
    hideMooa,
    hideParanja,
    hideRegionPopup,
    hideScrollbar,
} from '@self/platform/spec/gemini/helpers/hide';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideAllElementsBySelector} from '@self/project/src/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';


export default {
    suiteName: 'AlcoholList',
    url: '/catalog--vino/82914',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'adult',
                value: '1:1:ADULT',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        hideScrollbar(actions);
        disableAnimations(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A46%3A58.300982.jpg
            ...MainSuite,
            // автозагрузка на "Показать ещё" ломает скринтесты. Отключаем JS
            url: '/catalog--vino/82914?_mod=robot',
            ignore: [
                SearchHeader.totalOffersCount,
            ],
            before(actions) {
                hideAllElementsBySelector(actions, [
                    SearchResultSuite.selector,
                ].join(', '));
            },
        },
        SearchResultSuite,
    ],
};
