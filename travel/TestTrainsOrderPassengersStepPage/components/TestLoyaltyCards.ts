import {Component, Input} from 'helpers/project/common/components';

export interface ILoyaltyCardsForm {
    roadCard?: string; // 'RzhdU',
    bonusCard?: string; // 'RzhdB'
}

export class TestLoyaltyCards extends Component {
    toggleLink: Component;
    roadCard: Input;
    bonusCard: Input;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'loyaltyCards') {
        super(browser, qa);
        this.toggleLink = new Component(browser, {
            parent: this.qa,
            current: 'toggleLink',
        });
        this.bonusCard = new Input(browser, {
            parent: this.qa,
            current: 'RzhdB',
        });
        this.roadCard = new Input(browser, {
            parent: this.qa,
            current: 'RzhdU',
        });
    }

    async fill(cards: ILoyaltyCardsForm): Promise<void> {
        await this.toggleLink.click();

        if (cards.roadCard) {
            await this.roadCard.type(cards.roadCard, true);
        }

        if (cards.bonusCard) {
            await this.bonusCard.type(cards.bonusCard, true);
        }
    }
}
