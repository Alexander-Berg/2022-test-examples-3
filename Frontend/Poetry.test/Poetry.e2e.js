specs('Колдунщик стихов', () => {
    it.skip('Простой ответ', async function() {
        const result = await this.client.request('пушкин помню чудное мгновение', {
            exp_flags: { goodwin_poetry_enabled: 1 }
        });

        this.asserts.yaCheckTextInclude(result, 'Я помню чудное мгновенье:\nПередо мной явилась ты,\n');
        this.asserts.yaCheckVoiceInclude(result, 'Я помню чудное мгновенье:.Передо мной явилась ты,.');
    });
});
