import {ComponentArray} from 'components/ComponentArray';

import {TestTrainsBreadcrumbsItem} from './TestTrainsBreadcrumbsItem';

export class TestTrainsBreadcrumbs extends ComponentArray<TestTrainsBreadcrumbsItem> {
    constructor(
        browser: WebdriverIO.Browser,
        qa: QA = 'trainsBreadcrumbs-item',
    ) {
        super(browser, qa, TestTrainsBreadcrumbsItem);
    }
}
