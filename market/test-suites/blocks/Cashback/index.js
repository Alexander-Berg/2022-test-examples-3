import {makeCase, makeSuite} from 'ginny';
import {getBonusString} from '@self/root/src/utils/string';

const GRADIENT_IN_CSS = 'rgba(0,0,0,0)'
 + 'radial-gradient(106.26%150.99%at-3.13%19.4%,rgb(80,90,221)37.5%,rgb(190,64,192)65.1%,rgb(251,168,43)92.19%)'
 + 'repeatscroll0%0%/autopadding-boxtext';

const createExpectedText = (value, format, isExtraCashback) => {
    const cashText = isExtraCashback && format === 'full' ? ' — повышенный кешбэк' : ' на Плюс';
    return `${value} ${format !== 'short' ? getBonusString(value) : ''}${format === 'full' ? cashText : ''}`;
};

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
                await this.cashbackDealTerms.isExisting()
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
                    this.params.cashbackFormat,
                    this.params.isExtraCashback
                );

                await this.cashbackDealTerms.getCashbackText()
                    .should.eventually.to.be.include(
                        EXPECTED_TEXT,
                        `Текст должен содержать ${EXPECTED_TEXT}`
                    );
            },
        }),
        'При наведении у тултипа ожидаемое поведение': makeCase({
            async test() {
                await this.cashbackDealTerms.onCashbackHover();
                await this.cashbackInfoTooltip.isExisting()
                    .should.eventually.to.be.equal(
                        this.params.isTooltipOnHover,
                        `При наведении на кэшбэк тултип${this.params.isTooltipOnHover ? '' : ' не'} должен отображаться`
                    );
            },
        }),
        'Цвет текста имеет правильное значение': makeCase({
            // Проверка на наличие градиентного значения для background свойства для повышенного кешбэка
            async test() {
                const textBackgroundColor = this.browser.allure.runStep(
                    'Получаем свойство background текста',
                    () => this.cashbackDealText.root.getCssProperty('background')
                        .then(color => color.parsed.rgba)
                );

                if (!this.params.isExtraCashback) {
                    return textBackgroundColor
                        .should.not.be.equal(
                            GRADIENT_IN_CSS,
                            'Кешбэк не должен быть градиентным'
                        );
                }


                return textBackgroundColor
                    .should.eventually.to.be.equal(
                        GRADIENT_IN_CSS,
                        'свойство background не должно быть задано'
                    );
            },
        }),
    },
});
