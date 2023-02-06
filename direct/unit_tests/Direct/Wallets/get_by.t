use Direct::Modern;

use Test::More;
use Test::Deep;

use Direct::Test::DBObjects;
use Direct::Wallets;

Direct::Test::DBObjects->create_tables();

subtest 'get_by campaign_id' => sub {
    my $dbo = Direct::Test::DBObjects->new->with_user;
    my $w1 = $dbo->create_wallet;
    my $w2 = $dbo->create_wallet;

    my $items = Direct::Wallets->get_by(campaign_id => $w2->id)->items;
    is(@$items, 1);
    my $hash = $items->[0]->to_hash();
    cmp_deeply($hash, superhashof(
        {
            id => $w2->id,
            user_id => $dbo->user->id,
            client_id => $dbo->user->client_id,
            agency_id => 0,
            agency_user_id => undef,
            manager_user_id => undef,
            currency => 'RUB',
        }
    ));
};

subtest 'get_by client_id' => sub {
    my $dbo1 = Direct::Test::DBObjects->new->with_user;
    $dbo1->create_wallet;
    $dbo1->create_wallet;

    my $dbo2 = Direct::Test::DBObjects->new->with_user;
    my $w1 = $dbo2->create_wallet;
    my $w2 = $dbo2->create_wallet;

    my $wallets = Direct::Wallets->get_by(client_id => $dbo2->user->client_id);
    is(@{$wallets->items}, 2);

    my $wallets_by_id = $wallets->items_by('campaign_id');
    is($wallets_by_id->{$w1->id}->id, $w1->id);
    is($wallets_by_id->{$w2->id}->id, $w2->id);
};

done_testing();
