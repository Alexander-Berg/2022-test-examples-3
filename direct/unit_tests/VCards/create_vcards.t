#!/usr/bin/env perl
use 5.14.0;
use strict;
use warnings;
use utf8;
use Carp;

use base qw/Test::Class/;
use Test::More;
use Data::Dumper;

use Settings;
use Yandex::DBShards;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use Test::JavaIntapiMocks::GenerateObjectIds;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

sub load_modules: Tests(startup => 1) {
    use_ok 'VCards';
}

sub create_test_database: Tests(startup) {
    my %db = (
        vcards => {
            original_db => PPC(shard => 'all'),
        },
        users => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [{ClientID => 1, uid => 1 }],
            },
        },
        shard_client_id => {
            original_db => PPCDICT,
            rows => [
                {ClientID => 1, shard => 1 },
            ],
        },
        shard_inc_cid => {
            original_db => PPCDICT,
            rows => [
                {ClientID => 1, cid => 1},
            ],
        },
        shard_uid => {
            original_db => PPCDICT,
            rows => [
                {uid => 1, ClientID => 1 },
            ],
        },
        shard_inc_vcard_id => {
            original_db => PPCDICT,
            rows => [
                # initial auto-increment
                {vcard_id => 99, ClientID => 0 },
            ],
        },
    );
    init_test_dataset(\%db);
}

# Проверяем, что все неоднозначности в Perl-овых структурах данных и в БД не приводят к размножению визиток.
sub test_all_dupe_combinations: Test(290) {
    my %vcard = (
        uid => 1,
        cid => 1,
        phone => '8#800#888-88-88',
    );
    create_vcards(1, [\%vcard]);
    is get_one_field_sql(PPC(uid => 1), "select count(*) from vcards where uid = 1"), 1;

    for my $org_details_id_in_db (0, undef) {
        for my $name_in_db ('', undef) {
            do_update_table(
                PPC(uid => 1), 'vcards',
                {
                    org_details_id => $org_details_id_in_db,
                    name => $name_in_db,
                },
                where => {uid => 1}
            );

            for my $org_details_id ('', undef, 0) {
                for my $geo_id ('', undef, 0) {
                    for my $metro ('', undef) {
                        for my $name ('', undef) {
                            my %vcard_for_saving = (
                                %vcard,
                                org_details_id => $org_details_id,
                                geo_id => $geo_id,
                                metro => $metro,
                                name => $name,
                            );
                            create_vcards(1, [\%vcard_for_saving]);
                            local $Data::Dumper::Indent = 0;
                            is $vcard_for_saving{duplicate}, 1, "Duplicate on " . Dumper(\%vcard);
                            is $vcard_for_saving{vcard_id}, 100, "Vcard id on " . Dumper(\%vcard);
                        }
                    }
                }
            }
        }
    }
}

__PACKAGE__->runtests();
