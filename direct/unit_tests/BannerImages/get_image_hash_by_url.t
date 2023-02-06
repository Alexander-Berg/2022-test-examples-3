use Direct::Modern;
use Test::More;
use Test::Deep;

use BannerImages;

cmp_deeply(
    [get_image_hash_by_url("")],
    []
);

cmp_deeply(
    [get_image_hash_by_url("https://direct.yandex.ru/images/direct-picture/45743/kTic_6NvAn-OQB7sQb7baQ")],
    ["direct-picture", "45743", "kTic_6NvAn-OQB7sQb7baQ"]
);

cmp_deeply(
    [get_image_hash_by_url("https://direct.yandex.ru/images/direct/45743/kTic_6NvAn-OQB7sQb7baQ")],
    ["direct", "45743", "kTic_6NvAn-OQB7sQb7baQ"]
);

cmp_deeply(
    [get_image_hash_by_url("https://direct.yandex.ru/images/unknown-namespace/45743/kTic_6NvAn-OQB7sQb7baQ")],
    [],
    'unknown namespace',
);

cmp_deeply(
    [get_image_hash_by_url("https://direct.yandex.ru/images/direct/aaaaa/kTic_6NvAn-OQB7sQb7baQ")],
    [],
    'non-digit group id',
);


cmp_deeply(
    [get_image_hash_by_url("http://direct.yandex.ru/images/o7PHIAXTkxjs0dR6heGCwQ")],
    [undef, undef, "o7PHIAXTkxjs0dR6heGCwQ"]
);

cmp_deeply(
    [get_image_hash_by_url("https://direct.yandex.ru/images/jfjUUE73grFCBq41993tyr")],
    [undef, undef, "jfjUUE73grFCBq41993tyr"]
);

cmp_deeply(
    [get_image_hash_by_url("https://direct.yandex.ru/images/")],
    []
);

cmp_deeply(
    [get_image_hash_by_url("https://example.com/images/image.jpg")],
    []
);


done_testing;
