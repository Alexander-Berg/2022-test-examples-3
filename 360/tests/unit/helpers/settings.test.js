import { preprocessSettings } from '../../../components/helpers/settings';

describe('settingsHelper', () => {
    describe('preprocessSettings', () => {
        it('тип отображения по умолчанию на десктопах', () => {
            const data = preprocessSettings({}, false);
            expect(data.view).toBe('icons');
        });

        it('тип отображения по умолчанию на смартфонах', () => {
            const data = preprocessSettings({}, true);
            expect(data.view).toBe('list');
        });

        it('не меняет ранее установленный тип отображения на десктопах', () => {
            const data = preprocessSettings({ view: 'tile' });
            expect(data.view).toBe('tile');
        });

        it('не меняет ранее установленный тип отображения на смартфонах', () => {
            const data = preprocessSettings({ view: 'tile' }, true);
            expect(data.view).toBe('tile');
        });
    });
});
