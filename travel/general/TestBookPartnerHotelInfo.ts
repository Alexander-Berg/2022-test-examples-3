import {TestModal} from 'components/TestModal';
import {Component} from 'components/Component';

/**
 * Модал на странице ошибки бронирования / странице отмены бронирования
 * с информацией об отеле от партнера
 * @see BookPartnerHotelInfo
 **/
export class TestBookPartnerHotelInfo extends Component {
    modal: TestModal;
    hotelName: Component;
    imagesCarousel: Component;
    mapContainer: Component;
    hotelDescriptions: Component;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.modal = new TestModal(browser);
        this.hotelName = new Component(browser, {
            path: [this.modal.qa],
            current: 'hotelName',
        });
        this.imagesCarousel = new Component(browser, {
            path: [this.modal.qa],
            current: 'imagesCarousel',
        });
        this.mapContainer = new Component(browser, {
            path: [this.modal.qa],
            current: 'mapContainer',
        });
        this.hotelDescriptions = new Component(browser, {
            path: [this.modal.qa],
            current: 'hotelDescriptions',
        });
    }
}
