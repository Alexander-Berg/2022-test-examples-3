import {mergeSuites, makeSuite, prepareSuite} from 'ginny';

import PromoSuite from '@self/platform/spec/hermione/test-suites/blocks/VertProductSnippet/promo';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import VertProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';

export default makeSuite('Скроллбокс на промо хабе.', {
    environment: 'testing',
    story: mergeSuites(
        prepareSuite(PromoSuite, {
            pageObjects: {
                snippet() {
                    return this.createPageObject(VertProductSnippet, {parent: ScrollBox.root});
                },
            },
        })
    ),
});
