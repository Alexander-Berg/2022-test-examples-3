import { yaOpenComponent } from '../hermione/commands/yaOpenComponent';

declare global {
    namespace WebdriverIO {
        interface Browser extends WebdriverIO.Browser {
            yaOpenComponent: typeof yaOpenComponent;
        }
    }

    namespace Hermione {
        interface TestDefinitionCallbackCtx {
            PO: IPageObject
        }

        interface TestDone {
            (error?: unknown): unknown;
            browser: WebdriverIO.Browser
        }
    }

    interface IPageObjectElem {
        (): string;

        [elem: string]: IPageObjectElem;
    }

    interface IPageObject {
        [elem: string]: IPageObjectElem;
    }
}
