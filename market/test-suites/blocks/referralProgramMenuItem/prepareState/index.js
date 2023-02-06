import {referralProgramPerk, referralProgramGotFullReward} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

export async function commonPrepareState({isReferralProgramActive, isGotFullReward}) {
    if (isReferralProgramActive) {
        await this.browser.setState(
            'Loyalty.collections.perks',
            isGotFullReward ? [referralProgramGotFullReward] : [referralProgramPerk]
        );
    } else {
        await this.browser.setState('Loyalty.collections.perks', []);
    }
}

function prepareState() {}

declare export default prepareState;
