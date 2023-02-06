export const a = createResolver(
    ctx => Promise.resolve(42)
);

export const b = createResolver(
    ctx => Promise.resolve(42),
    {remote: true, foo: 'bar'}
);

const impl = ctx => Promise.resolve(42);

export const c = createResolver(impl, {foo: 'bar'});
