declare namespace CatNamespace {
    // Common units

    type Callback = () => void;

    type MaybeElement<T extends Element = Element> = T | null;

    type Collection<T = any> = Record<string, T>;

    // Helpers

    type TestParams = {
        name: string;
        id: string;
        isMobile?: boolean;
        skip?: boolean;
    };
    type Test = ({name, id, isMobile, skip}: TestParams, fn: Callback) => void;

    export type DescribeNameScope = 'Page' | 'Pidget' | 'Component' | 'Widget' | 'Examples';
    // eslint-disable-next-line prettier/prettier
    export type DescribeName<Scope extends DescribeNameScope> = `${Scope}. ${string}`;
    type Describe = <Scope extends DescribeNameScope>(name: DescribeName<Scope>, fn: Callback) => void;

    type Step = <T>(name: string, fn: () => T) => T;

    type GetBySelector = <T extends Element = Element>(query: string, container?: Element) => T;

    type QueryBySelector = <T extends Element = Element>(query: string, container?: Element) => MaybeElement<T>;

    type QueryBySelectorAll = <T extends Element = Element>(query: string, container?: Element) => T[];

    type GetElementsTextsBySelector = (query: string, container?: Element) => Array<string | null>;

    type QueryByI18nElementDeep = (elem: MaybeElement, key?: string) => MaybeElement;

    type GetI18nKeyDeep = (elem: MaybeElement) => string | null;

    type GetI18nParamsDeep = (elem: MaybeElement) => Record<string, unknown>;

    type GetI18nDataDeep = (elem: MaybeElement) => {key: string | null; params: Record<string, unknown>};

    type QueryByI18nKeyDeep = (key: string, elem?: MaybeElement) => MaybeElement;

    type JoinableSelectors = {parentSelector?: string; additionalSelector?: string};

    type JoinSelectors = (selector: string, joinableSelectors: JoinableSelectors) => string;

    // Asserts

    type AssertCssRule = (rule: string, value: string, element: HTMLElement) => void;

    type AssertLink = (expectedKey: string | null, expectedUrl: string, element: MaybeElement) => void;

    type AssertI18nKeyDeep = (
        expected: string | {key: string; params?: Collection; visible?: boolean},
        element?: MaybeElement,
    ) => void;

    type AssertI18nToBeContained = (i18nKey: string, element: Element) => void;

    type AssertI18nParams = (expected: Collection, element: Element) => void;

    type WaitForGoalReached = (
        store: any,
        params: {
            goal: {type: string; payload?: any};
            task: () => any;
            timeout?: number;
            commonActionType?: string;
        },
    ) => Promise<void>;

    export interface Cat {
        /**
         * Функция обертка над стандартным it,
         * которая дополнительно привязывает к отчету id кейса
         * @example
         * cat.test({id: 'marketmbi-1353', name: '...'}, () => {...});
         */
        test: Test;

        /**
         * Функция обертка над стандартным describe.
         * Валидирует название для группировки блоков тестов в allure-отчете
         * @example
         * cat.describe('Page. Страница Заказы', () => {...});
         */
        describe: Describe;

        /**
         * Функция описывает шаг для allure-отчета.
         * Внутри шага могут быть произвольные jest-проверки и подшаги
         * @example
         * await cat.step('Проверяем данные в нижнем блоке', async () => {
         *   await cat.step('Ждем пока спиннер догрузится', async () => {
         *     await waitFor(...);
         *   })
         * })
         */
        step: Step;

        /**
         * Функция для поиска элемента на странице по селектору.
         * Если элемент не найден, функция выкинет исключение.
         * @example
         * cat.getBySelector('[data-e2e="switcher-crossdock"]')
         */
        getBySelector: GetBySelector;

        /**
         * Функция для поиска элемента на странице по селектору.
         * Если элемент не найден, функция вернет null.
         * @example
         * cat.queryBySelector('[data-e2e="switcher-crossdock"]')
         */
        queryBySelector: QueryBySelector;

        /**
         * Функция для поиска элементов по селектору.
         * Вернет массив (не htmlcollection) элементов.
         * @example
         * cat.queryBySelectorAll('[data-e2e="switcher-crossdock"]')
         */
        queryBySelectorAll: QueryBySelectorAll;

        /**
         * Функция для получения текстов элементов по селектору.
         * Вернет массив элементами которого могут быть строки и null.
         * @example
         * cat.getElementsTextsBySelector('[data-e2e="switcher-crossdock"]')
         */
        getElementsTextsBySelector: GetElementsTextsBySelector;

        /**
         * Находит у текущего или у одного из вложенных элементов
         * атрибут data-e2e-i18n-key и возвращает сам элемент.
         * Если элемент не найден, функция вернет null
         *
         * @example
         * expect(
         *  cat.queryI18nElementDeep(container)
         * ).toBeInTheDocument(),
         */
        queryI18nElementDeep: QueryByI18nElementDeep;

        /**
         * Находит у текущего или у одного из вложенных элементов
         * атрибут data-e2e-i18n-key и возвращает его ключ
         *
         * Аналог getI18nKeyDeep из AT (spec/utils/i18n.ts)
         *
         * @example
         * expect(
         *  cat.getI18nKeyDeep(element)
         * ).toEqual(pidgets.goods-stat-united:title)
         */
        getI18nKeyDeep: GetI18nKeyDeep;

        /**
         * Находит у текущего или у одного из вложенных элементов
         * атрибут data-e2e-i18n-key и возвращает его параметры
         *
         * Аналог getI18nKeyDeep из AT (spec/utils/i18n.ts)
         *
         * @example
         * expect(
         *  cat.getI18nParamsDeep(element)
         * ).toEqual({value: 1})
         */
        getI18nParamsDeep: GetI18nParamsDeep;

        /**
         * Находит у текущего или у одного из вложенных элементов
         * атрибут data-e2e-i18n-key и возвращает его ключ и параметры
         *
         * @example
         * expect(
         *  cat.getI18nDataDeep(element)
         * ).toEqual({
         *  key: pidgets.goods-stat-united:title,
         *  params: {value: 1},
         * }),
         */
        getI18nDataDeep: GetI18nDataDeep;

        /**
         * Находит у текущего или у одного из вложенных элементов
         * указанный i18n ключ и возвращает сам элемент.
         * Если элемент не найден, функция вернет null
         *
         * @example
         * expect(
         *  cat.queryByI18nKeyDeep('pidgets.goods-stat-united:title', container)
         * ).toBeInTheDocument(),
         */
        queryByI18nKeyDeep: QueryByI18nKeyDeep;

        /**
         * Функция для проверки css свойства и значения у элемента,
         * также можно проверять на отсутствие свойства у элемента передав в `value` пустое значение
         * @param rule css правило
         * @param value значение правила для проверки
         * @param element DOM-node для тестирования
         * @example
         * <p style="color: red">example</p>
         * assertCssRule('color', 'red', el) - OK
         * assertCssRule('color', 'green', el) - FAIL
         * assertCssRule('text-align', '', el) - OK
         */
        assertCssRule: AssertCssRule;

        /**
         * Проверяет на корректность текст и путь ссылки.
         * Так как в компонентных тестах нет зависимости от окружения,
         * то проверяется ключ ссылки, который в реальном приложении
         * заменяется роутером (client.next/legacy/router.tsx) на реальные
         *
         * @example
         * cat.assertLink(
         *   'pages.stat-fulfillment:async-reports.report.download-button',
         *   'market-partner:file:stat-async-report:get?reportId=xxx&campaignId=1000568416',
         *   cat.queryBySelector(pagePO.asyncReportsPO.reportDownloadLink),
         * );
         */
        assertLink: AssertLink;

        /**
         * Проверяет, что у текущего или у одного из вложенных
         * элементов есть атрибут data-e2e-i18n-key с заданным значением
         *
         * Аналог assertI18nKeyDeep из AT (spec/utils/asserts.ts)
         * @example
         * cat.assertI18nKeyDeep('pages.assortment:prices.hint', hint);
         */
        assertI18nKeyDeep: AssertI18nKeyDeep;

        /**
         * Проверяет, что i18n ключ содержится в элементе в виде текста.
         * Например, если используется функция i18n``, то data-e2e-i18n-key атрибута не будет.
         * @param i18nKey - ожидаемый ключ
         * @param element - элемент в котором ожидается строка в виде ключа
         * @example
         * cat.assertI18nToBeContained(
         *      'pages.installment:custom-installment-modal.products-group.error.empty',
         *      modalPO.categorySelectorAlert
         * );
         */
        assertI18nToBeContained: AssertI18nToBeContained;

        /**
         * Проверяет, что тег <I18n>
         * отрисовался с ожидаемым значением пропса params
         * @param expected ожидаемые params
         * @param element отрендеренный I18n
         * @example
         * const i18nParams = {quantity: PRICE_CHANGE_QUANTITY.MANY};
         * cat.assertI18nParams(i18nParams, po.priceModalPO.title);
         */
        assertI18nParams: AssertI18nParams;

        /**
         * Соединяет поданные селекторы в один, более точный.
         *
         * @param selector уточняемый селектор
         * @param joinableSelectors.parentSelector родительский селектор для уточняемого селектора
         * @param joinableSelectors.additionalSelector дополнительный селектор для уточняемого селектора
         * @example
         * const finalSelector = cat.joinSelectors('[data-e2e="elem"]', {
         *     parentSelector: '[data-e2e="parent-elem"]',
         *     additionalSelector: '[data-e2e="exactly-that-elem"]',
         * });
         */
        joinSelectors: JoinSelectors;

        /**
         * Функция для проверки достижения цели метрики, выбросит исключение
         * если цель с переданными параметрами не будет достигнута за указанное время
         * @param store - стор тестируемой страницы
         * @param goal - ожидаемые данные цели
         * @param task - действие, пораждающее достижение цели
         * @param timeout - время ожидания достижения цели
         * @param commonActionType - тип базового экшена
         * @example
         * waitForGoalReached(store, {
         *      goal: {type: 'goal_type', payload: {param: 'expected_param'}},
         *      task: () => fireEvent.click(buttonWithGoal),
         *      commonActionType: string,
         * })
         */
        waitForGoalReached: WaitForGoalReached;

        /**
         * Возвращает расширенный ПО.
         * Расширение PO спредом может вызвать ошибки, если в нем содержатся get-свойства.
         * @param po - оригинальный ПО.
         * @param methodCreator - функция принимающая оригинально по и возвращающая новые методы.
         * @returns ПО со новыми свойствами и оригинальным ПО в качестве прототипа.
         *
         * @example
         * const newPo = cat.extendPo(oldPo, po => ({
         *   someSelector() {
         *      return `${po.root} div`
         *   }
         * }))
         */
        extendPo: <T extends {rootSelector: string}, N extends Record<string, any>>(
            po: T,
            methodsCreator: (originalPo: T) => N,
        ) => Omit<T, keyof N> & N;
    }
}

/**
 * Неймспейс для компонентных тестов
 */
declare const cat: CatNamespace.Cat;
