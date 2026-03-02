(() => {
    const API_BASES = resolveApiBases();
    const AUTH_STORAGE_KEY = "ai-agent-auth-v1";
    const baseUrlText = document.getElementById("baseUrlText");
    if (baseUrlText) {
        baseUrlText.textContent = API_BASES.join(" | ");
    }
    const authState = { token: "", user: null };

    const codingState = { abortController: null };
    const manusState = { abortController: null };

    const codingRefs = {
        list: document.getElementById("codingSessionList"),
        messages: document.getElementById("codingMessages"),
        title: document.getElementById("codingActiveTitle"),
        meta: document.getElementById("codingActiveMeta"),
        inputId: document.getElementById("codingChatId")
    };

    const manusRefs = {
        list: document.getElementById("manusSessionList"),
        messages: document.getElementById("manusMessages"),
        title: document.getElementById("manusActiveTitle"),
        meta: document.getElementById("manusActiveMeta"),
        inputId: document.getElementById("manusSessionId")
    };

    const codingForm = document.getElementById("codingForm");
    const codingInput = document.getElementById("codingInput");
    const codingStopBtn = document.getElementById("codingStopBtn");
    const codingNewSessionBtn = document.getElementById("codingNewSessionBtn");
    const codingFeatureText = document.querySelector(".pgapp-panel .panel-head p");

    const manusForm = document.getElementById("manusForm");
    const manusInput = document.getElementById("manusInput");
    const manusStopBtn = document.getElementById("manusStopBtn");
    const manusNewSessionBtn = document.getElementById("manusNewSessionBtn");
    const authGate = document.getElementById("authGate");
    const authHint = document.getElementById("authHint");
    const authUserText = document.getElementById("authUserText");
    const logoutBtn = document.getElementById("logoutBtn");
    const showLoginTabBtn = document.getElementById("showLoginTab");
    const showRegisterTabBtn = document.getElementById("showRegisterTab");
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");
    const loginUsernameInput = document.getElementById("loginUsername");
    const loginPasswordInput = document.getElementById("loginPassword");
    const registerUsernameInput = document.getElementById("registerUsername");
    const registerNicknameInput = document.getElementById("registerNickname");
    const registerPasswordInput = document.getElementById("registerPassword");
    const registerConfirmPasswordInput = document.getElementById("registerConfirmPassword");
    const appShell = document.querySelector(".app-shell");
    const panelSwitchLoading = document.getElementById("panelSwitchLoading");
    const sidebarToggleButtons = Array.from(document.querySelectorAll("[data-sidebar-toggle]"));
    const panelSwitchButtons = Array.from(document.querySelectorAll(".view-switch-btn"));
    const panelNodes = {
        pgapp: document.querySelector('.chat-panel[data-panel="pgapp"]'),
        manus: document.querySelector('.chat-panel[data-panel="manus"]')
    };
    const PANEL_SWITCH_DURATION_MS = 260;
    let panelSwitchTimer = null;

    if (codingStopBtn) {
        codingStopBtn.textContent = "停止输出";
    }
    if (manusStopBtn) {
        manusStopBtn.textContent = "停止输出";
    }
    if (codingFeatureText) {
        codingFeatureText.textContent = "面向编程开发场景的智能对话，支持流式返回，适合需求拆解、方案设计与代码实现协作";
    }

    bindEnterToSubmit(codingInput, codingForm);
    bindEnterToSubmit(manusInput, manusForm);
    setupPanelSwitch("pgapp");
    setupSidebarToggle();

    const codingManager = createSessionManager({
        ...codingRefs,
        emptyMessage: "你好，我是你的编程助手，有什么可以帮到你吗？",
        metaPrefix: "",
        historyIdPrefix: "coding-session",
        onSessionDelete: async (chatId) => {
            await deleteCodingHistorySession(chatId);
        },
        onSessionActivated: (chatId) => {
            void loadCodingHistoryMessages(chatId);
        }
    });

    const manusManager = createSessionManager({
        ...manusRefs,
        emptyMessage: "你好，我是超级智能体，可以解决你的各种复杂问题",
        metaPrefix: "",
        historyIdPrefix: "manus-session",
        onSessionDelete: async (chatId) => {
            await deleteManusHistorySession(chatId);
        },
        onSessionActivated: (chatId) => {
            void loadManusHistoryMessages(chatId);
        }
    });

    initializeSession(codingManager, codingRefs.inputId, "coding-session-1");
    initializeSession(manusManager, manusRefs.inputId, "manus-session-1");

    codingNewSessionBtn.addEventListener("click", () => {
        const newId = createSessionId("coding-session");
        codingRefs.inputId.value = newId;
        codingManager.activate(newId);
    });

    manusNewSessionBtn.addEventListener("click", () => {
        const newId = createSessionId("manus-session");
        manusRefs.inputId.value = newId;
        manusManager.activate(newId);
    });

    bindSessionInput(codingRefs.inputId, codingManager, "coding-session");
    bindSessionInput(manusRefs.inputId, manusManager, "manus-session");
    initAuthModule();

    codingForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const message = codingInput.value.trim();
        if (!message) return;
        if (!ensureLoggedIn()) return;

        closeStream(codingState);

        const chatId = normalizeId(codingRefs.inputId.value, "coding-session");
        codingRefs.inputId.value = chatId;
        codingManager.activate(chatId);

        codingManager.addMessage(chatId, "user", message);
        codingInput.value = "";

        const assistantMessage = codingManager.addMessage(chatId, "assistant", "", { streaming: true });
        let usageInfo = null;
        const path = `/ai/pgapp/chat/rag/sse?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`;
        streamByFetch(path, codingState, {
            onChunk: (chunk) => codingManager.appendToMessage(chatId, assistantMessage, chunk),
            onUsage: (usage) => {
                usageInfo = usage;
            },
            onComplete: () => {
                const usageText = formatUsageText(usageInfo);
                if (usageText) {
                    codingManager.appendToMessage(chatId, assistantMessage, `\n\n${usageText}`);
                }
                if (!assistantMessage.text.trim()) {
                    codingManager.setMessageText(chatId, assistantMessage, "（已结束，未收到内容）");
                }
                codingManager.finishMessage(chatId, assistantMessage);
            },
            onError: (errMsg) => {
                const usageText = formatUsageText(usageInfo);
                if (usageText) {
                    codingManager.appendToMessage(chatId, assistantMessage, `\n\n${usageText}`);
                }
                if (!assistantMessage.text.trim()) {
                    codingManager.setMessageText(chatId, assistantMessage, errMsg);
                } else {
                    codingManager.addMessage(chatId, "system", errMsg);
                }
                codingManager.finishMessage(chatId, assistantMessage);
            }
        });
    });

    manusForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const message = manusInput.value.trim();
        if (!message) return;
        if (!ensureLoggedIn()) return;

        closeStream(manusState);

        const sessionId = normalizeId(manusRefs.inputId.value, "manus-session");
        manusRefs.inputId.value = sessionId;
        manusManager.activate(sessionId);

        manusManager.addMessage(sessionId, "user", message);
        manusInput.value = "";

        const assistantMessage = manusManager.addMessage(sessionId, "assistant", "", { streaming: true });
        let usageInfo = null;
        const path = `/ai/manus/chat?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(sessionId)}`;
        streamByFetch(path, manusState, {
            onChunk: (chunk) => manusManager.appendToMessage(sessionId, assistantMessage, chunk),
            onUsage: (usage) => {
                usageInfo = usage;
            },
            onComplete: () => {
                const usageText = formatUsageText(usageInfo);
                if (usageText) {
                    manusManager.appendToMessage(sessionId, assistantMessage, `\n\n${usageText}`);
                }
                if (!assistantMessage.text.trim()) {
                    manusManager.setMessageText(sessionId, assistantMessage, "（已结束，未收到内容）");
                }
                manusManager.finishMessage(sessionId, assistantMessage);
            },
            onError: (errMsg) => {
                const usageText = formatUsageText(usageInfo);
                if (usageText) {
                    manusManager.appendToMessage(sessionId, assistantMessage, `\n\n${usageText}`);
                }
                if (!assistantMessage.text.trim()) {
                    manusManager.setMessageText(sessionId, assistantMessage, errMsg);
                } else {
                    manusManager.addMessage(sessionId, "system", errMsg);
                }
                manusManager.finishMessage(sessionId, assistantMessage);
            }
        });
    });

    codingStopBtn.addEventListener("click", () => {
        closeStream(codingState);
        const activeId = codingManager.getActiveId();
        if (activeId) {
            codingManager.addMessage(activeId, "system", "已手动停止流式输出。");
        }
    });

    manusStopBtn.addEventListener("click", () => {
        closeStream(manusState);
        const activeId = manusManager.getActiveId();
        if (activeId) {
            manusManager.addMessage(activeId, "system", "已手动停止流式输出。");
        }
    });

    function initAuthModule() {
        switchAuthTab("login");
        updateAuthView();
        bindAuthEvents();
        restoreAuthSession();
    }

    function bindAuthEvents() {
        if (showLoginTabBtn) {
            showLoginTabBtn.addEventListener("click", () => switchAuthTab("login"));
        }
        if (showRegisterTabBtn) {
            showRegisterTabBtn.addEventListener("click", () => switchAuthTab("register"));
        }
        if (logoutBtn) {
            logoutBtn.addEventListener("click", async () => {
                await requestJson("/auth/logout", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" }
                }).catch(() => null);
                clearAuthState();
                resetAllSessionsToDefault();
                setAuthHint("已退出登录，请重新登录。");
                setAuthGateVisible(true);
            });
        }
        if (loginForm) {
            loginForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                const username = (loginUsernameInput?.value || "").trim();
                const password = loginPasswordInput?.value || "";
                if (!username || !password) {
                    setAuthHint("请输入用户名和密码。");
                    return;
                }
                try {
                    const data = await requestJson("/auth/login", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ username, password })
                    }, { disableAuth: true });
                    authState.token = data.token || "";
                    authState.user = data.user || null;
                    persistAuthState();
                    updateAuthView();
                    setAuthGateVisible(false);
                    await syncAllHistoryOnLogin();
                    setAuthHint("登录成功。");
                    if (loginPasswordInput) {
                        loginPasswordInput.value = "";
                    }
                } catch (e) {
                    setAuthHint(`登录失败：${toMessage(e)}`);
                }
            });
        }
        if (registerForm) {
            registerForm.addEventListener("submit", async (event) => {
                event.preventDefault();
                const username = (registerUsernameInput?.value || "").trim();
                const nickname = (registerNicknameInput?.value || "").trim();
                const password = registerPasswordInput?.value || "";
                const confirmPassword = registerConfirmPasswordInput?.value || "";
                if (password !== confirmPassword) {
                    setAuthHint("两次输入的密码不一致。");
                    return;
                }
                try {
                    await requestJson("/auth/register", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ username, nickname, password })
                    }, { disableAuth: true });
                    if (loginUsernameInput) {
                        loginUsernameInput.value = username;
                    }
                    if (loginPasswordInput) {
                        loginPasswordInput.value = "";
                    }
                    if (registerForm) {
                        registerForm.reset();
                    }
                    switchAuthTab("login");
                    setAuthHint("注册成功，请登录。");
                } catch (e) {
                    setAuthHint(`注册失败：${toMessage(e)}`);
                }
            });
        }
    }

    async function restoreAuthSession() {
        const saved = readPersistedAuthState();
        if (!saved || !saved.token) {
            setAuthGateVisible(true);
            return;
        }
        authState.token = saved.token;
        authState.user = saved.user || null;
        try {
            const user = await requestJson("/auth/me", {
                method: "GET"
            });
            authState.user = user;
            persistAuthState();
            setAuthGateVisible(false);
            updateAuthView();
            await syncAllHistoryOnLogin();
        } catch (_) {
            clearAuthState();
            resetAllSessionsToDefault();
            setAuthGateVisible(true);
            setAuthHint("登录已过期，请重新登录。");
        }
    }

    async function syncAllHistoryOnLogin() {
        try {
            await loadCodingHistorySessions();
            await loadManusHistorySessions();
        } catch (e) {
            setAuthHint(`历史会话加载失败：${toMessage(e)}`);
        }
    }

    function resetAllSessionsToDefault() {
        codingManager.clearSessions();
        manusManager.clearSessions();
        initializeSession(manusManager, manusRefs.inputId, "manus-session-1");
        initializeSession(codingManager, codingRefs.inputId, "coding-session-1");
    }

    function resetCodingSessionsToDefault() {
        codingManager.clearSessions();
        initializeSession(codingManager, codingRefs.inputId, "coding-session-1");
    }

    async function loadCodingHistorySessions() {
        if (!authState.token) return;
        const list = await requestJson("/ai/pgapp/history/sessions", {
            method: "GET"
        });
        const sessions = Array.isArray(list) ? list : [];
        if (!sessions.length) {
            return;
        }
        codingManager.resetFromHistory(sessions);
        const activeId = codingManager.getActiveId();
        if (activeId) {
            await loadCodingHistoryMessages(activeId, { force: true });
        }
    }

    async function loadCodingHistoryMessages(chatId, options = {}) {
        if (!authState.token) {
            return;
        }
        if (!chatId) {
            return;
        }
        const { force = false } = options;
        if (!force && codingManager.isHistoryLoaded(chatId)) {
            return;
        }
        const list = await requestJson(`/ai/pgapp/history/messages?chatId=${encodeURIComponent(chatId)}`, {
            method: "GET"
        });
        const records = Array.isArray(list) ? list : [];
        const mapped = records.map((item) => ({
            role: mapHistoryMessageTypeToRole(item.messageType),
            text: item.messageText || ""
        }));
        codingManager.replaceMessages(chatId, mapped);
    }

    async function loadManusHistorySessions() {
        if (!authState.token) return;
        const list = await requestJson("/ai/manus/history/sessions", {
            method: "GET"
        });
        const sessions = Array.isArray(list) ? list : [];
        if (!sessions.length) {
            return;
        }
        manusManager.resetFromHistory(sessions);
        const activeId = manusManager.getActiveId();
        if (activeId) {
            await loadManusHistoryMessages(activeId, { force: true });
        }
    }

    async function loadManusHistoryMessages(chatId, options = {}) {
        if (!authState.token) {
            return;
        }
        if (!chatId) {
            return;
        }
        const { force = false } = options;
        if (!force && manusManager.isHistoryLoaded(chatId)) {
            return;
        }
        const list = await requestJson(`/ai/manus/history/messages?chatId=${encodeURIComponent(chatId)}`, {
            method: "GET"
        });
        const records = Array.isArray(list) ? list : [];
        const mapped = records.map((item) => ({
            role: mapHistoryMessageTypeToRole(item.messageType),
            text: item.messageText || ""
        }));
        manusManager.replaceMessages(chatId, mapped);
    }

    async function deleteCodingHistorySession(chatId) {
        if (!authState.token || !chatId) {
            return;
        }
        await requestJson(`/ai/pgapp/history/session?chatId=${encodeURIComponent(chatId)}`, {
            method: "DELETE"
        });
    }

    async function deleteManusHistorySession(chatId) {
        if (!authState.token || !chatId) {
            return;
        }
        await requestJson(`/ai/manus/history/session?chatId=${encodeURIComponent(chatId)}`, {
            method: "DELETE"
        });
    }

    function ensureLoggedIn() {
        if (authState.token) {
            return true;
        }
        setAuthGateVisible(true);
        switchAuthTab("login");
        setAuthHint("请先登录后再发送消息。");
        return false;
    }

    function switchAuthTab(tab) {
        const loginActive = tab !== "register";
        if (showLoginTabBtn) {
            showLoginTabBtn.classList.toggle("active", loginActive);
            showLoginTabBtn.setAttribute("aria-selected", String(loginActive));
        }
        if (showRegisterTabBtn) {
            showRegisterTabBtn.classList.toggle("active", !loginActive);
            showRegisterTabBtn.setAttribute("aria-selected", String(!loginActive));
        }
        if (loginForm) {
            loginForm.classList.toggle("active", loginActive);
        }
        if (registerForm) {
            registerForm.classList.toggle("active", !loginActive);
        }
    }

    function setAuthHint(text) {
        if (!authHint) return;
        authHint.textContent = text || "";
    }

    function setAuthGateVisible(visible) {
        if (!authGate) return;
        authGate.classList.toggle("active", visible);
        authGate.setAttribute("aria-hidden", String(!visible));
    }

    function updateAuthView() {
        if (authUserText) {
            authUserText.textContent = authState.user
                ? `${authState.user.nickname || authState.user.username}（${authState.user.username}）`
                : "未登录";
        }
        if (logoutBtn) {
            logoutBtn.hidden = !authState.token;
        }
    }

    function persistAuthState() {
        try {
            const payload = JSON.stringify({
                token: authState.token || "",
                user: authState.user || null
            });
            localStorage.setItem(AUTH_STORAGE_KEY, payload);
        } catch (_) {
            // Ignore local persistence errors and keep runtime auth state.
        }
    }

    function readPersistedAuthState() {
        try {
            const raw = localStorage.getItem(AUTH_STORAGE_KEY);
            if (!raw) return null;
            const data = JSON.parse(raw);
            if (!data || typeof data !== "object") return null;
            return data;
        } catch (_) {
            return null;
        }
    }

    function clearAuthState() {
        authState.token = "";
        authState.user = null;
        try {
            localStorage.removeItem(AUTH_STORAGE_KEY);
        } catch (_) {
            // Ignore local persistence errors and keep runtime auth state.
        }
        updateAuthView();
    }

    async function requestJson(path, init = {}, options = {}) {
        const { res, url } = await fetchWithApiBase(path, init, options);
        const text = await safeText(res);
        let body = null;
        if (text) {
            try {
                body = JSON.parse(text);
            } catch (_) {
                body = null;
            }
        }
        if (!res.ok) {
            if (res.status === 401) {
                clearAuthState();
                resetAllSessionsToDefault();
                setAuthGateVisible(true);
                setAuthHint("登录已过期，请重新登录。");
            }
            const message = body && body.message ? body.message : `${res.status} ${text}`;
            throw new Error(`${message} | URL: ${url}`);
        }
        if (!body || typeof body !== "object") {
            throw new Error(`接口返回非 JSON：${url}`);
        }
        if (body.code !== 0) {
            throw new Error(body.message || "请求失败");
        }
        return body.data;
    }

    function initializeSession(manager, inputEl, fallbackId) {
        const id = normalizeId(inputEl.value, fallbackId);
        inputEl.value = id;
        manager.activate(id);
    }

    function setupSidebarToggle() {
        if (!appShell || !sidebarToggleButtons.length) return;
        updateSidebarToggleState(appShell.classList.contains("sidebar-collapsed"));
        sidebarToggleButtons.forEach((button) => {
            button.addEventListener("click", () => {
                const collapsed = appShell.classList.toggle("sidebar-collapsed");
                updateSidebarToggleState(collapsed);
            });
        });
    }

    function updateSidebarToggleState(collapsed) {
        sidebarToggleButtons.forEach((button) => {
            button.dataset.collapsed = String(collapsed);
            button.setAttribute("aria-pressed", String(collapsed));
            button.textContent = collapsed ? "显示侧栏" : "隐藏侧栏";
        });
    }

    function setupPanelSwitch(defaultPanel) {
        if (!panelSwitchButtons.length) return;
        activatePanel(defaultPanel, { immediate: true });
        panelSwitchButtons.forEach((button) => {
            button.addEventListener("click", () => {
                const panelId = button.dataset.panelTarget;
                activatePanel(panelId);
            });
        });
    }

    function activatePanel(panelId, options = {}) {
        if (!panelNodes[panelId]) return;
        updatePanelSwitchButtons(panelId);

        const activeEntry = Object.entries(panelNodes).find(([, panel]) => panel && panel.classList.contains("active"));
        const activeId = activeEntry ? activeEntry[0] : null;
        const activePanel = activeEntry ? activeEntry[1] : null;
        const targetPanel = panelNodes[panelId];

        if (activeId === panelId && targetPanel && !targetPanel.classList.contains("is-exiting")) {
            return;
        }

        if (panelSwitchTimer) {
            clearTimeout(panelSwitchTimer);
            panelSwitchTimer = null;
            Object.values(panelNodes).forEach((panel) => panel && panel.classList.remove("is-exiting"));
            setPanelSwitchLoading(false);
        }

        const showTarget = () => {
            for (const [id, panel] of Object.entries(panelNodes)) {
                if (!panel) continue;
                const isActive = id === panelId;
                panel.classList.toggle("active", isActive);
                panel.classList.remove("is-exiting");
                panel.setAttribute("aria-hidden", String(!isActive));
            }
        };

        if (options.immediate || !activePanel) {
            showTarget();
            setPanelSwitchLoading(false);
            return;
        }

        setPanelSwitchLoading(true);
        activePanel.classList.add("is-exiting");
        activePanel.setAttribute("aria-hidden", "true");
        panelSwitchTimer = setTimeout(() => {
            showTarget();
            setPanelSwitchLoading(false);
            panelSwitchTimer = null;
        }, PANEL_SWITCH_DURATION_MS);
    }

    function updatePanelSwitchButtons(panelId) {
        panelSwitchButtons.forEach((button) => {
            const isActive = button.dataset.panelTarget === panelId;
            button.classList.toggle("active", isActive);
            button.setAttribute("aria-selected", String(isActive));
        });
    }

    function setPanelSwitchLoading(active) {
        if (!panelSwitchLoading) return;
        panelSwitchLoading.classList.toggle("active", active);
        panelSwitchLoading.setAttribute("aria-hidden", String(!active));
    }

    function bindSessionInput(inputEl, manager, prefix) {
        const applySession = () => {
            const id = normalizeId(inputEl.value, prefix);
            inputEl.value = id;
            manager.activate(id);
        };

        inputEl.addEventListener("blur", applySession);
        inputEl.addEventListener("keydown", (event) => {
            if (event.key !== "Enter") return;
            event.preventDefault();
            applySession();
        });
    }

    function renderMessageContent(container, text, options = {}) {
        const { streaming = false } = options;
        const rawText = text || "";
        const manusStepsView = resolveManusStepsView(container, rawText);

        container.textContent = "";
        if (manusStepsView) {
            renderManusStepsView(container, manusStepsView, { streaming });
            return;
        }

        appendMessageSegments(container, rawText, { streaming });
    }

    function appendMessageSegments(container, text, options = {}) {
        const { streaming = false } = options;
        const segments = splitMessageSegments(text || "");
        if (!segments.hasCode) {
            container.appendChild(createTextBlock(text || ""));
            return;
        }

        for (const segment of segments.parts) {
            if (segment.type === "text") {
                if (!segment.content) continue;
                container.appendChild(createTextBlock(segment.content));
                continue;
            }

            const codeBlock = document.createElement("div");
            codeBlock.className = "code-block";

            const codeHeader = document.createElement("div");
            codeHeader.className = "code-block-head";

            const codeLang = document.createElement("span");
            codeLang.className = "code-lang";
            codeLang.textContent = segment.language || "code";

            const copyButton = document.createElement("button");
            copyButton.type = "button";
            copyButton.className = "code-copy-btn";
            copyButton.textContent = "复制";
            copyButton.addEventListener("click", () => copyCodeToClipboard(segment.code, copyButton));

            const actions = document.createElement("div");
            actions.className = "code-actions";
            actions.appendChild(copyButton);

            const previewEnabled = canPreviewFrontendCode(segment.language, segment.code);
            if (previewEnabled) {
                const previewButton = document.createElement("button");
                previewButton.type = "button";
                previewButton.className = "code-preview-btn";
                previewButton.textContent = "预览";
                previewButton.disabled = streaming;
                previewButton.addEventListener("click", () => openCodePreview(container, segment.language, segment.code));
                actions.appendChild(previewButton);
            }

            const codeBox = document.createElement("textarea");
            codeBox.className = "code-copy-box";
            codeBox.readOnly = true;
            codeBox.value = segment.code;
            codeBox.rows = Math.min(Math.max(segment.code.split("\n").length + 1, 4), 16);

            codeHeader.appendChild(codeLang);
            codeHeader.appendChild(actions);
            codeBlock.appendChild(codeHeader);
            codeBlock.appendChild(codeBox);
            container.appendChild(codeBlock);
        }
    }

    function resolveManusStepsView(container, text) {
        if (!container || !container.classList || !container.classList.contains("assistant")) {
            return null;
        }
        if (!container.closest(".manus-panel")) {
            return null;
        }
        return parseManusStepsText(text);
    }

    function parseManusStepsText(text) {
        const raw = String(text || "");
        const stepRegex = /Step\s+(\d+)\s*:/g;
        const matches = [];
        let match;

        while ((match = stepRegex.exec(raw)) !== null) {
            matches.push({
                stepNo: Number(match[1]),
                start: match.index,
                contentStart: stepRegex.lastIndex
            });
        }

        if (!matches.length) {
            return null;
        }

        const steps = [];
        for (let i = 0; i < matches.length; i++) {
            const current = matches[i];
            const contentEnd = i + 1 < matches.length ? matches[i + 1].start : raw.length;
            const content = raw.slice(current.contentStart, contentEnd);
            if (!content || !content.trim()) {
                continue;
            }
            steps.push({
                stepNo: current.stepNo,
                label: `Step ${current.stepNo}`,
                content
            });
        }

        if (!steps.length) {
            return null;
        }

        const displayableSteps = steps.filter((step) => !shouldHideManusStep(step.content));
        const candidates = displayableSteps.length ? displayableSteps : steps;

        let finalStepIndex = -1;
        for (let i = candidates.length - 1; i >= 0; i--) {
            if (!isFailedManusStep(candidates[i].content)) {
                finalStepIndex = i;
                break;
            }
        }
        if (finalStepIndex < 0) {
            finalStepIndex = candidates.length - 1;
        }

        const finalStep = candidates[finalStepIndex];
        const previousSteps = [];
        for (let i = 0; i < candidates.length; i++) {
            if (i === finalStepIndex) {
                continue;
            }
            previousSteps.push(candidates[i]);
        }

        return { finalStep, previousSteps };
    }

    function shouldHideManusStep(text) {
        const raw = String(text || "");
        if (!raw) {
            return false;
        }
        const normalized = raw.toLowerCase();
        const isScrapeWebPageResult = normalized.includes("工具 scrapewebpage 返回结果")
            || normalized.includes("tool scrapewebpage")
            || normalized.includes("scrapewebpage returned");
        if (!isScrapeWebPageResult) {
            return false;
        }
        return normalized.includes("<!doctype html")
            || normalized.includes("<html")
            || normalized.includes("\\n<html")
            || normalized.includes("window.location")
            || normalized.includes("fingerprintjs");
    }

    function isFailedManusStep(text) {
        const normalized = String(text || "").trim().toLowerCase();
        if (!normalized) {
            return false;
        }
        return normalized.startsWith("步骤执行失败")
            || normalized.startsWith("step failed")
            || normalized.startsWith("error")
            || normalized.includes("执行错误：")
            || normalized.includes("执行错误:");
    }

    function renderManusStepsView(container, view, options = {}) {
        const { streaming = false } = options;
        const wrapper = document.createElement("div");
        wrapper.className = "manus-steps";

        const finalBlock = document.createElement("section");
        finalBlock.className = "manus-final-step";

        const finalLabel = document.createElement("div");
        finalLabel.className = "manus-step-label";
        finalLabel.textContent = `最终结果（${view.finalStep.label}）`;

        const finalBody = document.createElement("div");
        finalBody.className = "manus-step-body";
        appendMessageSegments(finalBody, view.finalStep.content, { streaming });

        finalBlock.appendChild(finalLabel);
        finalBlock.appendChild(finalBody);
        wrapper.appendChild(finalBlock);

        if (view.previousSteps.length > 0) {
            const collapsed = document.createElement("details");
            collapsed.className = "manus-previous-steps";

            const summary = document.createElement("summary");
            summary.textContent = `中间步骤 ${view.previousSteps.length} 条（默认折叠）`;
            collapsed.appendChild(summary);

            for (const step of view.previousSteps) {
                const stepItem = document.createElement("section");
                stepItem.className = "manus-step-item";

                const stepLabel = document.createElement("div");
                stepLabel.className = "manus-step-label manus-step-label-sub";
                stepLabel.textContent = step.label;

                const stepBody = document.createElement("div");
                stepBody.className = "manus-step-body";
                appendMessageSegments(stepBody, step.content, { streaming: false });

                stepItem.appendChild(stepLabel);
                stepItem.appendChild(stepBody);
                collapsed.appendChild(stepItem);
            }
            wrapper.appendChild(collapsed);
        }

        container.appendChild(wrapper);
    }

    function splitMessageSegments(text) {
        const fenceRegex = /```([a-zA-Z0-9_+#.-]*)\n?([\s\S]*?)```/g;
        const parts = [];
        let lastIndex = 0;
        let hasCode = false;
        let match;

        while ((match = fenceRegex.exec(text)) !== null) {
            const fullMatch = match[0];
            const language = (match[1] || "").trim();
            const code = (match[2] || "");
            const start = match.index;
            if (start > lastIndex) {
                parts.push({ type: "text", content: text.slice(lastIndex, start) });
            }
            parts.push({ type: "code", language, code });
            hasCode = true;
            lastIndex = start + fullMatch.length;
        }

        if (lastIndex < text.length) {
            parts.push({ type: "text", content: text.slice(lastIndex) });
        }

        return { hasCode, parts };
    }

    function createTextBlock(text) {
        const textBlock = document.createElement("div");
        textBlock.className = "bubble-text";
        const lines = String(text || "").split("\n");
        for (const line of lines) {
            const lineNode = document.createElement("div");
            if (!line) {
                lineNode.className = "text-line";
                lineNode.textContent = "\u00A0";
                textBlock.appendChild(lineNode);
                continue;
            }
            const headingMatch = line.match(/^(\s*)(#{1,6})\s+(.+)$/);
            if (headingMatch) {
                const level = Math.min(headingMatch[2].length, 4);
                lineNode.className = `text-line heading-line h${level}`;
                if (headingMatch[1]) {
                    lineNode.appendChild(document.createTextNode(headingMatch[1]));
                }
                renderInlineMarkdownText(lineNode, headingMatch[3]);
                textBlock.appendChild(lineNode);
                continue;
            }

            const unorderedBulletMatch = line.match(/^(\s*)[-*•]\s+(.+)$/);
            if (unorderedBulletMatch) {
                lineNode.className = "text-line bullet-line unordered-bullet-line";
                if (unorderedBulletMatch[1]) {
                    lineNode.appendChild(document.createTextNode(unorderedBulletMatch[1]));
                }
                renderInlineMarkdownText(lineNode, unorderedBulletMatch[2]);
                textBlock.appendChild(lineNode);
                continue;
            }

            const orderedBulletMatch = line.match(/^(\s*)(\d{1,3}[.)]|[一二三四五六七八九十]+、)\s+(.+)$/);
            if (orderedBulletMatch) {
                lineNode.className = "text-line bullet-line ordered-bullet-line";
                if (orderedBulletMatch[1]) {
                    lineNode.appendChild(document.createTextNode(orderedBulletMatch[1]));
                }
                const marker = document.createElement("span");
                marker.className = "bullet-index";
                marker.textContent = `${orderedBulletMatch[2]} `;
                lineNode.appendChild(marker);
                renderInlineMarkdownText(lineNode, orderedBulletMatch[3]);
                textBlock.appendChild(lineNode);
                continue;
            }

            lineNode.className = "text-line";
            renderInlineMarkdownText(lineNode, line);
            textBlock.appendChild(lineNode);
        }
        return textBlock;
    }

    function renderInlineMarkdownText(target, text) {
        const inlineRegex = /(\*\*[\s\S]+?\*\*|__[\s\S]+?__|\*[^*\n]+\*|_[^_\n]+_)/g;
        let lastIndex = 0;
        let match;

        while ((match = inlineRegex.exec(text)) !== null) {
            const start = match.index;
            if (start > lastIndex) {
                target.appendChild(document.createTextNode(text.slice(lastIndex, start)));
            }
            const token = match[0];
            const isStrong = token.startsWith("**") || token.startsWith("__");
            const span = document.createElement("span");
            span.className = isStrong ? "inline-strong" : "inline-em";
            span.textContent = isStrong ? token.slice(2, -2) : token.slice(1, -1);
            target.appendChild(span);
            lastIndex = start + match[0].length;
        }

        if (lastIndex < text.length) {
            target.appendChild(document.createTextNode(text.slice(lastIndex)));
        }
    }

    function canPreviewFrontendCode(language, code) {
        const lang = String(language || "").trim().toLowerCase();
        if (lang === "html" || lang === "htm" || lang === "css" || lang === "js" || lang === "javascript") {
            return true;
        }
        return /<!doctype html|<html|<head|<body|<style|<script/i.test(code);
    }

    function renderCodePreview(frame, language, code) {
        const lang = String(language || "").trim().toLowerCase();
        const previewDoc = buildPreviewDocument(lang, code || "");
        frame.srcdoc = previewDoc;
    }

    function openCodePreview(sourceNode, language, code) {
        const previewRoot = getOrCreatePreviewDrawer();
        const previewFrame = previewRoot.querySelector(".chat-preview-frame");
        const previewTitle = previewRoot.querySelector(".chat-preview-title");
        if (previewTitle) {
            previewTitle.textContent = `运行结果预览 · ${resolvePreviewLabel(language)}`;
        }
        renderCodePreview(previewFrame, language, code);
        previewRoot.classList.add("active");
        previewRoot.setAttribute("aria-hidden", "false");
        document.body.classList.add("code-preview-open");
    }

    function closeCodePreview(previewRoot) {
        if (!previewRoot) return;
        previewRoot.classList.remove("active");
        previewRoot.setAttribute("aria-hidden", "true");
        document.body.classList.remove("code-preview-open");
        const previewFrame = previewRoot.querySelector(".chat-preview-frame");
        if (previewFrame) {
            previewFrame.srcdoc = "";
        }
    }

    function getOrCreatePreviewDrawer() {
        let previewRoot = document.querySelector(".chat-preview-drawer-root");
        if (previewRoot) {
            return previewRoot;
        }

        previewRoot = document.createElement("div");
        previewRoot.className = "chat-preview-drawer-root";
        previewRoot.setAttribute("aria-hidden", "true");

        const backdrop = document.createElement("div");
        backdrop.className = "chat-preview-backdrop";
        backdrop.addEventListener("click", () => closeCodePreview(previewRoot));

        const drawer = document.createElement("aside");
        drawer.className = "chat-preview-drawer";
        drawer.setAttribute("role", "dialog");
        drawer.setAttribute("aria-modal", "true");
        drawer.setAttribute("aria-label", "前端代码运行预览");

        const previewHead = document.createElement("div");
        previewHead.className = "chat-preview-head";

        const previewTitle = document.createElement("span");
        previewTitle.className = "chat-preview-title";
        previewTitle.textContent = "运行结果预览";

        const closeButton = document.createElement("button");
        closeButton.type = "button";
        closeButton.className = "chat-preview-close-btn";
        closeButton.textContent = "关闭";
        closeButton.addEventListener("click", () => closeCodePreview(previewRoot));

        const previewFrame = document.createElement("iframe");
        previewFrame.className = "chat-preview-frame";
        previewFrame.setAttribute("sandbox", "allow-scripts allow-forms allow-modals allow-popups allow-popups-to-escape-sandbox allow-downloads allow-top-navigation-by-user-activation allow-same-origin");
        previewFrame.setAttribute("referrerpolicy", "no-referrer");

        previewHead.appendChild(previewTitle);
        previewHead.appendChild(closeButton);
        drawer.appendChild(previewHead);
        drawer.appendChild(previewFrame);
        previewRoot.appendChild(backdrop);
        previewRoot.appendChild(drawer);
        document.body.appendChild(previewRoot);

        window.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && previewRoot.classList.contains("active")) {
                closeCodePreview(previewRoot);
            }
        });

        return previewRoot;
    }

    function resolvePreviewLabel(language) {
        const lang = String(language || "").trim().toLowerCase();
        if (!lang) {
            return "HTML";
        }
        if (lang === "js") {
            return "JavaScript";
        }
        if (lang === "htm") {
            return "HTML";
        }
        return lang.toUpperCase();
    }

    function buildPreviewDocument(lang, code) {
        const originalCode = String(code || "");
        const escapedScript = originalCode.replace(/<\/script>/gi, "<\\/script>");
        const hasHtmlShell = /<!doctype html|<html|<head|<body/i.test(originalCode);
        if (hasHtmlShell) {
            return originalCode;
        }

        if (lang === "js" || lang === "javascript") {
            return `<!doctype html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
<script>
${escapedScript}
<\/script>
</body>
</html>`;
        }

        if (lang === "css") {
            return `<!doctype html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
${originalCode}
</style>
</head>
<body></body>
</html>`;
        }

        return `<!doctype html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
${originalCode}
</body>
</html>`;
    }

    async function copyCodeToClipboard(code, button) {
        const originalText = button.textContent;
        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(code);
            } else {
                const temp = document.createElement("textarea");
                temp.value = code;
                document.body.appendChild(temp);
                temp.select();
                document.execCommand("copy");
                temp.remove();
            }
            button.textContent = "已复制";
        } catch (_) {
            button.textContent = "复制失败";
        }
        setTimeout(() => {
            button.textContent = originalText;
        }, 1200);
    }

    function createSessionManager({
        list,
        messages,
        title,
        meta,
        emptyMessage,
        metaPrefix,
        inputId,
        onSessionActivated,
        onSessionDelete,
        historyIdPrefix = "session"
    }) {
        const sessions = new Map();
        let activeId = null;
        let openMenuSessionId = null;

        document.addEventListener("click", (event) => {
            if (!openMenuSessionId) {
                return;
            }
            const target = event.target;
            if (!(target instanceof Element)) {
                closeActionMenu();
                return;
            }
            if (!list.contains(target)) {
                closeActionMenu();
            }
        });

        function sortSessions(sessionItems) {
            return sessionItems.sort((a, b) => {
                const pinDiff = Number(!!b.pinned) - Number(!!a.pinned);
                if (pinDiff !== 0) {
                    return pinDiff;
                }
                return b.updatedAt - a.updatedAt;
            });
        }

        function closeActionMenu() {
            if (!openMenuSessionId) {
                return;
            }
            openMenuSessionId = null;
            renderSessionList();
        }

        function patchMessageBubble(id, messageObj) {
            if (id !== activeId || !messageObj) return false;
            const bubble = messageObj.element;
            if (!bubble || !bubble.isConnected) return false;
            bubble.className = `bubble ${messageObj.role}${messageObj.streaming ? " is-streaming" : ""}`;
            renderMessageContent(bubble, messageObj.text, { streaming: messageObj.streaming });
            messages.scrollTop = messages.scrollHeight;
            return true;
        }

        function ensureSession(id) {
            if (!sessions.has(id)) {
                sessions.set(id, {
                    id,
                    title: "新会话",
                    hasCustomTitle: false,
                    messages: [],
                    historyLoaded: false,
                    updatedAt: Date.now(),
                    createdAt: Date.now(),
                    pinned: false
                });
            }
            return sessions.get(id);
        }

        function activate(id) {
            const session = ensureSession(id);
            activeId = session.id;
            openMenuSessionId = null;
            renderSessionList();
            renderActiveSession();
            if (inputId) {
                inputId.value = session.id;
            }
        }

        function getActiveId() {
            return activeId;
        }

        function addMessage(id, role, text, options = {}) {
            const session = ensureSession(id);
            const message = { role, text: text || "", streaming: !!options.streaming };
            session.messages.push(message);
            session.historyLoaded = true;
            session.updatedAt = Date.now();

            if (role === "user" && !session.hasCustomTitle && text && text.trim()) {
                session.title = createSessionTitle(text);
                session.hasCustomTitle = true;
            }

            if (!activeId) {
                activeId = session.id;
            }

            renderSessionList();
            renderActiveSession();
            return message;
        }

        function appendToMessage(id, messageObj, chunk) {
            if (!chunk) return;
            messageObj.text += chunk;
            messageObj.streaming = true;
            patchMessageBubble(id, messageObj);
        }

        function setMessageText(id, messageObj, text) {
            messageObj.text = text || "";
            if (!patchMessageBubble(id, messageObj) && id === activeId) {
                renderActiveSession();
            }
        }

        function finishMessage(id, messageObj) {
            messageObj.streaming = false;
            if (!patchMessageBubble(id, messageObj) && id === activeId) {
                renderActiveSession();
            }
        }

        function togglePin(id) {
            const session = sessions.get(id);
            if (!session) {
                return;
            }
            session.pinned = !session.pinned;
            openMenuSessionId = null;
            renderSessionList();
        }

        async function deleteSession(id) {
            const session = sessions.get(id);
            if (!session) {
                return;
            }
            const confirmed = window.confirm("确定删除该对话吗？");
            if (!confirmed) {
                return;
            }

            try {
                if (onSessionDelete) {
                    await onSessionDelete(id);
                }
            } catch (e) {
                window.alert(`删除失败：${toMessage(e)}`);
                return;
            }

            const deletingActive = activeId === id;
            sessions.delete(id);
            if (openMenuSessionId === id) {
                openMenuSessionId = null;
            }

            if (deletingActive) {
                const sorted = sortSessions(Array.from(sessions.values()));
                activeId = sorted.length ? sorted[0].id : null;
                if (inputId) {
                    inputId.value = activeId || "";
                }
                if (activeId && onSessionActivated) {
                    Promise.resolve(onSessionActivated(activeId)).catch(() => null);
                }
            }

            renderSessionList();
            renderActiveSession();

            if (!activeId) {
                const fallbackId = createSessionId(historyIdPrefix);
                activate(fallbackId);
            }
        }

        function renderSessionList() {
            const sorted = sortSessions(Array.from(sessions.values()));

            list.innerHTML = "";
            for (let index = 0; index < sorted.length; index++) {
                const session = sorted[index];
                const item = document.createElement("div");
                item.className = `session-item ${session.id === activeId ? "active" : ""} ${session.pinned ? "pinned" : ""} ${openMenuSessionId === session.id ? "menu-open" : ""}`;
                item.style.setProperty("--session-delay", `${Math.min(index * 24, 180)}ms`);

                const mainBtn = document.createElement("button");
                mainBtn.type = "button";
                mainBtn.className = "session-main-btn";
                mainBtn.innerHTML = `<div class="session-title">${escapeHtml(session.title)}</div>`;
                mainBtn.addEventListener("click", () => {
                    activate(session.id);
                    if (onSessionActivated) {
                        Promise.resolve(onSessionActivated(session.id)).catch(() => null);
                    }
                });

                const actionWrap = document.createElement("div");
                actionWrap.className = "session-item-action-wrap";

                const actionBtn = document.createElement("button");
                actionBtn.type = "button";
                actionBtn.className = "session-action-trigger";
                actionBtn.setAttribute("aria-label", "会话操作");
                actionBtn.textContent = "⋯";
                actionBtn.addEventListener("click", (event) => {
                    event.stopPropagation();
                    openMenuSessionId = openMenuSessionId === session.id ? null : session.id;
                    renderSessionList();
                });

                const actionMenu = document.createElement("div");
                actionMenu.className = `session-action-menu ${openMenuSessionId === session.id ? "open" : ""}`;

                const pinActionBtn = document.createElement("button");
                pinActionBtn.type = "button";
                pinActionBtn.className = "session-action-menu-item";
                pinActionBtn.textContent = session.pinned ? "取消置顶" : "置顶对话";
                pinActionBtn.addEventListener("click", (event) => {
                    event.stopPropagation();
                    togglePin(session.id);
                });

                const deleteActionBtn = document.createElement("button");
                deleteActionBtn.type = "button";
                deleteActionBtn.className = "session-action-menu-item danger";
                deleteActionBtn.textContent = "删除对话";
                deleteActionBtn.addEventListener("click", async (event) => {
                    event.stopPropagation();
                    await deleteSession(session.id);
                });

                actionMenu.appendChild(pinActionBtn);
                actionMenu.appendChild(deleteActionBtn);
                actionWrap.appendChild(actionBtn);
                actionWrap.appendChild(actionMenu);

                item.appendChild(mainBtn);
                item.appendChild(actionWrap);
                list.appendChild(item);
            }
        }

        function renderActiveSession() {
            if (!activeId) {
                title.textContent = "未选择会话";
                if (meta) {
                    meta.textContent = "";
                    meta.hidden = true;
                }
                messages.innerHTML = "";
                messages.classList.remove("is-empty");
                return;
            }

            const session = ensureSession(activeId);
            title.textContent = session.title;
            if (metaPrefix) {
                meta.textContent = `${metaPrefix}: ${session.id}`;
                meta.hidden = false;
            } else {
                meta.textContent = "";
                meta.hidden = true;
            }

            for (const msg of session.messages) {
                msg.element = null;
            }
            messages.innerHTML = "";
            if (session.messages.length === 0) {
                messages.classList.add("is-empty");
                const empty = document.createElement("div");
                empty.className = "empty-state-message";
                empty.textContent = emptyMessage;
                messages.appendChild(empty);
            } else {
                messages.classList.remove("is-empty");
                for (let index = 0; index < session.messages.length; index++) {
                    const msg = session.messages[index];
                    const bubble = document.createElement("div");
                    bubble.className = `bubble ${msg.role}${msg.streaming ? " is-streaming" : ""}`;
                    bubble.style.setProperty("--bubble-delay", `${Math.min(index * 20, 200)}ms`);
                    renderMessageContent(bubble, msg.text, { streaming: msg.streaming });
                    msg.element = bubble;
                    messages.appendChild(bubble);
                }
            }
            messages.scrollTop = messages.scrollHeight;
        }

        function resetFromHistory(items) {
            const historyItems = Array.isArray(items) ? items : [];
            if (!historyItems.length) {
                return;
            }
            const pinnedIdSet = new Set(
                Array.from(sessions.values())
                    .filter((item) => item && item.pinned)
                    .map((item) => item.id)
            );
            sessions.clear();
            let firstId = "";
            let preferredId = "";

            for (let index = 0; index < historyItems.length; index++) {
                const item = historyItems[index] || {};
                const id = normalizeId(item.chatId || "", `${historyIdPrefix}-${Date.now()}-${index}`);
                const lastMessageText = String(item.lastMessageText || "").trim();
                const updatedAt = parseDatetimeToTimestamp(item.lastMessageAt) || Date.now() - index;
                sessions.set(id, {
                    id,
                    title: lastMessageText ? createSessionTitle(lastMessageText) : id,
                    hasCustomTitle: true,
                    messages: [],
                    historyLoaded: false,
                    updatedAt,
                    createdAt: updatedAt,
                    pinned: pinnedIdSet.has(id)
                });
                if (!firstId) {
                    firstId = id;
                }
                if (id === activeId) {
                    preferredId = id;
                }
            }

            activeId = preferredId || firstId;
            openMenuSessionId = null;
            renderSessionList();
            renderActiveSession();
            if (inputId && activeId) {
                inputId.value = activeId;
            }
        }

        function replaceMessages(id, messageList) {
            const session = ensureSession(id);
            const items = Array.isArray(messageList) ? messageList : [];
            session.messages = items.map((item) => ({
                role: item.role || "assistant",
                text: item.text || "",
                streaming: false
            }));
            session.historyLoaded = true;
            const latestMessage = session.messages.length ? session.messages[session.messages.length - 1] : null;
            if (latestMessage && latestMessage.text) {
                session.updatedAt = Date.now();
            }
            if (!session.hasCustomTitle) {
                const firstUser = session.messages.find((msg) => msg.role === "user" && msg.text && msg.text.trim());
                if (firstUser) {
                    session.title = createSessionTitle(firstUser.text);
                    session.hasCustomTitle = true;
                }
            }
            renderSessionList();
            if (id === activeId) {
                renderActiveSession();
            }
        }

        function isHistoryLoaded(id) {
            if (!sessions.has(id)) {
                return false;
            }
            return !!sessions.get(id).historyLoaded;
        }

        function clearSessions() {
            sessions.clear();
            activeId = null;
            openMenuSessionId = null;
            renderSessionList();
            renderActiveSession();
        }

        return {
            activate,
            addMessage,
            appendToMessage,
            setMessageText,
            finishMessage,
            getActiveId,
            resetFromHistory,
            replaceMessages,
            isHistoryLoaded,
            clearSessions
        };
    }

    async function streamByFetch(path, state, handlers) {
        const abortController = new AbortController();
        state.abortController = abortController;

        try {
            const { res, url } = await fetchWithApiBase(path, {
                method: "GET",
                signal: abortController.signal,
                headers: {
                    "Accept": "text/event-stream"
                }
            });

            if (!res.ok || !res.body) {
                const err = await safeText(res);
                if (res.status === 401) {
                    clearAuthState();
                    setAuthGateVisible(true);
                    setAuthHint("登录已过期，请重新登录。");
                }
                throw new Error(`${res.status} ${err} | URL: ${url}`);
            }

            const reader = res.body.getReader();
            const decoder = new TextDecoder("utf-8");
            let buffer = "";

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const parsed = parseChunk(buffer);
                buffer = parsed.rest;
                if (parsed.text) {
                    handlers.onChunk(parsed.text);
                }
                if (parsed.usages && handlers.onUsage) {
                    for (const usagePayload of parsed.usages) {
                        const usage = parseUsagePayload(usagePayload);
                        if (usage) {
                            handlers.onUsage(usage);
                        }
                    }
                }
            }

            if (buffer.length > 0) {
                const tailEvent = parseSseEvent(buffer);
                if (tailEvent && tailEvent.data !== "[DONE]") {
                    if (tailEvent.event === "usage" && handlers.onUsage) {
                        const usage = parseUsagePayload(tailEvent.data);
                        if (usage) {
                            handlers.onUsage(usage);
                        }
                    } else {
                        handlers.onChunk(tailEvent.data);
                    }
                }
            }
            handlers.onComplete();
        } catch (e) {
            if (e && e.name === "AbortError") {
                handlers.onComplete();
                return;
            }
            handlers.onError(`流式请求失败：${toMessage(e)}`);
        } finally {
            state.abortController = null;
        }
    }

    async function fetchWithApiBase(path, init = {}, options = {}) {
        const headers = new Headers(init.headers || {});
        if (!options.disableAuth && authState.token) {
            headers.set("Authorization", `Bearer ${authState.token}`);
        }
        const requestInit = {
            ...init,
            headers
        };
        let lastResponse = null;
        let lastUrl = "";
        const errors = [];

        for (let i = 0; i < API_BASES.length; i++) {
            const apiBase = API_BASES[i];
            const url = `${apiBase}${path}`;
            try {
                const res = await fetch(url, requestInit);
                lastResponse = res;
                lastUrl = url;
                // Fallback to next base when current host likely has no /api reverse proxy.
                if (res.status === 404 && i < API_BASES.length - 1) {
                    continue;
                }
                return { res, url };
            } catch (e) {
                errors.push(`${url} -> ${toMessage(e)}`);
            }
        }

        if (lastResponse) {
            return { res: lastResponse, url: lastUrl };
        }

        const detail = errors.length ? errors.join("; ") : "unknown error";
        throw new Error(`无法连接后端接口，已尝试：${detail}`);
    }

    function parseChunk(buffer) {
        const events = buffer.split(/\r?\n\r?\n/);
        const rest = events.pop() ?? "";
        let text = "";
        const usages = [];

        for (const eventText of events) {
            if (!eventText) continue;
            const parsedEvent = parseSseEvent(eventText);
            if (!parsedEvent || parsedEvent.data === "[DONE]") continue;
            if (parsedEvent.event === "usage") {
                usages.push(parsedEvent.data);
                continue;
            }
            text += parsedEvent.data;
        }

        return { text, usages, rest };
    }

    function parseSseEvent(eventText) {
        const lines = String(eventText || "").split(/\r?\n/);
        let eventName = "message";
        const dataLines = [];

        for (const line of lines) {
            if (line.startsWith("event:")) {
                eventName = (line.charAt(6) === " " ? line.slice(7) : line.slice(6)).trim() || "message";
                continue;
            }
            if (line.startsWith("data:")) {
                const payload = line.slice(5);
                dataLines.push(payload);
                continue;
            }
            if (line.startsWith("event:") || line.startsWith("id:") || line.startsWith("retry:") || line.startsWith(":")) {
                continue;
            }
            if (line) {
                dataLines.push(line);
            }
        }

        return {
            event: eventName,
            data: dataLines.join("\n")
        };
    }

    function parseUsagePayload(payload) {
        try {
            const usage = JSON.parse(payload);
            return usage && typeof usage === "object" ? usage : null;
        } catch (e) {
            return null;
        }
    }

    function formatUsageText(usage) {
        if (!usage) return "";
        if (usage.available === false) {
            return "Token统计：不可用";
        }
        const promptTokens = Number.isFinite(Number(usage.promptTokens)) ? Number(usage.promptTokens) : 0;
        const completionTokens = Number.isFinite(Number(usage.completionTokens)) ? Number(usage.completionTokens) : 0;
        const totalTokens = Number.isFinite(Number(usage.totalTokens))
            ? Number(usage.totalTokens)
            : (promptTokens + completionTokens);
        return `Token统计：输入 ${promptTokens} / 输出 ${completionTokens} / 总计 ${totalTokens}`;
    }

    function closeStream(state) {
        if (state.abortController) {
            state.abortController.abort();
            state.abortController = null;
        }
    }

    function normalizeId(idText, prefix) {
        const value = (idText || "").trim();
        if (value) return value;
        return createSessionId(prefix);
    }

    function createSessionId(prefix) {
        return `${prefix}-${Date.now()}`;
    }

    function createSessionTitle(text) {
        const normalized = (text || "").replace(/\s+/g, " ").trim();
        if (!normalized) return "新会话";

        const firstSentence = normalized.split(/[。！？?!\n]/)[0].trim() || normalized;
        const maxLen = 18;
        if (firstSentence.length <= maxLen) {
            return firstSentence;
        }
        return `${firstSentence.slice(0, maxLen)}...`;
    }

    function mapHistoryMessageTypeToRole(messageType) {
        const type = String(messageType || "").trim().toUpperCase();
        if (type === "USER") return "user";
        if (type === "SYSTEM") return "system";
        return "assistant";
    }

    function parseDatetimeToTimestamp(datetimeValue) {
        if (!datetimeValue) return 0;
        const ts = Date.parse(datetimeValue);
        if (!Number.isFinite(ts)) return 0;
        return ts;
    }

    function bindEnterToSubmit(textarea, form) {
        if (!textarea || !form) return;
        textarea.addEventListener("keydown", (event) => {
            if (event.key !== "Enter") return;
            if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;
            if (event.isComposing) return;
            event.preventDefault();
            form.requestSubmit();
        });
    }

    function resolveApiBases() {
        const bases = [];
        const host = window.location.hostname || "localhost";
        const protocol = window.location.protocol;
        const port = window.location.port;

        if (protocol === "file:") {
            appendApiBase(bases, "http://localhost:8123/api");
            appendApiBase(bases, "http://127.0.0.1:8123/api");
            return bases;
        }

        appendApiBase(bases, `${window.location.origin}/api`);

        const isLocalHost = host === "localhost" || host === "127.0.0.1";
        if (port === "8123" || isLocalHost) {
            if (protocol === "https:") {
                appendApiBase(bases, `https://${host}:8123/api`);
                appendApiBase(bases, `http://${host}:8123/api`);
            } else {
                appendApiBase(bases, `http://${host}:8123/api`);
                appendApiBase(bases, `https://${host}:8123/api`);
            }
        }
        return bases;
    }

    function appendApiBase(bases, base) {
        if (!base || bases.includes(base)) return;
        bases.push(base);
    }

    function escapeHtml(text) {
        return String(text)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    async function safeText(response) {
        try {
            return await response.text();
        } catch (_) {
            return "";
        }
    }

    function toMessage(e) {
        return e && e.message ? e.message : String(e);
    }
})();

