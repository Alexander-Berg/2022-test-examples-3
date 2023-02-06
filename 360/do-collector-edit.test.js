'use strict';

const model = require('./do-collector-edit.js');

let core;
let params;
let mockRpop;
let mockFurita;
let mockService;

beforeEach(() => {
    params = {
        email: 'email@example.test',
        is_oauth: '0',
        login: 'login',
        no_delete_msg: 'on',
        password: 'H4!b5at+kWls-8yh4Guq',
        popid: '9000',
        port: '42',
        protocol: 'imap',
        server: 'example.test',
        use_ssl: true
    };

    mockRpop = jest.fn().mockResolvedValue('raw');
    mockFurita = jest.fn().mockResolvedValue('raw');
    mockService = jest.fn((service) => service === 'rpop' ? mockRpop : mockFurita);
    core = {
        service: mockService,
        hideParamInLog: jest.fn()
    };
});

describe('должен передать правильные параметры сервису rpop', () => {
    it('для imap-сборщика с ssl и no_delete_msg', async () => {
        await model(params, core);

        expect(mockService).toHaveBeenCalledWith('rpop');
        expect(mockRpop).toHaveBeenCalledWith('/api/edit', {
            email: 'email@example.test',
            imap: '1',
            login: 'login',
            no_delete_msgs: '1',
            password: 'H4!b5at+kWls-8yh4Guq',
            popid: '9000',
            port: '42',
            server: 'example.test',
            ssl: '1'
        });
    });

    it('для pop-сборщика без ssl и no_delete_msg', async () => {
        params.protocol = 'pop';
        params.use_ssl = false;
        params.no_delete_msg = 'off';

        await model(params, core);

        expect(mockService).toHaveBeenCalledWith('rpop');
        expect(mockRpop).toHaveBeenCalledWith('/api/edit', {
            email: 'email@example.test',
            imap: '0',
            login: 'login',
            no_delete_msgs: '0',
            password: 'H4!b5at+kWls-8yh4Guq',
            popid: '9000',
            port: '42',
            server: 'example.test',
            ssl: '0'
        });
    });

    it('для oAuth-сборщика', async () => {
        params = {
            is_oauth: '1',
            no_delete_msg: 'on',
            popid: '9000'
        };

        await model(params, core);

        expect(mockService).toHaveBeenCalledWith('rpop');
        expect(mockRpop).toHaveBeenCalledWith('/api/edit', {
            no_delete_msgs: '1',
            popid: '9000'
        });
    });

    it('для oAuth-сборщика + social_task_id', async () => {
        params = {
            is_oauth: '1',
            social_task_id: 'social_task_id',
            no_delete_msg: 'on',
            popid: '9000'
        };

        await model(params, core);

        expect(mockService).toHaveBeenCalledWith('rpop');
        expect(mockRpop).toHaveBeenCalledWith('/api/edit', {
            no_delete_msgs: '1',
            popid: '9000',
            social_task_id: 'social_task_id'
        });
    });
});

describe('должен передать правильные параметры сервису furita', () => {
    it('cliker_folder', async () => {
        params.filter_name = 'filter_name';
        params.cliker_folder = 'cliker_folder';
        params.move_folder = 'move_folder';

        await model(params, core);

        expect(mockService).toHaveBeenCalledWith('furita');
        expect(mockFurita).toHaveBeenCalledWith('/api/edit.json', {
            attachment: '',
            clicker: [ 'cliker_folder' ],
            field1: 'X-yandex-rpop-id',
            field2: '3',
            field3: '9000',
            letter: 'nospam',
            logic: '0',
            move_folder: 'move_folder',
            name: 'filter_name',
            order: '0'
        });
    });

    it('cliker_label', async () => {
        params.filter_name = 'filter_name';
        params.cliker_label = 'cliker_label';
        params.move_label = 'move_label';

        await model(params, core);

        expect(mockService).toHaveBeenCalledWith('furita');
        expect(mockFurita).toHaveBeenCalledWith('/api/edit.json', {
            attachment: '',
            clicker: [ 'cliker_label' ],
            field1: 'X-yandex-rpop-id',
            field2: '3',
            field3: '9000',
            letter: 'nospam',
            logic: '0',
            move_label: 'move_label',
            name: 'filter_name',
            order: '0'
        });
    });
});
