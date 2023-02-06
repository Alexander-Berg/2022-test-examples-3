#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;

use Settings;

use Yandex::DBUnitTest qw/:all/;
use Yandex::DBShards;
use Yandex::HashUtils qw/hash_cut/;

use Direct::Test::DBObjects;

BEGIN {
    use_ok('Direct::Model::Banner');
    use_ok('Direct::Banners');
}

sub mk_group { Direct::Model::AdGroup->new(@_ )}
sub mk_banner { Direct::Model::Banner->new(@_ ) }

subtest 'Href coef_goal_context_id group status_bs_synced' => sub {
        Direct::Test::DBObjects->create_tables;

        my $uid = 1;

        no warnings 'redefine';
        local *Direct::Banners::get_new_id_multi = sub {
            my ($key, $cnt, $chain_key, $chain_val) = @_;
            state $next_id = 1;
            is_deeply({$key => $cnt, $chain_key => $chain_val}, {bid => $cnt, uid => $uid});
            return [map { $next_id++ } 1..$cnt];
        };
        use warnings 'redefine';

        subtest 'Prepare create banner' => sub {
                my $banner = mk_banner(title => 'banner title', body => 'banner_body', href => 'https://ya.ru',
                    domain => 'ya.ru', status_moderate => 'New', status_post_moderate => 'New', vcard_status_moderate => 'New',
                    sitelinks_status_moderate => 'New', client_id => '1');
                $banner->adgroup(mk_group(status_moderate => 'New', has_show_conditions => 1, geo=> "225"));

                $banner->href("https://ya.ru/test={coef_goal_context_id}");

                Direct::Banners->new([$banner])->prepare_create($uid);
                is($banner->do_bs_sync_adgroup, 1);
            };

        subtest 'Prepare update banner' => sub {
                my $banner = mk_banner(id => 1, title => 'banner title', title_extension => undef, body => 'banner_body', href => 'https://ya.ru',
                    domain => 'ya.ru', status_moderate => 'New', status_post_moderate => 'New', vcard_status_moderate => 'New',
                    sitelinks_status_moderate => 'New');
                $banner->adgroup(mk_group(status_moderate => 'New', has_show_conditions => 1));
                $banner->old($banner->clone);

                $banner->href("https://ya.ru/test={coef_goal_context_id}");

                Direct::Banners->new([$banner])->prepare_update();
                is($banner->do_bs_sync_adgroup, 1);
            };

        subtest 'Prepare create banner with punycode domain' => sub {
                my $data = [
                    {href => 'http://xn----7sbbdi4aqddevcb0bpdx9lyc.xn--p1ai/uslugi/', domain => undef, expected_domain => 'стоматология-улыбка.рф'},
                    {href => 'http://www.ремонт-ванн.рф/nalivnaya-stakrilovaya-vanna/', domain => undef, expected_domain => 'www.ремонт-ванн.рф'},
                    {href => "https://ya.ru/test={coef_goal_context_id}", expected_domain => 'ya.ru'},
                    {href => "https://ya.ru/test={coef_goal_context_id}", domain => 'test-domain.qqq.ru', expected_domain => 'test-domain.qqq.ru'},
                    {href => "https://ya.ru/test={coef_goal_context_id}", domain => 'стоматология-улыбка.рф', expected_domain => 'стоматология-улыбка.рф'},
                    {href => "https://ya.ru/test={coef_goal_context_id}", domain => 'xn----7sbbdi4aqddevcb0bpdx9lyc.xn--p1ai', expected_domain => 'стоматология-улыбка.рф'},
                ];
                foreach my $d (@$data) {
                    my $banner = mk_banner(title => 'banner title', body => 'banner_body', status_moderate => 'New', status_post_moderate => 'New', vcard_status_moderate => 'New', sitelinks_status_moderate => 'New', client_id => '1', %{ hash_cut($d, qw/href domain/) });
                    $banner->adgroup(mk_group(status_moderate => 'New', has_show_conditions => 1, geo=> "225"));
                    Direct::Banners->new([$banner])->prepare_create($uid);
 
                    is_deeply(
                        hash_cut($banner->to_db_hash, qw/href domain reverse_domain statusBsSynced statusModerate statusPostModerate/), {
                        'href' => $d->{href},
                        'domain' => $d->{expected_domain},
                        'reverse_domain' => scalar(reverse $d->{expected_domain}),
                        'statusBsSynced' => 'No',
                        'statusModerate' => 'New',
                        'statusPostModerate' => 'No',
                    });
                }
            };

        subtest 'Prepare update banner with punycode domain' => sub {
                my $data = [
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'https://ya.ru/test={coef_goal_context_id}', old_domain => 'ya.ru', new_domain => 'ya.ru', expected_domain => 'ya.ru', 'statusBsSynced' => 'Yes', 'statusModerate' => 'Yes'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'https://ya.ru/test={coef_goal_context_id}', old_domain => 'ya.ru', new_domain => '', expected_domain => 'ya.ru', 'statusBsSynced' => 'Yes', 'statusModerate' => 'Yes'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'https://ya.ru/test={coef_goal_context_id}', old_domain => 'ya.ru', new_domain => 'www2.qqq.ккк.ru', expected_domain => 'www2.qqq.ккк.ru', 'statusBsSynced' => 'No', 'statusModerate' => 'Ready'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'http://xn----7sbbdi4aqddevcb0bpdx9lyc.xn--p1ai/uslugi/', old_domain => 'ya.ru', new_domain => undef, expected_domain => 'стоматология-улыбка.рф', 'statusBsSynced' => 'No', 'statusModerate' => 'Ready'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'http://www.ремонт-ванн.рф/nalivnaya-stakrilovaya-vanna/', old_domain => 'ya.ru', new_domain => undef, expected_domain => 'www.ремонт-ванн.рф', 'statusBsSynced' => 'No', 'statusModerate' => 'Ready'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'http://xn----7sbbdi4aqddevcb0bpdx9lyc.xn--p1ai/uslugi/', old_domain => 'ya.ru', new_domain => 'стоматология-улыбка.рф', expected_domain => 'стоматология-улыбка.рф', 'statusBsSynced' => 'No', 'statusModerate' => 'Ready'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'http://www.ремонт-ванн.рф/nalivnaya-stakrilovaya-vanna/', old_domain => 'ya.ru', new_domain => 'стоматология-улыбка.рф', expected_domain => 'стоматология-улыбка.рф', 'statusBsSynced' => 'No', 'statusModerate' => 'Ready'},
                    {old_href => 'https://ya.ru/test={coef_goal_context_id}', new_href => 'http://www.ремонт-ванн.рф/nalivnaya-stakrilovaya-vanna/', old_domain => 'ya.ru', new_domain => 'xn----7sbbdi4aqddevcb0bpdx9lyc.xn--p1ai', expected_domain => 'стоматология-улыбка.рф', 'statusBsSynced' => 'No', 'statusModerate' => 'Ready'},
                ];
                foreach my $d (@$data) {
                    my $banner = mk_banner(id => 1, title => 'banner title', title_extension => undef, body => 'banner_body', status_moderate => 'Yes', status_post_moderate => 'No', status_bs_synced => 'Yes', href => $d->{old_href}, domain => $d->{old_domain},
                    );
                    $banner->adgroup(mk_group(status_moderate => 'New', has_show_conditions => 1));
                    $banner->old($banner->clone);
 
                    $banner->href($d->{new_href});
                    $banner->domain($d->{new_domain});
                    Direct::Banners->new([$banner])->prepare_update();
 
                    is_deeply(
                        hash_cut($banner->to_db_hash, qw/href domain reverse_domain statusBsSynced statusModerate statusPostModerate/), {
                        'href' => $d->{new_href},
                        'domain' => $d->{expected_domain},
                        'reverse_domain' => scalar(reverse $d->{expected_domain}),
                        'statusBsSynced' => $d->{statusBsSynced},
                        'statusModerate' => $d->{statusModerate},
                        'statusPostModerate' => 'No',
                    });
                }
            };
    };

done_testing;
