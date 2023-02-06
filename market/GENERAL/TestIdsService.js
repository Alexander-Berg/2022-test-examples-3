import axios from 'axios';

export const getPuids = () =>
    axios.get('/api/test_ids/puids')
        .then(r => r.data);
