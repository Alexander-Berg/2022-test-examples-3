import {
    makeSuite,
    makeCase,
} from 'ginny';

const STREET_INFO = {
    name: 'Тверская',
    suggestText: 'Тверская улица',
    coordinates: [55.76392399999373, 37.60644099999995],
};

export default makeSuite('Поиск по карте', {
    issue: 'MARKETFRONT-47969',
    id: 'marketfront-4673',
    story: {
        'При вводе названия улицы': {
            'карта масштабируется и фокусируется на этой улице': makeCase({
                async test() {
                    await this.returnMapSuggest.waitForVisible()
                        .should.eventually.be.equal(
                            true,
                            'Инпут поиска по улице должен быть отображён'
                        );

                    const initialMapZoom = await this.returnMap.getZoom();

                    await this.returnMapSuggest.setText(STREET_INFO.name, false);
                    await this.returnMapSuggest.selectSuggestion(STREET_INFO.suggestText);

                    await this.browser.waitUntil(
                        async () => await this.returnMap.getZoom() > initialMapZoom,
                        3000,
                        'Зум не изменился',
                        1000
                    );


                    return this.browser.waitUntil(
                        async () => {
                            const [a, b] = await this.returnMap.getCenter();

                            await this.expect(Math.abs(getSafeNumber(a) - getSafeNumber(STREET_INFO.coordinates[0]))).to.be.lessThanOrEqual(1);

                            await this.expect(Math.abs(getSafeNumber(b) - getSafeNumber(STREET_INFO.coordinates[1]))).to.be.lessThanOrEqual(1);

                            return true;
                        },
                        3000,
                        'Центр карты не соответствует координатам выбранной улицы',
                        1000
                    );
                },
            }),
        },
    },
});

function getSafeNumber(num) {
    return Number(String(num).replace('.', ''));
}
