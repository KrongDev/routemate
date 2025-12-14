CREATE USER IF NOT EXISTS 'routemate'@'%' IDENTIFIED WITH mysql_native_password BY 'routemate';
GRANT REPLICATION SLAVE ON *.* TO 'routemate'@'%';

GRANT ALL PRIVILEGES ON routemate_db.* TO 'routemate'@'%';

FLUSH PRIVILEGES;
