/**
 * Удаление всех устройств привязанных через метод yaAddDevices
 * @return {Promise<void>}
 */
module.exports = async function yaUnlinkDevices() {
    await this.onRecord(async() => {
        const response = await this.executeAsync(function(done) {
            const skillId = 'eacb68b3-27dc-4d8d-bdbb-b4f6fb7babd2';

            fetch(`/m/user/skills/${skillId}/unbind`, {
                headers: {
                    'x-csrf-token': window.storage.csrfToken2,
                },
                credentials: 'include',
                method: 'POST',
            }).then(done);
        }).then(execResult => execResult.value);

        if (!response.ok) {
            throw new Error(`Ошибка отвязки устройств: ${response.status} - ${response.statusText}`);
        }
    });
};
