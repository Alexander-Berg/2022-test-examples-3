import {expect} from 'chai';

import { getServicePageUrl } from '../../../../tsum-front-core/configs/routes';
import { EXISTING_COMPONENT_DATA, EXISTING_SERVICE_DATA } from './constants';

import ServiceInfoPO from '../../src/features/market-map/containers/ServiceInfo/__pageObject'
import ServiceInfoTabsPO from '../../src/features/market-map/containers/ServiceInfoTabs/__pageObject'
import CriticalMembersPO from '../../src/features/market-map/containers/CriticalMembers/__pageObject'
import ContactsPO from '../../src/features/market-map/containers/Contacts/__pageObject'
import BreadcrumbsPO from '../../src/features/market-map/containers/Breadcrumbs/__pageObject'
import SupportChatPO from '../../src/features/market-map/containers/SupportChat/__pageObject'
import DescriptionPO from '../../src/features/market-map/components/Description/__pageObject'
import ServicesTablePO from '../../src/features/market-map/containers/ServicesTable/__pageObject';
import InfoLinksPO from '../../src/features/market-map/containers/InfoLinks/__pageObject';
import MembersPO from '../../src/features/market-map/containers/Members/__pageObject';
import SearchBarPO from '../../src/features/market-map/components/ServicesSearchBar/__pageObject';

describe('Страница сервиса', () => {
    it('Отображение основных элементов', async function () {
        const {slug} = EXISTING_COMPONENT_DATA;
        const existingComponentPageUrl = getServicePageUrl(slug);
        const serviceInfo = new ServiceInfoPO(this.browser);
        const serviceInfoTabs = new ServiceInfoTabsPO(this.browser);
        const criticalMembers = new CriticalMembersPO(this.browser);
        const contacts = new ContactsPO(this.browser);
        const breadcrumbs = new BreadcrumbsPO(this.browser);
        const supportChat = new SupportChatPO(this.browser);

        await this.browser.loginAndRedirect(existingComponentPageUrl);
        await serviceInfo.waitForVisible();

        const serviceInfoTabsVisible = await serviceInfoTabs.isVisible();
        const criticalMembersVisible = await criticalMembers.isVisible();
        const contactsVisible = await contacts.isVisible();
        const breadcrumbsVisible = await breadcrumbs.isVisible();
        const supportChatVisible = await supportChat.isVisible();

        expect(serviceInfoTabsVisible).to.equal(true);
        expect(criticalMembersVisible).to.equal(true);
        expect(contactsVisible).to.equal(true);
        expect(breadcrumbsVisible).to.equal(true);
        expect(supportChatVisible).to.equal(true);
    });

    it('Открытие вкладки О Сервисе по дефолту', async function () {
        const {slug} = EXISTING_SERVICE_DATA;
        const existingServicePageUrl = getServicePageUrl(slug);
        const serviceInfo = new ServiceInfoPO(this.browser);
        const description = new DescriptionPO(this.browser);
        const servicesTable = new ServicesTablePO(this.browser);

        await this.browser.loginAndRedirect(existingServicePageUrl);
        await serviceInfo.waitForVisible();

        const isAboutTabVisible = await description.isVisible();
        const isComponentsTabVisible = await servicesTable.isVisible();

        expect(isAboutTabVisible).to.equal(true);
        expect(isComponentsTabVisible).to.equal(false);
    });

    it('Открытие вкладки с переданным параметром', async function () {
        const {slug} = EXISTING_SERVICE_DATA;
        const existingServicePageUrl = getServicePageUrl(slug);
        const serviceInfo = new ServiceInfoPO(this.browser);
        const description = new DescriptionPO(this.browser);
        const servicesTable = new ServicesTablePO(this.browser);
        const url = `${existingServicePageUrl}?tab=components`

        await this.browser.loginAndRedirect(url);
        await serviceInfo.waitForVisible();

        const isAboutTabVisible = await description.isVisible();
        const isComponentsTabVisible = await servicesTable.isVisible();

        expect(isAboutTabVisible).to.equal(false);
        expect(isComponentsTabVisible).to.equal(true);
    });

    it('Содержимое вкладки О сервисе', async function () {
        const {slug} = EXISTING_COMPONENT_DATA;
        const existingComponentPageUrl = getServicePageUrl(slug);

        const description = new DescriptionPO(this.browser);
        const infoLinks = new InfoLinksPO(this.browser);
        const members = new MembersPO(this.browser);
        const serviceInfo = new ServiceInfoPO(this.browser);

        await this.browser.loginAndRedirect(existingComponentPageUrl);
        await serviceInfo.waitForVisible();

        const isDescriptionVisible = await description.isVisible();
        const isInfoLinksVisible = await infoLinks.isVisible();
        const isMembersVisible = await members.isVisible();

        expect(isDescriptionVisible).to.equal(true);
        expect(isInfoLinksVisible).to.equal(true);
        expect(isMembersVisible).to.equal(true);
    });

    it.skip('Переход на главную страницу по нажатию линки в breadcrumbs', async function () {
        const {slug} = EXISTING_COMPONENT_DATA;
        const existingComponentPageUrl = getServicePageUrl(slug);
        const breadcrumbs = new BreadcrumbsPO(this.browser);
        const serviceInfo = new ServiceInfoPO(this.browser);
        const searchBar = new SearchBarPO(this.browser);

        await this.browser.loginAndRedirect(existingComponentPageUrl);
        await serviceInfo.waitForVisible();

        const rootBreadcrumbLink = await breadcrumbs.getRootBreadcrumbLink();
        await rootBreadcrumbLink.click();

        await searchBar.waitForVisible();

        const searchBarExists = await searchBar.isVisible();

        expect(searchBarExists).to.equal(true);
    });
});
