import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import HeadLine from '@self/platform/spec/page-objects/Journal/HeadLine';
import TagList from '@self/platform/spec/page-objects/Journal/TagList';
import FeaturedPages from '@self/platform/spec/page-objects/Journal/FeaturedPages';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Subscription from '@self/platform/spec/page-objects/Subscription';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalMainPage',
    url: '/journal',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    ignore: [
        {every: Counter.root},
    ],
    childSuites: [
        {
            suiteName: 'Header',
            selector: [
                HeadLine.root,
                TagList.root,
            ],
            capture() {
            },
        },
        {
            suiteName: 'FeaturedPages',
            selector: FeaturedPages.root,
            capture() {
            },
        },
        {
            suiteName: 'ScrollBoxSnippet',
            selector: ScrollBox.snippet,
            capture() {
            },
        },
        {
            suiteName: 'Subscription',
            selector: Subscription.root,
            capture() {
            },
        },
    ],
};
