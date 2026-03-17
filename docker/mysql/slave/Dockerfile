FROM mysql:8.4

COPY conf/my.cnf /etc/mysql/conf.d/my.cnf
COPY init/01-init-slave.sh /docker-entrypoint-initdb.d/01-init-slave.sh

RUN chmod +x /docker-entrypoint-initdb.d/01-init-slave.sh
