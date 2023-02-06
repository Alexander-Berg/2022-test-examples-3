import {AbstractPO, resolve, Selector, selectorCreate, SelectorWithParams} from 'spec/utils/po';

const {UserMenuTrigger} = resolve('~/components/UserMenu/UserMenuTrigger');
const {UserDropdown} = resolve('~/components/UserMenu/UserDropdown');

class PO extends AbstractPO {
    @Selector()
    get root() {
        return selectorCreate('.p-layout');
    }

    @Selector()
    get avatar() {
        return selectorCreate(this.by`${UserDropdown} ${UserMenuTrigger}`);
    }

    @Selector()
    get crm() {
        return selectorCreate('[data-at="crm-link"]');
    }

    @Selector()
    get shopTable() {
        return selectorCreate('.dt');
    }

    @Selector()
    get crmCell() {
        return selectorCreate('[data-e2e-i18n-key="pages.manager-partner-list:table.campaignId.links.crm"]');
    }

    @SelectorWithParams()
    balanceCellByNth(nth: number) {
        return selectorCreate(this.by`.dt tr:nth-child(${nth}) td:last-child`);
    }
}
export default new PO().makePO();
