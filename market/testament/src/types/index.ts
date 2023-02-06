declare module 'asker' {
    export default class Request {
        done(err: Error | null, data: unknown): void;

        getResponseMetaBase(): {
            time: {network: number; total: number};
            retries: {used: number; limit: number};
        };
    }
}
