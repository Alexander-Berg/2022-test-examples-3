const { ReactEntity } = require('../../../../../../vendors/hermione');

module.exports = {
    UniSearchFilter: new ReactEntity({ block: 'UniSearchFilter' }),
    UniSearchFilterMain: new ReactEntity({ block: 'UniSearchFilter', mod: 'main' }),
    UniSearchPopupFilterTitle: new ReactEntity({ block: 'UniSearchFilter', elem: 'PopupFilterTitle' }),
    UniSearchPopupCancelButton: new ReactEntity({ block: 'UniSearchFilter', elem: 'PopupCancelButton' }),
};
