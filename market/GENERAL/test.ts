import type {Page, ElementHandle} from 'puppeteer';
import {WrongSelectorError} from '../constants';

export const querySelector = async <T extends Element = Element>(
    page: Page,
    selector: string,
    timeout?: number,
): Promise<ElementHandle<T>> => {
    return page
        .waitForSelector(selector, {timeout})
        .catch(() => {
            throw new WrongSelectorError(selector);
        })
        .then(el => {
            if (el == null) throw new WrongSelectorError(selector);
            return el;
        });
};
