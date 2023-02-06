import { render } from './oebs-tab.po';
import { getOebsAgreementMock } from '~/test/jest/mocks/data/oebs-agreement';

describe('Просмотр списка OEBS-согласований', () => {
    describe('Положительные', () => {
        it('Выводится только таблица со списком из 3 согласований', () => {
            const oebsTab = render({
                oebs: {
                    agreements: {
                        next: null,
                        previous: null,
                        results: [
                            getOebsAgreementMock(1, { id: 1 }),
                            getOebsAgreementMock(1, { id: 2 }),
                            getOebsAgreementMock(1, { id: 3 }),
                        ],
                    },
                },
            });

            expect(oebsTab.loadMoreBtn?.container).toBeUndefined();
            expect(oebsTab.tableBody?.container.children.length).toBe(3);
        });

        it('Выводится таблица и кнопка "Загрузить еще"', () => {
            const oebsTab = render({
                oebs: {
                    agreements: {
                        next: '/next/?cursor=67-1',
                        previous: null,
                        results: [
                            getOebsAgreementMock(1, { id: 1 }),
                            getOebsAgreementMock(1, { id: 2 }),
                            getOebsAgreementMock(1, { id: 3 }),
                            getOebsAgreementMock(1, { id: 4 }),
                            getOebsAgreementMock(1, { id: 5 }),
                            getOebsAgreementMock(1, { id: 6 }),
                            getOebsAgreementMock(1, { id: 7 }),
                            getOebsAgreementMock(1, { id: 8 }),
                            getOebsAgreementMock(1, { id: 9 }),
                            getOebsAgreementMock(1, { id: 10 }),
                        ],
                    },
                },
            });

            expect(oebsTab.loadMoreBtn?.container).toBeInTheDocument();
            expect(oebsTab.tableBody?.container.children.length).toBe(10);
        });
    });
});
