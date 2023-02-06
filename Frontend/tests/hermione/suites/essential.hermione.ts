import assert from 'assert';
import { authorize } from '../auth';

describe('Общее', function() {
    beforeEach(async({ browser }) => {
        await authorize(browser);
    });

    it('Поиск по имени агенства', async({ browser }) => {
        await browser.url('/partner-office');

        await browser.$('.partner-selector__wrapper').waitForDisplayed({ timeout: 15000 });
        await browser.$('.partner-selector__wrapper').click();
        await browser.$('.partner-selector__input input').setValue('RoboAgency');
        await browser.$('=RoboAgency ъ').click();
        await browser.$('.agency-info').waitForDisplayed();

        const text = await browser.$('.agency-info').getText();
        assert(text.includes('RoboAgency'));
        assert(text.includes('666'));
    });
});
