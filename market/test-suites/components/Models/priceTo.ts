'use strict';

import {Case} from 'spec/gemini/lib/types';
import Filters from 'spec/page-objects/Filters';

const input = Filters.textRangeTo(0);

export default {
    suiteName: 'Models-page. Price to validation test',
    selector: [Filters.root],
    before(actions, find) {
        actions.waitForElementToShow(Filters.root, 1000);
        actions.click(find(input));
        actions.wait(100);
        actions.sendKeys(input, 'test');
        actions.click(find(Filters.root));
        actions.wait(1000);
    },
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    capture() {},
} as Case;
