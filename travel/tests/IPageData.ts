export default interface IPageData {
    body: string;
    headers: Record<string, string>;
    url: string;

    httpCode?: number;
}
