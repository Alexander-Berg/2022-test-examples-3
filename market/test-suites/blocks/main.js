import Main from '@self/platform/spec/page-objects/main';

import {
    hideRegionPopup,
    hideHeadBanner,
    hideDevTools,
    hideSnippetList,
    hideSearchResults,
    hideProductTabs,
    hideHeader2,
    hideTopmenu,
    hideFooterSubscriptionWrap,
    hideFooter,
    hideYndxBug,
} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'Main',
    selector: Main.root,
    before(actions) {
        hideSnippetList(actions);
        hideSearchResults(actions);
        hideHeadBanner(actions);
        hideProductTabs(actions);
        hideHeader2(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideYndxBug(actions);
    },
    capture() {},
};
