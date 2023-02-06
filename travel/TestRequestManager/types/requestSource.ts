export enum ERequestSourceType {
    SSR = 'SSR',
    BROWSER = 'Browser',
}

export interface ICommonSource {
    pageUrl: string;
}

export interface INodeRequestSource extends ICommonSource {
    type: ERequestSourceType.SSR;
}

export interface IBrowserRequestSource extends ICommonSource {
    type: ERequestSourceType.BROWSER;
    requestUrl: string;
}

export type TRequestSource = INodeRequestSource | IBrowserRequestSource;
