import 'hermione';
import {expect} from 'chai';

import CustomSelectionHead from '../../../src/components/jmf/CustomSelectionHead/__pageObject__';
import {login} from '../../helpers';
import {STATUSES} from './config';

STATUSES.forEach(({description, url, expectedStatusText}) => {
    describe('ocrm-889: Статусы выборок', () => {
        beforeEach(function() {
            return login(url, this);
        });

        // id: 'ocrm-889',
        // issue: 'OCRM-6051',
        it(`${description} Должен отображаться на странице ${url}`, async function() {
            const cardCustomHeader = new CustomSelectionHead(this.browser);

            await cardCustomHeader.isDisplayed();

            const statusMarkerText = await cardCustomHeader.getStatusMarkerText();

            expect(statusMarkerText, 'Текст статуса заголовка некорректный').to.equal(expectedStatusText);
        });
    });
});
