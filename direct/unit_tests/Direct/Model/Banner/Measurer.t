use Direct::Modern;

use Test::More;
use Test::Exception;
use Test::Deep;

BEGIN {
    use_ok('Direct::Model::Banner::Measurer');
}

sub mk_measurer { Direct::Model::Banner::Measurer->new(@_) }

subtest model => sub {
    lives_ok { mk_measurer() };
    lives_ok { mk_measurer(banner_id => 674, measurer_system => 'moat') };
    dies_ok { mk_measurer(banner_id => 674, measurer_system => 'abcdq') };
};

subtest admetrica_params => sub {
    cmp_deeply
      mk_measurer(banner_id => 1, campaign_id => 23, adgroup_id => 50, creative_id => 8, measurer_system => 'admetrica', params => '{"campaign-id": 788, "creative-id": 712}')->expand_params,
      {"campaign-id" => 788, "creative-id" => 712};
};

subtest moat_params => sub {
    cmp_deeply
      mk_measurer(client_id => 5123, campaign_id => 200, adgroup_id => 764, creative_id => 14, measurer_system => 'moat', banner_id => 10, params => '{}')->expand_params,
      {moatClientLevel1 => '5123', moatClientLevel2 => '200', moatClientLevel3 => '764', moatClientLevel4 => '10', partnerCode => "yandexhostedvideo786420153684"};

    cmp_deeply
      mk_measurer(client_id => 5123, campaign_id => 200, adgroup_id => 764, creative_id => undef, measurer_system => 'moat', banner_id => 10, params => '{}')->expand_params,
      {moatClientLevel1 => '5123', moatClientLevel2 => '200', moatClientLevel3 => '764', moatClientLevel4 => '10', partnerCode => "yandexhostedvideo786420153684"};

   cmp_deeply
      mk_measurer(client_id => 519, campaign_id => 12, adgroup_id => 59, creative_id => 28, measurer_system => 'moat', banner_id => 10, params => '{"moat_id": 635512, "ssid": "jGYEH551gDGFQ"}')->expand_params,
      {moatClientLevel1 => '519', moatClientLevel2 => '12', moatClientLevel3 => '59', moatClientLevel4 => '10', moat_id => 635512, ssid => "jGYEH551gDGFQ", partnerCode => "yandexhostedvideo786420153684"};

   cmp_deeply
      mk_measurer(client_id => 5123, campaign_id => 200, adgroup_id => 764, creative_id => 45, measurer_system => 'moat', banner_id => 23, params => '{"moatClientLevel3": "641", "moatClientLevel4": "505"}')->expand_params,
      {moatClientLevel1 => '5123', moatClientLevel2 => '200', moatClientLevel3 => '641', moatClientLevel4 => '505', partnerCode => "yandexhostedvideo786420153684"};
};

subtest adloox_params => sub {
    cmp_deeply
      mk_measurer(client_id => 5123, campaign_id => 200, measurer_system => 'adloox', banner_id => 10, adgroup_type => 'cpm_banner', params => '{}')->expand_params,
      {id2 => '200', id5 => '10', id7 => '5123', tagid => "880" , creatype => "2"};

   cmp_deeply
      mk_measurer(client_id => 519, campaign_id => 12, adgroup_id => 59, creative_id => 28, adgroup_type => 'cpm_banner', measurer_system => 'adloox', banner_id => 10, params => '{"ssid": "jGYEH551gDGFQ"}')->expand_params,
      {"ssid" => "jGYEH551gDGFQ", id2 => '12', id5 => '10', id7 => '519', tagid => "880" , creatype => "2"};

   cmp_deeply
      mk_measurer(client_id => 5123, campaign_id => 200, adgroup_id => 764, creative_id => 45, adgroup_type => 'cpm_video', measurer_system => 'adloox', banner_id => 10, params => '{"id2": "641", "id7": "505"}')->expand_params,
      {id2 => '641', id5 => '10', id7 => '505', tagid => "881" , creatype => "6"};

   cmp_deeply
      mk_measurer(client_id => 5123, campaign_id => 200, adgroup_id => 764, creative_id => 45, adgroup_type => 'cpm_audio', measurer_system => 'adloox', banner_id => 10, params => '{"id2": "641", "id7": "505"}')->expand_params,
      {id2 => '641', id5 => '10', id7 => '505', tagid => "880" , creatype => "2"};

};


done_testing;
