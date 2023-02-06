const El = require('../Entity');
const elemsBase = require('../common');

const elems = { ...elemsBase };

elems.Header = new El({ block: 'Header' });
elems.Header.Form = new El('form');
elems.Header.Input = new El('[name="text"]');
elems.Header.ClearBtn = new El('[class*="header__clear"]');
elems.Header.Suggest = new El('form');
elems.Header.Suggest.Input = new El({ block: 'yandex-header__input' });
elems.Header.Suggest.InputClear = new El({ block: 'yandex-header__clear' });
elems.Header.Suggest.SearchButton = new El({ block: 'yandex-header__submit' });
elems.Header.Suggest.Popup = new El({ block: 'search-suggest__popup' });
elems.Header.Suggest.FirstItem = new El({ block: 'search-suggest__item' }).firstChild();

module.exports = elems;
