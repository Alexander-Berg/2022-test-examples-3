const El = require('../Entity');

const elems = {};

elems.About = new El({ block: 'About' });
elems.About.Title = new El({ block: 'About', elem: 'Title' });
elems.Base = new El({ block: 'base-root' });
elems.Base.Title = new El({ block: 'base-root', elems: 'h2' });

module.exports = elems;
