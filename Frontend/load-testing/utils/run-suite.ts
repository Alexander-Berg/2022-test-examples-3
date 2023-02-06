/* eslint-disable */
import * as fs from 'fs';
import { Client } from 'ssh2';
import { TestSuite } from '../types';
import { processTestSuite } from './generate';
import { defaultSuiteConfig } from '../configs/suites/default';
import { askForInput } from '../../cli/utils';

const runSuite = async(name: string, silent = false) => {
    if (process.env.CONFIG_ENV !== 'load') {
        throw new Error('Only available in "load" environment');
    }

    const log = (...args: any[]) => !silent && console.log(...args);

    let suite: TestSuite;

    try {
        suite = Object.assign({}, defaultSuiteConfig, require(`../test-suits/${name}`).default);
    } catch (e) {
        throw new Error('No suite with such name');
    }

    const { ammo, tankConfig } = await processTestSuite(suite);

    const conn = new Client();

    const connectionOptions = {
        host: suite.tank,
        username: suite.operator,
        privateKey: fs.readFileSync(process.env.SSH_PRIVATE_KEY!),
        passphrase: await askForInput('Passphrase (for connecting to tank via ssh):', 'password'),
    };

    return new Promise<void>((resolve, reject) => {
        conn.on('ready', () => {
            conn.sftp(async(err, sftp: any) => {
                if (err) {
                    reject(err);
                }

                log('Sending ammo...');

                sftp.writeFile('/ammo.txt', ammo, (writeError1: Error) => {
                    if (writeError1) {
                        reject(writeError1);
                    }

                    log('Sending config...');
                    sftp.writeFile('/tank.yaml', tankConfig, (writeError2: Error) => {
                        if (writeError2) {
                            reject(writeError2);
                        }

                        log('launch shooting');

                        conn.exec('yandex-tank -c tank.yaml ammo.txt', (e, stream) => {
                            if (e) {
                                log('Shooting start failed');
                                reject(e);
                            }

                            stream
                                .on('close', () => {
                                    log('Test finished');
                                    conn.end();
                                    resolve();
                                })
                                .on('data', (data: Buffer) => {
                                    const str = data.toString();

                                    // Получаем ссылку на лунапарк
                                    if (str.indexOf('Web link') > -1) {
                                        log(/https:\/\/lunapark\.yandex-team\.ru\/\d*/.exec(str)![0]);
                                    }

                                    if (str.indexOf('Waiting for test to finish') > -1) {
                                        log(str);
                                    }
                                })
                                .stderr.on('data', (data: Buffer) => {
                                    log(data.toString());
                                });
                        });
                    });
                });
            });
        }).connect(connectionOptions);
    });
};

export default runSuite;
