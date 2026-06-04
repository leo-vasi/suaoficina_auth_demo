const BASE_URL = "https://localhost:8443/auth";

// Mapeia mensagens de erro do backend
function friendlyError(raw, context) {
  const msg = (raw || "").toLowerCase();

  // E-mail já cadastrado
  if (msg.includes("already") || msg.includes("exists") || msg.includes("duplicate") || msg.includes("já cadastrado"))
    return "Este e-mail já está cadastrado. Tente fazer login.";

  // Credenciais inválidas
  if (msg.includes("invalid credentials") || msg.includes("senha") || msg.includes("invalid password"))
    return "E-mail ou senha incorretos. Verifique os dados e tente novamente.";

  // Conta bloqueada / brute force
  if (msg.includes("blocked") || msg.includes("locked") || msg.includes("too many") || msg.includes("bloqueada"))
    return "Conta bloqueada temporariamente por excesso de tentativas. Aguarde alguns minutos.";

  // Token expirado
  if (msg.includes("expired") || msg.includes("expirado"))
    return context === "session"
      ? "Sua sessão expirou. Por favor, faça login novamente."
      : "Este token expirou. Solicite um novo link de recuperação.";

  // Token inválido
  if (msg.includes("invalid token") || msg.includes("token inválido") || msg.includes("not found"))
    return context === "session"
      ? "Token de sessão inválido."
      : "Token inválido. Verifique o token ou solicite um novo.";

  // Usuário não encontrado
  if (msg.includes("user not found") || msg.includes("not found") || msg.includes("não encontrado"))
    return "Nenhuma conta encontrada com este e-mail.";

  // 2FA inválido
  if (msg.includes("2fa") || msg.includes("otp") || msg.includes("code"))
    return "Código 2FA inválido ou expirado. Tente novamente.";

  // Erro genérico de servidor
  if (msg.includes("500") || msg.includes("internal"))
    return "Erro interno no servidor. Tente novamente em instantes.";

  return raw || "Ocorreu um erro inesperado. Tente novamente.";
}

export async function giveConsent(email) {
  await fetch(`${BASE_URL}/consent`, {
    method: "POST",
    body: new URLSearchParams({ email }),
  });
}

export async function register(email, password) {
  const res = await fetch(`${BASE_URL}/register`, {
    method: "POST",
    body: new URLSearchParams({ email, password }),
  });
  if (!res.ok) throw new Error(friendlyError(await res.text(), "register"));
  return res.json();
}

export async function login(email, password) {
  const res = await fetch(`${BASE_URL}/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: new URLSearchParams({ email, password }),
  });

  if (!res.ok) {
    const msg = await res.text();

    if (msg === "2FA_REQUIRED") {
      return { requires2FA: true };
    }

    throw new Error(friendlyError(msg, "login"));
  }

  return res.json();
}

export async function enable2FA(email) {
  const res = await fetch(`${BASE_URL}/enable-2fa`, {
    method: "POST",
    body: new URLSearchParams({ email }),
  });
  if (!res.ok) throw new Error(friendlyError(await res.text(), "2fa"));
  return res.text();
}

export async function verify2FA(email, code) {
  const res = await fetch(`${BASE_URL}/verify-2fa`, {
    method: "POST",
    body: new URLSearchParams({ email, code }),
  });
  if (!res.ok) throw new Error(friendlyError(await res.text(), "2fa"));
  return res.json();
}

export async function forgotPassword(email) {
  const res = await fetch(`${BASE_URL}/forgot-password`, {
    method: "POST",
    body: new URLSearchParams({ email }),
  });
  if (!res.ok) throw new Error(friendlyError(await res.text(), "forgot"));
  return res.text();
}

export async function resetPassword(token, newPassword) {
  const res = await fetch(`${BASE_URL}/reset-password`, {
    method: "POST",
    body: new URLSearchParams({ token, newPassword }),
  });
  if (!res.ok) throw new Error(friendlyError(await res.text(), "reset"));
  return res.text();
}

export async function validateSession(token) {
  const res = await fetch(`${BASE_URL}/validate-session`, {
    method: "GET",
    headers: {
      "Authorization": `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error(friendlyError(await res.text(), "session"));
  return res.json();
}

export async function exportData(email) {
  const res = await fetch(`${BASE_URL}/export-data?email=${encodeURIComponent(email)}`);
  if (!res.ok) throw new Error(await res.text());
  return res.text();
}

export async function revokeConsent(email) {
  const res = await fetch(`${BASE_URL}/revoke-consent`, {
    method: "POST",
    body: new URLSearchParams({ email }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.text();
}

export async function deleteAccount(email) {
  const res = await fetch(`${BASE_URL}/delete-account`, {
    method: "DELETE",
    body: new URLSearchParams({ email }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.text();
}
