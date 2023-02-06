import {makeSuite, makeCase} from '@yandex-market/ginny';

import HeaderSearch from '@self/platform/widgets/content/HeaderSearch/__pageObject';
import ResetableButton from '@self/project/src/components/ResetableButton/__pageObject/index.desktop.js';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Чипсина магазина', {
    story: {
        'По умолчанию': {
            'видна': makeCase({
                id: 'marketfront-4354',
                test() {
                    return this.browser.assertView('plain', HeaderSearch.root);
                },
            }),
        },
        'При клике на крестик': {
            'закрывается': makeCase({
                id: 'marketfront-4355',
                async test() {
                    await this.browser.click(ResetableButton.remover);
                    await this.browser.waitForVisible(ResetableButton.remover, 2000, true);
                    return this.browser.assertView('plain', HeaderSearch.root);
                },
            }),
        },
    },
});
