import {Component} from 'helpers/project/common/components';

export class TestSimilarHotel extends Component {
    photo: Component;
    hotelNameWithStars: Component;
    rating: Component;
    categoryName: Component;
    firstOfferPrice: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.photo = new Component(browser, {
            parent: this.qa,
            current: 'photo',
        });
        this.hotelNameWithStars = new Component(browser, {
            parent: this.qa,
            current: 'hotelNameWithStars',
        });
        this.rating = new Component(browser, {
            parent: this.qa,
            current: 'rating',
        });
        this.categoryName = new Component(browser, {
            parent: this.qa,
            current: 'categoryName',
        });
        this.firstOfferPrice = new Component(browser, {
            parent: this.qa,
            current: 'firstOfferPrice',
        });
    }
}
