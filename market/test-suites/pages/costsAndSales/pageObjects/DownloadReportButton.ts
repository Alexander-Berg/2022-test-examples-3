import {AbstractPO, resolve, select, Selector, selectorCreate} from 'spec/utils/po';

const {InnerToggler} = resolve('b2b/withDropdown');
const {Popup} = resolve('b2b/Popup/Base');

class PO extends AbstractPO {
    @Selector()
    get root() {
        return selectorCreate(select`[data-e2e="tableTitleRow"] ${InnerToggler}`);
    }

    @Selector()
    get popup() {
        return selectorCreate(select`${Popup}[class*=style-active] [data-e2e="downloadReportPopup"]`);
    }
}
export default new PO().makePO();
