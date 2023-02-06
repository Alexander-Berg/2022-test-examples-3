var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    request_text: 'madonna',
    data_stub: {
        snippets: {
            full: {
            mus_type: '3',
            subtype: 'artist',
            remove: [
              'lyrics'
            ],
            alb_name: '',
            track_id: '',
            fst_search: '',
            track_len: '',
            grp_similar_ids: [
              '7099',
              '1438',
              '8773',
              '7090',
              '3477'
            ],
            popularity: '20363.0',
            applicable: 1,
            pop_tr_names: [
              'Music',
              'Masterpiece',
              'Like A Virgin',
              'Hung Up'
            ],
            serp_info: {
              remove: 'lyrics',
              subtype: 'artist',
              format: 'json',
              type: 'musicplayer',
              slot: 'full',
              flat: '1'
            },
            grp_similar: '7099\\tKylie Minogue\\n1438\\tLady Gaga\\n8773\\tMylène Farmer\\n7090\\tNelly Furtado\\n3477\\tRihanna',
            alb_cover: '3',
            template: 'musicplayer',
            geo_id: '225',
            alb_image_uri: stubs.imageUrlStub(120, 120, { color: 'bf390c', patternSize: 24, format: 'png' }),
            grp_data_json: {
              'popular-tracks': [
                {
                  'storage-dir': '51066_a0542adb.36318608.125906',
                  album: {
                    'storage-dir': '7a33aad4.a.62776',
                    cover: '3',
                    id: '62776'
                  },
                  version: 'Album Version',
                  id: '125906',
                  title: 'Music',
                  duration: 224
                },
                {
                  'storage-dir': '54244_3d6e6d3c.246486.215618',
                  album: {
                    'storage-dir': '61f4233f.a.21992',
                    cover: '1',
                    id: '21992'
                  },
                  version: 'Album Version',
                  id: '215618',
                  title: 'Masterpiece',
                  duration: 281
                },
                {
                  'storage-dir': '45873_684248f0.247144.216180',
                  album: {
                    'storage-dir': 'a970c680.a.22042',
                    cover: '1',
                    id: '22042'
                  },
                  version: 'Album Version',
                  id: '216180',
                  title: 'Like A Virgin',
                  duration: 218
                }
              ]
            },
            pop_tr_groups: [
              '',
              '',
              '',
              '',
              ''
            ],
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
            track_storage_dir: '',
            alb_year: '',
            alb_storage_dir: '7a33aad4.a.62776',
            pls_name: '',
            counter_prefix: '/snippet/musicplayer/artist/',
            alb_data_json: '',
            fst_data: '',
            slot_rank: 0,
            grp_data: '',
            track_remix: '',
            pls_user_id: '',
            slot: 'full',
            pop_tr_lens: [
              '224',
              '281',
              '218',
              '238',
              '202'
            ],
            track_len_str: '00:00',
            track_name: '',
            alb_data: '',
            various: '0',
            pls_kind: '',
            alb_cover_path: '7a33aad4.a.62776/3',
            pls_data_json: '',
            alb_id: '',
            pop_tr_versions: [
              'Album Version',
              'Album Version',
              'Album Version',
              '',
              'Radio Version'
            ],
            max_tr_cnt: 3,
            radio: '1',
            pls_user_login: '',
            pls_data: '',
            grp_similar_names: [
              'Kylie Minogue',
              'Lady Gaga',
              'Mylène Farmer',
              'Nelly Furtado',
              'Rihanna'
            ],
            pop_tr_ids: [
              '51066_a0542adb.36318608.125906',
              '54244_3d6e6d3c.246486.215618',
              '45873_684248f0.247144.216180',
              '27918_7b62ba7a.41811914.3619283',
              '56692_a472f1d2.40858552.133054'
            ],
            pls_track_count: '',
            grp: [
              {
                tracks: '347',
                name: 'Madonna',
                id: '1813',
                albums: '57'
              }
            ],
            alb_track_count: '',
            rank: '0.664687449386',
            track_lyrics: ''
          }
        },
        doctitle: '\u0007[Madonna\u0007]',
        url_parts: {},
        size: 93,
        is_recent: '1',
        url: 'http://music.yandex.ru/artist/1813?from=serp',
        green_url: 'music.yandex.ru/artist/1813?from=serp',
        host: 'music.yandex.ru',
        favicon_domain: 'music.yandex.ru',
        markers: {
          Rule: 'Vertical/dup',
          WizardPos: '1'
        },
        mime: 'xml'
    }
};
