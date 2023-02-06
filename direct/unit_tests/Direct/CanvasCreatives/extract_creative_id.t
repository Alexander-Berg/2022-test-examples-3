use Direct::Modern;
use Test::More;
use Direct::CanvasCreatives;

sub extract_creative_id {
    return Direct::CanvasCreatives->extract_creative_id(shift);
}


is(extract_creative_id("https://canvas.yandex.ru/creatives/1073787752/preview"), 1073787752);
is(extract_creative_id("http://canvas.yandex.ru/creatives/000/1073787752"), 1073787752);
is(extract_creative_id(""), undef);
is(extract_creative_id("http://canvas.yandex.ru/creatives/preview"), undef);
is(extract_creative_id("asdasd939kkljalsd239993/preview"), undef);
is(extract_creative_id("https://direct.yandex.com/images/direct-picture/98965/5UT_jYCSnqG5prUMgeqzmg"), undef);


done_testing;
