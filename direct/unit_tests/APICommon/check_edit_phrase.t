#!/usr/bin/perl

use Direct::Modern;

use Storable qw/dclone/;
use Test::More tests => 16;
use Test::Deep;

use APICommon qw/check_edit_phrase/;

{
    no warnings 'redefine';
    *ModerateChecks::check_moderate_phrase = sub { my ($new_phrase, $old_phrase) = @_; return $new_phrase eq $old_phrase ? 0 : 1 };
}

my $old_phrase = {
    phrase => 'phrase text',
    price => 0.41,
    autobudgetPriority => 3
};
my $new_phrase = {
    phrase => 'phrase text',
    price => 0.41,
    autobudgetPriority => 'Medium'
};

cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 0, edit_phrase => 0 }, 'same phrase data with price');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'YND_FIXED'), { flag => 1, edit_phrase => 0 }, 'new phrase price in y.e.');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'YND_FIXED', 'RUB'), { flag => 1, edit_phrase => 0 }, 'old phrase price in y.e.');

$new_phrase = {
    phrase => 'phrase text',
    price => 0.02,
    autobudgetPriority => 'Medium'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'YND_FIXED'), { flag => 0, edit_phrase => 0 }, 'new phrase price in y.e. same as old price in rub');

$old_phrase = {
    phrase => 'phrase text',
    price => 0,
    autobudgetPriority => 3
};
$new_phrase = {
    phrase => 'phrase text',
    price => 0,
    autobudgetPriority => 'Medium'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'YND_FIXED'), { flag => 0, edit_phrase => 0 }, 'new phrase zero price in y.e.');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'YND_FIXED', 'RUB'), { flag => 0, edit_phrase => 0 }, 'old phrase zero price in y.e.');

$new_phrase = {
    phrase => 'phrase text',
    price => 0.51,
    autobudgetPriority => 'Medium'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 1, edit_phrase => 0 }, 'changed price in new phrase');

$new_phrase = {
    phrase => 'other phrase text',
    price => 0.41,
    autobudgetPriority => 'Medium'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 1, edit_phrase => 1 }, 'changed phrase text');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'YND_FIXED'), { flag => 1, edit_phrase => 1 }, 'changed phrase text and new price in y.e.');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'YND_FIXED', 'RUB'), { flag => 1, edit_phrase => 1 }, 'changed phrase text and old price in y.e.');


$old_phrase = {
    phrase => 'phrase text',
    price_context => 0.41,
    autobudgetPriority => 3
};
$new_phrase = {
    phrase => 'phrase text',
    price_context => 0.41,
    autobudgetPriority => 'Medium'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 0, edit_phrase => 0 }, 'same phrase data with context_price');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'YND_FIXED'), { flag => 1, edit_phrase => 0 }, 'new phrase context_price in y.e.');
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'YND_FIXED', 'RUB'), { flag => 1, edit_phrase => 0 }, 'old phrase context_price in y.e.');

$new_phrase = {
    phrase => 'phrase text',
    price_context => 0.51,
    autobudgetPriority => 'Medium'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 1, edit_phrase => 0 }, 'old phrase context_price in y.e.');

$new_phrase = {
    phrase => 'phrase text',
    price_context => 0.41,
    autobudgetPriority => 'Low'
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 1, edit_phrase => 0 }, 'changed priority in new phrase');

$new_phrase = {
    phrase => 'phrase text',
    price_context => 0.41,
};
cmp_deeply(check_edit_phrase(dclone($new_phrase), dclone($old_phrase), 'RUB', 'RUB'), { flag => 0, edit_phrase => 0 }, 'new phrase without priority');
