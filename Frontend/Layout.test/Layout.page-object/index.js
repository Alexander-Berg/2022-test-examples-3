const { ReactEntity } = require('../../../../../../vendors/hermione');
const { Title } = require('../../../../UniSearch.components/Title/Title.test/Title.page-object');

const elems = {};

elems.Layout = new ReactEntity({ block: 'UniSearchLayout ' });
elems.Layout.Header = new ReactEntity({ block: 'UniSearchLayout', elem: 'Header' });
elems.Layout.Header.Title = Title.copy();
elems.Layout.Header.FilterScroller = new ReactEntity({ block: 'UniSearchLayout', elem: 'FilterScroller' });
elems.Layout.Content = new ReactEntity({ block: 'UniSearchLayout', elem: 'Content' });
elems.Layout.Shadow = new ReactEntity({ block: 'UniSearchShadow' });
elems.Layout.Footer = new ReactEntity({ block: 'UniSearchLayout', elem: 'Footer' });

module.exports = elems;
