const isDirtyBot = require.requireActual('../isDirtyBot').default;

describe('isDirtyBot', () => {
    it('для не ботов возвращаем false', () =>
        expect(isDirtyBot({})).toBe(false));

    it('для поисковых ботов возвращаем false', () =>
        expect(
            isDirtyBot({
                isBot: true,
                isSearchBot: true,
            }),
        ).toBe(false));

    it('для грязных ботов возвращаем true', () =>
        expect(
            isDirtyBot({
                isBot: true,
                isSearchBot: false,
            }),
        ).toBe(true));
});
