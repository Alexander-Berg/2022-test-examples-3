import type { UrlWithParsedQuery } from 'url';

export type HermioneReplacerFn = (
    args: any[],
    commandName: string,
    pluginOptions: TestdataManagerOptions,
) => any[];
export type TestpalmReplacerFn = (data: any, pluginOptions: TestdataManagerOptions) => any;
export type TestdataReplacerFn = (
    search: Partial<UrlPlain>,
    replacement: Partial<UrlPlain>,
    contents: string,
) => string;

export type TestdataManagerOptions = {
    enabled: boolean;
    searchPath: string;
    oldUrl?: Partial<UrlPlain>;
    newUrl?: Partial<UrlPlain>;
    useSlowAlgorithm: boolean;
    hermioneReplacers: Record<string, HermioneReplacerFn>;
    testpalmReplacers: Record<string, TestpalmReplacerFn>;
    testdataReplacers: Record<string, TestdataReplacerFn>;
};

export type UrlPlain = Omit<
    UrlWithParsedQuery,
    'query' | 'slashes' | 'host' | 'path' | 'href' | 'search'
> & {
    query: [string | undefined, string | undefined][] | null;
};
