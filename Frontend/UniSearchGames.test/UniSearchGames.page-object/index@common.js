const { ReactEntity } = require('../../../../../../vendors/hermione');
const { Layout } = require('../../../../UniSearch.components/Layout/Layout.test/Layout.page-object');

const UniSearchGames = new ReactEntity({ block: 'UniSearchGames' }).mix(Layout);

module.exports = {
    UniSearchGames,
};
