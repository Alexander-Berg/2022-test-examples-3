import photoGridHelper from '../../../components/helpers/helper-photo-grid';

const DEFAULT_ROW_SIZE = 150;
const DEFAULT_MARGIN = 8;

describe('photoGridHelper', () => {
    describe('.scaleSizesForJustifiedGrid', () => {
        it('Корректно отмасштабирует 4 одинаковых размера', () => {
            const result = photoGridHelper.scaleSizesForJustifiedGrid(
                [
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 }
                ],
                420, DEFAULT_ROW_SIZE, DEFAULT_MARGIN
            );
            expect(result.unfilled).toBe(null);
            expect(result.sizes).toEqual([
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 }
            ]);
        });

        it('Корректно отмасштабирует широкие горизонтальные размеры', () => {
            const result = photoGridHelper.scaleSizesForJustifiedGrid(
                [
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 },
                    { width: 1680, height: 600 }
                ],
                420, DEFAULT_ROW_SIZE, DEFAULT_MARGIN
            );
            expect(result.unfilled).toBe(null);
            expect(result.sizes).toEqual([
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 },
                { width: 420, height: 150 }
            ]);
        });

        it('Корректно отмасштабирует последний размер и вычислит оставшееся в последней строке место для одинаковых пропорций ', () => {
            const result = photoGridHelper.scaleSizesForJustifiedGrid(
                [
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 }
                ],
                420, DEFAULT_ROW_SIZE, DEFAULT_MARGIN
            );
            expect(result.unfilled).toEqual({ width: 206, height: 154.5 });
            expect(result.sizes).toEqual([
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 }
            ]);
        });

        it('Корректно отмасштабирует последний размер и вычислит оставшееся в последней строке место для разных пропорций ', () => {
            const result = photoGridHelper.scaleSizesForJustifiedGrid(
                [
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 },
                    { width: 2448, height: 3264 }
                ],
                420, DEFAULT_ROW_SIZE, DEFAULT_MARGIN
            );
            expect(result.unfilled).toEqual({ width: 299.5, height: 150 });
            expect(result.sizes).toEqual([
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 },
                { width: 112.5, height: 150 }
            ]);
        });

        it('Корректно отмасштабирует сетку с очень широким размером', () => {
            const result = photoGridHelper.scaleSizesForJustifiedGrid(
                [
                    { width: 3264, height: 2448 },
                    { width: 3264, height: 2448 },
                    { width: 1680, height: 300 }
                ],
                420, DEFAULT_ROW_SIZE, DEFAULT_MARGIN
            );
            expect(result.sizes).toEqual([
                { width: 206, height: 154.5 },
                { width: 206, height: 154.5 },
                { width: 420, height: 75 }
            ]);
        });
    });
});
