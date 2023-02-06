function getActive(selector, activeSelector, number, multirow) {
    function onScreen(element) {
        const rect = element.getBoundingClientRect();
        return rect.left > 0 && rect.right < 1920 && rect.top > 0 && rect.bottom < 1080;
    }

    const elements = document.querySelectorAll(selector);
    const rcElements = Array.from(elements).filter(onScreen);

    const multirowNumberMap = [0, 2, 4, 6, 1, 3, 5, 7];
    const num = multirow ? multirowNumberMap[number - 1] : (number - 1);
    const rcElement = rcElements[num];

    if (!rcElement) {
        throw Error(`Не удалось найти элемент ${number}. Количество элементов: ${rcElements.length}`);
    }
    if (!rcElement.classList.contains(activeSelector)) {
        throw Error(`Выбранный элемент с номером ${number} не содержит класс активности ${activeSelector}. Текущие классы: '${rcElement.classList}'`);
    }
}

module.exports = function(selector, activeSelector, number, multirow = false) {
    return this.execute(getActive, selector, activeSelector, number, multirow);
};
