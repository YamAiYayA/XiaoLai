(() => {
  const API_BASE = "https://api.guoziai.com/20260801/index.php?r=";
  const STORAGE_KEY = "yanyan_web_session_v1";

  const EVENT_TYPES = [
    { value: "feeding_formula", label: "奶粉", needsAmount: true },
    { value: "feeding_breast", label: "亲喂", needsSide: true },
    { value: "feeding_warm_breast", label: "热母乳", needsAmount: true },
    { value: "sleep_start", label: "睡觉", instant: true },
    { value: "sleep_end", label: "醒来", instant: true },
    { value: "poop", label: "拉粑粑", instant: true },
    { value: "care", label: "护理", instant: true },
    { value: "butt_clean", label: "洗屁股", instant: true },
    { value: "gas_exercise", label: "排气操", instant: true },
    { value: "jaundice_check", label: "测黄疸", needsJaundice: true },
    { value: "bath", label: "洗澡", instant: true },
    { value: "note", label: "备注", needsNote: true },
    { value: "custom", label: "自定义", needsCustom: true },
  ];

  const DEFAULT_FAVORITES = [40, 45, 60, 65, 70];

  const state = {
    booting: true,
    tab: "home",
    token: "",
    displayName: "",
    context: null,
    dashboard: null,
    timelineDate: todayKey(),
    timeline: [],
    todos: [],
    loading: false,
    message: "",
    error: "",
    loginMode: "token",
    loginToken: "",
    loginInvite: "",
    loginName: "",
    editor: defaultEditor(),
    modal: null,
  };

  const app = document.getElementById("app");

  function defaultEditor() {
    return {
      type: "feeding_formula",
      amount: "40",
      side: "left",
      durationMin: "15",
      note: "",
      customLabel: "",
      forehead: "",
      chest: "",
      followNow: true,
      occurredAt: Date.now(),
      favorites: loadFavorites(),
      showMore: false,
    };
  }

  function todayKey(ts = Date.now()) {
    const d = new Date(ts);
    const p = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  }

  function formatTime(ts) {
    const d = new Date(ts);
    const p = (n) => String(n).padStart(2, "0");
    return `${p(d.getHours())}:${p(d.getMinutes())}`;
  }

  function formatDateTime(ts) {
    return `${todayKey(ts)} ${formatTime(ts)}`;
  }

  function elapsedLabel(ms) {
    const totalMin = Math.max(0, Math.floor(ms / 60000));
    const days = Math.floor(totalMin / (60 * 24));
    const hours = Math.floor((totalMin % (60 * 24)) / 60);
    const minutes = totalMin % 60;
    if (days > 0) return `${days}天${hours}小时${minutes}分钟`;
    if (hours > 0) return `${hours}小时${minutes}分钟`;
    return `${minutes}分钟`;
  }

  function setCookie(name, value, days = 365) {
    const maxAge = days * 24 * 60 * 60;
    document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}; path=/yanyan; max-age=${maxAge}; SameSite=Lax`;
    // also set on root path in case site is opened without trailing rules
    document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}; path=/; max-age=${maxAge}; SameSite=Lax`;
  }

  function getCookie(name) {
    const key = `${encodeURIComponent(name)}=`;
    const parts = document.cookie.split(";");
    for (const part of parts) {
      const item = part.trim();
      if (item.startsWith(key)) {
        return decodeURIComponent(item.slice(key.length));
      }
    }
    return "";
  }

  function clearCookie(name) {
    document.cookie = `${encodeURIComponent(name)}=; path=/yanyan; max-age=0; SameSite=Lax`;
    document.cookie = `${encodeURIComponent(name)}=; path=/; max-age=0; SameSite=Lax`;
  }

  function loadSession() {
    try {
      const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
      if (raw && raw.token) return raw;
    } catch {}
    const token = getCookie("yanyan_token");
    const displayName = getCookie("yanyan_name");
    if (token) return { token, displayName };
    return {};
  }

  function saveSession() {
    const payload = {
      token: state.token,
      displayName: state.displayName,
    };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    } catch {}
    if (state.token) {
      setCookie("yanyan_token", state.token, 365);
      setCookie("yanyan_name", state.displayName || "", 365);
    }
  }

  function clearSession() {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {}
    clearCookie("yanyan_token");
    clearCookie("yanyan_name");
  }

  function isAuthError(message) {
    const text = String(message || "");
    return /登录|token|未授权|失效|无权限|请先登录/i.test(text);
  }

  function loadFavorites() {
    try {
      const raw = JSON.parse(localStorage.getItem("yanyan_formula_favorites") || "null");
      if (Array.isArray(raw) && raw.length) return raw.map(Number).filter(Boolean);
    } catch {}
    return [...DEFAULT_FAVORITES];
  }

  function saveFavorites(list) {
    localStorage.setItem("yanyan_formula_favorites", JSON.stringify(list));
  }

  async function api(route, { method = "GET", body, token } = {}) {
    const headers = { "Content-Type": "application/json" };
    const t = token ?? state.token;
    if (t) headers["X-Access-Token"] = t;
    const res = await fetch(API_BASE + route, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    const json = await res.json().catch(() => ({}));
    if (!json.success) {
      throw new Error(json.message || "请求失败");
    }
    return json.data;
  }

  function ofType(value) {
    return EVENT_TYPES.find((x) => x.value === value) || { value, label: value };
  }

  function summarizeRecord(record) {
    const payload = record.payload || {};
    switch (record.eventType) {
      case "feeding_formula":
        return `奶粉 ${payload.amountMl || 0}ml`;
      case "feeding_warm_breast":
        return `热母乳 ${payload.amountMl || 0}ml`;
      case "feeding_breast": {
        const side = payload.side === "left" ? "左侧" : payload.side === "right" ? "右侧" : payload.side || "";
        return `亲喂 ${side} ${payload.durationMin || 0}分钟`;
      }
      case "sleep_start":
        return "开始睡觉";
      case "sleep_end":
        return "醒来";
      case "poop":
        return "拉粑粑";
      case "care":
        return payload.note ? `护理 · ${payload.note}` : "护理";
      case "butt_clean":
        return "洗屁股";
      case "gas_exercise":
        return "排气操";
      case "jaundice_check":
        return `黄疸 额${payload.foreheadValue || 0} / 胸${payload.chestValue || 0}`;
      case "bath":
        return "洗澡";
      case "note":
        return payload.note || "备注";
      case "custom":
        return payload.customLabel || "自定义";
      default:
        return ofType(record.eventType).label;
    }
  }

  function feedCount(summary = {}) {
    return (summary.formulaFeedCount || 0) + (summary.warmBreastFeedCount || 0) + (summary.breastFeedCount || 0);
  }

  function summarizeLocal(records) {
    const s = {
      formulaAmountTotal: 0,
      warmBreastAmountTotal: 0,
      formulaFeedCount: 0,
      warmBreastFeedCount: 0,
      breastFeedCount: 0,
      poopCount: 0,
      totalCount: records.length,
    };
    records.forEach((r) => {
      const p = r.payload || {};
      if (r.eventType === "feeding_formula") {
        s.formulaFeedCount++;
        s.formulaAmountTotal += Number(p.amountMl || 0);
      } else if (r.eventType === "feeding_warm_breast") {
        s.warmBreastFeedCount++;
        s.warmBreastAmountTotal += Number(p.amountMl || 0);
      } else if (r.eventType === "feeding_breast") {
        s.breastFeedCount++;
      } else if (r.eventType === "poop") {
        s.poopCount++;
      }
    });
    return s;
  }

  function setMessage(msg, isError = false) {
    state.message = isError ? "" : msg;
    state.error = isError ? msg : "";
  }

  async function bootstrap() {
    state.booting = true;
    render();
    const saved = loadSession();
    if (!saved.token) {
      state.booting = false;
      render();
      return;
    }
    try {
      state.token = saved.token;
      state.displayName = saved.displayName || "";
      state.context = await api("bootstrap/context");
      // refresh cookie/localStorage expiry on successful auto-login
      saveSession();
      await refreshAll();
      state.booting = false;
      render();
    } catch (e) {
      state.booting = false;
      if (isAuthError(e.message)) {
        state.token = "";
        state.context = null;
        clearSession();
        setMessage(e.message || "登录失效，请重新登录", true);
      } else {
        // 网络抖动时保留登录态，避免每次进来都要重新登录
        setMessage(e.message || "网络异常，请下拉刷新重试", true);
      }
      render();
    }
  }

  async function refreshAll() {
    const babyId = state.context?.baby?.id;
    if (!state.token || !babyId) return;
    state.loading = true;
    render();
    try {
      const dateKey = todayKey();
      const [dashboard, timeline, todos] = await Promise.all([
        api(`stats/dashboard&babyId=${encodeURIComponent(babyId)}&dateKey=${encodeURIComponent(dateKey)}&recentLimit=12`),
        api(`records/listByDate&babyId=${encodeURIComponent(babyId)}&dateKey=${encodeURIComponent(state.timelineDate)}`),
        api("todos/list"),
      ]);
      state.dashboard = dashboard;
      state.timeline = timeline || [];
      state.todos = todos || [];
      setMessage("");
    } catch (e) {
      setMessage(e.message || "刷新失败", true);
    } finally {
      state.loading = false;
      render();
    }
  }

  async function loginWithToken() {
    state.loading = true;
    setMessage("");
    render();
    try {
      const member = await api("auth/login", {
        method: "POST",
        body: { token: state.loginToken.trim() },
        token: "",
      });
      state.token = member.accessToken;
      state.displayName = member.displayName;
      saveSession();
      state.context = await api("bootstrap/context");
      await refreshAll();
      state.tab = "home";
      setMessage(`欢迎，${member.displayName}`);
    } catch (e) {
      setMessage(e.message || "登录失败", true);
    } finally {
      state.loading = false;
      render();
    }
  }

  async function loginWithInvite() {
    state.loading = true;
    setMessage("");
    render();
    try {
      const member = await api("auth/login", {
        method: "POST",
        body: {
          inviteCode: state.loginInvite.trim(),
          displayName: state.loginName.trim(),
        },
        token: "",
      });
      state.token = member.accessToken;
      state.displayName = member.displayName;
      saveSession();
      state.context = await api("bootstrap/context");
      await refreshAll();
      state.tab = "home";
      setMessage(`欢迎，${member.displayName}`);
    } catch (e) {
      setMessage(e.message || "登录失败", true);
    } finally {
      state.loading = false;
      render();
    }
  }

  function logout() {
    state.token = "";
    state.displayName = "";
    state.context = null;
    state.dashboard = null;
    state.timeline = [];
    state.todos = [];
    clearSession();
    setMessage("已退出");
    render();
  }

  async function createRecord(eventType, payload, occurredAt = Date.now()) {
    const babyId = state.context?.baby?.id;
    if (!babyId) throw new Error("未找到宝宝档案");
    const dateKey = todayKey(occurredAt);
    await api("records/create", {
      method: "POST",
      body: { babyId, eventType, dateKey, occurredAt, payload },
    });
    await refreshAll();
  }

  async function deleteRecord(id) {
    await api("records/delete", { method: "POST", body: { id } });
    await refreshAll();
  }

  async function saveEditor(instant = false) {
    const opt = ofType(state.editor.type);
    const payload = {};
    if (opt.needsAmount) payload.amountMl = Number(state.editor.amount || 0);
    if (opt.needsSide) {
      payload.side = state.editor.side;
      payload.durationMin = Number(state.editor.durationMin || 0);
    }
    if (opt.needsJaundice) {
      payload.foreheadValue = Number(state.editor.forehead || 0);
      payload.chestValue = Number(state.editor.chest || 0);
    }
    if (opt.needsCustom) payload.customLabel = state.editor.customLabel.trim();
    if (opt.needsNote || state.editor.note.trim()) payload.note = state.editor.note.trim();

    const ts = instant || state.editor.followNow ? Date.now() : state.editor.occurredAt;
    state.loading = true;
    render();
    try {
      await createRecord(opt.value, payload, ts);
      state.modal = {
        title: "保存成功",
        text: `${opt.label}已保存`,
        confirmText: "好的",
        onConfirm: () => {
          state.modal = null;
          render();
        },
      };
      if (!instant) {
        state.editor.note = "";
        state.editor.customLabel = "";
      }
    } catch (e) {
      setMessage(e.message || "保存失败", true);
    } finally {
      state.loading = false;
      render();
    }
  }

  function shiftTimeline(days) {
    const d = new Date(state.timelineDate + "T00:00:00");
    d.setDate(d.getDate() + days);
    state.timelineDate = todayKey(d.getTime());
    refreshAll();
  }

  function lastFeedInfo() {
    const records = state.dashboard?.recentRecords || [];
    const feed = records.find((r) =>
      ["feeding_formula", "feeding_breast", "feeding_warm_breast"].includes(r.eventType),
    );
    if (!feed) return null;
    return {
      title: summarizeRecord(feed),
      time: formatDateTime(feed.occurredAt),
      elapsed: elapsedLabel(Date.now() - feed.occurredAt),
    };
  }

  function birthdayLabel() {
    const birthday = state.context?.baby?.birthday;
    if (!birthday) return "宝宝记录";
    const birth = new Date(birthday + "T00:00:00").getTime();
    const now = Date.now();
    const daysTotal = Math.max(0, Math.floor((now - birth) / 86400000));
    const months = Math.floor(daysTotal / 30);
    const days = daysTotal % 30;
    const hours = Math.floor(((now - birth) % 86400000) / 3600000);
    return `宝宝出生已 ${months}月${days}天${hours}小时`;
  }

  function esc(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  function renderLogin() {
    return `
      <div class="shell">
        <div class="login-hero">
          <div class="brand">妍妍养成记</div>
          <p class="sub">给手机浏览器用的家庭版，和 App 共用同一套记录。</p>
        </div>
        <div class="card">
          <div class="chips" style="margin-bottom:12px">
            <button class="chip ${state.loginMode === "token" ? "active" : ""}" data-action="login-mode" data-mode="token">成员 Token</button>
            <button class="chip ${state.loginMode === "invite" ? "active" : ""}" data-action="login-mode" data-mode="invite">邀请码</button>
          </div>
          ${
            state.loginMode === "token"
              ? `
            <div class="field">
              <label>访问 Token</label>
              <input id="loginToken" placeholder="爸爸 / 妈妈安装页生成的 token" value="${esc(state.loginToken)}" />
            </div>
            <button class="btn block" data-action="login-token" ${state.loading ? "disabled" : ""}>登录</button>
          `
              : `
            <div class="field">
              <label>家庭邀请码</label>
              <input id="loginInvite" placeholder="例如 46XSCU" value="${esc(state.loginInvite)}" />
            </div>
            <div class="field">
              <label>你的称呼</label>
              <input id="loginName" placeholder="妈妈 / 爸爸 / 月嫂" value="${esc(state.loginName)}" />
            </div>
            <button class="btn block" data-action="login-invite" ${state.loading ? "disabled" : ""}>加入并登录</button>
          `
          }
          ${state.error ? `<div class="msg error">${esc(state.error)}</div>` : ""}
          ${state.message ? `<div class="msg">${esc(state.message)}</div>` : ""}
          <p class="tiny" style="margin-top:12px">登录一次会自动记住（约一年）。请用 <b>Safari</b> 打开，不要用微信内置浏览器；再点「分享 → 添加到主屏幕」，以后从桌面进就不用反复登录。</p>
        </div>
      </div>
    `;
  }

  function nav() {
    const items = [
      ["home", "🏠", "首页"],
      ["records", "📅", "记录"],
      ["add", "＋", "新增"],
      ["todos", "✅", "待办"],
      ["me", "👤", "我的"],
    ];
    return `
      <nav class="nav">
        ${items
          .map(
            ([id, icon, label]) => `
          <button class="${state.tab === id ? "active" : ""}" data-action="tab" data-tab="${id}">
            <span class="icon">${icon}</span>${label}
          </button>`,
          )
          .join("")}
      </nav>
    `;
  }

  function renderHome() {
    const today = state.dashboard?.todaySummary || {};
    const week = state.dashboard?.weekSummary || {};
    const recent = state.dashboard?.recentRecords || [];
    const last = lastFeedInfo();
    const nick = state.context?.baby?.nickname || "妍妍";
    return `
      <div class="shell">
        <div class="brand">${esc(nick)}养成记</div>
        <p class="sub">${esc(birthdayLabel())}</p>
        <div class="card">
          <h3>上次吃奶</h3>
          ${
            last
              ? `<div class="stat"><div class="v">${esc(last.title)}</div><div class="k">${esc(last.time)} · 已过 ${esc(last.elapsed)}</div></div>`
              : `<div class="empty">暂无吃奶记录</div>`
          }
        </div>
        <div class="card">
          <h3>今天概览</h3>
          <div class="stats">
            <div class="stat"><div class="k">奶粉总量</div><div class="v">${Math.round(today.formulaAmountTotal || 0)}ml</div></div>
            <div class="stat"><div class="k">热母乳</div><div class="v">${Math.round(today.warmBreastAmountTotal || 0)}ml</div></div>
            <div class="stat"><div class="k">喂养次数</div><div class="v">${feedCount(today)}次</div></div>
            <div class="stat"><div class="k">排便</div><div class="v">${today.poopCount || 0}次</div></div>
          </div>
        </div>
        <div class="card">
          <div class="row">
            <h3 class="grow" style="margin:0">最近记录</h3>
            <button class="btn ghost" data-action="tab" data-tab="records">查看全部</button>
            <button class="btn secondary" data-action="refresh" ${state.loading ? "disabled" : ""}>刷新</button>
          </div>
          ${
            recent.length
              ? recent
                  .map(
                    (r) => `
              <div class="list-item">
                <div class="grow">
                  <div class="title">${esc(summarizeRecord(r))}</div>
                  <div class="meta">${esc(ofType(r.eventType).label)} · ${esc(r.createdByName || "家人")}</div>
                </div>
                <div class="time">${esc(formatTime(r.occurredAt))}</div>
              </div>`,
                  )
                  .join("")
              : `<div class="empty">今天还没有记录</div>`
          }
        </div>
        <div class="card">
          <h3>近 7 天</h3>
          <div class="stats">
            <div class="stat"><div class="k">奶粉总量</div><div class="v">${Math.round(week.formulaAmountTotal || 0)}ml</div></div>
            <div class="stat"><div class="k">热母乳</div><div class="v">${Math.round(week.warmBreastAmountTotal || 0)}ml</div></div>
          </div>
        </div>
        ${state.message ? `<div class="msg">${esc(state.message)}</div>` : ""}
        ${state.error ? `<div class="msg error">${esc(state.error)}</div>` : ""}
      </div>
      ${nav()}
    `;
  }

  function renderRecords() {
    const summary = summarizeLocal(state.timeline);
    return `
      <div class="shell">
        <div class="brand" style="font-size:1.7rem">记录</div>
        <div class="date-nav">
          <button class="btn secondary" data-action="day-prev">前一天</button>
          <div class="date">${esc(state.timelineDate)}</div>
          <button class="btn secondary" data-action="day-next">后一天</button>
        </div>
        <div class="card">
          <h3>当天汇总</h3>
          <div class="stats">
            <div class="stat"><div class="k">奶粉总量</div><div class="v">${Math.round(summary.formulaAmountTotal)}ml</div></div>
            <div class="stat"><div class="k">热母乳</div><div class="v">${Math.round(summary.warmBreastAmountTotal)}ml</div></div>
            <div class="stat"><div class="k">喂养次数</div><div class="v">${feedCount(summary)}次</div></div>
            <div class="stat"><div class="k">排便</div><div class="v">${summary.poopCount}次</div></div>
          </div>
        </div>
        <div class="card">
          <div class="swipe-hint">点右侧删除可去掉记错的条目</div>
          ${
            state.timeline.length
              ? state.timeline
                  .map(
                    (r) => `
              <div class="list-item">
                <div class="grow">
                  <div class="title">${esc(summarizeRecord(r))}</div>
                  <div class="meta">${esc(ofType(r.eventType).label)} · ${esc(r.createdByName || "家人")}</div>
                </div>
                <div class="time">${esc(formatTime(r.occurredAt))}</div>
                <button class="btn ghost" data-action="delete-record" data-id="${esc(r.id)}">删除</button>
              </div>`,
                  )
                  .join("")
              : `<div class="empty">这一天还没有记录</div>`
          }
        </div>
      </div>
      ${nav()}
    `;
  }

  function renderAdd() {
    const ed = state.editor;
    const opt = ofType(ed.type);
    const displayTs = ed.followNow ? Date.now() : ed.occurredAt;
    const moreValues = [];
    for (let i = 20; i <= 200; i += 5) {
      if (!ed.favorites.includes(i)) moreValues.push(i);
    }
    return `
      <div class="shell">
        <div class="brand" style="font-size:1.7rem">新增</div>
        <p class="sub">${esc(birthdayLabel())}</p>
        <div class="card">
          <h3>记录时间</h3>
          <div class="row" style="gap:10px; margin-bottom:10px">
            <div class="stat grow">
              <div class="k">日期</div>
              <div class="v" style="font-size:1rem">${esc(todayKey(displayTs))}</div>
              <button class="btn ghost" data-action="pick-date">点击修改</button>
            </div>
            <div class="stat grow">
              <div class="k">时间</div>
              <div class="v" style="font-size:1rem">${esc(formatTime(displayTs))}${ed.followNow ? ":.." : ""}</div>
              <button class="btn ghost" data-action="pick-time">点击修改</button>
            </div>
          </div>
          <div class="chips">
            ${[1, 3, 5, 10, 15]
              .map((m) => `<button class="chip" data-action="mins-ago" data-mins="${m}">${m}分钟前</button>`)
              .join("")}
            <button class="chip ${ed.followNow ? "active" : ""}" data-action="now">现在</button>
          </div>
          <input id="hiddenDate" type="date" hidden />
          <input id="hiddenTime" type="time" hidden />
        </div>

        <div class="card">
          <h3>记录类型</h3>
          <div class="chips">
            ${EVENT_TYPES.map(
              (t) => `
              <button class="chip ${ed.type === t.value ? "active" : ""}" data-action="pick-type" data-type="${t.value}">${esc(t.label)}</button>
            `,
            ).join("")}
          </div>
        </div>

        ${
          opt.needsAmount
            ? `
          <div class="card">
            <h3>奶量（ml）</h3>
            <div class="field">
              <input id="amountInput" inputmode="numeric" value="${esc(ed.amount)}" />
            </div>
            <div class="chips">
              ${ed.favorites
                .map(
                  (v) => `
                <button class="chip ${String(v) === String(ed.amount) ? "active" : ""}" data-action="amount" data-ml="${v}">${v}ml</button>
              `,
                )
                .join("")}
              <button class="chip" data-action="toggle-more">${ed.showMore ? "收起" : "更多"}</button>
            </div>
            <p class="tiny">点「更多」可把奶量加到常驻。</p>
            ${
              ed.showMore
                ? `<div class="chips" style="margin-top:8px">${moreValues
                    .map((v) => `<button class="chip" data-action="pin-amount" data-ml="${v}">${v}ml</button>`)
                    .join("")}</div>`
                : ""
            }
          </div>`
            : ""
        }

        ${
          opt.needsSide
            ? `
          <div class="card">
            <h3>侧别 / 时长</h3>
            <div class="chips" style="margin-bottom:10px">
              <button class="chip ${ed.side === "left" ? "active" : ""}" data-action="side" data-side="left">左侧</button>
              <button class="chip ${ed.side === "right" ? "active" : ""}" data-action="side" data-side="right">右侧</button>
            </div>
            <div class="field">
              <label>时长（分钟）</label>
              <input id="durationInput" inputmode="numeric" value="${esc(ed.durationMin)}" />
            </div>
          </div>`
            : ""
        }

        ${
          opt.needsJaundice
            ? `
          <div class="card">
            <h3>测黄疸</h3>
            <div class="field"><label>额头</label><input id="foreheadInput" value="${esc(ed.forehead)}" /></div>
            <div class="field"><label>胸口</label><input id="chestInput" value="${esc(ed.chest)}" /></div>
          </div>`
            : ""
        }

        ${
          opt.needsCustom
            ? `
          <div class="card">
            <div class="field"><label>做了什么</label><input id="customInput" maxlength="20" value="${esc(ed.customLabel)}" placeholder="如：游泳、早教" /></div>
          </div>`
            : ""
        }

        <div class="card">
          <div class="field">
            <label>备注</label>
            <textarea id="noteInput" rows="2" placeholder="可选">${esc(ed.note)}</textarea>
          </div>
          <div class="row">
            <button class="btn grow" data-action="save" ${state.loading ? "disabled" : ""}>保存记录</button>
            <button class="btn secondary grow" data-action="reset-editor">清空重写</button>
          </div>
          ${state.error ? `<div class="msg error">${esc(state.error)}</div>` : ""}
        </div>
      </div>
      ${nav()}
    `;
  }

  function renderTodos() {
    const pending = state.todos.filter((t) => t.status !== "done" && !t.isDeleted);
    const done = state.todos.filter((t) => t.status === "done" && !t.isDeleted);
    return `
      <div class="shell">
        <div class="brand" style="font-size:1.7rem">待办</div>
        <div class="card">
          <div class="row">
            <input id="todoTitle" class="grow" placeholder="新建待办" style="border:1px solid var(--line);border-radius:12px;padding:11px 12px;background:#fff" />
            <button class="btn" data-action="add-todo">新建</button>
          </div>
        </div>
        <div class="card">
          <h3>待完成 · ${pending.length}</h3>
          ${
            pending.length
              ? pending
                  .map(
                    (t) => `
              <div class="list-item">
                <button class="chip" data-action="toggle-todo" data-id="${esc(t.id)}">完成</button>
                <div class="grow"><div class="title">${esc(t.title)}</div><div class="meta">${t.category === "adult" ? "大人" : "宝宝"}</div></div>
              </div>`,
                  )
                  .join("")
              : `<div class="empty">暂无待办</div>`
          }
        </div>
        <div class="card">
          <h3>已完成 · ${done.length}</h3>
          ${done
            .map(
              (t) => `
            <div class="list-item">
              <button class="chip" data-action="toggle-todo" data-id="${esc(t.id)}">恢复</button>
              <div class="grow"><div class="title" style="text-decoration:line-through;opacity:.7">${esc(t.title)}</div></div>
            </div>`,
            )
            .join("")}
        </div>
      </div>
      ${nav()}
    `;
  }

  function renderMe() {
    const ctx = state.context || {};
    return `
      <div class="shell">
        <div class="brand" style="font-size:1.7rem">我的</div>
        <div class="card">
          <div class="list-item"><div class="grow"><div class="meta">家庭</div><div class="title">${esc(ctx.family?.name || "-")}</div></div></div>
          <div class="list-item"><div class="grow"><div class="meta">邀请码</div><div class="title">${esc(ctx.family?.inviteCode || "-")}</div></div></div>
          <div class="list-item"><div class="grow"><div class="meta">宝宝</div><div class="title">${esc(ctx.baby?.nickname || "-")}</div></div></div>
          <div class="list-item"><div class="grow"><div class="meta">生日</div><div class="title">${esc(ctx.baby?.birthday || "-")}</div></div></div>
          <div class="list-item"><div class="grow"><div class="meta">当前身份</div><div class="title">${esc(ctx.member?.displayName || state.displayName || "-")}</div></div></div>
        </div>
        <button class="btn secondary block" data-action="refresh">刷新资料</button>
        <div style="height:8px"></div>
        <button class="btn danger block" data-action="logout">退出登录</button>
      </div>
      ${nav()}
    `;
  }

  function renderModal() {
    if (!state.modal) return "";
    const m = state.modal;
    return `
      <div class="modal-mask" data-action="close-modal">
        <div class="modal" onclick="event.stopPropagation()">
          <h3>${esc(m.title)}</h3>
          <p>${esc(m.text)}</p>
          <div class="actions">
            ${m.cancelText ? `<button class="btn secondary" data-action="modal-cancel">${esc(m.cancelText)}</button>` : ""}
            <button class="btn" data-action="modal-confirm">${esc(m.confirmText || "确定")}</button>
          </div>
        </div>
      </div>
    `;
  }

  function render() {
    if (state.booting) {
      app.innerHTML = `<div class="shell"><div class="empty">加载中…</div></div>`;
      return;
    }
    if (!state.token) {
      app.innerHTML = renderLogin() + renderModal();
      bind();
      return;
    }
    if (!state.context) {
      app.innerHTML = `
        <div class="shell">
          <div class="brand" style="font-size:1.7rem">妍妍养成记</div>
          <div class="card">
            <div class="empty">已记住登录，但这次没连上服务器。</div>
            ${state.error ? `<div class="msg error">${esc(state.error)}</div>` : ""}
            <button class="btn block" data-action="retry-boot">重新连接</button>
            <div style="height:8px"></div>
            <button class="btn secondary block" data-action="logout">退出登录</button>
          </div>
        </div>
      ` + renderModal();
      bind();
      return;
    }
    let body = "";
    if (state.tab === "home") body = renderHome();
    else if (state.tab === "records") body = renderRecords();
    else if (state.tab === "add") body = renderAdd();
    else if (state.tab === "todos") body = renderTodos();
    else body = renderMe();
    app.innerHTML = body + renderModal();
    bind();
  }

  function readEditorFields() {
    const amount = document.getElementById("amountInput");
    const duration = document.getElementById("durationInput");
    const note = document.getElementById("noteInput");
    const custom = document.getElementById("customInput");
    const forehead = document.getElementById("foreheadInput");
    const chest = document.getElementById("chestInput");
    if (amount) state.editor.amount = amount.value.replace(/\D/g, "");
    if (duration) state.editor.durationMin = duration.value.replace(/\D/g, "");
    if (note) state.editor.note = note.value;
    if (custom) state.editor.customLabel = custom.value;
    if (forehead) state.editor.forehead = forehead.value;
    if (chest) state.editor.chest = chest.value;
  }

  function bind() {
    app.onclick = async (e) => {
      const el = e.target.closest("[data-action]");
      if (!el) return;
      const action = el.dataset.action;

      if (action === "login-mode") {
        state.loginMode = el.dataset.mode;
        render();
        return;
      }
      if (action === "retry-boot") {
        await bootstrap();
        return;
      }
      if (action === "login-token") {
        state.loginToken = document.getElementById("loginToken")?.value || "";
        await loginWithToken();
        return;
      }
      if (action === "login-invite") {
        state.loginInvite = document.getElementById("loginInvite")?.value || "";
        state.loginName = document.getElementById("loginName")?.value || "";
        await loginWithInvite();
        return;
      }
      if (action === "tab") {
        state.tab = el.dataset.tab;
        if (state.tab === "add" && state.editor.followNow) state.editor.occurredAt = Date.now();
        render();
        return;
      }
      if (action === "refresh") {
        await refreshAll();
        return;
      }
      if (action === "logout") {
        logout();
        return;
      }
      if (action === "day-prev") {
        shiftTimeline(-1);
        return;
      }
      if (action === "day-next") {
        shiftTimeline(1);
        return;
      }
      if (action === "delete-record") {
        const id = el.dataset.id;
        state.modal = {
          title: "删除这条记录？",
          text: "删除后不可恢复。",
          confirmText: "删除",
          cancelText: "取消",
          onConfirm: async () => {
            state.modal = null;
            try {
              await deleteRecord(id);
              setMessage("已删除");
            } catch (err) {
              setMessage(err.message || "删除失败", true);
            }
            render();
          },
          onCancel: () => {
            state.modal = null;
            render();
          },
        };
        render();
        return;
      }
      if (action === "pick-type") {
        readEditorFields();
        const type = el.dataset.type;
        const opt = ofType(type);
        state.editor.type = type;
        if (opt.instant) {
          state.modal = {
            title: opt.label,
            text: "以此刻记录吗？",
            confirmText: "确定（瞬间记录）",
            cancelText: "取消（进入详情）",
            onConfirm: async () => {
              state.modal = null;
              state.editor.type = type;
              await saveEditor(true);
            },
            onCancel: () => {
              state.modal = null;
              render();
            },
          };
        }
        render();
        return;
      }
      if (action === "mins-ago") {
        readEditorFields();
        state.editor.followNow = false;
        state.editor.occurredAt = Date.now() - Number(el.dataset.mins) * 60000;
        render();
        return;
      }
      if (action === "now") {
        readEditorFields();
        state.editor.followNow = true;
        state.editor.occurredAt = Date.now();
        render();
        return;
      }
      if (action === "pick-date") {
        const input = document.getElementById("hiddenDate");
        input.value = todayKey(state.editor.followNow ? Date.now() : state.editor.occurredAt);
        input.hidden = false;
        input.showPicker?.();
        input.onchange = () => {
          readEditorFields();
          const [y, m, d] = input.value.split("-").map(Number);
          const next = new Date(state.editor.occurredAt);
          next.setFullYear(y, m - 1, d);
          state.editor.followNow = false;
          state.editor.occurredAt = next.getTime();
          render();
        };
        input.click();
        return;
      }
      if (action === "pick-time") {
        const input = document.getElementById("hiddenTime");
        const d = new Date(state.editor.followNow ? Date.now() : state.editor.occurredAt);
        const p = (n) => String(n).padStart(2, "0");
        input.value = `${p(d.getHours())}:${p(d.getMinutes())}`;
        input.hidden = false;
        input.showPicker?.();
        input.onchange = () => {
          readEditorFields();
          const [hh, mm] = input.value.split(":").map(Number);
          const next = new Date(state.editor.occurredAt);
          next.setHours(hh, mm, 0, 0);
          state.editor.followNow = false;
          state.editor.occurredAt = next.getTime();
          render();
        };
        input.click();
        return;
      }
      if (action === "amount") {
        state.editor.amount = el.dataset.ml;
        render();
        return;
      }
      if (action === "pin-amount") {
        const ml = Number(el.dataset.ml);
        state.editor.amount = String(ml);
        if (!state.editor.favorites.includes(ml) && state.editor.favorites.length < 12) {
          state.editor.favorites = [...state.editor.favorites, ml].sort((a, b) => a - b);
          saveFavorites(state.editor.favorites);
        }
        state.editor.showMore = false;
        render();
        return;
      }
      if (action === "toggle-more") {
        readEditorFields();
        state.editor.showMore = !state.editor.showMore;
        render();
        return;
      }
      if (action === "side") {
        readEditorFields();
        state.editor.side = el.dataset.side;
        render();
        return;
      }
      if (action === "save") {
        readEditorFields();
        await saveEditor(false);
        return;
      }
      if (action === "reset-editor") {
        state.editor = defaultEditor();
        render();
        return;
      }
      if (action === "add-todo") {
        const title = document.getElementById("todoTitle")?.value?.trim();
        if (!title) return;
        try {
          await api("todos/create", {
            method: "POST",
            body: {
              title,
              category: "baby",
              babyId: state.context?.baby?.id || "",
            },
          });
          await refreshAll();
        } catch (err) {
          setMessage(err.message || "添加失败", true);
          render();
        }
        return;
      }
      if (action === "toggle-todo") {
        try {
          await api("todos/toggle", { method: "POST", body: { id: el.dataset.id } });
          await refreshAll();
        } catch (err) {
          setMessage(err.message || "更新失败", true);
          render();
        }
        return;
      }
      if (action === "close-modal" || action === "modal-cancel") {
        state.modal?.onCancel?.();
        state.modal = null;
        render();
        return;
      }
      if (action === "modal-confirm") {
        const fn = state.modal?.onConfirm;
        state.modal = null;
        if (fn) await fn();
        else render();
      }
    };
  }

  // Keep "现在" clock ticking on add page
  setInterval(() => {
    if (state.token && state.tab === "add" && state.editor.followNow && !state.modal) {
      const timeCard = app.querySelector('[data-action="pick-time"]')?.previousElementSibling;
      // cheap refresh every 15s
    }
  }, 15000);

  bootstrap();
})();
