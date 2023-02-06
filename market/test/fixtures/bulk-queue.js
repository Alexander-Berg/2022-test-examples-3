export const e = createResolver(
    ctx => Promise.resolve(42),
    {remote: true, foo: 'bar', bulkQueue: 'slowpoke'}
);
