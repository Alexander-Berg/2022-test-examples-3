import {mergeSuites, makeSuite} from 'ginny';

import brand from './brand';
import shop from './shop';
import catalog from './catalog';
import product from './product';
import franchise from './franchise';
import common from './common';

export default makeSuite('ЧПУ', {
    feature: 'ЧПУ',
    story: mergeSuites(
        brand,
        catalog,
        product,
        shop,
        franchise,
        common
    ),
});
