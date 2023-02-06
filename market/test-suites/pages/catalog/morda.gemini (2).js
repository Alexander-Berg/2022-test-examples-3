import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Viewport from '@self/platform/spec/page-objects/Viewport';
import MenuCatalog from '@self/platform/spec/page-objects/components/MenuCatalog/';
import Subscription from '@self/platform/spec/page-objects/Subscription';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Footer from '@self/platform/spec/page-objects/Footer';

import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';

import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideRoll,
    hideModalFloat,
    hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import cookies from '@self/platform/constants/cookie';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'Morda',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.AGGRESSIVE_SMART_BANNER_HIDDEN,
                value: '1',
            },
            {
                name: cookies.SIMPLE_SMART_BANNER_HIDDEN,
                value: '1',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        hideHeadBanner(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: `${ScrollBox.root} ${Viewport.root} [data-zone-name="product"]`},
                // Сниппеты карусели Лего
                {every: `${ScrollBox.root} ${Viewport.root} [data-zone-name="specialSnippet"]`},
                {every: '[data-zone-name="navnode"]'},
                {every: '[data-zone-name="Banner"]'},
                {every: 'h3'},
            ],
            before(actions) {
                initLazyWidgets(actions, 10000);
                hideRoll(actions);
                const selector = [
                    '[data-zone-name="Subscription"]',
                    '[data-zone-name="Footer"]',
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        {
            suiteName: 'HeaderLogo',
            selector: `${Header.row}:nth-child(1)`,
            capture(actions) {
                actions.waitForElementToShow(`${Header.row}:nth-child(1)`, 5000);
            },
        },
        {
            suiteName: 'HeaderSearch',
            selector: `${Header.row}:nth-child(2)`,
            capture(actions) {
                actions.waitForElementToShow(`${Header.row}:nth-child(2)`, 5000);
            },
        },
        {
            suiteName: 'Subscribe',
            selector: `${Subscription.root} form`,
            before(actions) {
                initLazyWidgets(actions, 5000);
            },
            capture() {
            },
        },
        {
            suiteName: 'Footer',
            selector: '[data-zone-name="Footer"]',
            ignore: Footer.stat,
            capture() {
            },
        },
        {
            suiteName: 'Categories-menu',
            selector: MenuCatalog.root,
            before(actions, find) {
                actions.click(find(Header.menuTrigger));
                actions.waitForElementToShow(SideMenu.catalog);
                actions.click(find(SideMenu.catalog));
                actions.wait(1000);
            },
            capture() {},
        },
    ],
};
