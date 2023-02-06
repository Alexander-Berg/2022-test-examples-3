#!/usr/bin/env python
# -*- coding: utf-8 -*-

import extsearch.images.robot.scripts.cm.semidup2.imagereduce as imagereduce

import fake_factory


def test_parallel_run1():
    settings = {}
    data = {}
    factory = fake_factory.FakeFactory(settings, data)
    imagereduce.parallel_run(factory, 'eat', ['--meal', 'pizza'], 3, 8)
    commands = factory.get_imagereduce().get_commands()
    assert len(commands) == 3
    assert commands[0][1][-4:] == ['--partstart', '0', '--partstop', '2']
    assert commands[1][1][-4:] == ['--partstart', '2', '--partstop', '5']
    assert commands[2][1][-4:] == ['--partstart', '5', '--partstop', '8']


def test_parallel_run2():
    settings = {}
    data = {}
    factory = fake_factory.FakeFactory(settings, data)
    imagereduce.parallel_run(factory, 'sleep', ['--phase', 'night'], 5, 5)
    commands = factory.get_imagereduce().get_commands()
    assert len(commands) == 5
    assert commands[0][1][-2:] == ['--part', '0']
    assert commands[4][1][-2:] == ['--part', '4']


def test_parallel_run3():
    settings = {}
    data = {}
    factory = fake_factory.FakeFactory(settings, data)
    imagereduce.parallel_run(factory, 'play', ['--game', 'chess'], 1, 4)
    commands = factory.get_imagereduce().get_commands()
    assert len(commands) == 1
    assert commands[0][1][-4:] == ['--partstart', '0', '--partstop', '4']
