type Mock = {
    times?: number;
    host:
        | 'http://laas.yandex.ru'
        | 'https://api.content.market.yandex.ru'
        | 'http://sovetnik-filter.yandex.net'
        | 'http://rw.vs.market.yandex.net'
        | 'http://api-report.vs.market.yandex.net';
    route: string | RegExp;
    method?: 'get' | 'post' | 'delete';
    response: {
        status?: 'OK';
        context?: {
            region: Record<string, any>;
            currency: Record<string, any>;
            id: string;
            time: Date;
            marketUrl: string;
        };
    };
    allowUnmocked?: boolean;
};

export default Mock;
