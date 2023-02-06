#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Test::More tests => 5;
use Test::Deep;
use Yandex::Test::UTF8Builder;

use Storable qw/dclone/;

BEGIN { use_ok( 'PhraseText', 'ensure_phrase_have_props' ); }

my $phrase_with_all_properties = {
    phrase => 'окна пвх',
    numword => 2,
    norm_phrase => 'окно пвх',
    norm_hash => '9595796445660914630',
    md5 => '852b1e06d09323c6ec2e5346f34d3017',
};

# если у фразы уже есть свойства, то ничего измениться не должно
cmp_deeply( ensure_phrase_have_props(dclone($phrase_with_all_properties)), $phrase_with_all_properties, 'есть все свойства' );

# если не хватает каких-то обязательных свойств, то должны заполниться все
my $phrase_with_part_of_properties = dclone($phrase_with_all_properties);
delete @{$phrase_with_part_of_properties}{qw/numword md5 norm_hash/};
cmp_deeply( ensure_phrase_have_props($phrase_with_part_of_properties), $phrase_with_all_properties, 'нет части свойств' );

# если не хватает только norm_hash, то дополняется он только при наличии with_norm_hash
my $phrase_without_norm_hash = dclone($phrase_with_all_properties);
delete $phrase_without_norm_hash->{norm_hash};
cmp_deeply( ensure_phrase_have_props(dclone($phrase_without_norm_hash)), $phrase_without_norm_hash, 'нет norm_hash и НЕ указано with_norm_hash' );
cmp_deeply( ensure_phrase_have_props(dclone($phrase_without_norm_hash), with_norm_hash => 1), $phrase_with_all_properties, 'нет norm_hash и указано with_norm_hash' );
