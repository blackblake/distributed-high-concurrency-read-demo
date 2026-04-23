#!/bin/bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
PRODUCT_ID="${PRODUCT_ID:-1}"
STOCK="${STOCK:-10}"

echo "[1] init seckill stock for product ${PRODUCT_ID}: ${STOCK}"
curl -s -X POST "${BASE_URL}/api/seckill/stock/${PRODUCT_ID}?quantity=${STOCK}"
echo

echo
echo "[2] concurrent seckill from 20 users (only ${STOCK} should succeed)"
seq 1001 1020 | xargs -n1 -P20 -I{} sh -c "
  curl -s -X POST ${BASE_URL}/api/seckill/orders \
    -H 'Content-Type: application/json' \
    -d '{\"userId\":{},\"productId\":${PRODUCT_ID}}'
  echo
"

echo
echo "[3] remaining stock (should be 0)"
curl -s "${BASE_URL}/api/seckill/stock/${PRODUCT_ID}"; echo

echo
echo "[4] wait 2s for Kafka consumers to land orders in DB"
sleep 2

echo
echo "[5] user 1001's orders"
curl -s "${BASE_URL}/api/orders?userId=1001"; echo

echo
echo "[6] duplicate seckill for user 1001 should be rejected"
curl -s -X POST "${BASE_URL}/api/seckill/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"userId\":1001,\"productId\":${PRODUCT_ID}}"; echo
