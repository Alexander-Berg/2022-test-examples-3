#!/usr/bin/perl
use my_inc "../..";
use Direct::Modern;

use Test::More;
use Test::Deep;
use JSON;

use Yandex::DBTools;
use Test::CreateDBObjects;
use Test::Subtest;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

use PrimitivesIds;
use Settings;
use BS::TrafaretAuction;
use Currencies;

=head1 DESCRIPTION

https://wiki.yandex-team.ru/users/zaitceva/Stavki-i-Strategii/

=cut

BEGIN {
    use_ok 'Campaign';
}

my $guarantee = 42;
my $GUARANTEE_30 = sprintf "%.2f", $guarantee * 1.3;

{
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
    undef &BS::TrafaretAuction::trafaret_auction;
    *BS::TrafaretAuction::trafaret_auction = sub {
        my $banners = shift;
        for my $banner (@$banners) {
            for my $ph (@{$banner->{phrases}}) {
                $ph->{guarantee} = [ map { { bid_price => $guarantee * 1e6, amnesty_price => $guarantee * 1e6 } } (1..4)];
                $ph->{bs_data_exists} = 1;
            }
        }
    };
    undef &ADVQ6::advq_get_phrases_shows_multi;
    *ADVQ6::advq_get_phrases_shows_multi = sub { return '' };
}

create_tables();

my $manual_search = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"name":"default"},"net":{"name":"stop"},"is_net_stop":1}'));
my $auto_search = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"avg_bid":42,"sum":"","name":"autobudget_avg_click"},"net":{"name":"stop"},"is_net_stop":1}'));
my $manual_net = Campaign::get_canonical_strategy(from_json('{"search":{"name":"stop"},"net":{"name":"maximum_coverage"},"name":"different_places","is_net_stop":0}'));
my $auto_net = Campaign::get_canonical_strategy(from_json('{"search":{"name":"stop"},"net":{"name":"autobudget_avg_click","avg_bid":42,"sum":""},"name":"different_places","is_net_stop":0}'));
my $auto_all = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"avg_bid":50,"sum":"","name":"autobudget_avg_click"},"net":{"name":"default"},"is_net_stop":0}'));
my $manual_all = Campaign::get_canonical_strategy(from_json('{"name":"","search":{"name":"default"},"net":{"name":"default"},"is_net_stop":0}'));
my $manual_diff_places = Campaign::get_canonical_strategy(from_json('{"net":{"name":"maximum_coverage"},"search":{"name":"default"},"name":"different_places","is_net_stop":0}'));

my $DEFAULT_PRICE = sprintf "%.2f", get_currency_constant(RUB => 'DEFAULT_PRICE');

sub _is_price_context
{
    my ($cid, $expected, $desc) = @_;
    $desc //= "price_context == $expected";
    is_one_field(PPC(cid => $cid), ['select price_context from bids', where => { cid => $cid }], $expected, $desc);
}

sub _is_price
{
    my ($cid, $expected, $desc) = @_;
    $desc //= "price == $expected";
    is_one_field(PPC(cid => $cid), ['select price from bids', where => { cid => $cid }], $expected, $desc);
}

sub _set_strategy
{
    my ($cid, $strategy) = @_;
    
    my $camp = get_camp_info($cid);
    $camp->{strategy} = Campaign::campaign_strategy($camp);

    Campaign::camp_set_strategy( $camp, $strategy, { uid => get_uid(cid => $cid) });
}

sub _create_camp
{
    my ($strategy, $type) = @_;
    $type //= 'text';
    my $user = create('user');
    my $cid = Campaign::create_empty_camp(
        type => $type, currency => 'RUB', client_chief_uid => $user->{uid}, ClientID => $user->{ClientID},
        client_fio => 'Test testovich', client_email => 'example@example.com',
    );

    _set_strategy($cid, $strategy);
    
    return $cid;
}

subtest_ 'case1' => sub {
    # -ручная стратегия-на поиске
    my $cid = _create_camp($manual_search);
    # -создаем группу со ставкой на поиске 5 руб. 
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 5.00 }]);
    # -меняем стратегию: авто - на поиске
    _set_strategy($cid, $auto_search);
    # -меняем стратегию: ручная - сети
    _set_strategy($cid, $manual_net);

    # ОР: в сетях проставится ставка 5 руб.
    _is_price_context($cid, '5.00');
};

subtest_ 'case2' => sub {
    # -ручная стратегия-на поиске
    my $cid = _create_camp($manual_search);
    # -создаем группу со ставкой 5 руб. 
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 5.00, price_context => 0 }]);
    # -меняем стратегию: ручная - сети
    _set_strategy($cid, $manual_net);
    # ОР: в сетях проставится ставка 5 руб.
    _is_price_context($cid, '5.00');
};

subtest_ 'case3' => sub {
    # -ручная стратегия-на поиске 
    # -меняем стратегию: авто - на поиске
    my $cid = _create_camp($auto_search);
    # -создаем группу 
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 0 }]);
    # -меняем стратегию: ручная-сети
    _set_strategy($cid, $manual_net);
    # ОР: в сетях проставится ставка по-умолчанию 3 руб.
    _is_price_context($cid, $DEFAULT_PRICE);
};

subtest_ 'case4' => sub {
=head2 doc
-авто стратегия-на поиске 
-создаем группу 
-меняем стратегию: ручная-на поиске
Фактический результат сейчас: проставится ставка 3 руб. 
ОР: Если у фразы не было выставленной ручной ставки (ни в сетях, ни на поиске), то для такой фразы в ручной стратегии-на поиске проставлять вход ставку "Прогноз входа в Гарантию + 30%" (если есть статистика по фразе), если статистики по фразе нет то выставляется ставка по умолчанию - 3 рубля.
=cut
    my $cid = _create_camp($auto_search);
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 0 }]);
    _set_strategy($cid, $manual_search);
    _is_price($cid, $GUARANTEE_30);
};

subtest_ 'case5' => sub {
    # -ручная стратегия-в сетях 
    my $cid = _create_camp($manual_net);
    # -создаем группу со ставкой 5 руб. 
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 5 }]);
    # -меняем стратегию: авто - в сетях, заполняем manual_prices
    _set_strategy($cid, $auto_net);
    # -меняем стратегию: ручная - поиск, восстанавливаем price_context из manual_prices, копируем ставку из сети на поиск
    _set_strategy($cid, $manual_search);
    # ОР: в ручной стратегии на поиске проставится ставка 5 руб.
    _is_price($cid, '5.00');
};

subtest_ 'case_DIRECT-68729' => sub {
# подключить стратегию: ручное управление, поиск
    my $cid = _create_camp($manual_search);
# создать группу, ставка на поиске 22
    create('group', cid => $cid, phrases => [ {phrase => 'test', price => 22, price_context => 0} ]);
# переключить стратегию: автостратегия средняя цена клика, только в сетях, сохранили manual_prices
    _set_strategy($cid, $auto_net);
# переключить стратегию: ручное управление - на поиске, восстановили из manual_prices, а затем нужно не затереть ее при копировании ставки из сети
    _set_strategy($cid, $manual_search);
# Результат: ставка на поиске 0.3 ИЛИ ставка на поиске 0.3 , ставка на сети 0.3
# ОР: ставка на поиске 22 ИЛИ ставки на поиске 22, ставка на сети 22
    _is_price($cid, '22.00');
};

subtest_ 'case6' => sub {
=head2 doc
-ручная стратегия-в сетях 
-меняем стратегию: авто - в сетях
-создаем группу (автоматом проставляется 3 рубля на втором шаге создания РО) 
-меняем стратегию: ручная - поиск
Видео: https://jing.yandex-team.ru/files/zaitceva/screencast_2017-06-23_17-57-51.mp4
Фактический результат сейчас: в ручной стратегии на поиске проставится ставка 3 руб.
ОР (по аналогии с кейсом 4): Если у фразы не было выставленной ручной ставки (ни в сетях, ни на поиске), то для такой фразы в ручной стратегии-на поиске проставлять вход ставку "Прогноз входа в Гарантию + 30%". Нужно понять, почему при создании группы в автоматической стратегии-в сетях проставляется (записывается) 3 руб.
=cut
    my $cid = _create_camp($auto_net);
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 0 }]);
    _set_strategy($cid, $manual_search);
    _is_price($cid, $GUARANTEE_30);
};

subtest_ 'case7_1' => sub {
=head2 doc
-ручная стратегия-на всех площадках (без раздельного управления)
-создаем группу со ставкой 50 руб. 
-меняем стратегию: авто - на всех площадках
-меняем стратегию: ручная - сети
ОР1: Если на шаге 1 в параметрах кампании заданы стандартные настройки в сетях (100% от ставки на поиске) - в сетях проставится ставка 50 руб,
=cut
    my $cid = _create_camp($manual_all);
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 50, price_context => 0 }]);
    _set_strategy($cid, $auto_all);
    _set_strategy($cid, $manual_net);
    _is_price_context($cid, '50.00');
};

subtest_ 'case7_2' => sub {
=head2 doc
-ручная стратегия-на всех площадках (без раздельного управления)
-создаем группу со ставкой 50 руб. 
-меняем стратегию: авто - на всех площадках
-меняем стратегию: ручная - сети
ОР2: Если на шаге 1 в параметрах кампании заданы настройки в сетях (например, 10% от ставки на поиске) - в сетях проставится ставка 5 руб (10% от 50 рублей)
=cut
    my $cid = _create_camp($manual_all);
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 50, price_context => 0 }]);
    _set_strategy($cid, $auto_all);
    do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
    _set_strategy($cid, $manual_diff_places);
    _is_price($cid, '50.00');
    _is_price_context($cid, '5.00');
};

subtest_ 'case8' => sub {
# -ручная стратегия-в сетях 
    my $cid = _create_camp($manual_net);
# -создаем группу со ставкой 5 руб. 
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 5 }]);
# -меняем стратегию: авто - сети; save_manual_prices(null, 5)
    _set_strategy($cid, $auto_net);
# -ручная стратегия-на всех площадках (раздельное управление), restore_manual_prices(null, 5), copy_prices(5)
    _set_strategy($cid, $manual_diff_places);
# ОР: в ручной стратегии на поиске и в сетях проставится ставка по 5 руб.
    _is_price_context($cid, '5.00');
    _is_price($cid, '5.00');
};

subtest_ 'case9' => sub {
# -авто стратегия-на поиске 
    my $cid = _create_camp($auto_net);
# -создаем группу 
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 0 }]);
# -меняем на ручную стратегию-в сетях
    _set_strategy($cid, $manual_net);
# ОР: в ручной стратегии-в сетях проставится ставка по-умолчанию - 3 руб.
    _is_price_context($cid, $DEFAULT_PRICE);
};

subtest_ 'case9_1' => sub {
    # автостратегия (не важно: в сети, на поиске, на всех площадках)
    my $cid = _create_camp($auto_all);
    # создать группу
    create('group', cid => $cid, phrases => [ { phrase => 'test phrase', price => 0, price_context => 0 } ]);
    # переключить стратегию: ручное управление на всех площадках (раздельное)
    _set_strategy($cid, $manual_diff_places);
    # ставка на поиске: гарантия+30%, ставка в сетях: 3 руб
    _is_price($cid, $GUARANTEE_30);
    _is_price_context($cid, $DEFAULT_PRICE);
};

=begin doc

Логика кейсов 10-11-12
Не наследуем вручную проставленную ставку с сетей на поиск, если ранее для фразы была задана ставка для поиска в рамках ручной стратегии. 
Наследуем вручную проставленную ставку с сетей на поиск, если ранее для фразы НЕ была задана ставка для поиска в рамках ручной стратегии.

=cut

subtest_ 'case010' => sub {
# -ручная стратегия на всех площадках (совместное управление)
    my $cid =_create_camp($manual_all);
# -создаем группу со ставкой 5 рублей
    create('group', cid => $cid, phrases => [ { phrase => 'test phrase', price => 5, price_context => 5 } ]);
# -меняем на ручную стратегию-в сетях 
    _set_strategy($cid, $manual_net);
# -ставим ставку в сетях 10 руб
    do_update_table(PPC(cid => $cid), 'bids', { price_context => 10 }, where => { cid => $cid });
# -меняем на стратегию на всех площадках (раздельное управление)
    _set_strategy($cid, $manual_diff_places);
# ОР: ставка на поиске 5 руб/ставка в сетях 10 руб
    _is_price($cid, '5.00');
    _is_price_context($cid, '10.00');
};

subtest_ 'case011' => sub {
# -ручная стратегия на поиск
    my $cid =_create_camp($manual_search);
# -создаем группу со ставкой 5 рублей
    create('group', cid => $cid, phrases => [ { phrase => 'test phrase', price => 5, price_context => 0 } ]);
# -меняем на ручную стратегию-в сетях 
    _set_strategy($cid, $manual_net);
# -меняем ставку в сетях 10 руб
    do_update_table(PPC(cid => $cid), 'bids', { price_context => 10 }, where => { cid => $cid });
# -меняем на стратегию на всех площадках (раздельное управление)
    _set_strategy($cid, $manual_diff_places);
# ОР: ставка на поиске 5 руб/ставка в сетях 10 руб
    _is_price($cid, '5.00');
    _is_price_context($cid, '10.00');
};

subtest_ 'case012' => sub {
# -авто стратегия на поиск
    my $cid =_create_camp($auto_search);
# -создаем группу (ставка проставляется системой в рамках авто стратегии)
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 0 }]);
# -меняем на ручную стратегию-в сетях 
    do_sql(PPC(cid => $cid), "select 666");
    _set_strategy($cid, $manual_net);
# -ставим ставку в сетях 10 руб
    do_update_table(PPC(cid => $cid), 'bids', { price_context => 10 }, where => { cid => $cid });
# -меняем на стратегию на всех площадках (раздельное управление)
    _set_strategy($cid, $manual_diff_places);
# ОР: ставка на поиске 10 руб/ставка в сетях 10 руб
    _is_price($cid, '10.00');
    _is_price_context($cid, '10.00');
};

subtest_ 'DIRECT-70107' => sub {
    subtest_ 'case1' => sub {
        # Устанавливаю стратегию Ручное управление (раздельное управление)
        my $cid = _create_camp($manual_diff_places);
        # Создаю новую группу, указываю вручную ставку для поиска и сетей (поиск - 11 рублей, сети - 22 рубля)
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 11, price_context => 22 }]);
        # Меняю стратегию на автобюджетную (Средняя цена клика) на всех площадках, указываю максимальную цену клика в сетях 10% от цены на поиске
        _set_strategy($cid, $auto_all);
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
        # Затем меняю стратегию на Ручное управление Раздельное управление или Сети
        _set_strategy($cid, $manual_diff_places);
        # Результат: ставка в сетях равна первоначально установленной ставке в сетях
        # Ожидается: установится 10% ставки на поиске
        # (в соответсвии с таблицей https://wiki.yandex-team.ru/users/sonch/soxranenie-stavki-pri-perekljuchenii-poisk-seti/)
        _is_price($cid, '11.00');
        _is_price_context($cid, '1.10');
    };
    subtest_ case2 => sub {
        # Раздельное управление -> Автобюджетная Все площадки 10% -> Сети
        my $cid = _create_camp($manual_diff_places);
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 5, price_context => 10 }]);
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
        _set_strategy($cid, $auto_all); # save_manual_prices
        _set_strategy($cid, $manual_net); # restore_manual_prices, set_initial_prices
        _is_price_context($cid, '0.50');
        # Ожидается: в сетях - 10% от первоначальной цены на поиске
        # Результат: в сетях - первоначальная цена в сетях
    };
    subtest_ case3 => sub {
        # Раздельное управление -> Автобюджетная Все площадки (без ограницения цены клика в сетях) -> Раздельное управление
        my $cid = _create_camp($manual_diff_places);
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 5, price_context => 10 }]);
        _set_strategy($cid, $auto_all);
        _set_strategy($cid, $manual_diff_places);
        # Ожидается: на поиске - первоначальная цена с поиска, в сетях - первоначальная цена в сетях
        _is_price($cid, '5.00');
        _is_price_context($cid, '10.00');
        # Результат: на поиске - первоначальная цена с поиска, в сетях - первоначальная цена на поиске
    };
    subtest_ case4 => sub {
# Сети -> Автобюджетная Все площадки 10% -> Раздельное управление
        my $cid = _create_camp($manual_net);
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 10 }]);
        _set_strategy($cid, $auto_all);
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
        _set_strategy($cid, $manual_diff_places);
        _is_price($cid, '10.00');
        _is_price_context($cid, '1.00');
# Ожидается: на поиске - первоначальная цена на сети, в сетях - 10% от первоначальной цены на сети
# Результат: на поиске - первоначальная цена на сети, в сетях - дефолтная ставка
    };
    subtest_ case5 => sub {
# Сети -> Автобюджетная Все площадки 10% -> Сети
        my $cid = _create_camp($manual_net);
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0, price_context => 10 }]);
        _set_strategy($cid, $auto_all);
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
        _set_strategy($cid, $manual_net);
        _is_price_context($cid, '10.00');
# Ожидается: в сетях - первоначальная цена в сетях (т.к. при переключении нет поисковой ставки)
# Результат: в сетях - дефолтная ставка
    };
    subtest_ case6 => sub {
        my $cid = _create_camp($manual_search);
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 22, price_context => 0 }]);
        _set_strategy($cid, $auto_all);
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
        _set_strategy($cid, $manual_net);
        _is_price_context($cid, '2.20');
    };

    subtest_ case7 => sub {
        my $cid = _create_camp($manual_search);
        create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 22, price_context => 0 }]);
        _set_strategy($cid, $auto_all);
        do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
        _set_strategy($cid, $manual_diff_places);
        _is_price_context($cid, '2.20');
    };
};

subtest_ 'DIRECT-71951' => sub {
# Устанавливаю стратегию Ручное управление Все площадки или Ручное управление Поиск
    my $cid = _create_camp($manual_all);
# Создаю новое объявление, указываю ставку 12 рублей
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 12, price_context => 0 }]);
# Переключаю стратегию на Ручное управление Сети
    _set_strategy($cid, $manual_net);
# Ожидалось: в сетях ставка 12 рублей
    _is_price_context($cid, '12.00');
# Результат: ставка 3 рубля (дефолтная)
# Должна быть такая же логика как ТГО https://wiki.yandex-team.ru/users/sonch/soxranenie-stavki-pri-perekljuchenii-poisk-seti/
};

subtest_ 'DIRECT-71944' => sub {
    # Указываю стратегию Средняя цена клика На всех площадках
    my $cid = _create_camp($auto_all);
    # В Настройках в сети указываю максимальную цену клика 10% от цены на поиске
    do_update_table(PPC(cid => $cid), 'campaigns', { ContextPriceCoef => 10 }, where => { cid => $cid });
    # Создаю новое объявление с несколькими фразами
    create('group', cid => $cid, phrases => [{ phrase => 'test phrase', price => 0 }]);
    # # Переключаю стратегию на Ручное управление Раздельное управление площадками
    _set_strategy($cid, $manual_diff_places);
    # # Ожидается: ставка на поиске - цена певого места + 30%, ставка в сети - дефолтная (3 рубля)
    _is_price($cid, $GUARANTEE_30);
    _is_price_context($cid, $DEFAULT_PRICE);
    # Результат: ставка на поиске - цена певого места + 30%, ставка в сети - 10% от цены на поиске
    # Если у фразы ранее не была проставлена ставка, то в сетях должна проставляться дефолтная
};


run_subtests();
