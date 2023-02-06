import Mooa from '@self/platform/spec/page-objects/mooa';
import RegionSelector from '@self/platform/spec/page-objects/widgets/content/RegionSelector';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Region',
    url: '/my/region',
    selector: RegionSelector.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        new ClientAction(actions).removeElems(Mooa.root);
    },
    capture(actions) {
        actions.wait(1000);
        actions.waitForElementToShow(RegionSelector.root, 10000);
    },
};
