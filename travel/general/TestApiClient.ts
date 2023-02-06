import axios, {AxiosInstance} from 'axios';
import https from 'https';

export class TestApiClient {
    protected apiClient: AxiosInstance;

    constructor() {
        this.apiClient = this.createApiClient();
    }

    createApiClient(): AxiosInstance {
        return axios.create({
            baseURL: process.env.E2E_URL,
            httpsAgent: new https.Agent({
                rejectUnauthorized: false,
            }),
        });
    }
}
