#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 11;

use OrgDetails qw/validate_ogrn/;


is (validate_ogrn("1027739358778"), 1, 'Valid OGRN');
is (validate_ogrn("7027739358778"), 0, 'First number is invalid');
is (validate_ogrn("10277r935t778"), 0, 'Has letters');
is (validate_ogrn("102477455778"), 0,  'Short number');
is (validate_ogrn("10247745577845"), 0, 'Long number (14)');
is (validate_ogrn("10247745577843455"), 0, 'Long number (17)');
is (validate_ogrn("1027739019204"), 0, 'Wrong checksum (OGRN)');
is (validate_ogrn("1037723007960"), 1, 'Diff is 10 (but check num is 0)');

is (validate_ogrn("304540220800032"), 1, 'Valid OGRNIP');
is (validate_ogrn("3045402208000t32"), 0, 'Letter after 11 symbols');
is (validate_ogrn("310253706101022"), 0, 'Wrong checksum (OGRNIP)');
