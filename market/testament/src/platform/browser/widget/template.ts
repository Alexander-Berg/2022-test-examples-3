const template = (widgetFullPath: string): string =>
    `
        import Runtime from '@yandex-market/apiary/client/runtime';

        import '${widgetFullPath}';

        runtime = new Runtime({strategy: {}, useShadowStore: false});
        runtime.run();

        window.testamentApiaryRuntime = runtime;
    `;

export default template;
