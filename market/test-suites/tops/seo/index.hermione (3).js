import {makeSuite, mergeSuites} from 'ginny';

import chpu from './chpu';
import Gallery from './gallery';
import PageProductMedicine from './page-product-medicine';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('SEO', {
    story: mergeSuites(
        chpu,
        makeSuite('Блок «Галерея картинок»', {
            feature: 'Блок «Галерея картинок»',
            story: Gallery,
        }),
        PageProductMedicine
    ),
});
