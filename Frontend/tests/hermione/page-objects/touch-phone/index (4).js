const El = require('../Entity');
const elems = require('../common');

elems.DrugFilterList = new El({ block: 'DrugFilterList' });
elems.DrugFilterList.CommonFilter = new El({ block: 'HorizontalScroll', elem: 'Item' }).firstChild();
elems.DrugFilterList.CountryFilter = new El({ block: 'HorizontalScroll', elem: 'Item' }).nthChild(2);
elems.DrugFilterList.ActiveFilter = new El({ block: 'DrugFilterList', elem: 'Item' }).mods({ active: true });
elems.DrugFilterList.ActiveFilter.CloseIcon = new El({ block: 'DrugFilterList', elem: 'ItemIcon' });

elems.Select = new El({ block: 'Select' }).mods({ open: true });
elems.Select.Content = new El({ block: 'Tray', elem: 'Content' });
elems.Select.Content.firstItem = new El({ block: 'Select', elem: 'Option' }).firstChild();
elems.Select.Content.secondItem = new El({ block: 'Select', elem: 'Option' }).nthChild(2);
elems.SelectCloseSpace = new El({ block: 'DrugList', elem: 'AcceptButton' });

module.exports = elems;
