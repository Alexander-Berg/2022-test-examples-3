import yt.yson as yson


PORTION = [
    # New hash, single row
    {
        'Url': 'https://www.youtube.com/watch?v=cViWTYZxuJI',
        'Hash': '000035C66D7BCE73BF84AE7E5A2E5B2A9449409B97474A406A1176F6EA560EBC3B73782C744898024DC2F92CD478E3BD55E6E5325EB130CF78E84346585B54D4',
        'ContentType': 'EVideo',
        'RowIndex': yson.YsonUint64(0),
        'RecordTime': yson.YsonUint64(1562841222),
        'HasNextRow': False,
    },
    # New hash, multiple rows
    {
        'Url': 'https://vk.com/video395771395_456239382',
        'Hash': '80386BC04BDF317289664F64BC0A8D5926CABC95FEFA81D37830E8CF188233B4D11D44F01A9D002F9A6A5185154BE26DE340851698058FBC5486997EC3346D5A',
        'ContentType': 'EAudio',
        'RowIndex': yson.YsonUint64(0),
        'RecordTime': yson.YsonUint64(1562860888),
        'HasNextRow': True,
    },
    {
        'Url': 'https://vk.com/video395771395_456239382',
        'Hash': '80386BC04BDF317289664F64BC0A8D5926CABC95FEFA81D37830E8CF188233B4D11D44F01A9D002F9A6A5185154BE26DE340851698058FBC5486997EC3346D5A',
        'ContentType': 'EAudio',
        'RowIndex': yson.YsonUint64(1),
        'RecordTime': yson.YsonUint64(1562860888),
        'HasNextRow': False,
    },
    # Old hash
    {
        'Url': 'https://video.mail.ru/mail/voron2785/_myvideo/305.html',
        'Hash': '3F06820696F6A8200F6E96EF674DE529230B912BF6B8D3E0F9B9A0E6A98830D7BBCDBCB1FC2AB278D16928DF5ABD7F135E23E163A31D79FD2D5DDCBD9C8B908E',
        'ContentType': 'EVideo',
        'RowIndex': yson.YsonUint64(0),
        'RecordTime': yson.YsonUint64(1562843445),
        'HasNextRow': False,
    },
    # Wrong shard
    {
        'Url': 'https://xhamster.com/videos/webcam-7494341',
        'Hash': '000DBA32FBB481B33A9319AA4A549417F7CFC6A08859A34FAD4BF2BE17C95E419F497A5AACDC6EE4EDDBA45E2298FC801158C217CB8A528D12C48480D74BAA50',
        'ContentType': 'EAudio',
        'RowIndex': yson.YsonUint64(0),
        'RecordTime': yson.YsonUint64(1562648976),
        'HasNextRow': False,
    },
]

KEYS = [
    # Old hash
    {
        'Hash': '3F06820696F6A8200F6E96EF674DE529230B912BF6B8D3E0F9B9A0E6A98830D7BBCDBCB1FC2AB278D16928DF5ABD7F135E23E163A31D79FD2D5DDCBD9C8B908E',
        'ContentType': 'EVideo',
        'RowIndex': yson.YsonUint64(0),
        'RecordTime': yson.YsonUint64(1530951832),
        'HasNextRow': False,
    },
    # Just another hash
    {
        'Hash': 'AC932810FD69D8126C787116FCD13FA26F36D3E3F738A4C9A99022F70B42C43D760F4CE9723AF856CF20C7B7CAB36B9E7F1094B7BE783578212975AC09791000',
        'ContentType': 'EAudio',
        'RowIndex': yson.YsonUint64(0),
        'RecordTime': yson.YsonUint64(1519841656),
        'HasNextRow': False,
    },
]
