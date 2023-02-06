'use strict';

import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Таб.', {
    story: {
        'Отрисовывается целевой контент': makeCase({
            test() {
                return (
                    this.tabs
                        .clickTabByTitle(this.params.tabsTitle)
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        .then(() => this.browser.waitForExist(this.params.tabsChild))
                        .then(() =>
                            this.tabs
                                .elem(this.params.tabsChild)
                                .isVisible()
                                .should.eventually.be.equal(true, 'Отображается'),
                        )
                );
            },
        }),
    },
});
