export default interface IDataRequest {
    host: string; // rasp.yandex.ru
    path: string; // /station/23

    count?: number;
    status?: string;
    referer?: string;
    userAgent?: string;
}
