import { ProtobufJSContext } from '@yandex-int/apphost-lib';
import { ApphostRouter } from './router';

describe('ApphostRouter class', function() {
    const ctx = {} as unknown as ProtobufJSContext;
    it('выполняет добавленные хендлеры подряд', async function() {
        const router = new ApphostRouter();
        const log: number[] = [];
        router
            .use(() => {
                log.push(1);
            })
            .use(() => {
                log.push(2);
            })
            .use(() => {
                log.push(3);
            })
            .use(() => {
                log.push(4);
            });
        await router.handler(ctx);
        expect(log).toEqual([1, 2, 3, 4]);
    });
    it('заканчивает выполнение на первом хэндлере, который вернул истинное значение', async() => {
        const router = new ApphostRouter();
        const log: number[] = [];
        router
            .use(() => {
                log.push(1);
            })
            .use(() => {
                log.push(2);
                return true;
            })
            .use(() => {
                log.push(3);
            })
            .use(() => {
                log.push(4);
            });
        await router.handler(ctx);
        expect(log).toEqual([1, 2]);
    });
    it('вызывает событие finish завершения обработки', async() => {
        const router = new ApphostRouter();
        const finish1 = jest.fn();
        const finish2 = jest.fn();
        const finish3 = jest.fn();
        router
            .use(req => {
                req.once('finish', finish1);
            })
            .use(req => {
                req.once('finish', finish2);
            })
            .use(req => {
                req.once('finish', finish3);
            })
            .use(() => {
                expect(finish1).not.toHaveBeenCalled();
                expect(finish2).not.toHaveBeenCalled();
                expect(finish3).not.toHaveBeenCalled();
            });
        await router.handler(ctx);
        expect(finish1).toHaveBeenCalled();
        expect(finish2).toHaveBeenCalled();
        expect(finish3).toHaveBeenCalled();
        expect(finish1).toHaveBeenCalled();
    });
    it('вызывает событие finish если запрос был завершен хендлером', async() => {
        const router = new ApphostRouter();
        const finish1 = jest.fn();
        router
            .use(req => {
                req.once('finish', finish1);
            })
            .use(() => {
                expect(finish1).not.toHaveBeenCalled();
                return true;
            })
            .use(() => {
                throw new Error('This should have not been called');
            });
        await router.handler(ctx);
        expect(finish1).toHaveBeenCalled();
    });
    it('вызывает событие finish если запрос обработан error handler\'ом', async() => {
        const router = new ApphostRouter();
        const finish1 = jest.fn();
        router
            .use(req => {
                req.once('finish', finish1);
            })
            .use(() => {
                expect(finish1).not.toHaveBeenCalled();
                throw new Error('test');
            })
            .error(() => {
                expect(finish1).not.toHaveBeenCalled();
                return true;
            })
            .use(() => {
                throw new Error('This should have not been called');
            });
        await router.handler(ctx);
        expect(finish1).toHaveBeenCalled();
    });
    it('выбирает нужный error handler', async() => {
        const router = new ApphostRouter();
        const log: string[] = [];
        router
            .use(() => {
                log.push('ok1');
            })
            .error(() => {
                log.push('e1');
            })
            .use(() => {
                log.push('ok2');
                throw new Error('test');
            })
            .error(() => {
                log.push('e2');
            });
        await router.handler(ctx);
        expect(log).toEqual(['ok1', 'ok2', 'e2']);
    });
    it('продолжает использовать or error handler\'ы, пока ошибка не обработана', async() => {
        const router = new ApphostRouter();
        const log: string[] = [];
        router
            .use(() => {
                log.push('ok1');
                throw new Error('test');
            })
            .error((req, ctx, e) => {
                log.push('e1');
                throw e;
            })
            .use(() => {
                log.push('ok2');
            })
            .error(() => {
                log.push('e2');
            });
        await router.handler(ctx);
        expect(log).toEqual(['ok1', 'e1', 'e2']);
    });
    it('возвращается к обычным хендлерам после обработки ошибки', async() => {
        const router = new ApphostRouter();
        const log: string[] = [];
        const e1 = new Error('test');
        const e2 = new Error('test2');
        router
            .use(() => {
                log.push('ok1');
                throw e1;
            })
            .error((req, ctx, e) => {
                expect(e).toEqual(e1);
                log.push('e1');
            })
            .use(() => {
                log.push('ok2');
                throw e2;
            })
            .error((req, ctx, e) => {
                expect(e).toEqual(e2);
                log.push('e2');
            })
            .use(() => {
                log.push('ok3');
            });
        await router.handler(ctx);
        expect(log).toEqual(['ok1', 'e1', 'ok2', 'e2', 'ok3']);
    });
    it('не возвращается к обычным хендлерам, если при обработке ошибки выполнение запроса завершено', async() => {
        const router = new ApphostRouter();
        const log: string[] = [];
        router
            .use(() => {
                log.push('ok1');
                throw new Error('test');
            })
            .error(() => {
                log.push('e1');
                return true;
            })
            .use(() => {
                log.push('ok2');
            })
            .error(() => {
                log.push('e2');
            });
        await router.handler(ctx);
        expect(log).toEqual(['ok1', 'e1']);
    });
    it('реджектит промис, если ошибка из хендлера не была обработана', () => {
        const router = new ApphostRouter();
        const e = new Error('test');
        router
            .use(() => {
                throw e;
            });
        return expect(router.handler(ctx)).rejects.toEqual(e);
    });
    it('реджектит промис, если error handler кинул другую ошибку', async() => {
        const router = new ApphostRouter();
        const e = new Error('test');
        const e2 = new Error('handler error');
        const handler2 = jest.fn();
        const lastUse = jest.fn();
        router
            .use(() => {
                throw e;
            })
            .error(() => {
                throw e2;
            })
            .error(handler2)
            .use(lastUse);
        await expect(router.handler(ctx)).rejects.toEqual(e2);
        expect(e2.message).toContain('test');
        expect(handler2).not.toHaveBeenCalled();
        expect(lastUse).not.toHaveBeenCalled();
    });
});
