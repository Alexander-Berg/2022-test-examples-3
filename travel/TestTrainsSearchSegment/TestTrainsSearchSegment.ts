import _get from 'lodash/get';

import {TestTrainsOrderSegmentTimeAndDuration} from 'helpers/project/trains/components/TestTrainsOrderSegmentTimeAndDuration';

import {Button} from 'components/Button';
import {TestPrice} from 'components/TestPrice';
import {Component} from 'components/Component';

import {ETrainsSegmentTariffTitle} from '../TestTrainsSearchSegmentTariff/TestTrainsSearchSegmentTariff';
import {TestTrainsSearchSegmentTariffs} from '../TestTrainsSearchSegmentTariffs/TestTrainsSearchSegmentTariffs';

export interface ITestVariantSegmentInfo {
    number: string | null;
    hasElectronicRegistration: boolean | null;
    company: string | null;
    firm: string | null;
    departureTime: string | null;
    arrivalTime: string | null;
    departureDate?: string | null;
    arrivalDate?: string | null;
}

export type TTestVariantSegmentOptions = Partial<{
    isFirm: boolean;
    firm: string;
    trainName: string;
    excludeFirms: string[];
    company: string;
    ownBooking: boolean;
    minTariffsCount: number;
    tariffTitle: ETrainsSegmentTariffTitle;
}>;

export const SAPSAN_TRAIN_NAME = '«Сапсан»';

export class TestTrainsSearchSegment extends Component {
    number: Component;
    company: Component;
    firm: Component;
    trainName: Component;
    pricesSpinner: Component;
    electronicRegistration: Component;
    timeAndDuration: TestTrainsOrderSegmentTimeAndDuration;
    stations: Component | null;
    departureStation: Component | null;
    arrivalStation: Component | null;
    duration: Component;
    boyActionButton: Button;
    ufsActionButton: Button;
    tariffs: TestTrainsSearchSegmentTariffs;
    variantMinPrice: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.number = new Component(browser, {
            parent: this.qa,
            current: 'number',
        });
        this.company = new Component(browser, {
            parent: this.qa,
            current: 'company',
        });
        this.firm = new Component(browser, {
            parent: this.qa,
            current: 'firm',
        });
        this.trainName = new Component(browser, {
            parent: this.qa,
            current: 'trainName',
        });
        this.company = new Component(browser, {
            parent: this.qa,
            current: 'company',
        });
        this.electronicRegistration = new Component(browser, {
            parent: this.qa,
            current: 'electronicRegistration',
        });
        this.timeAndDuration = new TestTrainsOrderSegmentTimeAndDuration(
            browser,
            this.qa,
        );
        this.stations = this.isTouch
            ? new Component(browser, {
                  parent: this.qa,
                  current: 'stations',
              })
            : null;
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
        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });
        this.boyActionButton = new Button(browser, {
            parent: this.qa,
            current: 'boyActionButton',
        });
        this.ufsActionButton = new Button(browser, {
            parent: this.qa,
            current: 'ufsActionButton',
        });
        this.tariffs = new TestTrainsSearchSegmentTariffs(browser, {
            parent: this.qa,
            current: 'tariffs',
        });
        this.pricesSpinner = new Component(browser, 'pricesSpinner');
        this.variantMinPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'variantMinPrice',
        });
    }

    checkSegmentFieldDisplay = async (fieldName: string): Promise<void> => {
        const isDisplayed = await _get(this, fieldName).isDisplayed(0);

        if (!isDisplayed) {
            throw new Error(
                `В сегменте должно быть отображено поле: ${fieldName}`,
            );
        }
    };

    checkSegmentDirectionMainFields = async (): Promise<void> => {
        const segmentNeedCheckFields = [
            'number',
            'company',
            'duration',
            'timeAndDuration.arrival',
            'timeAndDuration.departure',
        ];

        if (this.isTouch) {
            segmentNeedCheckFields.push('stations');
        } else {
            segmentNeedCheckFields.push('departureStation', 'arrivalStation');
        }

        for (const fieldName of segmentNeedCheckFields) {
            try {
                await this.checkSegmentFieldDisplay(fieldName);
            } catch {
                throw new Error(
                    `В сегменте должно быть отображено поле: ${fieldName}`,
                );
            }
        }
    };

    checkSegmentTariffs = async (): Promise<void> => {
        await this.tariffs.checkTariffsMainFields();
    };

    getFirm = async (): Promise<string | null> => {
        try {
            return await this.firm.getText(0);
        } catch (err) {
            return null;
        }
    };

    getTrainName = async (): Promise<string | null> => {
        try {
            return await this.trainName.getText(0);
        } catch (err) {
            return null;
        }
    };

    getCompany = async (): Promise<string | null> => {
        try {
            return await this.company.getText(0);
        } catch (err) {
            return null;
        }
    };

    getArrivalDate = async (): Promise<string | null> => {
        try {
            return await this.timeAndDuration.arrival.date.getText(0);
        } catch (err) {
            return null;
        }
    };

    getArrivalTime = async (): Promise<string | null> => {
        try {
            return await this.timeAndDuration.arrival.time.getText(0);
        } catch (err) {
            return null;
        }
    };

    getDepartureDate = async (): Promise<string | null> => {
        try {
            return await this.timeAndDuration.departure.date.getText(0);
        } catch (err) {
            return '';
        }
    };

    getDepartureTime = async (): Promise<string | null> => {
        try {
            return await this.timeAndDuration.departure.time.getText(0);
        } catch (err) {
            return '';
        }
    };

    getTrainNumber = async (): Promise<string | null> => {
        try {
            return await this.number.getText(0);
        } catch (err) {
            return null;
        }
    };

    checkHasElectronicRegistration = async (): Promise<boolean> => {
        return this.electronicRegistration.isDisplayed(0);
    };

    getInfo = async (): Promise<ITestVariantSegmentInfo> => {
        const firm = await this.getFirm();
        const company = await this.getCompany();
        const number = await this.getTrainNumber();
        const hasElectronicRegistration =
            await this.checkHasElectronicRegistration();
        const arrivalDate = await this.getArrivalDate();
        const arrivalTime = await this.getArrivalTime();
        const departureDate = await this.getDepartureDate();
        const departureTime = await this.getDepartureTime();

        return {
            firm,
            number,
            company,
            arrivalDate,
            arrivalTime,
            departureDate,
            departureTime,
            hasElectronicRegistration,
        };
    };

    isSegmentMatchesToOptions = async (
        options: TTestVariantSegmentOptions,
    ): Promise<boolean> => {
        const {
            firm = '',
            trainName = '',
            company = '',
            isFirm = false,
            tariffTitle = '',
            excludeFirms = [],
            minTariffsCount = 0,
        } = options;

        const segmentFirm = await this.getFirm();
        const segmentCompany = await this.getCompany();

        if (isFirm) {
            if (!segmentFirm) {
                return false;
            }
        }

        if (firm) {
            if (segmentFirm !== firm) {
                return false;
            }
        }

        if (trainName) {
            const segmentTrainName = await this.getTrainName();

            if (trainName !== segmentTrainName) {
                return false;
            }
        }

        if (excludeFirms.length && segmentFirm) {
            const isSegmentFirmContained = excludeFirms.includes(segmentFirm);

            if (isSegmentFirmContained) {
                return false;
            }
        }

        if (company) {
            if (segmentCompany !== company) {
                return false;
            }
        }

        if (tariffTitle) {
            const tariffByTitle = await this.tariffs.findTariffByTitle(
                tariffTitle,
            );

            return Boolean(tariffByTitle);
        }

        if (minTariffsCount) {
            const tariffsCount = await this.tariffs.tariffs.count();

            return tariffsCount >= minTariffsCount;
        }

        return true;
    };
}
