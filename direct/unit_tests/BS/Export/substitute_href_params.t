#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 7;

BEGIN { use_ok( 'BS::Export' ); }

use utf8;
use open ':std' => ':utf8';

*prochref = \&BS::Export::substitute_href_params;

is(prochref('http://ya.ru/yandsearch?c={CampaignID}&p1={param1}&p2={param2}&b={BannerID}&s={source}&ba={BannerID}&a={AdID}&agi={AdGroupID}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}',
            {campaignid => 400, bannerid => 100500, adid => 100500, adgroupid => 600}, 'text')
    , 'http://ya.ru/yandsearch?c=400&p1={param1}&p2={param2}&b=100500&s={source}&ba=100500&a=100500&agi=600&bannerid=100500&adid=100500&banner_id=100500&ad_id=100500'
    , 'full params list for text campaign');

is(prochref('http://ya.ru/yandsearch?p={POS}&p1={AdID}',
        {campaignid => 400, bannerid => 100500, adid => 100500, adgroupid => 600}, 'text')
    , 'http://ya.ru/yandsearch?p={POS}&p1=100500'
    , 'one param, one placeholder');

is(prochref('http://ya.ru/yandsearch?text=test', undef, 'text')
    , 'http://ya.ru/yandsearch?text=test'
    , 'without params');

# для перфомансов и динамиков параметры про баннер не должны раскрываться (они будут раскрыты до БКшного макроса {PEID})
is(prochref('http://ya.ru/yandsearch?c={Campaign_ID}&p1={param1}&p2={param2}&b={BAnNeR_ID}&s={source}&ba={BannerID}&a={aD_iD}&agi={AdGroup_ID}',
            {campaignid => 400, bannerid => 100500, adid => 100500, adgroupid => 600}, 'text')
    , 'http://ya.ru/yandsearch?c=400&p1={param1}&p2={param2}&b=100500&s={source}&ba=100500&a=100500&agi=600'
    , 'full params list awful case');

is(prochref('http://ya.ru/yandsearch?c={CampaignID}&p1={param1}&p2={param2}&b={BannerID}&s={source}&ba={BannerID}&a={AdID}&agi={AdGroupID}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}',
            {campaignid => 400, bannerid => 100500, adid => 100500, adgroupid => 600}, 'performance')
    , 'http://ya.ru/yandsearch?c=400&p1={param1}&p2={param2}&b={BannerID}&s={source}&ba={BannerID}&a={AdID}&agi=600&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}'
    , 'full params list for performance campaign');

is(prochref('http://ya.ru/yandsearch?c={CampaignID}&p1={param1}&p2={param2}&b={BannerID}&s={source}&ba={BannerID}&a={AdID}&agi={AdGroupID}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}',
            {campaignid => 400, bannerid => 100500, adid => 100500, adgroupid => 600}, 'dynamic')
    , 'http://ya.ru/yandsearch?c=400&p1={param1}&p2={param2}&b={BannerID}&s={source}&ba={BannerID}&a={AdID}&agi=600&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}'
    , 'full params list for dynamic campaign');
