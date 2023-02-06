import type { IMetrikaGoal } from '../../typings';
/**
 * Проверяет, что была отправлена определённая метричная цель.
 * Дожидается отправки в течение секунды.
 *
 * @param {IMetrikaGoal} goal
 */

export async function yaCheckMetrikaGoal(this: WebdriverIO.Browser, goal: IMetrikaGoal) {
    const { counterId, name: goalName, params: goalParams = {} } = goal;

    const getMessage = async() => {
        const msg = [`В счётчик ${counterId} не отправилась цель «${goalName}» с параметрами ${JSON.stringify(goalParams)}.`];
        const goalsByName = await this.yaGetMetrikaGoals({
            counterId,
            name: goalName,
        });

        if (goalsByName.length) {
            msg.push(`Отправленные цели с именем «${goalName}»: ${JSON.stringify(goalsByName, null, 2)}.`);
        } else {
            msg.push(`В счётчик ${counterId} не было отправлено ни одной цели с именем «${goalName}».`);
        }

        return msg.join('\n');
    };

    const condition = async() => {
        const goals = await this.yaGetMetrikaGoals(goal);
        return goals.length > 0;
    };

    await this.waitUntil(condition, { timeout: 1500 }).catch(async e => {
        const message = await getMessage();
        e.message = `${message}\n(Original error: ${e.message})`;
        throw e;
    });
}
