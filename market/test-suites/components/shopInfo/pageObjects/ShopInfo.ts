import {AbstractPO, resolve, select, Selector, selectorCreate} from 'spec/utils/po';

const {ShopInfo} = resolve('~/containers/Sidebar/components/ShopInfo');
const {Popup} = resolve('~/components/Popup/Base');

class PO extends AbstractPO {
    @Selector()
    get root() {
        return selectorCreate(select`${ShopInfo}`);
    }

    @Selector()
    get trigger() {
        return selectorCreate(select`${ShopInfo} > div:first-child`);
    }

    @Selector()
    get popupContent() {
        return selectorCreate(select`${ShopInfo} ${Popup} > div`);
    }
}
export default new PO().makePO();
