/* istanbul ignore file */

export default {
    body: ({invoiceId}: {[x: string]: string}) => `
        <?xml version="1.0"?>
        <methodCall>
            <methodName>TestBalance.ExecuteSQL</methodName>
            <params>
                <param>
                    <value>balance</value>
                </param>
                <param>
                    <value>SELECT external_id FROM t_invoice WHERE id = ${invoiceId}</value>
                </param>
            </params>
        </methodCall>
    `,
};
