#!/usr/bin/perl

use Direct::Modern;
use Test::More;

BEGIN { use_ok( 'Direct::Validation::Domains' ); }

use utf8;
use open ':std' => ':utf8';

*v = \&Direct::Validation::Domains::is_valid_app_id;

ok(v('ru.yandex.maps'));
ok(v('ru.yandex.di_rec-t'));
ok(v('ru-RU.yandex.sprav'));
ok(v('ru.yandex.33maps'));

ok(!v('id012345679'));
ok(!v('i012345679'));
ok(!v('id012345679z'));

ok(!v('ru.yandex.-maps'));
ok(!v('ru.yandex.карты'));
ok(!v('ruyandex'));
ok(!v('1ru.yandex'));
ok(!v('123456'));

# с фичой disable_number_id_and_short_bundle_id_allowed, позволяющей запрещать числовые и короткие id
ok(v('123456', 1));
ok(v('yabro', 1));
ok(v('imiphone', 1));
ok(v('id12345679', 1));

ok(!v('__app', 1));
ok(!v('.app', 1));

done_testing();
