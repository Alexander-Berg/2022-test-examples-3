import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import JournalScrollBox from '@self/platform/spec/page-objects/Journal/JournalKMScrollBox';
import JournalEntrypoint from '@self/platform/spec/page-objects/Journal/JournalEntrypoint';

import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


export default {
    suiteName: 'KM-Journal-scrollbox',
    selector: JournalScrollBox.root,
    ignore: [
        {every: Counter.root},
        {every: 'picture'},
    ],
    before(actions) {
        initLazyWidgets(actions, 2000);
        actions.waitForElementToShow(JournalEntrypoint.root, 2000);
    },
    capture() {},
};
