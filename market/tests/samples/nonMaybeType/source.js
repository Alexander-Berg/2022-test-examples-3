type X = ?string;

type Y = $NonMaybeType<X>;
type Z = $NonMaybeType<?number>;

export type {Y, Z};
