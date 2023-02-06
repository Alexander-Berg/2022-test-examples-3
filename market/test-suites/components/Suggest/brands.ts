'use strict';

import InputB2b from 'spec/page-objects/InputB2b';
import Search from 'spec/page-objects/Search';
import {Case} from 'spec/gemini/lib/types';

export default {
    suiteName: 'Managers Suggest. Brands',
    selector: [Search.dropdownVendors],
    before(actions, find) {
        actions.waitForElementToShow(InputB2b.root, 10000);
        actions.click(find(InputB2b.root));
        actions.waitForElementToShow(Search.dropdownVendors, 10000);
        // ждём, когда когда отработает анимация появления
        actions.wait(1000);
    },
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    capture() {},
} as Case;
