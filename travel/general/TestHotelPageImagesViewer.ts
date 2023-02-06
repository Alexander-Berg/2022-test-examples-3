import {some} from 'p-iteration';

import {Component} from 'helpers/project/common/components';
import {ComponentArray} from 'helpers/project/common/components/ComponentArray';

export class TestHotelPageImagesViewer extends Component {
    readonly heading: Component;
    readonly closeButton: Component;
    readonly prevButton: Component;
    readonly nextButton: Component;
    readonly carousel: Component;
    readonly mainImage: Component;
    readonly carouselImages: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);
        this.heading = new Component(browser, {
            parent: this.qa,
            current: 'heading',
        });
        this.closeButton = new Component(browser, {
            parent: this.qa,
            current: 'closeButton',
        });
        this.prevButton = new Component(browser, {
            parent: this.qa,
            current: 'controlButtonPrev',
        });
        this.nextButton = new Component(browser, {
            parent: this.qa,
            current: 'controlButtonNext',
        });
        this.carousel = new Component(browser, {
            parent: this.qa,
            current: 'carousel',
        });
        this.mainImage = new Component(browser, {
            parent: this.qa,
            current: 'mainImage',
        });
        this.carouselImages = new ComponentArray(
            browser,
            {
                current: 'carouselImage',
            },
            Component,
        );
    }

    async isNavigationVisible(): Promise<boolean> {
        const buttons = [this.nextButton, this.prevButton];

        return some(buttons, button => button.isVisible());
    }

    async getSelectedImageSrc(): Promise<string | null> {
        const image = await this.carouselImages.find(async item => {
            const attr = await item.getAttribute('data-active');

            return attr === 'true';
        });

        if (!image) {
            throw new Error(
                'Нет активного изображения в TestHotelPageImagesViewer',
            );
        }

        return await image.getAttribute('src');
    }
}
