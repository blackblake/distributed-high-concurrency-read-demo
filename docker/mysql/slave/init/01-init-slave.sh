#!/bin/sh
set -e

until mysql -h mysql-master -uroot -prootpass -e "SELECT 1" >/dev/null 2>&1; do
  echo "waiting for mysql-master..."
  sleep 2
done

mysql -uroot -prootpass <<'SQL'
SET GLOBAL super_read_only = OFF;
SET GLOBAL read_only = OFF;
STOP REPLICA;
RESET REPLICA ALL;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-master',
  SOURCE_PORT=3306,
  SOURCE_USER='repl',
  SOURCE_PASSWORD='replpass',
  SOURCE_AUTO_POSITION=1,
  GET_SOURCE_PUBLIC_KEY=1;
START REPLICA;
SET GLOBAL read_only = ON;
SET GLOBAL super_read_only = ON;
SQL
