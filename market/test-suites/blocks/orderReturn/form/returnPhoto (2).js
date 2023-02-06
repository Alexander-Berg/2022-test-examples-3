import {
    makeSuite,
    prepareSuite,
} from 'ginny';

import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import photoUpload from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/photoUpload';
import {pickPointPostamat} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

import {fillReturnsForm} from '@self/root/src/spec/hermione/scenarios/returns';


export default makeSuite('Фото возврата.', {
    environment: 'kadavr',
    story: prepareSuite(photoUpload, {
        hooks: {
            beforeEach() {
                this.setPageObjects({
                    returnItemsScreen: () => this.createPageObject(
                        ReturnItems,
                        {parent: this.returnsPage}
                    ),
                    reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsPage}),
                });

                return this.browser.yaScenario(this, fillReturnsForm, {
                    itemsIndexes: [3],
                    outlet: pickPointPostamat,
                });
            },
        },
    }),
});
