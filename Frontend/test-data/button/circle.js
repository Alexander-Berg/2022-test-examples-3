const data = require('../../tools/data');

module.exports = data.createSnippet({
    block: 'container',
    content: [
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'telegram', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'vkontakte', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'facebook', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'whatsapp', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'viber', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            tag: 'br',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'chat', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'call', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'chat-inverted', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'call-inverted', icon: true },
            url: '#',
            target: '_blank',
        },
        {
            block: 'button',
            mods: { size: 'm', circle: true, type: 'link', theme: 'google', icon: true },
            url: '#',
            target: '_blank',
        },
    ],
});
