const svgToSprite = require('../../core/svg-to-sprite');

describe('svg-to-sprite', () => {
    beforeEach(() => {
        svgToSprite.resetSvgSprite();
    });

    describe('pushSvgToSprite', () => {
        it('иконка должна добавиться к спрайту', () => {
            const icon = { name: 'icon1', icon: 'icon code' };

            svgToSprite.pushSvgToSprite(icon);

            expect(svgToSprite.getAddedToSpriteSvgNames(), 'Иконка не добавилась в спрайт').toEqual(['icon1']);
            expect(svgToSprite.hasSpriteSvg(), 'Флаг наличия свг-спрайта выставлен неверно').toBe(true);
        });

        it('среди иконок с одинаковыми именами добавится только первая', () => {
            const icon1 = { name: 'icon1', icon: 'icon1 code' };
            const icon2 = { name: 'icon1', icon: 'icon2 code' };

            svgToSprite.pushSvgToSprite(icon1);
            svgToSprite.pushSvgToSprite(icon2);

            expect(svgToSprite.getAddedToSpriteSvgNames(), 'В спрайте неправильное количество иконок')
                .toEqual(['icon1']);

            expect(svgToSprite.flushSpriteSvg(), 'Не так иконка добавилась в спрайт').toEqual('icon1 code');
        });
    });

    it('reset спрайт должен почиститься', () => {
        const icon = { name: 'icon1', icon: 'icon code' };

        svgToSprite.pushSvgToSprite(icon);
        svgToSprite.resetSvgSprite();

        expect(svgToSprite.getAddedToSpriteSvgNames(), 'Спрайт не пустой').toEqual([]);
        expect(svgToSprite.hasSpriteSvg(), 'Флаг наличия свг-спрайта выставлен неверно').toBe(false);
    });

    describe('setExistedSvgInSprite', () => {
        it('иконка не должна добавиться в спрайт, если она уже есть на странице', () => {
            svgToSprite.setExistedSvgInSprite(['icon1']);
            svgToSprite.pushSvgToSprite({ name: 'icon1', icon: 'icon1 code' });

            expect(svgToSprite.getAddedToSpriteSvgNames(), 'Иконка добавилась в спрайт').toEqual([]);
        });

        it('иконка должна добавиться в спрайт, если ее нет на странице', () => {
            svgToSprite.setExistedSvgInSprite(['icon2']);
            svgToSprite.pushSvgToSprite({ name: 'icon1', icon: 'icon1 code' });

            expect(svgToSprite.getAddedToSpriteSvgNames(), 'Иконка не добавилась в спрайт').toEqual(['icon1']);
        });
    });
});
