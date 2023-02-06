export default interface IDataForRequestPage {
    url: string;

    timeout?: number;
    headers?: Record<string, string>;
    cookies?: Record<string, string>;
    encoding?: 'utf8';
    followRedirect?: boolean;
}
