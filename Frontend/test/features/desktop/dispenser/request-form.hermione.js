const PO = require('./PO');

describe('Dispenser: Форма заказа оборудования', function() {
    it('Внешний вид формы', function() {
        return this.browser
            .openIntranetPage({
                pathname: '/hardware/5390/edit',
            }, { user: 'robot-abc-002' })
            .waitForVisible(PO.resourcesForm.serviceSuggest.chosen(), 5000)

            // внешний вид формы [form]
            .assertView('form', PO.resourcesForm())

            // внешний вид провайдера [provider]
            .assertView('provider', PO.resourcesForm.provider())

            // навести курсор мыши на первую строку с ресурсом
            .moveToObject(PO.resourcesForm.row())

            // внешний вид строки при наведении [row-hovered]
            .assertView('row-hovered', PO.resourcesForm.row());
    });
});
