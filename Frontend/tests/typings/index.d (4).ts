import { yaOpenComponent } from '../hermione/commands/yaOpenComponent';

declare global {
    namespace WebdriverIO {
        interface Browser {
            yaOpenComponent: typeof yaOpenComponent;
            yaLoginWritable(): Promise<void>;
            yaAddHousehold(name: string, address?: string): Promise<void>;

            /**
             * Клик в элемент на основе селектора. Например селектор - элемент меню
             * @param itemsSelector - селектор элемента
             * @param itemText - селектор текста
             * @param strict - строгое сравнение
             */
            yaClickToSelectorByText(itemsSelector: string, itemText: string | string[], strict?: boolean): Promise<void>;

            /**
             * Takes a screenshot of the passed selector and compares the received screenshot with the reference.
             *
             * @remarks
             * For more details, see {@link https://github.com/gemini-testing/hermione#assertview documentation}.
             *
             * @example
             * ```ts
             *
             * it('some test', function() {
             *     return this.browser
             *         .url('some/url')
             *         .assertView(
             *             'plain',
             *             '.button',
             *             {
             *                 ignoreElements: ['.link'],
             *                 tolerance: 2.3,
             *                 antialiasingTolerance: 4,
             *                 allowViewportOverflow: true,
             *                 captureElementFromTop: true,
             *                 compositeImage: true,
             *                 screenshotDelay: 600,
             *                 selectorToScroll: '.modal'
             *             }
             *         )
             *});
             * ```
             *
             * @param state state name, should be unique within one test
             * @param selectors DOM-node selector that you need to capture
             * @param opts additional options, currently available:
             * "ignoreElements", "tolerance", "antialiasingTolerance", "allowViewportOverflow", "captureElementFromTop",
             * "compositeImage", "screenshotDelay", "selectorToScroll"
             */
            yaAssertView(state: string, selectors: string | Array<string>, opts?: Hermione.AssertViewOpts): Promise<void>;

            /**
             * Делает скриншот модалки BottomSheet
             *
             * @remarks
             * For more details, see {@link https://github.com/gemini-testing/hermione#assertview documentation}.
             *
             * @example
             * ```ts
             *
             * it('some test', function() {
             *     return this.browser
             *         .url('some/url')
             *         .assertView(
             *             'plain',
             *             '.button',
             *             {
             *                 ignoreElements: ['.link'],
             *                 tolerance: 2.3,
             *                 antialiasingTolerance: 4,
             *                 allowViewportOverflow: true,
             *                 captureElementFromTop: true,
             *                 compositeImage: true,
             *                 screenshotDelay: 600,
             *                 selectorToScroll: '.modal'
             *             }
             *         )
             *});
             * ```
             *
             * @param state state name, should be unique within one test
             * @param selectors DOM-node selector that you need to capture
             * @param opts additional options, currently available:
             * "ignoreElements", "tolerance", "antialiasingTolerance", "allowViewportOverflow", "captureElementFromTop",
             * "compositeImage", "screenshotDelay", "selectorToScroll"
             */
            yaAssertViewBottomSheet(state: string, selectors: string | Array<string>, opts?: Hermione.AssertViewOpts): Promise<void>;

            /**
             * @deprecated - assetView - устарело, используй yaAssertView
             */
            assertView: any;
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
