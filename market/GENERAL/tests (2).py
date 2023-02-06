import random
from typing import Callable

import torch
from fasttext import load_model

from src.FastText_PyTorch.dataset import ClassDisjointModels


def check_hashing(
    filename: str,
    min_ngram: int = 5,
    max_ngram: int = 5
):
    """ Function to check whether self-implemented
        hashing works faithful to fastText C Hashing """
    fasttext_model = load_model(filename)

    for word in fasttext_model.get_words():

        subwords = ClassDisjointModels.extract_subwords(
            word, min_ngram, max_ngram)

        for subword in subwords:
            orig_id = fasttext_model.get_subword_id(subword)
            alter_id = ClassDisjointModels.fnv1a_hash(subword)

            if orig_id != alter_id:
                raise AssertionError(subword)

    print('All hashing tests passed')


def check_collater(
    collater_fn: Callable,
    dataset: torch.utils.data.Dataset,
    model: torch.nn.Module,
    idx: int,
    num_exps: int = 4
):
    target_sample = dataset[idx]
    targets_emb = []

    for _ in range(num_exps):
        batch_size = random.randint(1, len(dataset))
        idxs_to_batch = [
            random.randrange(0, len(dataset)) for _ in range(batch_size - 1)]

        samples = [target_sample] + [dataset[idx] for idx in idxs_to_batch]
        batch = collater_fn(samples)

        seq_embs = model(batch['indices'], batch['offsets'], batch['num_words'])
        targets_emb.append(seq_embs[0])

    for i in range(1, num_exps):
        if not torch.allclose(targets_emb[i - 1], targets_emb[i]):
            raise AssertionError(
                'Embeddings for same sequence did not match for different batches')

    print('All collater tests passed')
