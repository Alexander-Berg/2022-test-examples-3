const { resolve } = require('path');
const dotenv = require('dotenv');

dotenv.config({ path: resolve(process.cwd(), 'configs/env/.env.test') });
