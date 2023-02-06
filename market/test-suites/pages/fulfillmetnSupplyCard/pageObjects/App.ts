import {AbstractPO, Selector, selectorCreate} from 'spec/utils/po';

class PO extends AbstractPO {
    @Selector()
    get root() {
        return selectorCreate('.p-page__content');
    }
}
export default new PO().makePO();
