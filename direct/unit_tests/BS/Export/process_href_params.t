#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 8;

BEGIN { use_ok( 'BS::Export' ); }

use utf8;
use open ':std' => ':utf8';

*prochref = \&BS::Export::process_href_params;

is(prochref('http://ya.ru/yandsearch?p={position}&p1={param1}&p2={param2}&phrase={PhraseID}&pt={position_type}&s={source}&st={source_type}&bm={addphrases}&kwd={keyword}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}', 'text')
    , 'http://ya.ru/yandsearch?p={POS}&p1={PARAM1}&p2={PARAM2}&phrase={PHRASE_EXPORT_ID}&pt={PTYPE}&s={SRC}&st={STYPE}&bm={BM}&kwd={PHRASE}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}'
    , 'full params list for text campaign');

is(prochref('http://ya.ru/yandsearch?p={position}&p1=param1', 'text')
    , 'http://ya.ru/yandsearch?p={POS}&p1=param1'
    , 'one param');

is(prochref('http://ya.ru/yandsearch?text=test', 'text')
    , 'http://ya.ru/yandsearch?text=test'
    , 'without params');

is(prochref('http://ya.ru/yandsearch?p={poSiTion}&p1={paRam1}&p2={Param2}&ret={retargeting_id}&phrase={phrase_id}&pt={position_type}&s={source}&st={sourCe_type}&bm={adDphrases}&kwd={keyword}', 'text')
    , 'http://ya.ru/yandsearch?p={POS}&p1={PARAM1}&p2={PARAM2}&ret={PARAM126}&phrase={PHRASE_EXPORT_ID}&pt={PTYPE}&s={SRC}&st={STYPE}&bm={BM}&kwd={PHRASE}'
    , 'full params list in awful case');

is(prochref('http://ya.ru/yandsearch?p={poSiTion}&p1={paRam1}&p2={Param2}&ret={retargeting_id}&phrase={phrase_id}&pt={position_type}&s={source}&st={sourCe_type}&bm={adDphrases}&kwd={keyword}', 'text')
    , 'http://ya.ru/yandsearch?p={POS}&p1={PARAM1}&p2={PARAM2}&ret={PARAM126}&phrase={PHRASE_EXPORT_ID}&pt={PTYPE}&s={SRC}&st={STYPE}&bm={BM}&kwd={PHRASE}'
    , 'full params list in awful case');

# для перфомансов и динамиков параметры про номер баннера должны раскрываться в {PEID}
is(prochref('http://ya.ru/yandsearch?p={position}&p1={param1}&p2={param2}&phrase={PhraseID}&pt={position_type}&s={source}&st={source_type}&bm={addphrases}&kwd={keyword}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}', 'performance')
    , 'http://ya.ru/yandsearch?p={POS}&p1={PARAM1}&p2={PARAM2}&phrase={PHRASE_EXPORT_ID}&pt={PTYPE}&s={SRC}&st={STYPE}&bm={BM}&kwd={PHRASE}&bannerid={PEID}&adid={PEID}&banner_id={PEID}&ad_id={PEID}'
    , 'full params list for performance campaign');

is(prochref('http://ya.ru/yandsearch?p={position}&p1={param1}&p2={param2}&phrase={PhraseID}&pt={position_type}&s={source}&st={source_type}&bm={addphrases}&kwd={keyword}&bannerid={bannerid}&adid={adid}&banner_id={banner_id}&ad_id={ad_id}', 'dynamic')
    , 'http://ya.ru/yandsearch?p={POS}&p1={PARAM1}&p2={PARAM2}&phrase={PHRASE_EXPORT_ID}&pt={PTYPE}&s={SRC}&st={STYPE}&bm={BM}&kwd={PHRASE}&bannerid={PEID}&adid={PEID}&banner_id={PEID}&ad_id={PEID}'
    , 'full params list for dynamic campaign');
