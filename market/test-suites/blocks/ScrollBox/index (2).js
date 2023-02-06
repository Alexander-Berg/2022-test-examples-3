import {makeSuite, makeCase} from 'ginny';

export default makeSuite('ScrollBox.', {
    feature: 'ScrollBox',
    story: {
        'По умолчанию': {
            'должен быть показан.': makeCase({
                id: 'm-touch-1934',
                test() {
                    return this.ScrollBox
                        .isVisible()
                        .should.eventually.to.be.equal(true, 'ScrollBox отобразился на странице');
                },
            }),
        },
    },
});
