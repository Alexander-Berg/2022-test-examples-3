import IFlightInfo from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/types/IFlightInfo';

import TestAviaDesktopResultVariant from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaDesktopResultVariant/TestAviaDesktopResultVariant';
import TestAviaMobileResultVariant from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaMobileResultVariant/TestAviaMobileResultVariant';
import TestAviaBadges from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaMobileResultVariant/components/TestAviaBadges';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';
import {Button} from 'components/Button';

export class TestAviaResultVariant extends Component {
    desktopResultVariant: TestAviaDesktopResultVariant;
    mobileResultVariant: TestAviaMobileResultVariant;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.desktopResultVariant = new TestAviaDesktopResultVariant(
            browser,
            this.qa,
        );
        this.mobileResultVariant = new TestAviaMobileResultVariant(
            browser,
            this.qa,
        );
    }

    get orderLink(): Component {
        if (this.isTouch) {
            return this.mobileResultVariant.orderLink;
        }

        return this.desktopResultVariant.orderLink;
    }

    get price(): TestPrice {
        if (this.isTouch) {
            return this.mobileResultVariant.price;
        }

        return this.desktopResultVariant.price;
    }

    get buyButton(): Button {
        if (this.isTouch) {
            return this.mobileResultVariant.buyButton;
        }

        return this.desktopResultVariant.buyButton;
    }

    get textBuyButton(): Component {
        if (this.isTouch) {
            return this.mobileResultVariant.textBuyButton;
        }

        return this.desktopResultVariant.textBuyButton;
    }

    get badges(): TestAviaBadges {
        if (this.isTouch) {
            return this.mobileResultVariant.badges;
        }

        return this.desktopResultVariant.badges;
    }

    get baggageInfo(): Component | null {
        if (this.isTouch) {
            return this.mobileResultVariant.baggageInfo;
        }

        return null;
    }

    get carryOnIcon(): Component | null {
        if (this.isDesktop) {
            return this.desktopResultVariant.carryOnIcon;
        }

        return null;
    }

    get baggageIcon(): Component | null {
        if (this.isDesktop) {
            return this.desktopResultVariant.baggageIcon;
        }

        return null;
    }

    get airlineLogos(): Component[] {
        if (this.isTouch) {
            return [this.mobileResultVariant.logo];
        }

        return [
            this.desktopResultVariant.airline.logo,
            this.desktopResultVariant.forwardFlights.airline.logo,
            this.desktopResultVariant.backwardFlights.airline.logo,
        ];
    }

    get airlineTitles(): Component[] {
        if (this.isTouch) {
            return [this.mobileResultVariant.title];
        }

        return [
            this.desktopResultVariant.airline.title,
            this.desktopResultVariant.forwardFlights.airline.title,
            this.desktopResultVariant.backwardFlights.airline.title,
        ];
    }

    async getForwardFlightInfo(): Promise<IFlightInfo> {
        if (this.isTouch) {
            const forwardFlight =
                await this.mobileResultVariant.flights.first();

            return forwardFlight.getFlightInfo();
        }

        return this.desktopResultVariant.forwardFlights.getFlightInfo();
    }

    async getBackwardFlightInfo(): Promise<IFlightInfo> {
        if (this.isTouch) {
            const forwardFlight = await this.mobileResultVariant.flights.last();

            return forwardFlight.getFlightInfo();
        }

        return this.desktopResultVariant.backwardFlights.getFlightInfo();
    }

    async moveToOrder(): Promise<void> {
        await this.orderLink.scrollIntoView();
        await this.orderLink.click();
    }

    async checkAviacompanyInForwardFlight(
        aviacompanies: string[],
    ): Promise<boolean> {
        if (this.isTouch) {
            throw new Error('Touch is not implemented');
        }

        const airlineTitle =
            await this.desktopResultVariant.forwardFlights.airline.title.getText();

        return airlineTitle
            .split(' • ')
            .map(s => s.trim())
            .every(companyName => aviacompanies.includes(companyName));
    }

    async checkBadgeExist(badgeName: string): Promise<boolean> {
        return this.badges.badges.some(async badge => {
            return (await badge.getText()) === badgeName;
        });
    }
}
