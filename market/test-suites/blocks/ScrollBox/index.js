import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок ScrollBox.
 * @param {PageObject.ScrollBox} scrollBox
 */
export default makeSuite('Компонент ScrollBox.', {
    feature: 'ScrollBox',
    environment: 'kadavr',
    story: {
        'При нажатии стрелок навигации': {
            'корректно прокручиваются элементы': makeCase({
                // Нужно создать в пальме отдельные кейсы для компонентов из levitan-gui
                id: 'marketfront-2370',
                issue: 'MARKETVERSTKA-28490',
                async test() {
                    let isFirstItemDisplayed;

                    isFirstItemDisplayed = await this.scrollBox.isItemDisplayed(1);
                    await this.expect(isFirstItemDisplayed).to.be.equal(true, 'Первый элемент виден');

                    await this.scrollBox.clickRight();
                    isFirstItemDisplayed = await this.scrollBox.isItemDisplayed(1);
                    await this.expect(isFirstItemDisplayed).to.be.equal(false, 'Первый элемент не виден');

                    await this.scrollBox.clickLeft();
                    isFirstItemDisplayed = await this.scrollBox.isItemDisplayed(1);
                    await this.expect(isFirstItemDisplayed).to.be.equal(true, 'Снова виден первый элемент');
                },
            }),
            'появляются новые элементы': makeCase({
                id: 'marketfront-2592',
                issue: 'MARKETVERSTKA-29326',
                async test() {
                    let firstHiddenItem = 0;
                    const items = await this.scrollBox.getItems();

                    for (let i = 1; i <= items.value.length; i++) {
                        // eslint-disable-next-line no-await-in-loop
                        const isDisplayed = await this.scrollBox.isItemDisplayed(i);

                        if (!isDisplayed) {
                            firstHiddenItem = i;
                            break;
                        }
                    }

                    let isItemDisplayed;
                    isItemDisplayed = await this.scrollBox.isItemDisplayed(firstHiddenItem);
                    await this.expect(isItemDisplayed).to.be.equal(false, `Элемент с индексом ${firstHiddenItem} не виден`);

                    await this.scrollBox.clickRight();
                    isItemDisplayed = await this.scrollBox.isItemDisplayed(firstHiddenItem);
                    return this.expect(isItemDisplayed).to.be.be.equal(true, 'Ранее скрытый элемент виден');
                },
            }),
        },
    },
});
