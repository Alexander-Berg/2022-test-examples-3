import { assert } from 'chai';

interface IWizard {
    find: (selector: string) => []
}

export const assertHasExactlyCountElements = (wizard: IWizard, selector: string, count: number) => {
    const actualCount = wizard.find(selector).length;

    return assert.equal(
        wizard.find(selector).length,
        count,
        `Разметка должна содержать ровно ${count} элементов с селектором ${selector}. Было найдено ${actualCount} элементов.`,
    );
};

export const assertHasNoElements = (wizard: IWizard, selector: string) => {
    return assertHasExactlyCountElements(wizard, selector, 0);
};
