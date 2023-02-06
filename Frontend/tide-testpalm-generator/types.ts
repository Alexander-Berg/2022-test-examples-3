export type TestCommand = {
    name: string;
    code: string;
    arguments: any[];
    path: string;
};

export type PluginConfig = {
    enabled: boolean;
    rewrite: boolean;
    allLinks: boolean;
    hermioneToTestpalm: boolean;
    filePaths: string[];
    ignoreObjects: string[];
    textReplacers: Record<string, string>;
    replacers: Record<string, any>;
};

export type PluginConfigPartial = Partial<PluginConfig>;
