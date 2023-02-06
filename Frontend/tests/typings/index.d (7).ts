import { yaOpenComponent } from '../hermione/commands/yaOpenComponent';

declare global {
    namespace WebdriverIO {
        interface Browser {
            yaOpenComponent: typeof yaOpenComponent;
        }
    }

    namespace Hermione {
        interface TestDefinitionCallbackCtx {
            PO: PageObject
        }
    }

    interface PageObjectElem {
        (): string;
        [elem: string]: PageObjectElem;
    }

    interface PageObject {
        [elem: string]: PageObjectElem;
    }
}
