import TestRoom from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestRooms/components/TestRoom';
import TestRoomsWithoutOffers from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestRooms/components/TestRoomsWithoutOffers';

import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {ComponentArray} from 'components/ComponentArray';

export default class TestRooms extends Component {
    allRoomsLink: Button;
    rooms: ComponentArray<TestRoom>;
    roomsWithoutOffers: TestRoomsWithoutOffers;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.allRoomsLink = new Button(browser, {
            parent: this.qa,
            current: 'allRoomsLink',
        });

        this.rooms = new ComponentArray(
            browser,
            {parent: this.qa, current: 'room'},
            TestRoom,
        );

        this.roomsWithoutOffers = new TestRoomsWithoutOffers(browser, {
            parent: this.qa,
            current: 'roomsWithoutOffers',
        });
    }
}
