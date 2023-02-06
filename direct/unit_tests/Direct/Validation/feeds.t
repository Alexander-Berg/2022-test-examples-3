use my_inc "../../../";
use Direct::Modern;
use Test::More;
use Yandex::Test::ValidationResult;

BEGIN {
    use_ok 'Direct::Validation::Feeds';
    use_ok 'Direct::Model::Feed';
}

sub mk {
    Direct::Model::Feed->new(@_);
}

sub v {
    &Direct::Validation::Feeds::validate_feed;
}

subtest "When validating email" => sub {
    subtest "it is not required" => sub {
        ok_validation_result v(mk(name => 'aaa'));
    };
    subtest "it should look like valid email" => sub {
        cmp_validation_result v(mk(email => 'some strange thing', name => 'aaa')), {
            email => vr_errors('InvalidField'),
        };
    };
    subtest "undef should be considered valid" => sub {
        ok_validation_result(v(mk(name => 'a', email => undef)));
    };
};

subtest "When validating url" => sub {
    subtest "it should be not too long" => sub {
        my $long_url = 'http://ya.ru/?param=' . ('x' x 8192 );
        cmp_validation_result v(mk(url => $long_url, name => 'aaa')), {
            url => vr_errors('InvalidField'),
        };
    };
    subtest "it should look like valid url" => sub {
        cmp_validation_result v(mk(url => 'dododood lalal', name => 'aaa')), {
            url => vr_errors('InvalidField'),
        };
    };
    subtest "it is not required for 'file' source" => sub {
        ok_validation_result v(mk(name => 'aaa', source => 'file', filename => 'a.xml'));
    };
    subtest "it is required for 'url' source" => sub {
        cmp_validation_result v(mk(name => 'aaa', source  => 'url')), {
            url => vr_errors('ReqField'),
        };
        cmp_validation_result v(mk(name => 'aaa', source  => 'url', url => undef)), {
            url => vr_errors('EmptyField'),
        };
        cmp_validation_result v(mk(name => 'aaa', source  => 'url', url => '')), {
            url => vr_errors('EmptyField'),
        };
    };
    subtest "ftp:// urls are allowed" => sub {
        ok_validation_result v(mk(name => 'aaa', url => 'ftp://example.com'));
    };

    subtest "ip address is allowed" => sub {
        ok_validation_result v(mk(name => 'aaa', url => 'http://37.143.8.9/price_auto/feeds/euroset_msk_display_beta.xml'));
    };
};

subtest "When validating name" => sub {
    subtest "it is required" => sub {
        cmp_validation_result v(mk()), {
            name => vr_errors('ReqField')
        };
    };
    subtest "it should not be empty" => sub {
        cmp_validation_result v(mk(name => '')), {
            name => vr_errors('EmptyField')
        };
    };
};

subtest "When validating login" => sub {
    subtest "it is not required" => sub {
        ok_validation_result(v(mk(name=>'a')));
    };
    subtest "it could be undefined" => sub {
        ok_validation_result(v(mk(login => undef, name=>'a')));
    };
    subtest "it couldn't be an empty string" => sub {
        cmp_validation_result v(mk(name=>'a', login => '')), { login => vr_errors('EmptyField') };
        cmp_validation_result v(mk(name=>'a', login => '   ')), { login => vr_errors('EmptyField') };
    };
    subtest "any non-empty string should be considered valid" => sub {
        ok_validation_result(v(mk(login => 'hohoho', name=>'a')));
    };
};

subtest "When validating filename" => sub {
    subtest "it should be required for 'file' source" => sub {
        cmp_validation_result v(mk(name => 'a', source => 'file')), {filename => vr_errors('ReqField')};
        cmp_validation_result v(mk(name => 'a', source => 'file', filename => undef)), {filename => vr_errors('EmptyField')};
        cmp_validation_result v(mk(name => 'a', source => 'file', filename => '')), {filename => vr_errors('EmptyField')};
        ok_validation_result v(mk(name => 'a', source => 'file', filename => 'a.xml'));
        ok_validation_result v(mk(name => 'feed_123', source => 'file', filename => 'excel_fees.xlsx'));
    };
};

done_testing;
