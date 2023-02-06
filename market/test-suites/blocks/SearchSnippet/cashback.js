import {makeCase, makeSuite} from 'ginny';
import {getBonusString} from '@self/root/src/utils/string';

const createExpectedText = (value, format) =>
    `${value} ${format !== 'short' ? getBonusString(value) : ''}${format === 'full' ? ' на Плюс' : ''}`.trim();

export default makeSuite('Кешбэк', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-18721',
    params: {
        cashbackAmount: 'Размер кешбэка у оффера',
        cashbackFormat: 'Формат надписи short | long | full',
        isTooltipOnHover: 'Показывается ли тултип при наведении',
        isExtraCashback: 'Тестируем повышенный кешбэк?',
    },
    story: {
        'По умолчанию отображается': makeCase({
            async test() {
                await this.snippetPrice.cashbackDeal.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Кешбэк должен отображаться'
                    );
            },
        }),
        'Содержит текст "х баллов на плюс"': makeCase({
            async test() {
                const EXPECTED_TEXT = createExpectedText(
                    this.params.cashbackAmount,
                    this.params.cashbackFormat
                );

                await this.snippetPrice.cashbackDeal.getText()
                    .should.eventually.to.be.include(
                        EXPECTED_TEXT,
                        `Текст должен содержать ${EXPECTED_TEXT}`
                    );
            },
        }),
        'При нажатии у тултипа ожидаемое поведение': makeCase({
            async test() {
                await this.snippetPrice.cashbackDeal.click();
                await this.cashbackInfoTooltip.isExisting()
                    .should.eventually.to.be.equal(
                        this.params.isTooltipOnHover,
                        `При наведении на кэшбэк тултип ${this.params.isTooltipOnHover ? '' : 'не'} должен отображаться`
                    );
            },
        }),
    },
});
