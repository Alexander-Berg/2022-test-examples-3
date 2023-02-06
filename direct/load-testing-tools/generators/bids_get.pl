#!/usr/bin/perl

use strict;
use warnings;

use my_inc '/var/www/ppc.yandex.ru', for => 'protected';

$Yandex::DBTools::CONFIG_FILE = '/etc/direct/db-config-np/db-config.test2.json';
use Encode qw/encode_utf8/;
use JSON;
use Getopt::Long;
use Yandex::DBTools;
use POSIX;
use List::Util qw(reduce shuffle);

use utf8;
use open qw/:std :encoding(UTF-8)/;

# Первое число вероятность появления selectionCriteria ключа содержащего не менее второго значения и менее третьего значения айдишников 4 число верхнее ограничение выборки на шард 
my $criteria_proportions = {
    'KeywordIds' => [
        [0.80, 5, 100, 100],
        [0.15, 100, 600, 50],
        [0.05, 600, 10000, 20],
    ], 
    'CampaignIds' => [
        [1.00, 1, 2]
    ],
    'AdGroupIds' => [
        [0.85, 1, 10],
        [0.10, 10, 50],
        [0.05, 50, 100],
    ]
};

# Ключ хеша вероятность появления данного запроса на основании логов продакшна 
my $fields_criteria_proportions = [
    [0.07,
        {"selectionCriteria" => [
                "KeywordIds"
            ],
            "fields" => [
                "AuctionBids",
                "Bid",
                "CampaignId",
                "ContextBid",
                "ContextCoverage",
                "CurrentSearchPrice",
                "KeywordId"
            ]
        }
    ],
    [0.03, {
            "fields" => [
                "AuctionBids",
                "Bid",
                "ContextBid",
                "ContextCoverage",
                "CurrentSearchPrice",
                "KeywordId"
            ],
            "selectionCriteria" => [
                "KeywordIds",
                "ServingStatuses"
            ]
        }
    ],
    [0.12, {
            "fields" => [
                "AuctionBids",
                "Bid",
                "ContextBid",
                "ContextCoverage",
                "KeywordId"
            ],
            "selectionCriteria" => [
                "KeywordIds"
            ]
        }
    ],
    [0.04, {
            "fields" => [
                "AuctionBids",
                "Bid",
                "CampaignId",
                "CurrentSearchPrice",
                "KeywordId"
            ],
            "selectionCriteria" => [
                "KeywordIds"
            ]
        }
    ],
    [0.06, {
            "selectionCriteria" => [
                "CampaignIds"
            ],
            "fields" => [
                "AdGroupId",
                "AuctionBids",
                "Bid",
                "CampaignId",
                "CompetitorsBids",
                "ContextBid",
                "ContextCoverage",
                "CurrentSearchPrice",
                "KeywordId",
                "MinSearchPrice",
                "SearchPrices",
                "StrategyPriority"
            ]
        }
    ],
    [0.13, {
            "selectionCriteria" => [
                "AdGroupIds"
            ],
            "fields" => [
                "AuctionBids",
                "Bid",
                "CompetitorsBids",
                "ContextBid",
                "ContextCoverage",
                "CurrentSearchPrice",
                "KeywordId",
                "SearchPrices"
            ]
        }
    ],
    [0.06, {
            "selectionCriteria" => [
                "KeywordIds"
            ],
            "fields" => [
                "AdGroupId",
                "AuctionBids",
                "Bid",
                "CampaignId",
                "ContextBid",
                "ContextCoverage",
                "CurrentSearchPrice",
                "KeywordId",
                "MinSearchPrice",
                "SearchPrices"
            ]
        }
    ],
    [0.03, {
            "fields" => [
                "AdGroupId",
                "AuctionBids",
                "Bid",
                "CampaignId",
                "CompetitorsBids",
                "ContextBid",
                "ContextCoverage",
                "CurrentSearchPrice",
                "KeywordId",
                "MinSearchPrice",
                "SearchPrices",
                "ServingStatus",
                "StrategyPriority"
            ],
            "selectionCriteria" => [
                "CampaignIds"
            ]
        }
    ],
    [0.11, {
            "selectionCriteria" => [
                "KeywordIds",
                "ServingStatuses"
            ],
            "fields" => [
                "AuctionBids",
                "Bid",
                "ContextBid",
                "CurrentSearchPrice",
                "KeywordId"
            ]
        }
    ],
    [0.35, {
            "fields" => [
                "AdGroupId",
                "AuctionBids",
                "Bid",
                "CampaignId",
                "ContextBid",
                "KeywordId",
                "ServingStatus"
            ],
            "selectionCriteria" => [
                "CampaignIds"
            ]
        }
    ]
];

my %cids_logins;  
my %bids;
my %logins_pids;
my %logins_by_pids_count;
my %cdf_result;

my @SHARDS = (1, 2);

my $user_agent = 'lunapark';
my $token = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';   # для "фальшивого" паспорта подойдёт любая последовательность, которая пройдет валидацию 
# если нужно пользоваться настоящим токеном, можно делать sed '/^Authorization:/ s/AAAA\+/<token>/' на файл с патронами

# функция распределения необходима для построения массива распределения по заданным весам
sub cdf {
    my $weights = shift;
    my $sum = reduce {$a + $b} @$weights;
    my $cumsum;
    my $result = [];
    foreach (@$weights) {
        $cumsum += $_;
        push @$result, $cumsum/$sum;
    }
    return $result;
}

# функция бинарного поиска, ищет место вставки значение в массив, необходима для получения элементов массива с заданной вероятностью появления каждого элемента
sub bisect{
    my ($arr, $val) = @_;
    my $left = 0;
    my $right = @$arr - 1;
    while($left < $right) {
        my $mid = ($left + $right) >> 1;
        if($val <= $arr -> [$mid]) {
            $right = $mid;
        } else {
            $left = $mid + 1;
        }
    }
    return $right;
}

# функция получения случайного элемента из массива
sub get_random_element {
    my $arr = shift;
    my $len = @$arr;
    return $arr -> [int(rand($len))];
}

# функция генерирует заданное количество запросов
sub generate_requests {
    my $count = shift;
    my $result = [];
    my $result_count;
    my $tag;
    OUT_POINT: foreach my $fields_criteria (@$fields_criteria_proportions) {
        my $percent = $fields_criteria -> [0];
        for(my $i = 0; $i < ceil($percent * $count); $i++) {
            last OUT_POINT if(++$result_count > $count);
            my ($criteria, $login, $tag) =  get_selection_criteria_and_login($fields_criteria -> [1] -> {'selectionCriteria'});
            push($result, {
                    tag => $tag,
                    login => $login,
                    request =>{
                        SelectionCriteria => $criteria,
                        FieldNames => $fields_criteria -> [1] -> {'fields'}
                    } 
                });
        }
    }
    return shuffle($result);
}

# функция для массива критериев подставляет валидные значения полученные из базы данных, в соответствии с таблицей распределения criteria_proportions
sub get_selection_criteria_and_login {
    my $criteria = shift;
    die "Unexpected criterias set:".to_json($criteria, {pretty => 1, utf8 => 1}) if(values $criteria > 1 && (values $criteria == 2 && !(grep {$_ eq 'ServingStatuses'} values $criteria)));
    my $login; 
    my $result_criteria;
    my $tag;
    foreach (values $criteria) {
        if ($_ eq 'KeywordIds') {
            ($login, $result_criteria -> {$_}, $tag) = get_bids_criteria();
        } elsif($_ eq 'CampaignIds') {
            ($login, $result_criteria -> {$_}, $tag) = get_cids_criteria();
        } elsif($_ eq 'AdGroupIds') {
            ($login, $result_criteria -> {$_}, $tag) = get_pids_criteria();
        } elsif($_ eq 'ServingStatuses') {
            $result_criteria -> {$_} = ['ELIGIBLE'];
        }
    }
    return ($result_criteria, $login, $tag);
}

# функция получения значений KeywordIds в соответствии с вероятностным распределением
sub get_bids_criteria {
    my $idx = bisect($cdf_result{'KeywordIds'}, rand);
    my $proportions =  $criteria_proportions -> {'KeywordIds'} -> [$idx];
    my $range = join "-", $proportions -> [1], $proportions -> [2];
    my @keys = keys $bids{$range};
    my $cid = get_random_element(\@keys);
    my $selected_bids = $bids{$range} -> {$cid};
    if(@$selected_bids >= 10_000) {
        @$selected_bids = @$selected_bids[0..9999];
    }
    return ($cids_logins{$cid}, $selected_bids, "KeywordIds_$range");
}

# функция получения значений cids, ожидает что в критериях используется только 1 кампания, по статистике 99% использующих CampaignIds используют только 1 значение на запрос
sub get_cids_criteria {
    die "Unexpected CampaingsIds proportions" unless(@{$cdf_result{'CampaignIds'}} == 1);
    my @keys = keys %cids_logins;
    my $cid = get_random_element(\@keys);
    return ($cids_logins{$cid}, [$cid], "CampaignIds_1");
}

# функция получения значений pids в соответствии с вероятностным распределением
sub get_pids_criteria {
    my $idx = bisect($cdf_result{'AdGroupIds'}, rand);
    my $proportions =  $criteria_proportions -> {'AdGroupIds'} -> [$idx];
    my ($min, $max) =  ($proportions -> [1], $proportions -> [2]);
    my $range = join "-", $min, $max;
    die "Not found users with $range" unless($logins_by_pids_count{$range});
    my $random_login = get_random_element($logins_by_pids_count{$range});
    my $pids = $logins_pids{$random_login};
    my $high = int(rand($max)) + 1;
    $pids = [@$pids[0..$high]] if(@$pids > $max);
    return ($random_login, $pids, "AdGroupIds_$range");
}

# функция получения logins, cids, pids, bids из базы данных
sub get_cids_bids_with_bids_count {
    my ($min, $max, $limit) = @_;
    my $cids_logins = {};
    my $cids_bids = {};
    my $logins_pids = {};
    foreach my $shard (@SHARDS) {
        my $logins_cids =  get_all_sql("ppc:$shard", 
            [ "select login, cid from users u join campaigns c using(uid) join bids b using(cid) where OrderId <> 0 and clicks > 1 group by cid having count(*) >= $min and  count(*) < $max  order by cid desc limit $limit"]);

        foreach my $row (@$logins_cids) {
            $cids_logins -> {$row -> {'cid'}} = $row -> {'login'};
        }
        my @cids = keys %$cids_logins;
        my $cids_pids_bids = get_all_sql("ppc:$shard", ['select cid, pid, id from bids', where => {cid__in => \@cids}]);
        foreach my $row (@$cids_pids_bids) {
            my $cid = $row -> {'cid'};
            my $login = $cids_logins -> {$cid};
            $cids_bids -> {$cid} //= [];
            push $cids_bids -> {$cid}, $row -> {'id'}; 
            $logins_pids -> {$login} //= [];
            push $logins_pids -> {$login}, $row -> {'pid'} unless(grep {$_ == $row -> {'pid'}} @{$logins_pids -> {$login}});
        }
    }
    return {
        cids_logins => $cids_logins,
        cids_bids => $cids_bids,
        logins_pids => $logins_pids
    };
}

sub init_generator {
    foreach my $proportion (values $criteria_proportions -> {'KeywordIds'}) {
        my $min = $proportion -> [1];
        my $max = $proportion -> [2]; 
        my $limit = $proportion -> [3];
        my $result = get_cids_bids_with_bids_count($min, $max, $limit);
        %cids_logins = (%cids_logins, %{$result -> {'cids_logins'}});
        $bids{"$min-$max"} = $result -> {'cids_bids'};
        foreach my $login (keys %{$result -> {'logins_pids'}}) {
            $logins_pids{$login} //= [];
            push $logins_pids{$login}, @{$result -> {'logins_pids'} -> {$login}};
        }
    }

    foreach my $login (keys %logins_pids) {
        foreach my $proportion (values $criteria_proportions -> {'AdGroupIds'}) {
            my ($min, $max) = ($proportion -> [1], $proportion -> [2]);
            my $len = $logins_pids{$login};
            if(@{$logins_pids{$login}} >= $min) {
                $logins_by_pids_count{"$min-$max"} //= [];
                push $logins_by_pids_count{"$min-$max"}, $login;
            } else {
                last;
            }
        }
    }

    foreach my $criteria (keys $criteria_proportions) {
        my @weights;
        foreach my $proportion (values $criteria_proportions -> {$criteria}) {
            push @weights, $proportion -> [0];
        }
        $cdf_result{$criteria} = cdf(\@weights);
    }
}

sub self_test {
    my $requests = shift;
    my %fields_criteria_hash;
    my %criteria_hash;
    my %criteria_sum_count;
    foreach my $fields_criteria (@$fields_criteria_proportions) {
        my $new_key =  (join ",", sort(values $fields_criteria -> [1] -> {'selectionCriteria'})).'|'
        .(join ",", sort(values $fields_criteria -> [1] -> {'fields'}));

        $fields_criteria_hash{$new_key} = {
            expected => $fields_criteria -> [0]
        };
    }
    my $count = scalar @$requests;
    foreach my $req (@$requests) {
        my $params = $req -> {'request'};
        my $key = (join ",", sort(keys $params -> {'SelectionCriteria'})).'|'.(join ",", sort(values $params -> {'FieldNames'}));
        $fields_criteria_hash{$key} -> {'actualCount'}++;
        my $criterias =  $params -> {'SelectionCriteria'};
        foreach my $criteria (keys $criterias) {
            next if($criteria eq 'ServingStatuses');
            $criteria_hash{$criteria} //= {};
            foreach my $proportions (values $criteria_proportions -> {$criteria}) {
                my $min = $proportions -> [1];
                my $max = $proportions -> [2];
                my $size = scalar @{$criterias -> {$criteria}};
                if(($size >= $min) && ($size < $max)) {
                    $criteria_hash{$criteria} -> {"$min-$max"}++;
                    $criteria_sum_count{$criteria}++;
                    last;        
                }
            }
        }
    }
    printf "%-135s %-20s %-20s %-20s\n", "ключ", "ожидаемый процент(%)", "актуальный процент(%)", "отклонение(%)";
    foreach (keys %fields_criteria_hash) {
        my $actual_percent = $fields_criteria_hash{$_} -> {'actualCount'}/$count;
        my $expected = $fields_criteria_hash{$_} -> {'expected'};
        printf "%-135s %-20.3f %-20.3f %-20.3f\n", $_, $expected * 100, $actual_percent * 100, ($actual_percent/$expected - 1) * 100;
    }

    my %expected_criteria_percent;
    foreach my $criteria (keys $criteria_proportions) {
        $expected_criteria_percent{$criteria} = {};
        foreach my $p (values $criteria_proportions -> {$criteria}) {
            $expected_criteria_percent{$criteria} -> {($p -> [1])."-".($p -> [2])} = $p -> [0];
        }
    }
    print "\n" x 3;
    printf "%-35s %-20s %-20s %-20s\n", "Критерий",  "ожидаемый процент(%)", "актуальный процент(%)", "отклонение(%)";
    foreach my $criteria (keys %criteria_hash) {
        printf "%-35s\n", $criteria;
        foreach my $range (keys $criteria_hash{$criteria}) {
            my $actual_percent = $criteria_hash{$criteria} -> {$range} / $criteria_sum_count{$criteria};
            my $expected_percent = $expected_criteria_percent{$criteria} -> {$range};
            printf "%-35s %-20.3f %-20.3f %-20.3f\n", "\t$range", $expected_percent * 100,
            $actual_percent * 100, ($actual_percent/$expected_percent - 1)*100; 
        }
    }
}


my %O;
$O{count} = 10_000;
$O{test} = 0;
GetOptions(
    'count=s' => \$O{count},
    'test+' => \$O{test}
) or die "can't parse options, stop\n";

init_generator();
if ($O{test}) {
    self_test(generate_requests($O{count}));
    exit;
}
my $requests_params = generate_requests($O{count});
for my $req_param (values $requests_params) {
    my $login = $req_param->{login}; 
    my $tag = $req_param->{tag};
    my $req = to_json({
            method => 'get',
            params => $req_param->{request}
        });
    # http://yandextank.readthedocs.io/en/latest/tutorial.html#request-style
    my $request = sprintf "POST /json/v5/bids HTTP/1.0\nHost: test-direct.yandex.ru\nContent-Length: %d\nAuthorization: Bearer %s\nClient-Login: %s\nUser-Agent: %s\n\n%s", length(encode_utf8($req)), $token, $login, $user_agent, $req;
    printf "%d %s\n%s\n", length(encode_utf8($request)), $tag, $request;
}
