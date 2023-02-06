import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {Keys} from '../../constants';

const PAGE_URL = '/dataimport/ticket$b2bLead/import$b2bCommercialDepartmentLeads/form';

/**
 * Проверяем, что:
 * После открытия автокомплита со списком доступных для выбора очередей
 * в импорте лидов КД и 12-ти нажатий на клавиатуре на стрелку вниз,
 * фокус в автокомплите сместится на 13-ый элемент
 */
describe(`ocrm-1630: Фокус в саджесте смещается при переключении стрелками`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('При 12-кратном нажатии на стрелку вниз список очередей скроллится до 13-ого элемента', async function() {
        const service = new ContentWithLabel(this.browser, 'body', '[data-ow-test-attribute-container="service"]');

        await service.isDisplayed();
        const input = await service.elem('input');

        await input.click();

        const autocompleteMenuList = await this.browser.$('#ow-popper-portal ul');

        const autocompleteMenuListIsDisplayed = await autocompleteMenuList.waitForDisplayed();

        expect(autocompleteMenuListIsDisplayed).to.equal(true, 'Список очередей не появился');

        const autocompleteMenuThirteenElement = await autocompleteMenuList.$(`li:nth-child(13)`);
        const autocompleteMenuThirteenElementParent = await autocompleteMenuThirteenElement.parentElement();
        const autocompleteMenuThirteenElementParentHeight = await autocompleteMenuThirteenElementParent.getSize(
            'height'
        );
        const autocompleteMenuThirteenElementParentLocation = await autocompleteMenuThirteenElementParent.getLocation(
            'y'
        );

        const autocompleteMenuElevenElementLocation = await autocompleteMenuThirteenElement.getLocation('y');

        const autocompleteMenuVisibilityArea =
            Number(autocompleteMenuThirteenElementParentLocation) + Number(autocompleteMenuThirteenElementParentHeight);
        const elevenElementIsInvisible = Number(autocompleteMenuElevenElementLocation) > autocompleteMenuVisibilityArea;

        expect(elevenElementIsInvisible).to.equal(true, '13-ый элемент находится в зоне видимости до скролла');

        await input.addValue(Array(12).fill(Keys.DOWN));

        const autocompleteMenuElevenElementNewLocation = await autocompleteMenuThirteenElement.getLocation('y');

        const thirteenElementIsVisible =
            Number(autocompleteMenuElevenElementNewLocation) < autocompleteMenuVisibilityArea;

        expect(thirteenElementIsVisible).to.equal(true, 'Список очередей не проскроллился до 13-ого элемента');
    });
});
