import {mergeSuites, makeSuite} from 'ginny';

import shop from './shop';
import catalog from './catalog';
import brand from './brand';
import product from './product';
import common from './common';

export default makeSuite('ЧПУ', {
    feature: 'ЧПУ',
    story: mergeSuites(
        catalog,
        shop,
        brand,
        product,
        common
    ),
});
