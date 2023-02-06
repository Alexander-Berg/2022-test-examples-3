#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;
use utf8;

use Test::More;
use Yandex::Test::UTF8Builder;

use Models::Phrase qw/validate_phrase_one_href_param/;

my @valid_params = (
    "параметр с символами разных языков 12345.678 test \x{454}\x{456} \x{4E8}\x{4A2}",
    'x' x 255,
);

my @invalid_params = (
    'параметр со спецсимволами символами! $$$ &@"',
    "параметр с переводом\nстроки в середине",
    "параметр с переводом строки в конце\n",
    'x' x 256,
);

Test::More::plan(tests => scalar(@valid_params) + scalar(@invalid_params));

for my $param (@valid_params) {
    is(validate_phrase_one_href_param($param), undef, "параметр '$param' даёт ошибку");
}

for my $param (@invalid_params) {
    isnt(validate_phrase_one_href_param($param), undef, "параметр '$param' НЕ даёт ошибку");
}
