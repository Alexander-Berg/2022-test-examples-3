import boto3
from botocore.exceptions import ClientError
import json


def main():
    sqs_queue = 'audit_logs'
    sqs_url = "http://localhost:9324"

    client = boto3.client('sqs', region_name='yandex', endpoint_url=sqs_url,
                          aws_access_key_id='', aws_secret_access_key="unused",
                          aws_session_token='')

    message = json.dumps({
        'dt': '1581516859',
        'microsecond': '378',
        'user_uid': '5335353',
        'object_id': '55335',
        'object_type': 'object_type',
        'action': 'BAN',
        'service': 'srv',
    })

    try:
        client.create_queue(QueueName=sqs_queue, Attributes={'DelaySeconds': '1'})
    except ClientError:
        pass

    response = client.send_message(
            QueueUrl=f'{sqs_url}/queue/{sqs_queue}',
            MessageBody=message
    )

    print(response)


if __name__ == '__main__':
    main()
