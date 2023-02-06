import { waitFor } from '@testing-library/react';

export function compareString(input: string | undefined | null, str: string | RegExp) {
    input = (input || '').trim();

    return typeof str === 'string' ? input === str : str.test(input);
}

export function findAllElements(container: Element, selector: string): Element[] {
    return Array.from(container.querySelectorAll(selector));
}

export function getText({ textContent }: Element): string {
    return textContent === null ? '' : textContent.trim();
}

export function selectElement(container: Element, selector: string): Element {
    const el = container.querySelector(selector);
    if (el === null) {
        throw new Error(`element not found: ${selector}`);
    }

    return el;
}

export function selectTypedElement<T extends Element>(ctr: new () => T, container: Element, selector: string): T {
    const el = selectElement(container, selector);

    return castElement(ctr, el);
}

export function castElement<T extends Element>(ctr: new () => T, el: Element) {
    if (el instanceof ctr) {
        return el as T;
    }

    throw new Error(`element is ${el} but ${ctr} expected`);
}

export function findElement(container: Element, selector: string, text?: string|RegExp): Element | undefined {
    const elements = Array.from<Element>(container.querySelectorAll(selector));

    if (elements.length) {
        if (text) {
            return elements.find(el => compareString(el.textContent, text));
        }

        return elements[0];
    }

    return undefined;
}

export function findPopupElement(selector: string, container: Element = document.body) {
    return container.querySelector(selector) || undefined;
}

export async function waitForPopupElement(selector: string, container: Element = document.body): Promise<Element> {
    await waitFor(() => {
        expect(findPopupElement(selector, container)).toBeInTheDocument();
    });

    return selectElement(container, selector);
}
