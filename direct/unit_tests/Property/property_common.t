#! /usr/bin/perl

=head1 property_common.t

    Тест проверяет на общую работоспособность модуль Property

=cut

use strict;
use Test::More;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

use Property;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

my $dataset = {
    ppc_properties => {
        original_db => PPCDICT
    },
};
init_test_dataset($dataset);

my $prop_name = '__test_prop_' . int(rand(10_000));
my $prop = Property->new($prop_name);
my $prop2 = Property->new($prop_name);

$prop->set('123');
is($prop->get(), '123', "correct value get after set");
$prop->set('124');
is($prop->get(), '124', "correct value get after set - 2");

$prop->cas('125');
is($prop->get(), '125', "correct value get after cas (last_value not changed)");

$prop->delete();
$prop->cas('125');
is($prop->get(), '125', "correct value get after cas (on undef prev value after delete)");

my $prop2 = Property->new($prop_name);
$prop2->set('123');
$prop->cas('126');
is($prop->get(), '123', "correct value get after cas (after change property value by third parties)");


dies_ok(sub { $prop = Property->new($prop_name); $prop->cas('126')}, "die when last_value is not determined");

$prop = Property->new($prop_name);
$prop->set(undef);
$prop->cas('125');
is($prop->get(), '125', "correct value get after cas (on undef prev value after set)");

# caching
$prop2->set("122");
is($prop->get(), "122");
$prop2->set("123");
is($prop->get(60), "122");
is($prop->get(0), "123");

$prop->delete();

done_testing();
