use Direct::Modern;
use Test::More;
use JSON;

use Yandex::DBUnitTest qw/:all/;
use Settings;

BEGIN {
    use_ok('Direct::Model::CanvasCreative');
}

copy_table(PPCDICT, 'ppc_properties');

my $creative = Direct::Model::CanvasCreative->new({
   id => 871,
   _moderate_info => undef,
});
is($creative->detect_lang, 'ru');
                           
$creative->_moderate_info(encode_json({}));
is($creative->detect_lang, 'ru');

$creative->_moderate_info(encode_json({texts => [{text => "Не является публичной оффертой"}]}));
is($creative->detect_lang, 'ru');

done_testing;


