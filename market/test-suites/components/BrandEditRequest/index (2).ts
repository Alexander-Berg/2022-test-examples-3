'use strict';

import {makeSuite, importSuite, mergeSuites} from 'ginny';

export default makeSuite('Заявка.', {
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления заявки', () => this.item.waitForExist());
            },
        },
        importSuite('BrandEditRequest/edit'),
    ),
});
