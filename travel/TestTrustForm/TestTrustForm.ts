import {MINUTE, SECOND} from 'helpers/constants/dates';

import {Component} from 'components/Component';

import {ITestCard, card} from './card';

export class TestTrustForm extends Component {
    private get selectors(): {
        frame: string;
        form: string;
        payButton: string;
        prePayButton: string | null;
        cardNumberInput: string;
        cardValidDateMonthInput: string;
        cardValidDateYearInput: string;
        cardCVCInput: string;
        saveCheckboxChecked: string;
        frame3DS: string;
        input3DS: string;
        submit3DS: string;
        newCardButton: string;
    } {
        const common = {
            frame: '[data-label="card-form--frame"]',
            form: 'form',
            payButton: '[data-label="card-form--submit"] button',
            saveCheckboxChecked: '[data-label="card-form--card-bind"] input',
            frame3DS: 'iframe[name="frame-3ds"]',
            input3DS: 'input[name="3ds_code"]',
            submit3DS: 'input[type="submit"]',
            newCardButton: '[data-label="method--new-card"]',
            cardNumberInput: '#card_number-input',
            cardValidDateMonthInput:
                '.card_valid-date input[name=expiration_month]',
            cardValidDateYearInput:
                '.card_valid-date input[name=expiration_year]',
            cardCVCInput: '#card_cvv-input',
        };

        if (this.isTouch) {
            return {
                ...common,
                prePayButton: '[data-label="checkout--submit"]',
            };
        }

        return {
            ...common,
            prePayButton: null,
        };
    }

    async pay({
        number,
        validDateMonth,
        validDateYear,
        CVC,
    }: ITestCard = card): Promise<void> {
        await this.initialize();

        await this.setCardNumber(number);
        await this.setValidDate(validDateMonth, validDateYear);
        await this.setCVC(CVC);

        await this.submit();

        await this.handle3DS();
    }

    private async handle3DS(): Promise<void> {
        try {
            await this.browser
                .$(this.selectors.frame3DS)
                .waitForExist({timeout: 30 * SECOND});
        } catch (e) {
            // не дождались 3ds - значит его нет
            return;
        }

        await this.fallIntoFrame(this.selectors.frame3DS);

        await this.browser
            .$(this.selectors.input3DS)
            .waitForExist({timeout: MINUTE});

        await this.fillField(this.selectors.input3DS, '200');

        await this.castylClick(this.selectors.submit3DS);
    }

    private async initialize(): Promise<void> {
        await this.fallIntoFrame('iframe');

        if (!(await this.browser.$(this.selectors.frame).isDisplayed())) {
            await this.selectNewCard();
        }

        await this.fallIntoFrame(this.selectors.frame, 30 * SECOND);
    }

    private async fallIntoFrame(
        selector: string,
        timeout: number = 10 * SECOND,
    ): Promise<void> {
        await this.browser.$(selector).waitForExist({timeout});

        const frame = await this.browser.$(selector);

        await this.browser.switchToFrame(frame);
    }

    /**
     * Выбираем новую карту в списке карт
     */
    private async selectNewCard(): Promise<void> {
        await this.castylClick(this.selectors.newCardButton);

        if (this.isTouch && this.selectors.prePayButton) {
            await this.castylClick(this.selectors.prePayButton);
        }
    }

    private async fillField(
        selector: string,
        value: string,
        timeout?: number,
    ): Promise<void> {
        const fieldElement = this.browser.$(selector);

        await fieldElement.waitForClickable({timeout});

        await fieldElement.click();
        await fieldElement.addValue(value);
    }

    private async setCardNumber(cardNumber: string): Promise<void> {
        await this.fillField(this.selectors.cardNumberInput, cardNumber);
    }

    private async setValidDate(month: number, year: number): Promise<void> {
        await this.fillField(
            this.selectors.cardValidDateMonthInput,
            String(month).padStart(2, '0'),
        );
        await this.fillField(
            this.selectors.cardValidDateYearInput,
            String(year).padStart(2, '0'),
        );
    }

    private async setCVC(cvc: number): Promise<void> {
        await this.fillField(
            this.selectors.cardCVCInput,
            String(cvc).padStart(3, '0'),
        );
    }

    private async submit(): Promise<void> {
        await this.browser.switchToParentFrame();

        await this.uncheckCardSave();

        await this.castylClick(this.selectors.payButton);
    }

    private async uncheckCardSave(): Promise<void> {
        const checkboxIsVisible = await this.browser
            .$(this.selectors.saveCheckboxChecked)
            .isDisplayed();

        /**
         * Галочки может и не быть:
         * - если выбрана сохраненная карта
         * - если пользователь не авторизован,
         *
         */
        if (!checkboxIsVisible) {
            return;
        }

        const checkbox = await this.browser.$(
            this.selectors.saveCheckboxChecked,
        );

        await checkbox.scrollIntoView({block: 'center'});

        await checkbox.click();
    }

    private async castylClick(selector: string): Promise<void> {
        if (this.isTouch) {
            /**
             * Castyl: Приходится кликать через js,
             * обычный клик выполняется, но ничего не делает в таче
             */
            await this.browser.clickJS(selector);
        } else {
            const element = this.browser.$(selector);

            await element.click();
        }
    }
}
