import {setCookies} from '@yandex-market/gemini-extended-actions/';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import HeadBanner from '@self/platform/spec/page-objects/w-head-banner';
import SideMenu from '@self/platform/spec/gemini/test-suites/blocks/side-menu.gemini';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat, hideScrollbar} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import cookies from '@self/platform/constants/cookie';


export default {
    suiteName: 'SideMenu',
    url: '/blog',
    before(actions, find) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.LKOB_COOKIE,
                value: '1',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        actions.click(find(Header.menuTrigger));
        actions.wait(1000);
        hideScrollbar(actions);
    },
    childSuites: [
        {
            ...SideMenu,
            ignore: [
                HeadBanner.root,
            ],
        },
    ],
};
