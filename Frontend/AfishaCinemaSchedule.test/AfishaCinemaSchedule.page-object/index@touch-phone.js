const { Entity } = require('../../../../../../vendors/hermione');
const { serpTitle, showcase, button2 } = require('../../../../../../../hermione/page-objects/common/blocks');
const { companiesComposite } = require('../../../../../../../hermione/page-objects/touch-phone');

const elems = {};

const afishaCinema = new Entity({ block: 'composite' }).mods({ 't-mod': 'afisha-cinema' });
afishaCinema.title = serpTitle.copy();
afishaCinema.showcase = showcase.copy();
afishaCinema.schedule = new Entity({ block: 'schedule' });
afishaCinema.ticketsButton = new Entity({ block: 'buy-tickets-button' }).descendant(button2);

elems.companiesComposite = companiesComposite.copy();
elems.afishaCinema = afishaCinema.copy();
elems.bcardSideBlock = new Entity({ block: 'side-block-bcard' });
elems.bcardSideBlock.afisha = elems.afishaCinema.copy();

module.exports = elems;
