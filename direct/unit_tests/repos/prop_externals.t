#!/usr/bin/perl

# проверка на то, что все исходники в utf8

use warnings;
use strict;

use XML::LibXML;
use List::MoreUtils qw/any/;
use Test::More skip_all => "externals deleted before moving to Arcadia: ARCADIA-1050";

use Settings;

my $DIR = $Settings::ROOT;

# пропускаем все externals
my $xml = `svn propget -R --xml svn:externals $DIR`;
my $doc = XML::LibXML->new()->parse_string($xml)->documentElement();            
my @targets = $doc->findnodes("/properties/target");            
Test::More::plan(tests => scalar(@targets) * 2);

my @externals_formats = (
    qr/^((?:[\w\.\-\/]+)\s+-r\d+\s+.*\n)+$/ms,
    qr/^\s*$/ms,
    qr/^(?:-r\s*\d+\s+[\w\.\-\/\:]+\s+[\w\.\-\/]+\s*\n)+$/ms,
    qr/^(?:[\w\.\-\/]+\s+[\w\.\-\/\:\+]+\s*\n)+$/ms,
);

my @known_externals = (
    qr#^\s*$#ms,
    qr#//svn\.yandex\.ru/lego/versions/2\.7/blocks\s+lego/blocks$#,
    qr#//svn\.yandex\.ru/lego/versions/2\.7/tools\s+lego/tools$#,
    qr#//svn\.yandex\.ru/direct-moderate/trunk/protected/lists/\s+wordlists$#,
);

for my $t (@targets) {
    my $path = $t->getAttribute('path');
    my $value = $t->findvalue("./property/text()");
    ok((any { $value =~ $_ } @externals_formats), "svn:externals for $path ($value)");
    ok((any { $value =~ $_ } @known_externals), "'$value' (set on $path) is known svn external");
}
