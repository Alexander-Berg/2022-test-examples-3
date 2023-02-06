import { RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

class NoElementError extends Error {
}

class TooManyElementsError extends Error {
}

export function selectElement(container: Element, selector: string): Element {
    const el = container.querySelectorAll(selector);

    if (el === null) {
        throw new NoElementError(`Element not found: ${selector}`);
    }

    if (el.length > 1) {
        throw new TooManyElementsError(`Found multiple elements matching selector: ${selector}`);
    }

    return el[0];
}

export class PageFragment {
    constructor(public container: Element) {
    }

    /**
     * Возвращает найденный элемент или null.
     * Бросает исключение, если нашлось больше одного элемента.
     *
     * @param c - расширение PageFragment, которое описывает искомый элемент
     * @param selector - css-селектор для поиска элемента
     *
     * @throws - если нашлось больше одного элемента
     */
    protected query<T extends PageFragment>(c: new (container: Element) => T, selector: string): T | null {
        try {
            return this.get(c, selector);
        } catch (e) {
            if (e instanceof NoElementError) {
                return null;
            }

            throw e;
        }
    }

    /**
     * Возвращает найденный элемент.
     * Бросает исключение, если элемента не нашлось.
     * Бросает исключение, если нашлось больше одного элемента.
     *
     * @param c - расширение PageFragment, которое описывает искомый элемент
     * @param selector - css-селектор для поиска элемента
     *
     * @throws - если элемента не нашлось, или если нашлось больше одного элемента
     */
    protected get<T extends PageFragment>(c: new (container: Element) => T, selector: string): T {
        const el = selector ? selectElement(this.container, selector) : this.container;
        return new c(el);
    }

    /**
     * Возвращает коллекцию найденных элементов. Если совпавших элементов нет, возвращает пустой массив.
     *
     * @param c - расширение PageFragment, которое описывает искомые элементы
     * @param selector - css-селектор для поиска элементов
     */
    protected getAll<T extends PageFragment>(c: new (container: Element) => T, selector: string): T[] {
        return Array.from<Element>(this.container.querySelectorAll(selector))
            .map(el => new c(el));
    }
}

export class RootPageFragment extends PageFragment {
    constructor(public renderResult: RenderResult) {
        super(renderResult.container);
    }
}

export class Checkbox extends PageFragment {
    click() {
        userEvent.click(this.inputControl);
    }

    get inputControl() {
        return this.container?.getElementsByTagName('input')[0];
    }

    get isChecked() {
        return this.inputControl?.checked || false;
    }

    get isDisabled() {
        return this.inputControl?.disabled;
    }
}

export class Button extends PageFragment {
    click() {
        userEvent.click(this.container);
    }

    get buttonControl() {
        return this.container as HTMLButtonElement;
    }

    get isDisabled() {
        return this.buttonControl.disabled;
    }

    get isInProgress() {
        return this.container.classList.contains('Button2_progress');
    }
}
