type X = {
    x: string;
    y: boolean;
}

type Y = $Shape<X>;
type Z = $Shape<{x: string, y: string}>;

export type {Y, Z};
