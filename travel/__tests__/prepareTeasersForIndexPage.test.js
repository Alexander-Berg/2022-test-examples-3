jest.disableAutomock();

import prepareTeasersForIndexPage from '../prepareTeasersForIndexPage';

describe('prepareTeasersForIndexPage', () => {
    const teasersSample = [
        {id: 1, importance: 13},
        {id: 2, importance: 24},
        {id: 3, importance: 15},
    ];

    const types = ['attention', 'special', 'normal'];

    it('возвращает по одному тизеру каждого типа', () => {
        const teasers = {
            ahtung: teasersSample,
            special: [...teasersSample, {id: 4, importance: 24}],
            normal: teasersSample,
        };

        const expected = [
            types.map(type => ({type, ...teasersSample[1]})),
            types.map(type => ({
                type,
                ...(type === 'special'
                    ? {id: 4, importance: 24}
                    : teasersSample[1]),
            })),
        ];

        // Проверка, что случайный тизер - один из ожидаемых
        for (let i = 0; i < 10; i++) {
            expect(expected).toContainEqual(
                prepareTeasersForIndexPage(teasers),
            );
        }
    });
});
