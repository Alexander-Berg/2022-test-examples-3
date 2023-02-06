// @flow

/**
 * Инструмент для очистки моков
 * Бывает так, что мок нужно сбросить, а после замокать еще раз
 *
 * Этот интерфейс позволяет запросто очистить старое значение и замокать новое внутри `jest.runCode`
 */

type CleanUpCallback = () => void;

/**
 * Этот объект окажется в глобальном скоупе раннера, значение будет сохраняться
 */
const mockCleanUps: {[key: string]: CleanUpCallback[]} = {};

export function cleanUpMocks(key: string) {
    while (Array.isArray(mockCleanUps[key]) && mockCleanUps[key].length > 0) {
        const cleanUpCallback = mockCleanUps[key].pop();
        cleanUpCallback();
    }
}

export function saveToCleanUp(key: string, ...callbacks: CleanUpCallback[]) {
    if (!Array.isArray(mockCleanUps[key])) {
        mockCleanUps[key] = [];
    }

    mockCleanUps[key].push(...callbacks);
}
