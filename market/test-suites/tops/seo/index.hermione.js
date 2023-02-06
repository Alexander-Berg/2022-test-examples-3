import {makeSuite, mergeSuites} from 'ginny';

import Linker from './linker';
import Chpu from './chpu';
import Canonical from './canonical';
import PageProductAlco from './page-product-alco';
import PageProductMedicine from './page-product-medicine';
import PageTitle from './page-title';
import Breadcrumbs from './breadcrumbs';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('SEO', {
    story: mergeSuites(
        makeSuite('Блок «Часто ищут»', {
            feature: 'Блок «Часто ищут»',
            story: Linker,
        }),
        makeSuite('Canonical-url', {
            environment: 'kadavr',
            feature: 'Canonical-url',
            story: Canonical,
        }),
        makeSuite('Title страницы', {
            environment: 'kadavr',
            feature: 'Title страницы',
            story: PageTitle,
        }),
        makeSuite('Хлебные крошки', {
            feature: 'Хлебные крошки',
            story: Breadcrumbs,
        }),
        Chpu,
        PageProductAlco,
        PageProductMedicine
    ),
});
