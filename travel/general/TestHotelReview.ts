import {Component} from 'helpers/project/common/components';

export class TestHotelReview extends Component {
    authorName: Component;
    stars: Component;
    text: Component;
    date: Component;
    likeButton: Component;
    dislikeButton: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.authorName = new Component(browser, {
            parent: this,
            current: 'authorName',
        });
        this.stars = new Component(browser, {
            parent: this,
            current: 'stars',
        });
        this.text = new Component(browser, {
            parent: this,
            current: 'text',
        });
        this.date = new Component(browser, {
            parent: this,
            current: 'hotelReviewDate',
        });
        this.likeButton = new Component(browser, {
            parent: this,
            current: 'hotelReviewLikeButton',
        });
        this.dislikeButton = new Component(browser, {
            parent: this,
            current: 'hotelReviewDislikeButton',
        });
    }
}
