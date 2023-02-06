const { ReactEntity } = require('../../../../../../vendors/hermione');
const { scroller } = require('../../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');
const { companyCard } = require('../../../../../../components/CompanyCard/CompanyCard.test/CompanyCard.page-object');
const { companyCardCompact } = require('../../../../../../components/CompanyCardCompact/CompanyCardCompact.test/CompanyCardCompact.page-object');

const elems = {};

elems.similarCompanies = new ReactEntity({ block: 'SimilarCompanies' });
elems.similarCompanies.title = new ReactEntity({ block: 'OneOrgSection', elem: 'Title' });
elems.similarCompanies.scroller = scroller.copy();
elems.similarCompanies.scroller.companyCard = companyCard.copy();
elems.similarCompanies.scroller.companyCard.rating = companyCardCompact.rating.copy();
elems.similarCompanies.scroller.firstItem = elems.similarCompanies.scroller.companyCard.copy().nthType(1);
elems.similarCompanies.scroller.secondItem = elems.similarCompanies.scroller.companyCard.copy().nthType(2);
elems.similarCompanies.scroller.thirdItem = elems.similarCompanies.scroller.companyCard.copy().nthType(3);
elems.similarCompanies.images = new ReactEntity({ block: 'Topic' })
    .descendant(new ReactEntity({ block: 'Thumb', elem: 'Image' }));

module.exports = elems;
