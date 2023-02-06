var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    request_text: 'the hunting party слушать',
    data_stub: {
        snippets: {
            full: {
                mus_type: '2',
                remove: [
                    'lyrics'
                ],
                alb_name: 'The Hunting Party',
                track_id: '',
                fst_search: '',
                track_len: '',
                grp_similar_ids: [],
                popularity: '14359.25',
                subtype: 'album',
                applicable: 1,
                pop_tr_names: [
                    'Keys To The Kingdom',
                    'All For Nothing',
                    'Guilty All The Same'
                ],
                alb_cover: '1',
                template: 'musicplayer',
                alb_image_uri: stubs.imageUrlStub(120, 120, { color: 'bf390c', patternSize: 24, format: 'png' }),
                track_version: '',
                type: 'musicplayer',
                types: {
                    kind: 'wizard',
                    all: [
                        'snippets',
                        'musicplayer'
                    ],
                    main: 'musicplayer'
                },
                alb_data_json: {
                    'first-tracks': [
                        {
                            'storage-dir': '118650_d3c88c3e.25250012.18061610',
                            album: {
                                'storage-dir': '595d519b.a.2000237',
                                cover: '1',
                                id: '2000237'
                            },
                            grp_name: 'Linkin Park',
                            id: '18061610',
                            title: 'Keys To The Kingdom',
                            duration: 218
                        },
                        {
                            'storage-dir': '49612_0cc259d2.25250014.18061606',
                            album: {
                                'storage-dir': '595d519b.a.2000237',
                                cover: '1',
                                id: '2000237'
                            },
                            version: 'feat. Page Hamilton',
                            grp_name: 'Linkin Park',
                            id: '18061606',
                            title: 'All For Nothing',
                            duration: 213
                        },
                        {
                            'storage-dir': '9886_9ec19fdd.26826249.17196199',
                            album: {
                                'storage-dir': '595d519b.a.2000237',
                                cover: '1',
                                id: '2000237'
                            },
                            version: 'feat. Rakim',
                            grp_name: 'Linkin Park',
                            id: '17196199',
                            title: 'Guilty All The Same',
                            duration: 355
                        }
                    ]
                },
                fst_data: '',
                slot_rank: 0,
                grp_data: '',
                track_remix: '',
                pls_user_id: '',
                slot: 'full',
                pop_tr_lens: [
                    '218',
                    '213',
                    '355'
                ],
                track_len_str: '00:00',
                track_name: '',
                alb_data: '118650_d3c88c3e.25250012.18061610\\t218\\tKeys To The Kingdom\\t\\n49612_0cc259d2.25250014.18061606\\t213\\tAll For Nothing\\tfeat. Page Hamilton\\n9886_9ec19fdd.26826249.17196199\\t355\\tGuilty All The Same\\tfeat. Rakim',
                various: '0',
                pls_kind: '',
                alb_cover_path: '595d519b.a.2000237/1',
                pls_data_json: '',
                alb_id: '2000237',
                pop_tr_versions: [
                    '',
                    'feat. Page Hamilton',
                    'feat. Rakim'
                ],
                max_tr_cnt: 3,
                radio: '',
                pls_user_login: '',
                pls_data: '',
                grp_similar_names: [],
                pop_tr_ids: [
                    '118650_d3c88c3e.25250012.18061610',
                    '49612_0cc259d2.25250014.18061606',
                    '9886_9ec19fdd.26826249.17196199'
                ],
                pls_track_count: '',
                grp: [
                    {
                        tracks: '132',
                        name: 'Linkin Park',
                        id: '36800',
                        albums: '35'
                    }
                ],
                alb_track_count: '12',
                rank: '0.639937771985',
                track_lyrics: ''
            }
        },
        doctitle: 'Linkin Park The \u0007[Hunting\u0007] \u0007[Party\u0007]',
        size: 293,
        is_recent: '1',
        url: 'http://music.yandex.ru/album/2000237?from=serp',
        server_descr: 'YMUSIC',
        markers: {
            Rule: 'Vertical/dup',
            WizardPos: '0'
        }
    }
};
