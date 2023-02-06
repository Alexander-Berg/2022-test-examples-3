import HeaderCatalog from '@self/platform/widgets/content/HeaderCatalog/__pageObject';
import AllFilters from '@self/platform/widgets/content/AllFilters/__pageObject';
import AllFiltersFooter from '@self/platform/widgets/content/AllFilters/Footer/__pageObject';
import Tooltip from '@self/platform/components/ProductFullSpecs/Tooltip/__pageObject';
import Header2Menu from '@self/platform/spec/page-objects/header2-menu';


import {
    hideHeadBanner,
    hideFooterSubscriptionWrap,
    hideFooter,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {waitForElementVisible} from '@self/platform/spec/gemini/helpers/visible';


export default {
    suiteName: 'ClickOnMenuClosesFilterPopup',
    selector: 'body',
    before(actions) {
        hideHeadBanner(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
        hideAllElementsBySelector(actions, AllFiltersFooter.productLink);
        waitForElementVisible(actions, Header2Menu.catalogEntrypoint, 10000);
    },
    capture: {
        opened(actions, find) {
            actions
                // активируем попап с подсказкой
                // кликаем на подсказку из первого (верхнего) фильтра во второй колонке
                // так как в этой колонке фильтры находятся сверху, как раз под открытым меню
                .click(find(`${AllFilters.filterColumn}:nth-child(2) ${Tooltip.root}`))
                .wait(1000);
        },
        afterClickOnMenu(actions, find) {
            actions
                // наводим на пункт горизонтального меню
                .click(find(`${Header2Menu.catalogEntrypoint}`))
                // ждём пока меню загрузится
                .waitForElementToShow(HeaderCatalog.root, 10000);
        },
    },
    ignore: [
        {every: '[data-zone-name="Showcase"]'},
    ],
};
