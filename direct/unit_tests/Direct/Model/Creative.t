#!/usr/bin/env perl

use Direct::Modern;

use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use my_inc '../../../';

use Settings;

BEGIN {
    use_ok 'Direct::Model::Creative';
}



sub mk_creative { return Direct::Model::Creative->new(@_) }

subtest 'Creative Model' => sub {
    lives_ok { mk_creative() } 'just created';
    dies_ok  { mk_creative(unknown => 'args') } 'dies when wrong args given';
    is ( mk_creative(template_id => 320)->to_hash()->{preview_scale}, 0.5, 'preview_scale definition');

    #source_media_type
    ok ( !exists mk_creative(template_id => 320)->to_hash()->{source_media_type}, 'source_media_type not exists in to_hash result');
    is ( mk_creative()->source_media_type, undef, 'source_media_type default is undef if accessed');
    is ( mk_creative(source_media_type => undef)->source_media_type, undef, 'source_media_type could be set to undef');
    dies_ok { mk_creative(source_media_type => 'gif') } 'source_media_type can not be anything than undef';

    dies_ok  { mk_creative()->preview_scale } 'dies when template_id not defined';
    dies_ok  { mk_creative(template_id => 000)->preview_scale } 'no preview_scale for template';

};


done_testing;
