import '../hermione/auth';
import { rewardsPath as exportedRewardsPath } from '../../.config/vars';
import { historyRewardsPath as exportedHistoryRewardsPath } from '../../.config/vars';
import { historyRewardsDetailsPath as exportedHistoryRewardsDetailsPath } from '../../.config/vars';
import { authorize } from '../hermione/auth';

export const rewardsPath = exportedRewardsPath;
export const historyRewardsDetailsPath = exportedHistoryRewardsDetailsPath;
export const historyRewardsPath = exportedHistoryRewardsPath;

export function delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export async function setup(browser: WebdriverIO.Browser) {
    await authorize(browser);
}
