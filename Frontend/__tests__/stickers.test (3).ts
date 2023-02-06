import { updatePacks, selectPack, stickersReducer } from '../stickers';

const createStickerPack = (id) => ({
    id,
    title: `pack-${id}`,
    description: `pack-desc-${id}`,
    stickers: [],
});

describe('Stickers reducer', () => {
    describe('set current sticker pack', () => {
        it('returns new state with updated current sticker pack', () => {
            const initialState = {
                packs: {},
                packsRequested: false,
                activePackId: '',
            };

            const newState = stickersReducer(initialState, selectPack('1'));

            expect(newState).not.toBe(initialState);
            expect(newState).toEqual({
                packs: {},
                packsRequested: false,
                activePackId: '1',
            });
        });
    });

    describe('update sticker pack', () => {
        it('returns new state with new packs', () => {
            const initialState = {
                packs: { a: createStickerPack('a') },
                packsRequested: false,
                activePackId: '',
            };

            const newState = stickersReducer(initialState, updatePacks([
                createStickerPack('b'),
            ]));

            expect(newState).not.toBe(initialState);
            expect(newState).toEqual({
                packs: {
                    a: createStickerPack('a'),
                    b: createStickerPack('b'),
                },
                packsRequested: false,
                activePackId: '',
            });
        });
    });
});
