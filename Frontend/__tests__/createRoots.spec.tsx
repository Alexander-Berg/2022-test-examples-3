import { createRoots } from '../createRoots';
import { ClassicRoot, DrawerRoot } from '../Views';

jest.mock('../Views');

describe('Турбо-оверлей', () => {
    describe('createRoots', () => {
        it('Создает только ClassicRoot при view-type="classic"', () => {
            const { singlepage, multipage } = createRoots('classic');

            expect(singlepage).toBeInstanceOf(ClassicRoot);
            expect(multipage).toBeInstanceOf(ClassicRoot);
            expect(singlepage).toStrictEqual(multipage);
        });

        it('Создает DrawerRoot для singlepage и ClassicRoot для multipage при view-type="drawer"', () => {
            const { singlepage, multipage } = createRoots('drawer');

            expect(singlepage).toBeInstanceOf(DrawerRoot);
            expect(multipage).toBeInstanceOf(ClassicRoot);
        });

        it('Создает только ClassicRoot при отсутствии view-type', () => {
            const { singlepage, multipage } = createRoots('classic');

            expect(singlepage).toBeInstanceOf(ClassicRoot);
            expect(multipage).toBeInstanceOf(ClassicRoot);
            expect(singlepage).toStrictEqual(multipage);
        });
    });
});
