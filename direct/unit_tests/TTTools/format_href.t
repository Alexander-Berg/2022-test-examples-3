#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More;

use TTTools;

my $OPEN_STAT = '_openstat=dGVzdDsxOzE7';

is( TTTools::format_href('http://ya.ru/', 'Yes'), 
    , "http://ya.ru/?$OPEN_STAT"
    , 'flag Yes');

is( TTTools::format_href('http://ya.ru/', 'No'), 
    , "http://ya.ru/"
    , 'flag No');

is( TTTools::format_href('http://ya.ru/'), 
    , "http://ya.ru/"
    , 'flag undefined');

is( TTTools::format_href('ya.ru/'), 
    , "http://ya.ru/"
    , 'protocol addition');

is( TTTools::format_href('ya.ru/asdf?qwe=12', 'Yes'), 
    , "http://ya.ru/asdf?qwe=12&$OPEN_STAT"
    , 'with param');

is( TTTools::format_href('ya.ru/asdf#qwe', 'Yes'), 
    , "http://ya.ru/asdf?$OPEN_STAT#qwe"
    , 'with anchor');

is( TTTools::format_href('ya.ru/asd?qweq=12#qwe', 'Yes'), 
    , "http://ya.ru/asd?qweq=12&$OPEN_STAT#qwe"
    , 'with param & anchor');

is( TTTools::format_href('ya.ru/?', 'Yes'), 
    , "http://ya.ru/?&$OPEN_STAT"
    , 'with question mark');

is( TTTools::format_href('ya.ru/#test#/asd?qweq=12#qwe', 'Yes'), 
    , "http://ya.ru/test/asd?qweq=12&$OPEN_STAT#qwe"
    , 'with param & template & anchor');
    
is( TTTools::format_href('ya.ru/#test#/asdf?qwe=12', 'Yes'), 
    , "http://ya.ru/test/asdf?qwe=12&$OPEN_STAT"
    , 'with param & template');    
    
is( TTTools::format_href('direct.yandex.ru/direct/#990509#', 'Yes'), 
    , "http://direct.yandex.ru/direct/990509?$OPEN_STAT"
    , 'with param & template');    
    
is( TTTools::format_href('direct.yandex.ru/direct/#990509?utm_medium=cpc', 'Yes'), 
    , "http://direct.yandex.ru/direct/?$OPEN_STAT#990509?utm_medium=cpc"
    , 'with param & anchor');

done_testing;
