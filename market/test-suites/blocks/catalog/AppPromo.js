import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';

import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';


export default {
    suiteName: 'AppPromo',
    selector: '[data-zone-name="appPromo"] > div > div',
    capture() {},
    before(actions) {
        // почему-то этот элемент приводит к сдвигу селекторов, поэтому его скрываем
        hideElementBySelector(actions, SearchOptions.root);
    },
};
