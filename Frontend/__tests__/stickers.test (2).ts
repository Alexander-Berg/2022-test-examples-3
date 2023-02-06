import { getStickerPacksIds } from '../stickers';

describe('StickersSelectors', () => {
    describe('#getStickerPacksIds', () => {
        it('Should return ids from flag if flag is exists', () => {
            // @ts-ignore
            global.window.flags = {
                sticker_packs: '18,154',
            };

            // @ts-ignore
            expect(getStickerPacksIds({})).toEqual(['18', '154']);
        });

        it('Should return ids from bucket if flag is not exists', () => {
            // @ts-ignore
            global.window.flags = {};

            const state = {
                buckets: {
                    sticker_packs: {
                        data: {
                            sticker_packs: ['52', '76'],
                        },
                    },
                },
            };

            // @ts-ignore
            expect(getStickerPacksIds(state)).toEqual(['52', '76']);
        });
    });
});
