/**
 * Создаёт символ.
 *
 * @function
 * @returns {symbol}
 */
const createSymbol = () => {
    return Symbol('something');
};

/**
 * Создаёт случайную строку.
 *
 * @function
 * @returns {string}
 */
const createString = () => {
    return Math.random().toString(16).slice(2);
};

/**
 * Создаёт случайное целое число в диапазоне [1, 100].
 *
 * @function
 * @returns {number}
 */
const createNumber = () => {
    return Math.round(Math.random() * 100);
};

/**
 * Создаёт массив указанной длины, заполненый индексами.
 *
 * @function
 * @param {number} length
 */
const createArray = (length) => {
    const array = Array.from({ length }).fill(0);

    return array.map((_, index) => index);
};

/**
 * Создаёт массив указанной длины, применяя конструктор для создания каждого элемента.
 *
 * @function
 * @param {number} length
 * @param {(index: number) => any} itemConstructor
 * @returns {any[]}
 */
const createArrayOf = (length, itemConstructor) => {
    return createArray(length).map(itemConstructor);
};

/**
 * Создаёт массиа символов.
 *
 * @function
 * @param {number} [length = 5]
 * @returns {symbol[]}
 */
const createArrayOfSymbols = (length = 5) => {
    return createArrayOf(length, createSymbol);
};

/**
 * Создаёт массиа строк.
 *
 * @function
 * @param {number} [length = 5]
 * @returns {string[]}
 */
const createArrayOfStrings = (length = 5) => {
    return createArrayOf(length, createString);
};

/**
 * Создаёт массиа чисел.
 *
 * @function
 * @param {number} [length = 5]
 * @returns {number[]}
 */
const createArrayOfNumbers = (length = 5) => {
    return createArrayOf(length, createNumber);
};

/**
 * Создаёт объект с указанными застабленными методами.
 *
 * @function
 * @param {string[]} methods
 * @param {Sinon} sinonInstance
 */
const stubObj = (methods, sinonInstance = null) => {
    const theSinon = sinonInstance || sinon;

    if (!theSinon) {
        throw new Error('Sinon not found');
    }

    const stubObj = Object.create(null);
    const preparedMethods = Array.from(methods || []);

    preparedMethods.forEach((name) => {
        stubObj[name] = theSinon.stub();
    });

    return stubObj;
};

/**
 * Создаёт экземпляр mobx модели, делая геттеры модели переопределяемыми.
 *
 * @function
 * @param {Object} params
 * @returns {Object}
 */
const createStore = (model, params = {}) => {
    const defineProperty = Object.defineProperty;

    Object.defineProperty = (target, property, attributes) => {
        const newAttributes = { ...attributes };

        if (newAttributes.configurable === false && property !== 'snapshot') {
            newAttributes.configurable = true;
        }

        return defineProperty(target, property, newAttributes);
    };

    const instance = model.create(params);

    Object.defineProperty = defineProperty;

    return instance;
};

const mockResponse = (body = '', options = {}) => {
    const ops = Object.assign({
        status: 200,
        headers: { 'Content-type': 'application/json' }
    }, options);

    return Promise.resolve(new window.Response(body, ops));
};

export {
    createNumber,
    createSymbol,
    createString,
    createArray,
    createArrayOf,
    createArrayOfNumbers,
    createArrayOfSymbols,
    createArrayOfStrings,
    stubObj,
    createStore,
    mockResponse
};
