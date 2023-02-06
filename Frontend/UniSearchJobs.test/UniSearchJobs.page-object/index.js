const { ReactEntity } = require('../../../../../../vendors/hermione');
const { Layout } = require('../../../../UniSearch.components/Layout/Layout.test/Layout.page-object');
const { More } = require('../../../../UniSearch.components/More/More.test/More.page-object');
const { List } = require('../../../../UniSearch.components/List/List.test/List.page-object');

const UniSearchJobs = new ReactEntity({ block: 'UniSearchJobs' }).mix(Layout);

UniSearchJobs.Content.List = List.copy();
UniSearchJobs.Footer.More = More.copy();

UniSearchJobs.Content.List.Item.Container = new ReactEntity({ block: 'UniSearchJobsItem', elem: 'Container' });
UniSearchJobs.Content.List.Item.Links = new ReactEntity({ block: 'UniSearchJobsItem', elem: 'Links' });
UniSearchJobs.Content.List.Item.Links.Link = new ReactEntity({ block: 'Link' });

module.exports = {
    UniSearchJobs,
};
