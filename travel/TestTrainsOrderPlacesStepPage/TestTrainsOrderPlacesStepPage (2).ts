import {regions} from 'suites/trains';
import moment, {Moment} from 'moment';
import random from 'lodash/random';
import {stringify} from 'querystring';

import {ARCTIC_TRAIN} from 'helpers/constants/imMocks';

import {ECoachType} from 'helpers/project/trains/types/coachType';
import ISelectedPlaces from 'helpers/project/trains/pages/TestTrainsOrderPlacesStepPage/types/ISelectedPlaces';

import TrainsOrderPageLayout from 'helpers/project/trains/components/TrainsOrderPageLayout';
import TestOrderSummaryCompact from 'helpers/project/trains/components/TestOrderSummaryCompact/TestOrderSummaryCompact';
import {TestTrainOrderSegments} from 'helpers/project/trains/components/TestTrainOrderSegments/TestTrainOrderSegments';
import dateFormats from 'helpers/utilities/date/formats';
import {TestCoachTypeGroupItem} from 'helpers/project/trains/components/TestCoachTypeGroupItem';
import {getRegionInfo} from 'helpers/project/trains/utils/getRegionInfo';

import {TestTrainsServiceClassSelector} from '../../components/TestTrainsServiceClassSelector';
import {Component} from 'components/Component';
import {TestCoachTypeGroups} from '../../components/TestCoachTypeGroups';
import {TestTrainsPlacesViewType} from '../../components/TestTrainsPlacesViewType';
import {TestCoachTypeSelector} from '../../components/TestCoachTypeSelector';
import {TestPassengersCountSection} from '../../components/TestPassengersCountSection';
import {TestPrice} from 'components/TestPrice';

import {retry} from '../../../common/retry';

export class TestTrainsOrderPlacesStepPage extends TrainsOrderPageLayout {
    readonly coachesGroupsTitle: Component;
    readonly orderSegments: TestTrainOrderSegments;
    readonly orderError: Component;
    readonly passengersCountSection: TestPassengersCountSection;
    readonly serviceClassSelector: TestTrainsServiceClassSelector;
    readonly coachTypeGroups: TestCoachTypeGroups | null;
    readonly placesViewType: TestTrainsPlacesViewType | null;
    readonly coachTypeSelector: TestCoachTypeSelector | null;
    readonly orderSummaryCompact: TestOrderSummaryCompact;
    readonly layoutError500: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'placesStep') {
        super(browser, qa);

        this.orderSegments = new TestTrainOrderSegments(browser);
        this.orderError = new Component(browser, 'orderError');

        this.passengersCountSection = new TestPassengersCountSection(
            browser,
            'passengersCountSection',
        );

        this.serviceClassSelector = new TestTrainsServiceClassSelector(
            browser,
            'serviceClassSelector',
        );

        this.coachesGroupsTitle = new Component(browser, {
            parent: this.qa,
            current: 'coachesGroupsTitle',
        });

        this.coachTypeGroups = this.isTouch
            ? null
            : new TestCoachTypeGroups(browser);

        this.placesViewType = this.isTouch
            ? new TestTrainsPlacesViewType(browser, 'placesViewType')
            : null;
        this.coachTypeSelector = this.isTouch
            ? new TestCoachTypeSelector(browser, 'coachTypeSelector')
            : null;

        this.orderSummaryCompact = new TestOrderSummaryCompact(
            browser,
            'orderSummaryCompact',
        );

        this.layoutError500 = new Component(browser, 'layoutError500');
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

    /**
     * Выбирает случайное свободное место в первом типе вагона и классе
     */
    async selectAvailablePlace(): Promise<void> {
        if (this.isTouch) {
            await this.selectAvailablePlaceTouch();
        } else {
            await this.selectAvailablePlaceDesktop();
        }
    }

    async selectAnyPlacesInPlatzkarte(count = 1): Promise<ISelectedPlaces> {
        if (this.isTouch) {
            return this.selectAnyPlacesInPlatzkarteInTouch(count);
        }

        return this.selectAnyPlacesInPlatzkarteInDesktop(count);
    }

    async selectPlatzkartePlacesWithRequirements(): Promise<void> {
        const firstClass =
            await this.selectPlatzkarteGroupWithFirstClassInDesktop();

        await firstClass.placesViewType.placesViewTypeTabs.requirementsTab.click();
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

        return this.orderSummary.totalPrice.price;
    }

    async goNextStep(): Promise<void> {
        // без небольшой паузы не всегда переходит на шаг ввода данных
        await this.browser.pause(500);

        if (this.isTouch && (await this.orderSummaryCompact.isVisible())) {
            return this.orderSummaryCompact.orderButton.click();
        }

        return this.orderSummary.orderButton.click();
    }

    async beddingIsChecked(): Promise<boolean> {
        if (await this.orderSummary.isVisible()) {
            const firstTrain = await this.orderSummary.trains.first();

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
        await this.browser.setCookie({
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

    private async selectAvailablePlaceDesktop(): Promise<void> {
        const {coachTypeGroups} = this;

        if (!coachTypeGroups) {
            throw new Error(
                'selectAnyPlaceDesktop можно вызывать только для Desktop',
            );
        }

        const typeGroup = (await coachTypeGroups.groups.items)[0];

        const classType = (await typeGroup.classes.items)[0];

        await classType.scrollIntoView();
        await classType.click();

        await classType.placesViewType.placesViewTypeTabs.schemasTab.click();

        const coaches = await classType.placesViewType.coachTab.coaches.items;

        await coaches[0].transportSchema.clickRandomAvailablePlace();
    }

    private async selectAvailablePlaceTouch(): Promise<void> {
        const {coachTypeSelector, placesViewType} = this;

        if (!coachTypeSelector || !placesViewType) {
            throw new Error('Work only on touch devices');
        }

        const coaches = await placesViewType.coachTab.coaches.items;

        await coaches[0].transportSchema.clickRandomAvailablePlace();
    }

    private async selectAnyPlacesInPlatzkarteInTouch(
        count = 1,
    ): Promise<ISelectedPlaces> {
        const {
            coachTypeSelector,
            serviceClassSelector,
            placesViewType,
            coachesGroupsTitle,
        } = this;

        if (!coachTypeSelector || !placesViewType) {
            throw new Error('Work only on touch devices');
        }

        if (await coachTypeSelector.platzkarteButton.isVisible()) {
            await coachTypeSelector.platzkarteButton.click();
        } else {
            const title = await coachesGroupsTitle.getText();

            if (!title.includes('плац')) {
                /**
                 * Если нет кнопки для плацкартных вагонов
                 * и в заголовке нет слова про плацкарт
                 * значит в поезде нет плацкартных вагонов
                 */
                throw new Error('No platzkarte coach group');
            }
        }

        const coach = await placesViewType.coachTab.coaches.first();

        if (!coach) {
            throw new Error('No platzkarte places');
        }

        const serviceClassCode =
            await serviceClassSelector.coachTypeGroup.getServiceClassCode();
        const coachNumber = await coach.coachHeader.getCoachNumber();
        const places = await coach.transportSchema.selectPlaces(count);

        return {serviceClassCode, places, coachNumber};
    }

    private async selectPlatzkarteGroupWithFirstClassInDesktop(): Promise<TestCoachTypeGroupItem> {
        const {coachTypeGroups} = this;

        if (!coachTypeGroups) {
            throw new Error('Work only on desktop');
        }

        const platzkarteGroup = await coachTypeGroups.getGroupByType(
            ECoachType.PLATZKARTE,
        );

        if (!platzkarteGroup) {
            throw new Error('Not have platzkarte group');
        }

        const firstClass = await platzkarteGroup.classes.first();

        await firstClass.scrollIntoView();

        if (!(await firstClass.isExpanded())) {
            await firstClass.classTitle.click();
        }

        return firstClass;
    }

    private async selectAnyPlacesInPlatzkarteInDesktop(
        count = 1,
    ): Promise<ISelectedPlaces> {
        const firstClass =
            await this.selectPlatzkarteGroupWithFirstClassInDesktop();

        if (!(await firstClass.isExpanded())) {
            await firstClass.placesViewType.placesViewTypeTabs.schemasTab.click();
        }

        const firstCoach =
            await firstClass.placesViewType.coachTab.coaches.first();

        const serviceClassCode = await firstClass.getServiceClassCode();
        const coachNumber = await firstCoach.coachHeader.getCoachNumber();
        const places = await firstCoach.transportSchema.selectPlaces(count);

        return {serviceClassCode, places, coachNumber};
    }
}
