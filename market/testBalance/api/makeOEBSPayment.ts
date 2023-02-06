/* istanbul ignore file */

/**
 * Подробнее читать тут:
 * @see {@link https://wiki.yandex-team.ru/testirovanie/functesting/billing/integrationinterface/#makeoebspayment}
 */

export default {
    body: ({invoiceNumber, paymentValue}: {[x: string]: string | number}) => `
        <?xml version="1.0"?>
        <methodCall>
            <methodName>TestBalance.MakeOEBSPayment</methodName>
            <params>
                <param>
                    <value>
                        <struct>
                            <member>
                                <name>InvoiceID</name>
                                <value>${invoiceNumber}</value>
                            </member>
                            <member>
                                <name>PaymentSum</name>
                                <value>${paymentValue}</value>
                            </member>
                        </struct>
                    </value>
                </param>
            </params>
        </methodCall>
    `,
};
