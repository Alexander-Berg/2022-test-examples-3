import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

import BottomDrawerWebview from '@self/root/src/components/BottomDrawerWebview/__pageObject';

import common from './common';


export default makeSuite('При открытом BottomDrawer', {
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    bottomDrawer: () => this.createPageObject(BottomDrawerWebview),
                });

                await this.bottomDrawer.swipeUp();
            },
        },
        prepareSuite(common)
    ),
});
