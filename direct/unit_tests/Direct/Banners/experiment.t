#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;

use Settings;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::BannersAdditions');
}


Direct::Test::DBObjects->create_tables();
my $db_obj = Direct::Test::DBObjects->new()->with_adgroup('text');

my $bid1 = $db_obj->create_banner()->id;
my $bid2 = $db_obj->create_banner()->id;

my $experiment = '{"test_param":1,"cool_param":"lol"}';
my $new_experiment = '{"test_param":2,"cool_param":"lil"}';

# Создание эксперимента
_set_and_check($db_obj, $bid1, $experiment);
_set_and_check($db_obj, $bid2, $experiment);

# Обновление эксперимента
# заменяем старый эксперимент на тот же самый эксперимент
_set_status_bs_sync($db_obj->get_banner($bid1), 'Yes');
_set_and_check($db_obj, $bid1, $experiment);
is $db_obj->get_banner($bid1)->experiment, $experiment, "didn't change";
is $db_obj->get_banner($bid1)->status_bs_synced, 'Yes', "status bs sync didn't change";
# заменяем старый эксперимент на новый
_set_status_bs_sync($db_obj->get_banner($bid2), 'Yes');
_set_and_check($db_obj, $bid2, $new_experiment);
is $db_obj->get_banner($bid2)->status_bs_synced, 'No', "change";

# Удаление экперимента
# Передали пустую строку
_set_status_bs_sync($db_obj->get_banner($bid1), 'Yes');
_set_and_check($db_obj, $bid1, '');
is $db_obj->get_banner($bid1)->status_bs_synced, 'No', "status bs sync change";
# Передали undef
_set_status_bs_sync($db_obj->get_banner($bid2), 'Yes');
_set_and_check($db_obj, $bid2, undef);
is $db_obj->get_banner($bid2)->status_bs_synced, 'No', "status bs sync change";

done_testing;


sub _set_and_check {
    my ($db_obj, $bid, $experiment) = @_;
    Direct::BannersAdditions::save_banners_experiments({$bid => $experiment});
    my $banner = $db_obj->get_banner($bid);
    if ($experiment) {
        is $banner->experiment, $experiment, "set <$experiment> for bid=$bid";
    }
    else {
        is $banner->experiment, undef, "disable for bid=$bid";
    }

    return;
}

sub _set_status_bs_sync {
    my ($banner, $status) = @_;

    $banner->set_db_column_value('banners', 'statusBsSynced', $status);
    $banner->manager_class->new(items => [$banner])->update();
}
