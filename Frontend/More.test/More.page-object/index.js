const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.More = new ReactEntity({ block: 'UniSearchMore' });
elems.More.Button = new ReactEntity({ block: 'UniSearchMore', elem: 'Button' });
elems.More.Spin = new ReactEntity({ block: 'UniSearchMore', elem: 'Spin' });

module.exports = elems;
