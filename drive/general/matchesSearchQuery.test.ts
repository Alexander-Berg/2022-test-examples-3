import SearchWallets from './index';

describe('matchesSearchQuery method', function () {
    it('should not find a match, "болотные ежики" & "ООО "Ежики на болоте""', function () {
        const match = SearchWallets.matchesSearchQuery("ООО \"Ежики на болоте\"", "болотные ежики");
        expect(match).toEqual(undefined);
    });

    it('should not find a match, "медикатур" & "ООО Туризм', function () {
        const match = SearchWallets.matchesSearchQuery("медикатур", "ООО Туризм");
        expect(match).toEqual(undefined);
    });

    it('should find a match, "ежики" & "ООО "Ежики на болоте""', function () {
        const match = SearchWallets.matchesSearchQuery("ООО \"Ежики на болоте\"", "ежики");
        expect(match).toEqual("ООО \"Ежики на болоте\"");
    });

    it('should find a match, "ооо ежики на болоте" & "ООО "Ежики на болоте""', function () {
        const match = SearchWallets.matchesSearchQuery("ООО \"Ежики на болоте\"", "ооо ежики на болоте");
        expect(match).toEqual("ООО \"Ежики на болоте\"");
    });

    it('should find a match, "ООО "Ежики на болоте"" & "ООО "Ежики на болоте""', function () {
        const match = SearchWallets.matchesSearchQuery("ООО \"Ежики на болоте\"", "ООО \"Ежики на болоте\"");
        expect(match).toEqual("ООО \"Ежики на болоте\"");
    });

    it('should not find a match, "ежики на болоте" & "ООО Ежики', function () {
        const match = SearchWallets.matchesSearchQuery("ежики на болоте", "ООО Ежики");
        expect(match).toEqual(undefined);
    });
});
