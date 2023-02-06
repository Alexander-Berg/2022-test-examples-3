import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import commonSuite from './common';

export default makeSuite('Как добраться до ПВЗ', {
    issue: 'MARKETFRONT-52439',
    environment: 'kadavr',
    feature: 'Как добраться до ПВЗ',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        prepareSuite(commonSuite, {
            suiteName: 'Заказ с доставкой в почтовое отделение.',
            params: {
                isPost: true,
            },
        }),

        prepareSuite(commonSuite, {
            suiteName: 'Заказ с доставкой в обычный пункт самовывоза.',
            params: {
                isPost: false,
            },
        })
    ),
});
