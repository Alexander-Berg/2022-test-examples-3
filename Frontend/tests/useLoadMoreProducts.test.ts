import { getNextPage } from '../hooks/useLoadMoreProducts';

describe('useLoadMoreProducts', () => {
    describe('getNextPage()', () => {
        it('Нет страницы в URL и загружена первая страница', () => {
            const page = getNextPage({
                itemsCount: 12,
                currentPage: 1,
                itemsPerPage: 12,
            });
            expect(page).toBe(2);
        });

        it('Нет страницы в URL и загружены первые две страницы', () => {
            const page = getNextPage({
                itemsCount: 24,
                currentPage: 1,
                itemsPerPage: 12,
            });
            expect(page).toBe(3);
        });

        it('Нет страницы в URL и на первую страницу пришло товаров меньше, чем ожидается', () => {
            const page = getNextPage({
                itemsCount: 10,
                currentPage: 1,
                itemsPerPage: 12,
            });
            expect(page).toBe(2);
        });

        it('Десятая страница в URL и загружены её товары', () => {
            const page = getNextPage({
                itemsCount: 12,
                currentPage: 10,
                itemsPerPage: 12,
            });
            expect(page).toBe(11);
        });

        it('Десятая страница в URL и загружены товары 10 и 11 страниц', () => {
            const page = getNextPage({
                itemsCount: 24,
                currentPage: 10,
                itemsPerPage: 12,
            });
            expect(page).toBe(12);
        });

        it('Десятая страница в URL и пришло товаров меньше, чем ожидается на странице', () => {
            const page = getNextPage({
                itemsCount: 10,
                currentPage: 10,
                itemsPerPage: 12,
            });
            expect(page).toBe(11);
        });
    });
});
