import fetch from 'node-fetch';

import {fetchSwagger, fetchConfig} from './fetch-swagger';


jest.mock('node-fetch');


describe('fetchSwagger', () => {
    it('should fetch spec by url', async () => {
        const specByUrl = {};
        const urlToSpec = 'http://url.to/spec';

        (fetch as any).mockResolvedValue({
            json: () => Promise.resolve(specByUrl),
        });

        const receivedSpec = await fetchSwagger(urlToSpec);

        expect(fetch).toHaveBeenCalledWith(urlToSpec, fetchConfig);
        expect(receivedSpec).toEqual(specByUrl);
    });
});
