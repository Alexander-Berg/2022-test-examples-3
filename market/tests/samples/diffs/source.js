type A = {
    x: string,
    y: boolean,
};

type B = {
    y: boolean,
};

type X = $Diff<A, B>;
type Y = $Diff<A, {x: string}>;
type Z = $Diff<{x: string, y: boolean}, {x: string}>;

export {X, Y, Z};
