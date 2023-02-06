const El = require('../Entity');

const elems = {};

elems.About = new El({ block: 'About' });
elems.About.Title = new El({ block: 'About', elem: 'Title' });

module.exports = elems;
