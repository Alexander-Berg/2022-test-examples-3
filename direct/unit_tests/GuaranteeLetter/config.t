#!/usr/bin/perl

=pod

    $Id$

    Проверяет разные внутренние настройки модуля генерации гарантийных писем

=cut

use warnings;
use strict;

use utf8;
use Encode qw/find_encoding/;

use Test::More;
use Test::Deep;

use GuaranteeLetter;

my $regions = GuaranteeLetter::_get_regions();
my $encodings = GuaranteeLetter::_get_encodings();
my $topics = GuaranteeLetter::_get_topics_hash();

Test::More::plan(tests => ((keys %$encodings) + 2));

sub check_default_topicid {
    foreach my $region (@$regions) {
        return 0 unless $topics->{$GuaranteeLetter::DEFAULT_TOPIC_ID}->{region}->{$region};
    }

    return 1;
}


# Для "регионов" (шаблонов) задана существующая кодировка
while (my ($region, $enc) = each %$encodings) {
    ok(find_encoding($enc), "valid encoding for region $region");   
}

# Заданы кодировки для всех "регионов" (и нет лишних)
cmp_bag($regions, [keys %$encodings], 'Encoding for all regions');

# Проверим, что тематика "по-умолчанию" есть у всех "регионов"
ok(check_default_topicid, 'default topic id is available for all regions')
