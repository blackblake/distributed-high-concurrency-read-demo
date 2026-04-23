#!/bin/bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"

echo "[1] create product"
CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/products" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Homework Demo Product",
    "price":299.00,
    "stock":50,
    "description":"Created by demo script",
    "status":"ONLINE"
  }')
echo "${CREATE_RESPONSE}"

PRODUCT_ID=$(echo "${CREATE_RESPONSE}" | sed -E 's/.*"id":([0-9]+).*/\1/')

echo
echo "[2] first read, expect SLAVE_DB"
curl -s "${BASE_URL}/api/products/${PRODUCT_ID}"

echo
echo
echo "[3] second read, expect CACHE"
curl -s "${BASE_URL}/api/products/${PRODUCT_ID}"

echo
echo
echo "[4] update product, expect MASTER_DB"
curl -s -X PUT "${BASE_URL}/api/products/${PRODUCT_ID}" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Homework Demo Product Updated",
    "price":399.00,
    "stock":35,
    "description":"Updated by demo script",
    "status":"ONLINE"
  }'

echo
echo
echo "[5] read after update, expect cache rebuild"
curl -s "${BASE_URL}/api/products/${PRODUCT_ID}"

echo
echo
echo "[6] query non-existing product twice, second time should hit NULL_CACHE"
curl -s "${BASE_URL}/api/products/999999"
echo
curl -s "${BASE_URL}/api/products/999999"

echo
