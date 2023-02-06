const { create, ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.OrgHeader = new ReactEntity({ block: 'OrgHeader' });
elems.OneOrgTabs = new ReactEntity({ block: 'one-org-tabs' });
elems.NoticeStripe = new ReactEntity({ block: 'NoticeStripe' });

module.exports = create(elems);
