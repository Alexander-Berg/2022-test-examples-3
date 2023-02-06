#!/usr/bin/perl

#    $Id$

use strict;
use utf8;
use warnings;
use Test::Deep;
use Test::MockObject::Extends;
use HTTP::Headers;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

use LWPRedirect;
use URLDomain;

my %db = (
    trusted_redirects => {
        original_db => PPCDICT,
        like => 'trusted_redirects',
        rows => [
            {
                domain => 'notcounter1.com',
                redirect_type => 'short',
            },
            {
                domain => 'counter1.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter2.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter3.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter4.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter5.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter6.com',
                redirect_type => 'counter',
            },
            {
                domain => 'pixel.everesttech.net',
                redirect_type => 'counter',
            },
        ],
    },
);

init_test_dataset(\%db);

# result_with_is_known_redirect - простукиваем только счётчики;
# result_without_is_known_redirect — простукиваем всё подряд, для несчётчиков домен берём из ссылки
my @chains = (
    {
        domains => [
            {name => 'http://counter1.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://counter1.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://counter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 404},
        ],
        result_without_is_known_redirect => [
            {url => 'http://counter1.com', label => 'Not found'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://counter1.com', label => 'Not found'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter1.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 404},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter1.com', label => 'Not found'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter2.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter2.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter2.com'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter2.com', code => 500},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter2.com', label => 'Error'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter2.com'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter1.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter1.com', label => 'http', redirect => 'http://counter1.com'},
            {url => 'http://counter1.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://counter1.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://counter1.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 404},
        ],
        result_without_is_known_redirect => [
            {url => 'http://counter1.com', label => 'http', redirect => 'http://counter2.com'},
            {url => 'http://counter2.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com', label => 'http', redirect => 'http://notcounter2.com'},
            {url => 'http://notcounter2.com', label => 'Not found'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://counter1.com', label => 'http', redirect => 'http://counter2.com'},
            {url => 'http://counter2.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 500},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter1.com', label => 'http', redirect => 'http://notcounter2.com'},
            {url => 'http://notcounter2.com', label => 'http', redirect => 'http://counter1.com'},
            {url => 'http://counter1.com', label => 'http', redirect => 'http://counter2.com'},
            {url => 'http://counter2.com', label => 'Error'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://notcounter2.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://counter1.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com', label => 'http', redirect => 'http://counter2.com'},
            {url => 'http://counter2.com', label => 'http', redirect => 'http://notcounter2.com'},
            {url => 'http://notcounter2.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://counter1.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter2.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {url => 'http://notcounter1.com', label => 'http', redirect => 'http://counter1.com'},
            {url => 'http://counter1.com', label => 'http', redirect => 'http://notcounter1.com'},
            {url => 'http://notcounter1.com', label => 'http', redirect => 'http://counter2.com'},
            {url => 'http://counter2.com'},
        ],
        result_with_is_known_redirect => [
            {url => 'http://notcounter1.com'},
        ],
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com', code => 399},
            {name => 'http://counter1.com', code => 200},
        ],
        result_without_is_known_redirect => [
            {
                redirect => 'http://counter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://counter1.com',
            },
        ],
        result_with_is_known_redirect => [
            {
                redirect => 'http://counter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://counter1.com',
            },
        ],
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com', code => 399},
            {name => 'http://counter1.com', code => 404},
        ],
        result_without_is_known_redirect => [
            {
                redirect => 'http://counter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://counter1.com',
                label => 'Not found',
            },
        ],
        result_with_is_known_redirect => [
            {
                redirect => 'http://counter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://counter1.com',
                label => 'Not found',
            },
        ],
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fnotcounter1.com', code => 399},
            {name => 'http://notcounter1.com', code => 404},
        ],
        result_without_is_known_redirect => [
            {
                redirect => 'http://notcounter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fnotcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://notcounter1.com',
                label => 'Not found',
            },
        ],
        result_with_is_known_redirect => [
            {
                redirect => 'http://notcounter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fnotcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://notcounter1.com',
            },
        ],
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com', code => 399},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 500},
        ],
        result_without_is_known_redirect => [
            {
                redirect => 'http://counter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://counter1.com',
                label => 'http',
                redirect => 'http://notcounter1.com',
            },
            {
                url => 'http://notcounter1.com',
                label => 'Error',
            },
        ],
        result_with_is_known_redirect => [
            {
                redirect => 'http://counter1.com',
                url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                label => 'predefined',
            },
            {
                url => 'http://counter1.com',
                label => 'http',
                redirect => 'http://notcounter1.com',
            },
            {
                url => 'http://notcounter1.com',
            },
        ],
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://counter3.com', code => 302},
            {name => 'http://counter4.com', code => 302},
            {name => 'http://counter5.com', code => 302},
            {name => 'http://counter6.com', code => 302},
        ],
        result_without_is_known_redirect => [
            {
              'redirect' => 'http://counter2.com',
              'url' => 'http://counter1.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter3.com',
              'url' => 'http://counter2.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter4.com',
              'url' => 'http://counter3.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter5.com',
              'url' => 'http://counter4.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter6.com',
              'url' => 'http://counter5.com',
              'label' => 'http',
            },
            {
              'url' => 'http://counter6.com',
              'label' => 'Error',
            },
        ],
        result_with_is_known_redirect => [
            {
              'redirect' => 'http://counter2.com',
              'url' => 'http://counter1.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter3.com',
              'url' => 'http://counter2.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter4.com',
              'url' => 'http://counter3.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter5.com',
              'url' => 'http://counter4.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://counter6.com',
              'url' => 'http://counter5.com',
              'label' => 'http',
            },
            {
              'url' => 'http://counter6.com',
              'label' => 'Error',
            },
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 302},
            {name => 'http://notcounter3.com', code => 302},
            {name => 'http://notcounter4.com', code => 302},
            {name => 'http://notcounter5.com', code => 302},
            {name => 'http://notcounter6.com', code => 302},
        ],
        result_without_is_known_redirect => [
            {
              'redirect' => 'http://notcounter2.com',
              'url' => 'http://notcounter1.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://notcounter3.com',
              'url' => 'http://notcounter2.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://notcounter4.com',
              'url' => 'http://notcounter3.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://notcounter5.com',
              'url' => 'http://notcounter4.com',
              'label' => 'http',
            },
            {
              'redirect' => 'http://notcounter6.com',
              'url' => 'http://notcounter5.com',
              'label' => 'http',
            },
            {
              'url' => 'http://notcounter6.com',
              'label' => 'Error',
            },
        ],
        result_with_is_known_redirect => [
            {
                'url' => 'http://notcounter1.com',
            },
        ],
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://counter3.com', code => 302},
            {name => 'http://counter4.com', code => 302},
            {name => 'http://counter5.com', code => 302},
        ],
        result_without_is_known_redirect => [
            {
                'redirect' => 'http://counter1.com',
                'url' => 'http://notcounter1.com',
                'label' => 'http',
            },
            {
                'redirect' => 'http://counter2.com',
                'url' => 'http://counter1.com',
                'label' => 'http',
            },
            {
                'redirect' => 'http://counter3.com',
                'url' => 'http://counter2.com',
                'label' => 'http',
            },
            {
                'redirect' => 'http://counter4.com',
                'url' => 'http://counter3.com',
                'label' => 'http',
            },
            {
                'redirect' => 'http://counter5.com',
                'url' => 'http://counter4.com',
                'label' => 'http',
            },
            {
                'url' => 'http://counter5.com',
                'label' => 'Error',
            },
        ],
        result_with_is_known_redirect => [
            {
                'url' => 'http://notcounter1.com',
            }
        ],
    },
);

Test::More::plan(tests => 2 * scalar(@chains));

sub resp_200 {
    return HTTP::Response->new(200, 'OK');
}

sub resp_302 {
    my ($location) = @_;

    return HTTP::Response->new(302, 'Found', HTTP::Headers->new(Location => $location));
}

sub resp_404 {
    return HTTP::Response->new(404, 'Not Found');
}

sub resp_500 {
    return HTTP::Response->new(500, 'Server Error')
}

# индекс текущей цепочки, данные и индекс домена в текущей цепочке
my $curr_chain = {
  idx => 0,
  domain_idx => 0,
  data => undef,
};

sub item {
    my ($domain_idx) = @_;

    return $curr_chain->{data}->{domains}->[$domain_idx];
}

sub incr_domain_idx {
    ++$curr_chain->{domain_idx};
}

sub first_domain {
    return item(0);
}

sub curr_domain {
    return item($curr_chain->{domain_idx});
}

sub next_domain {
    return item($curr_chain->{domain_idx} + 1);
}

sub reset_curr_chain {
    $curr_chain->{idx} = 0;
    $curr_chain->{domain_idx} = 0;
    $curr_chain->{data} = undef;
}

our $PREDEFINED_REDIRECT_CODE = 399;

my $ua = Test::MockObject::Extends->new('LWP::UserAgent');

sub sr {
    my ($self, $req) = @_;

    my $domain = curr_domain();

    if ( $domain->{code} == $PREDEFINED_REDIRECT_CODE ) {
        # predefined_redirect обрабатывается в get_redirect_chain,
        # поэтому нам нужно сдвинуть указатель вперед на одну позицию
        incr_domain_idx();
        $domain = curr_domain();
    }

    my $resp;

    if ($domain->{code} == 200) {
        $resp = resp_200();
    } elsif($domain->{code} == 302) {
            my $rdomain = next_domain();
            $resp = resp_302($rdomain ? $rdomain->{name} : 'http://dummy.com');
    } elsif($domain->{code} == 404) {
        $resp = resp_404();
    } elsif($domain->{code} == 500) {
        $resp = resp_500();
    } else {
        die('unknown code');
    }

    $resp->request($req);

    incr_domain_idx();

    return $resp;
}

$ua->mock('simple_request', \&sr);

*grc = \&LWPRedirect::get_redirect_chain;

sub delete_rr {
    my ($chain) = @_;

    foreach (@$chain) {
        delete($_->{request});
        delete($_->{response});
    }
}

my $chain_idx = 0;
foreach my $chain (@chains) {
    ++$chain_idx;
    my $chain_name = "chain_$chain_idx";
    $curr_chain->{idx} = $chain_idx;
    $curr_chain->{data} = $chain;

    # без is_known_redirect
    {
        my $chain = grc(
            first_domain()->{name},
            ua => $ua,
            predefined_redirect => \&URLDomain::predefined_redirect,
        )->{chain};
        # request & response не нужны
        delete_rr($chain);

        cmp_deeply($chain, $curr_chain->{data}->{result_without_is_known_redirect}, $chain_name);

        # обнуляем доменный индекс для следующего теста
        $curr_chain->{domain_idx} = 0;
    }

    # с is_known_redirect
    {
        my $chain = grc(
            first_domain()->{name},
            ua => $ua,
            predefined_redirect => \&URLDomain::predefined_redirect,
            is_known_redirect => \&URLDomain::is_known_redirect,
        )->{chain};
        # request & response не нужны
        delete_rr($chain);

        cmp_deeply($chain, $curr_chain->{data}->{result_with_is_known_redirect}, $chain_name);
    }

    reset_curr_chain();
}
