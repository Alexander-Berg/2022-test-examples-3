import {AbstractPO, Selector, selectorCreate} from 'spec/utils/po';

class PO extends AbstractPO {
    @Selector()
    get root() {
        return selectorCreate('.p-layout');
    }

    @Selector()
    get header() {
        return selectorCreate('.p-layout__header-wrapper');
    }

    @Selector()
    get accountBlock() {
        return selectorCreate('[data-e2e="accountWidget"]');
    }
}
export default new PO().makePO();
