#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Test::More;

use Yandex::Test::UTF8Builder;
use Settings;

BEGIN { use_ok('Models::AdGroup', 'get_first_excess_phrase_idx'); }

my $max = $Settings::DEFAULT_KEYWORD_COUNT_LIMIT;
*fep = sub {
    my ($p, %O) = @_;
    $O{limit} = $max  if !%O;
    Models::AdGroup::get_first_excess_phrase_idx($p, %O);
};

# если фразы укладываются в 4096 символов, должен возвращаться индекс -1
is(fep(get_banner_with_phrases([])), -1, 'пустой баннер');
is(fep(get_banner_with_phrases(['phrase1', 'phrase number2'])), -1, 'нормальный баннер с фразами, не вылезающими за ограничение');
is(fep(get_banner_with_phrases([('phrase') x $max])), -1, 'баннер с максимальным количеством фраз');
is(fep(get_banner_with_phrases([('phrase') x ($max+1)])), $max, 'баннер с превышением максимального количества фраз');
is(fep(get_banner_with_phrases([('phrase') x $max]), limit => $max-1), $max-1, 'явно указанный лимит');

# undef, если передан не баннер
is(fep({}), undef, 'пустой хеш вместо баннера');
is(fep(undef), undef, 'undef вместо баннера');
is(fep({name => 'test', banners => {}}), undef, 'кампания вместо баннера');

# баннер с фразами другой структуры (phrases => [ {phrase => $phrase_text} ]) должен также пониматься
is(fep(get_banner_with_phrases(['phrase1', 'phrase number2'], phrase_key_name => 'phrase')), -1, 'нормальный баннер другой структуры с фразами, не вылезающими за ограничение');
is(fep(get_banner_with_phrases(['x' x 4096], phrase_key_name => 'phrase')), -1, 'длина единственной фразы в баннере другой структуры ровно 4096 символов');

done_testing();

sub get_banner_with_phrases {
    my ($phrases, %O) = @_;
    my $phrase_key_name = $O{phrase_key_name} || 'phr';
    return {phrases => [map { {$phrase_key_name => $_} } @$phrases]};
}
