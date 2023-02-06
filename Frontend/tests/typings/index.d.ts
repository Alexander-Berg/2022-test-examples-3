import { yaOpenComponent } from '../hermione/commands/yaOpenComponent';

declare global {
    namespace WebdriverIO {
        interface Browser {
            yaOpenComponent: typeof yaOpenComponent;
        }
    }

    namespace Hermione {
        interface TestDefinitionCallbackCtx {
            PO: IPageObject
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
