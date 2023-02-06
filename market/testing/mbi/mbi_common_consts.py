#!/usr/bin/env python
# coding=utf-8

import unittest
import os

BALANCE_CLIENT_NOTIFY_ORDER2_TPL = '''
    <methodCall>
        <methodName>BalanceClient.NotifyOrder2</methodName>
        <params>
            <param>
                <value>
                    <struct>
                        <member>
                            <name>ServiceID</name>
                            <value>
                                <int>11</int>
                            </value>
                        </member>
                        <member>
                            <name>ServiceOrderID</name>
                            <value>
                                <string>{cmpg_id}</string>
                            </value>
                        </member>
                        <member>
                            <name>ConsumeQty</name>
                            <value>
                                <string>{amount}</string>
                            </value>
                        </member>
                        <member>
                            <name>Tid</name>
                            <value>
                                <string>{tid}</string>
                            </value>
                        </member>
                    </struct>
                </value>
            </param>
        </params>
    </methodCall>'''


class T(unittest.TestCase):
    def test_(self):
        pass


def main():
    pass


if __name__ == '__main__':
    if os.environ.get('UNITTEST') == '1':
        unittest.main()
    else:
        main()
