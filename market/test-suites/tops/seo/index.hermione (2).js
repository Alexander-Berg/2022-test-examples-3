import {makeSuite, mergeSuites} from '@yandex-market/ginny';

import Linker from './linker';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('SEO', {
    story: mergeSuites(
        makeSuite('Блок «Часто ищут»', {
            feature: 'Блок «Часто ищут»',
            story: Linker,
        })
    ),
});
