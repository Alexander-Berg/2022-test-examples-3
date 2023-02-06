-- auto-generated definition
CREATE TABLE IF NOT EXISTS wmwhse1.TASK_ROUTER_NOTIFICATION_HISTORY
(
    UUID                 NVARCHAR(36)                  NOT NULL,
    SENT_TIME            DATETIME DEFAULT now()        NOT NULL,
    SENDER               NVARCHAR(256)                 NOT NULL,
    RECEIVER             NVARCHAR(256)                 NOT NULL,
    TOPIC                NVARCHAR(256)                 NOT NULL,
    NOTIFICATION_MESSAGE NVARCHAR(MAX)                 NOT NULL,
    REACTION             NVARCHAR(100),
    device_id_sent       BIGINT,
    session_id_sent      NVARCHAR(128),
    subscription_id_sent NVARCHAR(256),
    device_id_received   BIGINT,
    id                   BIGINT IDENTITY PRIMARY KEY
);
