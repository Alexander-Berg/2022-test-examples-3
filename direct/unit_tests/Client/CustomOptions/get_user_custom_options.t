#!/usr/bin/perl

use strict;
use Test::More;
use Test::Deep;
use Settings;
use Yandex::DBUnitTest qw/:all/;
BEGIN { use_ok('Client::CustomOptions') };

my %db = (
    clients_custom_options => {
        original_db => PPC(shard => 'all'),
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 1 },
        ],
    },
);
init_test_dataset(\%db);

set_user_custom_options(1, { option1 => 1, option2 => 1 });
set_user_custom_options(2, { option1 => 0 });

cmp_deeply(get_user_custom_options(1), { option1 => 1, option2 => 1 });
cmp_deeply(get_user_custom_options(2), { option1 => 0 });

set_user_custom_options(1, { option1 => 2, option2 => 3 });

cmp_deeply(get_user_custom_options(1), { option1 => 2, option2 => 3 });

delete_user_custom_options(1, 'option1');
cmp_deeply(get_user_custom_options(1), { option2 => 3 });

delete_user_custom_options(1, 'option2');
cmp_deeply(get_user_custom_options(1), { });

done_testing();
