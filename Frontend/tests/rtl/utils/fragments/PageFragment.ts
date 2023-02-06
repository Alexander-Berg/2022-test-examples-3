import {
    castElement,
    compareString,
    findAllElements,
    getText,
    selectElement,
    selectTypedElement
} from '../queries';
import { findElement } from '../queries';

export interface SelectorByTitleOptions {
    selector: string;
    titleSelector?: string;
    titleContainerSelector?: string;
}

export class PageFragment {
    constructor(public container: Element) {
    }

    get text() {
        return getText(this.container);
    }

    textFromSelector(selector: string): string {
        const element = this.selectElement(selector);
        return getText(element);
    }

    selectElement(selector: string): Element {
        return selectElement(this.container, selector);
    }

    selectTypedElement<T extends Element>(ctr: new () => T, selector: string): T {
        return selectTypedElement(ctr, this.container, selector);
    }

    findElement(selector: string): Element | undefined {
        return findElement(this.container, selector);
    }

    findTypedElement<T extends Element>(ctr: new () => T, selector: string): T | undefined {
        const el = findElement(this.container, selector);

        return el && castElement(ctr, el);
    }

    protected has(selector: string): boolean {
        return Boolean(this.findElement(selector));
    }

    protected get<T extends PageFragment>(c: new (container: Element) => T, selector: string): T {
        const el = selector ? selectElement(this.container, selector) : this.container;
        return new c(el);
    }

    protected find<T extends PageFragment>(c: new (container: Element) => T, selector: string): T | undefined {
        const el = selector ? this.findElement(selector) : this.container;
        return el ? new c(el) : undefined;
    }

    protected getAll<T extends PageFragment>(c: new (container: Element) => T, selector: string): T[] {
        return findAllElements(this.container, selector).map(el => new c(el));
    }

    protected getElementByTitle(
        title: string | RegExp,
        opts: SelectorByTitleOptions,
    ): Element {
        const element = this.findElementByTitle(title, opts);

        if (!element) {
            throw new Error(`The "${title}" element is not found`);
        }

        return element;
    }

    protected findElementByTitle(
        title: string | RegExp,
        opts: SelectorByTitleOptions,
    ): Element | undefined {
        const { selector, titleSelector, titleContainerSelector = selector } = opts;

        const titles = Array.from<Element>(this.container.querySelectorAll(titleContainerSelector))
            .map(el => titleSelector ? selectElement(el, titleSelector) : el)
            .map(titleEl => titleEl.textContent && titleEl.textContent.trim());

        const index = titles.findIndex(t => compareString(t, title));

        if (index < 0) {
            return undefined;
        }

        return this.container.querySelectorAll(selector)[index];
    }

    protected getFragmentByTitle<T extends PageFragment>(
        title: string | RegExp,
        c: new (container: Element) => T,
        opts: SelectorByTitleOptions,
    ): T {
        const fragment = this.findFragmentByTitle(title, c, opts);

        if (!fragment) {
            throw new Error(`The fragment for the title "${title}" is not found`);
        }

        return fragment;
    }

    protected findFragmentByTitle<T extends PageFragment>(
        title: string | RegExp,
        c: new (container: Element) => T,
        opts: SelectorByTitleOptions,
    ): T | undefined {
        const elem = this.findElementByTitle(title, opts);

        if (!elem) {
            return undefined;
        }

        return new c(elem);
    }
}
