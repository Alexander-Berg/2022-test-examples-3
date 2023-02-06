export interface WidgetBuildResult {
    js: string;
    css: string;
}

export interface WidgetBuilderLayer {
    build(
        file: string,
        widgetFullPath: string,
        testDirname: string,
    ): Promise<WidgetBuildResult>;
}
