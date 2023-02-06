import {Component, ComponentArray} from 'helpers/project/common/components';

import {TestHotelReview} from './TestHotelReview';

export class TestHotelReviews extends Component {
    title: Component;
    sortBar: Component;
    keyPhrases: Component;
    reviewsList: ComponentArray<TestHotelReview>;
    moreReviewsButton: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this,
            current: 'hotelPageReviewsTitle',
        });
        this.sortBar = new Component(browser, {
            parent: this,
            current: 'reviewsSortBar',
        });
        this.keyPhrases = new Component(browser, {
            parent: this,
            current: 'keyPhrases',
        });
        this.reviewsList = new ComponentArray(
            browser,
            {
                parent: this,
                current: 'hotelPageReview',
            },
            TestHotelReview,
        );
        this.moreReviewsButton = new Component(browser, {
            parent: this,
            current: 'moreReviewsButton',
        });
    }
}
