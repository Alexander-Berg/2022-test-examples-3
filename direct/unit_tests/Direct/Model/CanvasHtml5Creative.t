#!/usr/bin/env perl

use Direct::Modern;

use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use my_inc '../../../';

use Settings;

BEGIN {
    use_ok 'Direct::Model::CanvasHtml5Creative';
}

lives_ok { mk_creative() } 'just created';
dies_ok  { mk_creative(unknown => 'args') } 'dies when wrong args given';

is ( mk_creative()->source_media_type, undef, 'source_media_type default is undef if accessed');
is ( mk_creative()->to_hash()->{source_media_type}, undef, 'source_media_type exists in to_hash result even if not set in new and is undef');
is ( mk_creative(source_media_type => undef)->source_media_type, undef, 'source_media_type could be set to undef');
is ( mk_creative(source_media_type => 'gif')->source_media_type, 'gif', 'source_media_type could be set to gif');

dies_ok { mk_creative(source_media_type => 'jif') } 'source_media_type can not be anything than known file types';
is ( mk_creative()->source_media_type, undef, 'dies when template_id not defined');

done_testing;

sub mk_creative { return Direct::Model::CanvasHtml5Creative->new(@_) }

