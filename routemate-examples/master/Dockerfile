FROM mysql:8.0

COPY my.cnf /etc/mysql/my.cnf
RUN chmod 644 /etc/mysql/my.cnf

COPY init.sql /docker-entrypoint-initdb.d/init.sql