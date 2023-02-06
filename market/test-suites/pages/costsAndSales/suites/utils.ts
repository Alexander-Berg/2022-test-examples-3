import type {Actions, Find} from 'spec/gemini/utils';
import {OptionSelector} from 'spec/pageObjects/containers';

export const clickOnOption = (index: number, actions: Actions, find: Find) => {
    const item = find(OptionSelector.popupItem(index));
    actions.click(item);
    actions.wait(1000);
};
