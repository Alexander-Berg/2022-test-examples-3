import { createBarchartData } from '../transform-stat-to-chart-data';

describe('createBarchartData', () => {
    it('корректно конвертирует данные в формат, нужный для отрисовки гистограммы', () => {
        const chartLabel = 'Оценки, которые вы поставили запросам';
        const statistics = {
            '1': 65,
            '2': 59,
            '3': 80,
            '4': 81,
            '5': 56,
            '6': 55,
        };

        const result = createBarchartData(statistics, chartLabel);

        expect(result).toStrictEqual({
            labels: ['1', '2', '3', '4', '5', '6'],
            datasets: [
                {
                    label: chartLabel,
                    data: [65, 59, 80, 81, 56, 55],
                },
            ],
        });
    });
});
