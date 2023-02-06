export default (data: Record<string, any>) => ({
    className: 'string',
    host: 'string',
    version: 'string',
    executingTime: 'string',
    actions: 'string',
    ...data,
});
