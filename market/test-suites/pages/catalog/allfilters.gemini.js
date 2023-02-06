import FilterCompound from '@self/platform/components/FilterCompound/__pageObject';
import Filters from '@self/platform/components/Filters/__pageObject';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import FiltersBottomButton from '@self/platform/spec/page-objects/components/FiltersBottomButton';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideElementBySelector,
    hideScrollbar,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Allfilters',
    url: {
        pathname: '/catalog--fotoapparaty/56199/filters',
        query: {
            hid: 91148,
            filterList: 'all',
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        disableAnimations(actions);
        actions.wait(1000);
        hideScrollbar(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A11%3A52.634150.png
            suiteName: 'Filters',
            selector: Filters.root,
            before(actions) {
                hideElementBySelector(actions, FiltersBottomButton.root);
                hideElementBySelector(actions, '[data-autotest-id="glprice"]');
                hideElementBySelector(actions, '[data-zone-name="Banner"]');
            },
            capture() {},
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A14%3A00.681122.png
            suiteName: 'FiltersBottomButton',
            selector: `${FiltersBottomButton.root} button`,
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A28%3A38.461796.jpg
            suiteName: 'checkboxFilter',
            url: '/catalog/54726/filters',
            selector: ModalFloat.root,
            before(actions, find) {
                // Кнопку скрываем, т.к. фильтр может оказаться под кнопкой и автотест кликнет по ней.
                hideElementBySelector(actions, FiltersBottomButton.root);
                actions.click(find(
                    `${FilterCompound.root}[data-autotest-id="15164148"]` // Оперативная память
                ));
            },
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A28%3A56.985321.jpg
            suiteName: 'colorFilter',
            selector: ModalFloat.root,
            before(actions, find) {
                // Кнопку скрываем, т.к. фильтр может оказаться под кнопкой и автотест кликнет по ней.
                hideElementBySelector(actions, FiltersBottomButton.root);
                actions.click(find(
                    `${FilterCompound.root}[data-autotest-id="13887626"]` // Цвет
                ));
            },
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A29%3A36.093958.jpg
            suiteName: 'rangeFilter',
            selector: ModalFloat.root,
            before(actions, find) {
                // Кнопку скрываем, т.к. фильтр может оказаться под кнопкой и автотест кликнет по ней.
                hideElementBySelector(actions, FiltersBottomButton.root);
                actions.click(find(
                    `${FilterCompound.root}[data-autotest-id="4892294"]` // Число мегапикселей матрицы
                ));
            },
            capture() {},
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A29%3A17.628072.jpg
            suiteName: 'radioButtonFilter',
            selector: ModalFloat.root,
            before(actions, find) {
                // Кнопку скрываем, т.к. фильтр может оказаться под кнопкой и автотест кликнет по ней.
                hideElementBySelector(actions, FiltersBottomButton.root);
                actions.click(find(
                    `${FilterCompound.root}[data-autotest-id="offer-shipping"]` // Способ доставки
                ));
            },
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A14%3A35.512610.jpg
            suiteName: 'PriceFilter',
            selector: '[data-autotest-id="glprice"]',
            capture() {},
        },
    ],
};
