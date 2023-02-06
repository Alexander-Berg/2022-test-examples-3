// @flow

type A = {
    a: number,
    b: string
};

export type B = {
    ...A,
    b: number,
}

export type C = {
    ...A,
    c: boolean,
}

export type D = {
    ...A,
    ...{
        b: boolean,
        d: boolean
    }
}

export type WithExact = {
    ...$Exact<A>,
    extraProp1: string,
    extraProp2: number,
}
