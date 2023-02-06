import '../noscript';
jest.mock('../../../components/extract-preloaded-data');
jest.mock('config', () => ({}));

import helperRemoteResource from '../../../components/helpers/remote-resource';

const testData = [
    {
        // перименование папки из диска
        data: {"values":[{"tag":"op","parameters":{"fid":"9e713df6762d49458386cd82eeea8c115febcb956e7142d6948247b31c20aa2d","key":"/disk/music/flowers","folder":"/disk/music/","type":"deleted","resource_type":"dir"},"value":""},{"tag":"op","parameters":{"fid":"9e713df6762d49458386cd82eeea8c115febcb956e7142d6948247b31c20aa2d","key":"/disk/music/myflowers","folder":"/disk/music/","type":"new","resource_type":"dir"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562570811991731,"old":1562570320603000}}}, // eslint-disable-line
        type: 'rename'
    },
    {
        // перемещение файла из диска
        data: {"values":[{"tag":"op","parameters":{"size":1822600,"key":"/disk/music/1 - 2015-04-20 10-11-03.JPG","fid":"5958d87941646006320303399945483a027ba7d0e7aec701f4710d45b8c9e42d","folder":"/disk/music/","sha256":"3272986ea748dda49b84282b0e38c4bf6893429e299166b114abeea9ed156f70","type":"deleted","resource_type":"file","md5":"e11c350f54bd83452fb546886a58a314"},"value":""},{"tag":"op","parameters":{"size":1822600,"key":"/disk/music/myflowers/1 - 2015-04-20 10-11-03.JPG","fid":"5958d87941646006320303399945483a027ba7d0e7aec701f4710d45b8c9e42d","folder":"/disk/music/myflowers/","sha256":"3272986ea748dda49b84282b0e38c4bf6893429e299166b114abeea9ed156f70","type":"new","resource_type":"file","md5":"e11c350f54bd83452fb546886a58a314"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562573712528853,"old":1562570811991731}}}, // eslint-disable-line
        type: 'move'
    },
    {
        // удаление файла из диска в корзину
        data: {"values":[{"tag":"op","parameters":{"size":4028274,"key":"/disk/music/aero_drum.mp3","fid":"9be8fbbd92a3886bfc825d287894945135cf99069a6545bcb77b67f21ec72fb8","folder":"/disk/music/","sha256":"b34b23cc4e469146aad6737426fb6b803f80adc62d24fbc6327758645c2b9dc9","type":"deleted","resource_type":"file","md5":"0aae401226b6a4ef0c07e3d9eff07f45"},"value":""},{"tag":"op","parameters":{"key":"/trash/aero_drum.mp3","name":"aero_drum.mp3","size":4028274,"fid":"9be8fbbd92a3886bfc825d287894945135cf99069a6545bcb77b67f21ec72fb8","md5":"0aae401226b6a4ef0c07e3d9eff07f45","folder":"/trash/","sha256":"b34b23cc4e469146aad6737426fb6b803f80adc62d24fbc6327758645c2b9dc9","type":"new","resource_type":"file"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562573793700130,"old":1562573712528853}}}, // eslint-disable-line
        type: 'delete'
    },
    {
        // восстановление файла из корзины
        data: {"values":[{"tag":"op","parameters":{"size":4028274,"key":"/trash/aero_drum.mp3","fid":"9be8fbbd92a3886bfc825d287894945135cf99069a6545bcb77b67f21ec72fb8","folder":"/trash/","sha256":"b34b23cc4e469146aad6737426fb6b803f80adc62d24fbc6327758645c2b9dc9","type":"deleted","resource_type":"file","md5":"0aae401226b6a4ef0c07e3d9eff07f45"},"value":""},{"tag":"op","parameters":{"size":4028274,"key":"/disk/music/aero_drum.mp3","fid":"9be8fbbd92a3886bfc825d287894945135cf99069a6545bcb77b67f21ec72fb8","folder":"/disk/music/","sha256":"b34b23cc4e469146aad6737426fb6b803f80adc62d24fbc6327758645c2b9dc9","type":"new","resource_type":"file","md5":"0aae401226b6a4ef0c07e3d9eff07f45"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562573842289423,"old":1562573793700130}}}, // eslint-disable-line
        type: 'restore'
    },
    {
        // удаление файла из корзины
        data: {"values":[{"tag":"op","parameters":{"fid":"dab9425d8dd74e4aa257f11ac2bebd00f7022017677840429f620786ba0a7396","key":"/trash/555555500000","folder":"/trash/","type":"deleted","resource_type":"dir"},"value":""}],"root":{"tag":"diff","parameters":{"new":"1553501558283885","old":"1562336766896876"}}}, // eslint-disable-line
        type: 'delete'
    },
    {
        // публикация файла
        data: {"values":[{"tag":"op","parameters":{"fid":"4d5d69445f28d469e9008ffa5211143a2886e6f6414dff72664fda034f5bbfd0","key":"/disk/music/2015-05-12 09-27-29.JPG","folder":"/disk/music/","type":"published","resource_type":"file"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562574009075494,"old":"1562573842289423"}}}, // eslint-disable-line
        type: 'publish'
    },
    {
        // загрузка файла в Диск
        data: {"values":[{"tag":"op","parameters":{"size":339038,"key":"/disk/342638051.jpg","fid":"4ccd5699ec1b5021d6fab6b1161c45e74522cb91e7af8e72bfc595361f0a25e0","folder":"/disk/","sha256":"d714c1082f64a0b218c14af12902146fd47d6f0b8cddfb32e6daac51771fe93a","type":"new","resource_type":"file","md5":"29ec2b288881b4174e305124bc4e2680"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562574579069169,"old":1555598027724688}}}, // eslint-disable-line
        type: 'upload'
    },
    {
        // изменение файла в Диске
        data: {"values":[{"tag":"op","parameters":{"size":339038,"key":"/disk/342638051.jpg","fid":"4ccd5699ec1b5021d6fab6b1161c45e74522cb91e7af8e72bfc595361f0a25e0","action":"setprop","folder":"/disk/","sha256":"d714c1082f64a0b218c14af12902146fd47d6f0b8cddfb32e6daac51771fe93a","type":"changed","resource_type":"file","md5":"29ec2b288881b4174e305124bc4e2680"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562574581674785,"old":"1562574579069169"}}}, // eslint-disable-line
        type: 'changed'
    },
    {
        // публикация безлимитного файла
        data: {"values":[{"tag":"op","parameters":{"fid":"4d5d69445f28d469e9008ffa5211143a2886e6f6414dff72664fda034f5bbfd0","key":"/photounlim/2015-05-12 09-27-29.JPG","folder":"/disk/music/","type":"published","resource_type":"file"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562574009075494,"old":"1562573842289423"}}}, // eslint-disable-line
        type: 'publish'
    },
    {
        // публикация файла из Я.Фотки в разделе Архив
        data: {"values":[{"tag":"op","parameters":{"fid":"4d5d69445f28d469e9008ffa5211143a2886e6f6414dff72664fda034f5bbfd0","key":"/attach/YaFotki/2015-05-12 09-27-29.JPG","folder":"/disk/YaFotki/","type":"published","resource_type":"file"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562574009075494,"old":"1562573842289423"}}}, // eslint-disable-line
        type: 'publish'
    },
    {
        // перемещение файла внутри служебной папки
        data: {"values":[{"tag":"op","parameters":{"size":1822600,"key":"/service folder/1.JPG","fid":"5958d87941646006320303399945483a027ba7d0e7aec701f4710d45b8c9e42d","folder":"/service folder/","sha256":"3272986ea748dda49b84282b0e38c4bf6893429e299166b114abeea9ed156f70","type":"deleted","resource_type":"file","md5":"e11c350f54bd83452fb546886a58a314"},"value":""},{"tag":"op","parameters":{"size":1822600,"key":"/service folder/2/1.JPG","fid":"5958d87941646006320303399945483a027ba7d0e7aec701f4710d45b8c9e42d","folder":"/service folder/2/","sha256":"3272986ea748dda49b84282b0e38c4bf6893429e299166b114abeea9ed156f70","type":"new","resource_type":"file","md5":"e11c350f54bd83452fb546886a58a314"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562573712528853,"old":1562570811991731}}}, // eslint-disable-line
        type: undefined
    },
    {
        // загрузка файла в служебную папку
        data: {"values":[{"tag":"op","parameters":{"size":339038,"key":"/service folder/342638051.jpg","fid":"4ccd5699ec1b5021d6fab6b1161c45e74522cb91e7af8e72bfc595361f0a25e0","folder":"/service folder/","sha256":"d714c1082f64a0b218c14af12902146fd47d6f0b8cddfb32e6daac51771fe93a","type":"new","resource_type":"file","md5":"29ec2b288881b4174e305124bc4e2680"},"value":""}],"root":{"tag":"diff","parameters":{"new":1562574579069169,"old":1555598027724688}}}, // eslint-disable-line
        type: undefined
    }
];

describe('HelperRemoteResource', () => {
    describe('getOperationData', () => {
        it('Должна верно определить действие с файлами внутри диска', () => {
            testData.forEach((test) => {
                const operationData = helperRemoteResource.getOperationData(test.data);
                expect(operationData.type).toEqual(test.type);
            });
        });
    });
});
