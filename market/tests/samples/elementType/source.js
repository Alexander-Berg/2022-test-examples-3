type X<T> = {
    x: T | string,
};

type P = 'x';

type Q = R;

type R = S;

type S = {
    s: 'S',
};

type A = {
    s: Q | R,
};

type B = {
    s: Q & R,
};

type C = {
    s: A | B,
};

type D = {
    s: A & B,
};

type E = F | {
    e: 'e',
};

type F = {
    f: 'f',
};

type G = H | H
type H = F & F;
type V = Q & R;
type U = Q | R;

type Y<T> = {
    y0: $ElementType<X<boolean>, T>,
    y1: $ElementType<X<boolean>, P>,
    y2: $ElementType<Q, 's'>,
    y3: $ElementType<Q | R, 's'>,
    y4: $ElementType<Q & R, 's'>,
    y5: $ElementType<V, 's'>,
    y6: $ElementType<U, 's'>,
    y7: $ElementType<{s: 'S'}, 's'>,
    y8: $ElementType<A, 's'>,
    y9: $ElementType<B, 's'>,
    y10: $ElementType<A | B, 's'>,
    y11: $ElementType<A & B, 's'>,
    y12: $ElementType<C, 's'>,
    y13: $ElementType<D, 's'>,
    y14: $ElementType<E, 'f'>,
    y15: $ElementType<G, 'f'>,
};

type Z = $PropertyType<Y<'x'>, 'y0'>;

export {Z};
