import {
    yandexPlusPerk,
    referralProgramPerk,
    referralProgramGotFullReward,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

async function prepareState() {
    const perks = [];

    if (this.params.isYaPlus) {
        perks.push(yandexPlusPerk);
    }

    if (this.params.isGotFullReward) {
        perks.push(referralProgramGotFullReward);
    } else {
        perks.push(referralProgramPerk);
    }

    await this.browser.setState('Loyalty.collections.perks', perks);

    if (typeof this.params.specialPrepareState === 'function') {
        await this.params.specialPrepareState.call(this);
    }

    // Небольшая задержка, что бы анимированное появление контента успевало пройти
    await this.browser.yaDelay(150);
}

export default prepareState;
