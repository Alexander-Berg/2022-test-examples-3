#!/usr/bin/perl

use strict;
use warnings;

use Test::More;

use Yandex::Test::UTF8Builder;
use utf8;

use Tag qw/check_tag_text/;

my @valid_tags = (
    'нормальный тэг',
    'ме\"%-_&+*:$;test',
    '-+. "!?\\()%$;',
    ';:/&\'*_=#№«»–—−',
    "\x{00a0}",
);
my @invalid_tags = (
    "тег с\nпереводом строки",
    "тег с переводом строки в конце\n",
);

Test::More::plan(tests => scalar(@valid_tags) + scalar(@invalid_tags));

for my $tag (@valid_tags) {
   is(check_tag_text($tag), undef, qq!метка "$tag" должна проходить валидацию!);
}
for my $tag (@invalid_tags) {
    isnt(check_tag_text($tag), undef, qq!метка "$tag" НЕ должна проходить валидацию!);
}
