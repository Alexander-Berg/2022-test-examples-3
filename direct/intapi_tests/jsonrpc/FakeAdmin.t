#! /usr/bin/perl 

use strict;
use warnings;

use DateTime;
use JSON;
use YAML;

use my_inc '../../';

use Client qw/ get_client_discount /;
use EnvTools;
use Notification;
use RBAC2::Extended;
use Settings;
use Test::Intapi;
use Yandex::DBTools;
use Yandex::DateTime;
use Yandex::HTTP;
use Yandex::Validate;

use Data::Dumper;

#use Encode;
#use utf8;
#use open ':std' => ':utf8';
#binmode(STDERR, ":utf8");
#binmode(STDOUT, ":utf8");

my $rbac;
unless (is_production() && is_beta()) {
    $rbac = RBAC2::Extended->get_singleton(1);
}

my $url = "/secret-jsonrpc/FakeAdmin";

my @tests = (
    {
        name => 'ok GetUserShard',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{'Login' => 'Elya-Saf'}],
        preprocess => sub {
            return to_json {
                method => "GetUserShard",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            lives_ok {is_valid_int($json->{result})} "$name: is ok";
        }
    },
    {
        name => 'ok FakeGetClientParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{'login' => 'Elya-Saf', fields => []}],
        preprocess => sub {
            return to_json {
                method => "FakeGetClientParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is(ref $json->{result}, 'HASH', "$name: is HASH");
        }
    },
    {
        name => 'ok FakeClientParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{login => 'Elya-Saf', disallow_money_transfer => 1}],
        preprocess => sub {
            return to_json {
                method => "FakeClientParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeGetCampaignParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{ cid => 118252 }],
        preprocess => sub {
            return to_json {
                method => "FakeGetCampaignParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is(ref $json->{result}, 'HASH', "$name: is HASH");
        }
    },
    {
        name => 'ok FakeCampaignParams', 
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{cid => 118252, statusModerate => 'Yes'}],
        preprocess => sub {
            return to_json {
                method => "FakeCampaignParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeGetGroupParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{ pid => 118252 }],
        preprocess => sub {
            return to_json {
                method => "FakeGetGroupParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is(ref $json->{result}, 'HASH', "$name: is HASH");
        }
    },
    {
        name => 'ok FakeGroupParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{pid => 270259, statusModerate => 'New'}],
        preprocess => sub {
            return to_json {
                method => "FakeGroupParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeGetBannerParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{ bid => 270714 }],
        preprocess => sub {
            return to_json {
                method => "FakeGetBannerParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is(ref $json->{result}, 'HASH', "$name: is HASH");
        }
    },
    {
        name => 'ok FakeBannerParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{bid => 270714, statusModerate => 'New'}],
        preprocess => sub {
            return to_json {
                method => "FakeBannerParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeGetPhrasesParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{pid => 270259}],
        preprocess => sub {
            return to_json {
                method => "FakeGetPhrasesParams",
                params => $_[0],
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            my $result = $json->{result};
            is(ref $result, 'ARRAY', "$name: phrases is ARRAY");
            my $phrase = $result->[0];
            is(ref $phrase, 'HASH', "$name: one phrase is HASH");
        }
    },
    {
        name => 'ok FakePhrasesParams',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [[{id => 92884591, statusModerate => 'Yes'}]],
        preprocess => sub {
            return to_json {
                method => "FakePhrasesParams",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok AddEvents',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [[{
            "TextDescription" => "Стиральные машины -2",
            "Timestamp" => "2013-12-03T18:45:54Z",
            "BannerID" => undef,
            "PhraseID" => undef,
            "CampaignID" => 454301,
            "EventType" => "MoneyOut",
            "EventName" => "На кампании закончились деньги"
        }]],
        preprocess => sub {
            return to_json {
                method => "AddEvents",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeUpdateRetargetingGoals',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{ConditionID => 801}],
        preprocess => sub {
            return to_json {
                method => "FakeUpdateRetargetingGoals",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            my $result = $json->{result};
            is(ref $result, 'HASH', "$name: is HASH");
        }
    },
    {
        name => 'ok FakeSetAllowBudgetAccountForAgency',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{Login => 'agency-enable-sa', YesNo => 'Yes'}],
        preprocess => sub {
            return to_json {
                method => "FakeSetAllowBudgetAccountForAgency",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeConvertCurrencyClient',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{
            "AddConvertCurrencyQueue" => 1,
            "ConvertType" => "COPY",
            "ForceConvert" => "1",
            "Login" => "Elya-Saf",
            "TargetCurrency" => "RUB",
            "SetNDS" => 18
        }],
        preprocess => sub {
            return to_json {
                method => "FakeConvertCurrencyClient",
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is($json->{result}, JSON::true, "$name: is ok");
        }
    },
    {
        name => 'ok FakeBalanceNotification',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [{cid => 118252,
                  mcb_campaign => 0,
                  sum_from_balance => 5000,
                }],
        preprocess => sub {
            return to_json {
                method => "FakeBalanceNotification",
                params => $_[0],
            };
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
        }
    },
    {
        name => 'ok FakeBalanceNotificationNDS',
        read_only => 1,
        url => $url,
        method => 'POST',
        data => [[{Login => 'Elya-Saf', VATRate => 13}]],
        preprocess => sub {
            return to_json {
                method => "FakeBalanceNotificationNDS",
                params => $_[0],
            };
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
        }
    },
    {
        name => 'ok SetDiscount',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => [[ 9436942, '33.3300' ]],
        preprocess => sub {
            return to_json {
                method => "SetDiscount",
                params => { $_[0]->[0] => $_[0]->[1] },
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = from_json($resp->content)} "$name: decode JSON";
            ok(!exists $json->{error}, "$name: not error");
            is(get_client_discount($data->[0]), $data->[1], "$name: discount matches");
        },
    },
    {
        name => 'ok FakeClientUnitsBalance',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => [{ client_id => 12345 }],
        preprocess => sub {
            return to_json {
                method => 'FakeClientUnitsBalance',
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok { $json = from_json($resp->content) } "$name: decode JSON";
            ok( !exists $json->{error}, "$name: not error" );
            is( $json->{result}, 24000, 'right result' );
        }
    },
    {
        name => 'ok FakeWithdrawClientUnits',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => [{ client_id => 12345, amount => 5000 }],
        preprocess => sub {
            return to_json {
                method => 'FakeWithdrawClientUnits',
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok { $json = from_json($resp->content) } "$name: decode JSON";
            ok( !exists $json->{error}, "$name: not error" );
            is( $json->{result}, 1, 'right result' );
        }
    },
    {
        name => 'ok FakeClearClientSpentUnits',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => [{ client_id => 12345 }],
        preprocess => sub {
            return to_json {
                method => 'FakeClearClientSpentUnits',
                params => $_[0],
            };
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok { $json = from_json($resp->content) } "$name: decode JSON";
            ok( !exists $json->{error}, "$name: not error" );
            is( $json->{result}, 1, 'right result' );
        }
    },
    {
        name => 'ok ShowMailLogs',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => [
            {
                now  => Yandex::DateTime->now(),
                rand => int(rand(1000000000)),
                cids => [320481406],
            },
            {
                now  => Yandex::DateTime->now(),
                rand => int(rand(1000000000)),
                uids => [282723545],
            },
            {
                now    => Yandex::DateTime->now(),
                rand   => int(rand(1000000000)),
                logins => ['vasay.lohankin'],
            },
            {
                now    => Yandex::DateTime->now(),
                rand   => int(rand(1000000000)),
                emails => ['vasay.lohankin@yandex.ru'],
            },
            {
                now  => Yandex::DateTime->now(),
                rand => int(rand(1000000000)),
                template_names => ['active_orders_money_out'],
            }
        ],
        preprocess => sub {
            my $params = shift;

            Notification::add_notification($rbac, 'active_orders_money_out', {
                    uid           => 282723545,
                    client_uid    => 282723545,
                    fio           => 'Тестеров Тестер Тестерович',
                    cid           => 320481406,
                    campaign_id   => 320481406,
                    camp_name     => 'test campaign '. $params->{rand},
                    campaign_name => 'test campaign '. $params->{rand},
                    client_login  => 'vasay.lohankin',
                    client_email  => 'vasay.lohankin@yandex.ru',
                    client_fio    => 'Тестеров Тестер Тестерович',
                    client_phone  => '+79161234567',
                    client_id     => 9436942,
                    campaign_type => 'text',
                },
                {mail_fio_tt_name => 'fio'}
            );

            return to_json {
                method => 'ShowMailLogs',
                params => {
                    date_from => $params->{now}->ymd(),
                    date_to   => $params->{now}->ymd(),
                    ( defined $params->{cids} ? ( cids => $params->{cids} ) : ()),
                    ( defined $params->{uids} ? ( uids => $params->{uids} ) : ()),
                    ( defined $params->{logins} ? ( logins => $params->{logins} ) : ()),
                    ( defined $params->{emails} ? ( emails => $params->{emails} ) : ()),
                    ( defined $params->{template_names} ? ( template_names => $params->{template_names} ) : ()),
                },
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok { $json = from_json($resp->content) } "$name: decode JSON";
            ok( !exists $json->{error}, "$name: not error" );
            is( ref $json->{result}, 'ARRAY', "$name: is ARRAY" );
            my $rand = $data->{rand};
            ok( !! grep( { $_->{content} =~ m/test campaign $rand/ } @{ $json->{result} } ), 'email exists' );
        }
    },
    {
        name => 'ok ShowSmsLogs',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => [
            {
                now      => Yandex::DateTime->now(),
                sms_time => DateTime->new(
                    year   => 1900 + int(rand(115)),
                    month  => 1 + int(rand(12)),
                    day    => 1,
                ),
                cids     => [320481406],
            },
            {
                now      => Yandex::DateTime->now(),
                sms_time => DateTime->new(
                    year   => 1900 + int(rand(115)),
                    month  => 1 + int(rand(12)),
                    day    => 1,
                ),
                uids     => [282723545],
            },
            {
                now      => Yandex::DateTime->now(),
                sms_time => DateTime->new(
                    year   => 1900 + int(rand(115)),
                    month  => 1 + int(rand(12)),
                    day    => 1,
                ),
                logins   => ['vasay.lohankin'],
            },
            {
                now      => Yandex::DateTime->now(),
                sms_time => DateTime->new(
                    year   => 1900 + int(rand(115)),
                    month  => 1 + int(rand(12)),
                    day    => 1,
                ),
                cids     => [320481406],
                uids     => [282723545],
            },
        ],
        preprocess => sub {
            my $params = shift;

            Notification::add_notification($rbac, 'active_orders_money_out', {
                    uid           => 282723545,
                    client_uid    => 282723545,
                    fio           => 'Тестеров Тестер Тестерович',
                    cid           => 320481406,
                    campaign_id   => 320481406,
                    camp_name     => 'test campaign',
                    campaign_name => 'test campaign',
                    client_login  => 'vasay.lohankin',
                    client_email  => 'vasay.lohankin@yandex.ru',
                    client_fio    => 'Тестеров Тестер Тестерович',
                    client_phone  => '+79161234567',
                    client_id     => 9436942,
                    campaign_type => 'text',
                    sms_time      => $params->{sms_time}->ymd(),
                },
                {mail_fio_tt_name => 'fio'}
            );

            return to_json {
                method => 'ShowSmsLogs',
                params => {
                    date_from => $params->{now}->ymd(),
                    date_to   => $params->{now}->ymd(),
                    ( defined $params->{cids} ? ( cids => $params->{cids} ) : ()),
                    ( defined $params->{uids} ? ( uids => $params->{uids} ) : ()),
                    ( defined $params->{logins} ? ( logins => $params->{logins} ) : ()),
                },
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok { $json = from_json($resp->content) } "$name: decode JSON";
            ok( !exists $json->{error}, "$name: not error" );
            is( ref $json->{result}, 'ARRAY', "$name: is ARRAY" );
            my $sms_time = $data->{sms_time}->ymd();
            ok( !! grep( { $_->{sms_time} eq $sms_time } @{ $json->{result} } ), 'sms exists' );
        }
    },
);

run_tests(\@tests);
