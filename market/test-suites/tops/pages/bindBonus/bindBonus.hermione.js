import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import BindBonusPage from '@self/root/src/widgets/pages.desktop/BindBonusPage/view/__pageObject';
import bindBonusSuite from '@self/project/src/spec/hermione/test-suites/blocks/bindBonus';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница привязки купона', {
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    bindBonusPage: () => this.createPageObject(BindBonusPage),
                });
            },
        },
        prepareSuite(bindBonusSuite, {})
    ),
});
