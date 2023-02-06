const {NormalModuleReplacementPlugin} = require('webpack');
const path = require('path');

const mockPaths = [
    [/@yandex-market\/mandrel\/bcm\/base/, 'isomorphic/testingMocks/mandrel/bcmBase.ts'],
    [/@yandex-market\/mandrel\/bcm\/abstract/, 'isomorphic/testingMocks/mandrel/bcmAbstract.ts'],
    [/@yandex-market\/mandrel\/context/, 'isomorphic/testingMocks/mandrel/context.ts'],
    [/shared\/bcm\/cocon/, 'isomorphic/testingMocks/shared/bcmCocon.ts'],
    [/@yandex-market\/mandrel\/resolvers\/user-info\/usersInfo/, 'isomorphic/testingMocks/mandrel/usersInfo.ts'],
    [/@yandex-market\/mandrel\/resolver/, 'isomorphic/testingMocks/mandrel/resolver.ts'],
];

module.exports = {
    testMockExternalsWhitelist: mockPaths.map(([pathPattern]) => pathPattern),
    testMockPlugins: mockPaths.map(
        ([pathPattern, replaceBy]) =>
            new NormalModuleReplacementPlugin(pathPattern, path.resolve(process.cwd(), replaceBy)),
    ),
};
