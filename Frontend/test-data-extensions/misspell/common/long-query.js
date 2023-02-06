module.exports = {
    type: 'reask',
    request_text: 'f zuF D ifxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkg',
    extensions: {
        wizplaces: { 
            upper: [{
                remove: "switch_off_thes",
                types: {
                    kind: "wizard",
                    extra: [
                        "misspell"
                    ],
                    all: [
                        "wizard",
                        "web_misspell",
                        "misspell"
                    ],
                    main: "web_misspell"
                },
                wizplace: "upper",
                relev: 1,
                package: "YxWeb::Wizard::WebMisspell",
                counter_prefix: "/wiz/web_misspell/misspell/",
                subtype: [
                    "misspell"
                ],
                applicable: 1,
                kind: "merged",
                type: "web_misspell",
                misspell: {
                    source: "report",
                    types: {
                        kind: "wizard",
                        extra: [],
                        all: [
                            "wizard",
                            "misspell"
                        ],
                        main: "misspell"
                    },
                    wizplace: "upper",
                    relev: 1.1,
                    counter_prefix: "/wiz/misspell/",
                    orig_weight: 478,
                    subtype: [
                        "misspell"
                    ],
                    applicable: 1,
                    type: "misspell_source",
                    items: [
                        {
                            clear_text: "f zuF D ifxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkg",
                            source: "Misspell",
                            flags: 192,
                            dist: 10,
                            relev: 8000,
                            raw_source_text: "fzu FD ifxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkg",
                            from: "report",
                            weight: 458,
                            raw_text: "f zuF D ifxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkgfxkgxgkxkgxkgxkgxkgxkgxkgxzkgxkgxkgxkg",
                            orig_penalty: 0
                        }
                    ]
                }
            }]
        }
    }
};
