/*
 * @title Создать тестовое обращение
 */

def mentions = api.youScan.extractMentions(obj.originalBody)
mentions.each { mention ->
    api.bcp.create('ticket$testYouScan', [
        title            : mention.subject,
        clientName       : mention.clientName,
        service          : 'testService',
        channel          : 'mail',
        youScanMentionUrl: [
            href : mention.youScanMentionUrl,
            value: 'Упоминание в YouScan'
        ],
        mentionSourceUrl : [
            href : mention.mentionSourceUrl,
            value: mention.mentionSourceTitle
        ],
        requestDate      : mention.mentionDate,
        description      : mention.body,
        '@comment'       : api.comments.preparePublic(mention.body)
    ])
}
