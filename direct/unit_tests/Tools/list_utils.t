#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 29;

use Yandex::ListUtils;

is_deeply(range(), [], "range() without parameters");
is_deeply(range(10), [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], "range() with one positive parameter");
is_deeply(range(-10), [], "range() with one negative parameter");
is_deeply(range(10, 20), [10, 11, 12, 13, 14, 15, 16, 17, 18, 19], "range() with two parameters (positive, positive)");
is_deeply(range(-10, 5), [-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4], "range() with two parameters (negative, positive)");
is_deeply(range(10, -10), [], "range() with two parameters (positive, negative)");
is_deeply(range(-10 ,-20), [], "range() with two parameters (negative, negative): start > stop");
is_deeply(range(-20 ,-10), [-20, -19, -18, -17, -16, -15, -14, -13, -12, -11], "range() with two parameters (negative, negative): start < stop");

is_deeply(range(-10 ,-20), [], "range() with three parameters (positive, positive, positive)");

is_deeply(range(1, 10, 3), [1, 4, 7], "range() with three parameters (positive, positive, positive): start < stop");
is_deeply(range(10, 1, 3), [], "range() with three parameters (positive, positive, positive): start > stop");
is_deeply(range(1, 10, -3), [], "range() with three parameters (positive, positive, negative)");
is_deeply(range(1, -10, 3), [], "range() with three parameters (positive, negative, positive)");
is_deeply(range(1, -10, -3), [1, -2, -5, -8], "range() with three parameters (positive, negative, negative)");
is_deeply(range(-1, 10, 3), [-1, 2, 5, 8], "range() with three parameters (negative, positive, positive)");
is_deeply(range(-1, 10, -3), [], "range() with three parameters (negative, positive, negative)");
is_deeply(range(-1, -10, 3), [], "range() with three parameters (negative, negative, positive): start > stop");
is_deeply(range(-10, -1, 3), [-10, -7, -4], "range() with three parameters (negative, negative, positive): start < stop");
is_deeply(range(-10, -1, -3), [], "range() with three parameters (negative, negative, negative): start < stop && step < 0");
is_deeply(range(-1, -10, -3), [-1, -4, -7], "range() with three parameters (negative, negative, negative): start > stop && step < 0");
is_deeply(range(-1, -10, 0), [], "range() with start > stop && step = 0");
is_deeply(range(1, 10, 0), [], "range() with start < stop && step = 0");

is_deeply(range(1.5, 10, 1), [], "range() with non integer parameter (start)");
is_deeply(range("1:5", 10, 1), [], "range() with non integer parameter (start)");
is_deeply(range(1, 10.5, 1), [], "range() with non integer parameter (stop)");
is_deeply(range(1, "10;5", 1), [], "range() with non integer parameter (stop)");
is_deeply(range(1, 10, 1.5), [], "range() with non integer parameter (step)");
is_deeply(range(1, 10, "1,5"), [], "range() with non integer parameter (step)");
is_deeply(range(1, 10, 3,5), [1, 4, 7], "range() with four parameters");


1;
