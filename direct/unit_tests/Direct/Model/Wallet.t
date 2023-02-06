#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;

use Yandex::DBUnitTest qw/:all/;
use Direct::Test::DBObjects;
use Settings;


BEGIN {
    use_ok('Direct::Model::Wallet');
    use_ok('Direct::Model::Wallet::Manager');
    use_ok('Direct::Model::Autopay');
}

sub mk_wallet { Direct::Model::Wallet->new(@_) };

subtest "Model" => sub {
    lives_ok { mk_wallet() };
};

subtest 'Wallet Manager' => sub {
	Direct::Test::DBObjects->create_tables;

	my $db_obj = Direct::Test::DBObjects->new()->with_user();
	my $user = $db_obj->user;

     subtest 'update wallet' => sub {
 		my $wallet = $db_obj->create_wallet({user_id => $db_obj->user->id, autopay_mode=>'min_balance', paymethod_id=>123456789, paymethod_type=>'card', remaining_sum=>100, payment_sum=>200, person_id => 1});
 		Direct::Model::Wallet::Manager->new(items => [$wallet])->update();
        cmp_model_with($wallet, $db_obj->get_wallets($wallet->id)->[0]);
     };
};

done_testing;
