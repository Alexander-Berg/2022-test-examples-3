#!/usr/bin/env python
import argparse


WARN_DIFF = 0.01
ERR_DIFF = 0.03


def add_table(columns, rows, width='50%', caption=None):
    tab = ['\n'.join(['<td><b>{}</b></td>'.format(c) for c in columns])]
    for row in rows:
        tab.append('\n'.join(['<td>{}</td>'.format(val) for val in row]))
    return '<table width="{}" border="1px"><caption>{}</caption>{}</table>'.format(width,
                                                                                   caption if caption is not None else '',
                                                                                   '\n'.join(['<tr>{}</tr>'.format(tr) for tr in tab]))


def paint(text, color):
    return '<span style="color:{}">{}</span>'.format(color, text)

def accept(prod_loss, dev_loss, changed, total):
    warn = paint('Warn', 'orange')
    err = paint('Fail', 'red')
    prod_loss = float(prod_loss) / total
    if prod_loss >= WARN_DIFF:
        return warn
    elif prod_loss >= ERR_DIFF:
        return err
    dev_loss = float(dev_loss) / total
    if dev_loss >= WARN_DIFF:
        return warn
    elif dev_loss >= ERR_DIFF:
        return err
    changed = float(changed) / total
    if changed >= WARN_DIFF:
        return warn
    elif changed >= ERR_DIFF:
        return err
    else:
        return paint('Success', 'green')


def make_link(url):
    return '<a href="{}">{}..</a>'.format(url, url[:120])


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--prod', type=argparse.FileType('r'), required=True)
    ap.add_argument('--dev', type=argparse.FileType('r'), required=True)
    ap.add_argument('--output', type=argparse.FileType('w'), required=True)
    args = ap.parse_args()
    prod = {}
    dev = {}
    for line in args.prod:
        vec = line.strip().split('\t')
        if len(vec) >= 2:
            url, label = vec[:2]
            prod[url] = label
    for line in args.dev:
        vec = line.strip().split('\t')
        if len(vec) >= 2:
            url, label = vec[:2]
            dev[url] = label
    dev_missing = []
    prod_missing = []
    keys = set()
    for url in prod.iterkeys():
        if url not in dev:
            dev_missing.append(url)
        keys.add(url)
    for url in dev.iterkeys():
        if url not in prod:
            prod_missing.append(url)
        keys.add(url)
    changes = []
    for url in keys:
        if url in dev and url in prod:
            if prod[url] != dev[url]:
                changes.append([url, prod[url], dev[url]])


    columns = ['Total URLs', 'Prod loss', 'Dev loss', 'Changes', 'Accepted']
    rows = [[len(keys), len(prod_missing), len(dev_missing), len(changes), accept(len(prod_missing), len(dev_missing), len(changes), len(keys))]]
    tab1 = add_table(columns, rows, caption='Total', width='100%')

    columns = ['URL', 'Comment']
    rows = []
    for url in prod_missing:
        rows.append([url, 'prod loss'])
    for url in dev_missing:
        rows.append([url, 'dev loss'])
    for row in changes:
        rows.append([make_link(row[0]), '{}-&gt;{}'.format(row[1], row[2])])
    tab2 = add_table(columns, rows, width='100%', caption='Details')
    args.output.write('<!doctype html><head></head><body>{}<br><br>{}</body></html>'.format(tab1, tab2))
