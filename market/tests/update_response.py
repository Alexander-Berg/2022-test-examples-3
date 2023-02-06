import requests
import json
import argparse


# The logic of the pruning:
# Make response and whitelist traverse together - nodes that are in the response and not in the whitelist will be removed from response
# When the leaf is reached (None) in the whitelist, pruning of the response dictionary stops on the current branch
whitelist = {
    'search': {
        'results': {
            'entity': None,
            'shop': None,
            'marketSku': None,
            'sku': None,
            'fee': None,
            'promoCodeEnabled': None,
            'wareId': None,
            'supplier': None,
            'isAnalogOffer': None,
            'isAdult': None,
            'delivery': None,
            'prices': None,
            'offerColor': None,
            'isFulfillment': None,
            'slug': None,
            'isDefaultOffer': None,
            'benefit': None,
            'debug': {
                'feed': None,
                'offerUrl': None,
                'offerTitle': None,
                'sale': None,
                'hid': None,
                'wareId': None,
                'modelId': None,
                'buyboxDebug': {
                    'WonMethod': None,
                    'Offers': None,
                    'RejectedOffers': None,
                    'Won': None,
                },
            },
        },
        'modelId': None,
        'marketSku': None,
    }
}


def make_pruning(response_layer, whitelist_layer):
    parts_to_delete = []
    if whitelist_layer is None:
        return
    if isinstance(response_layer, list):
        for layer_obj in response_layer:
            make_pruning(layer_obj, whitelist_layer)
    else:
        for response_path in response_layer:
            if response_path not in whitelist_layer:
                parts_to_delete.append(response_path)
            else:
                make_pruning(response_layer[response_path], whitelist_layer[response_path])
        for marked_to_delete in parts_to_delete:
            del response_layer[marked_to_delete]


def main(args):
    report_request = 'http://warehouse-report.vs.market.yandex.net:17051/' \
                    'yandsearch?place=productoffers&rids=213&market-sku={}' \
                    '&rgb=green_with_blue&pp=6&offers-set=defaultList,listCpa' \
                    '&cart=&grhow=supplier&debug=da'.format(args.msku)

    response = requests.get(report_request)
    full_response_len = len(response.text)
    jsoned_response = json.loads(response.text)

    make_pruning(jsoned_response, whitelist)
    answer_as_string = json.dumps(jsoned_response)
    reduce_amount = float(full_response_len) / len(answer_as_string)
    if not args.quiet:
        print('Length of the full response: {}'.format(full_response_len))
        print('Length of pruned version: {}'.format(len(answer_as_string)))
        print('Length was reduced {} times'.format(int(reduce_amount)))

    with open(args.output, 'w') as outfile:
        outfile.write(answer_as_string)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Get actual report response for productoffers request')
    parser.add_argument('--msku', default=101446185752, help='Msku to form request for')
    parser.add_argument('--output', '-o', default='response.json', help='File where to save response')
    parser.add_argument('--quiet', '-q', action='store_true', help='Do not write any information to the stdout')
    main(parser.parse_args())
