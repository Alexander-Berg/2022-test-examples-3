import {random} from 'lodash';

import {ElementArray} from 'webdriverio/build/types';

import {Component} from 'components/Component';

export default class TestTrainCoachTransportSchema extends Component {
    async selectPlaces(count: number = 1): Promise<number[]> {
        const selectedPlaces = [];

        for (let i = 0; i < count; i++) {
            const place = await this.clickRandomAvailablePlace();

            selectedPlaces.push(place);
        }

        return selectedPlaces;
    }

    async clickRandomAvailablePlace(): Promise<number> {
        await this.scrollIntoView();

        const places = await this.getAvailablePlaces();

        const randomPlace = await places[random(0, places.length - 1)];
        const randomPlaceText = await randomPlace.getText();

        /**
         * TRAVELFRONT-3097: хак для firefox. Браузер пытается проскроллить до элемента,
         * на который идет клик, FF не поддерживает этого внутри svg, поэтому кликаем программно через JS
         */
        if ((await this.browser.getBrowserName()) === 'firefox') {
            const id = await randomPlace.getAttribute('id');
            const escapedId = id.replace(/\//g, '\\/');

            await this.browser.clickJS(`${this.selector} #${escapedId}`);
        } else {
            await randomPlace.click();
        }

        return Number(randomPlaceText);
    }

    private get placeAvailableSelector(): string {
        return [this.selector, '.Place_available'].join(' ');
    }

    async getAvailablePlaces(): Promise<ElementArray> {
        return this.browser.$$(this.placeAvailableSelector);
    }
}
