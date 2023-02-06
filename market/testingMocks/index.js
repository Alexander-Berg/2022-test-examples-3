const {NormalModuleReplacementPlugin} = require('webpack');
const path = require('path');

const mockPaths = [
    [/@yandex-market\/mandrel\/bcm\/base/, 'isomorphic/testingMocks/mandrel/bcmBase.ts'],
    [/@yandex-market\/mandrel\/bcm\/abstract/, 'isomorphic/testingMocks/mandrel/bcmAbstract.ts'],
    [/@yandex-market\/mandrel\/context/, 'isomorphic/testingMocks/mandrel/context.ts'],
    [/@yandex-market\/b2b-core\/shared\/bcm/, 'isomorphic/testingMocks/shared/bcmCocon.ts'],
    [/^@yandex-market\/b2b-core\/shared\/utils$/, 'isomorphic/testingMocks/mockB2bCoreUtils.ts'],
    [/@yandex-market\/mandrel\/resolvers\/user-info\/usersInfo/, 'isomorphic/testingMocks/mandrel/usersInfo.ts'],
    [/@yandex-market\/mandrel\/resolver/, 'isomorphic/testingMocks/mandrel/resolver.ts'],
    [/configs\/current\/node/, 'isomorphic/testingMocks/mockCurrentNodeConfig.ts'],
    [/@yandex-market\/b2b-core[/]shared[/]configs/, 'isomorphic/testingMocks/mockB2bCoreConfigs.ts'],
];

module.exports = {
    testMockExternalsWhitelist: mockPaths.map(([pathPattern]) => pathPattern),
    testMockPlugins: mockPaths.map(
        ([pathPattern, replaceBy]) =>
            new NormalModuleReplacementPlugin(pathPattern, path.resolve(process.cwd(), replaceBy)),
    ),
};
