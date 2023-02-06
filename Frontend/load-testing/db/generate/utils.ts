/* eslint-disable */
/** Возвращает целое число in [min; max] */
export function getRandomInt(min: number, max: number) {
    return Math.round(Math.random() * (max - min) + min);
}

export const getRandomBoolean = () => Math.random() >= 0.5;

export const bigTimeout = {
    retry: {
        timeout: 1000000,
        name: 'Query',
    },
};
