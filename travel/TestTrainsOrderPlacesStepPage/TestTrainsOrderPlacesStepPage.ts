import {regions} from 'suites/trains';
import moment, {Moment} from 'moment';
import random from 'lodash/random';
import {stringify} from 'querystring';

import {ARCTIC_TRAIN} from 'helpers/constants/imMocks';

import {ECoachType} from 'helpers/project/trains/types/coachType';
import ISelectedPlaces from 'helpers/project/trains/pages/TestTrainsOrderPlacesStepPage/types/ISelectedPlaces';

import dateFormats from 'helpers/utilities/date/formats';
import {getRegionInfo} from 'helpers/project/trains/utils/getRegionInfo';
import TrainsOrderPageLayout from 'helpers/project/trains/components/TrainsOrderPageLayout';
import {TestTrainCoaches} from 'helpers/project/trains/components/TestTrainCoaches/TestTrainCoaches';
import {TestTrainOrderSegments} from 'helpers/project/trains/components/TestTrainOrderSegments/TestTrainOrderSegments';
import TestOrderSummaryCompact from 'helpers/project/trains/components/TestOrderSummaryCompact/TestOrderSummaryCompact';
import {TestCoachTypeTabsSelector} from 'helpers/project/trains/components/TestCoachTypeTabsSelector/TestCoachTypeTabsSelector';

import {Component} from 'components/Component';
import {TestPassengersCountSection} from '../../components/TestPassengersCountSection';
import {TestPrice} from 'components/TestPrice';

import {retry} from '../../../common/retry';

export class TestTrainsOrderPlacesStepPage extends Component {
    readonly coachesGroupsTitle: Component;
    readonly orderSegments: TestTrainOrderSegments;
    readonly orderError: Component;
    readonly passengersCountSection: TestPassengersCountSection;

    readonly coachTypeTabsSelector: TestCoachTypeTabsSelector;
    readonly trainCoaches: TestTrainCoaches;
    readonly orderSummaryCompact: TestOrderSummaryCompact;
    readonly layoutError500: Component;
    readonly layout: TrainsOrderPageLayout;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'placesStep') {
        super(browser, qa);

        this.layout = new TrainsOrderPageLayout(this.browser);
        this.orderSegments = new TestTrainOrderSegments(this.browser);
        this.orderError = new Component(this.browser, 'orderError');

        this.passengersCountSection = new TestPassengersCountSection(
            this.browser,
            'passengersCountSection',
        );

        this.coachesGroupsTitle = new Component(this.browser, {
            parent: this.qa,
            current: 'coachesGroupsTitle',
        });

        this.trainCoaches = new TestTrainCoaches(browser, 'coaches');

        this.coachTypeTabsSelector = new TestCoachTypeTabsSelector(
            browser,
            'coachTypeTabsSelector',
        );

        this.orderSummaryCompact = new TestOrderSummaryCompact(
            browser,
            'orderSummaryCompact',
        );

        this.layoutError500 = new Component(this.browser, 'layoutError500');
    }

    async waitTrainDetailsLoaded(): Promise<void> {
        try {
            const errorPage = retry(
                async () => {
                    await this.layoutError500.waitForVisible();
                },
                {attempts: 25, delay: 1000},
            )();

            const content = retry(
                async () => {
                    await this.passengersCountSection.waitForVisible();
                },
                {attempts: 25, delay: 1000},
            )();

            await Promise.race([errorPage, content]);
        } catch (err) {
            throw new Error('Информация о поезде не загрузилась');
        }
    }

    async selectAvailablePlace(): Promise<void> {
        const coach = await this.trainCoaches.coaches.first();

        if (!coach) {
            throw new Error('Не найден вагон в дефолтном табике');
        }

        await coach.transportSchema.clickRandomAvailablePlace();
    }

    async selectAnyPlacesInPlatzkarte(count = 1): Promise<ISelectedPlaces> {
        await this.coachTypeTabsSelector.setActiveCoachType(
            ECoachType.PLATZKARTE,
        );

        const coach = await this.trainCoaches.coaches.first();

        if (!coach) {
            throw new Error(
                `Не найден вагон с типом: ${ECoachType.PLATZKARTE}`,
            );
        }

        const coachNumber = await coach.coachHeader.getCoachNumber();
        const places = await coach.transportSchema.selectPlaces(count);

        return {coachNumber, places};
    }

    async selectPassengers({
        adults,
        children,
        babies,
    }: {
        adults?: number;
        children?: number;
        babies?: number;
    }): Promise<void> {
        if (adults !== undefined) {
            await this.passengersCountSection.selectPassengerCountByType(
                'adults',
                adults,
            );
        }

        if (children !== undefined) {
            await this.passengersCountSection.selectPassengerCountByType(
                'children',
                children,
            );
        }

        if (babies !== undefined) {
            await this.passengersCountSection.selectPassengerCountByType(
                'babies',
                babies,
            );
        }
    }

    async getTotalPrice(): Promise<TestPrice> {
        if (this.isTouch && (await this.orderSummaryCompact.isVisible())) {
            return this.orderSummaryCompact.price;
        }

        return this.layout.orderSummary.totalPrice.price;
    }

    async goNextStep(): Promise<void> {
        // без небольшой паузы не всегда переходит на шаг ввода данных
        await this.browser.pause(500);

        if (this.isTouch && (await this.orderSummaryCompact.isVisible())) {
            return this.orderSummaryCompact.orderButton.click();
        }

        return this.layout.orderSummary.orderButton.click();
    }

    async beddingIsChecked(): Promise<boolean> {
        if (await this.layout.orderSummary.isVisible()) {
            const firstTrain = await this.layout.orderSummary.trains.first();

            return firstTrain.bedding.isChecked();
        }

        if (await this.orderSummaryCompact.isVisible()) {
            await this.orderSummaryCompact.openOrderSummary();

            const firstTrain =
                await this.orderSummaryCompact.orderSummary.trains.first();

            const isChecked = await firstTrain.bedding.isChecked();

            await this.orderSummaryCompact.closeOrderSummary();

            return isChecked;
        }

        throw new Error('OrderSummary not found');
    }

    async setMockImPathCookie(mockImPath: string): Promise<void> {
        await this.browser.setCookies({
            name: 'mockImPath',
            value: mockImPath,
        });
    }

    async browseToPageWithoutTransfer(
        mockImPath: string = ARCTIC_TRAIN,
    ): Promise<{
        fromName: string;
        toName: string;
    }> {
        const {name: fromName, id: fromId} = getRegionInfo(regions.msk);
        const {name: toName, id: toId} = getRegionInfo(regions.spb);

        const departureMoment = moment().add(random(7, 21), 'days');
        const departureRobotDate = departureMoment.format(dateFormats.ROBOT);

        const query = {
            adults: 1,
            fromId,
            fromName,
            number: '016А',
            provider: 'P1',
            time: '00.41',
            toId,
            toName,
            when: departureRobotDate,
        };

        await this.setMockImPathCookie(mockImPath);

        await this.browser.url(`/trains/order/?${stringify(query)}`);

        return {
            fromName,
            toName,
        };
    }

    async browseToPageWithTransfer(mockImPath: string = ARCTIC_TRAIN): Promise<{
        fromName: string;
        toName: string;
        transferName: string;
        firstSegmentDepartureMoment: Moment;
    }> {
        const {name: fromName, id: fromId} = getRegionInfo(regions.spb);
        const {name: toName, id: toId} = getRegionInfo(regions.ekb);
        const {name: transferName, id: transferId} = getRegionInfo(regions.msk);

        const firstSegmentDepartureMoment = moment().add(random(7, 21), 'days');
        const firstSegmentDepartureRobotDate =
            firstSegmentDepartureMoment.format(dateFormats.ROBOT);
        const secondSegmentDepartureMoment = moment(
            firstSegmentDepartureMoment,
        ).add(1, 'day');
        const secondSegmentDepartureRobotDate =
            secondSegmentDepartureMoment.format(dateFormats.ROBOT);

        const query = {
            adults: 1,
            forward: [
                [
                    'P1',
                    '751А',
                    fromId,
                    transferId,
                    `${firstSegmentDepartureRobotDate}T05.30`,
                ].join('_'),
                [
                    'P1',
                    '002Э',
                    transferId,
                    toId,
                    `${secondSegmentDepartureRobotDate}T00.35`,
                ].join('_'),
            ].join(','),
            fromId,
            fromName,
            number: '751А',
            provider: 'P1',
            time: '05.30',
            toId,
            toName,
            when: firstSegmentDepartureRobotDate,
        };

        await this.setMockImPathCookie(mockImPath);

        await this.browser.url(`/trains/order/?${stringify(query)}`);

        return {
            fromName,
            toName,
            transferName,
            firstSegmentDepartureMoment,
        };
    }
}
