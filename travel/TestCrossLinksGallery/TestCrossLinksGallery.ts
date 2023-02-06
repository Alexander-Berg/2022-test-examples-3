import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';
import {ComponentArray} from 'components/ComponentArray';
import TestCrossLinkItem from 'components/TestCrossLinksGallery/components/TestCrossLinkItem';

export default class TestCrossLinksGallery extends Component {
    title: Component;
    moreLink: TestLink;
    itemSkeletons: ComponentArray;
    items: ComponentArray<TestCrossLinkItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });

        this.moreLink = new TestLink(this.browser, {
            parent: this.qa,
            current: 'moreLink',
        });

        this.itemSkeletons = new ComponentArray(
            this.browser,
            {parent: this.qa, current: 'itemSkeleton'},
            Component,
        );

        this.items = new ComponentArray(
            this.browser,
            {parent: this.qa, current: 'item'},
            TestCrossLinkItem,
        );
    }
}
