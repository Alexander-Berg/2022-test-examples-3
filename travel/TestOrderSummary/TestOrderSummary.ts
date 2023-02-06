import TestTrainItem from 'helpers/project/trains/components/TestOrderSummary/components/TestTrainItem/TestTrainItem';
import TestInsurance from 'helpers/project/trains/components/TestOrderSummary/components/TestInsurance';
import TestTotalPrice from 'helpers/project/trains/components/TestOrderSummary/components/TestTotalPrice';
import TestGoToConfirmStep from 'helpers/project/trains/components/TestOrderSummary/components/TestGoToConfirmStep';

import {Button} from 'components/Button';
import {ComponentArray} from 'components/ComponentArray';
import {Component} from 'components/Component';

/**
 * Корзинка на страницах заказа в ЖД
 */
export class TestOrderSummary extends Component {
    title: Component;
    trains: ComponentArray<TestTrainItem>;
    insurance: TestInsurance;
    totalPrice: TestTotalPrice;
    orderButton: Button;
    goToConfirmStep: TestGoToConfirmStep | null;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.trains = new ComponentArray(
            browser,
            {parent: this.qa, current: 'trainItem'},
            TestTrainItem,
        );

        this.insurance = new TestInsurance(browser, {
            parent: this.qa,
            current: 'insurance',
        });

        this.totalPrice = new TestTotalPrice(browser, {
            parent: this.qa,
            current: 'totalPrice',
        });

        this.orderButton = new Button(browser, {
            parent: this.qa,
            current: 'orderButton',
        });

        this.goToConfirmStep = this.isTouch
            ? new TestGoToConfirmStep(browser, {
                  parent: this.qa,
                  current: 'goToConfirmStep',
              })
            : null;
    }

    async getTotalPriceByTickets(): Promise<number> {
        return this.trains.reduce(async (accTotalPrice, trainItem) => {
            const ticketsTotalPrice = await trainItem.places.places.reduce(
                async (accTicketsTotalPrice, placeItem) => {
                    return (
                        accTicketsTotalPrice +
                        (await placeItem.price.getPriceValue())
                    );
                },
                0,
            );

            const beddingPrice = (await trainItem.bedding.getPrice()) ?? 0;

            return accTotalPrice + ticketsTotalPrice + beddingPrice;
        }, 0);
    }
}
