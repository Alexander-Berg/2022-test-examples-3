import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Дисклеймер с информацией о приблизительных сроках и стоимости', {
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    const isVisible = await this.shopsInfo.automaticallyCalculatedDeliveryDisclaimer.isVisible();

                    return this.expect(isVisible).to.be.equal(true, 'Дисклеймер присутствует на странице');
                },
            }),
        },
    },
});
