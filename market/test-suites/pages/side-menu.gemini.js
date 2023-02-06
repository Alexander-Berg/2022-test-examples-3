import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Side menu button',
    selector: SideMenu.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    capture: {
    },
};
