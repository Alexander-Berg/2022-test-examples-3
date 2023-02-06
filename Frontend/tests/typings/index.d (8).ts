// eslint-disable-next-line no-unused-vars,import/no-extraneous-dependencies,@typescript-eslint/no-unused-vars
import { Context } from 'mocha';
// eslint-disable-next-line import/no-extraneous-dependencies
import { Client } from 'webdriverio';

declare module 'webdriverio' {
    interface Client<T> {
        /* Hermione specific commands */
        assertView: (
            state: string,
            selector: string | string[],
            opts?: AssertViewOptions
        ) => Client<T>;

        /* Custom commands */
        yaOpenComponent: (
            id: string, withPlatform?: boolean, knobs?: Array<{name: string, value: string}>
        ) => Client<T>;
    }
}

declare global {
    interface IPageObjectElem {
        (): string;
        [elem: string]: IPageObjectElem;
    }

    interface IPageObject {
        [elem: string]: IPageObjectElem;
    }

    // eslint-disable-next-line no-undef
    class HermioneContext extends Context {
        browser: Client<unknown>;
        PO: IPageObject;
    }
}
