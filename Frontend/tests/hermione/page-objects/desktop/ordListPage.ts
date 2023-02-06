import assert from 'assert';
import Page from './page';
import { ordPath, ordPathById } from '../../../../.config/vars';
import { ReportStatus } from '../enums';

export default class OrdListPage extends Page {
    bro: WebdriverIO.Browser;

    get title() { return 'Title' }
    get indicator() { return 'table > thead > tr' }

    get reportStatus() { return 'div[data-test-id="ToolsPane"] > * >    div > .select__input-wrapper > div:nth-child(1)' }
    get reportStatusIndicator() { return '.popover-base__popper li:nth-child(1) button' }
    get period() { return 'div[data-test-id="ToolsPane"] > div:nth-child(2) > div > div' }
    get periodFrom() { return 'div[data-test-id="ToolsPane"] > div:nth-child(2) > div > div:nth-child(2)' }
    get periodTo() { return 'div[data-test-id="ToolsPane"] > div:nth-child(2) > div > div:nth-child(3)' }
    get addReport() { return 'div[data-test-id="ToolsPane"] > div:nth-child(3) div button' }

    get rows() { return this.bro.$$('tbody > tr').map(el => el.getText()) }
    get table() { return this.bro.$$('tbody') }
    get tableRow() { return this.bro.$$('tbody > tr') }

    constructor(bro: WebdriverIO.Browser) {
        super(bro);
        this.bro = bro;
    }
    async selectStatus(st: ReportStatus) {
        try {
            await this.bro.yaWaitForVisible(this.reportStatusIndicator, 500);
            await this.bro.$(this.reportStatusIndicator).waitForClickable();
        } catch (e) {
            await this.bro.$(this.reportStatus).waitForClickable();
            await this.bro.$(this.reportStatus).click();
        }

        switch (st) {
            case ReportStatus.All: {
                const button = '.popover-base__popper li:nth-child(1) button';
                await this.bro.$(button).click();
                await this.bro.yaWaitForHidden(button);
                await this.bro.yaWaitForVisible(this.indicator);
                break;
            }
            case ReportStatus.Draft: {
                const button = '.popover-base__popper li:nth-child(2) button';
                await this.bro.$(button).click();
                await this.bro.yaWaitForHidden(button);
                await this.bro.yaWaitForVisible(this.indicator);
                break;
            }
            case ReportStatus.Sent: {
                const button = '.popover-base__popper li:nth-child(3) button';
                await this.bro.$(button).click();
                await this.bro.yaWaitForHidden(button);
                await this.bro.yaWaitForVisible(this.indicator);
                break;
            }
            default: {
                assert(false, 'Test internal error: no enum found');
            }
        }
    }

    async open(path?: number, tail?: string) {
        if (!path && !tail) {
            await super.openBase(ordPath, this.indicator);
        } else if (!path && tail) {
            await super.openBase(ordPath + tail, this.indicator);
        } else if (path && !tail) {
            await super.openBase(ordPathById(path), this.indicator);
        } else {
            await super.openBase(ordPathById(path) + tail, this.indicator);
        }

        await this.bro.yaWaitForPageLoad();
    }
}
