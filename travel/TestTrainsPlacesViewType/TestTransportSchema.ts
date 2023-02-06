import {random} from 'lodash';

import {ElementClass} from 'helpers/project/common/ElementClass/ElementClass';

import {Component} from 'components/Component';

export class TestTransportSchema extends Component {
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

        const randomPlace = places[random(0, places.length - 1)];

        /**
         * TRAVELFRONT-3097: хак для firefox. Браузер пытается проскроллить до элемента,
         * на который идет клик, FF не поддерживает этого внутри svg, поэтому кликаем программно через JS
         */
        if ((await this.browser.getBrowserName()) === 'firefox') {
            await randomPlace.clickJS();
        } else {
            await randomPlace.click();
        }

        return Number(await randomPlace.getText());
    }

    private get placeSelector(): string {
        return [this.prepareQaSelector(this.qa), '.Place'].join(' ');
    }

    private get placeAvailableSelector(): string {
        return [this.prepareQaSelector(this.qa), '.Place_available'].join(' ');
    }

    /**
     * getElements возвращает ElementClass[] с неправильными селекторами, что имеет значение,
     * если дальше с этими элементами использовать clickJS.
     * Неправильный selector, т.к. nth-child выставляется только с учетом элементов,
     * которые мы ищем, игнорируя индекс среди всех элементов.
     *
     * Пример:
     * <div class="root">
     *   <div>1</div>
     *   <div>2</div>
     *   <div class="active">3</div>
     * </div>
     *
     * getElements('.root .active') вернет элемент с селектором '.root .active:nth-child(1)',
     * хотя индекс среди всех элементов - 3.
     *
     * Правим selector ручками.
     */
    private async getAvailablePlaces(): Promise<ElementClass[]> {
        const allPlaces = await this.getElements(this.placeSelector);
        const availablePlaces = await this.getElements(
            this.placeAvailableSelector,
        );

        return availablePlaces.map(availablePlace => {
            const indexInAllPlaces = allPlaces.findIndex(
                p => p.elementId === availablePlace.elementId,
            );

            availablePlace.selector = availablePlace.selector?.replace(
                /nth-child\([^)]*\)$/,
                `nth-child(${indexInAllPlaces + 1})`,
            );

            return availablePlace;
        });
    }
}
