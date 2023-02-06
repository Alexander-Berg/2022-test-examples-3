declare namespace ShotNamespace {
    type Test = CatNamespace.Test;

    type Describe = CatNamespace.Describe;

    type Viewport = {viewport: {width: number; height: number}};

    type SetTransparentBackground = () => Promise<void>;

    type IncreasedThresholdConfig = {
        comparisonMethod?: 'pixelmatch' | 'ssim';
        failureThreshold?: number;
        failureThresholdType?: 'pixel' | 'percent';
    };

    type MakeScreenshot = (
        options?: import('puppeteer').ScreenshotOptions,
        element?: import('puppeteer').ElementHandle | null,
    ) => Promise<Buffer>;

    type ExpectToMatchImageSnapshot = (
        screenshot: Buffer,
        options?: import('jest-image-snapshot').MatchImageSnapshotOptions,
    ) => void;

    type GetElementTankerDictionary = (selector?: string) => Promise<Record<string, string>>;

    type Render = (
        component: JSX.Element,
        options?: import('@yandex-market/jest-puppeteer-react').JestPuppeteerReactRenderOptions,
    ) => Promise<import('puppeteer').Page>;

    export interface Shot {
        /**
         * Функция обертка над стандартным it,
         * которая дополнительно привязывает к отчету id кейса
         * @example
         * shot.test({id: 'marketmbi-1353', name: '...'}, () => {...});
         */
        test: Test;

        /**
         * Функция обертка над стандартным describe.
         * Валидирует название для группировки блоков тестов в allure-отчете
         * @example
         * shot.describe('Page. Страница Заказы', () => {...});
         */
        describe: Describe;

        /**
         * Константа, устанавливающая десктопный viewport для скриншотов.
         * Используется, если нужно посмотреть на очень широкий компонент.
         * @example
         * await render(
         *    <Provider store={store}>
         *      <WeeklyStat />
         *    </Provider>,
         *    shot.desktopViewport,
         * );
         */
        desktopViewport: Viewport;

        /**
         * Константа, устанавливающая мобильный viewport для скриншотов.
         * Используется, если нужно посмотреть на компонент в мобильном разрешении.
         * @example
         * await render(
         *    <Provider store={store}>
         *      <WeeklyStat />
         *    </Provider>,
         *    shot.mobileViewport,
         * );
         */
        mobileViewport: Viewport;

        /**
         * Устанавливает прозрачный фон у скриншота.
         * Очень полезна при скриншотах модальных окон, чтобы не было различий в тенях.
         * @example
         * await shot.setTransparentBackground();
         */
        setTransparentBackground: SetTransparentBackground;

        /**
         * Конфиг, увеличивающий процент несовпадения при проверке скриншотов.
         * Нужен для компонентов с большими svg изображениями или в других краевых случаях.
         * По возможности, его нужно избегать и исследовать все случаи, когда он понадобился.
         * @example
         * shot.expectToMatchImageSnapshot(screenshot, shot.increasedThresholdConfig);
         */
        increasedThresholdConfig: IncreasedThresholdConfig;

        /**
         * Функция для скриншота страницы/элемнета.
         * Обёртка над page.screenshot из puppeteer
         * @example
         * const screenshot = await shot.makeScreenshot();
         */
        makeScreenshot: MakeScreenshot;

        /**
         * Функция для сравнения скриншота со снепшотом.
         * Обёртка над expect().toMatchImageSnapshot из jest-image-snapshot
         * Нужна для формирования уникальных коротких имён скриншотов
         * @example
         * shot.expectToMatchImageSnapshot(screenshot);
         */
        expectToMatchImageSnapshot: ExpectToMatchImageSnapshot;

        /**
         * Функция для получения словаря только нужных танкерных переводов.
         * Для того, чтобы все сработало, надо запустить тесты командой npm run test:screenshot:i18n:generate-dict
         * @example
         * const dictionary = await shot.GetElementTankerDictionary('body');
         * @example
         * const keys = await shot.GetElementTankerDictionary();
         */
        getElementTankerDictionary: GetElementTankerDictionary;

        /**
         * Функция для рендера компонента
         * Нужна кастомная, чтобы наверняка дожидаться отрендеренного элемента
         * @example
         * await shot.render(<Component />, options)
         */
        render: Render;
    }
}

/**
 * Неймспейс для скриншотных тестов
 */
declare const shot: ShotNamespace.Shot;
