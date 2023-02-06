#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Test::Deep;

use RedirectCheckQueue;

use Settings;

use utf8;
use open ':std' => ':utf8';

*n = \&RedirectCheckQueue::_dict_href_normalize;

is(n(undef), undef);
is(n(''), '');
is(n('http://www.ya.ru/?afsd=eqwr'), 'http://www.ya.ru/?afsd=eqwr');
is(n('www.ya.ru/?afsd=eqwr'), 'www.ya.ru/?afsd=eqwr');
is(n('www.ya.ru/?asdf=wqer&utm_term=eqwr&utm_campaign=fadsf&wer=avzx'),
   'www.ya.ru/?asdf=wqer&wer=avzx'
    );
is(n('www.ya.ru/qqq?utm_term=eqwr&utm_campaign=fadsf'),
   'www.ya.ru/qqq'
    );
is(n('www.ya.ru/?utm_term=eqwr&utm_campaign=fadsf&wer=avzx'),
   'www.ya.ru/?wer=avzx'
    );
is(n('www.ya.ru/?asdf=234&utm_term=eqwr&utm_campaign=fadsf'),
   'www.ya.ru/?asdf=234'
    );

done_testing();

1;
