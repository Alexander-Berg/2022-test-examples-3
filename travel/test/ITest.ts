import puppeteer from 'puppeteer';

export interface IResource {
    resourceUri: string;
    resourceSize: number;
    resourceType: puppeteer.ResourceType;
}

export interface ICoveredPage {
    pageName: string;
    pageUri: string;
    resources: IResource[];
}
