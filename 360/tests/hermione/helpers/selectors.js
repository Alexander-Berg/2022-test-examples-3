const PageObjects = require('../page-objects/public');

const wowGridGroup = (n) => `${PageObjects.wowGrid.group()}:nth-of-type(${n})`;
const wowGridItem = (n) => `${PageObjects.wowGrid.item()}:nth-of-type(${n})`;
const wowGridItemByTitle = (title) => `${PageObjects.wowGrid.item()}[title="${title}"]`;
const listingItem = (n) => `${PageObjects.listing.listingItem()}:nth-of-type(${n})`;

module.exports = {
    wowGridGroup,
    wowGridItem,
    wowGridItemByTitle,
    listingItem,
};
