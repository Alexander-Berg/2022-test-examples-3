declare global {
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
