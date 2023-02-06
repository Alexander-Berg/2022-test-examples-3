require('models/resource/resource');
require('models/contacts/contacts');
require('models/users/users-folder');
require('models/invitees/invitees-folder/invitees-folder');

describe('Модель inviteesFolder', () => {
    describe('#getItemByEmail', () => {
        beforeEach(function() {
            const folder = ns.Model.get('resource', {
                id: '/disk/folder'
            });
            folder.setData({
                name: 'hello, world',
                meta: {
                    group: { gid: 1 }
                }
            });
            const contacts = [
                {
                    avatar: '',
                    department: 'Служба планирования мобильных клиентов',
                    email: 'dep-97a1a4fa56354730b49b@garris-debug-ws.yaserv.biz',
                    groupid: 'department:21',
                    id: 'department:21',
                    idFolder: '/disk/Музыка',
                    is_ya_directory: true,
                    name: 'Группа коммуникаций',
                    service: 'ya_directory',
                    status: 'contact',
                    type: 'department',
                    userid: 'department:21'
                },
                {
                    avatar: '',
                    id: 'b2btest@yandex.ru',
                    idFolder: '/disk/Музыка',
                    name: 'b2btest@yandex.ru',
                    rights: 660,
                    service: 'email',
                    status: 'proposed',
                    userid: 'b2btest@yandex.ru'
                }
            ];
            ns.Model.get('contacts').setData({
                status: 'loaded',
                contacts: []
            });
            ns.Model.get('usersFolder', {
                idFolder: '/disk/folder',
                gidFolder: 1
            }).setDataDefault();

            this.invitees = ns.Model.get('inviteesFolder', {
                idFolder: '/disk/folder'
            });
            const contact1 = ns.Model.get('inviteeFolder', {
                idFolder: '/disk/folder',
                id: 'department:21'
            });
            contact1.setData(contacts[0]);
            const contact2 = ns.Model.get('inviteeFolder', {
                idFolder: '/disk/folder',
                id: 'b2btest@yandex.ru'
            });
            contact2.setData(contacts[1]);

            this.invitees.insert([contact1, contact2]);
        });
        afterEach(function() {
            this.invitees.destroyAll();
        });
        it('должен вернуть модельку из коллекции по переданному email', function() {
            const invitee = this.invitees.getItemByEmail('dep-97a1a4fa56354730b49b@garris-debug-ws.yaserv.biz');
            expect(invitee.get('.name') === 'Группа коммуникаций').to.be.ok();
        });
    });
});
