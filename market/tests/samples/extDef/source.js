type A = $$extDef<string[], {}>

type B = $$extDef<A[], {
    uniqueItems: true,
}>

type X = {
    a: $$extDef<A[], {
        additionalItems: {
            a: {
                a: 'string',
            },
        },
        maxItems: 10,
        minItems: 1,
        uniqueItems: false,
    }>,

    b: B,

    c: $$extDef<string, {
        maxLength: 1,
        minLength: 1,
        pattern: '/[a-z]+/i',
        format: 'email',
    }>,

    d: $$extDef<number, {
        multipleOf: 1,
        maximum: 10,
        exclusiveMaximum: 10,
        minimum: 1,
        exclusiveMinimum: 1,
    }>,

    e: $$extDef<string[], {
        minProperties: 1,
        maxProperties: 2,
    }>,

    f: $$extDef<$$extDef<{a: 'string'}, {maxProperties: 1}>[], {uniqueItems: false}>,

    g: $$extDef<'g', {
        title: 'lol',
        description: 'kek',
        lol: 'kek',
    }>,

    h: $$extDef<'h', {
        title: 'lol',
        description: 'kek',
    }> | 'kek',

    i: $$extDef<'i' | 'I', {
        title: 'i',
    }>,

    j: $$extDef<'j' & 'J', {
        title: 'j',
    }>,

    k: $$extDef<$ElementType<{k: 'k1'} | {k: 'k2'}, 'k'>, {
        title: 'k',
    }>,

    l: $$extDef<$PropertyType<{l: 'l1'} & {l: 'l2'}, 'l'>, {
        title: 'l',
    }>,
};

export type {X};
