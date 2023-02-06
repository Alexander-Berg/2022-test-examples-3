import { DrawerRoot } from '../DrawerRoot';

// Заготовка для будущих тестов
describe('Турбо-оверлей', () => {
    describe('Модуль создания html (DrawerView)', () => {
        it('Создает корректный html при иницилизации', () => {
            const drawerRoot = new DrawerRoot();
            expect(document.documentElement).toMatchSnapshot();

            expect(drawerRoot.isMultipage()).toStrictEqual(false);
        });
    });
});
