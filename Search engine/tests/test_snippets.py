#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import library.python.resource as resource
import search.geo.tools.social_links.extract_facts.lib.snippets as snippets


def get_sample_post():
    return json.loads(resource.find('/test_snippets.json'))


def test_sent_borders():
    post = get_sample_post()
    text = post['text']
    sentences = [snippets.get_sentence(text, b).strip() for b in snippets.get_sentence_borders(text)]
    sentences = [s for s in sentences if s]  # rm empty sentences
    valid_sentences = post['sentences']
    assert sentences == valid_sentences


def test_get_fact_intervals():
    post = get_sample_post()
    intervals = snippets.get_facts_intervals(post)
    assert sorted(intervals) == [[210, 214], [215, 219]]


def test_have_intersection():
    assert snippets.have_intersection([0, 2], [1, 3])
    assert snippets.have_intersection([0, 3], [1, 2])
    assert not snippets.have_intersection([0, 1], [2, 3])
    assert snippets.have_intersection([1, 3], [0, 2])
    assert snippets.have_intersection([1, 2], [0, 3])
    assert not snippets.have_intersection([2, 3], [0, 1])


def test_get_intersected_sentences():
    sent_borders = [[0, 10], [12, 15], [16, 20], [30, 40], [41, 50]]
    fact_intervals = [[5, 7], [16, 40]]
    res = snippets.get_intersected_sentences(sent_borders, fact_intervals)
    assert res == [0, 2, 3]
