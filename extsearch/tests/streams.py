# coding: utf-8

from .run import run_vm


def test_fail(tmpdir):
    input_stream = {
        'ContentGroupID': '12345',
        'ContentVersionID': '321',
        'FaasCustomParameters': None,
        'InputStreamID': '123',
        'Status': 'faas-sent',
        'UUID': 'uuid',
        'VodProviderOptions': '{}',
        'VodProviderThumbIdx': 5,
    }

    webhook = {
        'ChangedAt': 1601282647,
        'CreatedAt': 1601282420,
        'Error': 'transcoding bin failed: exit status 255',
        'ErrorCode': 3,
        'ErrorCodeStr': 'EBadInput',
        'ErrorInfoOutput': 'convert_parallel_op_2 failed AssertionError ()\nconvert_parallel_op failed AssertionError ()',
        'InputUrl': 'https://dumatv.ru/uploads/videos/fragments/2019-06/%D0%90%D0%BA%D1%81%D0%B0%D0%BA%D0%BE%D0%B2%2.mp4',
        'Status': 6,
        'StatusStr': 'ETSError',
        'TaskId': '258d7b9d-41b47b62-8a159cea-7bb30fdb',
        'User': 'news'
    }

    return run_vm(tmpdir, input_stream, webhook, '{"content_group_id":"12345","status":"rt-transcoder-failed"}')


def test_ok_with_previews(tmpdir):
    input_stream = {
        'ContentGroupID': '12345',
        'ContentVersionID': '321',
        'FaasCustomParameters': None,
        'InputStreamID': '123',
        'Status': 'faas-sent',
        'UUID': 'uuid',
        'VodProviderOptions': '{}',
        'VodProviderThumbIdx': 5
    }

    webhook = {
        'Bitrate': 4579169,
        'ChangedAt': 1601384863,
        'ContentStatus': 2,
        'ContentStatusStr': 'ECSDone',
        'CreatedAt': 1601384801,
        'DurationMs': 46234,
        'FileSize': 26464168,
        'HasAudioStream': True,
        'InputHeight': 1080,
        'InputUrl': 'https://s.ura.news/images/news/upload/video/news/1052451849/239232f093ca4775ec719b1bbf88ab4a.mp4',
        'InputWidth': 1080,
        'MetarobotResultsUrl': 'http://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0_results.pb.txt',
        'PreviewStatus': 2,
        'PreviewStatusStr': 'EPSDone',
        'Previews': [
            {
                'Height': 360,
                'TargetHeight': 360,
                'TargetWidth': 480,
                'Url': 'https://video-preview.s3.yandex.net/vh/18183299633916394533_simple-preview-360.mp4',
                'Width': 360
            },
            {
                'Height': 360,
                'TargetHeight': 360,
                'TargetWidth': 640,
                'Url': 'https://video-preview.s3.yandex.net/vh/18183299633916394533_vmaf-preview-360.mp4',
                'Vmaf': True,
                'Width': 360
            },
            {
                'Height': 720,
                'TargetHeight': 720,
                'TargetWidth': 1280,
                'Url': 'https://video-preview.s3.yandex.net/vh/18183299633916394533_vmaf-preview-720.mp4',
                'Vmaf': True,
                'Width': 720
            }
        ],
        'Status': 5,
        'StatusStr': 'ETSDone',
        'Streams': [
            {
                'AudioCodecStr': 'EAC_NONE',
                'DrmTypeStr': 'EDT_NONE',
                'DynamicRangeStr': 'EDR_NONE',
                'Format': 7,
                'FormatStr': 'EKaltura',
                'Url': 's3://vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/kaltura/desc_0a965ce68241db234fc79e9d26623375',
                'VideoCodecStr': 'EVC_NONE',
                'VideoQualityStr': 'EVQ_NONE'
            }
        ],
        'TaskId': '5fac211e-a17413b9-a935a18d-a66313b0',
        'Thumbnails': [
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_0.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_1.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_2.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_3.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_4.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_5.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_6.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_7.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_8.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_9.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_10.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_11.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_12.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_13.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_14.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_15.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_16.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_17.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_18.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_19.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_20.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_21.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_22.jpg',
            'https://s3.mds.yandex.net/vh-ya-news-converted/vod-content/18183299633916394533/5fac211e-a17413b9-a935a18d-a66313b0/preview/screen_23.jpg'
        ],
        'User': 'news'
    }
    return run_vm(tmpdir, input_stream, webhook, '{"content_group_id":"12345","status":"converted"}')


def test_ok_catchup(tmpdir):
    input_stream = {
        'ContentGroupID': '12345',
        'ContentVersionID': '321',
        'InputStreamID': '123',
        'Status': 'faas-sent',
        'UUID': 'uuid',
        'VodProviderOptions': '{}',
        'VodProviderThumbIdx': 5,
        'FaasCustomParameters': {
            'live_to_vod': True
        }
    }

    webhook = {
        'Bitrate': 8426149,
        'ChangedAt': 1601051248,
        'ContentStatus': 2,
        'ContentStatusStr': 'ECSDone',
        'CreatedAt': 1601049782,
        'DurationMs': 1115560,
        'FileSize': 1174984453,
        'HasAudioStream': True,
        'InputHeight': 1080,
        'InputUrl': 'https://strm.yandex.ru/kal/government/government0.m3u8?start=1600790366&end=1600791490',
        'InputWidth': 1920,
        'OutputUrl': 'https://strm.yandex.ru/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/hls/master.m3u8',
        'PreviewStatus': 2,
        'PreviewStatusStr': 'EPSDone',
        'Previews': [
            {
                'Height': 270,
                'TargetHeight': 360,
                'TargetWidth': 480,
                'Url': 'https://video-preview.s3.yandex.net/vh/57417016303534492_reference-270.mp4',
                'Width': 480
            },
            {
                'Height': 360,
                'TargetHeight': 360,
                'TargetWidth': 640,
                'Url': 'https://video-preview.s3.yandex.net/vh/57417016303534492_vmaf-preview-360.mp4',
                'Vmaf': True,
                'Width': 640
            },
            {
                'Height': 720,
                'TargetHeight': 720,
                'TargetWidth': 1280,
                'Url': 'https://video-preview.s3.yandex.net/vh/57417016303534492_vmaf-preview-720.mp4',
                'Vmaf': True,
                'Width': 1280
            }
        ],
        'Status': 5,
        'StatusStr': 'ETSDone',
        'Streams': [
            {
                'AudioCodecStr': 'EAC_NONE',
                'DrmTypeStr': 'EDT_NONE',
                'DynamicRangeStr': 'EDR_NONE',
                'Format': 1,
                'FormatStr': 'EHls',
                'Url': 'https://strm.yandex.ru/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/hls/master.m3u8',
                'VideoCodecStr': 'EVC_NONE',
                'VideoQualityStr': 'EVQ_NONE'
            },
            {
                'AudioCodecStr': 'EAC_NONE',
                'DrmTypeStr': 'EDT_NONE',
                'DynamicRangeStr': 'EDR_NONE',
                'Format': 3,
                'FormatStr': 'EDash',
                'Url': 'https://strm.yandex.ru/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/dash/manifest.mpd',
                'VideoCodecStr': 'EVC_NONE',
                'VideoQualityStr': 'EVQ_NONE'
            }
        ],
        'TaskId': 'f5dea45d-a7bb0fb0-2ba6553a-e1518216',
        'Thumbnails': [
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_0.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_1.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_2.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_3.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_4.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_5.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_6.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_7.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_8.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_9.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_10.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_11.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_12.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_13.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_14.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_15.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_16.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_17.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_18.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_19.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_20.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_21.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_22.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_23.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_24.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_25.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_26.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_27.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_28.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_29.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_30.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_31.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_32.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_33.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_34.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_35.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_36.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_37.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_38.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_39.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_40.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_41.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_42.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_43.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_44.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_45.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_46.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_47.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_48.jpg',
            'https://s3.mds.yandex.net/vh-special-converted/vod-content/57417016303534492/f5dea45d-a7bb0fb0-2ba6553a-e1518216/preview/screen_49.jpg',
        ],
        'User': 'catchup-vod'
    }
    return run_vm(tmpdir, input_stream, webhook, '{"content_group_id":"12345","status":"converted"}')
