/* eslint-disable */
export const keysOf = <T>(obj: T) => {
    return Object.keys(obj) as Array<keyof T>;
};

/**
 * Создает цикличный генератор, возващающий элементы массива по порядку
 * @param arr
 */
export const makeGenerator = <T>(arr: T[]) => {
    return function*() {
        let i = 0;

        while (true) {
            yield arr[i];

            i = (i + 1) % arr.length;
        }
    };
};

export const makeGeneratorGetter = <T>(arr: T[]) => {
    const generator = makeGenerator(arr)();

    return () => generator.next().value;
};
