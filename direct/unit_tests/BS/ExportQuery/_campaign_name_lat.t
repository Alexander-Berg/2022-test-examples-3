#!/usr/bin/perl

use utf8;

use Test::More;

use BS::ExportQuery qw/_campaign_name_lat/;


is(BS::ExportQuery::_campaign_name_lat('russia RUSSIA'), 'russia_RUSSIA');
is(BS::ExportQuery::_campaign_name_lat('ßßß ẞẞẞ'), '_');

done_testing();

