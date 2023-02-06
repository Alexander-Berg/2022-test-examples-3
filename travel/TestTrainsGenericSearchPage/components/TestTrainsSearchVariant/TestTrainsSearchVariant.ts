import _get from 'lodash/get';
import {some} from 'p-iteration';

import {TestTrainsOrderSegmentTimeAndDuration} from 'helpers/project/trains/components/TestTrainsOrderSegmentTimeAndDuration';
import {
    TestTrainsSearchSegment,
    TTestVariantSegmentOptions,
} from 'helpers/project/trains/pages/TestTrainsGenericSearchPage/components/TestTrainsSearchSegment/TestTrainsSearchSegment';

import {Button} from 'components/Button';
import {TestPrice} from 'components/TestPrice';
import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export type TVariantAndSegmentOptions = TTestVariantSegmentOptions &
    Partial<{
        ownBooking: boolean;
        needVariantWithTransfer: boolean;
    }>;

export class TestTrainsSearchVariant extends Component {
    segments: ComponentArray<TestTrainsSearchSegment>;
    pricesSpinner: Component;
    departureStation: Component | null;
    stations: Component | null;
    arrivalStation: Component | null;
    timeAndDuration: TestTrainsOrderSegmentTimeAndDuration;
    transfers: Component;
    duration: Component;
    boyActionButton: Button;
    ufsActionButton: Button;
    variantMinPrice: TestPrice;
    toggleSegmentsVisibilityLink: Component;
    segmentsModalBackButton: Component | null = null;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.pricesSpinner = new Component(browser, 'pricesSpinner');
        this.departureStation = this.isTouch
            ? null
            : new Component(browser, {
                  parent: this.qa,
                  current: 'departure-station',
              });
        this.arrivalStation = this.isTouch
            ? null
            : new Component(browser, {
                  parent: this.qa,
                  current: 'arrival-station',
              });
        this.stations = this.isTouch
            ? new Component(browser, {
                  parent: this.qa,
                  current: 'stations',
              })
            : null;
        this.transfers = new Component(browser, {
            parent: this.qa,
            current: 'transfers',
        });
        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });
        this.timeAndDuration = new TestTrainsOrderSegmentTimeAndDuration(
            browser,
            this.qa,
        );
        this.toggleSegmentsVisibilityLink = new Component(browser, {
            parent: this.qa,
            current: 'toggleSegmentsVisibilityLink',
        });
        this.segments = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'segment',
            },
            TestTrainsSearchSegment,
        );
        this.boyActionButton = new Button(browser, {
            parent: this.qa,
            current: 'boyActionButton',
        });
        this.ufsActionButton = new Button(browser, {
            parent: this.qa,
            current: 'ufsActionButton',
        });
        this.variantMinPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'variantMinPrice',
        });
        this.segmentsModalBackButton = this.isTouch
            ? new Component(this.browser, {
                  parent: this.qa,
                  current: 'backButton',
              })
            : null;
    }

    checkIsVariantWithTransfer = async (): Promise<boolean> => {
        return this.transfers.isDisplayed(0);
    };

    checkIsBoyVariant = async (): Promise<boolean> => {
        const boyActionButton = await this.getBoyActionButton();

        if (!boyActionButton) {
            return false;
        }

        return boyActionButton.isDisplayed(0);
    };

    clickToBoyActionButton = async (): Promise<void> => {
        const boyActionButton = await this.getBoyActionButton();
        const isBoyVariant = await this.checkIsBoyVariant();

        if (!boyActionButton || !isBoyVariant) {
            throw new Error('Должна быть кнопка: "Выбрать место"');
        }

        await boyActionButton.scrollIntoView();
        await boyActionButton.click();
    };

    getBoyActionButton = async (): Promise<Button | null> => {
        const variantOrFirstSegment = await this.getVariantOrFirstSegment();

        if (!variantOrFirstSegment) {
            return null;
        }

        return variantOrFirstSegment.boyActionButton;
    };

    getUfsActionButton = async (): Promise<Button | null> => {
        const variantOrFirstSegment = await this.getVariantOrFirstSegment();

        if (!variantOrFirstSegment) {
            return null;
        }

        return variantOrFirstSegment.ufsActionButton;
    };

    checkHasVariantPricesSpinner = async (): Promise<boolean> => {
        const variantOrFirstSegment = await this.getVariantOrFirstSegment();

        if (!variantOrFirstSegment) {
            return true;
        }

        return variantOrFirstSegment.pricesSpinner.isDisplayed(0);
    };

    checkVariantFieldDisplay = async (fieldName: string): Promise<void> => {
        const isDisplayed = await _get(this, fieldName).isDisplayed(0);

        if (!isDisplayed) {
            throw new Error(
                `В варианте должно быть отображено поле: ${fieldName}`,
            );
        }
    };

    checkActionButton = async (): Promise<void> => {
        const hasMinPrice = await this.getVariantMinPrice();

        if (!hasMinPrice) {
            return;
        }

        const actionButtons = [
            await this.getBoyActionButton(),
            await this.getUfsActionButton(),
        ];

        const hasDisplayedButtons = await some(actionButtons, actionButton =>
            actionButton ? actionButton.isDisplayed(0) : false,
        );

        if (!hasDisplayedButtons) {
            throw new Error(
                'Должна быть отображена кнопка перехода на выбор мест',
            );
        }
    };

    checkVariantDirectionMainFields = async (): Promise<void> => {
        const variantNeedCheckFields = [
            'duration',
            'transfers',
            'timeAndDuration.arrival',
            'timeAndDuration.departure',
        ];

        if (this.isTouch) {
            variantNeedCheckFields.push('stations');
        } else {
            variantNeedCheckFields.push('departureStation', 'arrivalStation');
        }

        for (const fieldName of variantNeedCheckFields) {
            try {
                await this.checkVariantFieldDisplay(fieldName);
            } catch {
                throw new Error(
                    `В варианте должно быть отображено поле: ${fieldName}`,
                );
            }
        }
    };

    checkSegmentsDirectionMainFields = async (): Promise<void> => {
        await this.segments.forEach(async segment => {
            await Promise.all([
                await segment.checkSegmentDirectionMainFields(),
                await segment.checkSegmentTariffs(),
            ]);
        });
    };

    checkVariantSegments = async (): Promise<void> => {
        const isVariantWithTransfer = await this.checkIsVariantWithTransfer();

        if (isVariantWithTransfer) {
            const isVisibleToggleSegmentsVisibilityLink =
                await this.toggleSegmentsVisibilityLink.isVisible();

            if (!isVisibleToggleSegmentsVisibilityLink) {
                throw new Error(
                    'Вариант должен содержать кнопку для просмотра всего маршрута',
                );
            }

            await this.toggleSegmentsVisibilityLink.click();
        }

        await this.checkSegmentsDirectionMainFields();

        if (isVariantWithTransfer) {
            if (this.segmentsModalBackButton) {
                await this.segmentsModalBackButton.click();
            }
        }
    };

    getVariantOrFirstSegment = async (): Promise<
        TestTrainsSearchVariant | TestTrainsSearchSegment | null
    > => {
        const isVariantWithTransfer = await this.checkIsVariantWithTransfer();

        if (isVariantWithTransfer) {
            return this;
        }

        const firstSegment = await this.segments.first();

        if (!firstSegment) {
            return null;
        }

        return firstSegment;
    };

    getVariantMinPrice = async (): Promise<number | null> => {
        try {
            const variantOrFirstSegment = await this.getVariantOrFirstSegment();

            if (!variantOrFirstSegment) {
                return null;
            }

            const hasVariantMinPrice =
                await variantOrFirstSegment.variantMinPrice.isDisplayed(0);

            if (!hasVariantMinPrice) {
                return null;
            }

            return await variantOrFirstSegment.variantMinPrice.getPriceValue();
        } catch (e) {
            return null;
        }
    };

    getVariantDepartureDate = async (): Promise<string> => {
        try {
            return await this.timeAndDuration.departure.date.getText();
        } catch (err) {
            throw new Error(
                'Дата отправления должна присутствовать у варианта',
            );
        }
    };

    getVariantArrivalDate = async (): Promise<string> => {
        try {
            return await this.timeAndDuration.arrival.date.getText();
        } catch (err) {
            throw new Error('Дата прибытия должна присутствовать у варианта');
        }
    };

    getVariantSegmentByOptions = async (
        options: TVariantAndSegmentOptions = {},
    ): Promise<TestTrainsSearchSegment | null> => {
        const {ownBooking = true, needVariantWithTransfer = false} = options;
        const segments = await this.segments.items;
        const isVariantWithTransfer = await this.checkIsVariantWithTransfer();

        if (needVariantWithTransfer !== isVariantWithTransfer) {
            return null;
        }

        if (ownBooking) {
            const isBoyVariant = await this.checkIsBoyVariant();

            if (!isBoyVariant) {
                return null;
            }
        }

        if (isVariantWithTransfer) {
            await this.toggleSegmentsVisibilityLink.click();
        }

        for (const segment of segments) {
            const isSegmentMatched = await segment.isSegmentMatchesToOptions(
                options,
            );

            if (isSegmentMatched) {
                return segment;
            }
        }

        return null;
    };
}
