import type { IMetrikaGoal, TMetrikaParams } from '../../typings';

/**
 * Функция частичного сравнения объектов.
 * @param {object} full - полный объект
 * @param {object} partial - объект-подмножество полного объекта
 * @returns {boolean} - соответствуют ли все поля объекта-подмножества полям из полного объекта
 */

type PartialDeepEqualsInput = TMetrikaParams | keyof TMetrikaParams;

const metrikaParamsPartialDeepEquals = (full: PartialDeepEqualsInput, partial: PartialDeepEqualsInput): boolean => {
    if (!full || typeof full !== 'object') {
        return full === partial;
    }
    if (!partial || typeof partial !== 'object') {
        return false;
    }
    const fullKeys = Object.keys(full);
    const partialKeys = Object.keys(partial);
    for (const key of partialKeys) {
        if (!fullKeys.includes(key) || !metrikaParamsPartialDeepEquals(full[key], partial[key])) {
            return false;
        }
    }
    return true;
};

/**
 * Возвращает список метричных целей по критериям на текущий момент времени.
 *
 * @param {WebdriverIO.Browser} this
 * @param {IMetrikaGoal} goal
 * @returns {[String, IMetrikaParams][]} - возвращает информацию о цели, если она не была отправлена
 */
export async function yaGetMetrikaGoals(this: WebdriverIO.Browser, {
    counterId,
    name: goalName,
    params: goalParams,
}: IMetrikaGoal) {
    const goalsList = await this.execute(id => window.Ya.Metrika.getGoalsFor(id, 0), counterId);

    const result = goalsList.filter(([name, params]) => {
        let isSame = name === goalName;
        if (typeof goalParams !== 'undefined') {
            isSame = isSame && metrikaParamsPartialDeepEquals(params, goalParams);
        }
        return isSame;
    });

    return result;
}
