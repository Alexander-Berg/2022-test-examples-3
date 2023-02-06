#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 6;

BEGIN { use_ok( 'Models::Banner' ); }

use utf8;
use open ':std' => ':utf8';

*has = \&Models::Banner::has_phraseid_param;

ok(has('http://ya.ru/yandsearch?c={CampaignID}&p1={param1}&p2={param2}&b={BannerID}&s={source}&ba={BannerID}&a={AdID}&agi={AdGroupID}&pid={PhraseID}'), 'full params list');

ok(!has('http://ya.ru/yandsearch'), 'no param');

ok(!has('http://ya.ru/yandsearch?c={CampaignID}&p1={param1}&p2={param2}&b={BannerID}'), 'no param as well');

ok(has('http://ya.ru/yandsearch?c={CampaignID}&p1={param1}&pid={PARAM127}&p2={param2}&b={BannerID}'), 'href already substituted');

ok(has('http://kremlin.ru/?c={CampaignID}&p1={param1}&p2={param2}&b={BannerID}&pid={param127}'), 'href already substituted (2)');
