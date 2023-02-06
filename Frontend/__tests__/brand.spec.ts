import { set, get } from 'lodash';
import { IAdapterContext } from 'types/AdapterContext';
import brand from '../brand';

const { components } = brand;

describe('OrganicUgc/brand', () => {
    function makeContext() {
        const ctx: object = {};

        // prettier-ignore
        // tslint:disable-next-line max-line-length
        set(ctx, 'data.cgidata.args.data.0', 'H4sIAAAAAAAAE2WP0W6EIBBFvwbe3Ijubn3hQeua9DPoOlEigkXQdL++iGLY9OVyMsy9M2O4EUBRnaKSoCzdoCq9xlx4/Tz4aGtQTVCx88Pr1VfyYHScRVGx3X2RYHT84fn2bnH8CJW932kT7GmUvNursOoZWHuujvC3uU008R7deF73/0w//Yg9NyfRzgXKG2y1oL0x04zyEmVu3WZd18ukFqZBquGiLQZpuPn9ammavdigpKu5tk7Ndha248mLjSBZMmm2JK36ZosAySEZmIFOac5dM15BPNUItAchFB6flGT59lxvdzzZuaebJAsTFrCGH95Sr3vlDyoE1+j1AQAA');
        set(ctx, 'data.cgidata.args.checksum.0', '/3rVHJVpro22n5TkADDHn6/sxznL+35k+IDliIz7vwk=');
        set(ctx, 'data.doc.content.0.organic-ugc.gzipped', true);

        return ctx as IAdapterContext;
    }

    function makeContextFor1Page() {
        const ctx: object = {};

        // prettier-ignore
        // tslint:disable-next-line max-line-length
        set(ctx, 'data.cgidata.args.data.0', 'H4sIAAAAAAAAA32RTW+EIBCGfw1cGj9AR+XAQReb9N5Lj1tlVxq/Cri2/76ISVu7SS/PMO8MLwPI0Sr7+dRyLW/tOVDjZQr1Em0xGmSrGjXKCHeTsccOPDQc0iwlGYN9nbO8gBTP56s0nLhoO/7XZ15Mx7GW76rlBDLGgAFhADEkNHjpnuXbPMTYTNpyBBXKK0TppFupXURJuWmU3pRRr6p3Y28qPTkGq1HfibYfPwWrBrllIFAuNu3O81eT6wCB3YP0kiMRI0Y2VrVnhQRBxcmvhWfhlcyzetilPTwe9+29juWdIu6qzqKmqHCnp96sjL0MntQz9YyDo4+bYi/UoU9izwQvuuedtbNxt42idV3Df/96lX0zDZJflDb2C/ZwSJAfAgAA');
        set(ctx, 'data.cgidata.args.checksum.0', 'qccmG2GE7TlIFKmApXJT4mkyc4TVACDpOSGZz+1ahhA=');
        set(ctx, 'data.doc.content.0.organic-ugc.gzipped', true);
        set(ctx, 'data.doc.content.0.organic-ugc.type', 'surveys');

        return ctx as IAdapterContext;
    }

    describe('title', () => {
        const { title } = components;
        it('Корректно возвращает результат ф-ии', () => {
            const ctx = makeContext();

            // eslint-disable-next-line
            const result = title({} as any, ctx);
            expect(result).toBe('Комментатор – С какими продуктами сочетаются различные специи и пряности?');
        });
    });

    describe('favicon', () => {
        const { favicon } = components;
        it('Корректно возвращает результат ф-ии', () => {
            const ctx = makeContext();
            const result = favicon('', ctx);

            expect(result).toBe(
                'https://favicon.yandex.net/favicon/www.povarenok.ru?size=32&color=255%2C255%2C255%2C0&stub=1'
            );
        });
    });

    describe('cover', () => {
        const { cover } = components;

        it('Корректно возвращает результат ф-ии', () => {
            const ctx = makeContext();

            set(ctx, 'data.env.platform', 'phone');

            const turbojson = { ['organic-ugc']: {} };
            const { content } = cover(turbojson, ctx.data);

            expect(content).toMatchObject([
                { block: 'serp-organic' },
                { block: 'divider' },
                { block: 'yandex-comments' },
                { block: 'iframe-size-measurer' },
            ]);

            const yandexComments = content.find(item => item && item.block === 'yandex-comments');
            const coomentsParams = get(yandexComments, 'params') as ICmntParams;

            expect(coomentsParams.stats).toEqual({ push: 'push-value', reqid: 'reqid-value' });
            expect(coomentsParams.metrika).toEqual(['123', '456']);
        });

        it('Корректно возвращает результат ф-ии при pages=1', () => {
            const ctx = makeContextFor1Page();

            set(ctx, 'data.env.platform', 'phone');

            const turbojson = { ['organic-ugc']: { type: 'surveys' } };
            const { content } = cover(turbojson, ctx.data);

            expect(content).toMatchObject([
                {
                    block: 'serp-organic-surveys',
                    pagesCount: 1,
                    pages: [{}],
                    currentYear: new Date().getFullYear(),
                    reqid: '1569959519550532-YhTejpm0',
                    push: '',
                    skip: false,
                    apiKey: undefined,
                    scriptSrc: undefined,
                    welcomeMessages: undefined,
                    redirectUrl: 'https://yandex.ru/ugcpub/cabinet?main_tab=professions',
                },
            ]);
        });
    });
});
