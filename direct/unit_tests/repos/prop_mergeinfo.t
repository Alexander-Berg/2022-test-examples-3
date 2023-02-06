#!/usr/bin/perl

# проверка на то, что атрибут mergeinfo есть только у корня

use warnings;
use strict;

use XML::LibXML;
use Test::More skip_all => 'не дружит с Арканумом: https://st.yandex-team.ru/DIRECT-95593';
use Test::More tests => 1;

use Settings;

my $DIR = $Settings::ROOT;

# пропускаем все externals
my $xml = `svn propget -R --xml svn:mergeinfo $DIR`;
my $doc = XML::LibXML->new()->parse_string($xml)->documentElement();            
my @paths = grep {$_ ne $DIR} map {$_->getAttribute('path')} $doc->findnodes('/properties/target');
ok(!@paths, "Attribute svn:mergeinfo set for paths: ".join(", ", @paths));
