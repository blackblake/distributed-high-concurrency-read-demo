const $ = (id) => document.getElementById(id);

async function call(method, url, body) {
    const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: body ? JSON.stringify(body) : undefined
    });
    const upstream = res.headers.get("X-Upstream-Addr") || "-";
    let text;
    try { text = JSON.stringify(await res.json(), null, 2); }
    catch (_) { text = await res.text(); }
    return `HTTP ${res.status}  upstream=${upstream}\n${text}`;
}

$("btnQuery").onclick = async () => {
    $("productOutput").textContent = await call("GET", `/api/products/${$("productId").value}`);
};

$("btnWarm").onclick = async () => {
    $("productOutput").textContent = await call("POST", `/api/debug/cache/warm/${$("productId").value}`);
};

$("btnSeckill").onclick = async () => {
    $("seckillOutput").textContent = await call("POST", "/api/seckill/orders", {
        userId: Number($("userId").value),
        productId: Number($("skProductId").value)
    });
};

$("btnOrderById").onclick = async () => {
    const id = $("orderId").value.trim();
    if (!id) { $("orderOutput").textContent = "请填写订单 ID"; return; }
    $("orderOutput").textContent = await call("GET", `/api/orders/${id}`);
};

$("btnOrderByUser").onclick = async () => {
    $("orderOutput").textContent = await call("GET", `/api/orders?userId=${$("orderUserId").value}`);
};
