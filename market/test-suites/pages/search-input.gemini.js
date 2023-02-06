import cookies from '@self/platform/constants/cookie';
import SearchForm from '@self/platform/spec/page-objects/widgets/SearchForm';
import {hideModalFloat, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Search input bar',
    selector: SearchForm.suggestInput,
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
        hideModalFloat(actions);
        hideRegionPopup(actions);
    },
    capture() {
    },
};
