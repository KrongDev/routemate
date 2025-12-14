#!/bin/bash
set -e

echo "Waiting for MySQL to start..."
until mysqladmin ping -h localhost -uroot -proot --silent; do
  sleep 1
done

echo "Ensuring replication user exists on slave..."
mysql -uroot -proot <<EOF
CREATE USER IF NOT EXISTS 'routemate'@'%'
IDENTIFIED WITH mysql_native_password BY 'routemate';
CREATE DATABASE routemate_db;
GRANT ALL PRIVILEGES ON routemate_db.* TO 'routemate'@'%';
GRANT REPLICATION SLAVE ON *.* TO 'routemate'@'%';
FLUSH PRIVILEGES;
EOF

echo "Waiting for master to be reachable..."
until mysql -h routemate-mysql-master -uroot -proot -e "SELECT 1" >/dev/null 2>&1; do
  sleep 2
done

echo "Fetching master status..."
MASTER_STATUS=$(mysql -h routemate-mysql-master -uroot -proot -e "SHOW MASTER STATUS\G")
MASTER_LOG_FILE=$(echo "$MASTER_STATUS" | grep File: | awk '{print $2}')
MASTER_LOG_POS=$(echo "$MASTER_STATUS" | grep Position: | awk '{print $2}')

echo "Master log file: $MASTER_LOG_FILE"
echo "Master log pos : $MASTER_LOG_POS"

echo "Configuring replication..."
mysql -uroot -proot <<EOF
STOP REPLICA;
RESET REPLICA ALL;

CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='routemate-mysql-master',
  SOURCE_PORT=3306,
  SOURCE_USER='routemate',
  SOURCE_PASSWORD='routemate',
  SOURCE_LOG_FILE='$MASTER_LOG_FILE',
  SOURCE_LOG_POS=$MASTER_LOG_POS;

START REPLICA;
EOF

echo "Replication configured successfully."
