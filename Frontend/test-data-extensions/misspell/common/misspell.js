module.exports = {
    type: 'wizard',
    request_text: 'тлактор',
    data_stub: [
        {
            corrected_text: "",
            counter_prefix: "wiz/reask",
            enabled: 0,
            force: null,
            immediately: null,
            orig_doc_count: 0,
            raw_source_text: "т(л)актор",
            rule: "Misspell",
            shortened_raw_source_text: true,
            show_message: 1,
            text: "трактор",
            type: "reask",
            wizplace: "upper",
            misspell: {
                applicable: 0,
                counter_prefix: "/wiz/misspell/",
                orig_weight: null,
                source: "report",
                type: "misspell_source",
                types: {
                    all: ["wizard", "misspell"],
                    extra: [],
                    kind: "wizard",
                    main: "misspell"
                },
                items: [{
                    clear_text: "трактор",
                    dist: null,
                    flags: 0,
                    from: "report",
                    orig_penalty: null,
                    raw_source_text: "т(л)актор",
                    raw_text: "т(р)актор",
                    relev: 10000,
                    source: "Misspell",
                    weight: null
                }]
            }
        }
    ]
};
