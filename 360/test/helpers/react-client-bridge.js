before(function() {
    // эту функцию подложит импорт (см. karma.conf.js)
    // react-client/src/app/containers/Body/Script/scripts/reactClientBridge.js
    reactClientBridge();

    window.ReactClientBridge.Exposed = {
        PsSettingsDatasyncService: {
            isBeautifulEmailForceEnabled: false
        },
        GeneralThemesBridge: {
            getGeneralThemeNames: () => ([]),
            getThemeName: () => '',
            onGeneralThemeChange: () => {}
        }
    };
});
