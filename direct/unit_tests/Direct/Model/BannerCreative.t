#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;


BEGIN {
    use_ok('Direct::Model::BannerCreative');
    use_ok('Direct::Model::BannerCreative::Manager');
}


sub mk_banner { Direct::Model::BannerCreative->new( @_ ) }


subtest 'BannerCreative Model' => sub {
        lives_ok { mk_banner() };
        lives_ok { mk_banner({}) };
        dies_ok { mk_banner("unknown" => "args") };

        lives_ok { mk_banner(creative_id => 1) };
        lives_ok { mk_banner(creative => bless({ }, 'Direct::Model::CanvasCreative')) };
    };

done_testing();

