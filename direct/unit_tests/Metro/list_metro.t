use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;
use Test::Exception;


sub load_modules: Tests(startup => 1) {
    use_ok 'Metro';
}

sub l {
    &Metro::list_metro;
}

sub city_without_metro_doesnt_die : Test(2) {
    lives_ok {
        is_deeply l('Железногорск (Красноярский край)'), [];
    }
}

sub big_cities_by_name : Test(3) {
    ok scalar @{l('Москва')};
    ok scalar @{l('Санкт-Петербург')};
    ok scalar @{l('Киев')};
}

sub big_cities_by_id : Test(3) {
    ok scalar @{l(1)};
    ok scalar @{l(2)};
    ok scalar @{l(143)};
}

__PACKAGE__->runtests();
