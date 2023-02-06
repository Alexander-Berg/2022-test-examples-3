import {
    makeSuite,
    prepareSuite,
} from 'ginny';

import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import {ReturnItemReason} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItemReason/__pageObject';

import photoUpload from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/photoUpload';


export default makeSuite('Фото возврата.', {
    environment: 'kadavr',
    story: prepareSuite(photoUpload, {
        hooks: {
            async beforeEach() {
                this.setPageObjects({
                    returnsForm: () => this.createPageObject(ReturnsPage),
                    reasonTypeSelector: () => this.createPageObject(ReturnItemReason, {parent: this.returnsForm}),
                });
            },
        },
    }),
});
