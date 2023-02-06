import {makeSuite, makeCase} from '@yandex-market/ginny';
import SinsHeader from '@self/root/src/widgets/parts/SinsHeader/components/View/__pageObject/index.desktop.js';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Шапка магазина', {
    story: {
        'По-умолчанию': {
            'отображается верно': makeCase({
                id: 'marketfront-4356',
                test() {
                    return this.browser.assertView('plain', SinsHeader.root);
                },
            }),
        },
    },
});
