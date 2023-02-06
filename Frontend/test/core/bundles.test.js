const { bundles, init } = require('../../core/bundles');
const { setPlatform } = require('../../core/assets/runtimeConfig');
const bundlesContent = require('../../core/assets/bundlesContent');

const stubBemSources = () => {
    jest.spyOn(bundlesContent, 'getBemBlockCss').mockImplementation(name =>
        name === 'bundleName' ? 'bundle-name-css-content' : undefined
    );
    jest.spyOn(bundlesContent, 'getBemBlockJs').mockImplementation(name =>
        name === 'bundleName.bundle' ? 'bundle-name-js-content' : undefined
    );
    init();
};

const stubReactSources = () => {
    jest.spyOn(bundlesContent, 'getReactBlockCss').mockReturnValue('bundle-name-react-css-content');
    jest.spyOn(bundlesContent, 'getReactBlockJs').mockReturnValue('bundle-name-react-js-content');
    init();
};

function checkBundlesContent() {
    expect(bundles.flushReactJS()).toEqual([{
        data: 'bundle-name-react-js-content',
        name: 'BundleName',
    }]);

    expect(bundles.flushReactCSS()).toEqual([
        'bundle-name-react-css-content',
    ]);

    expect(bundles.flushBundlesJS()).toEqual([{
        data: 'bundle-name-js-content',
        name: 'bundleName',
    }]);

    expect(bundles.flushBundlesCSS()).toEqual(
        'bundle-name-css-content'
    );
}

describe('bundles', () => {
    beforeEach(() => {
        setPlatform('touch-phone');
        bundles.resetBundles();
        jest.restoreAllMocks();
        init();
    });

    it('Добавляет новые бэмовые бандлы', () => {
        stubBemSources();

        // делаем дважды, чтобы также проверить то, что бандл приезжает только один раз
        bundles.pushBundle('bundleName');
        bundles.pushBundle('bundleName');

        expect(bundles.flushBundlesJS()).toEqual([{
            data: 'bundle-name-js-content',
            name: 'bundleName',
        }]);

        expect(bundles.flushBundlesCSS()).toEqual('bundle-name-css-content');

        expect(bundlesContent.getBemBlockCss).toBeCalledTimes(1);
        expect(bundlesContent.getBemBlockCss).toBeCalledWith('bundleName');

        expect(bundlesContent.getBemBlockJs).toBeCalledTimes(1);
        expect(bundlesContent.getBemBlockJs).toBeCalledWith('bundleName.bundle');
    });

    it('Добавляет новые реактовые бандлы с поведением по умолчанию', () => {
        stubReactSources();

        // делаем дважды, чтобы также проверить то, что бандл приезжает только один раз
        bundles.pushBundleReact('BundleName');
        bundles.pushBundleReact('BundleName');

        expect(bundles.flushReactJS()).toEqual([{
            data: 'bundle-name-react-js-content',
            name: 'BundleName',
        }]);

        expect(bundles.flushReactCSS()).toEqual(['bundle-name-react-css-content']);

        expect(bundlesContent.getReactBlockCss).toBeCalledTimes(1);
        expect(bundlesContent.getReactBlockCss).toBeCalledWith('BundleName');

        expect(bundlesContent.getReactBlockJs).toBeCalledTimes(1);
        expect(bundlesContent.getReactBlockJs).toBeCalledWith('BundleName');
    });

    it('Принимает в качестве параметров необходимость клиенстких стилей / скриптов', () => {
        stubReactSources();

        // делаем дважды, чтобы также проверить то, что бандл приезжает только один раз
        bundles.pushBundleReact('BundleName', { needCSS: false, needJS: true });
        bundles.pushBundleReact('BundleName', { needCSS: false, needJS: true });

        expect(bundles.flushReactJS()).toEqual([{
            data: 'bundle-name-react-js-content',
            name: 'BundleName',
        }]);

        expect(bundles.flushReactCSS(), 'Есть клиенсткие стили, когда не должно быть').toEqual([]);

        expect(bundlesContent.getReactBlockCss, 'Получение css не должно вызываться без необходимости').toBeCalledTimes(0);

        expect(bundlesContent.getReactBlockJs).toBeCalledTimes(1);
        expect(bundlesContent.getReactBlockJs).toBeCalledWith('BundleName');

        jest.restoreAllMocks();

        stubReactSources();
        bundles.resetBundles();

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: false });
        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: false });

        expect(bundles.flushReactCSS()).toEqual(['bundle-name-react-css-content']);
        expect(bundles.flushReactJS(), 'Есть клиенсткие скрипты, когда не должно быть').toEqual([]);

        expect(bundlesContent.getReactBlockJs, 'Получение js не должно вызываться без необходимости').toBeCalledTimes(0);

        expect(bundlesContent.getReactBlockCss).toBeCalledTimes(1);
        expect(bundlesContent.getReactBlockCss).toBeCalledWith('BundleName');
    });

    it('Сообщает о наличии / отсутствии бандлов', () => {
        stubReactSources();
        stubBemSources();

        expect(bundles.hasReactCSS()).toBe(false);
        expect(bundles.hasReactJS()).toBe(false);

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');

        expect(bundles.hasReactCSS()).toBe(true);
        expect(bundles.hasReactJS()).toBe(true);
    });

    it('Возвращает список добавленных бандлов', () => {
        stubReactSources();
        stubBemSources();

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');

        checkBundlesContent();

        expect(bundles.getAddedBundlesNames()).toEqual({ BundleName: 1, bundleName: 1 });
    });

    it('Устанавливает уже добавленные бандлы', () => {
        stubReactSources();
        stubBemSources();

        bundles.setExistedBundles({ BundleName: 1, bundleName: 0 });

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');

        expect(bundles.flushReactJS()).toEqual([]);
        expect(bundles.flushReactCSS()).toEqual([]);
        expect(bundles.flushBundlesJS()).toEqual([]);
        expect(bundles.flushBundlesCSS()).toEqual('');
    });

    it('Добавляет реактовый js для бандла, который был добавлен без js', () => {
        stubReactSources();
        stubBemSources();

        bundles.setExistedBundles({ BundleName: 0, bundleName: 0 });

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');

        expect(bundles.flushReactJS()).toEqual([{
            data: 'bundle-name-react-js-content',
            name: 'BundleName',
        }]);
        expect(bundles.flushReactCSS()).toEqual([]);
        expect(bundles.flushBundlesJS()).toEqual([]);
        expect(bundles.flushBundlesCSS()).toEqual('');
    });

    it('Добавляет inline-стили темизации react', () => {
        bundles.pushThemeReact('ReactBlock1', '.class1 {}');
        bundles.pushThemeReact('ReactBlock2', '.class2 {}');

        expect(bundles.flushThemeReactCSS()).toEqual(['.class1 {}', '.class2 {}']);
    });

    it('Не добавляет повторно inline-стиль темизации с тем же именем', () => {
        bundles.pushThemeReact('ReactBlock', '.first-pushed {}');
        bundles.pushThemeReact('ReactBlock', '.second-pushed {}');

        expect(bundles.flushThemeReactCSS()).toEqual(['.first-pushed {}']);
    });

    it('Сбрасывает данные', () => {
        stubReactSources();
        stubBemSources();

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');
        bundles.pushThemeReact('ReactTheme', '.awesome-class {color: red;}');

        bundles.resetBundles();

        expect(bundles.flushReactJS()).toEqual([]);
        expect(bundles.flushReactCSS()).toEqual([]);
        expect(bundles.flushBundlesJS()).toEqual([]);
        expect(bundles.flushBundlesCSS()).toEqual('');
        expect(bundles.flushThemeReactCSS()).toEqual([]);

        // также сбрасываются данные для добавленных бандлов
        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');

        checkBundlesContent();

        // также сбрасываются данные для существующих клиентских бандлов
        bundles.setExistedBundles(['BundleName', 'bundleName']);
        bundles.resetBundles();

        bundles.pushBundleReact('BundleName', { needCSS: true, needJS: true });
        bundles.pushBundle('bundleName');

        checkBundlesContent();
    });

    it('После сброса inline-стилей темизациии можно добавить новвый стиль с тем же именем снова', () => {
        bundles.pushThemeReact('ReactBlock', '.old-class {}');
        expect(bundles.flushThemeReactCSS()).toEqual(['.old-class {}']);

        bundles.resetBundles();
        expect(bundles.flushThemeReactCSS()).toEqual([]);

        bundles.pushThemeReact('ReactBlock', '.new-class {}');
        expect(bundles.flushThemeReactCSS()).toEqual(['.new-class {}']);
    });
});
