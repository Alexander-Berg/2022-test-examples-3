import { getButterfly } from './butterfly';

describe('butterfly', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let ctx: any = {};

    beforeEach(() => {
        ctx = {
            isYandexNet: () => false,
            isInternalAccount: () => false,
            request: {
                headers: {},
                cookies_parsed: {},
            },
        };
    });

    it('ничего не возвращает если внешняя сеть и нет логина под стафом', () => {
        expect(getButterfly(ctx)).toBeUndefined();
    });

    it('ничего не возвращает если в параметрах есть noMooa', () => {
        ctx.isYandexNet = () => true;
        ctx.request.params = {
            nomooa: ['1'],
        };

        expect(getButterfly(ctx)).toBeUndefined();

        ctx.isInternalAccount = () => true;
        expect(getButterfly(ctx)).toBeUndefined();

        ctx.isYandexNet = () => false;
        expect(getButterfly(ctx)).toBeUndefined();
    });

    it('возвращает строку для внутренней сети не в контексте гермионы', () => {
        ctx.isYandexNet = () => true;
        expect(typeof getButterfly(ctx)).toEqual('string');

        ctx.isInternalAccount = () => true;
        expect(typeof getButterfly(ctx)).toEqual('string');
    });

    it('возвращает строку для внешней сети и логина под стафом', () => {
        ctx.isInternalAccount = () => true;

        expect(typeof getButterfly(ctx)).toEqual('string');
    });
});
